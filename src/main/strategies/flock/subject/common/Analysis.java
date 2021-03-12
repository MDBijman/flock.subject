package flock.subject.common;

import org.apache.commons.lang3.tuple.Pair;
import org.strategoxt.lang.Context;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.TermFactory;
import java.io.IOException;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.function.Supplier;
import org.spoofax.terms.StrategoTuple;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoInt;
import org.spoofax.terms.StrategoString;
import org.spoofax.terms.StrategoList;
import flock.subject.common.Graph;
import flock.subject.common.Graph.Node;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.Live;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.strategies.Program;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ConstProp;

public class Analysis {
	public HashSet<Node> dirty_live;
	public HashSet<Node> new_live;
	public boolean is_forward_live;
	public boolean has_run_live = false;
	public HashSet<Node> dirty_values;
	public HashSet<Node> new_values;
	public boolean is_forward_values;
	public boolean has_run_values = false;
	public HashSet<Node> dirty_alias;
	public HashSet<Node> new_alias;
	public boolean is_forward_alias;
	public boolean has_run_alias = false;

	public Analysis() {
		dirty_live = new HashSet<Node>();
		new_live = new HashSet<Node>();
		is_forward_live = false;
		dirty_values = new HashSet<Node>();
		new_values = new HashSet<Node>();
		is_forward_values = true;
		dirty_alias = new HashSet<Node>();
		new_alias = new HashSet<Node>();
		is_forward_alias = true;
	}

	public List<Set<Node>> getDirtySets() {
		ArrayList<Set<Node>> dirtySets = new ArrayList<>();
		dirtySets.add(this.dirty_live);
		dirtySets.add(this.dirty_values);
		dirtySets.add(this.dirty_alias);
		return dirtySets;
	}

	public List<Set<Node>> getNewSets() {
		ArrayList<Set<Node>> newSets = new ArrayList<>();
		newSets.add(this.new_live);
		newSets.add(this.new_values);
		newSets.add(this.new_alias);
		return newSets;
	}
	
	public void addToDirty(Node n) {
		dirty_live.add(n);
		dirty_values.add(n);
		dirty_alias.add(n);
	}

	public void addToNew(Node n) {
		new_live.add(n);
		new_values.add(n);
		new_alias.add(n);
	}

	public void removeFromDirty(Set<Node> removedNodes) {
		dirty_live.removeAll(removedNodes);
		dirty_values.removeAll(removedNodes);
		dirty_alias.removeAll(removedNodes);
	}

	public void removeFromNew(Set<Node> removedNodes) {
		new_live.removeAll(removedNodes);
		new_values.removeAll(removedNodes);
		new_alias.removeAll(removedNodes);
	}

	public void updateUntilBoundary_live(Graph graph, Node node) {
		float boundary = graph.intervalOf(node);
		Set<Node> dirtyNodes = new HashSet<>(dirty_live);
		for (Node n : dirty_live)
			dirtyNodes.addAll(getTermDependencies(graph, n));
		for (Node n : new_live)
			dirtyNodes.addAll(getTermDependencies(graph, n));
		if (!has_run_live) {
			LiveVariablesFlowAnalysis.performDataAnalysis(graph, graph.roots(), new_live, dirtyNodes, boundary);
			has_run_live = true;
		} else {
			LiveVariablesFlowAnalysis.updateDataAnalysis(graph, new_live, dirtyNodes, boundary);
		}
		dirty_live.removeIf(n -> graph.intervalOf(n) >= boundary);
		new_live.removeIf(n -> graph.intervalOf(n) >= boundary);
	}

	public void updateUntilBoundary_values(Graph graph, Node node) {
		float boundary = graph.intervalOf(node);
		Set<Node> dirtyNodes = new HashSet<>(dirty_values);
		for (Node n : dirty_values)
			dirtyNodes.addAll(getTermDependencies(graph, n));
		for (Node n : new_values)
			dirtyNodes.addAll(getTermDependencies(graph, n));
		if (!has_run_values) {
			ValueFlowAnalysis.performDataAnalysis(graph, graph.roots(), new_values, dirtyNodes, boundary);
			has_run_values = true;
		} else {
			ValueFlowAnalysis.updateDataAnalysis(graph, new_values, dirtyNodes, boundary);
		}
		dirty_values.removeIf(n -> graph.intervalOf(n) <= boundary);
		new_values.removeIf(n -> graph.intervalOf(n) <= boundary);
	}

	public void updateUntilBoundary_alias(Graph graph, Node node) {
		float boundary = graph.intervalOf(node);
		Set<Node> dirtyNodes = new HashSet<>(dirty_alias);
		for (Node n : dirty_alias)
			dirtyNodes.addAll(getTermDependencies(graph, n));
		for (Node n : new_alias)
			dirtyNodes.addAll(getTermDependencies(graph, n));
		if (!has_run_alias) {
			PointsToFlowAnalysis.performDataAnalysis(graph, graph.roots(), new_alias, dirtyNodes, boundary);
			has_run_alias = true;
		} else {
			PointsToFlowAnalysis.updateDataAnalysis(graph, new_alias, dirtyNodes, boundary);
		}
		dirty_alias.removeIf(n -> graph.intervalOf(n) <= boundary);
		new_alias.removeIf(n -> graph.intervalOf(n) <= boundary);
	}

	public static Set<Node> getTermDependencies(Graph g, Node n) {
		Set<Node> r = new HashSet<>();
		if (TermUtils.isAppl(g.termOf(n), "Int", 1)) {
			r.addAll(g.parentsOf(n));
		}
		for (Node p : g.childrenOf(n)) {
			if (TermUtils.isAppl(g.termOf(p), "Assign", -1)) {
				r.add(p);
			}
		}
		return r;
	}

	public static boolean removeFact(Context context, Node current, CfgNodeId toRemove) {
		boolean removedLiveness = false;
		if (current.getProperty("live") != null) {
			Set<Live> lv = (Set<Live>) current.getProperty("live").lattice.value();
			removedLiveness = lv.removeIf(ll -> ll.origin.contains(toRemove));
		}
		boolean removedConst = false;
		if (current.getProperty("values") != null) {
			HashMap cv = (HashMap) current.getProperty("values").lattice.value();
			Set<Entry> e = cv.entrySet();
			removedConst = e.removeIf(entry -> {
				Lattice l = (Lattice) entry.getValue();
				return l.origin().contains(toRemove);
			});
		}
		if (current.getProperty("alias") != null) {
		}
		return removedLiveness || removedConst;
	}
}
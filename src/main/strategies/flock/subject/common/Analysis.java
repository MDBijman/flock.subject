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
import flock.subject.common.CfgGraph;
import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LivenessValue;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class Analysis {
	public HashSet<CfgNode> dirty_live;
	public HashSet<CfgNode> new_live;
	public boolean is_forward_live;
	public HashSet<CfgNode> dirty_values;
	public HashSet<CfgNode> new_values;
	public boolean is_forward_values;
	public HashSet<CfgNode> dirty_alias;
	public HashSet<CfgNode> new_alias;
	public boolean is_forward_alias;

	public Analysis() {
		dirty_live = new HashSet<CfgNode>();
		new_live = new HashSet<CfgNode>();
		is_forward_live = false;
		dirty_values = new HashSet<CfgNode>();
		new_values = new HashSet<CfgNode>();
		is_forward_values = true;
		dirty_alias = new HashSet<CfgNode>();
		new_alias = new HashSet<CfgNode>();
		is_forward_alias = true;
	}

	public List<Set<CfgNode>> getDirtySets() {
		ArrayList<Set<CfgNode>> dirtySets = new ArrayList<>();
		dirtySets.add(this.dirty_live);
		dirtySets.add(this.dirty_values);
		dirtySets.add(this.dirty_alias);
		return dirtySets;
	}

	public List<Set<CfgNode>> getNewSets() {
		ArrayList<Set<CfgNode>> newSets = new ArrayList<>();
		newSets.add(this.new_live);
		newSets.add(this.new_values);
		newSets.add(this.new_alias);
		return newSets;
	}

	public void addToDirty(CfgNode n) {
		dirty_live.add(n);
		dirty_values.add(n);
		dirty_alias.add(n);
	}

	public void addToNew(CfgNode n) {
		new_live.add(n);
		new_values.add(n);
		new_alias.add(n);
	}

	public void removeFromDirty(Set<CfgNodeId> removedIds) {
		dirty_live.removeIf(n -> removedIds.contains(n.id));
		dirty_values.removeIf(n -> removedIds.contains(n.id));
		dirty_alias.removeIf(n -> removedIds.contains(n.id));
	}

	public void removeFromNew(Set<CfgNodeId> removedIds) {
		new_live.removeIf(n -> removedIds.contains(n.id));
		new_values.removeIf(n -> removedIds.contains(n.id));
		new_alias.removeIf(n -> removedIds.contains(n.id));
	}

	public void updateUntilBoundary_live(HashMap<CfgNodeId, Long> idToInterval, CfgNodeId id) {
		long boundary = idToInterval.get(id);
		Set<CfgNode> dirtyNodes = new HashSet<>(dirty_live);
		for (CfgNode n : dirty_live)
			dirtyNodes.addAll(getTermDependencies(n));
		for (CfgNode n : new_live)
			dirtyNodes.addAll(getTermDependencies(n));
		LiveVariablesFlowAnalysis.updateDataAnalysis(new_live, dirtyNodes, boundary);
		dirty_live.removeIf(n -> idToInterval.get(n.id) >= boundary);
		new_live.removeIf(n -> idToInterval.get(n.id) >= boundary);
	}

	public void updateUntilBoundary_values(HashMap<CfgNodeId, Long> idToInterval, CfgNodeId id) {
		long boundary = idToInterval.get(id);
		Set<CfgNode> dirtyNodes = new HashSet<>(dirty_values);
		for (CfgNode n : dirty_values)
			dirtyNodes.addAll(getTermDependencies(n));
		for (CfgNode n : new_values)
			dirtyNodes.addAll(getTermDependencies(n));
		ValueFlowAnalysis.updateDataAnalysis(new_values, dirtyNodes, boundary);
		dirty_values.removeIf(n -> idToInterval.get(n.id) <= boundary);
		new_values.removeIf(n -> idToInterval.get(n.id) <= boundary);
	}

	public void updateUntilBoundary_alias(HashMap<CfgNodeId, Long> idToInterval, CfgNodeId id) {
		long boundary = idToInterval.get(id);
		Set<CfgNode> dirtyNodes = new HashSet<>(dirty_alias);
		for (CfgNode n : dirty_alias)
			dirtyNodes.addAll(getTermDependencies(n));
		for (CfgNode n : new_alias)
			dirtyNodes.addAll(getTermDependencies(n));
		PointsToFlowAnalysis.updateDataAnalysis(new_alias, dirtyNodes, boundary);
		dirty_alias.removeIf(n -> idToInterval.get(n.id) <= boundary);
		new_alias.removeIf(n -> idToInterval.get(n.id) <= boundary);
	}

	public static Set<CfgNode> getTermDependencies(CfgNode n) {
		Set<CfgNode> r = new HashSet<>();
		if (TermUtils.isAppl(n.term, "Int", 1)) {
			r.addAll(n.parents);
		}
		for (CfgNode p : n.children) {
			if (TermUtils.isAppl(p.term, "Assign", -1)) {
				r.add(p);
			}
		}
		return r;
	}

	public static boolean removeFact(Context context, CfgNode current, CfgNodeId toRemove) {
		boolean removedLiveness = false;
		if (current.getProperty("live") != null) {
			Set<LivenessValue> lv = (Set<LivenessValue>) current.getProperty("live").value;
			assert lv != null;
			removedLiveness = lv.removeIf(ll -> {
				return ll.origin.getId() == toRemove.getId();
			});
		}
		boolean removedConst = false;
		if (current.getProperty("values") != null) {
			HashMap<String, ValueValue> cv = (HashMap<String, ValueValue>) current.getProperty("values").value;
			assert cv != null;
			removedConst = cv.entrySet().removeIf(ll -> {
				return ll.getValue().origin.contains(toRemove);
			});
		}
		if (current.getProperty("alias") != null) {
		}
		return removedLiveness || removedConst;
	}
}
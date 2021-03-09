package org.spoofax;

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
import flock.subject.common.CfgNode;
import flock.subject.common.Graph;
import flock.subject.common.Graph.Node;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LiveValue;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class FlowAnalysis {
	public static void main(String[] args) throws IOException {
		IStrategoTerm ast = new TAFTermReader(new TermFactory()).parseFromFile(args[0]);
		Graph graph = Graph.createCfg(ast);
		performDataAnalysis(graph);
		System.out.println(graph.toGraphviz());
	}

	public static void initNodeValue(Node node) {
		node.addProperty("live", Lattices.MaySet);
		node.getProperty("live").value = node.getProperty("live").lattice.bottom();
	}

	public static void initNodeTransferFunction(Node node) {
		{
			if (true) {
				node.getProperty("live").transfer = TransferFunctions.TransferFunction0;
			}
			if (TermUtils.isAppl(node.term)
					&& (M.appl(node.term).getName().equals("Assign") && node.term.getSubtermCount() == 2)) {
				node.getProperty("live").transfer = TransferFunctions.TransferFunction1;
			}
			if (TermUtils.isAppl(node.term)
					&& (M.appl(node.term).getName().equals("DerefAssign") && node.term.getSubtermCount() == 2)) {
				node.getProperty("live").transfer = TransferFunctions.TransferFunction2;
			}
			if (TermUtils.isAppl(node.term)
					&& (M.appl(node.term).getName().equals("Deref") && node.term.getSubtermCount() == 1)) {
				node.getProperty("live").transfer = TransferFunctions.TransferFunction3;
			}
			if (TermUtils.isAppl(node.term)
					&& (M.appl(node.term).getName().equals("Ref") && node.term.getSubtermCount() == 1)) {
				node.getProperty("live").transfer = TransferFunctions.TransferFunction4;
			}
			if (true) {
				node.getProperty("live").init = TransferFunctions.TransferFunction5;
			}
		}
	}

	public static void performDataAnalysis(Graph g, Node root) {
		HashSet<Node> nodeset = new HashSet<Node>();
		nodeset.add(root);
		performDataAnalysis(g, new HashSet<Node>(), nodeset);
	}

	public static void performDataAnalysis(Graph g, Collection<Node> nodeset) {
		performDataAnalysis(g, new HashSet<Node>(), nodeset);
	}

	public static void performDataAnalysis(Graph g) {
		performDataAnalysis(g, g.roots(), g.nodes());
	}

	public static void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset) {
		performDataAnalysis(g, roots, nodeset, new HashSet<Node>());
	}

	public static void updateDataAnalysis(Graph g, Collection<Node> news, Collection<Node> dirty) {
		performDataAnalysis(g, new HashSet<Node>(), news, dirty);
	}

	public static void updateDataAnalysis(Graph g, Collection<Node> news, Collection<Node> dirty,
			float intervalBoundary) {
		performDataAnalysis(g, new HashSet<Node>(), news, dirty, intervalBoundary);
	}

	public static void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset,
			Collection<Node> dirty) {
		performDataAnalysis(g, roots, nodeset, dirty, -Float.MAX_VALUE);
	}

	public static void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset,
			Collection<Node> dirty, float intervalBoundary) {
		Queue<Node> worklist = new LinkedBlockingQueue<>();
		HashSet<Node> inWorklist = new HashSet<>();
		for (Node node : nodeset) {
			if (node.interval < intervalBoundary)
				continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeValue(node);
			initNodeTransferFunction(node);
		}
		for (Node node : dirty) {
			if (node.interval < intervalBoundary)
				continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeTransferFunction(node);
		}
		for (Node root : roots) {
			if (root.interval < intervalBoundary)
				continue;
			{
				root.getProperty("live").value = root.getProperty("live").init.eval(root);
			}
		}
		while (!worklist.isEmpty()) {
			Node node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval < intervalBoundary)
				continue;
			Object live_n = node.getProperty("live").transfer.eval(node);
			for (Node successor : g.childrenOf(node)) {
				if (successor.interval < intervalBoundary)
					continue;
				boolean changed = false;
				if (changed && !inWorklist.contains(successor)) {
					worklist.add(successor);
					inWorklist.add(successor);
				}
			}
			for (Node successor : g.parentsOf(node)) {
				boolean changed = false;
				if (successor.interval < intervalBoundary)
					continue;
				Object live_o = successor.getProperty("live").value;
				if (node.getProperty("live").lattice.nleq(live_n, live_o)) {
					successor.getProperty("live").value = node.getProperty("live").lattice.lub(live_o, live_n);
					changed = true;
				}
				if (changed && !inWorklist.contains(successor)) {
					worklist.add(successor);
					inWorklist.add(successor);
				}
			}
		}
	}
}

class Lattices {
	public static Lattice MaySet = new MaySetLattice();
	public static Lattice MustSet = new MustSetLattice();
}

class MustSetLattice extends Lattice {
	@Override
	public Object bottom() {
		return new UniversalSet();
	}

	@Override
	public Object top() {
		return new HashSet();
	}

	@Override
	public Object lub(LatticeValue l, LatticeValue r) {
		return SetUtils.intersection(l, r);
	}

	@Override
	public boolean leq(LatticeValue l, LatticeValue r) {
		return SetUtils.isSupersetEquals(l, r);
	}

	@Override
	public Object glb(LatticeValue l, LatticeValue r) {
		return SetUtils.union(l, r);
	}

	@Override
	public boolean geq(LatticeValue l, LatticeValue r) {
		return SetUtils.isSubsetEquals(l, r);
	}
}

class MaySetLattice extends Lattice {
	@Override
	public Object bottom() {
		return new HashSet();
	}

	@Override
	public Object top() {
		return new UniversalSet();
	}

	@Override
	public Object lub(LatticeValue l, LatticeValue r) {
		return SetUtils.union(l, r);
	}

	@Override
	public boolean leq(LatticeValue l, LatticeValue r) {
		return SetUtils.isSubsetEquals(l, r);
	}

	@Override
	public Object glb(LatticeValue l, LatticeValue r) {
		return SetUtils.intersection(l, r);
	}

	@Override
	public boolean geq(LatticeValue l, LatticeValue r) {
		return SetUtils.isSupersetEquals(l, r);
	}
}

class MapLattice extends Lattice {
	Lattice valueLattice;

	MapLattice(Lattice valueLattice) {
		this.valueLattice = valueLattice;
	}

	@Override
	public Object bottom() {
		return new HashMap();
	}

	@Override
	public Object lub(LatticeValue l, LatticeValue r) {
		return MapUtils.union(valueLattice, l, r);
	}
}

class TransferFunctions {
	public static TransferFunction TransferFunction0 = new TransferFunction0();
	public static TransferFunction TransferFunction1 = new TransferFunction1();
	public static TransferFunction TransferFunction2 = new TransferFunction2();
	public static TransferFunction TransferFunction3 = new TransferFunction3();
	public static TransferFunction TransferFunction4 = new TransferFunction4();
	public static TransferFunction TransferFunction5 = new TransferFunction5();
}

class TransferFunction0 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		return UserFunctions.live_f(next_t);
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm e_t = Helpers.at(term, 1);
		return ((Supplier) () -> {
			Set result = new HashSet();
			for (Object m_t : (Set) UserFunctions.live_f(next_t)) {
				if (!n_t.equals(m_t)) {
					result.add(m_t);
				}
			}
			return result;
		}).get();
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		Object previous_t = UserFunctions.live_f(next_t);
		Object current_t = SetUtils.create(new LiveValue(n_t, node.getId()));
		Object result_t = SetUtils.union(previous_t, current_t);
		return result_t;
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		Object previous_t = UserFunctions.live_f(next_t);
		Object current_t = SetUtils.create(new LiveValue(n_t, node.getId()));
		Object result_t = SetUtils.union(previous_t, current_t);
		return result_t;
	}
}

class TransferFunction4 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		Object previous_t = UserFunctions.live_f(next_t);
		Object current_t = SetUtils.create(new LiveValue(n_t, node.getId()));
		Object result_t = SetUtils.union(previous_t, current_t);
		return result_t;
	}
}

class TransferFunction5 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		return SetUtils.create(false);
	}
}

class UserFunctions {
	public static Object live_f(Object o) {
		Node node = (Node) o;
		return node.getProperty("live").value;
	}
}
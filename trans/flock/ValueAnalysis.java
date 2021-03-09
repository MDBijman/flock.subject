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
		node.addProperty("values", Lattices.Environment);
		node.getProperty("values").value = node.getProperty("values").lattice.bottom();
	}

	public static void initNodeTransferFunction(Node node) {
		{
			if (true) {
				node.getProperty("values").transfer = TransferFunctions.TransferFunction0;
			}
			if (TermUtils.isAppl(node.term)
					&& (M.appl(node.term).getName().equals("Assign") && node.term.getSubtermCount() == 2)) {
				node.getProperty("values").transfer = TransferFunctions.TransferFunction1;
			}
			if (TermUtils.isAppl(node.term) && (M.appl(node.term).getName().equals("Assign")
					&& (node.term.getSubtermCount() == 2 && (TermUtils.isAppl(Helpers.at(node.term, 1))
							&& (M.appl(Helpers.at(node.term, 1)).getName().equals("Int")
									&& Helpers.at(node.term, 1).getSubtermCount() == 1))))) {
				node.getProperty("values").transfer = TransferFunctions.TransferFunction2;
			}
			if (true) {
				node.getProperty("values").init = TransferFunctions.TransferFunction3;
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
		performDataAnalysis(g, roots, nodeset, dirty, Float.MAX_VALUE);
	}

	public static void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset,
			Collection<Node> dirty, float intervalBoundary) {
		Queue<Node> worklist = new LinkedBlockingQueue<>();
		HashSet<Node> inWorklist = new HashSet<>();
		for (Node node : nodeset) {
			if (node.interval > intervalBoundary)
				continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeValue(node);
			initNodeTransferFunction(node);
		}
		for (Node node : dirty) {
			if (node.interval > intervalBoundary)
				continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeTransferFunction(node);
		}
		for (Node root : roots) {
			if (root.interval > intervalBoundary)
				continue;
			{
				root.getProperty("values").value = root.getProperty("values").init.eval(root);
			}
		}
		while (!worklist.isEmpty()) {
			Node node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval > intervalBoundary)
				continue;
			Object values_n = node.getProperty("values").transfer.eval(node);
			for (Node successor : g.childrenOf(node)) {
				if (successor.interval > intervalBoundary)
					continue;
				boolean changed = false;
				Object values_o = successor.getProperty("values").value;
				if (node.getProperty("values").lattice.nleq(values_n, values_o)) {
					successor.getProperty("values").value = node.getProperty("values").lattice.lub(values_o, values_n);
					changed = true;
				}
				if (changed && !inWorklist.contains(successor)) {
					worklist.add(successor);
					inWorklist.add(successor);
				}
			}
			for (Node successor : g.parentsOf(node)) {
				boolean changed = false;
				if (successor.interval > intervalBoundary)
					continue;
				if (changed && !inWorklist.contains(successor)) {
					worklist.add(successor);
					inWorklist.add(successor);
				}
			}
		}
	}
}

class ValueLattice extends Lattice {
	@Override
	public LatticeValue bottom() {
		return new StrategoAppl(new StrategoConstructor("Bottom", 0), new IStrategoTerm[] {}, null);
	}

	@Override
	public LatticeValue top() {
		return new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
	}
}

class EnvironmentLattice extends Lattice {
	@Override
	public LatticeValue bottom() {
		return new MapLattice(new ValueLattice()).bottom();
	}

	@Override
	public LatticeValue top() {
		return new MapLattice(new ValueLattice()).top();
	}

	@Override
	public Object lub(LatticeValue l_t, LatticeValue r_t) {
		return new MapLattice(new ValueLattice()).lub(l_t, r_t);
	}
}

class TransferFunctions {
	public static TransferFunction TransferFunction0 = new TransferFunction0();
	public static TransferFunction TransferFunction1 = new TransferFunction1();
	public static TransferFunction TransferFunction2 = new TransferFunction2();
	public static TransferFunction TransferFunction3 = new TransferFunction3();
}

class TransferFunction0 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		return UserFunctions.values_f(prev_t);
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		return new EnvironmentLattice().lub(((Supplier) () -> {
			Map result = new HashMap();
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.values_f(prev_t)).entrySet()) {
				Object k_t = e.getKey();
				Object v_t = e.getValue();
				if (!k_t.equals(n_t)) {
					result.put(k_t, v_t);
				}
			}
			return result;
		}).get(), MapUtils.create(((IStrategoString) n_t).stringValue(), new ValueValue(
				new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null), node.getId())));
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm i_t = Helpers.at(Helpers.at(term, 1), 0);
		return new EnvironmentLattice().lub(((Supplier) () -> {
			Map result = new HashMap();
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.values_f(prev_t)).entrySet()) {
				Object k_t = e.getKey();
				Object v_t = e.getValue();
				if (!k_t.equals(n_t)) {
					result.put(k_t, v_t);
				}
			}
			return result;
		}).get(),
				MapUtils.create(((IStrategoString) n_t).stringValue(),
						new ValueValue(new StrategoAppl(new StrategoConstructor("Const", 1),
								new IStrategoTerm[] { Helpers.toTerm(i_t) }, null), node.getId())));
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public Object eval(Node node) {
		IStrategoTerm term = node.term;
		return MapUtils.create();
	}
}

class UserFunctions {
	public static Object values_f(Object o) {
		Node node = (Node) o;
		return node.getProperty("values").value;
	}
}
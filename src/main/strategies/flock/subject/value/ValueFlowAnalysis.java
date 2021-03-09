package flock.subject.value;

import org.apache.commons.lang3.tuple.Pair;
import org.strategoxt.lang.Context;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
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
import flock.subject.common.Lattice.MapLattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LiveValue;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.strategies.Program;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class ValueFlowAnalysis {
	public static void main(String[] args) throws IOException {
		IStrategoTerm ast = new TAFTermReader(new TermFactory()).parseFromFile(args[0]);
		Graph graph = Graph.createCfg(ast);
		performDataAnalysis(graph);
		System.out.println(graph.toGraphviz());
	}

	public static void initNodeValue(Node node) {
		node.addProperty("values", EnvironmentLattice.bottom());
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
				root.getProperty("values").lattice = root.getProperty("values").init.eval(root);
			}
		}
		while (!worklist.isEmpty()) {
			Node node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval > intervalBoundary)
				continue;
			Lattice values_n = node.getProperty("values").transfer.eval(node);
			for (Node successor : g.childrenOf(node)) {
				if (successor.interval > intervalBoundary)
					continue;
				boolean changed = false;
				Lattice values_o = successor.getProperty("values").lattice;
				if (values_n.nleq(values_o)) {
					successor.getProperty("values").lattice = values_o.lub(values_n);
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
	public ValueValue value;
	
	ValueLattice(IStrategoTerm v, Set<CfgNodeId> origin) {
		this.value = new ValueValue(v, origin);
	}
	
	static ValueLattice bottom() {
		return new ValueLattice(new StrategoAppl(new StrategoConstructor("Bottom", 0), new IStrategoTerm[] {}, null), null);
	}

	static ValueLattice top() {
		return new ValueLattice(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null), null);
	}
	
	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public Object value() {
		return this.value.value;
	}
	
	@Override
	public Set<CfgNodeId> origin() {
		return this.value.origin;
	}
	
	@Override
	public Lattice lub(Lattice right) {
		HashSet<CfgNodeId> origins = new HashSet<>();
		origins.addAll(this.origin());
		origins.addAll(((ValueLattice)right).origin());
		IStrategoTerm term12976 = Helpers
				.toTerm(new StrategoTuple(new IStrategoTerm[] { Helpers.toTerm(this.value()), Helpers.toTerm(right.value()) }, null));
		if (TermUtils.isTuple(term12976) && (TermUtils.isAppl(Helpers.at(term12976, 0))
				&& (M.appl(Helpers.at(term12976, 0)).getName().equals("Top")
						&& Helpers.at(term12976, 0).getSubtermCount() == 0))) {
			return new ValueLattice(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null),
					origins);
		}
		if (TermUtils.isTuple(term12976) && (TermUtils.isAppl(Helpers.at(term12976, 1))
				&& (M.appl(Helpers.at(term12976, 1)).getName().equals("Top")
						&& Helpers.at(term12976, 1).getSubtermCount() == 0))) {
			return new ValueLattice(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null),
					origins);
		}
		if (TermUtils.isTuple(term12976) && (TermUtils.isAppl(Helpers.at(term12976, 0)) && (M
				.appl(Helpers.at(term12976, 0)).getName().equals("Const")
				&& (Helpers.at(term12976, 0).getSubtermCount() == 1 && (TermUtils.isAppl(Helpers.at(term12976, 1))
						&& (M.appl(Helpers.at(term12976, 1)).getName().equals("Const")
								&& Helpers.at(term12976, 1).getSubtermCount() == 1)))))) {
			IStrategoTerm i_t = Helpers.at(Helpers.at(term12976, 0), 0);
			IStrategoTerm j_t = Helpers.at(Helpers.at(term12976, 1), 0);
			return (boolean) i_t.equals(j_t)
					? new ValueLattice(new StrategoAppl(new StrategoConstructor("Const", 1),
							new IStrategoTerm[] { Helpers.toTerm(i_t) }, null), origins)
					: new ValueLattice(
							new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null),
							origins);
		}
		if (TermUtils.isTuple(term12976) && (TermUtils.isAppl(Helpers.at(term12976, 1))
				&& (M.appl(Helpers.at(term12976, 1)).getName().equals("Bottom")
						&& Helpers.at(term12976, 1).getSubtermCount() == 0))) {
			return new ValueLattice((IStrategoTerm)this.value(), origins);
		}
		if (TermUtils.isTuple(term12976) && (TermUtils.isAppl(Helpers.at(term12976, 0))
				&& (M.appl(Helpers.at(term12976, 0)).getName().equals("Bottom")
						&& Helpers.at(term12976, 0).getSubtermCount() == 0))) {
			return new ValueLattice((IStrategoTerm)right.value(), origins);
		}
		throw new RuntimeException("Could not match term '" + term12976 + "'.");
	}
}

class EnvironmentLattice extends Lattice {
	MapLattice value;
	
	EnvironmentLattice(MapLattice v) {
		this.value = v;
	}
	
	static EnvironmentLattice bottom() {
		return new EnvironmentLattice(MapLattice.bottom());
	}

	@Override
	public Lattice lub(Lattice o) {
		return new EnvironmentLattice((MapLattice) value.lub((((EnvironmentLattice) o).value)));
	}

	@Override
	public Object value() {
		return this.value.value();
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
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		return new EnvironmentLattice(new MapLattice((Map)UserFunctions.values_f(prev_t)));
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		Map result = new HashMap();
		for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.values_f(prev_t)).entrySet()) {
			Object k_t = e.getKey();
			Object v_t = e.getValue();
			if (!k_t.equals(n_t)) {
				result.put(k_t, v_t);
			}
		}
		Lattice lhs = new EnvironmentLattice(new MapLattice(result));
		HashSet<CfgNodeId> origin = new HashSet<>();
		origin.add(node.getId());
		Lattice rhs = new EnvironmentLattice(new MapLattice(MapUtils.create(((IStrategoString) n_t).stringValue(), new ValueLattice(
				new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null), origin))));
		return lhs.lub(rhs);
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm i_t = Helpers.at(Helpers.at(term, 1), 0);
		Map result = new HashMap();
		for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.values_f(prev_t)).entrySet()) {
			Object k_t = e.getKey();
			Object v_t = e.getValue();
			if (!k_t.equals(n_t)) {
				result.put(k_t, v_t);
			}
		}
		Lattice lhs = new EnvironmentLattice(new MapLattice(result));
		HashSet<CfgNodeId> origin = new HashSet<>();
		origin.add(node.getId());
		Lattice rhs = new EnvironmentLattice(new MapLattice(MapUtils.create(((IStrategoString) n_t).stringValue(),
				new ValueLattice(new StrategoAppl(new StrategoConstructor("Const", 1),
						new IStrategoTerm[] { Helpers.toTerm(i_t) }, null), origin))));
		return lhs.lub(rhs);
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		return EnvironmentLattice.bottom();
	}
}

class UserFunctions {
	public static Object values_f(Object o) {
		Node node = (Node) o;
		return node.getProperty("values").lattice.value();
	}
}
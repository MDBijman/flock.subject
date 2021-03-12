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
import flock.subject.common.Graph;
import flock.subject.common.Graph.Node;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.Lattice.MaySet;
import flock.subject.common.Lattice.MustSet;
import flock.subject.common.Lattice.MapLattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.Live;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ConstProp;

public class ValueFlowAnalysis {
	public static void main(String[] args) throws IOException {
		IStrategoTerm ast = new TAFTermReader(new TermFactory()).parseFromFile(args[0]);
		Graph graph = Graph.createCfg(ast);
		performDataAnalysis(graph);
		System.out.println(graph.toGraphviz());
	}

	public static void initNodeValue(Node node) {
		node.addProperty("values", Environment.bottom());
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
			root.getProperty("values").lattice = root.getProperty("values").init.eval(root);
		}
		for (Node node : nodeset) {
			if (node.interval > intervalBoundary)
				continue;
			{
				Lattice init = node.getProperty("values").lattice;
				for (Node pred : g.parentsOf(node)) {
					Lattice live_o = pred.getProperty("values").transfer.eval(pred);
					init = init.lub(live_o);
				}
				node.getProperty("values").lattice = init;
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

class Value extends Lattice {
	ConstProp value;

	public Value(ConstProp v) {
		this.value = v;
	}

	@Override
	public Object value() {
		return this.value.value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public Set<CfgNodeId> origin() {
		return this.value.origin;
	}

	public static Value bottom() {
		return new Value(
				new ConstProp(new StrategoAppl(new StrategoConstructor("Bottom", 0), new IStrategoTerm[] {}, null)));
	}

	public static Value top() {
		return new Value(
				new ConstProp(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null)));
	}

	@Override
	public Lattice lub(Lattice other) {
		Lattice l = this;
		Lattice r = other;
		IStrategoTerm l_t = (IStrategoTerm) l.value();
		IStrategoTerm r_t = (IStrategoTerm) r.value();
		IStrategoTerm term12 = Helpers
				.toTerm(new StrategoTuple(new IStrategoTerm[] { Helpers.toTerm(l_t), Helpers.toTerm(r_t) }, null));
		Lattice result16 = null;
		if (TermUtils.isTuple(term12)
				&& (TermUtils.isAppl(Helpers.at(term12, 0)) && (M.appl(Helpers.at(term12, 0)).getName().equals("Top")
						&& Helpers.at(term12, 0).getSubtermCount() == 0))) {
			result16 = new Value(
					new ConstProp(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null))
							.withOrigin(l.origin()).withOrigin(r.origin()));
		}
		if (TermUtils.isTuple(term12)
				&& (TermUtils.isAppl(Helpers.at(term12, 1)) && (M.appl(Helpers.at(term12, 1)).getName().equals("Top")
						&& Helpers.at(term12, 1).getSubtermCount() == 0))) {
			result16 = new Value(
					new ConstProp(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null))
							.withOrigin(l.origin()).withOrigin(r.origin()));
		}
		if (TermUtils.isTuple(term12)
				&& (TermUtils.isAppl(Helpers.at(term12, 0)) && (M.appl(Helpers.at(term12, 0)).getName().equals("Const")
						&& (Helpers.at(term12, 0).getSubtermCount() == 1 && (TermUtils.isAppl(Helpers.at(term12, 1))
								&& (M.appl(Helpers.at(term12, 1)).getName().equals("Const")
										&& Helpers.at(term12, 1).getSubtermCount() == 1)))))) {
			IStrategoTerm i_t = Helpers.at(Helpers.at(term12, 0), 0);
			IStrategoTerm j_t = Helpers.at(Helpers.at(term12, 1), 0);
			result16 = (boolean) i_t.equals(j_t)
					? new Value(new ConstProp(new StrategoAppl(new StrategoConstructor("Const", 1),
							new IStrategoTerm[] { Helpers.toTerm(i_t) }, null)).withOrigin(l.origin())
									.withOrigin(r.origin()))
					: new Value(new ConstProp(
							new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null))
									.withOrigin(l.origin()).withOrigin(r.origin()));
		}
		if (TermUtils.isTuple(term12)
				&& (TermUtils.isAppl(Helpers.at(term12, 1)) && (M.appl(Helpers.at(term12, 1)).getName().equals("Bottom")
						&& Helpers.at(term12, 1).getSubtermCount() == 0))) {
			result16 = new Value(new ConstProp(l_t).withOrigin(l.origin()).withOrigin(r.origin()));
		}
		if (TermUtils.isTuple(term12)
				&& (TermUtils.isAppl(Helpers.at(term12, 0)) && (M.appl(Helpers.at(term12, 0)).getName().equals("Bottom")
						&& Helpers.at(term12, 0).getSubtermCount() == 0))) {
			result16 = new Value(new ConstProp(r_t).withOrigin(l.origin()).withOrigin(r.origin()));
		}
		if (result16 == null) {
			throw new RuntimeException("Could not match term");
		}
		return result16;
	}
}

class Environment extends MapLattice {
	public Environment(MapLattice v) {
		super(v);
	}

	public static Environment bottom() {
		return new Environment(MapLattice.bottom());
	}

	public static Environment top() {
		return new Environment(MapLattice.top());
	}

	@Override
	public Lattice lub(Lattice other) {
		Lattice l = this;
		Lattice r = other;
		MapLattice l_t = (MapLattice) l;
		MapLattice r_t = (MapLattice) r;
		return new Environment((MapLattice) new MapLattice(l_t).lub(r_t));
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
		return UserFunctions.values_f(prev_t);
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm t_t = term;
		IStrategoTerm n_t = Helpers.at(term, 0);
		Map result17 = new HashMap();
		for (Map.Entry<Object, Object> entry : new MapLattice(UserFunctions.values_f(prev_t))) {
			Object k_t = entry.getKey();
			Object v_t = entry.getValue();
			if (!k_t.equals(n_t)) {
				result17.put(k_t, v_t);
			}
		}
		MapLattice prevmap_t = new MapLattice(result17);
		Environment previous_t = (Environment) new Environment(prevmap_t);
		MapLattice newmap_t = (MapLattice) new MapLattice(MapUtils.create(((IStrategoString) n_t).stringValue(),
				new Value(
						new ConstProp(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null))
								.withOrigin(Helpers.getTermPosition(t_t)))));
		Environment new_t = (Environment) new Environment(newmap_t);
		Environment result_t = (Environment) new Environment(previous_t).lub(new_t);
		return result_t;
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm t_t = term;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm i_t = Helpers.at(Helpers.at(term, 1), 0);
		Map result18 = new HashMap();
		for (Map.Entry<Object, Object> entry : new MapLattice(UserFunctions.values_f(prev_t))) {
			Object k_t = entry.getKey();
			Object v_t = entry.getValue();
			if (!k_t.equals(n_t)) {
				result18.put(k_t, v_t);
			}
		}
		MapLattice prevmap_t = new MapLattice(result18);
		Environment previous_t = (Environment) new Environment(prevmap_t);
		MapLattice newmap_t = (MapLattice) new MapLattice(
				MapUtils.create(((IStrategoString) n_t).stringValue(),
						new Value(new ConstProp(new StrategoAppl(new StrategoConstructor("Const", 1),
								new IStrategoTerm[] { Helpers.toTerm(i_t) }, null))
										.withOrigin(Helpers.getTermPosition(t_t)))));
		Environment new_t = (Environment) new Environment(newmap_t);
		Environment result_t = (Environment) new Environment(previous_t).lub(new_t);
		return result_t;
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		return Environment.bottom();
	}
}

class UserFunctions {
	public static Lattice values_f(Object o) {
		Node node = (Node) o;
		return node.getProperty("values").lattice;
	}
}
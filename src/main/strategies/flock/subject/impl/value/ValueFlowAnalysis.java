package flock.subject.impl.value;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoTuple;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;

import flock.subject.common.Analysis;
import flock.subject.common.FlockLattice;
import flock.subject.common.FlockLattice.FlockCollectionLattice;
import flock.subject.common.FlockLattice.FlockValueLattice;
import flock.subject.common.FlockLattice.MapLattice;
import flock.subject.common.FlockValue;
import flock.subject.common.FlockValue.FlockValueWithDependencies;
import flock.subject.common.Graph;
import flock.subject.common.Graph.Node;
import flock.subject.impl.value.ValueFlowAnalysisProperties.ConstProp;
import flock.subject.common.Helpers;
import flock.subject.common.MapUtils;
import flock.subject.common.TransferFunction;

public class ValueFlowAnalysis extends Analysis {
	public ValueFlowAnalysis() {
		super("values", Direction.FORWARD);
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

	@Override
	public void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset, Collection<Node> dirty,
			float intervalBoundary) {
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
			this.changedNodes.add(root);
		}
		for (Node node : nodeset) {
			if (node.interval > intervalBoundary)
				continue;
			{
				FlockLattice init = node.getProperty("values").lattice;
				for (Node pred : g.parentsOf(node)) {
					FlockLattice live_o = pred.getProperty("values").transfer.eval(pred);
					init = init.lub(live_o);
				}
				node.getProperty("values").lattice = init;
				this.changedNodes.add(node);
			}
		}
		while (!worklist.isEmpty()) {
			Node node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval > intervalBoundary)
				continue;
			FlockLattice values_n = node.getProperty("values").transfer.eval(node);
			for (Node successor : g.childrenOf(node)) {
				if (successor.interval > intervalBoundary)
					continue;
				boolean changed = false;
				FlockLattice values_o = successor.getProperty("values").lattice;
				if (values_n.nleq(values_o)) {
					successor.getProperty("values").lattice = values_o.lub(values_n);
					changed = true;
				}
				if (changed && !inWorklist.contains(successor)) {
					worklist.add(successor);
					inWorklist.add(successor);
				}
				if (changed) {
					this.changedNodes.add(successor);
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
				if (changed) {
					this.changedNodes.add(successor);
				}
			}
		}
	}
}

class Value implements FlockValueLattice {
	ConstProp value;

	public Value(ConstProp v) {
		this.value = v;
	}

	@Override
	public FlockValue value() {
		return this.value;
	}

	@Override
	public String toString() {
		return value.toString();
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
	public FlockLattice lub(FlockLattice other) {
		FlockValueLattice l = this;
		FlockValueLattice r = (FlockValueLattice) other;
		FlockValueWithDependencies l_v = (FlockValueWithDependencies) l.value();
		FlockValueWithDependencies r_v = (FlockValueWithDependencies) r.value();
		IStrategoTerm l_t = (IStrategoTerm) l_v.toTerm();
		IStrategoTerm r_t = (IStrategoTerm) r_v.toTerm();
		IStrategoTerm term0 = Helpers
				.toTerm(new StrategoTuple(new IStrategoTerm[] { Helpers.toTerm(l_t), Helpers.toTerm(r_t) }, null));
		FlockLattice result0 = null;
		if (TermUtils.isTuple(term0)
				&& (TermUtils.isAppl(Helpers.at(term0, 0)) && (M.appl(Helpers.at(term0, 0)).getName().equals("Top")
						&& Helpers.at(term0, 0).getSubtermCount() == 0))) {
			result0 = new Value(
					new ConstProp(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null))
							.withOrigin(l_v.dependencies()).withOrigin(r_v.dependencies()));
		}
		if (TermUtils.isTuple(term0)
				&& (TermUtils.isAppl(Helpers.at(term0, 1)) && (M.appl(Helpers.at(term0, 1)).getName().equals("Top")
						&& Helpers.at(term0, 1).getSubtermCount() == 0))) {
			result0 = new Value(
					new ConstProp(new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null))
							.withOrigin(l_v.dependencies()).withOrigin(r_v.dependencies()));
		}
		if (TermUtils.isTuple(term0)
				&& (TermUtils.isAppl(Helpers.at(term0, 0)) && (M.appl(Helpers.at(term0, 0)).getName().equals("Const")
						&& (Helpers.at(term0, 0).getSubtermCount() == 1 && (TermUtils.isAppl(Helpers.at(term0, 1))
								&& (M.appl(Helpers.at(term0, 1)).getName().equals("Const")
										&& Helpers.at(term0, 1).getSubtermCount() == 1)))))) {
			IStrategoTerm i_t = Helpers.at(Helpers.at(term0, 0), 0);
			IStrategoTerm j_t = Helpers.at(Helpers.at(term0, 1), 0);
			result0 = (boolean) i_t.equals(j_t)
					? new Value(new ConstProp(new StrategoAppl(new StrategoConstructor("Const", 1),
							new IStrategoTerm[] { Helpers.toTerm(i_t) }, null)).withOrigin(l_v.dependencies())
									.withOrigin(r_v.dependencies()))
					: new Value(new ConstProp(
							new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null))
									.withOrigin(l_v.dependencies()).withOrigin(r_v.dependencies()));
		}
		if (TermUtils.isTuple(term0)
				&& (TermUtils.isAppl(Helpers.at(term0, 1)) && (M.appl(Helpers.at(term0, 1)).getName().equals("Bottom")
						&& Helpers.at(term0, 1).getSubtermCount() == 0))) {
			result0 = new Value(new ConstProp(l_t).withOrigin(l_v.dependencies()).withOrigin(r_v.dependencies()));
		}
		if (TermUtils.isTuple(term0)
				&& (TermUtils.isAppl(Helpers.at(term0, 0)) && (M.appl(Helpers.at(term0, 0)).getName().equals("Bottom")
						&& Helpers.at(term0, 0).getSubtermCount() == 0))) {
			result0 = new Value(new ConstProp(r_t).withOrigin(l_v.dependencies()).withOrigin(r_v.dependencies()));
		}
		if (result0 == null) {
			throw new RuntimeException("Could not match term");
		}
		return result0;
	}
}

class Environment extends MapLattice implements FlockCollectionLattice {
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
	public FlockLattice lub(FlockLattice other) {
		FlockCollectionLattice l = this;
		FlockCollectionLattice r = (FlockCollectionLattice) other;
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
	public FlockLattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		return UserFunctions.values_f(prev_t);
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public FlockLattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm t_t = term;
		IStrategoTerm n_t = Helpers.at(term, 0);
		Map result1 = new HashMap();
		for (Object o : new MapLattice(UserFunctions.values_f(prev_t))) {
			Entry entry = (Entry) o;
			Object k_t = entry.getKey();
			Object v_t = entry.getValue();
			if (!k_t.equals(n_t)) {
				result1.put(k_t, v_t);
			}
		}
		MapLattice prevmap_t = new MapLattice(result1);
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
	public FlockLattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm t_t = term;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm i_t = Helpers.at(Helpers.at(term, 1), 0);
		Map result2 = new HashMap();
		for (Object o : new MapLattice(UserFunctions.values_f(prev_t))) {
			Entry entry = (Entry) o;
			Object k_t = entry.getKey();
			Object v_t = entry.getValue();
			if (!k_t.equals(n_t)) {
				result2.put(k_t, v_t);
			}
		}
		MapLattice prevmap_t = new MapLattice(result2);
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
	public FlockLattice eval(Node node) {
		IStrategoTerm term = node.term;
		return Environment.bottom();
	}
}

class UserFunctions {
	public static FlockLattice values_f(Object o) {
		Node node = (Node) o;
		return node.getProperty("values").lattice;
	}
}
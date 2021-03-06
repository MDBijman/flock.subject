package flock.subject.impl.alias;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoTuple;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;

import flock.subject.common.Analysis;
import flock.subject.common.FlockLattice;
import flock.subject.common.FlockLattice.MapLattice;
import flock.subject.common.Graph;
import flock.subject.common.Graph.Node;
import flock.subject.common.Helpers;
import flock.subject.common.MapUtils;
import flock.subject.common.TransferFunction;

public class PointsToFlowAnalysis extends Analysis {
	public PointsToFlowAnalysis() {
		super("alias", "locations", Direction.FORWARD);
	}

	public static void initNodeValue(Node node) {
		node.addProperty("locations", LocationMapLattice.bottom());
	}

	public static void initNodeTransferFunction(Node node) {
		{
			if (true) {
				node.getProperty("locations").transfer = TransferFunctions.TransferFunction0;
			}
			if (TermUtils.isAppl(node.term) && (M.appl(node.term).getName().equals("Assign")
					&& (node.term.getSubtermCount() == 2 && (TermUtils.isAppl(Helpers.at(node.term, 1))
							&& (M.appl(Helpers.at(node.term, 1)).getName().equals("Ref")
									&& Helpers.at(node.term, 1).getSubtermCount() == 1))))) {
				node.getProperty("locations").transfer = TransferFunctions.TransferFunction1;
			}
			if (TermUtils.isAppl(node.term) && (M.appl(node.term).getName().equals("Assign")
					&& (node.term.getSubtermCount() == 2 && (TermUtils.isAppl(Helpers.at(node.term, 1))
							&& (M.appl(Helpers.at(node.term, 1)).getName().equals("Malloc")
									&& Helpers.at(node.term, 1).getSubtermCount() == 0))))) {
				node.getProperty("locations").transfer = TransferFunctions.TransferFunction2;
			}
			if (true) {
				node.getProperty("locations").init = TransferFunctions.TransferFunction3;
			}
		}
	}

	public void performDataAnalysis(Graph g, Node root) {
		HashSet<Node> nodeset = new HashSet<Node>();
		nodeset.add(root);
		performDataAnalysis(g, new HashSet<Node>(), nodeset);
	}

	public void performDataAnalysis(Graph g, Collection<Node> nodeset) {
		performDataAnalysis(g, new HashSet<Node>(), nodeset);
	}

	public void performDataAnalysis(Graph g) {
		performDataAnalysis(g, g.roots(), g.nodes());
	}

	public void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset) {
		performDataAnalysis(g, roots, nodeset, new HashSet<Node>());
	}

	public void updateDataAnalysis(Graph g, Collection<Node> news, Collection<Node> dirty) {
		performDataAnalysis(g, new HashSet<Node>(), news, dirty);
	}

	@Override
	public void updateDataAnalysis(Graph g, Collection<Node> news, Collection<Node> dirty, float intervalBoundary) {
		performDataAnalysis(g, new HashSet<Node>(), news, dirty, intervalBoundary);
	}

	public void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset, Collection<Node> dirty) {
		performDataAnalysis(g, roots, nodeset, dirty, Long.MAX_VALUE);
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
			{
				root.getProperty("locations").lattice = root.getProperty("locations").init.eval(root);
			}
		}
		while (!worklist.isEmpty()) {
			Node node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval > intervalBoundary)
				continue;
			FlockLattice locations_n = node.getProperty("locations").transfer.eval(node);
			for (Node successor : g.childrenOf(node)) {
				if (successor.interval > intervalBoundary)
					continue;
				boolean changed = false;
				FlockLattice locations_o = successor.getProperty("locations").lattice;
				if (locations_n.nleq(locations_o)) {
					successor.getProperty("locations").lattice = locations_o.lub(locations_n);
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

class LocationLattice implements FlockLattice {
	IStrategoTerm value;

	LocationLattice(IStrategoTerm v) {
		this.value = v;
	}

	@Override
	public Object value() {
		return this.value;
	}

	@Override
	public FlockLattice lub(FlockLattice o) {
		return new LocationLattice((IStrategoTerm) ((Supplier) () -> {
			IStrategoTerm term0 = Helpers.toTerm(new StrategoTuple(
					new IStrategoTerm[] { Helpers.toTerm(this.value()), Helpers.toTerm(o.value()) }, null));
			if (TermUtils.isTuple(term0)
					&& (TermUtils.isAppl(Helpers.at(term0, 0)) && (M.appl(Helpers.at(term0, 0)).getName().equals("Top")
							&& Helpers.at(term0, 0).getSubtermCount() == 0))) {
				return new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
			}
			if (TermUtils.isTuple(term0)
					&& (TermUtils.isAppl(Helpers.at(term0, 1)) && (M.appl(Helpers.at(term0, 1)).getName().equals("Top")
							&& Helpers.at(term0, 1).getSubtermCount() == 0))) {
				return new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
			}
			if (TermUtils.isTuple(term0)
					&& (TermUtils.isAppl(Helpers.at(term0, 0)) && (M.appl(Helpers.at(term0, 0)).getName().equals("Loc")
							&& (Helpers.at(term0, 0).getSubtermCount() == 1 && (TermUtils.isAppl(Helpers.at(term0, 1))
									&& (M.appl(Helpers.at(term0, 1)).getName().equals("Loc")
											&& Helpers.at(term0, 1).getSubtermCount() == 1)))))) {
				IStrategoTerm i_t = Helpers.at(Helpers.at(term0, 0), 0);
				IStrategoTerm j_t = Helpers.at(Helpers.at(term0, 1), 0);
				return (boolean) i_t.equals(j_t)
						? new StrategoAppl(new StrategoConstructor("Loc", 1),
								new IStrategoTerm[] { Helpers.toTerm(i_t) }, null)
						: new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
			}
			if (TermUtils.isTuple(term0) && (TermUtils.isAppl(Helpers.at(term0, 1))
					&& (M.appl(Helpers.at(term0, 1)).getName().equals("Bottom")
							&& Helpers.at(term0, 1).getSubtermCount() == 0))) {
				return this.value();
			}
			if (TermUtils.isTuple(term0) && (TermUtils.isAppl(Helpers.at(term0, 0))
					&& (M.appl(Helpers.at(term0, 0)).getName().equals("Bottom")
							&& Helpers.at(term0, 0).getSubtermCount() == 0))) {
				return o.value();
			}
			throw new RuntimeException("Could not match term '" + term0 + "'.");
		}).get());
	}

}

class LocationMapLattice implements FlockLattice {
	MapLattice value;

	LocationMapLattice(MapLattice m) {
		this.value = m;
	}

	static public LocationMapLattice bottom() {
		return new LocationMapLattice(MapLattice.bottom());
	}

	@Override
	public FlockLattice lub(FlockLattice o) {
		return this.value.lub((MapLattice) o.value());
	}

	@Override
	public Object value() {
		return this.value;
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
		return new LocationMapLattice(new MapLattice((Map) UserFunctions.locations_f(prev_t)));
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public FlockLattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm m_t = Helpers.at(Helpers.at(term, 1), 0);
		Map result_l = new HashMap();
		for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.locations_f(prev_t)).entrySet()) {
			Object k_t = e.getKey();
			Object v_t = e.getValue();
			if (!k_t.equals(n_t)) {
				result_l.put(k_t, v_t);
			}
		}
		Map result_r = new HashMap();
		for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.locations_f(prev_t)).entrySet()) {
			Object k_t = e.getKey();
			Object v_t = e.getValue();
			if (k_t.equals(m_t)) {
				result_r.put(n_t, v_t);
			}
		}
		LocationMapLattice l = new LocationMapLattice(new MapLattice(result_l));
		LocationMapLattice r = new LocationMapLattice(new MapLattice(result_r));
		return l.lub(r);
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public FlockLattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		Map result = new HashMap();
		for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.locations_f(prev_t)).entrySet()) {
			Object k_t = e.getKey();
			Object v_t = e.getValue();
			if (!k_t.equals(n_t)) {
				result.put(k_t, v_t);
			}
		}
		LocationMapLattice l = new LocationMapLattice(new MapLattice(result));
		LocationMapLattice r = new LocationMapLattice(new MapLattice(MapUtils.create(n_t,
				new StrategoAppl(new StrategoConstructor("Loc", 1),
						new IStrategoTerm[] { Helpers.toTerm(prev_t.getProperty("position").lattice.value()) },
						null))));
		return l.lub(r);
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public FlockLattice eval(Node node) {
		IStrategoTerm term = node.term;
		return new LocationMapLattice(new MapLattice(MapUtils.create()));
	}
}

class UserFunctions {
	public static Object locations_f(Object o) {
		Node node = (Node) o;
		return node.getProperty("locations").lattice.value();
	}
}
package flock.subject.alias;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.TermFactory;
import java.io.IOException;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
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
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;

public class PointsToFlowAnalysis {
	public static void main(String[] args) throws IOException {
		IStrategoTerm ast = new TAFTermReader(new TermFactory()).parseFromFile(args[0]);
		CfgGraph graph = CfgGraph.createControlFlowGraph(ast);
		performDataAnalysis(graph);
		System.out.println(graph.toGraphviz());
	}
	public static void initNodeValue(CfgNode node) {
		node.addProperty("locations", Lattices.LocationMap);
		node.getProperty("locations").value = node.getProperty("locations").lattice.bottom();
	}
	public static void initNodeTransferFunction(CfgNode node) {
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

	public static void performDataAnalysis(CfgNode root) {
		HashSet<CfgNode> nodeset = new HashSet<CfgNode>();
		nodeset.add(root);
		performDataAnalysis(new HashSet<CfgNode>(), nodeset);
	}
	
	public static void performDataAnalysis(Set<CfgNode> nodeset) {
		performDataAnalysis(new HashSet<CfgNode>(), nodeset);
	}
	
	public static void performDataAnalysis(CfgGraph graph) {
		performDataAnalysis(graph.roots, graph.flatten());
	}
	
	public static void performDataAnalysis(Set<CfgNode> roots, Set<CfgNode> nodeset) {
		performDataAnalysis(roots, nodeset, new HashSet<CfgNode>());
	}

	public static void updateDataAnalysis(Set<CfgNode> news, Set<CfgNode> dirty) {
		performDataAnalysis(new HashSet<CfgNode>(), news, dirty);
	}

	public static void updateDataAnalysis(Set<CfgNode> news, Set<CfgNode> dirty, long intervalBoundary) {
		performDataAnalysis(new HashSet<CfgNode>(), news, dirty, intervalBoundary);
	}
	
	public static void performDataAnalysis(Set<CfgNode> roots, Set<CfgNode> nodeset, Set<CfgNode> dirty) {
		performDataAnalysis(roots, nodeset, dirty, Long.MAX_VALUE);
	}
	
	public static void performDataAnalysis(Set<CfgNode> roots, Set<CfgNode> nodeset, Set<CfgNode> dirty, long intervalBoundary) {
		Queue<CfgNode> worklist = new LinkedBlockingQueue<>();
		HashSet<CfgNode> inWorklist = new HashSet<>();
		for (CfgNode node : nodeset) {
			if (node.interval > intervalBoundary) continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeValue(node);
			initNodeTransferFunction(node);
		}
		for (CfgNode node : dirty) {
			if (node.interval > intervalBoundary) continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeTransferFunction(node);
		}
		for (CfgNode root : roots) {
			root.getProperty("locations").value = root.getProperty("locations").init.eval(root);
		}
		while (!worklist.isEmpty()) {
			CfgNode node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval > intervalBoundary) continue;
			Object locations_n = node.getProperty("locations").transfer.eval(node);
			for (CfgNode successor : node.children) {
				if (successor.interval > intervalBoundary) continue;
				boolean changed = false;
				Object locations_o = successor.getProperty("locations").value;
				if (node.getProperty("locations").lattice.nleq(locations_n, locations_o)) {
					successor.getProperty("locations").value = node.getProperty("locations").lattice.lub(locations_o,
							locations_n);
					changed = true;
				}
				if (changed && !inWorklist.contains(successor)) {
					worklist.add(successor);
					inWorklist.add(successor);
				}
			}
			for (CfgNode successor : node.parents) {
				if (successor.interval > intervalBoundary) continue;
				boolean changed = false;
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
	public static Lattice Location = new LocationLattice();
	public static Lattice LocationMap = new LocationMapLattice();
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
	public Object lub(Object l, Object r) {
		return SetUtils.intersection(l, r);
	}

	@Override
	public boolean leq(Object l, Object r) {
		return SetUtils.isSupersetEquals(l, r);
	}

	@Override
	public Object glb(Object l, Object r) {
		return SetUtils.union(l, r);
	}

	@Override
	public boolean geq(Object l, Object r) {
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
	public Object lub(Object l, Object r) {
		return SetUtils.union(l, r);
	}

	@Override
	public boolean leq(Object l, Object r) {
		return SetUtils.isSubsetEquals(l, r);
	}

	@Override
	public Object glb(Object l, Object r) {
		return SetUtils.intersection(l, r);
	}

	@Override
	public boolean geq(Object l, Object r) {
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
	public Object lub(Object l, Object r) {
		return MapUtils.union(valueLattice, l, r);
	}
}

class LocationLattice extends Lattice {
	@Override
	public Object bottom() {
		return new StrategoAppl(new StrategoConstructor("Bottom", 0), new IStrategoTerm[] {}, null);
	}

	@Override
	public Object top() {
		return new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
	}

	@Override
	public Object lub(Object l_t, Object r_t) {
		return ((Supplier) () -> {
			IStrategoTerm term25 = Helpers
					.toTerm(new StrategoTuple(new IStrategoTerm[] { Helpers.toTerm(l_t), Helpers.toTerm(r_t) }, null));
			if (TermUtils.isTuple(term25) && (TermUtils.isAppl(Helpers.at(term25, 0))
					&& (M.appl(Helpers.at(term25, 0)).getName().equals("Top")
							&& Helpers.at(term25, 0).getSubtermCount() == 0))) {
				return new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
			}
			if (TermUtils.isTuple(term25) && (TermUtils.isAppl(Helpers.at(term25, 1))
					&& (M.appl(Helpers.at(term25, 1)).getName().equals("Top")
							&& Helpers.at(term25, 1).getSubtermCount() == 0))) {
				return new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
			}
			if (TermUtils.isTuple(term25) && (TermUtils.isAppl(Helpers.at(term25, 0))
					&& (M.appl(Helpers.at(term25, 0)).getName().equals("Loc")
							&& (Helpers.at(term25, 0).getSubtermCount() == 1 && (TermUtils.isAppl(Helpers.at(term25, 1))
									&& (M.appl(Helpers.at(term25, 1)).getName().equals("Loc")
											&& Helpers.at(term25, 1).getSubtermCount() == 1)))))) {
				IStrategoTerm i_t = Helpers.at(Helpers.at(term25, 0), 0);
				IStrategoTerm j_t = Helpers.at(Helpers.at(term25, 1), 0);
				return (boolean) i_t.equals(j_t)
						? new StrategoAppl(new StrategoConstructor("Loc", 1),
								new IStrategoTerm[] { Helpers.toTerm(i_t) }, null)
						: new StrategoAppl(new StrategoConstructor("Top", 0), new IStrategoTerm[] {}, null);
			}
			if (TermUtils.isTuple(term25) && (TermUtils.isAppl(Helpers.at(term25, 1))
					&& (M.appl(Helpers.at(term25, 1)).getName().equals("Bottom")
							&& Helpers.at(term25, 1).getSubtermCount() == 0))) {
				return l_t;
			}
			if (TermUtils.isTuple(term25) && (TermUtils.isAppl(Helpers.at(term25, 0))
					&& (M.appl(Helpers.at(term25, 0)).getName().equals("Bottom")
							&& Helpers.at(term25, 0).getSubtermCount() == 0))) {
				return r_t;
			}
			throw new RuntimeException("Could not match term '" + term25 + "'.");
		}).get();
	}
}

class LocationMapLattice extends Lattice {
	@Override
	public Object bottom() {
		return new MapLattice(new LocationLattice()).bottom();
	}

	@Override
	public Object top() {
		return new MapLattice(new LocationLattice()).top();
	}

	@Override
	public Object lub(Object l_t, Object r_t) {
		return new MapLattice(new LocationLattice()).lub(l_t, r_t);
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
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode prev_t = node;
		return UserFunctions.locations_f(prev_t);
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm m_t = Helpers.at(Helpers.at(term, 1), 0);
		return new LocationMapLattice().lub(((Supplier) () -> {
			Map result = new HashMap();
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.locations_f(prev_t)).entrySet()) {
				Object k_t = e.getKey();
				Object v_t = e.getValue();
				if (!k_t.equals(n_t)) {
					result.put(k_t, v_t);
				}
			}
			return result;
		}).get(), ((Supplier) () -> {
			Map result = new HashMap();
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.locations_f(prev_t)).entrySet()) {
				Object k_t = e.getKey();
				Object v_t = e.getValue();
				if (k_t.equals(m_t)) {
					result.put(n_t, v_t);
				}
			}
			return result;
		}).get());
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode prev_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		return new LocationMapLattice().lub(((Supplier) () -> {
			Map result = new HashMap();
			for (Map.Entry<Object, Object> e : ((Map<Object, Object>) UserFunctions.locations_f(prev_t)).entrySet()) {
				Object k_t = e.getKey();
				Object v_t = e.getValue();
				if (!k_t.equals(n_t)) {
					result.put(k_t, v_t);
				}
			}
			return result;
		}).get(), MapUtils.create(n_t, new StrategoAppl(new StrategoConstructor("Loc", 1),
				new IStrategoTerm[] { Helpers.toTerm(prev_t.getProperty("position").value) }, null)));
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		return MapUtils.create();
	}
}

class UserFunctions {
	public static Object locations_f(Object o) {
		CfgNode node = (CfgNode) o;
		return node.getProperty("locations").value;
	}
}
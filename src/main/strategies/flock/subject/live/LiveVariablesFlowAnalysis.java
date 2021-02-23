package flock.subject.live;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
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
import flock.subject.live.Lattices;
import flock.subject.live.MaySetLattice;
import flock.subject.live.MustSetLattice;
import flock.subject.live.TransferFunction0;
import flock.subject.live.TransferFunction1;
import flock.subject.live.TransferFunction2;
import flock.subject.live.TransferFunctions;
import flock.subject.live.UserFunctions;
import flock.subject.strategies.Program;

public class LiveVariablesFlowAnalysis {
	public static void main(String[] args) throws IOException {
		IStrategoTerm ast = new TAFTermReader(new TermFactory()).parseFromFile("snippets/c/many_vars.aterm");
		CfgGraph graph = CfgGraph.createControlFlowGraph(ast);
		performDataAnalysis(graph);
		System.out.println(graph.toGraphviz());
	}

	public static void initNodeValue(CfgNode node) {
		node.addProperty("live", Lattices.MaySet);
		node.getProperty("live").value = node.getProperty("live").lattice.bottom();
	}

	public static void initNodeTransferFunction(CfgNode node) {
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
		performDataAnalysis(roots, nodeset, dirty, Long.MIN_VALUE);
	}

	public static void performDataAnalysis(Set<CfgNode> roots, Set<CfgNode> nodeset, Set<CfgNode> dirty,
			long intervalBoundary) {
		Queue<CfgNode> worklist = new LinkedBlockingQueue<>();
		HashSet<CfgNode> inWorklist = new HashSet<>();
		for (CfgNode node : nodeset) {
			if (node.interval < intervalBoundary)
				continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeValue(node);
			initNodeTransferFunction(node);
		}
		for (CfgNode node : dirty) {
			if (node.interval < intervalBoundary)
				continue;
			worklist.add(node);
			inWorklist.add(node);
			initNodeTransferFunction(node);
		}
		for (CfgNode root : roots) {
			if (root.interval < intervalBoundary)
				continue;
			{
				root.getProperty("live").value = root.getProperty("live").init.eval(root);
			}
		}
		while (!worklist.isEmpty()) {
			CfgNode node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval < intervalBoundary)
				continue;
			Object live_n = node.getProperty("live").transfer.eval(node);

			for (CfgNode successor : node.children) {
				if (successor.interval < intervalBoundary)
					continue;
				boolean changed = false;
				if (changed && !inWorklist.contains(successor)) {
					worklist.add(successor);
					inWorklist.add(successor);
				}
			}
			for (CfgNode successor : node.parents) {
				if (successor.interval < intervalBoundary)
					continue;
				boolean changed = false;
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
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode next_t = node;
		return UserFunctions.live_f(next_t);
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode next_t = node;
		IStrategoString n_t = (IStrategoString) Helpers.at(term, 0);
		IStrategoTerm e_t = Helpers.at(term, 1);
		return ((Supplier) () -> {
			Set result = new HashSet();
			for (Object m_t : (Set) UserFunctions.live_f(next_t)) {
				if (!n_t.stringValue().equals(((LivenessValue) m_t).name)) {
					result.add(m_t);
				}
			}
			return result;
		}).get();
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode next_t = node;
		IStrategoString n_t = (IStrategoString) Helpers.at(term, 0);
		Object previous_t = UserFunctions.live_f(next_t);
		Object current_t = SetUtils.create(new LivenessValue(n_t.stringValue(), node.id));
		Object result_t = SetUtils.union(previous_t, current_t);
		return result_t;
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode next_t = node;
		IStrategoString n_t = (IStrategoString) Helpers.at(term, 0);
		Object previous_t = UserFunctions.live_f(next_t);
		Object current_t = SetUtils.create(new LivenessValue(n_t.stringValue(), node.id));
		Object result_t = SetUtils.union(previous_t, current_t);
		return result_t;
	}
}

class TransferFunction4 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		CfgNode next_t = node;
		IStrategoString n_t = (IStrategoString) Helpers.at(term, 0);
		Object previous_t = UserFunctions.live_f(next_t);
		Object current_t = SetUtils.create(new LivenessValue(n_t.stringValue(), node.id));
		Object result_t = SetUtils.union(previous_t, current_t);
		return result_t;
	}
}

class TransferFunction5 extends TransferFunction {
	@Override
	public Object eval(CfgNode node) {
		IStrategoTerm term = node.term;
		return SetUtils.create();
	}
}

class UserFunctions {
	public static Object live_f(Object o) {
		CfgNode node = (CfgNode) o;
		return node.getProperty("live").value;
	}
}
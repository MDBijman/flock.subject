package flock.subject.live;

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
import flock.subject.common.Analysis;
import flock.subject.common.Graph.Node;
import flock.subject.common.Analysis.Direction;
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

public class LiveVariablesFlowAnalysis extends Analysis {
	public LiveVariablesFlowAnalysis() {
		super("live", Direction.BACKWARD);
	}

	public static void initNodeValue(Node node) {
		node.addProperty("live", MaySet.bottom());
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

	@Override
	public void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset, Collection<Node> dirty,
			float intervalBoundary) {
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
			root.getProperty("live").lattice = root.getProperty("live").init.eval(root);
		}
		for (Node node : nodeset) {
			if (node.interval < intervalBoundary)
				continue;
			{
				Lattice init = node.getProperty("live").lattice;
				for (Node pred : g.childrenOf(node)) {
					Lattice live_o = pred.getProperty("live").transfer.eval(pred);
					init = init.lub(live_o);
				}
				node.getProperty("live").lattice = init;
			}
		}
		while (!worklist.isEmpty()) {
			Node node = worklist.poll();
			inWorklist.remove(node);
			if (node.interval < intervalBoundary)
				continue;
			Lattice live_n = node.getProperty("live").transfer.eval(node);
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
				Lattice live_o = successor.getProperty("live").lattice;
				if (live_n.nleq(live_o)) {
					successor.getProperty("live").lattice = live_o.lub(live_n);
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
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		return UserFunctions.live_f(next_t);
	}
}

class TransferFunction1 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm n_t = Helpers.at(term, 0);
		IStrategoTerm e_t = Helpers.at(term, 1);
		Set result17 = new HashSet();
		for (Object m_t : (Set) UserFunctions.live_f(next_t).value()) {
			if (!n_t.equals(m_t)) {
				result17.add(m_t);
			}
		}
		MaySet result_t = new MaySet(result17);
		return result_t;
	}
}

class TransferFunction2 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm t_t = term;
		IStrategoTerm n_t = Helpers.at(term, 0);
		MaySet previous_t = (MaySet) UserFunctions.live_f(next_t);
		MaySet current_t = (MaySet) new MaySet(SetUtils.create(new Live(n_t).withOrigin(Helpers.getTermPosition(t_t))));
		MaySet result_t = (MaySet) new MaySet(previous_t).lub(current_t);
		return result_t;
	}
}

class TransferFunction3 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm t_t = term;
		IStrategoTerm n_t = Helpers.at(term, 0);
		MaySet previous_t = (MaySet) UserFunctions.live_f(next_t);
		MaySet current_t = (MaySet) new MaySet(SetUtils.create(new Live(n_t).withOrigin(Helpers.getTermPosition(t_t))));
		MaySet result_t = (MaySet) new MaySet(previous_t).lub(current_t);
		return result_t;
	}
}

class TransferFunction4 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		Node next_t = node;
		IStrategoTerm t_t = term;
		IStrategoTerm n_t = Helpers.at(term, 0);
		MaySet previous_t = (MaySet) UserFunctions.live_f(next_t);
		MaySet current_t = (MaySet) new MaySet(SetUtils.create(new Live(n_t).withOrigin(Helpers.getTermPosition(t_t))));
		MaySet result_t = (MaySet) new MaySet(previous_t).lub(current_t);
		return result_t;
	}
}

class TransferFunction5 extends TransferFunction {
	@Override
	public Lattice eval(Node node) {
		IStrategoTerm term = node.term;
		return new MaySet(SetUtils.create());
	}
}

class UserFunctions {
	public static Lattice live_f(Object o) {
		Node node = (Node) o;
		return node.getProperty("live").lattice;
	}
}
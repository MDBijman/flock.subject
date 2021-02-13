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
import java.util.Map.Entry;
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

public class CfgGraph {
	public Set<CfgNode> roots;
	public HashMap<CfgNodeId, CfgNode> idToNode;
	public HashMap<CfgNodeId, IStrategoTerm> idToTerm;
	public HashMap<CfgNodeId, Long> idToInterval;
	private HashMap<String, Analysis> analyses;
	static private long nextInterval = 0;
	static private CfgNodeId nextId = new CfgNodeId(0);

	public CfgGraph(Set<CfgNode> roots, IStrategoTerm root) {
		this.roots = roots;
		this.idToNode = new HashMap<>();
		this.idToTerm = new HashMap<>();
		this.analyses = new HashMap<>();
		analyses.put("live", new Analysis("live", false));
		analyses.put("value", new Analysis("value", true));
		analyses.put("alias", new Analysis("alias", true));
		for (CfgNode n : this.flatten()) {
			this.idToNode.put(n.id, n);
		}
		computeIntervals();
	}

	private void addToDirty(CfgNode n) {
		for (Analysis a : analyses.values()) {
			a.dirtyNodes.add(n);
		}
	}

	private void addToNew(CfgNode n) {
		for (Analysis a : analyses.values()) {
			a.newNodes.add(n);
		}
	}

	private void removeFromDirty(Set<CfgNodeId> removedIds) {
		for (Analysis a : analyses.values()) {
			a.dirtyNodes.removeIf(n -> removedIds.contains(n.id));
		}
	}

	private void removeFromNew(Set<CfgNodeId> removedIds) {
		for (Analysis a : analyses.values()) {
			a.newNodes.removeIf(n -> removedIds.contains(n.id));
		}
	}

	private void computeIntervals() {
		nextInterval = 1;
		this.idToInterval = new HashMap<>();
		Set<CfgNode> visited = new HashSet<>();
		visited.addAll(this.roots);
		Queue<CfgNode> next = new LinkedBlockingQueue<>();
		for (CfgNode r : this.roots) {
			idToInterval.put(r.id, nextInterval++);
			next.addAll(r.children);
		}
		while (!next.isEmpty()) {
			CfgNode n = next.remove();
			if (visited.contains(n)) {
				continue;
			}
			visited.add(n);
			long newInterval = nextInterval++;
			idToInterval.put(n.id, newInterval);
			Queue<CfgNode> parentQueue = new LinkedBlockingQueue<>();
			parentQueue.addAll(n.parents);
			while (!parentQueue.isEmpty()) {
				CfgNode p = parentQueue.remove();
				if (visited.contains(p))
					continue;
				visited.add(p);
				idToInterval.put(p.id, newInterval);
				for (CfgNode pp : p.parents) {
					parentQueue.add(pp);
				}
				for (CfgNode c : p.children) {
					next.add(c);
				}
			}
			for (CfgNode c : n.children) {
				next.add(c);
			}
		}
	}

	public void replaceNode(Context context, CfgNode current, IStrategoTerm replacement) {
		Set<CfgNodeId> removedIds = getAllIds(current.term);
		Set<CfgNode> dependents = Analysis.getTermDependencies(current);

		for (CfgNodeId n : removedIds) {
			if (idToNode.get(n) != null)
				dependents.add(idToNode.get(n));
		}

		for (CfgNode n : this.flatten()) {
			boolean changed = false;
			for (CfgNode d : dependents) {
				boolean isRemoved = Analysis.removeFact(context, n, d.id);
				changed |= isRemoved;
			}
			if (changed) {
				addToDirty(n);
			}
		}

		Pair<Set<CfgNode>, Set<CfgNode>> newCfgNodes = createCfg(replacement);
		Set<CfgNode> newHeads = newCfgNodes.getLeft();
		Set<CfgNode> newLeaves = newCfgNodes.getRight();
		Set<CfgNode> oldParents = new HashSet<>();
		Set<CfgNode> oldChildren = new HashSet<>();
		Set<CfgNode> allCfgNodes = this.flatten();
		for (CfgNode n : allCfgNodes) {
			if (removedIds.contains(n.id))
				continue;
			if (n.parents.removeIf(c -> removedIds.contains(c.id))) {
				oldChildren.add(n);
			}
			if (n.children.removeIf(c -> removedIds.contains(c.id))) {
				oldParents.add(n);
			}
		}
		removeFromDirty(removedIds);
		removeFromNew(removedIds);
		boolean replacedRoot = this.roots.removeIf(root -> removedIds.contains(root.id));
		if (replacedRoot) {
			if (newHeads.size() > 0) {
				this.roots.addAll(newHeads);
			} else {
				for (CfgNode n : oldChildren) {
					this.roots.add(n);
				}
			}
		}
		for (CfgNode oldChild : oldChildren) {
			if (newLeaves.size() > 0) {
				for (CfgNode newLeaf : newLeaves) {
					newLeaf.addChild(oldChild);
				}
			} else {
				for (CfgNode oldParent : oldParents) {
					oldParent.addChild(oldChild);
				}
			}
		}
		for (CfgNode oldParent : oldParents) {
			if (newHeads.size() > 0) {
				for (CfgNode newHead : newHeads) {
					oldParent.addChild(newHead);
				}
			} else {
				for (CfgNode oldChild : oldChildren) {
					oldParent.addChild(oldChild);
				}
			}
		}
		computeIntervals();
		for (CfgNode n : newHeads) {
			addToNew(n);
		}
	}

	public void update(Context context, IStrategoTerm program) {
		updateTermMap(program);
		for (Analysis a : analyses.values()) {
			for (CfgNode n : a.dirtyNodes)
				idToNode.put(n.id, n);
			for (CfgNode n : a.newNodes)
				idToNode.put(n.id, n);
		}
		for (Entry<CfgNodeId, CfgNode> e : idToNode.entrySet()) {
			e.getValue().term = idToTerm.get(e.getKey());
		}
		for (Entry<CfgNodeId, Long> e : this.idToInterval.entrySet()) {
			idToNode.get(e.getKey()).interval = e.getValue();
		}
	}

	private void updateTermMap(IStrategoTerm program) {
		for (IStrategoTerm t : program.getSubterms()) {
			updateTermMap(t);
		}
		CfgNodeId id = getTermNodeId(program);
		if (id != null) {
			this.idToTerm.put(id, program);
		}
	}

	private Set<CfgNodeId> getAllIds(IStrategoTerm program) {
		HashSet<CfgNodeId> set = new HashSet<>();
		getAllIds(set, program);
		return set;
	}

	private void getAllIds(Set<CfgNodeId> visited, IStrategoTerm program) {
		for (IStrategoTerm t : program.getSubterms()) {
			getAllIds(visited, t);
		}

		CfgNodeId id = getTermNodeId(program);

		if (id != null) {
			visited.add(id);
		}
	}

	static public CfgNodeId nextNodeId() {
		CfgNodeId id = nextId;
		nextId = nextId.successor();
		return id;
	}

	public CfgNode getCfgNode(CfgNodeId id) {
		return this.idToNode.get(id);
	}

	public Set<CfgNode> getRoots() {
		return this.roots;
	}

	public String toGraphviz() {
		String result = "digraph G {\n";
		Set<CfgNode> allNodes = flatten();
		for (CfgNode node : allNodes) {
			result += node.hashCode() + "[label=\"" + node.id.getId() + " "
					+ node.term.toString().replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b")
							.replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'")
							.replace("\"", "\\\"")
					+ "\"];";
			for (CfgNode child : node.children) {
				result += node.hashCode() + "->" + child.hashCode() + ";\n";
			}
		}
		result += "}\n";
		return result;
	}

	public Set<CfgNode> flatten() {
		Set<CfgNode> nodes = new HashSet<>();
		for (CfgNode root : roots) {
			nodes.addAll(root.flatten());
		}
		return nodes;
	}

	private static Set<CfgNode> getAllLeaves(Set<CfgNode> input) {
		Set<CfgNode> result = new HashSet<>();
		for (CfgNode node : input) {
			result.addAll(node.getLeaves());
		}
		return result;
	}

	public static CfgGraph createControlFlowGraph(IStrategoTerm ast) {
		return new CfgGraph(createCfgs(ast), ast);
	}

	private static boolean isEmptyPair(Pair<Set<CfgNode>, Set<CfgNode>> p) {
		return p.getLeft().isEmpty() && p.getRight().isEmpty();
	}
	
	private static Set<CfgNode> createCfgs(IStrategoTerm term) {
		Set<CfgNode> nodes = new HashSet<>();
		if (TermUtils.isAppl(term)) {
			if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Root") && term.getSubtermCount() == 1)) {
				nodes.addAll(createCfg(term).getLeft());
			}
		}
		for (IStrategoTerm subterm : term.getSubterms()) {
			nodes.addAll(createCfgs(subterm));
		}
		return nodes;
	}

	private static Pair<Set<CfgNode>, Set<CfgNode>> createCfg(IStrategoTerm term) {
		Set<CfgNode> result_heads = new HashSet<>();
		Set<CfgNode> result_leaves = new HashSet<>();
		if (TermUtils.isList(term)) {
			IStrategoList list = M.list(term);
			if (list.isEmpty()) {
				return Pair.of(result_heads, result_leaves);
			}
			Pair<Set<CfgNode>, Set<CfgNode>> result = createCfg(list.head());
			result_heads.addAll(result.getLeft());
			result_leaves.addAll(result.getRight());
			list = list.tail();
			while (!list.isEmpty()) {
				Pair<Set<CfgNode>, Set<CfgNode>> new_result = createCfg(list.head());
				if (new_result.getLeft().isEmpty() && new_result.getRight().isEmpty()) {
					list = list.tail();
					continue;
				}

				for (CfgNode leaf : result_leaves) {
					leaf.addChild(new_result.getLeft());
				}
				result_leaves = new_result.getRight();
				list = list.tail();
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Root") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Root_t = term;
			IStrategoTerm s_t = Helpers.at(term, 0);
			Pair<Set<CfgNode>, Set<CfgNode>> s_nr = createCfg(s_t);
			result_heads.addAll(s_nr.getLeft());
			result_leaves.addAll(s_nr.getRight());
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Func") && term.getSubtermCount() == 3)) {
			IStrategoTerm _Func_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 1);
			IStrategoTerm r_t = Helpers.at(term, 2);
			Pair<Set<CfgNode>, Set<CfgNode>> stmts_nr = createCfg(stmts_t);
			result_heads = stmts_nr.getLeft();
			Pair<Set<CfgNode>, Set<CfgNode>> r_nr = createCfg(r_t);
			result_leaves = r_nr.getRight();
			for (CfgNode leaf : stmts_nr.getRight()) {
				leaf.addChild(r_nr.getLeft());
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Assign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Assign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			result_heads = e_nr.getLeft();
			CfgNode _Assign_nb = new CfgNode(getTermNodeId(term), term);
			result_leaves.add(_Assign_nb);
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(_Assign_nb);
			}
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("DerefAssign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _DerefAssign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			result_heads = e_nr.getLeft();
			CfgNode _DerefAssign_nb = new CfgNode(getTermNodeId(term), term);
			result_leaves.add(_DerefAssign_nb);
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(_DerefAssign_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Skip") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Skip_t = term;
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ret") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ret_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			result_heads = e_nr.getLeft();
			CfgNode _Ret_nb = new CfgNode(getTermNodeId(term), term);
			result_leaves.add(_Ret_nb);
			for (CfgNode leave : e_nr.getRight()) {
				leave.addChild(_Ret_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Block") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Block_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 0);
			Pair<Set<CfgNode>, Set<CfgNode>> stmts_nr = createCfg(stmts_t);
			result_heads = stmts_nr.getLeft();
			result_leaves = stmts_nr.getRight();
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("IfThenElse") && term.getSubtermCount() == 3)) {
			IStrategoTerm _IfThenElse_t = term;
			IStrategoTerm c_t = Helpers.at(term, 0);
			IStrategoTerm t_t = Helpers.at(term, 1);
			IStrategoTerm e_t = Helpers.at(term, 2);
			Pair<Set<CfgNode>, Set<CfgNode>> c_nr = createCfg(c_t);
			result_heads = c_nr.getLeft();
			Pair<Set<CfgNode>, Set<CfgNode>> t_nr = createCfg(t_t);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			if (isEmptyPair(t_nr)) {
				result_leaves.addAll(c_nr.getRight());
			}
			if (isEmptyPair(e_nr)) {
				result_leaves.addAll(c_nr.getRight());
			}
			for (CfgNode leaf : c_nr.getRight()) {
				leaf.addChild(t_nr.getLeft());
			}
			for (CfgNode leaf : c_nr.getRight()) {
				leaf.addChild(e_nr.getLeft());
			}
			result_leaves.addAll(t_nr.getRight());
			result_leaves.addAll(e_nr.getRight());
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("While") && term.getSubtermCount() == 2)) {
			IStrategoTerm _While_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			IStrategoTerm s_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			Pair<Set<CfgNode>, Set<CfgNode>> s_nr = createCfg(s_t);
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(s_nr.getLeft());
			}
			for (CfgNode leaf : s_nr.getRight()) {
				leaf.addChild(e_nr.getLeft());
			}
			result_heads.addAll(e_nr.getLeft());
			result_leaves.addAll(e_nr.getRight());
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Add") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Add_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			CfgNode _Add_nb = new CfgNode(getTermNodeId(term), term);
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Add_nb);
			}
			result_heads = e1_nr.getLeft();
			result_leaves.add(_Add_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Sub") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Sub_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			CfgNode _Sub_nb = new CfgNode(getTermNodeId(term), term);
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Sub_nb);
			}
			result_heads = e1_nr.getLeft();
			result_leaves.add(_Sub_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gt") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gt_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			CfgNode _Gt_nb = new CfgNode(getTermNodeId(term), term);
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Gt_nb);
			}
			result_heads = e1_nr.getLeft();
			result_leaves.add(_Gt_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gte") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gte_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			CfgNode _Gte_nb = new CfgNode(getTermNodeId(term), term);
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Gte_nb);
			}
			result_heads = e1_nr.getLeft();
			result_leaves.add(_Gte_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Print") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Print_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			CfgNode _Print_nb = new CfgNode(getTermNodeId(term), term);
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(_Print_nb);
			}
			result_heads = e_nr.getLeft();
			result_leaves.add(_Print_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Malloc") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Malloc_t = term;
			CfgNode _Malloc_nb = new CfgNode(getTermNodeId(term), term);
			result_heads.add(_Malloc_nb);
			result_leaves.add(_Malloc_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Free") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Free_t = term;
			CfgNode _Free_nb = new CfgNode(getTermNodeId(term), term);
			result_heads.add(_Free_nb);
			result_leaves.add(_Free_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Int") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Int_t = term;
			CfgNode _Int_nb = new CfgNode(getTermNodeId(term), term);
			result_heads.add(_Int_nb);
			result_leaves.add(_Int_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("True") && term.getSubtermCount() == 0)) {
			IStrategoTerm _True_t = term;
			CfgNode _True_nb = new CfgNode(getTermNodeId(term), term);
			result_heads.add(_True_nb);
			result_leaves.add(_True_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("False") && term.getSubtermCount() == 0)) {
			IStrategoTerm _False_t = term;
			CfgNode _False_nb = new CfgNode(getTermNodeId(term), term);
			result_heads.add(_False_nb);
			result_leaves.add(_False_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ref_t = term;
			CfgNode _Ref_nb = new CfgNode(getTermNodeId(term), term);
			result_heads.add(_Ref_nb);
			result_leaves.add(_Ref_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Deref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Deref_t = term;
			CfgNode _Deref_nb = new CfgNode(getTermNodeId(term), term);
			result_heads.add(_Deref_nb);
			result_leaves.add(_Deref_nb);
		} else {
			throw new RuntimeException("Could not create CFG node for term '" + term + "'.");
		}
		return Pair.of(result_heads, result_leaves);
	}

	public static CfgNodeId getTermNodeId(IStrategoTerm n) {
		if (n.getAnnotations().size() == 0)
			return null;
		assert TermUtils.isAppl(n.getAnnotations().getSubterm(0), "FlockNodeId", 1);
		IStrategoInt id = (IStrategoInt) n.getAnnotations().getSubterm(0).getSubterm(0);
		return new CfgNodeId(id.intValue());
	}
}
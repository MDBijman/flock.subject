package flock.subject.common;

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

public class CfgGraph {
	public Set<CfgNode> roots;
	public HashMap<IStrategoTerm, CfgNode> termToNode;
	public HashMap<CfgNodeId, CfgNode> idToNode;
	private CfgNodeId nextId;

	public CfgGraph(Set<CfgNode> roots) {
		this.roots = roots;
		this.termToNode = new HashMap<IStrategoTerm, CfgNode>();
		this.idToNode = new HashMap<CfgNodeId, CfgNode>();
		this.nextId = new CfgNodeId(0);
		
		for (CfgNode t : flatten()) {
			this.termToNode.put(t.term, t);
			this.idToNode.put(this.nextId, t);
			t.id = this.nextId;
			this.nextId = this.nextId.successor();
		}
	}

	public CfgNode getTermNode(IStrategoTerm t) {
		return this.termToNode.get(t);
	}

	public Set<CfgNode> getRoots() {
		return this.roots;
	}

	public String toGraphviz() {
		String result = "digraph G {\n";
		for (CfgNode node : flatten()) {
			result += node.hashCode() + "[label=\""
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
		return new CfgGraph(createCfgs(ast));
	}

	private static Set<CfgNode> createCfgs(IStrategoTerm term) {
		Set<CfgNode> nodes = new HashSet<>();
		if (TermUtils.isAppl(term)) {
			if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Root") && term.getSubtermCount() == 1)) {
				nodes.addAll(createCfg(term));
			}
		}
		for (IStrategoTerm subterm : term.getSubterms()) {
			nodes.addAll(createCfgs(subterm));
		}
		return nodes;
	}

	private static Set<CfgNode> createCfg(IStrategoTerm term) {
		Set<CfgNode> result = new HashSet<>();
		if (TermUtils.isList(term)) {
			IStrategoList list = M.list(term);
			if (list.isEmpty()) {
				return result;
			}
			Set<CfgNode> heads = createCfg(list.head());
			result.addAll(heads);
			list = list.tail();
			while (!list.isEmpty()) {
				Set<CfgNode> newHeads = createCfg(list.head());
				Set<CfgNode> leaves = getAllLeaves(heads);
				for (CfgNode leave : leaves) {
					leave.addChild(newHeads);
				}
				heads = newHeads;
				list = list.tail();
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Root") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Root_t = term;
			IStrategoTerm s_t = Helpers.at(term, 0);
			Set<CfgNode> s_nr = createCfg(s_t);
			Set<CfgNode> s_lr = getAllLeaves(s_nr);
			result.addAll(s_nr);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Func") && term.getSubtermCount() == 3)) {
			IStrategoTerm _Func_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 1);
			IStrategoTerm r_t = Helpers.at(term, 2);
			Set<CfgNode> stmts_nr = createCfg(stmts_t);
			Set<CfgNode> stmts_lr = getAllLeaves(stmts_nr);
			Set<CfgNode> r_nr = createCfg(r_t);
			Set<CfgNode> r_lr = getAllLeaves(r_nr);
			result.addAll(stmts_nr);
			for (CfgNode leave : stmts_lr) {
				leave.addChild(r_nr);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Assign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Assign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Set<CfgNode> e_nr = createCfg(e_t);
			Set<CfgNode> e_lr = getAllLeaves(e_nr);
			CfgNode _Assign_nb = new CfgNode(_Assign_t);
			Set<CfgNode> _Assign_lb = _Assign_nb.getLeaves();
			result.addAll(e_nr);
			for (CfgNode leave : e_lr) {
				leave.addChild(_Assign_nb);
			}
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("DerefAssign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _DerefAssign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Set<CfgNode> e_nr = createCfg(e_t);
			Set<CfgNode> e_lr = getAllLeaves(e_nr);
			CfgNode _DerefAssign_nb = new CfgNode(_DerefAssign_t);
			Set<CfgNode> _DerefAssign_lb = _DerefAssign_nb.getLeaves();
			result.addAll(e_nr);
			for (CfgNode leave : e_lr) {
				leave.addChild(_DerefAssign_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Skip") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Skip_t = term;
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ret") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ret_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Set<CfgNode> e_nr = createCfg(e_t);
			Set<CfgNode> e_lr = getAllLeaves(e_nr);
			CfgNode _Ret_nb = new CfgNode(_Ret_t);
			Set<CfgNode> _Ret_lb = _Ret_nb.getLeaves();
			result.addAll(e_nr);
			for (CfgNode leave : e_lr) {
				leave.addChild(_Ret_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Block") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Block_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 0);
			Set<CfgNode> stmts_nr = createCfg(stmts_t);
			Set<CfgNode> stmts_lr = getAllLeaves(stmts_nr);
			result.addAll(stmts_nr);
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("IfThenElse") && term.getSubtermCount() == 3)) {
			IStrategoTerm _IfThenElse_t = term;
			IStrategoTerm c_t = Helpers.at(term, 0);
			IStrategoTerm t_t = Helpers.at(term, 1);
			IStrategoTerm e_t = Helpers.at(term, 2);
			Set<CfgNode> c_nr = createCfg(c_t);
			Set<CfgNode> c_lr = getAllLeaves(c_nr);
			Set<CfgNode> t_nr = createCfg(t_t);
			Set<CfgNode> t_lr = getAllLeaves(t_nr);
			Set<CfgNode> e_nr = createCfg(e_t);
			Set<CfgNode> e_lr = getAllLeaves(e_nr);
			result.addAll(c_nr);
			for (CfgNode leave : c_lr) {
				leave.addChild(t_nr);
			}
			for (CfgNode leave : c_lr) {
				leave.addChild(e_nr);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("While") && term.getSubtermCount() == 2)) {
			IStrategoTerm _While_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			IStrategoTerm s_t = Helpers.at(term, 1);
			Set<CfgNode> e_nr = createCfg(e_t);
			Set<CfgNode> e_lr = getAllLeaves(e_nr);
			Set<CfgNode> s_nr = createCfg(s_t);
			Set<CfgNode> s_lr = getAllLeaves(s_nr);
			result.addAll(e_nr);
			for (CfgNode leave : e_lr) {
				leave.addChild(s_nr);
			}
			for (CfgNode leave : s_lr) {
				leave.addChild(e_nr);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Add") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Add_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Set<CfgNode> e1_nr = createCfg(e1_t);
			Set<CfgNode> e1_lr = getAllLeaves(e1_nr);
			Set<CfgNode> e2_nr = createCfg(e2_t);
			Set<CfgNode> e2_lr = getAllLeaves(e2_nr);
			CfgNode _Add_nb = new CfgNode(_Add_t);
			Set<CfgNode> _Add_lb = _Add_nb.getLeaves();
			result.addAll(e1_nr);
			for (CfgNode leave : e1_lr) {
				leave.addChild(e2_nr);
			}
			for (CfgNode leave : e2_lr) {
				leave.addChild(_Add_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Sub") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Sub_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Set<CfgNode> e1_nr = createCfg(e1_t);
			Set<CfgNode> e1_lr = getAllLeaves(e1_nr);
			Set<CfgNode> e2_nr = createCfg(e2_t);
			Set<CfgNode> e2_lr = getAllLeaves(e2_nr);
			CfgNode _Sub_nb = new CfgNode(_Sub_t);
			Set<CfgNode> _Sub_lb = _Sub_nb.getLeaves();
			result.addAll(e1_nr);
			for (CfgNode leave : e1_lr) {
				leave.addChild(e2_nr);
			}
			for (CfgNode leave : e2_lr) {
				leave.addChild(_Sub_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gt") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gt_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Set<CfgNode> e1_nr = createCfg(e1_t);
			Set<CfgNode> e1_lr = getAllLeaves(e1_nr);
			Set<CfgNode> e2_nr = createCfg(e2_t);
			Set<CfgNode> e2_lr = getAllLeaves(e2_nr);
			CfgNode _Gt_nb = new CfgNode(_Gt_t);
			Set<CfgNode> _Gt_lb = _Gt_nb.getLeaves();
			result.addAll(e1_nr);
			for (CfgNode leave : e1_lr) {
				leave.addChild(e2_nr);
			}
			for (CfgNode leave : e2_lr) {
				leave.addChild(_Gt_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gte") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gte_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Set<CfgNode> e1_nr = createCfg(e1_t);
			Set<CfgNode> e1_lr = getAllLeaves(e1_nr);
			Set<CfgNode> e2_nr = createCfg(e2_t);
			Set<CfgNode> e2_lr = getAllLeaves(e2_nr);
			CfgNode _Gte_nb = new CfgNode(_Gte_t);
			Set<CfgNode> _Gte_lb = _Gte_nb.getLeaves();
			result.addAll(e1_nr);
			for (CfgNode leave : e1_lr) {
				leave.addChild(e2_nr);
			}
			for (CfgNode leave : e2_lr) {
				leave.addChild(_Gte_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Print") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Print_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Set<CfgNode> e_nr = createCfg(e_t);
			Set<CfgNode> e_lr = getAllLeaves(e_nr);
			CfgNode _Print_nb = new CfgNode(_Print_t);
			Set<CfgNode> _Print_lb = _Print_nb.getLeaves();
			result.addAll(e_nr);
			for (CfgNode leave : e_lr) {
				leave.addChild(_Print_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Malloc") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Malloc_t = term;
			CfgNode _Malloc_nb = new CfgNode(_Malloc_t);
			Set<CfgNode> _Malloc_lb = _Malloc_nb.getLeaves();
			result.add(_Malloc_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Free") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Free_t = term;
			CfgNode _Free_nb = new CfgNode(_Free_t);
			Set<CfgNode> _Free_lb = _Free_nb.getLeaves();
			result.add(_Free_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Int") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Int_t = term;
			CfgNode _Int_nb = new CfgNode(_Int_t);
			Set<CfgNode> _Int_lb = _Int_nb.getLeaves();
			result.add(_Int_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("True") && term.getSubtermCount() == 0)) {
			IStrategoTerm _True_t = term;
			CfgNode _True_nb = new CfgNode(_True_t);
			Set<CfgNode> _True_lb = _True_nb.getLeaves();
			result.add(_True_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("False") && term.getSubtermCount() == 0)) {
			IStrategoTerm _False_t = term;
			CfgNode _False_nb = new CfgNode(_False_t);
			Set<CfgNode> _False_lb = _False_nb.getLeaves();
			result.add(_False_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ref_t = term;
			CfgNode _Ref_nb = new CfgNode(_Ref_t);
			Set<CfgNode> _Ref_lb = _Ref_nb.getLeaves();
			result.add(_Ref_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Deref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Deref_t = term;
			CfgNode _Deref_nb = new CfgNode(_Deref_t);
			Set<CfgNode> _Deref_lb = _Deref_nb.getLeaves();
			result.add(_Deref_nb);
		} else {
			throw new RuntimeException("Could not create CFG node for term '" + term + "'.");
		}
		return result;
	}
}
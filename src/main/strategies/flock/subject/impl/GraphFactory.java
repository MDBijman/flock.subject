package flock.subject.impl;

import java.util.HashSet;

import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;

import flock.subject.common.CfgNodeId;
import flock.subject.common.Graph;
import flock.subject.common.Graph.Node;
import flock.subject.common.Helpers;

public class GraphFactory {
	public static Graph createCfg(IStrategoTerm term) {
		Graph result_graph = new Graph();
		for (IStrategoTerm subterm : term.getSubterms()) {
			Graph subgraph = createCfg(subterm);
			if (subgraph.size() != 0) {
				result_graph.mergeGraph(subgraph);
			}
		}
		if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Root") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Root_t = term;
			IStrategoTerm s_t = Helpers.at(term, 0);
			Graph s_nr = createCfg_inner(s_t);
			result_graph.mergeGraph(s_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(s_nr.leaves);
		} else {
		}
		return result_graph;
	}

	private static Graph createCfg_inner(IStrategoTerm term) {
		Graph result_graph = new Graph();
		if (TermUtils.isList(term)) {
			IStrategoList list = M.list(term);
			if (list.isEmpty()) {
				return new Graph();
			}
			result_graph.mergeGraph(createCfg_inner(list.head()));
			list = list.tail();
			while (!list.isEmpty()) {
				Graph new_result = createCfg_inner(list.head());
				result_graph.attachChildGraph(result_graph.leaves, new_result);
				result_graph.leaves = new_result.leaves;
				list = list.tail();
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Func") && term.getSubtermCount() == 3)) {
			IStrategoTerm _Func_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 1);
			IStrategoTerm r_t = Helpers.at(term, 2);
			Graph stmts_nr = createCfg_inner(stmts_t);
			Graph r_nr = createCfg_inner(r_t);
			result_graph.mergeGraph(stmts_nr);
			result_graph.attachChildGraph(stmts_nr.leaves, r_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(r_nr.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Assign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Assign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Graph e_nr = createCfg_inner(e_t);
			Graph _Assign_nb = new Graph(getTermNode(_Assign_t), _Assign_t);
			result_graph.mergeGraph(e_nr);
			result_graph.attachChildGraph(e_nr.leaves, _Assign_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Assign_nb.leaves);
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("DerefAssign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _DerefAssign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Graph e_nr = createCfg_inner(e_t);
			Graph _DerefAssign_nb = new Graph(getTermNode(_DerefAssign_t), _DerefAssign_t);
			result_graph.mergeGraph(e_nr);
			result_graph.attachChildGraph(e_nr.leaves, _DerefAssign_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_DerefAssign_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ret") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ret_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Graph e_nr = createCfg_inner(e_t);
			Graph _Ret_nb = new Graph(getTermNode(_Ret_t), _Ret_t);
			result_graph.mergeGraph(e_nr);
			result_graph.attachChildGraph(e_nr.leaves, _Ret_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Ret_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Skip") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Skip_t = term;
			result_graph.leaves = new HashSet<>();
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Block") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Block_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 0);
			Graph stmts_nr = createCfg_inner(stmts_t);
			result_graph.mergeGraph(stmts_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(stmts_nr.leaves);
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("IfThenElse") && term.getSubtermCount() == 3)) {
			IStrategoTerm _IfThenElse_t = term;
			IStrategoTerm c_t = Helpers.at(term, 0);
			IStrategoTerm t_t = Helpers.at(term, 1);
			IStrategoTerm e_t = Helpers.at(term, 2);
			Graph c_nr = createCfg_inner(c_t);
			Graph t_nr = createCfg_inner(t_t);
			Graph e_nr = createCfg_inner(e_t);
			result_graph.mergeGraph(c_nr);
			result_graph.attachChildGraph(c_nr.leaves, t_nr);
			result_graph.attachChildGraph(c_nr.leaves, e_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(t_nr.leaves);
			result_graph.leaves.addAll(e_nr.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("While") && term.getSubtermCount() == 2)) {
			IStrategoTerm _While_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			IStrategoTerm s_t = Helpers.at(term, 1);
			Graph e_nr = createCfg_inner(e_t);
			Graph s_nr = createCfg_inner(s_t);
			result_graph.mergeGraph(e_nr);
			result_graph.attachChildGraph(e_nr.leaves, s_nr);
			result_graph.attachChildGraph(s_nr.leaves, e_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(e_nr.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Add") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Add_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Graph e1_nr = createCfg_inner(e1_t);
			Graph e2_nr = createCfg_inner(e2_t);
			Graph _Add_nb = new Graph(getTermNode(_Add_t), _Add_t);
			result_graph.mergeGraph(e1_nr);
			result_graph.attachChildGraph(e1_nr.leaves, e2_nr);
			result_graph.attachChildGraph(e2_nr.leaves, _Add_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Add_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Sub") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Sub_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Graph e1_nr = createCfg_inner(e1_t);
			Graph e2_nr = createCfg_inner(e2_t);
			Graph _Sub_nb = new Graph(getTermNode(_Sub_t), _Sub_t);
			result_graph.mergeGraph(e1_nr);
			result_graph.attachChildGraph(e1_nr.leaves, e2_nr);
			result_graph.attachChildGraph(e2_nr.leaves, _Sub_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Sub_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gt") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gt_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Graph e1_nr = createCfg_inner(e1_t);
			Graph e2_nr = createCfg_inner(e2_t);
			Graph _Gt_nb = new Graph(getTermNode(_Gt_t), _Gt_t);
			result_graph.mergeGraph(e1_nr);
			result_graph.attachChildGraph(e1_nr.leaves, e2_nr);
			result_graph.attachChildGraph(e2_nr.leaves, _Gt_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Gt_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gte") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gte_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Graph e1_nr = createCfg_inner(e1_t);
			Graph e2_nr = createCfg_inner(e2_t);
			Graph _Gte_nb = new Graph(getTermNode(_Gte_t), _Gte_t);
			result_graph.mergeGraph(e1_nr);
			result_graph.attachChildGraph(e1_nr.leaves, e2_nr);
			result_graph.attachChildGraph(e2_nr.leaves, _Gte_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Gte_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Print") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Print_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Graph e_nr = createCfg_inner(e_t);
			Graph _Print_nb = new Graph(getTermNode(_Print_t), _Print_t);
			result_graph.mergeGraph(e_nr);
			result_graph.attachChildGraph(e_nr.leaves, _Print_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Print_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Malloc") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Malloc_t = term;
			Graph _Malloc_nb = new Graph(getTermNode(_Malloc_t), _Malloc_t);
			result_graph.mergeGraph(_Malloc_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Malloc_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Free") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Free_t = term;
			Graph _Free_nb = new Graph(getTermNode(_Free_t), _Free_t);
			result_graph.mergeGraph(_Free_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Free_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Int") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Int_t = term;
			Graph _Int_nb = new Graph(getTermNode(_Int_t), _Int_t);
			result_graph.mergeGraph(_Int_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Int_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("True") && term.getSubtermCount() == 0)) {
			IStrategoTerm _True_t = term;
			Graph _True_nb = new Graph(getTermNode(_True_t), _True_t);
			result_graph.mergeGraph(_True_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_True_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("False") && term.getSubtermCount() == 0)) {
			IStrategoTerm _False_t = term;
			Graph _False_nb = new Graph(getTermNode(_False_t), _False_t);
			result_graph.mergeGraph(_False_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_False_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ref_t = term;
			Graph _Ref_nb = new Graph(getTermNode(_Ref_t), _Ref_t);
			result_graph.mergeGraph(_Ref_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Ref_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Deref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Deref_t = term;
			Graph _Deref_nb = new Graph(getTermNode(_Deref_t), _Deref_t);
			result_graph.mergeGraph(_Deref_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Deref_nb.leaves);
		} else {
			throw new RuntimeException("Could not create CFG node for term '" + term + "'.");
		}
		return result_graph;
	}

	public static Node getTermNode(IStrategoTerm n) {
		if (n.getAnnotations().size() == 0)
			return null;
		assert TermUtils.isAppl(n.getAnnotations().getSubterm(0), "FlockNodeId", 1);
		IStrategoInt id = (IStrategoInt) n.getAnnotations().getSubterm(0).getSubterm(0);
		return new Node(new CfgNodeId(id.intValue()));
	}
}
package flock.subject.common;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.TermFactory;
import java.io.IOException;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.Context;

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

import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.common.CfgGraph;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.live.LivenessValue;
import flock.subject.strategies.Program;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class CfgGraph {
	public Set<CfgNode> roots;
	public HashMap<CfgNodeId, CfgNode> idToNode;
	public HashMap<CfgNodeId, IStrategoTerm> idToTerm;

	private HashSet<CfgNode> dirty;
	static private CfgNodeId nextId = new CfgNodeId(0);
	
	public CfgGraph(Set<CfgNode> roots, IStrategoTerm root) {
		this.roots = roots;
		this.idToNode = new HashMap<>();
		this.idToTerm = new HashMap<>();
		this.dirty = new HashSet<CfgNode>();
		
		for (CfgNode n : this.flatten()) {
			this.idToNode.put(n.id, n);
		}
	}

	public void replaceNode(Context context, CfgNode current, IStrategoTerm replacement) {
		Set<CfgNodeId> ids = getAllIds(current.term);

		Set<CfgNode> dependents = getTermDependencies(current);
		for (CfgNode n : dependents) {
			ids.add(n.id);
			//context.getIOAgent().printError("Added dep: " + n.id.getId());
		}
		
		//context.getIOAgent().printError("Ids in node/dependency:");
		//for(CfgNodeId id: ids) {
		//	context.getIOAgent().printError(id.getId() + "");
		//}
		//context.getIOAgent().printError("End");

		for (CfgNode n : this.flatten()) {
			boolean changed = false;
			for (CfgNodeId id : ids) {
				boolean isRemoved = removeFact(context, n, id);
				changed |= isRemoved;
			}
			if (changed) {
				dirty.add(n);
				//context.getIOAgent().printError("Changed at: " + n.id.getId() + "");
			}
		}

		Set<CfgNode> newCfgNodes = createCfg(replacement);
		
		// Fix parent to (new) child links
		for (CfgNode parent : current.parents) {
			parent.children.remove(current);
			parent.addChild(newCfgNodes);
		}
		
		// Fix new node to old child links
		for (CfgNode newLeaf : getAllLeaves(newCfgNodes)) {
			newLeaf.addChild(current.children);
			for (CfgNode child : current.children) {
				child.parents.remove(current);
				child.parents.add(newLeaf);
			}
		}
		
		// Fix roots
		boolean replacedRoot = this.roots.removeIf(root -> ids.contains(root.id));
		if (replacedRoot) {
			if (newCfgNodes.size() > 0) {
				this.roots.addAll(newCfgNodes);
			} else {
				this.roots.addAll(current.children);
			}
		}
		
		for (CfgNode n : newCfgNodes) {
			dirty.add(n);
		}


	}
	
	private boolean removeFact(Context context, CfgNode current, CfgNodeId toRemove) {
		Set<LivenessValue> lv = (Set<LivenessValue>) current.getProperty("live").value;
		assert lv != null;
		boolean removedLiveness = lv.removeIf(ll -> { 
			boolean isSame = ll.origin.getId() == toRemove.getId();
			if (isSame) {
				//context.getIOAgent().printError("Removing fact with origin " + toRemove.getId() + " at " + current.id.getId());
			}
			return isSame; 
		});
		
		HashMap<String, ValueValue> cv = (HashMap<String, ValueValue>) current.getProperty("values").value;
		assert cv != null;
		
		//context.getIOAgent().printError(current.id.getId() +" "+ toRemove.getId()+" "+ cv.toString());
		boolean removedConst = cv.entrySet().removeIf(ll -> { 
			boolean isSame = ll.getValue().origin.contains(toRemove);
			if (isSame) {
				//context.getIOAgent().printError("Removing fact with origin " + toRemove.getId() + " at " + current.id.getId());
			}
			return isSame; 
		});
	
		return removedLiveness || removedConst;
	}

	public void update(Context context, IStrategoTerm program) {
		updateTermMap(program);
	
		for (CfgNode n : dirty) {
			idToNode.put(n.id, n);
		}
		
		for (Entry<CfgNodeId, CfgNode> e : idToNode.entrySet()) {
			e.getValue().term = idToTerm.get(e.getKey());
		}

		
		Set<CfgNode> recompute = new HashSet<>();
		
		for (CfgNode n : dirty) {
			recompute.addAll(getTermDependencies(n));
		}
		long begin = System.currentTimeMillis();
		ValueFlowAnalysis.performDataAnalysis(recompute);
		context.getIOAgent().printError("Time: " + (System.currentTimeMillis() - begin));
		LiveVariablesFlowAnalysis.performDataAnalysis(recompute);
		PointsToFlowAnalysis.performDataAnalysis(recompute);
		
		dirty.clear();
	}
	
	private Set<CfgNode> getTermDependencies(CfgNode n) {
		Set<CfgNode> r = new HashSet<>();
		
		r.add(n);
		
		// Folding
		if (TermUtils.isAppl(n.term, "Int", 1)) {
			r.addAll(n.parents);
		}
		// Propagation
		for (CfgNode p : n.children) {
			if (TermUtils.isAppl(p.term, "Assign", -1)) {
				r.add(p);
			}			
		}
	
		return r;
	}
	
	private void updateTermMap(IStrategoTerm program) {
		for(IStrategoTerm t : program.getSubterms()) {
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
		for (CfgNode node : flatten()) {
			
			String valueString = node.getProperty("values") != null ? node.getProperty("values").value.toString().replace("\"", "\\\"") :"";
			
			result += node.hashCode() + "[label=\""
					+ node.id.getId() + " "
					+ node.term.toString().replace("\"", "\\\"") + " "
					+ valueString
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
			CfgNode _Assign_nb = new CfgNode(getTermNodeId(term));
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
			CfgNode _DerefAssign_nb = new CfgNode(getTermNodeId(term));
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
			CfgNode _Ret_nb = new CfgNode(getTermNodeId(term));
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
			CfgNode _Add_nb = new CfgNode(getTermNodeId(term));
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
			CfgNode _Sub_nb = new CfgNode(getTermNodeId(term));
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
			CfgNode _Gt_nb = new CfgNode(getTermNodeId(term));
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
			CfgNode _Gte_nb = new CfgNode(getTermNodeId(term));
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
			CfgNode _Print_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _Print_lb = _Print_nb.getLeaves();
			result.addAll(e_nr);
			for (CfgNode leave : e_lr) {
				leave.addChild(_Print_nb);
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Malloc") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Malloc_t = term;
			CfgNode _Malloc_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _Malloc_lb = _Malloc_nb.getLeaves();
			result.add(_Malloc_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Free") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Free_t = term;
			CfgNode _Free_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _Free_lb = _Free_nb.getLeaves();
			result.add(_Free_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Int") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Int_t = term;
			CfgNode _Int_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _Int_lb = _Int_nb.getLeaves();
			result.add(_Int_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("True") && term.getSubtermCount() == 0)) {
			IStrategoTerm _True_t = term;
			CfgNode _True_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _True_lb = _True_nb.getLeaves();
			result.add(_True_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("False") && term.getSubtermCount() == 0)) {
			IStrategoTerm _False_t = term;
			CfgNode _False_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _False_lb = _False_nb.getLeaves();
			result.add(_False_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ref_t = term;
			CfgNode _Ref_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _Ref_lb = _Ref_nb.getLeaves();
			result.add(_Ref_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Deref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Deref_t = term;
			CfgNode _Deref_nb = new CfgNode(getTermNodeId(term));
			Set<CfgNode> _Deref_lb = _Deref_nb.getLeaves();
			result.add(_Deref_nb);
		} else {
			throw new RuntimeException("Could not create CFG node for term '" + term + "'.");
		}
		return result;
	}
	
	public static CfgNodeId getTermNodeId(IStrategoTerm n) {
		if (n.getAnnotations().size() == 0) return null;
		assert TermUtils.isAppl(n.getAnnotations().getSubterm(0), "FlockNodeId", 1);
		IStrategoInt id = (IStrategoInt) n.getAnnotations().getSubterm(0).getSubterm(0);
		return new CfgNodeId(id.intValue());
	}

}
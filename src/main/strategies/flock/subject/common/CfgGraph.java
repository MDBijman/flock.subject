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
import flock.subject.common.CfgGraph;
import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LivenessValue;
import flock.subject.strategies.Program;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class CfgGraph {
	public Set<CfgNode> roots;
	private Set<CfgNode> allNodes;
	public HashMap<CfgNodeId, CfgNode> idToNode;
	public HashMap<CfgNodeId, IStrategoTerm> idToTerm;
	public HashMap<CfgNodeId, Long> idToInterval;
	public Analysis analysis;
	static private CfgNodeId nextId = new CfgNodeId(0);
	private boolean debug = true;

	public CfgGraph(Set<CfgNode> roots, IStrategoTerm root) {
		this.roots = roots;
		this.idToNode = new HashMap<>();
		this.idToTerm = new HashMap<>();
		this.analysis = new Analysis();
		this.allNodes = this.flatten();
		for (CfgNode n : this.allNodes) {
			this.idToNode.put(n.id, n);
		}
		this.removeGhostNodes();
		computeIntervals();
		checkInvariants();
	}
	
	private void checkInvariants() {
		if (debug) {
			checkIntervals();
			checkGraph();
		}
	}

	private void checkIntervals() {
		for (CfgNode n : this.allNodes) {
			for (CfgNode c : n.children) {
				assert c.interval >= n.interval;
				if (c.interval < n.interval) {
					Program.log("validation", "Error in intervals!");
				}
			}
		}
	}
	
	private void checkGraph() {
		for (CfgNode n : this.allNodes) {
			for (CfgNode c : n.children) {
				if (!c.parents.contains(n)) {
					Program.log("validation", "Error in parent-child!");
				}
			}
			for (CfgNode p : n.parents) {
				if (!p.children.contains(n)) {
					Program.log("validation", "Error in parent-child!");
				}
			}
		}
	}
	
	private void computeIntervals() {
		Program.increment("computeIntervals");
		Program.beginTime("computeIntervals");
		this.idToInterval = new HashMap<>();
		Long next_index = new Long(1);
		Stack<CfgNode> S = new Stack<>();
		Set<CfgNode> nodes = this.allNodes;
		HashSet<CfgNodeId> onStack = new HashSet<>();
		HashMap<CfgNodeId, Long> lowlink = new HashMap<>();
		HashMap<CfgNodeId, Long> index = new HashMap<>();
		Set<Set<CfgNode>> components = new HashSet<>();
		HashMap<CfgNode, Set<CfgNode>> nodeComponent = new HashMap<>();
		for (CfgNode n : nodes)
			n.interval = Long.MAX_VALUE;
		for (CfgNode r : roots) {
			if (index.get(r.id) == null) {
				strongConnect(r, next_index, S, onStack, lowlink, index, components, nodeComponent);
			}
		}
		
		// This can be optimized much better
		// Kahn's algorithm applied to the scc's of the graph
		
		// Setup the edges between the scc's
		// Map from SCC to all successor SCC's
		HashMap<Set<CfgNode>, Set<Set<CfgNode>>> successors = new HashMap<>();
		HashMap<Set<CfgNode>, Set<Set<CfgNode>>> predecessors = new HashMap<>();
		for (Set<CfgNode> set : components) {
			successors.put(set, new HashSet<>());
			predecessors.put(set, new HashSet<>());
		}
		Program.beginTime("a");
		for (CfgNode n : nodes) {
			for (CfgNode c : n.children) {
				Set<CfgNode> nComponent = nodeComponent.get(n);
				Set<CfgNode> cComponent = nodeComponent.get(c);
				if (nComponent != cComponent) {
					successors.get(nComponent).add(cComponent);
					predecessors.get(cComponent).add(nComponent);
				}
			}
		}
		Program.endTime("a");
		
		Set<Set<CfgNode>> noOutgoing = new HashSet<>();
		
		// Find component with no successors (i.e. no outgoing edge)
		for (Set<CfgNode> component : components) {
			if (successors.get(component).isEmpty()) {
				noOutgoing.add(component);
			}
		}
		
		long currIndex = components.size() + 1;
		while (!components.isEmpty()) {
			Set<CfgNode> c = noOutgoing.iterator().next();
			noOutgoing.remove(c);
			
			// Remove component and set interval of its nodes
			components.remove(c);
			for (Set<CfgNode> p : predecessors.get(c))
			{
				successors.get(p).remove(c);
				if (successors.get(p).size() == 0) 
					noOutgoing.add(p);
			}
			
			successors.remove(c);
			predecessors.remove(c);
			
			for (CfgNode n : c) {
				n.interval = currIndex;
			}
			currIndex--;
		}
		

		// End
		
		for (CfgNode n : nodes) {
			this.idToInterval.put(n.id, n.interval);
		}
		Program.endTime("computeIntervals");
	}

	private void strongConnect(CfgNode v, Long next_index, Stack<CfgNode> S, HashSet<CfgNodeId> onStack,
			HashMap<CfgNodeId, Long> lowlink, HashMap<CfgNodeId, Long> index, Set<Set<CfgNode>> components, HashMap<CfgNode, Set<CfgNode>> nodeComponent) {
		index.put(v.id, next_index);
		lowlink.put(v.id, next_index);
		next_index += 1;
		S.push(v);
		onStack.add(v.id);
		for (CfgNode w : v.children) {
			if (index.get(w.id) == null) {
				strongConnect(w, next_index, S, onStack, lowlink, index, components, nodeComponent);
				if (lowlink.get(w.id) < lowlink.get(v.id)) {
					lowlink.put(v.id, lowlink.get(w.id));
				}
			} else {
				if (onStack.contains(w.id)) {
					if (index.get(w.id) < lowlink.get(v.id)) {
						lowlink.put(v.id, index.get(w.id));
					}
				}
			}
		}
		if (lowlink.get(v.id) == index.get(v.id)) {
			CfgNode w;
			HashSet<CfgNode> component = new HashSet<CfgNode>();
			do {
				w = S.pop();
				onStack.remove(w.id);
				nodeComponent.put(w, component);
				component.add(w);
			} while (w != v);
			components.add(component);
		}
	}

	public void removeNode(Context context, IStrategoTerm node)
	{
		Program.increment("removeNode");
		Program.log("api", "removing node " + node.toString());
		// Nodes outside of the removed id's that have analysis results directly depending on them (static analysis)
		Set<CfgNode> dependents = new HashSet<>();
		
		CfgNode currentNode = this.getCfgNode(getTermNodeId(node));
		if (currentNode != null) {
			dependents.addAll(Analysis.getTermDependencies(currentNode));
		}

		// Set of id's that were in the removed node
		Set<CfgNodeId> removedIds = getAllIds(node);
		
		analysis.removeFromDirty(removedIds);
		analysis.removeFromNew(removedIds);
		
		// Add removed id's to that set
		for (CfgNodeId n : removedIds) {
			if (idToNode.get(n) != null)
				dependents.add(idToNode.get(n));
		}

		// Go through graph and remove facts with origin in removed id's
		for (CfgNode n : this.allNodes) {
			if (removedIds.contains(n.id)) {
				n.isGhost = true;
			} else {
				boolean changed = false;
				for (CfgNode d : dependents) {
					boolean isRemoved = Analysis.removeFact(context, n, d.id);
					changed |= isRemoved;
				}
				if (changed) {
					analysis.addToDirty(n);
				}
			}
		}
		
		// Remove any ghost nodes created by the createCfg call
		this.removeGhostNodes();
		// Recompute intervals
		computeIntervals();
		// Debug
		checkInvariants();
	}
	
	public void replaceNode(Context context, IStrategoTerm current, IStrategoTerm replacement) {
		Program.increment("replaceNode");
		Program.beginTime("replaceNode");
		Program.beginTime("replaceNode remove facts");
		// Nodes outside of the removed id's that have analysis results directly depending on them (static analysis)
		Set<CfgNode> dependents = new HashSet<>();

		CfgNode currentNode = this.getCfgNode(getTermNodeId(current));
		if (currentNode != null) {
			dependents.addAll(Analysis.getTermDependencies(currentNode));
		}

		// Set of id's that were in the removed node
		Set<CfgNodeId> removedIds = getAllIds(current);		
		// Add removed id's to that set
		for (CfgNodeId n : removedIds) {
			if (idToNode.get(n) != null)
				dependents.add(idToNode.get(n));
		}
		// Go through graph and remove facts with origin in removed id's
		for (CfgNode n : this.allNodes) {
			boolean changed = false;
			for (CfgNode d : dependents) {
				boolean isRemoved = Analysis.removeFact(context, n, d.id);
				changed |= isRemoved;
			}
			if (changed) {
				analysis.addToDirty(n);
			}
		}
		for (CfgNodeId n : removedIds) {
			if (idToNode.get(n) != null)
			{
				this.allNodes.remove(idToNode.get(n));
			}
		}
		Program.endTime("replaceNode remove facts");

		// Here we patch the graph
		Program.beginTime("replaceNode fix cfg");
		Pair<Set<CfgNode>, Set<CfgNode>> newCfgNodes = createCfg(replacement);

		Set<CfgNode> newHeads = newCfgNodes.getLeft();
		Set<CfgNode> newLeaves = newCfgNodes.getRight();
		Set<CfgNode> oldParents = new HashSet<>();
		Set<CfgNode> oldChildren = new HashSet<>();
		
		Set<CfgNode> newNodes = new HashSet<>();
		for (CfgNode n : newHeads) {
			for (CfgNode c : n.flatten()) {
				if (!c.isGhost) {
					analysis.addToNew(c);
					newNodes.add(c);
				}
			}
		}
		
		for (CfgNode n : this.allNodes) {
			if (removedIds.contains(n.id))
				continue;
			if (n.parents.removeIf(c -> removedIds.contains(c.id))) {
				oldChildren.add(n);
			}
			if (n.children.removeIf(c -> removedIds.contains(c.id))) {
				oldParents.add(n);
			}
		}
		analysis.removeFromDirty(removedIds);
		analysis.removeFromNew(removedIds);
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
		
		this.allNodes.addAll(newNodes);
		Program.endTime("replaceNode fix cfg");
		
		// Remove any ghost nodes created by the createCfg call
		this.removeGhostNodes();
		// Recompute intervals
		computeIntervals();
		// Debug
		checkInvariants();

		Program.endTime("replaceNode");
	}

	public void init(Context context, IStrategoTerm program) {
		for (CfgNode n : this.allNodes) {
			analysis.addToNew(n);
		}
		update(context, program);
	}
	
	public void update(Context context, IStrategoTerm program) {
		Program.beginTime("update");
		updateTermMap(program);
		for (Set<CfgNode> a : analysis.getDirtySets()) {
			for (CfgNode n : a)
				idToNode.put(n.id, n);
		}
		for (Set<CfgNode> a : analysis.getNewSets()) {
			for (CfgNode n : a)
				idToNode.put(n.id, n);
		}
		for (Entry<CfgNodeId, CfgNode> entry : idToNode.entrySet()) {
			entry.getValue().term = idToTerm.get(entry.getKey());
		}
		for (Entry<CfgNodeId, Long> entry : this.idToInterval.entrySet()) {
			if (idToNode.get(entry.getKey()) == null) {
				Program.log("validation", entry.toString());
				Program.log("validation", this.toGraphviz().replace('\n', '\t'));
			}
			idToNode.get(entry.getKey()).interval = entry.getValue();
		}
		Program.endTime("update");
	}

	private void updateTermMap(IStrategoTerm program) {
		for (IStrategoTerm term : program.getSubterms()) {
			updateTermMap(term);
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
		for (IStrategoTerm term : program.getSubterms()) {
			getAllIds(visited, term);
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
		Program.beginTime("graphviz");
		StringBuilder result = new StringBuilder();
		result.append("digraph G {\n");
		for (CfgNode node : this.allNodes) {
			Property h = node.properties.get("live");
			String termString = node.term.toString(2);
			String propString = h == null ? " " : (h.value == null ? "" : " " + h.value.toString() + " ");
			result.append(node.hashCode() + "[label=\"" + node.id.getId() + " " + termString
					.toString().replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
					.replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'").replace("\"", "\\\"") + " "
					//+ node.interval + " "
					//+ propString.replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
					//		.replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'").replace("\"", "\\\"")
					+ "\"];");
			for (CfgNode child : node.children) {
				result.append(node.hashCode() + "->" + child.hashCode() + ";\n");
			}
		}
		result.append("}\n");
		Program.endTime("graphviz");
		return result.toString();
	}

	public Set<CfgNode> flatten() {
		Program.beginTime("flatten");
		Set<CfgNode> nodes = new HashSet<>();
		for (CfgNode root : roots) {
			nodes.addAll(root.flatten());
		}
		Program.endTime("flatten");
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
		CfgGraph g = new CfgGraph(createCfgs(ast), ast);
		g.removeGhostNodes();
		return g;
	}

	private void removeGhostNodes() {
		Program.beginTime("remove ghost");
		for (CfgNode n : this.allNodes) {
			if (n.isGhost) {
				for (CfgNode p : n.parents) {
					p.children.remove(n);
					p.addChild(n.children);
				}
				for (CfgNode c : n.children) {
					c.parents.remove(n);
				}
				if (this.roots.contains(n)) {
					this.roots.remove(n);
					this.roots.addAll(n.children);
				}
			}
		}
		this.allNodes.removeIf(n -> n.isGhost);
		Program.endTime("remove ghost");
	}

	private static boolean isEmptyPair(Pair<Set<CfgNode>, Set<CfgNode>> p) {
		return p.getLeft().isEmpty() && p.getRight().isEmpty();
	}

	private static Pair<Set<CfgNode>, Set<CfgNode>> patchIfEmpty(Pair<Set<CfgNode>, Set<CfgNode>> p) {
		if (isEmptyPair(p)) {
			Set<CfgNode> tail = new HashSet<>();
			Set<CfgNode> head = new HashSet<>();
			CfgNode ghostNode = new CfgNode();
			tail.add(ghostNode);
			head.add(ghostNode);
			return Pair.of(tail, head);
		} else {
			return p;
		}
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
			result = patchIfEmpty(result);
			result_heads.addAll(result.getLeft());
			result_leaves.addAll(result.getRight());
			list = list.tail();
			while (!list.isEmpty()) {
				Pair<Set<CfgNode>, Set<CfgNode>> new_result = createCfg(list.head());
				new_result = patchIfEmpty(new_result);
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
			s_nr = patchIfEmpty(s_nr);
			result_heads.addAll(s_nr.getLeft());
			result_leaves.addAll(s_nr.getRight());
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Func") && term.getSubtermCount() == 3)) {
			IStrategoTerm _Func_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 1);
			IStrategoTerm r_t = Helpers.at(term, 2);
			Pair<Set<CfgNode>, Set<CfgNode>> stmts_nr = createCfg(stmts_t);
			stmts_nr = patchIfEmpty(stmts_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> r_nr = createCfg(r_t);
			r_nr = patchIfEmpty(r_nr);
			result_heads.addAll(stmts_nr.getLeft());
			for (CfgNode leaf : stmts_nr.getRight()) {
				leaf.addChild(r_nr.getLeft());
			}
			result_leaves.addAll(r_nr.getRight());
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Assign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Assign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			e_nr = patchIfEmpty(e_nr);
			CfgNode _Assign_nb = new CfgNode(getTermNodeId(_Assign_t), _Assign_t);
			result_heads.addAll(e_nr.getLeft());
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(_Assign_nb);
			}
			result_leaves.add(_Assign_nb);
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("DerefAssign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _DerefAssign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			e_nr = patchIfEmpty(e_nr);
			CfgNode _DerefAssign_nb = new CfgNode(getTermNodeId(_DerefAssign_t), _DerefAssign_t);
			result_heads.addAll(e_nr.getLeft());
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(_DerefAssign_nb);
			}
			result_leaves.add(_DerefAssign_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ret") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ret_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			e_nr = patchIfEmpty(e_nr);
			CfgNode _Ret_nb = new CfgNode(getTermNodeId(_Ret_t), _Ret_t);
			result_heads.addAll(e_nr.getLeft());
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(_Ret_nb);
			}
			result_leaves.add(_Ret_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Skip") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Skip_t = term;
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Block") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Block_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 0);
			Pair<Set<CfgNode>, Set<CfgNode>> stmts_nr = createCfg(stmts_t);
			stmts_nr = patchIfEmpty(stmts_nr);
			result_heads.addAll(stmts_nr.getLeft());
			result_leaves.addAll(stmts_nr.getRight());
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("IfThenElse") && term.getSubtermCount() == 3)) {
			IStrategoTerm _IfThenElse_t = term;
			IStrategoTerm c_t = Helpers.at(term, 0);
			IStrategoTerm t_t = Helpers.at(term, 1);
			IStrategoTerm e_t = Helpers.at(term, 2);
			Pair<Set<CfgNode>, Set<CfgNode>> c_nr = createCfg(c_t);
			c_nr = patchIfEmpty(c_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> t_nr = createCfg(t_t);
			t_nr = patchIfEmpty(t_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			e_nr = patchIfEmpty(e_nr);
			result_heads.addAll(c_nr.getLeft());
			for (CfgNode leaf : c_nr.getRight()) {
				leaf.addChild(t_nr.getLeft());
			}
			result_leaves.addAll(t_nr.getRight());
			for (CfgNode leaf : c_nr.getRight()) {
				leaf.addChild(e_nr.getLeft());
			}
			result_leaves.addAll(e_nr.getRight());
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("While") && term.getSubtermCount() == 2)) {
			IStrategoTerm _While_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			IStrategoTerm s_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			e_nr = patchIfEmpty(e_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> s_nr = createCfg(s_t);
			s_nr = patchIfEmpty(s_nr);
			result_heads.addAll(e_nr.getLeft());
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(s_nr.getLeft());
			}
			for (CfgNode leaf : s_nr.getRight()) {
				leaf.addChild(e_nr.getLeft());
			}
			result_leaves.addAll(e_nr.getRight());
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Add") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Add_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			e1_nr = patchIfEmpty(e1_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			e2_nr = patchIfEmpty(e2_nr);
			CfgNode _Add_nb = new CfgNode(getTermNodeId(_Add_t), _Add_t);
			result_heads.addAll(e1_nr.getLeft());
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Add_nb);
			}
			result_leaves.add(_Add_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Sub") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Sub_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			e1_nr = patchIfEmpty(e1_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			e2_nr = patchIfEmpty(e2_nr);
			CfgNode _Sub_nb = new CfgNode(getTermNodeId(_Sub_t), _Sub_t);
			result_heads.addAll(e1_nr.getLeft());
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Sub_nb);
			}
			result_leaves.add(_Sub_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gt") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gt_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			e1_nr = patchIfEmpty(e1_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			e2_nr = patchIfEmpty(e2_nr);
			CfgNode _Gt_nb = new CfgNode(getTermNodeId(_Gt_t), _Gt_t);
			result_heads.addAll(e1_nr.getLeft());
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Gt_nb);
			}
			result_leaves.add(_Gt_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Gte") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Gte_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Pair<Set<CfgNode>, Set<CfgNode>> e1_nr = createCfg(e1_t);
			e1_nr = patchIfEmpty(e1_nr);
			Pair<Set<CfgNode>, Set<CfgNode>> e2_nr = createCfg(e2_t);
			e2_nr = patchIfEmpty(e2_nr);
			CfgNode _Gte_nb = new CfgNode(getTermNodeId(_Gte_t), _Gte_t);
			result_heads.addAll(e1_nr.getLeft());
			for (CfgNode leaf : e1_nr.getRight()) {
				leaf.addChild(e2_nr.getLeft());
			}
			for (CfgNode leaf : e2_nr.getRight()) {
				leaf.addChild(_Gte_nb);
			}
			result_leaves.add(_Gte_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Print") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Print_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Pair<Set<CfgNode>, Set<CfgNode>> e_nr = createCfg(e_t);
			e_nr = patchIfEmpty(e_nr);
			CfgNode _Print_nb = new CfgNode(getTermNodeId(_Print_t), _Print_t);
			result_heads.addAll(e_nr.getLeft());
			for (CfgNode leaf : e_nr.getRight()) {
				leaf.addChild(_Print_nb);
			}
			result_leaves.add(_Print_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Malloc") && term.getSubtermCount() == 0)) {
			IStrategoTerm _Malloc_t = term;
			CfgNode _Malloc_nb = new CfgNode(getTermNodeId(_Malloc_t), _Malloc_t);
			result_heads.add(_Malloc_nb);
			result_leaves.add(_Malloc_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Free") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Free_t = term;
			CfgNode _Free_nb = new CfgNode(getTermNodeId(_Free_t), _Free_t);
			result_heads.add(_Free_nb);
			result_leaves.add(_Free_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Int") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Int_t = term;
			CfgNode _Int_nb = new CfgNode(getTermNodeId(_Int_t), _Int_t);
			result_heads.add(_Int_nb);
			result_leaves.add(_Int_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("True") && term.getSubtermCount() == 0)) {
			IStrategoTerm _True_t = term;
			CfgNode _True_nb = new CfgNode(getTermNodeId(_True_t), _True_t);
			result_heads.add(_True_nb);
			result_leaves.add(_True_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("False") && term.getSubtermCount() == 0)) {
			IStrategoTerm _False_t = term;
			CfgNode _False_nb = new CfgNode(getTermNodeId(_False_t), _False_t);
			result_heads.add(_False_nb);
			result_leaves.add(_False_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ref_t = term;
			CfgNode _Ref_nb = new CfgNode(getTermNodeId(_Ref_t), _Ref_t);
			result_heads.add(_Ref_nb);
			result_leaves.add(_Ref_nb);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Deref") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Deref_t = term;
			CfgNode _Deref_nb = new CfgNode(getTermNodeId(_Deref_t), _Deref_t);
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
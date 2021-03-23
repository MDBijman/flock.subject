package flock.subject.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;

import flock.subject.common.Graph.Node;
import flock.subject.strategies.Program;

public class Graph {
	public static class Node {
		private CfgNodeId id;
		static private CfgNodeId nextId = new CfgNodeId(0);

		public boolean isGhost = false;
		public IStrategoTerm term = null;
		public float interval = 0.0f;
		public HashMap<String, Property> properties = new HashMap<>();

		public Node() {
			this.isGhost = true;
			this.id = nextNodeId();
		}

		public Node(CfgNodeId id) {
			this.id = id;
		}

		public Node(CfgNodeId id, IStrategoTerm t) {
			this.id = id;
			this.term = t;
		}

		public void addProperty(String name, Lattice lat) {
			properties.put(name, new Property(name, lat));
		}

		public Property getProperty(String name) {
			return properties.get(name);
		}

		@Override
		public String toString() {
			return "Node(" + this.getId().toString() + ")";
		}

		@Override
		public int hashCode() {
			return getId().hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Node)) {
				return false;
			}
			
			return this.id.equals(((Node) other).id);
		}

		public CfgNodeId getId() {
			return id;
		}
		
		public static CfgNodeId nextNodeId() {
			CfgNodeId next = Node.nextId;
			Node.nextId = new CfgNodeId(next.getId() + 1);
			return next;
		}
	}
	
	public void validate() {
		for (Node n : this.nodes.values()) {
			if (!this.nodeInterval.containsKey(n)) {
				throw new RuntimeException("Missing interval for node " + n.toString());
			}
			if (!n.isGhost && !this.nodeTerm.containsKey(n)) {
				throw new RuntimeException("Missing term for node " + n.toString());
			}
			if (!this.children.containsKey(n)) {
				throw new RuntimeException("Missing children for node " + n.toString());
			}
			if (!this.parents.containsKey(n)) {
				throw new RuntimeException("Missing parents for node " + n.toString());
			}
			for (Node c : this.children.get(n)) {
				if (c.interval < n.interval) {
					throw new RuntimeException("child interval smaller than parent interval " + n.toString() + n.interval + " - " + c.toString() + c.interval);
				}
			}
		}
	}
	
	/*
	 * Graph Structure fields
	 */

	private HashMap<Node, Set<Node>> children = new HashMap<>();
	private HashMap<Node, Set<Node>> parents = new HashMap<>();
	private HashMap<CfgNodeId, Node> nodes = new HashMap<>();
	private Set<Node> roots = new HashSet<>();
	private Set<Node> leaves = new HashSet<>();

	public Graph() {
		/*Node root = new Node();
		this.roots.add(root);
		this.leaves.add(root);
		this.nodes.put(root.getId(), root);
		this.children.put(root, new HashSet<>());
		this.parents.put(root, new HashSet<>());*/
	}

	public Graph(Node root, IStrategoTerm term) {
		root.term = term;
		this.roots.add(root);
		this.leaves.add(root);
		this.nodes.put(root.getId(), root);
		this.children.put(root, new HashSet<>());
		this.parents.put(root, new HashSet<>());
		this.nodeTerm.put(root, term);
	}
	
	private void mergeChildren(HashMap<Node, Set<Node>> other) {
		for (Entry<Node, Set<Node>> e : other.entrySet()) {
			if (this.children.containsKey(e.getKey())) {
				this.children.get(e.getKey()).addAll(e.getValue());
			} else {
				this.children.put(e.getKey(), e.getValue());
			}
		}
	}

	private void mergeParents(HashMap<Node, Set<Node>> other) {
		for (Entry<Node, Set<Node>> e : other.entrySet()) {
			if (this.parents.containsKey(e.getKey())) {
				this.parents.get(e.getKey()).addAll(e.getValue());
			} else {
				this.parents.put(e.getKey(), e.getValue());
			}
		}
	}
	
	void mergeGraph(Graph o) {
		this.nodes.putAll(o.nodes);
		this.mergeChildren(o.children);
		this.mergeParents(o.parents);
		this.nodeInterval.putAll(o.nodeInterval);
		this.nodeTerm.putAll(o.nodeTerm);
		this.leaves.addAll(o.leaves());
		this.roots.addAll(o.roots());
	}

	public void attachChildGraph(Collection<Node> parents, Graph o) {
		if (o.size() == 0) {
			return;
		}

		this.nodes.putAll(o.nodes);
		this.mergeChildren(o.children);
		this.mergeParents(o.parents);
		this.nodeInterval.putAll(o.nodeInterval);
		this.nodeTerm.putAll(o.nodeTerm);

		for (Node n : parents) {
			for (Node r : o.roots) {
				this.createEdge(n, r);
			}
		}
	}
	
	public void mergeGraph(Collection<Node> parents, Collection<Node> children, Graph o) {
		if (o.size() == 0) {
			for (Node p : parents) {
				for (Node c : children) {
					this.createEdge(p, c);
				}
			}
			return;
		}

		this.nodes.putAll(o.nodes);
		this.mergeChildren(o.children);
		this.mergeParents(o.parents);
		this.nodeInterval.putAll(o.nodeInterval);
		this.nodeTerm.putAll(o.nodeTerm);

		for (Node n : parents) {
			for (Node r : o.roots) {
				this.createEdge(n, r);
			}
		}
		
		for (Node n : children) {
			for (Node r : o.leaves) {
				this.createEdge(r, n);
			}
		}
	}
	
	public Node getNode(CfgNodeId n) {
		return this.nodes.get(n);
	}

	/*
	 * Graph Mutation
	 */

	private Node createOrGetNode(CfgNodeId id) {
		if (!this.nodes.containsKey(id)) {
			Node n = new Node(id);
			this.nodes.put(id, n);
			this.parents.put(n, new HashSet<>());
			this.children.put(n, new HashSet<>());
			return n;
		} else {
			return this.nodes.get(id);
		}
	}

	private void createEdge(Node parent, Node child) {
		this.children.get(parent).add(child);
		this.parents.get(child).add(parent);
	}

	public Set<Node> childrenOf(Node n) {
		return this.children.get(n);
	}

	public Set<Node> parentsOf(Node n) {
		return this.parents.get(n);
	}

	public Collection<Node> nodes() {
		return this.nodes.values();
	}

	public Collection<Node> roots() {
		return this.roots;
	}

	public Collection<Node> leaves() {
		return this.leaves;
	}

	public void removeNode(Node n) {
		if (this.roots.contains(n)) {
			this.roots.remove(n);

			for (Node child : this.children.get(n)) {
				this.roots.add(child);
			}
		}
		
		if (this.leaves.contains(n)) {
			this.leaves.remove(n);

			for (Node child : this.parents.get(n)) {
				this.leaves.add(child);
			}
		}
		
		for (Node child : this.children.get(n)) {
			this.parents.get(child).remove(n);
		}
		for (Node parent : this.parents.get(n)) {
			this.children.get(parent).remove(n);
		}
		for (Node child : this.children.get(n)) {
			for (Node parent : this.parents.get(n)) {
				this.createEdge(parent, child);
			}
		}

		this.children.remove(n);
		this.parents.remove(n);
		this.nodes.remove(n.getId());
	}

	private boolean unroot(Node n) {
		return this.roots.remove(n);
	}

	private boolean unleaf(Node n) {
		return this.leaves.remove(n);
	}

	public void removeNodeAndEdges(Node n) {
		if (this.unroot(n)) {
			this.roots.addAll(this.children.get(n));
		}
		if (this.unleaf(n)) {
			this.leaves.addAll(this.parents.get(n));
		}

		for (Node child : this.children.get(n)) {
			this.parents.get(child).remove(n);
		}
		for (Node parent : this.parents.get(n)) {
			this.children.get(parent).remove(n);
		}

		this.children.remove(n);
		this.parents.remove(n);
		this.nodes.remove(n.getId());
	}

	public void replaceNodes(Set<Node> n, Graph subGraph) {
		Program.beginTime("replacenodev2");
		Set<Node> oldParents = new HashSet<>();
		Set<Node> oldChildren = new HashSet<>();

		boolean containedRoot = n.stream().anyMatch(node -> this.roots.contains(node));
		boolean containedLeaf = n.stream().anyMatch(node -> this.leaves.contains(node));

		// Remove the nodes, and remember the nodes that were connected to the removed
		// set
		for (Node remove : n) {
			oldParents.remove(remove);
			oldChildren.remove(remove);
			oldParents.addAll(this.parentsOf(remove));
			oldChildren.addAll(this.childrenOf(remove));

			this.removeNodeAndEdges(remove);
		}

		// If root was replaced, then sub graph roots are also roots
		if (containedRoot) {
			this.roots.addAll(subGraph.roots);
		}
		// If leaf was replaced, then sub graph leaves are also leaves
		if (containedLeaf) {
			this.leaves.addAll(subGraph.leaves);
		}
		this.mergeGraph(oldParents, oldChildren, subGraph);
		this.updateIntervals(subGraph.nodes.values(), oldParents, oldChildren);
		this.validate();
		Program.endTime("replacenodev2");
	}

	public long size() {
		return this.nodes.size();
	}

	public void removeGhostNodes() {
		Program.beginTime("remove ghost");
		Set<Node> ghostNodes = this.nodes().stream().filter(n -> n.isGhost).collect(Collectors.toSet());
		for (Node n : ghostNodes) {
			this.removeNode(n);
		}
		Program.endTime("remove ghost");
	}

	/*
	 * Terms
	 */

	private HashMap<Node, IStrategoTerm> nodeTerm = new HashMap<>();

	public IStrategoTerm termOf(Node n) {
		return this.nodeTerm.get(n);
	}

	/*
	 * Intervals
	 */

	/*
	 * Interval fields
	 */

	private HashMap<Node, Float> nodeInterval = new HashMap<>();

	/*
	 * Interval calculation and updating
	 */

	public Float intervalOf(Node n) {
		return this.nodeInterval.get(n);
	}

	private void updateIntervals(Collection<Node> newNodes, Set<Node> oldParents, Set<Node> oldChildren) {
		Program.beginTime("updateInterval");
		if (newNodes.size() == 0) {
			return;
		}
		
		float parentMax = oldParents.stream().map(p -> this.intervalOf(p)).max(Float::compare).orElse(0.f);
		float childMin = oldChildren.stream().map(p -> this.intervalOf(p)).min(Float::compare).get();

		// Check if matching, then all newNodes get the common interval
		// If not, take the difference between min of parents and max of children, put
		// newNodes intervals in between as floats

		assert parentMax <= childMin;

		if (parentMax == childMin) {
			for (Node n : newNodes) {
				this.nodeInterval.put(n, parentMax);
			}
		} else {
			float diff = childMin - parentMax;
			
			float newMax = newNodes.stream().map(p -> this.intervalOf(p)).max(Float::compare).get();

			for (Node n : newNodes) {
				float currentInterval = this.intervalOf(n);
				float newInterval = parentMax + (currentInterval / (newMax + 1)) * diff;
				this.nodeInterval.put(n, newInterval);
				n.interval = newInterval;
			}
		}
		Program.endTime("updateInterval");
	}

	public void computeIntervals() {
		Program.beginTime("computeIntervals_v2");
		this.nodeInterval.clear();

		Long next_index = new Long(1);
		Stack<Node> S = new Stack<>();
		HashSet<Node> onStack = new HashSet<>();
		HashMap<Node, Long> lowlink = new HashMap<>();
		HashMap<Node, Long> index = new HashMap<>();
		Set<Set<Node>> components = new HashSet<>();
		HashMap<Node, Set<Node>> nodeComponent = new HashMap<>();

		for (Node n : this.roots) {
			if (index.get(n) == null) {
				strongConnect(n, next_index, S, onStack, lowlink, index, components, nodeComponent);
			}
		}

		// This can be optimized much better
		// Kahn's algorithm applied to the scc's of the graph

		// Setup the edges between the scc's
		// Map from SCC to all successor SCC's
		HashMap<Set<Node>, Set<Set<Node>>> successors = new HashMap<>();
		HashMap<Set<Node>, Set<Set<Node>>> predecessors = new HashMap<>();
		for (Set<Node> set : components) {
			successors.put(set, new HashSet<>());
			predecessors.put(set, new HashSet<>());
		}

		for (Node n : this.nodes.values()) {
			for (Node c : this.childrenOf(n)) {
				Set<Node> nComponent = nodeComponent.get(n);
				Set<Node> cComponent = nodeComponent.get(c);
				if (nComponent != cComponent) {
					successors.get(nComponent).add(cComponent);
					predecessors.get(cComponent).add(nComponent);
				}
			}
		}

		Set<Set<Node>> noOutgoing = new HashSet<>();

		// Find component with no successors (i.e. no outgoing edge)
		for (Set<Node> component : components) {
			if (successors.get(component).isEmpty()) {
				noOutgoing.add(component);
			}
		}

		long currIndex = components.size() + 1;
		while (!components.isEmpty()) {
			Set<Node> c = noOutgoing.iterator().next();
			noOutgoing.remove(c);

			// Remove component and set interval of its nodes
			components.remove(c);
			for (Set<Node> p : predecessors.get(c)) {
				successors.get(p).remove(c);
				if (successors.get(p).size() == 0)
					noOutgoing.add(p);
			}

			successors.remove(c);
			predecessors.remove(c);

			for (Node n : c) {
				this.nodeInterval.put(n, (float) currIndex);
				n.interval = (float) currIndex;
			}
			currIndex--;
		}
		Program.endTime("computeIntervals_v2");
	}

	private void strongConnect(Node v, Long next_index, Stack<Node> S, HashSet<Node> onStack,
			HashMap<Node, Long> lowlink, HashMap<Node, Long> index, Set<Set<Node>> components,
			HashMap<Node, Set<Node>> nodeComponent) {
		index.put(v, next_index);
		lowlink.put(v, next_index);
		next_index += 1;
		S.push(v);
		onStack.add(v);
		for (Node w : this.childrenOf(v)) {
			if (index.get(w) == null) {
				strongConnect(w, next_index, S, onStack, lowlink, index, components, nodeComponent);
				if (lowlink.get(w) < lowlink.get(v)) {
					lowlink.put(v, lowlink.get(w));
				}
			} else {
				if (onStack.contains(w)) {
					if (index.get(w) < lowlink.get(v)) {
						lowlink.put(v, index.get(w));
					}
				}
			}
		}
		if (lowlink.get(v) == index.get(v)) {
			Node w;
			HashSet<Node> component = new HashSet<>();
			do {
				w = S.pop();
				onStack.remove(w);
				nodeComponent.put(w, component);
				component.add(w);
			} while (w != v);
			components.add(component);
		}
	}

	/*
	 * Helpers
	 */

	public String toGraphviz() {
		Program.beginTime("graphviz");
		StringBuilder result = new StringBuilder();
		result.append("digraph G { ");
		for (Node node : this.nodes.values()) {
			Property h = node.properties.get("live");
			String termString = node.term.toString(1).replace("\\", "\\\\").replace("\t", "\\t").replace("\b",
					"\\b").replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'").replace("\"",
					"\\\"");
			String rootString = this.roots.contains(node) ? "root: " : "";
			String leafString = this.leaves.contains(node) ? "leaf: " : "";
			String intervalString = this.nodeInterval.containsKey(node) ? this.intervalOf(node).toString() : "";
			String propString = (h == null ? "" : (h.lattice.value() == null ? "" : h.lattice.value().toString())).replace("\\", "\\\\").replace("\t", "\\t").replace("\b",
					"\\b").replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'").replace("\"",
					"\\\"");
			
			result.append(node.getId().getId() + "[label=\"" + rootString + leafString + node.getId().getId() + " - "
					+ intervalString + " " + termString + " " + propString + "\"];");

			for (Node child : this.childrenOf(node)) {
				result.append(node.getId().getId() + "->" + child.getId().getId() + "; ");
			}
		}
		result.append("} ");
		Program.printDebug(result.toString());
		Program.endTime("graphviz");
		return result.toString();
	}
	
	public static Graph createCfg(IStrategoTerm term) {
		Graph result_graph = new Graph();
		if (TermUtils.isList(term)) {
			IStrategoList list = M.list(term);
			if (list.isEmpty()) {
				return new Graph();
			}
			result_graph.mergeGraph(createCfg(list.head()));
			list = list.tail();
			while (!list.isEmpty()) {
				Graph new_result = createCfg(list.head());
				result_graph.attachChildGraph(result_graph.leaves, new_result);
				result_graph.leaves = new_result.leaves;
				list = list.tail();
			}
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Root") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Root_t = term;
			IStrategoTerm s_t = Helpers.at(term, 0);
			Graph s_nr = createCfg(s_t);
			result_graph.mergeGraph(s_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(s_nr.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Func") && term.getSubtermCount() == 3)) {
			IStrategoTerm _Func_t = term;
			IStrategoTerm stmts_t = Helpers.at(term, 1);
			IStrategoTerm r_t = Helpers.at(term, 2);
			Graph stmts_nr = createCfg(stmts_t);
			Graph r_nr = createCfg(r_t);
			result_graph.mergeGraph(stmts_nr);
			result_graph.attachChildGraph(stmts_nr.leaves, r_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(r_nr.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Assign") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Assign_t = term;
			IStrategoTerm n_t = Helpers.at(term, 0);
			IStrategoTerm e_t = Helpers.at(term, 1);
			Graph e_nr = createCfg(e_t);
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
			Graph e_nr = createCfg(e_t);
			Graph _DerefAssign_nb = new Graph(getTermNode(_DerefAssign_t), _DerefAssign_t);
			result_graph.mergeGraph(e_nr);
			result_graph.attachChildGraph(e_nr.leaves, _DerefAssign_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_DerefAssign_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Ret") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Ret_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Graph e_nr = createCfg(e_t);
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
			Graph stmts_nr = createCfg(stmts_t);
			result_graph.mergeGraph(stmts_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(stmts_nr.leaves);
		} else if (TermUtils.isAppl(term)
				&& (M.appl(term).getName().equals("IfThenElse") && term.getSubtermCount() == 3)) {
			IStrategoTerm _IfThenElse_t = term;
			IStrategoTerm c_t = Helpers.at(term, 0);
			IStrategoTerm t_t = Helpers.at(term, 1);
			IStrategoTerm e_t = Helpers.at(term, 2);
			Graph c_nr = createCfg(c_t);
			Graph t_nr = createCfg(t_t);
			Graph e_nr = createCfg(e_t);
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
			Graph e_nr = createCfg(e_t);
			Graph s_nr = createCfg(s_t);
			result_graph.mergeGraph(e_nr);
			result_graph.attachChildGraph(e_nr.leaves, s_nr);
			result_graph.attachChildGraph(s_nr.leaves, e_nr);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(e_nr.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Add") && term.getSubtermCount() == 2)) {
			IStrategoTerm _Add_t = term;
			IStrategoTerm e1_t = Helpers.at(term, 0);
			IStrategoTerm e2_t = Helpers.at(term, 1);
			Graph e1_nr = createCfg(e1_t);
			Graph e2_nr = createCfg(e2_t);
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
			Graph e1_nr = createCfg(e1_t);
			Graph e2_nr = createCfg(e2_t);
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
			Graph e1_nr = createCfg(e1_t);
			Graph e2_nr = createCfg(e2_t);
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
			Graph e1_nr = createCfg(e1_t);
			Graph e2_nr = createCfg(e2_t);
			Graph _Gte_nb = new Graph(getTermNode(_Gte_t), _Gte_t);
			result_graph.mergeGraph(e1_nr);
			result_graph.attachChildGraph(e1_nr.leaves, e2_nr);
			result_graph.attachChildGraph(e2_nr.leaves, _Gte_nb);
			result_graph.leaves = new HashSet<>();
			result_graph.leaves.addAll(_Gte_nb.leaves);
		} else if (TermUtils.isAppl(term) && (M.appl(term).getName().equals("Print") && term.getSubtermCount() == 1)) {
			IStrategoTerm _Print_t = term;
			IStrategoTerm e_t = Helpers.at(term, 0);
			Graph e_nr = createCfg(e_t);
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

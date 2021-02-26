package flock.subject.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.tuple.Pair;

import flock.subject.strategies.Program;
public class Graph {
	public class Node {
		CfgNodeId id;

		public Node(CfgNodeId id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "Node(" + this.id.toString() + ")";
		}
	}

	/*
	 * Graph Structure fields
	 */

	private HashMap<Node, Set<Node>> children = new HashMap<>();
	private HashMap<Node, Set<Node>> parents = new HashMap<>();
	private HashMap<CfgNodeId, Node> nodes = new HashMap<>();
	private Set<Node> roots = new HashSet<>();

	public Graph(Node root) {
		this.roots.add(root);
		this.nodes.put(root.id, root);
		this.children.put(root, new HashSet<>());
		this.parents.put(root, new HashSet<>());
	}

	public Graph(CfgNode root) {
		// Create root
		roots.add(this.createOrGetNode(root.id));

		// Create adjacency list
		Set<CfgNode> nodes = root.flatten();

		for (CfgNode n : nodes) {
			Node newNode = this.createOrGetNode(n.id);

			for (CfgNode child : n.getChildren()) {
				Node newChild = this.createOrGetNode(child.id);
				this.createEdge(newNode, newChild);
			}

			for (CfgNode parent : n.getParents()) {
				Node newParent = this.createOrGetNode(parent.id);
				this.createEdge(newParent, newNode);
			}
		}

		this.computeIntervals();
	}

	private void mergeGraph(Graph o) {
		this.nodes.putAll(o.nodes);
		this.children.putAll(o.children);
		this.parents.putAll(o.parents);
		this.nodeInterval.putAll(o.nodeInterval);
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

	public void removeNode(Node n) {
		if (this.roots.contains(n)) {
			this.roots.remove(n);

			Set<Node> children = this.children.remove(n);
			this.parents.remove(n);

			for (Node child : children) {
				this.roots.add(child);
				this.parents.get(child).remove(n);
			}
		} else {
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
		}
		this.nodes.remove(n.id);

		this.computeIntervals();
	}

	public void removeNodeAndEdges(Node n) {
		if (roots.contains(n)) {
			roots.remove(n);
			roots.addAll(this.children.get(n));
		}

		for (Node child : this.children.get(n)) {
			this.parents.get(child).remove(n);
		}
		for (Node parent : this.parents.get(n)) {
			this.children.get(parent).remove(n);
		}

		this.children.remove(n);
		this.parents.remove(n);
		this.nodes.remove(n.id);
	}

	public void replaceNodes(Set<Node> n, Pair<Set<CfgNode>, Set<CfgNode>> subGraph) {
		Program.beginTime("replacenodev2");
		Set<Node> oldParents = new HashSet<>();
		Set<Node> oldChildren = new HashSet<>();

		boolean containedRoot = n.stream().anyMatch(node -> this.roots.contains(node));

		// Remove the nodes, and remember the nodes that were connected
		for (Node remove : n) {
			oldParents.remove(remove);
			oldChildren.remove(remove);
			oldParents.addAll(this.parentsOf(remove));
			oldChildren.addAll(this.childrenOf(remove));

			this.removeNodeAndEdges(remove);
		}

		// Creating the new <roots, leaves> structure
		// Remove when CfgNode code is removed
		if (subGraph.getLeft().size() != 1)
			Program.printDebug("Warning: Subgraph with more than 1 root!");

		CfgNode root = subGraph.getLeft().iterator().next();
		Graph subGraphWithNodes = new Graph(new Node(root.id));
		Set<Node> subGraphLeaves = new HashSet<>();

		for (CfgNode s : root.flatten()) {
			Node sNode = subGraphWithNodes.createOrGetNode(s.id);

			for (CfgNode child : s.children) {
				subGraphWithNodes.createEdge(sNode, subGraphWithNodes.createOrGetNode(child.id));
			}
		}

		for (CfgNode leaf : subGraph.getRight()) {
			subGraphLeaves.add(subGraphWithNodes.createOrGetNode(leaf.id));
		}

		subGraphWithNodes.computeIntervals();
		this.mergeGraph(subGraphWithNodes);
		//

		// If root was replaced, then sub graph roots are also roots
		if (containedRoot) {
			this.roots.addAll(subGraphWithNodes.roots);
		}

		// Stitch together the old parents and children of the removed nodes with the
		// new nodes
		for (Node oldParent : oldParents) {
			for (Node subGraphRoot : subGraphWithNodes.roots) {
				this.createEdge(oldParent, subGraphRoot);
			}
		}

		for (Node oldChild : oldChildren) {
			for (Node subGraphLeaf : subGraphLeaves) {
				this.createEdge(subGraphLeaf, oldChild);
			}
		}

		this.updateIntervals(subGraphWithNodes.nodes.values(), oldParents, oldChildren);
		//Program.printDebug(this.toGraphviz());
		Program.endTime("replacenodev2");
	}

	public long size() {
		return this.nodes.size();
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

	public float intervalOf(Node n) {
		return this.nodeInterval.get(n);
	}

	private void updateIntervals(Collection<Node> newNodes, Set<Node> oldParents, Set<Node> oldChildren) {
		Program.beginTime("updateInterval");
		float parentMax = oldParents
			.stream()
			.map(p -> this.intervalOf(p))
			.max(Float::compare).get();
		
		float childMin = oldChildren
			.stream()
			.map(p -> this.intervalOf(p))
			.min(Float::compare).get();
		
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
			
			float newMax = newNodes
				.stream()
				.map(p -> this.intervalOf(p))
				.max(Float::compare).get();
			
			for (Node n : newNodes) {
				float currentInterval = this.nodeInterval.get(n);
				float newInterval = parentMax + (currentInterval / (newMax + 1)) * diff;
				this.nodeInterval.put(n, newInterval);
			}
		}
		Program.endTime("updateInterval");
	}

	private void computeIntervals() {
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
			// Property h = node.properties.get("live");
			// String termString = node.term.toString(2);
			// String propString = h == null ? " " : (h.value == null ? "" : " " +
			// h.value.toString() + " ");
			String rootString = this.roots.contains(node) ? "root: " : "";
			String intervalString = this.nodeInterval.containsKey(node) ? this.nodeInterval.get(node).toString() : "";
			result.append(
					node.id.getId() + "[label=\"" + rootString + node.id.getId() + " - " + intervalString + "\"];");
			// + node.interval + " "
			// + propString.replace("\\", "\\\\").replace("\t", "\\t").replace("\b",
			// "\\b").replace("\n", "\\n")
			// .replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'").replace("\"",
			// "\\\"")

			for (Node child : this.children.get(node)) {
				result.append(node.id.getId() + "->" + child.id.getId() + "; ");
			}
		}
		result.append("} ");
		Program.endTime("graphviz");
		return result.toString();
	}
}

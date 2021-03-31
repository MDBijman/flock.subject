package flock.subject.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.Context;

import flock.subject.common.Graph.Node;
import flock.subject.common.Lattice.LatticeWithDependencies;
import flock.subject.live.Live;
import flock.subject.strategies.Program;
import flock.subject.value.ValueFlowAnalysis;

public abstract class Analysis {
	public enum Direction {
		FORWARD, BACKWARD
	}

	public final String name;
	public final String propertyName;
	public final Direction direction;
	public HashSet<Node> dirtyNodes = new HashSet<>();
	public HashSet<Node> newNodes = new HashSet<>();
	private boolean hasRunOnce = false;

	public Analysis(String name, Direction dir) {
		this.name = name;
		this.propertyName = name;
		this.direction = dir;
	}

	public Analysis(String name, String propertyName, Direction dir) {
		this.name = name;
		this.propertyName = propertyName;
		this.direction = dir;
	}
	
	public void addToDirty(Node n) {
		this.dirtyNodes.add(n);
	}

	public void addToNew(Node n) {
		this.newNodes.add(n);
	}

	public void remove(Graph g, Set<Node> nodes) {
		this.dirtyNodes.removeAll(nodes);
		this.newNodes.removeAll(nodes);
		
		for (Node node : nodes) {
			this.dependents.remove(node);
		}

		for (Set<Dependency> s : this.dependents.values()) {
			for (Node node : nodes) {
				s.remove(new Dependency(node.getId()));
			}
		}
		
		for (Node n : g.nodes()) {
			if (n.isGhost) {
				continue;
			}
			
			Property prop = n.getProperty(this.propertyName);
			
			if (prop == null) {
				continue;
			}
			
		}
	}

	public void clear() {
		this.newNodes.clear();
		this.dirtyNodes.clear();
		this.dependents.clear();
	}

	/*
	 * Analysis Logic
	 */

	public void updateUntilBoundary(Graph graph, Node node) {
		float boundary = graph.intervalOf(node);
		Set<Node> dirtyNodes = new HashSet<>(this.dirtyNodes);

		for (Node n : this.dirtyNodes) {
			dirtyNodes.addAll(getTermDependencies(graph, n));
		}
		for (Node n : this.newNodes) {
			dirtyNodes.addAll(getTermDependencies(graph, n));
		}

		if (!this.hasRunOnce) {
			this.performDataAnalysis(graph, graph.roots(), this.newNodes, dirtyNodes, boundary);
			this.hasRunOnce = true;
		} else {
			this.updateDataAnalysis(graph, this.newNodes, dirtyNodes, boundary);
		}

		removeNodesUntilBoundary(graph, boundary);
	}

	private void removeNodesUntilBoundary(Graph graph, float boundary) {
		if (this.direction == Direction.FORWARD) {
			this.dirtyNodes.removeIf(n -> graph.intervalOf(n) <= boundary);
			this.newNodes.removeIf(n -> graph.intervalOf(n) <= boundary);
		} else if (this.direction == Direction.BACKWARD) {
			this.dirtyNodes.removeIf(n -> graph.intervalOf(n) >= boundary);
			this.newNodes.removeIf(n -> graph.intervalOf(n) >= boundary);
		}
	}

	/*
	 * Analysis specific methods
	 */
	
	// FIXME autogenerate and specialise to analysis
	// Add this as abstract method to generated analysis file and invoke
	public static Set<Node> getTermDependencies(Graph g, Node n) {
		Set<Node> r = new HashSet<>();
		if (TermUtils.isAppl(g.termOf(n), "Int", 1)) {
			r.addAll(g.parentsOf(n));
		}
		for (Node p : g.childrenOf(n)) {
			if (TermUtils.isAppl(g.termOf(p), "Assign", -1)) {
				r.add(p);
			}
		}
		return r;
	}

	// FIXME use dependents

	// Maps node to all the nodes that depend on it
	private HashMap<CfgNodeId, Set<Dependency>> dependents = new HashMap<>();
	
	public void initDependents(Graph g) {
		for (Node n : g.nodes()) {
			if (n.isGhost) {
				continue;
			}
			
			Property prop = n.getProperty(this.propertyName);
			
			if (prop == null) {
				continue;
			}
			
			LatticeWithDependencies l = (LatticeWithDependencies) prop.lattice;
			
			if (l == null) {
				throw new RuntimeException("Lattice doesn't have dependency tracking: " + this.name);
			}
			
			for (Dependency d : l.dependencies()) {
				dependents.putIfAbsent(d.id, new HashSet<>());
				dependents.get(d.id).add(new Dependency(n.getId()));
			}
		}
	}
	
	public void removeFacts(Graph g, CfgNodeId origin) {
		initDependents(g);
		Set<Dependency> deps = dependents.get(origin);
		if (deps == null) return;
		
		for (Dependency d : deps) {
			Node n = g.getNode(d.id);
			
			if (n.isGhost) {
				continue;
			}
			
			Property prop = n.getProperty(this.propertyName);
			
			if (prop == null) {
				continue;
			}
			
			LatticeWithDependencies l = (LatticeWithDependencies) prop.lattice;
			
			if (l == null) {
				throw new RuntimeException("Lattice doesn't have dependency tracking: " + this.name);
			}
			
			l.removeByDependency(new Dependency(origin));
			this.addToDirty(n);
		}
	}
	
	/*
	 * Analysis implementation
	 */
	
	public void performDataAnalysis(Graph g, Node root) {
		HashSet<Node> nodeset = new HashSet<Node>();
		nodeset.add(root);
		performDataAnalysis(g, new HashSet<Node>(), nodeset);
	}

	public void performDataAnalysis(Graph g, Collection<Node> nodeset) {
		performDataAnalysis(g, new HashSet<Node>(), nodeset);
	}

	public void performDataAnalysis(Graph g) {
		performDataAnalysis(g, g.roots(), g.nodes());
	}

	public void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset) {
		performDataAnalysis(g, roots, nodeset, new HashSet<Node>());
	}

	public void updateDataAnalysis(Graph g, Collection<Node> news, Collection<Node> dirty) {
		performDataAnalysis(g, new HashSet<Node>(), news, dirty);
	}

	public void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset, Collection<Node> dirty) {
		if (this.direction == Direction.BACKWARD) {
			performDataAnalysis(g, roots, nodeset, dirty, -Float.MAX_VALUE);
		} else if (this.direction == Direction.FORWARD) {
			performDataAnalysis(g, roots, nodeset, dirty, Float.MAX_VALUE);
		}
	}
	
	public void updateDataAnalysis(Graph g, Collection<Node> news, Collection<Node> dirty, float intervalBoundary) {
		performDataAnalysis(g, new HashSet<Node>(), news, dirty, intervalBoundary);
	}
	
	public abstract void performDataAnalysis(Graph g, Collection<Node> roots, Collection<Node> nodeset,
			Collection<Node> dirty, float intervalBoundary);
}

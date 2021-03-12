package flock.subject.strategies;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;

import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.common.Analysis;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Graph;
import flock.subject.common.Graph.Node;
import flock.subject.common.Lattice;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;

public class Program {
	public static Program instance = new Program();

	public IOAgent io;
	public Graph graph;
	public Analysis analysis;

	public Program() {
		this.analysis = new Analysis();
	}
	
	public void createControlFlowGraph(Context context, IStrategoTerm current) {
		this.io = context.getIOAgent();
		this.graph = Graph.createCfg(current);
		this.graph.removeGhostNodes();
		this.graph.computeIntervals();
		this.graph.validate();
		this.update(current);
		initPosition(graph, context.getFactory());
	}

	public Node getNode(CfgNodeId id) {
		return graph.getNode(id);
	}

	public void replaceNode(Context context, IStrategoTerm current, IStrategoTerm replacement) {
		Program.increment("replaceNode");
		Program.beginTime("replaceNode");
		Program.beginTime("replaceNode remove facts");
		
		// Nodes outside of the removed id's that have analysis results directly depending on them (static analysis)
		Set<Node> dependents = new HashSet<>();

		Node currentNode = Graph.getTermNode(current);
		if (currentNode != null && this.graph.getNode(currentNode.getId()) != null) {
			dependents.addAll(Analysis.getTermDependencies(this.graph, currentNode));
		}

		// Set of id's that were in the removed node
		Set<Node> removedNodes = getAllNodes(current);	
		removedNodes = removedNodes
				.stream()
				.filter(n -> this.graph.getNode(n.getId()) != null)
				.collect(Collectors.toSet());

		// Add removed id's to that set
		for (Node n : removedNodes) {
			dependents.add(n);
		}

		// Go through graph and remove facts with origin in removed id's
		for (Node n : this.graph.nodes()) {
			boolean changed = false;
			for (Node d : dependents) {
				boolean isRemoved = Analysis.removeFact(context, n, d.getId());
				changed |= isRemoved;
			}
			if (changed) {
				analysis.addToDirty(n);
			}
		}
		
		analysis.removeFromDirty(removedNodes);
		analysis.removeFromNew(removedNodes);

		Program.endTime("replaceNode remove facts");

		// Here we patch the graph
		Program.beginTime("replaceNode fix cfg");
		Graph subGraph = Graph.createCfg(replacement);
		subGraph.removeGhostNodes();
		subGraph.computeIntervals();
		this.graph.replaceNodes(removedNodes, subGraph);
		
		for (Node n : subGraph.nodes()) {
			this.analysis.addToNew(n);
		}
		
		//this.allNodes.addAll(newNodes);
		Program.endTime("replaceNode fix cfg");
		Program.endTime("replaceNode");
	}

	public void removeNode(Context context, IStrategoTerm node) {
		
		Program.increment("removeNode");
		Program.log("api", "removing node " + node.toString());
		// Nodes outside of the removed id's that have analysis results directly depending on them (static analysis)
		Set<Node> dependents = new HashSet<>();
		
		Node currentNode = Graph.getTermNode(node);
		if (currentNode != null && this.graph.getNode(currentNode.getId()) != null) {
			dependents.addAll(Analysis.getTermDependencies(this.graph, currentNode));
		}

		// Set of id's that were in the removed node
		Set<Node> removedNodes = getAllNodes(node);
		
		analysis.removeFromDirty(removedNodes);
		analysis.removeFromNew(removedNodes);
		
		// Add removed id's to that set
		for (Node n : removedNodes) {
			dependents.add(n);
		}

		// Go through graph and remove facts with origin in removed id's
		for (Node n : this.graph.nodes()) {
			if (removedNodes.contains(n)) {
				n.isGhost = true;
			} else {
				boolean changed = false;
				for (Node d : dependents) {
					boolean isRemoved = Analysis.removeFact(context, n, d.getId());
					changed |= isRemoved;
				}
				if (changed) {
					analysis.addToDirty(n);
				}
			}
		}
		this.graph.removeGhostNodes();
	}
	
	private void initPosition(Graph g, ITermFactory factory) {
		int i = 0;
		for (Node n : g.nodes()) {
			n.addProperty("position", new PositionLattice(factory.makeInt(i)));
			i += 1;
		}
	}

	public CfgNodeId nextNodeId() {
		return Graph.Node.nextNodeId();
	}

	public void init(Context context, IStrategoTerm program) {
		this.analysis = new Analysis();
		for (Node n : this.graph.nodes()) {
			analysis.addToNew(n);
		}
		update(program);
	}

	public void update(IStrategoTerm program) {
		Program.beginTime("update");
		for (Node node : this.graph.nodes()) {
			node.interval = this.graph.intervalOf(node);
		}
		setNodeTerms(program);
		Program.endTime("update");
	}

	private void setNodeTerms(IStrategoTerm term) {
		Node id = Graph.getTermNode(term);
		if (id != null && this.graph.getNode(id.getId()) != null) {
			this.graph.getNode(id.getId()).term = term; 
		}
		
		for (IStrategoTerm subterm : term.getSubterms()) {
			setNodeTerms(subterm);
		}
	}
	
	private Set<Node> getAllNodes(IStrategoTerm program) {
		HashSet<Node> set = new HashSet<>();
		getAllNodes(set, program);
		return set;
	}

	private void getAllNodes(Set<Node> visited, IStrategoTerm program) {
		for (IStrategoTerm term : program.getSubterms()) {
			getAllNodes(visited, term);
		}
		Node id = Graph.getTermNode(program);
		if (id != null) {
			visited.add(id);
		}
	}
	
	public static void printDebug(String t) {
		instance.io.printError(t);
	}
	
	private static String[] enabled = {
		"time",
		"count",
		//"incremental",
		//"validation",
		//"api",
		//"graphviz"
	};
	private static HashSet<String> enabledTags = new HashSet<>(Arrays.asList(enabled));
	
	public static boolean isLogEnabled(String tag) {
		return enabledTags.contains(tag);
	}
	
	public static void log(String tag, String message) {
		if (enabledTags.contains(tag)) {
			printDebug("[" + tag + "]" + " " + message);
		}
	}
	
	private static HashMap<String, Long> runningMap = new HashMap<>();
	private static HashMap<String, Long> cumulMap = new HashMap<>();
	public static void beginTime(String tag) {
		runningMap.put(tag, System.currentTimeMillis());
		cumulMap.putIfAbsent(tag, 0L);
	}
	
	public static long endTime(String tag) {
		long t = System.currentTimeMillis() - runningMap.get(tag);
		runningMap.remove(tag);
		cumulMap.put(tag, cumulMap.get(tag) + t);
		return t;
	}
	
	public static void logTime(String tag) {
		Program.log("time", "time " + tag + ": " + cumulMap.get(tag));
	}
	
	public static void logTimers() {
		for (Entry<String, Long> e : cumulMap.entrySet()) {
			Program.log("time", e.getKey() + ": " + e.getValue());
		}
	}
	
	private static HashMap<String, Long> countMap = new HashMap<>();
	public static void increment(String tag) {
		countMap.putIfAbsent(tag, 0L);
		countMap.put(tag, countMap.get(tag) + 1);
	}

	public static void logCounts() {
		for (Entry<String, Long> e : countMap.entrySet()) {
			Program.log("count", e.getKey() + ": " + e.getValue());
		}
	}
}

class PositionLattice extends Lattice {
	
	IStrategoInt value;
	
	PositionLattice(IStrategoInt v) {
		this.value = v;
	}

	@Override
	public Lattice lub(Lattice o) {
		return null;
	}

	@Override
	public Object value() {
		return this.value;
	}

}
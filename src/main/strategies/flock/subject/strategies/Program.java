package flock.subject.strategies;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;

import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.common.CfgGraph;
import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Lattice;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;

public class Program {
	public static Program instance = new Program();

	public CfgGraph graph;
	public IOAgent io;

	public void createControlFlowGraph(Context context, IStrategoTerm current) {
		this.io = context.getIOAgent();
		this.graph = CfgGraph.createControlFlowGraph(current);
		// io.printError(graph.toGraphviz().replace("\n", "\t"));
		initPosition(graph, context.getFactory());
	}

	public void runValueAnalysis() {
		ValueFlowAnalysis.performDataAnalysis(graph);
	}

	public void runLiveVariableAnalysis() {
		LiveVariablesFlowAnalysis.performDataAnalysis(graph);
	}

	public void runPointsToAnalysis() {
		PointsToFlowAnalysis.performDataAnalysis(graph);
	}

	public CfgNode getCfgNode(CfgNodeId id) {
		return graph.getCfgNode(id);
	}

	public void replaceNode(Context context, IStrategoTerm current, IStrategoTerm replacement) {
		graph.replaceNode(context, current, replacement);
	}

	public void removeNode(Context context, IStrategoTerm current) {
		graph.removeNode(context, current);
	}
	
	private void initPosition(CfgGraph g, ITermFactory factory) {
		int i = 0;
		for (CfgNode n : g.flatten()) {
			n.addProperty("position", new PositionLattice());
			n.getProperty("position").value = factory.makeInt(i);
			i += 1;
		}
	}

	public CfgNodeId nextNodeId() {
		return CfgGraph.nextNodeId();
	}

	public void init(Context context, IStrategoTerm program) {
		graph.init(context, program);
	}

	public void update(Context context, IStrategoTerm program) {
		graph.update(context, program);
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

	@Override
	public Object bottom() {
		return null;
	}

	@Override
	public Object lub(Object l, Object r) {
		return null;
	}

}
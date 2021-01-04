package flock.subject.strategies;

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
		graph.replaceNode(context, graph.getCfgNode(CfgGraph.getTermNodeId(current)), replacement);
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

	public void update(Context context, IStrategoTerm program) {
		graph.update(context, program);
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
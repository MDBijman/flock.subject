package flock.subject.strategies.analysis;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.common.CfgGraph;
import flock.subject.common.CfgNode;
import flock.subject.common.Lattice;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;

import org.spoofax.terms.ParseError;

public class analyse_program_0_0 extends Strategy {
	
	public static analyse_program_0_0 instance = new analyse_program_0_0();
	
	CfgGraph graph;
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        ITermFactory factory = context.getFactory();
		
		try {
			context.getIOAgent().printError("Start Analysis");
			context.getIOAgent().printError("Creating CFG");
			this.graph = CfgGraph.createControlFlowGraph(current);
			
			initPosition(graph, factory);
			
			context.getIOAgent().printError("Running value analysis");
			ValueFlowAnalysis.performDataAnalysis(graph);
			context.getIOAgent().printError("Running liveness analysis");
			LiveVariablesFlowAnalysis.performDataAnalysis(graph);
			context.getIOAgent().printError("Running points-to analysis");			
			PointsToFlowAnalysis.performDataAnalysis(graph);
		
		} catch (ParseError e) {
			context.getIOAgent().printError(e.toString());
			return null;
		}
		
        return current; 
    }
	
	public CfgNode getCFGNode(IStrategoTerm t) {
		return graph.getTermNode(t);
	}

	private void initPosition(CfgGraph g, ITermFactory factory) {
		int i = 0;
		for (CfgNode n : g.flatten()) {
			n.addProperty("position", new PositionLattice());
			n.getProperty("position").value = factory.makeInt(i);
			i += 1;
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
package flock.subject.strategies.analysis;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.common.CfgNode;
import flock.subject.common.Lattice;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.strategies.Program;
import flock.subject.value.ValueFlowAnalysis;

import org.spoofax.terms.ParseError;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoInt;

public class analyse_program_0_0 extends Strategy {
	
	public static analyse_program_0_0 instance = new analyse_program_0_0();
		
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        ITermFactory factory = context.getFactory();
		
		try {
			context.getIOAgent().printError("Start Analysis");
			context.getIOAgent().printError("Creating CFG");
			
			Program.instance.createControlFlowGraph(context, current);
			Program.log("graphviz", Program.instance.graph.toGraphviz());
			Program.log("api", "analyse_program");
			Program.instance.init(context, current);

			
		} catch (ParseError e) {
			context.getIOAgent().printError(e.toString());
			return null;
		}
		
        return current; 
    }
}


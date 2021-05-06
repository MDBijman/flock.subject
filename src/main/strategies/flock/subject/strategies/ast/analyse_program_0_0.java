package flock.subject.strategies.ast;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.ParseError;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.Program;

public class analyse_program_0_0 extends Strategy {
	
	public static analyse_program_0_0 instance = new analyse_program_0_0();
		
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current) {
        ITermFactory factory = context.getFactory();
		
		try {
			context.getIOAgent().printError("Start Analysis");
			context.getIOAgent().printError("Creating CFG");
			Program.resetTimers();
			Program.beginTime("api@analyse");
			Program.instance.createControlFlowGraph(context, current);
			Program.log("graphviz", Program.instance.graph.toGraphviz());
			Program.log("api", "analyse_program");
			Program.instance.init(context, current);
			Program.endTime("api@analyse");

			
		} catch (ParseError e) {
			context.getIOAgent().printError(e.toString());
			return null;
		}
		
        return current; 
    }
}


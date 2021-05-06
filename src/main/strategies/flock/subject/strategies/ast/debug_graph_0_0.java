package flock.subject.strategies.ast;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.Program;

public class debug_graph_0_0 extends Strategy {
	
	public static debug_graph_0_0 instance = new debug_graph_0_0();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm program) {
        Program.log("debug", Program.instance.graph.toGraphviz().replace("\n", "\t"));
        Program.logTimers();
        Program.logCounts();
        return program;
    }
}
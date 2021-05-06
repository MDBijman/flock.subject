package flock.subject.strategies.ast;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.Program;

public class update_0_0 extends Strategy {
	
	public static update_0_0 instance = new update_0_0();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm program) {
        ITermFactory factory = context.getFactory();
        
        Program.beginTime("api@update");
        
        Program.log("api", "[update] " + program.toString());
        if (Program.isLogEnabled("graphviz"))
        	Program.log("graphviz", "at [update] " + Program.instance.graph.toGraphviz().replace("\n", "\t"));
		Program.instance.update(program);

        Program.endTime("api@update");
		
        return program;
    }
}
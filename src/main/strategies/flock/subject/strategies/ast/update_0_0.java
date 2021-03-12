package flock.subject.strategies.ast;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.util.TermUtils;
import org.spoofax.terms.TermFactory;

import java.io.FileWriter;
import java.io.IOException;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.SetUtils;
import flock.subject.strategies.Program;
import flock.subject.strategies.analysis.analyse_program_0_0;

import org.spoofax.terms.ParseError;

public class update_0_0 extends Strategy {
	
	public static update_0_0 instance = new update_0_0();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm program) {
        ITermFactory factory = context.getFactory();
        
        Program.log("api", "[update] " + program.toString());
        if (Program.isLogEnabled("graphviz"))
        	Program.log("graphviz", "at [update] " + Program.instance.graph.toGraphviz().replace("\n", "\t"));
		Program.instance.update(program);

        return program;
    }
}
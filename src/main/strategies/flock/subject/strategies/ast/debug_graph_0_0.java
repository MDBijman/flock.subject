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

import flock.subject.common.CfgNode;
import flock.subject.common.SetUtils;
import flock.subject.strategies.Program;
import flock.subject.strategies.analysis.analyse_program_0_0;

import org.spoofax.terms.ParseError;

public class debug_graph_0_0 extends Strategy {
	
	public static debug_graph_0_0 instance = new debug_graph_0_0();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm program) {
        Program.printDebug("[debug-graph]");
        Program.printDebug(Program.instance.graph.toGraphviz().replace("\n", "\t"));
        Program.logTimers();
        Program.logCounts();
        return program;
    }
}
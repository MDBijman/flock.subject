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
import flock.subject.common.CfgNodeId;
import flock.subject.common.SetUtils;
import flock.subject.strategies.Program;
import flock.subject.strategies.analysis.analyse_program_0_0;

import org.spoofax.terms.ParseError;

public class make_id_0_0 extends Strategy {
	
	public static make_id_0_0 instance = new make_id_0_0();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm c) {
        ITermFactory factory = context.getFactory();
        int id = (int) Program.instance.nextNodeId().getId();
		return factory.makeInt(id);
    }
}
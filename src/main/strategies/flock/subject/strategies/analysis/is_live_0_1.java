package flock.subject.strategies.analysis;

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

import org.spoofax.terms.ParseError;
import org.spoofax.terms.Term;

public class is_live_0_1 extends Strategy {
	
	public static is_live_0_1 instance = new is_live_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
        ITermFactory factory = context.getFactory();
        
        CfgNode c = analyse_program_0_0.instance.getCFGNode(current);

        if (c == null) {
        	return current;
        }
        
        boolean isLess = c.getProperty("live").lattice.leq(SetUtils.create(name), c.getProperty("live").value);

        context.getIOAgent().printError(name.toString() + " is-live: " + isLess + " at " + current.toString());
        
        return isLess ? current : null;
    }
}
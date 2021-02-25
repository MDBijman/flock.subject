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

public class replace_node_0_1 extends Strategy {
	
	public static replace_node_0_1 instance = new replace_node_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm newNode, IStrategoTerm oldNode) {
        ITermFactory factory = context.getFactory();
		Program.log("api", "[replace-node] " + oldNode.toString() + " with " + newNode.toString());
        Program.instance.replaceNode(context, oldNode, newNode);
		return newNode;
    }
}
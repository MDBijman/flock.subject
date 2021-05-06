package flock.subject.strategies.ast;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.Program;

public class remove_node_0_0 extends Strategy {
	
	public static remove_node_0_0 instance = new remove_node_0_0();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm node) {
        Program.beginTime("api@remove");
		Program.log("api", "[remove-node] " + node.toString());
		Program.instance.removeNode(context, node);
        Program.endTime("api@remove");
		return node;
    }
}
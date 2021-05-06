package flock.subject.strategies.ast;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.Program;

public class make_id_0_0 extends Strategy {
	
	public static make_id_0_0 instance = new make_id_0_0();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm c) {
        ITermFactory factory = context.getFactory();
        int id = (int) Program.instance.nextNodeId().getId();
		return factory.makeInt(id);
    }
}
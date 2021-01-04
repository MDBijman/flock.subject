package flock.subject.strategies.analysis;

import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.util.TermUtils;
import org.spoofax.terms.TermFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.SetUtils;
import flock.subject.strategies.Program;
import flock.subject.live.LivenessValue;

import org.spoofax.terms.ParseError;
import org.spoofax.terms.Term;

public class is_live_0_1 extends Strategy {
	
	public static is_live_0_1 instance = new is_live_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
        ITermFactory factory = context.getFactory();
        
        
        IStrategoInt id = (IStrategoInt) current;
        
        CfgNode c = Program.instance.getCfgNode(new CfgNodeId(id.intValue()));

        if (c == null) {
        	context.getIOAgent().printError("CfgNode is null with id " + id.intValue());
        	return current;
        }
        
        HashSet<String> names = new HashSet<>();
        for (LivenessValue lv : (HashSet<LivenessValue>) c.getProperty("live").value) {
			names.add(lv.name);
        }
        //context.getIOAgent().printError(debug);
        
        boolean isLess = c.getProperty("live").lattice.leq(SetUtils.create(((IStrategoString) name).stringValue()), names);
        context.getIOAgent().printError("[is-live] " + name.toString() + " is-live: " + isLess + " at " + current.toString());
        //context.getIOAgent().printError(c.getProperty("live").value.toString());
        
        return isLess ? current : null;
    }
}
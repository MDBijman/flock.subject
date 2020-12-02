package flock.subject.strategies.analysis;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.util.TermUtils;
import org.spoofax.terms.TermFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.CfgNode;
import flock.subject.common.SetUtils;

import org.spoofax.terms.ParseError;
import org.spoofax.terms.Term;

public class get_value_0_1 extends Strategy {
	
	public static get_value_0_1 instance = new get_value_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
        ITermFactory factory = context.getFactory();
        
        CfgNode c = analyse_program_0_0.instance.getCFGNode(current);
        if (c == null) {

             context.getIOAgent().printError("null node");
        	 return null;
        }
        
        context.getIOAgent().printError(name.toString());
        if (c.properties.containsKey("values")) {
            Map<Object, Object> values = (Map<Object, Object>) c.getProperty("values").value;
            
            if (values.containsKey(name)) {
                Object val = values.get(name);
                IStrategoTerm sval = (IStrategoTerm) val;
                return sval;
            } else {
                context.getIOAgent().printError("null key");
                context.getIOAgent().printError(values.toString());
            	return null;
            }
        } else {
            context.getIOAgent().printError("null value property");
        	return null;
        }
    }
}
package flock.subject.strategies.analysis;

import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.util.TermUtils;
import org.spoofax.terms.TermFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.SetUtils;
import flock.subject.strategies.Program;

import org.spoofax.terms.ParseError;
import org.spoofax.terms.Term;

public class get_alias_set_0_1 extends Strategy {
	
	public static get_alias_set_0_1 instance = new get_alias_set_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
        ITermFactory factory = context.getFactory();
        IStrategoInt id = (IStrategoInt) current;
        
        Program.instance.graph.updateAliasUntilBoundary(new CfgNodeId(id.intValue())); 
        CfgNode c = Program.instance.getCfgNode(new CfgNodeId(id.intValue()));
        
        if (c == null) {
        	 return null;
        }
        
        if (c.properties.containsKey("locations")) {
        	Map<Object, Object> values = (Map<Object, Object>) c.getProperty("locations").value;

            if (values.containsKey(name)) {
                IStrategoTerm val = (IStrategoTerm) values.get(name);
                
                Set<IStrategoTerm> aliasSet = new HashSet<IStrategoTerm>();
                
                // Check other values if same location appears
                for (Entry<Object, Object> e : values.entrySet()) {
                	if (!e.getKey().equals(name) && e.getValue().equals(val)) {
                        //Program.printDebug(name.toString() + " get-alias-set: " + true + " at " + current.toString());

                        aliasSet.add((IStrategoTerm) e.getKey()); 
                    }
                }
                
                return factory.makeList(aliasSet);
            } else {
                //context.getIOAgent().printError("[get-alias-set] null key");
            	return null;
            }
        } else {
            //context.getIOAgent().printError("[get-alias-set] null value property");
        	return null;
        }
    }
}
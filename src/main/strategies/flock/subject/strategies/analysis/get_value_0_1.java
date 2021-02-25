package flock.subject.strategies.analysis;

import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import java.util.Map;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.strategies.Program;
import flock.subject.value.ValueValue;

public class get_value_0_1 extends Strategy {
	
	public static get_value_0_1 instance = new get_value_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
        ITermFactory factory = context.getFactory();
        IStrategoInt id = (IStrategoInt) current;
    
        Program.beginTime("value");
        Program.instance.graph.analysis.updateUntilBoundary_values(Program.instance.graph, new CfgNodeId(id.intValue()));        
        Program.endTime("value");

        CfgNode c = Program.instance.getCfgNode(new CfgNodeId(id.intValue()));
        
        if (c == null) {
        	Program.printDebug("null node");
        	return null;
        }
        
        if (c.properties.containsKey("values")) {
            Map<Object, Object> values = (Map<Object, Object>) c.getProperty("values").value;
            
            if (values.containsKey(((IStrategoString) name).stringValue())) {
                Object val = values.get(((IStrategoString) name).stringValue());
                IStrategoTerm sval = ((ValueValue)val).value;
                return sval;
            } else {
            	Program.printDebug("null value");
            	return null;
            }
        } else {
        	Program.printDebug("null value property");
        	return null;
        }
    }
}
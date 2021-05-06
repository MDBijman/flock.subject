package flock.subject.impl.value;

import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import java.util.Map;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.CfgNodeId;
import flock.subject.common.Graph.Node;
import flock.subject.common.FlockLattice;
import flock.subject.common.Program;
import flock.subject.common.FlockLattice.FlockValueLattice;

public class get_value_0_1 extends Strategy {
	
	public static get_value_0_1 instance = new get_value_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
		Program.beginTime("api@value");
		ITermFactory factory = context.getFactory();
        CfgNodeId id = new CfgNodeId(((IStrategoInt) current).intValue());
        Node node = Program.instance.getNode(id);
    
        if (node == null) {
        	Program.printDebug("null node");
    		Program.endTime("api@value");
        	return null;
        }
        
        Program.beginTime("api@value - analysis");
        Program.instance.analysisWithName("values").updateUntilBoundary(Program.instance.graph,  node);
        Program.endTime("api@value - analysis");
        
        if (node.properties.containsKey("values")) {
            Map<Object, Object> values = (Map<Object, Object>) node.getProperty("values").lattice.value();
            if (values.containsKey(((IStrategoString) name).stringValue())) {
                Object val = values.get(((IStrategoString) name).stringValue());
                IStrategoTerm sval = (IStrategoTerm) (((FlockValueLattice) val).value()).toTerm();
        		Program.endTime("api@value");
                return sval;
            } else {
            	Program.printDebug("null value");
        		Program.endTime("api@value");
            	return null;
            }
        } else {
        	Program.printDebug("null value property");
    		Program.endTime("api@value");
    		return null;
        }
    }
}
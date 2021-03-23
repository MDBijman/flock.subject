package flock.subject.strategies.analysis;

import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import java.util.Map;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import flock.subject.common.CfgNodeId;
import flock.subject.common.Graph.Node;
import flock.subject.common.Lattice;
import flock.subject.strategies.Program;
import flock.subject.value.ConstProp;

public class get_value_0_1 extends Strategy {
	
	public static get_value_0_1 instance = new get_value_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
        ITermFactory factory = context.getFactory();
        CfgNodeId id = new CfgNodeId(((IStrategoInt) current).intValue());
        Node node = Program.instance.getNode(id);
    
        if (node == null) {
        	Program.printDebug("null node");
        	return null;
        }
        
        Program.beginTime("value");
        Program.instance.analysis.updateUntilBoundary_values(Program.instance.graph, node); 
        Program.endTime("value");
        //Program.log("graphviz", "after get_value update: " + Program.instance.graph.toGraphviz());
        
        if (node.properties.containsKey("values")) {
            Map<Object, Object> values = (Map<Object, Object>) node.getProperty("values").lattice.value();
            if (values.containsKey(((IStrategoString) name).stringValue())) {
                Object val = values.get(((IStrategoString) name).stringValue());
                IStrategoTerm sval = (IStrategoTerm)((Lattice)val).value();
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
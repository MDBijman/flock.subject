package flock.subject.impl.live;

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

import flock.subject.common.CfgNodeId;
import flock.subject.common.Program;
import flock.subject.common.Graph.Node;
import flock.subject.common.SetUtils;

import org.spoofax.terms.ParseError;
import org.spoofax.terms.Term;

public class is_live_0_1 extends Strategy {
	
	public static is_live_0_1 instance = new is_live_0_1();
	
	@Override 
	public IStrategoTerm invoke(Context context, IStrategoTerm current, IStrategoTerm name) {
		Program.beginTime("api@is_live");
        ITermFactory factory = context.getFactory();
        CfgNodeId id = new CfgNodeId(((IStrategoInt) current).intValue());
        Node node = Program.instance.getNode(id);
        if (node == null) {
        	Program.printDebug("CfgNode is null with id " + id.getId());
    		Program.endTime("api@is_live");
        	return current;
        }
        Program.beginTime("live");
        Program.instance.analysisWithName("live").updateUntilBoundary(Program.instance.graph,  node);
        Program.endTime("live");
        //Program.log("graphviz", "after is_live update: " + Program.instance.graph.toGraphviz().replace('\n', '\t'));
        
        HashSet<String> names = new HashSet<>();
        for (Live lv : (HashSet<Live>) node.getProperty("live").lattice.value()) {
			names.add(((IStrategoString)lv.value).stringValue());
        }
        
        boolean contains = names.contains(((IStrategoString) name).stringValue());
		Program.endTime("api@is_live");
        return contains ? current : null;
    }
}
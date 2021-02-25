package flock.subject.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

import flock.subject.strategies.analysis.analyse_program_0_0;
import flock.subject.strategies.analysis.get_alias_set_0_1;
import flock.subject.strategies.analysis.get_value_0_1;
import flock.subject.strategies.analysis.is_live_0_1;
import flock.subject.strategies.ast.debug_graph_0_0;
import flock.subject.strategies.ast.update_0_0;
import flock.subject.strategies.ast.make_id_0_0;
import flock.subject.strategies.ast.replace_node_0_1;
import flock.subject.strategies.ast.remove_node_0_0;

public class InteropRegisterer extends JavaInteropRegisterer {
    public InteropRegisterer() {
        super(new Strategy[] {
    		analyse_program_0_0.instance,
    		is_live_0_1.instance,
    		get_value_0_1.instance,
    		get_alias_set_0_1.instance,
    		replace_node_0_1.instance,
    		remove_node_0_0.instance,
    		update_0_0.instance,
    		make_id_0_0.instance,
    		debug_graph_0_0.instance
    	});
    }
}

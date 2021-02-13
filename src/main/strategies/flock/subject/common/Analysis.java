package flock.subject.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.Context;

import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.live.LivenessValue;
import flock.subject.value.ValueValue;

public class Analysis {
	public String name;
	public HashSet<CfgNode> dirtyNodes;
	public HashSet<CfgNode> newNodes;
	public boolean isForward;
	
	public Analysis(String name, boolean isForward) {
		this.dirtyNodes = new HashSet<>();
		this.newNodes = new HashSet<>();
		this.isForward = isForward;
		this.name = name;
	}
	
	public void updateUntilBoundary(long boundary, HashMap<CfgNodeId, Long> idToInterval) {
		Set<CfgNode> dirtyNodes = new HashSet<>(this.dirtyNodes);
		
		for (CfgNode n : dirtyNodes) dirtyNodes.addAll(getTermDependencies(n));
		for (CfgNode n : newNodes)   dirtyNodes.addAll(getTermDependencies(n));

		LiveVariablesFlowAnalysis.updateDataAnalysis(newNodes, dirtyNodes, boundary);

		this.dirtyNodes.removeIf(n -> idToInterval.get(n.id) >= boundary);
		this.newNodes.removeIf(n -> idToInterval.get(n.id) >= boundary);
	}
	
	public static boolean removeFact(Context context, CfgNode current, CfgNodeId toRemove) {
		boolean removedLiveness = false;
		if (current.getProperty("live") != null) {
			Set<LivenessValue> lv = (Set<LivenessValue>) current.getProperty("live").value;
			assert lv != null;
			removedLiveness = lv.removeIf(ll -> {
				return ll.origin.getId() == toRemove.getId();
			});			
		}

		boolean removedConst = false;
		if (current.getProperty("values") != null) {
			HashMap<String, ValueValue> cv = (HashMap<String, ValueValue>) current.getProperty("values").value;
			assert cv != null;

			 removedConst = cv.entrySet().removeIf(ll -> {
				return ll.getValue().origin.contains(toRemove);
			});
		}

		return removedLiveness || removedConst;
	}
	
	public static Set<CfgNode> getTermDependencies(CfgNode n) {
		Set<CfgNode> r = new HashSet<>();

		// Folding
		if (TermUtils.isAppl(n.term, "Int", 1)) {
			r.addAll(n.parents);
		}
		
		// Propagation
		for (CfgNode p : n.children) {
			if (TermUtils.isAppl(p.term, "Assign", -1)) {
				r.add(p);
			}
		}

		return r;
	}
}

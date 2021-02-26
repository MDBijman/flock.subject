package flock.subject.common;

import org.apache.commons.lang3.tuple.Pair;
import org.strategoxt.lang.Context;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.TermFactory;
import java.io.IOException;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.function.Supplier;
import org.spoofax.terms.StrategoTuple;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoInt;
import org.spoofax.terms.StrategoString;
import org.spoofax.terms.StrategoList;
import flock.subject.common.CfgGraph;
import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LivenessValue;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class CfgNode {
	public HashMap<String, Property> properties;
	public Set<CfgNode> children;
	public Set<CfgNode> parents;
	public CfgNodeId id;
	public IStrategoTerm term;
	public long interval;
	public boolean isGhost = false;
	private static long ghostIds = Long.MAX_VALUE;
	
	public CfgNode() {
		children = new HashSet<>();
		parents = new HashSet<>();
		properties = new HashMap<>();
		this.isGhost = true;
		this.id = new CfgNodeId(ghostIds--);
	}

	public CfgNode(CfgNodeId id) {
		children = new HashSet<>();
		parents = new HashSet<>();
		properties = new HashMap<>();
		this.id = id;
	}

	public CfgNode(CfgNodeId id, IStrategoTerm term) {
		children = new HashSet<>();
		parents = new HashSet<>();
		properties = new HashMap<>();
		this.id = id;
		this.term = term;
		this.interval = Long.MAX_VALUE;
	}

	public void addProperty(String name, Lattice lat) {
		properties.put(name, new Property(name, lat));
	}

	public Property getProperty(String name) {
		return properties.get(name);
	}

	public Set<CfgNode> getChildren() {
		return this.children;
	}

	public Set<CfgNode> getParents() {
		return this.parents;
	}

	public void addChild(CfgNode child) {
		child.parents.add(this);
		children.add(child);
	}

	public void addChild(Iterable<CfgNode> children) {
		for (CfgNode child : children) {
			addChild(child);
		}
	}

	public Set<CfgNode> flatten() {
		return flatten(new HashSet<>());
	}

	private Set<CfgNode> flatten(Set<CfgNode> visited) {
		if (visited.contains(this)) {
			return new HashSet<>();
		}
		visited.add(this);
		Set<CfgNode> nodes = new HashSet<>();
		nodes.add(this);
		for (CfgNode child : children) {
			nodes.addAll(child.flatten(visited));
		}
		return nodes;
	}

	public Set<CfgNode> getLeaves() {
		return getLeaves(new HashSet<>());
	}

	private Set<CfgNode> getLeaves(Set<CfgNode> visited) {
		Set<CfgNode> leaves = new HashSet<>();
		visited.add(this);
		Set<CfgNode> filteredChildren = children.stream().filter(v -> !visited.contains(v)).collect(Collectors.toSet());
		if (filteredChildren.isEmpty()) {
			leaves.add(this);
		} else {
			for (CfgNode child : filteredChildren) {
				leaves.addAll(child.getLeaves(visited));
			}
		}
		return leaves;
	}

	@Override
	public String toString() {
		return "CfgNode(" + this.term.toString(2) + ")";
	}
}
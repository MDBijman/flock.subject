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
import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LiveValue;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueValue;

public abstract class Lattice {
	public abstract Lattice lub(Lattice o);

	public abstract Object value();

	public Set<CfgNodeId> origin() {
		return new UniversalSet();
	}
	
	public boolean leq(Lattice r) {
		return this.lub(r).equals(r);
	}

	public boolean nleq(Lattice r) {
		return !this.leq(r);
	}

	public Object glb(Lattice r) {
		return null;
	}

	public boolean geq(Lattice r) {
		return this.glb(r).equals(this);
	}
	
	/*
	 * Default Implementations
	 */
	
	public static class MustSetLattice extends Lattice {
		Set value;
		
		public MustSetLattice(Set v) {
			this.value = v;
		}
		
		public static MustSetLattice bottom() {
			return new MustSetLattice(new UniversalSet());
		}

		public static MustSetLattice top() {
			return new MustSetLattice(new HashSet());
		}
		
		@Override
		public Object value() {
			return this.value;
		}

		@Override
		public Lattice lub(Lattice r) {
			return new MustSetLattice(SetUtils.intersection(this.value(), r.value()));
		}

		@Override
		public boolean leq(Lattice r) {
			return SetUtils.isSupersetEquals(this.value(), r.value());
		}

		@Override
		public Lattice glb(Lattice r) {
			return new MustSetLattice(SetUtils.union(this.value(), r.value()));
		}

		@Override
		public boolean geq(Lattice r) {
			return SetUtils.isSubsetEquals(this.value(), r.value());
		}
	}

	public static class MaySetLattice extends Lattice {
		Set value;
		
		public MaySetLattice(Set v) {
			this.value = v;
		}
		
		public static MaySetLattice bottom() {
			return new MaySetLattice(new HashSet());
		}

		public static MaySetLattice top() {
			return new MaySetLattice(new UniversalSet());
		}

		@Override
		public String toString() {
			return value.toString();
		}
		
		@Override
		public Object value() {
			return this.value;
		}

		@Override
		public Lattice lub(Lattice r) {
			return new MaySetLattice(SetUtils.union(this.value(), r.value()));
		}

		@Override
		public boolean leq(Lattice r) {
			return SetUtils.isSubsetEquals(this.value(), r.value());
		}

		@Override
		public Lattice glb(Lattice r) {
			return new MaySetLattice(SetUtils.intersection(this.value(), r.value()));
		}

		@Override
		public boolean geq(Lattice r) {
			return SetUtils.isSupersetEquals(this.value(), r.value());
		}
	}

	public static class MapLattice extends Lattice {
		Map value;

		public MapLattice(Map v) {
			this.value = v;
		}

		public static MapLattice bottom() {
			return new MapLattice(new HashMap());
		}

		@Override
		public Lattice lub(Lattice o) {
			return new MapLattice(MapUtils.union(this, o));
		}

		@Override
		public Object value() {
			return this.value;
		}
	}
}
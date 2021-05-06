package flock.subject.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.spoofax.terms.util.NotImplementedException;

public interface FlockLattice {
	public abstract FlockLattice lub(FlockLattice o);

	public abstract Object value();
	
	public default boolean leq(FlockLattice r) {
		return this.lub(r).equals(r);
	}

	public default boolean nleq(FlockLattice r) {
		return !this.leq(r);
	}

	public default Object glb(FlockLattice r) {
		return null;
	}

	public default boolean geq(FlockLattice r) {
		return this.glb(r).equals(this);
	}
	
	/*
	 * Default Implementations
	 */
	
	public static interface FlockCollectionLattice extends FlockLattice, Iterable {}
	public static interface FlockValueLattice extends FlockLattice {
		public abstract FlockValue value();
	}
	
	public static class MustSet implements FlockCollectionLattice {
		Set value;
		
		public MustSet(Set v) {
			this.value = v;
		}
		
		public static MustSet bottom() {
			return new MustSet(new UniversalSet());
		}

		public static MustSet top() {
			return new MustSet(new HashSet());
		}
		
		@Override
		public Object value() {
			return this.value;
		}

		@Override
		public FlockLattice lub(FlockLattice r) {
			return new MustSet(SetUtils.intersection(this.value(), r.value()));
		}

		@Override
		public boolean leq(FlockLattice r) {
			return SetUtils.isSupersetEquals(this.value(), r.value());
		}

		@Override
		public FlockLattice glb(FlockLattice r) {
			return new MustSet(SetUtils.union(this.value(), r.value()));
		}

		@Override
		public boolean geq(FlockLattice r) {
			return SetUtils.isSubsetEquals(this.value(), r.value());
		}

		@Override
		public Iterator iterator() {
			return value.iterator();
		}
	}

	public static class MaySet implements FlockCollectionLattice {
		Set value;
		
		public MaySet(MaySet v) {
			this.value = new HashSet<>();
			this.value.addAll(v.value);
		}
		
		public MaySet(Set v) {
			this.value = v;
		}
		
		public static MaySet bottom() {
			return new MaySet(new HashSet());
		}

		public static MaySet top() {
			return new MaySet(new UniversalSet());
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
		public FlockLattice lub(FlockLattice r) {
			return new MaySet(SetUtils.union(this.value(), r.value()));
		}

		@Override
		public boolean leq(FlockLattice r) {
			return SetUtils.isSubsetEquals(this.value(), r.value());
		}

		@Override
		public FlockLattice glb(FlockLattice r) {
			return new MaySet(SetUtils.intersection(this.value(), r.value()));
		}

		@Override
		public boolean geq(FlockLattice r) {
			return SetUtils.isSupersetEquals(this.value(), r.value());
		}
	
		@Override
		public Iterator iterator() {
			return value.iterator();
		}
	}

	public static class MapLattice implements FlockCollectionLattice {
		Map value;

		public MapLattice(FlockLattice o) {
			this.value = ((MapLattice) o).value;
		}
		
		public MapLattice(MapLattice o) {
			this.value = o.value;
		}
		
		public MapLattice(Map v) {
			this.value = v;
		}

		public static MapLattice bottom() {
			return new MapLattice(new HashMap());
		}
		
		public static MapLattice top() {
			throw new NotImplementedException();
		}

		@Override
		public Iterator iterator() {
			return value.entrySet().iterator();
		}
		
		@Override
		public FlockLattice lub(FlockLattice o) {
			return new MapLattice(MapUtils.union(this, o));
		}

		@Override
		public Object value() {
			return this.value;
		}
	}
}
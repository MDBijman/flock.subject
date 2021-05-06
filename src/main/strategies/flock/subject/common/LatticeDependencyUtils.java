package flock.subject.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import flock.subject.common.FlockLattice.FlockCollectionLattice;
import flock.subject.common.FlockLattice.FlockValueLattice;
import flock.subject.common.FlockLattice.MapLattice;
import flock.subject.common.FlockLattice.MaySet;
import flock.subject.common.FlockLattice.MustSet;
import flock.subject.common.FlockValue.FlockValueWithDependencies;

public abstract class LatticeDependencyUtils {
	public static boolean removeValuesByDependency(FlockLattice l, Dependency d) {
		Collection valuesInCollection = null;
		if (l instanceof MaySet) {
			valuesInCollection = (Collection) ((MaySet) l).value();
		} else if (l instanceof MustSet) {
			valuesInCollection = (Collection) ((MustSet) l).value();
		} else if (l instanceof MapLattice) {
			valuesInCollection = ((MapLattice) l).value.values();
		} else {
			throw new RuntimeException("Unknown lattice instance");
		}

		return valuesInCollection.removeIf(x -> {
			if (x instanceof FlockCollectionLattice) {
				removeValuesByDependency(l, d);
				return false;
			} else if (x instanceof FlockValueLattice) {
				Object val = ((FlockValueLattice) x).value();
				if (val instanceof FlockValueWithDependencies) {
					return ((FlockValueWithDependencies) val).hasDependency(d);
				} else if (val instanceof FlockValue) {
					return true;
				} else {
					throw new RuntimeException("Unknown lattice instance");
				}
			}
			
			throw new RuntimeException("Expected lattice or value with dependencies");
		});
	}
	
	public static Set<Dependency> gatherDependencies(FlockLattice l) {
		Set<Dependency> out = new HashSet<>();
		gatherDependencies(out, l);
		return out;
	}

	public static void gatherDependencies(Set<Dependency> out, FlockLattice l) {
		if (l instanceof MapLattice) {
			for (Object o : (MapLattice) l) {
				Entry e = (Entry) o;
				gatherDependencies(out, (FlockLattice) e.getValue());
			}
		}
		else if (l instanceof FlockCollectionLattice) {
			for (Object o : (FlockCollectionLattice) l) {
				if (o instanceof FlockLattice) {
					gatherDependencies(out, (FlockLattice) o);
				} else if (o instanceof FlockValueWithDependencies) {
					out.addAll(((FlockValueWithDependencies) o).dependencies());
				} else {
					throw new RuntimeException("Unknown lattice instance");
				}
			}
		} else if (l instanceof FlockValueLattice) {
			if (l.value() instanceof FlockValueWithDependencies) {
				out.addAll(((FlockValueWithDependencies) l.value()).dependencies());
			} else if (l.value() instanceof FlockValue) {
				return;
			} else {
				throw new RuntimeException("Invalid lattice type");
			}
		} else {
			Program.printDebug(l.toString());
			throw new RuntimeException("Invalid lattice type");
		}
	}
	
}
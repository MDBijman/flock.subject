package flock.subject.impl.value;

import org.apache.commons.lang3.tuple.Pair;
import org.strategoxt.lang.Context;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoString;
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
import flock.subject.common.Graph;
import flock.subject.common.Analysis;
import flock.subject.common.Graph.Node;
import flock.subject.common.Analysis.Direction;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Dependency;
import flock.subject.common.Helpers;
import flock.subject.common.FlockLattice;
import flock.subject.common.FlockLattice.MaySet;
import flock.subject.common.FlockLattice.MustSet;
import flock.subject.common.FlockLattice.MapLattice;
import flock.subject.common.FlockLattice.FlockValueLattice;
import flock.subject.common.FlockLattice.FlockCollectionLattice;
import flock.subject.common.FlockValue;
import flock.subject.common.FlockValue.FlockValueWithDependencies;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;

public class ValueFlowAnalysisProperties {
	static class ConstProp extends FlockValueWithDependencies {
		public IStrategoTerm value;
		public Set<Dependency> dependencies;

		ConstProp(IStrategoTerm value) {
			this.value = value;
			this.dependencies = SetUtils.create();
		}

		ConstProp(IStrategoTerm value, Dependency origin) {
			this.value = value;
			this.dependencies = SetUtils.create(origin);
		}

		ConstProp(IStrategoTerm value, Set<Dependency> dependencies) {
			this.value = value;
			this.dependencies.addAll(dependencies);
		}

		public ConstProp withOrigin(Set<Dependency> dependencies) {
			this.dependencies.addAll(dependencies);
			return this;
		}

		public ConstProp withOrigin(CfgNodeId id) {
			this.dependencies.add(new Dependency(id));
			return this;
		}

		@Override
		public IStrategoTerm toTerm() {
			return this.value;
		}

		@Override
		public String toString() {
			String pre = "Value(\"" + this.value.toString() + "\", {";
			StringBuilder sb = new StringBuilder();
			for (Dependency dep : dependencies) {
				sb.append(dep.id.getId());
				sb.append(",");
			}
			String post = "})";
			return pre + sb.toString() + post;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ConstProp)) {
				return false;
			}
			ConstProp other = (ConstProp) obj;
			return this.value.equals(other.value) && this.dependencies.equals(other.dependencies);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public Set<Dependency> dependencies() {
			return this.dependencies;
		}

		@Override
		public boolean removeDependency(Dependency d) {
			return this.dependencies.remove(d);
		}

		@Override
		public boolean hasDependency(Dependency d) {
			return this.dependencies.contains(d);
		}

		@Override
		public void addDependency(Dependency d) {
			this.dependencies.add(d);
		}
	}
}
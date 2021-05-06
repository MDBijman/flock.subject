package flock.subject.common;

import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

public abstract class FlockValue {
	public abstract IStrategoTerm toTerm();
	
	public static abstract class FlockValueWithDependencies extends FlockValue {
		public abstract boolean hasDependency(Dependency d);
		public abstract void addDependency(Dependency d);
		public abstract boolean removeDependency(Dependency d);
		public abstract Set<Dependency> dependencies();
	}
}

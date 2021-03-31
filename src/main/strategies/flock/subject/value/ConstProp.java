package flock.subject.value;

import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

import flock.subject.common.CfgNodeId;
import flock.subject.common.Dependency;
import flock.subject.common.IHasDependencies;
import flock.subject.common.SetUtils;
import flock.subject.common.Value.ValueWithDependencies;
import flock.subject.common.Lattice;

public class ConstProp extends ValueWithDependencies {
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

	/*
	 * IHasDependencies
	 */
	
	@Override
	public Set<Dependency> dependencies() {
		return this.dependencies;
	}

	@Override
	public void add(Dependency d) {
		this.dependencies.add(d);
	}

	@Override
	public boolean remove(Dependency d) {
		return this.dependencies.remove(d);		
	}
	
	@Override
	public boolean contains(Dependency d) {
		return this.dependencies.contains(d);		
	}
}
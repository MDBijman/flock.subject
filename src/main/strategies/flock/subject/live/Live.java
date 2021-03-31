package flock.subject.live;

import java.util.HashSet;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

import flock.subject.common.CfgNodeId;
import flock.subject.common.Dependency;
import flock.subject.common.IHasDependencies;
import flock.subject.common.Value;
import flock.subject.common.Value.ValueWithDependencies;

public class Live extends ValueWithDependencies {
	public IStrategoTerm value;
	public Set<Dependency> dependencies;

	Live(IStrategoTerm name, CfgNodeId origin) {
		this.value = name;
		this.dependencies = new HashSet<>();
		this.dependencies.add(new Dependency(origin));
	}

	Live(IStrategoTerm name) {
		this.value = name;
		this.dependencies = new HashSet<>();
	}
	
	public Live withOrigin(Set<CfgNodeId> ids) {
		for (CfgNodeId id : ids) {
			this.dependencies.add(new Dependency(id));
		}
		return this;
	}
	
	public Live withOrigin(CfgNodeId id) {
		this.dependencies.add(new Dependency(id));
		return this;
	}
	
	@Override
	public String toString() {
		return "Live(\"" + this.value + "\", " + this.dependencies.toString() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Live)) {
			return false;
		}
		Live other = (Live) obj;

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
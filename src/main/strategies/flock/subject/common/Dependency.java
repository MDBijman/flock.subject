package flock.subject.common;

import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

public class Dependency {
	public CfgNodeId id;

	public Dependency(CfgNodeId id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Dep(" + id.toString() + ")";
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Dependency))
			return false;
		return this.id.equals(((Dependency) other).id);
	}
}

package flock.subject.live;

import java.util.HashSet;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

import flock.subject.common.CfgNodeId;

public class Live {
	public IStrategoTerm value;
	public HashSet<CfgNodeId> origin;

	Live(IStrategoTerm name, CfgNodeId origin) {
		this.value = name;
		this.origin = new HashSet<>();
		this.origin.add(origin);
	}

	Live(IStrategoTerm name) {
		this.value = name;
		this.origin = new HashSet<>();
	}
	
	public Live withOrigin(Set<CfgNodeId> id) {
		this.origin.addAll(id);
		return this;
	}
	
	public Live withOrigin(CfgNodeId id) {
		this.origin.add(id);
		return this;
	}
	
	@Override
	public String toString() {
		return "Live(\"" + this.value + "\", " + this.origin.toString() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Live)) {
			return false;
		}
		Live other = (Live) obj;

		return this.value.equals(other.value) && this.origin.equals(other.origin);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
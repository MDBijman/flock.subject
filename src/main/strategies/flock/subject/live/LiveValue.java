package flock.subject.live;

import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.IStrategoTerm;

import flock.subject.common.CfgNodeId;

public class LiveValue {
	public IStrategoTerm value;
	public CfgNodeId origin;

	LiveValue(IStrategoTerm name, CfgNodeId origin) {
		this.value = name;
		this.origin = origin;
	}

	@Override
	public String toString() {
		return "Live(\"" + this.value + "\", " + this.origin.getId() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LiveValue)) {
			return false;
		}
		LiveValue other = (LiveValue) obj;

		return this.value.equals(other.value) && this.origin.equals(other.origin);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
package flock.subject.value;

import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

import flock.subject.common.CfgNodeId;
import flock.subject.common.SetUtils;
import flock.subject.common.Lattice;

public class ConstProp {
	public IStrategoTerm value;
	public Set<CfgNodeId> origin;
	
	ConstProp(IStrategoTerm value) {
		this.value = value;
		this.origin = SetUtils.create();
	}
	
	ConstProp(IStrategoTerm value, CfgNodeId origin) {
		this.value = value;
		this.origin = SetUtils.create(origin);
	}
	
	ConstProp(IStrategoTerm value, Set<CfgNodeId> origins) {
		this.value = value;
		this.origin = origins;
	}
	
	public ConstProp withOrigin(Set<CfgNodeId> id) {
		this.origin.addAll(id);
		return this;
	}
	
	public ConstProp withOrigin(CfgNodeId id) {
		this.origin.add(id);
		return this;
	}
	
	@Override
	public String toString() {
		String pre = "Value(\"" + this.value.toString() + "\", {";
		StringBuilder sb = new StringBuilder();
		for(CfgNodeId id : origin) {
			sb.append(id.getId());
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
				
		return this.value.equals(other.value) && this.origin.equals(other.origin);
	}
	
	@Override
	public int hashCode() {
		return value.hashCode();
	}
}
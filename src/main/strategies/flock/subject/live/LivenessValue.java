package flock.subject.live;

import flock.subject.common.CfgNodeId;

public class LivenessValue {
	public String name;
	public CfgNodeId origin;
	
	LivenessValue(String name, CfgNodeId origin) {
		this.name = name;
		this.origin = origin;
	}
	
	@Override
	public String toString() {
		return "Live(\"" + this.name + "\", " + this.origin.getId() +")";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LivenessValue)) {
			return false;
		}
		LivenessValue other = (LivenessValue) obj;
				
		return this.name.equals(other.name) && this.origin.equals(other.origin);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
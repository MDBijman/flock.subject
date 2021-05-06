package flock.subject.common;

public class CfgNodeId {
	private long id;

	public CfgNodeId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public CfgNodeId successor() {
		return new CfgNodeId(id + 1);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CfgNodeId)) {
			return false;
		}
		CfgNodeId other = (CfgNodeId) obj;
		return other.id == this.id;
	}

	@Override
	public int hashCode() {
		return new Long(id).hashCode();
	}

	@Override
	public String toString() {
		return id + "";
	}
}
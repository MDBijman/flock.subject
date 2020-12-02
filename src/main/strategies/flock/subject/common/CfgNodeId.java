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
}
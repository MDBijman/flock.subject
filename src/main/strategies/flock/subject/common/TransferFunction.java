package flock.subject.common;

import flock.subject.common.Graph.Node;

public abstract class TransferFunction {
	public abstract FlockLattice eval(Node node);
}
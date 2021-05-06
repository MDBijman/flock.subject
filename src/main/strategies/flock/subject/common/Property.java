package flock.subject.common;

import java.util.Set;
import java.util.stream.Collectors;

public class Property {
	String name;
	public TransferFunction transfer;
	public TransferFunction init;
	public FlockLattice lattice;

	public Property(String name, FlockLattice lattice) {
		this.name = name;
		this.lattice = lattice;
	}

	public Object getLattice() {
		return lattice;
	}

	public String toGraphviz() {
		return " " + name + "=" + valueToString(lattice.value());
	}

	private static String valueToString(Object value) {
		if (value instanceof UniversalSet) {
			return "{...}";
		}
		if (value instanceof Set) {
			return "{"
					+ String.join(", ",
							((Set<Object>) value).stream().map(v -> valueToString(v)).collect(Collectors.toList()))
					+ "}";
		}
		if (value instanceof String) {
			return "\\\"" + value + "\\\"";
		}
		return value.toString().replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
				.replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'").replace("\"", "\\\"");
	}
}
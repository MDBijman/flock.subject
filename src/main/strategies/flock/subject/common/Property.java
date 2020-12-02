package flock.subject.common;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.TermFactory;
import java.io.IOException;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
import java.util.Iterator;
import java.util.Collection;
import java.util.function.Supplier;
import org.spoofax.terms.StrategoTuple;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoInt;
import org.spoofax.terms.StrategoString;
import org.spoofax.terms.StrategoList;

public class Property {
	String name;
	public Lattice lattice;
	public TransferFunction transfer;
	public TransferFunction init;
	public Object value;

	public Property(String name, Lattice lattice) {
		this.name = name;
		this.lattice = lattice;
	}

	public Object getValue() {
		return value;
	}

	public String toGraphviz() {
		return " " + name + "=" + valueToString(value);
	}

	private static String valueToString(Object value) {
		if (value instanceof UniversalSet) {
			return "{...}";
		}
		if (value instanceof Set) {
			return "{"
					+ String.join(", ",
							((Set<Object>) value).stream().map(x -> valueToString(x)).collect(Collectors.toList()))
					+ "}";
		}
		if (value instanceof String) {
			return "\\\"" + value + "\\\"";
		}
		return value.toString().replace("\\", "\\\\").replace("\t", "\\t").replace("\b", "\\b").replace("\n", "\\n")
				.replace("\r", "\\r").replace("\f", "\\f").replace("\'", "\\'").replace("\"", "\\\"");
	}
}
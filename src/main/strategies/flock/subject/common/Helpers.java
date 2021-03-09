package flock.subject.common;

import org.apache.commons.lang3.tuple.Pair;
import org.strategoxt.lang.Context;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.TermFactory;
import java.io.IOException;
import org.spoofax.terms.util.M;
import org.spoofax.terms.util.TermUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.function.Supplier;
import org.spoofax.terms.StrategoTuple;
import org.spoofax.terms.StrategoAppl;
import org.spoofax.terms.StrategoConstructor;
import org.spoofax.terms.StrategoInt;
import org.spoofax.terms.StrategoString;
import org.spoofax.terms.StrategoList;
import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LiveValue;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueValue;

public class Helpers {
	public static IStrategoTerm toTerm(Object o) {
		if (o instanceof IStrategoTerm) {
			return (IStrategoTerm) o;
		}
		if (o instanceof Integer) {
			return new StrategoInt((int) o);
		}
		if (o instanceof String) {
			return new StrategoString((String) o, null);
		}
		if (o instanceof Set) {
			Set s = (Set) o;
			Object[] elements = s.toArray();
			IStrategoList result = new StrategoList(toTerm(elements[elements.length - 1]), null, null);
			for (int i = elements.length - 2; i >= 0; i--) {
				result = new StrategoList(toTerm(elements[i]), result, null);
			}
			return result;
		}
		throw new RuntimeException("Could not identify proper term type.");
	}

	public static IStrategoTerm at(IStrategoTerm term, int index) {
		if (term instanceof IStrategoTuple) {
			IStrategoTuple tuple = (IStrategoTuple) term;
			return tuple.get(index);
		}
		IStrategoAppl appl = (IStrategoAppl) term;
		return appl.getSubterm(index);
	}

	public static Object add(Object l, Object r) {
		l = extractPrimitive(l);
		r = extractPrimitive(r);
		if (l instanceof Integer && r instanceof Integer) {
			return (int) l + (int) r;
		}
		return l.toString() + r.toString();
	}

	public static Object extractPrimitive(Object o) {
		if (o instanceof StrategoInt) {
			StrategoInt term = (StrategoInt) o;
			return term.intValue();
		}
		if (o instanceof StrategoString) {
			StrategoString term = (StrategoString) o;
			return term.stringValue();
		}
		return o;
	}
}
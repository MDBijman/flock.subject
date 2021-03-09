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
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class SetUtils {
	public static Set create(Object... terms) {
		Set result = new HashSet();
		for (Object term : terms) {
			result.add(term);
		}
		return result;
	}

	public static Set union(Object l, Object r) {
		Set ls = (Set) l;
		Set rs = (Set) r;
		if (ls instanceof UniversalSet || rs instanceof UniversalSet) {
			return new UniversalSet();
		}
		Set<IStrategoTerm> result = new HashSet();
		result.addAll(ls);
		result.addAll(rs);
		return result;
	}

	public static Set intersection(Object l, Object r) {
		Set ls = (Set) l;
		Set rs = (Set) r;
		if (ls instanceof UniversalSet) {
			return rs;
		}
		if (rs instanceof UniversalSet) {
			return ls;
		}
		Set result = new HashSet();
		for (Object i : ls) {
			if (rs.contains(i)) {
				result.add(i);
			}
		}
		return result;
	}

	public static Set difference(Object l, Object r) {
		Set ls = (Set) l;
		Set rs = (Set) r;
		Set result = new HashSet();
		for (Object i : ls) {
			if (!rs.contains(i)) {
				result.add(i);
			}
		}
		return result;
	}

	public static boolean isSubsetEquals(Object l, Object r) {
		Set ls = (Set) l;
		Set rs = (Set) r;
		if (ls instanceof UniversalSet) {
			return false;
		}
		if (rs instanceof UniversalSet) {
			return true;
		}
		for (Object i : ls) {
			if (!rs.contains(i)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isSupersetEquals(Object l, Object r) {
		Set ls = (Set) l;
		Set rs = (Set) r;
		if (ls instanceof UniversalSet) {
			return true;
		}
		if (rs instanceof UniversalSet) {
			return false;
		}
		for (Object i : rs) {
			if (!ls.contains(i)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isSuperset(Object l, Object r) {
		if (l.equals(r)) {
			return false;
		}
		return isSupersetEquals(l, r);
	}

	public static boolean isSubset(Object l, Object r) {
		if (l.equals(r)) {
			return false;
		}
		return isSubsetEquals(l, r);
	}
}
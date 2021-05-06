package flock.subject.common;

import java.util.HashSet;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

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
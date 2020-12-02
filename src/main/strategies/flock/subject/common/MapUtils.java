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

public class MapUtils {
	public static Map union(Lattice lat, Object l, Object r) {
		Map ls = (Map) l;
		HashMap rs = (HashMap) r;
		Map res = new HashMap();
		res.putAll(ls);
		for (Map.Entry e : (Set<Map.Entry>) rs.entrySet()) {
			if (res.containsKey(e.getKey())) {
				Object v = res.get(e.getKey());
				Object m = lat.lub(e.getValue(), v);
				res.put(e.getKey(), m);
			} else {
				res.put(e.getKey(), e.getValue());
			}
		}
		return res;
	}

	public static Map create(Object k, Object v) {
		HashMap result = new HashMap();
		result.put(k, v);
		return result;
	}

	public static Map create() {
		return new HashMap();
	}
}
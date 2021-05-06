package flock.subject.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapUtils {
	public static Map union(FlockLattice l, FlockLattice r) {
		Map ls = (Map) l.value();
		HashMap rs = (HashMap) r.value();
		Map res = new HashMap();
		res.putAll(ls);
		for (Map.Entry i : (Set<Map.Entry>) rs.entrySet()) {
			if (res.containsKey(i.getKey())) {
				FlockLattice v = (FlockLattice)res.get(i.getKey());
				FlockLattice m = ((FlockLattice)i.getValue()).lub(v);
				res.put(i.getKey(), m);
			} else {
				res.put(i.getKey(), i.getValue());
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
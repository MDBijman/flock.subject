package flock.subject.common;

import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.IStrategoTuple;
import org.spoofax.terms.StrategoInt;
import org.spoofax.terms.StrategoList;
import org.spoofax.terms.StrategoString;
import org.spoofax.terms.util.TermUtils;

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
		throw new RuntimeException("Could not identify proper term type of " + o.toString());
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
	
	public static CfgNodeId getTermPosition(IStrategoTerm n) {
		if (n.getAnnotations().size() == 0)
			return null;
		assert TermUtils.isAppl(n.getAnnotations().getSubterm(0), "FlockNodeId", 1);
		IStrategoInt id = (IStrategoInt) n.getAnnotations().getSubterm(0).getSubterm(0);
		return new CfgNodeId(id.intValue());
	}
}
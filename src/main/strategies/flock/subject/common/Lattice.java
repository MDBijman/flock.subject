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

public abstract class Lattice {
  public abstract Object bottom( ) ;
  public abstract Object lub(Object l, Object r) ;
  public boolean leq(Object l, Object r) {
                                           return lub(l, r). equals(r);
                                         }
  public boolean nleq(Object l, Object r) {
                                            return !leq(l, r);
                                          }
  public Object glb(Object l, Object r) {
                                          return null;
                                        }
  public boolean geq(Object l, Object r) {
                                           return glb(l, r). equals(l);
                                         }
  public Object top( ) {
                         return null;
                       }
}
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

public final class UniversalSet implements Set {
  @Override public int size( ) {
                                 return Integer.MAX_VALUE;
                               }
  @Override public boolean isEmpty( ) {
                                        return false;
                                      }
  @Override public boolean contains(Object o) {
                                                return true;
                                              }
  @Override public Iterator iterator( ) {
                                          throw new UnsupportedOperationException ( );
                                        }
  @Override public Object [] toArray( ) {
                                          throw new UnsupportedOperationException ( );
                                        }
  @Override public boolean add(Object o) {
                                           return false;
                                         }
  @Override public boolean remove(Object o) {
                                              return false;
                                            }
  @Override public boolean addAll(Collection c) {
                                                  return false;
                                                }
  @Override public void clear( ) {
                                 }
  @Override public boolean removeAll(Collection c) {
                                                     return false;
                                                   }
  @Override public boolean retainAll(Collection c) {
                                                     return false;
                                                   }
  @Override public boolean containsAll(Collection c) {
                                                       return false;
                                                     }
  @Override public Object [] toArray(Object [] a) {
                                                    throw new UnsupportedOperationException ( );
                                                  }
  @Override public boolean equals(Object obj) {
                                                return obj instanceof UniversalSet;
                                              }
  @Override public int hashCode( ) {
                                     return getClass( ). hashCode( );
                                   }
}
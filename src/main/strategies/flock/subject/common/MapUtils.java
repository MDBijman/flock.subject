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
import flock.subject.common.CfgGraph;
import flock.subject.common.CfgNode;
import flock.subject.common.CfgNodeId;
import flock.subject.common.Helpers;
import flock.subject.common.Lattice;
import flock.subject.common.MapUtils;
import flock.subject.common.SetUtils;
import flock.subject.common.TransferFunction;
import flock.subject.common.UniversalSet;
import flock.subject.live.LivenessValue;
import flock.subject.live.LiveVariablesFlowAnalysis;
import flock.subject.alias.PointsToFlowAnalysis;
import flock.subject.value.ValueFlowAnalysis;
import flock.subject.value.ValueValue;

public class MapUtils {
  public static Map union(Lattice lat,Object l, Object r) {
                                                            Map ls = (Map ) l;
                                                            HashMap rs = (HashMap ) r;
                                                            Map res = new HashMap ( );
                                                            res. putAll(ls);
                                                            for( Map. Entry i : (Set<Map. Entry> ) rs. entrySet( )) {
                                                                                                                      if(res. containsKey(i. getKey( ))) {
                                                                                                                                                           Object v = res. get(i. getKey( ));
                                                                                                                                                           Object m = lat. lub(i. getValue( ), v);
                                                                                                                                                           res. put(i. getKey( ), m);
                                                                                                                                                         } else {
                                                                                                                                                                  res. put(i. getKey( ), i. getValue( ));
                                                                                                                                                                }
                                                                                                                    }
                                                            return res;
                                                          }
  public static Map create(Object k, Object v) {
                                                 HashMap result = new HashMap ( );
                                                 result. put(k, v);
                                                 return result;
                                               }
  public static Map create( ) {
                                return new HashMap ( );
                              }
}
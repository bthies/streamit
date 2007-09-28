package at.dms.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.dms.kjc.JExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.SLIREmptyVisitor;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRTwoStageFilter;
import at.dms.kjc.slicegraph.FilterContent;

/**
 * Pulled out of lowering.fission.StatelessMethodDublicate for general use:
 * 
 * Find methods reachable in a steady state.
 * Find all method names reachable from a particular method.
 *
 */
public class GetSteadyMethods {

    /**
     * Returns the methods of 'filter' that execute in the steady
     * state (either called from work, or likely to be message handlers).
     * 
     * @param filter : a filter.
     * @returns a list of methods that may execute in the steady state.
     */
    public static List<JMethodDeclaration> getSteadyMethods(SIRFilter filter) {
        // I'm not sure how to find who is a message handler.  So,
        // count as "steady" any method that is reachable from work or
        // NOT reachable from init (idea being that init functions
        // should not call their own method handlers).
        JMethodDeclaration[] methods = filter.getMethods();

        List<String> initReachable = getMethodsReachableFrom(methods, filter.getInit());
        List<String> workReachable = getMethodsReachableFrom(methods, filter.getWork());
        List<String> preReachable = new ArrayList<String>();
        if (filter instanceof SIRTwoStageFilter) {
            preReachable = getMethodsReachableFrom(methods, ((SIRTwoStageFilter)filter).getInitWork());
        }

        // count a method as 'steady' if it is reachable from work or
        // prework, or if it is NOT reachable from init
        Set<String> inits = new HashSet<String>();
        Set<String> works = new HashSet<String>();
        for (String s : initReachable) { inits.add(s); }
        for (String s : workReachable) { works.add(s); }
        for (String s : preReachable)  { works.add(s); }

        Set<JMethodDeclaration> result = new HashSet<JMethodDeclaration>();
        for (int i=0; i<methods.length; i++) {
            String name = methods[i].getName();
            if (works.contains(name) || !inits.contains(name)) {
                result.add(methods[i]);
            }
        }

        List<JMethodDeclaration> retval = new ArrayList<JMethodDeclaration>();
        retval.addAll(result);
        return retval;
    }
    
    public static List<JMethodDeclaration> getSteadyMethods(FilterContent filter) {
        // I'm not sure how to find who is a message handler.  So,
        // count as "steady" any method that is reachable from work or
        // NOT reachable from init (idea being that init functions
        // should not call their own method handlers).
        JMethodDeclaration[] methods = filter.getMethods();

        List<String> initReachable = getMethodsReachableFrom(methods, filter.getInit());
        List<String> workReachable = getMethodsReachableFrom(methods, filter.getWork());
        List<String> preReachable = new ArrayList<String>();
        if (filter.isTwoStage()) {
            preReachable = getMethodsReachableFrom(methods, filter.getInitWork());
        }

        // count a method as 'steady' if it is reachable from work or
        // prework, or if it is NOT reachable from init
        Set<String> inits = new HashSet<String>();
        Set<String> works = new HashSet<String>();
        for (String s : initReachable) { inits.add(s); }
        for (String s : workReachable) { works.add(s); }
        for (String s : preReachable)  { works.add(s); }

        Set<JMethodDeclaration> result = new HashSet<JMethodDeclaration>();
        for (int i=0; i<methods.length; i++) {
            String name = methods[i].getName();
            if (works.contains(name) || !inits.contains(name)) {
                result.add(methods[i]);
            }
        }

        List<JMethodDeclaration> retval = new ArrayList<JMethodDeclaration>();
        retval.addAll(result);
        return retval;
    }

    /**
     * Returns set of method NAMES (Strings) out of 'methods' that are
     * reachable from 'base'.  It is reachable if it is 'base', is called by
     * 'base' or by anthing 'base' transitively calls.
     * 
     * @param methods : array of methods to check for reachability from base.
     * @param base : base case for reachability test.
     * @return a list of names of methods in 'methods' as reflexive transitive closure of method calls from 'base'
     */
    public static List<String> getMethodsReachableFrom(JMethodDeclaration[] methods, JMethodDeclaration base) {
        // names of reachable methods
        final HashSet<String> reachable = new HashSet<String>();
        // iterate to steady state
        final boolean[] changed = { false };
        
        reachable.add(base.getName());
        do {
            changed[0] = false;
            for (int i=0; i<methods.length; i++) {
                if (reachable.contains(methods[i].getName())) {
                    methods[i].accept(new SLIREmptyVisitor() {
                            public void visitMethodCallExpression(JMethodCallExpression self,
                                                                  JExpression prefix,
                                                                  String ident,
                                                                  JExpression[] args) {
                                super.visitMethodCallExpression(self, prefix, ident, args);
                                if (!reachable.contains(self.getIdent())) {
                                    changed[0] = true;
                                    reachable.add(self.getIdent());
                                }
                            }
                        });
                }
            }
        } while (changed[0]);
        
        List<String> retval = new ArrayList<String>();
        retval.addAll(reachable);
        return retval;
    }

}

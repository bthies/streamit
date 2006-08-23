package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.List;
import java.util.LinkedList;

/**
 * This class inlines each phase (i.e., a helper function that does
 * I/O) of a filter into the call site of that phase.  This is useful
 * because some transformations (e.g., fusion) do not support I/O from
 * within helper functions.  By inlining the phases, all of the I/O is
 * moved to the work function.
 *
 * This class currently operates on SIRFilter's, assuming that any
 * phases are represented as plain methods that have I/O rates.  It
 * does not deal with SIRPhasedFilters, which explicitly represent the
 * phases separately -- this generality is not needed in the current
 * version of StreamIt, which does not include phases at the language
 * level (Aug 2006).
 *
 * This assumes there are no recursive functions that do I/O.
 */
public class InlinePhases {

    /**
     * Inlines all calls to phases in <filter> and removes those
     * phases from the method array.
     */ 
    public static void doit(SIRFilter filter) {
        inlineCallSites(filter);
        removePhases(filter);
    }

    /**
     * Replaces calls to phases with the body of the phase.
     */
    private static void inlineCallSites(final SIRFilter filter) {
        JMethodDeclaration[] methods = filter.getMethods();
        for (int i=0; i<methods.length; i++) {
            methods[i].accept(new SLIRReplacingVisitor() {
                    public Object visitExpressionStatement(JExpressionStatement self,
                                                           JExpression expr) {
                        // see if we have method call, with method
                        // target corresponding to phase.  Since
                        // phases have void return type, they must be
                        // immediately enclosed in an expression statement.
                        if (expr instanceof JMethodCallExpression) {
                            // see if we are calling a phase...
                            JMethodDeclaration phase = getPhase(filter, 
                                                                ((JMethodCallExpression)expr).getIdent());
                            if (phase!=null) {
                                // replace call with body of phase
                                return (JBlock)ObjectDeepCloner.deepCopy(phase.getBody());
                            }
                        }
                        // no point recursing further, as we won't find a phase
                        return self;
                    }
                });
        }
    }

    /**
     * Removes phases from the method list.
     */
    private static void removePhases(SIRFilter filter) {
        JMethodDeclaration[] methods = filter.getMethods();
        // methods that aren't phases
        List newMethods = new LinkedList();

        for (int i=0; i<methods.length; i++) {
            if (!isRemovablePhase(filter, methods[i])) {
                newMethods.add(methods[i]);
            }
        }
        filter.setMethods((JMethodDeclaration[])newMethods.toArray(new JMethodDeclaration[0]));
    }

    /**
     * Returns whether or not <method> is a phase that can be removed.
     */
    private static boolean isRemovablePhase(SIRFilter filter, JMethodDeclaration method) {
        // if does not do IO, then is not a phase so should not be
        // removed following inlining
        if (!method.doesIO()) return false;
        // keep work function
        if (method == filter.getWork()) return false;
        // keep initWork function, if present
        if (filter instanceof SIRTwoStageFilter && 
            method == ((SIRTwoStageFilter)filter).getInitWork()) {
            return false;
        }
        // otherwise method is phase that is dead following inlining
        return true;
    }
    

    /**
     * If there is a phase in <filter> with name <ident>, then return
     * that phase.  Otherwise return null.
     */
    private static JMethodDeclaration getPhase(SIRFilter filter, String ident) {
        JMethodDeclaration[] methods = filter.getMethods();
        for (int i=0; i<methods.length; i++) {
            if (methods[i].getName().equals(ident) && methods[i].doesIO()) {
                return methods[i];
            }
        }
        return null;
    }

    /**
     * The visitor that replaces calls to phases with the body of that
     * phase itself.
     */
    class PhaseInliner extends SLIRReplacingVisitor {
    }
    
}

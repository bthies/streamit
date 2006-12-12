/**
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.Hashtable;

/**
 * Final agressive filter optimization before code emission.
 * 
 * Unroller, Propagate, BlockFlattener, ArrayDestroyer (local), VarDeclRaiser on each method.
 * 
 * ArrayDestroyer (fields, if option set), DeadCodeElimination on the code unit as a whole.
 * 
 * Refactored out of cluster, raw, spacedynamic, spacetime backends
 * Also existed in rstream backend but commented out.
 * 
 * @author Allyn Dimock
 *
 */
public class FinalUnitOptimize {
    /**
     * Loop over all methods in a filter or other code unit optimizing all vetted by {@link #optimizeThisMethod()}
     *
     * @param unit  SIRCodeUnit to process.
     */
    
    public void optimize(SIRCodeUnit unit) {
        ArrayDestroyer arrayDest=new ArrayDestroyer();
        for (JMethodDeclaration method : unit.getMethods()) {
            if (! optimizeThisMethod(unit,method)) {
                continue;
            }
            Unroller unroller;
            do {
                do {
                    unroller = new Unroller(new Hashtable<JLocalVariable,JLiteral>());
                    method.accept(unroller);
                } while (unroller.hasUnrolled());
                method.accept(new Propagator(new Hashtable<JLocalVariable,Object>()));
                unroller = new Unroller(new Hashtable<JLocalVariable,JLiteral>());
                method.accept(unroller);
            } while (unroller.hasUnrolled());
            method.accept(new BlockFlattener());
            method.accept(new Propagator(new Hashtable<JLocalVariable,Object>()));
            method.accept(arrayDest);
            method.accept(new VarDeclRaiser());
        }
        if (KjcOptions.destroyfieldarray) {
            arrayDest.destroyFieldArrays(unit);
        }
        DeadCodeElimination.doit(unit);
    }
            
    /**
     * To not optimize <i>method</i> of <i>unit</i> return false from this method.
     * 
     * Made to be overridden.
     * 
     * @param unit    SIRCodeUnit in which the method occurrs
     * @param method  that we want to determine whether to optimize.
     * @return
     */
    protected boolean optimizeThisMethod(SIRCodeUnit unit, JMethodDeclaration method) {
        return true;
    }
    
}

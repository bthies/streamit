/**
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.spacetime.SpaceTimeBackend;

import java.util.Hashtable;

/**
 * Final agressive filter optimization before code emission.
 * 
 * Unroller, Propagate, BlockFlattener, ArrayDestroyer (locals), VarDeclRaiser on each method.
 * 
 * ArrayDestroyer (fields, if option set), DeadCodeElimination on the code unit as a whole.
 * 
 * Refactored out of cluster, raw, spacedynamic, spacetime backends.
 * Also existed in rstream backend but commented out.
 * 
 * @author Allyn Dimock
 *
 */
public class FinalUnitOptimize {
    /**
     * Loop over all methods in a code unit optimizing all vetted by {@link #optimizeThisMethod(SIRCodeUnit,JMethodDeclaration) optimizeThisMethod}
     * running standard optimization passes as mentioned in class header.
     * 
     * @param unit  SIRCodeUnit to process.
     */
    
    public void optimize(SIRCodeUnit unit) {
        // remove multiple copies of propagated static variables.
        StaticsProp.shareStaticVars(unit,null);

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
	}

	// could iterate to fixed point, but field prop slow in current form
	for (int i=0; i<2; i++) {
	    ArrayDestroyer arrayDest=new ArrayDestroyer();
	    for (JMethodDeclaration method : unit.getMethods() ) {
		if (! optimizeThisMethod(unit,method)) {
		    continue;
		}
		method.accept(new BlockFlattener());
		method.accept(new Propagator(new Hashtable<JLocalVariable,Object>()));
		method.accept(arrayDest);
		method.accept(new VarDeclRaiser());
	    }
	    if (KjcOptions.destroyfieldarray) {
		arrayDest.destroyFieldArrays(unit);
	    }
	    // only field prop the first time, and only repeat the loop if something was propagated
	    if (i==1 || !FieldProp.propagateWithoutUnrolling(unit)) { break; }
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

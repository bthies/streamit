package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

/**
 * This class propagates constants and unrolls loops.  Currently only
 * works for init functions.
 */
public class ConstantProp {

    private ConstantProp() {
    }

    /**
     * Propagates constants as far as possible in <str> and also
     * unrolls loops.
     */
    public static void propagateAndUnroll(SIRStream str) {
	// start at the outermost loop with an empty set of constants
	new ConstantProp().propagateAndUnroll(str, new Hashtable());
    }

    /**
     * Does the work on <str>, given that <constants> maps from
     * a JLocalVariable to a JLiteral for all constants that are known.
     */
    private void propagateAndUnroll(SIRStream str, Hashtable constants) {
	// propagate constants within init function of <str>
	str.getInit().accept(new Propagator(constants));
	// unroll loops within init function of <str>
	//str.getInit().accept(new Unroller(constants));
	// recurse into sub-streams
    }
}

/**
 * This class propagates constants and partially evaluates all
 * expressions as much as possible.
 */
class Propagator extends EmptyAttributeVisitor {
    /**
     * Map of known constants (JLocalVariable -> JLiteral)
     */
    private Hashtable constants;

    /**
     * Creates one of these given that <constants> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.
     */
    public Propagator(Hashtable constants) {
	super();
	this.constants = constants;
    }

}



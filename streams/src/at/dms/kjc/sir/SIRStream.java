package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This class represents a stream structure with one input and one
 * output.
 */
public class SIRStream extends SIROperator {
    /**
     * The fields of this, not including the input and output channels.  */
    protected JFieldDeclaration[] fields;
    /**
     * The user-defined methods of this, not including work, init,
     * initPath, etc.  This includes all message handlers and local
     * utility functions that are used within this structure.
     */
    protected JMethodDeclaration[] methods;
    /**
     * The init function.
     */
    protected JMethodDeclaration init;

    // don't set the init function upon instantation since the lowering
    // pass will have to create the init function
    protected SIRStream(JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
	this.fields = fields;
	this.methods = methods;
	this.init = init;
    }
}


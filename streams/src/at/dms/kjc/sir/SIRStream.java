package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This class represents a stream structure with one input and one
 * output.
 */
public abstract class SIRStream extends SIROperator {
    /**
     * The fields of this, not including the input and output channels.  */
    protected JFieldDeclaration[] fields;
    /**
     * The methods of this, INCLUDING work, init, initPath, etc.  This
     * includes all message handlers and local utility functions that
     * are used within this structure. 
     */
    protected JMethodDeclaration[] methods;
    /**
     * The init function.
     */
    protected JMethodDeclaration init;

    /*
     * Don't set the init function upon instantation since the lowering
     * pass will have to create the init function
     */
    protected SIRStream(SIRStream parent,
			JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
      super(parent);
      this.fields = fields;
      this.methods = methods;
    }

    protected SIRStream() {
	super(null);
	this.fields = null;
	this.methods = null;
    }


    /*
     * Set the fields member variable 
     */
    public void setFields (JFieldDeclaration[] f) {
	this.fields = f;
    }

    /*
     * Set the methods member variable 
     */
    public void setMethods (JMethodDeclaration[] m) {
	this.methods = m;
    }

    /**
     * sets the init function
     */
    public void setInit(JMethodDeclaration init) {
	this.init = init;
    }

}


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
	
	addMethod(init);
    }

    /**
     * adds method <meth> to this, if <meth> is not already registered
     * as a method of this.  
     */
    public void addMethod(JMethodDeclaration method) {
	// see if we already have <method> in this
	for (int i=0; i<methods.length; i++) {
	    if (methods[i]==method) {
		return;
	    }
	}
	// otherwise, create new methods array
	JMethodDeclaration[] newMethods = new JMethodDeclaration[methods.length
								+ 1];
	// copy in new method
	newMethods[0] = method;
	// copy in old methods
	for (int i=0; i<methods.length; i++) {
	    newMethods[i+1] = methods[i];
	}
	// reset old to new
	this.methods = newMethods;
    }

    
    /**
     * adds field <field> to this, if <field> is not already registered
     * as a field of this.  
     */
    public void addField(JFieldDeclaration field) {
	// see if we already have <field> in this
	for (int i=0; i<fields.length; i++) {
	    if (fields[i]==field) {
		return;
	    }
	}
	// otherwise, create new fields array
	JFieldDeclaration[] newFields = new JFieldDeclaration[fields.length
								+ 1];
	// copy in new field
	newFields[0] = field;
	// copy in old fields
	for (int i=0; i<fields.length; i++) {
	    newFields[i+1] = fields[i];
	}
	// reset old to new
	this.fields = newFields;
    }


    /**
     * Returns whether or not a function named <name> is defined in this.
     */
    public boolean hasMethod(String name) {
	for (int i=0; i<methods.length; i++) {
	    if (methods[i].getName().equals(name)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * gets the init function
     */
    public JMethodDeclaration getInit() {
	return init;
    }

}


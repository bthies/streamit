package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;

/**
 * This class represents a stream structure with one input and one
 * output.
 */
public abstract class SIRStream extends SIROperator implements Cloneable{
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
    /**
     * The work function.  Must be non-null for filters, but may be null
     * for SIRContainers.
     */
    protected JMethodDeclaration work;
    /**
     * The name of the class in the StreamIt source file corresponding
     * to this stream.
     */
    protected String ident;

    /*
     * Don't set the init function upon instantation since the lowering
     * pass will have to create the init function
     */
    protected SIRStream(SIRContainer parent,
			String ident,
			JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
      super(parent);
      this.ident = ident;
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

    /**
     * Adds <f> to the fields of this.  Does not check
     * for duplicates.
     */
    public void addFields (JFieldDeclaration[] f) {
	JFieldDeclaration[] newFields = 
	    new JFieldDeclaration[fields.length + f.length];
	for (int i=0; i<fields.length; i++) {
	    newFields[i] = fields[i];
	}
	for (int i=0; i<f.length; i++) {
	    newFields[fields.length+i] = f[i];
	}
	this.fields = newFields;
    }

    /**
     * Adds <m> to the methods of this.  Does not check for
     * duplicates. 
     */
    public void addMethods (JMethodDeclaration[] m) {
	JMethodDeclaration[] newMethods = 
	    new JMethodDeclaration[methods.length + m.length];
	for (int i=0; i<methods.length; i++) {
	    System.err.println("adding (old) method " + methods[i].getName() + " to " + this);
	    newMethods[i] = methods[i];
	}
	for (int i=0; i<m.length; i++) {
	    System.err.println("adding (new) method " + m[i].getName() + " to " + this);
	    newMethods[methods.length+i] = m[i];
	}
	this.methods = newMethods;
    }

    /**
     * Gets the field decl's of this stream.
     */
    public JFieldDeclaration[] getFields() {
	return fields;
    }

    /**
     * Gets the method decl's of this stream.
     */
    public JMethodDeclaration[] getMethods() {
	return methods;
    }

    /**
     * Sets the identifier of this.
     */
    public void setIdent(String ident) {
	this.ident = ident;
    }

    public String getIdent() {
	return ident;
    }

    /**
     * Sets the work function.
     */
    public void setWork (JMethodDeclaration newWork) {
	addReplacementMethod(newWork, this.work);
	this.work = newWork;
    }

    /**
     * Gets the work function.
     */
    public JMethodDeclaration getWork () {
	return this.work;
    }

    /*
     * Set the methods member variable 
     */
    public void setMethods (JMethodDeclaration[] m) {
	this.methods = m;
    }

    /**
     * Returns the output type of this.
     */
    public abstract CType getOutputType();

    /**
     * Returns the type of this stream.
     */
    public abstract LIRStreamType getStreamType();

    /**
     * Returns the input type of this.
     */
    public abstract CType getInputType();

    /**
     * Returns whether or not this class needs a call to an init
     * function to be generated.  Special library functions like
     * FileReader's and FileWriter's do not need an init call.
     */
    public boolean needsInit() {
	return true;
    }

    /**
     * Returns whether or not this class needs a call to a work
     * function to be generated.  Special library functions like
     * FileReader's and FileWriter's do not need a work call at the
     * level of the Kopi IR (it is generated in C).
     */
    public boolean needsWork() {
	return true;
    }

    /**
     * sets the init function
     */
    public void setInit(JMethodDeclaration newInit) {
	addReplacementMethod(newInit, this.init);
	this.init = newInit;
    }

    /**
     * sets the init function WITHOUT eliminating the old one in the
     * methods array.  Usually you want to use the plain setInit
     * unless you're fusing.
     */
    public void setInitWithoutReplacement(JMethodDeclaration newInit) {
	this.init = newInit;
    }

    /**
     * Adds <newMethod> to this.  If <origMethod> is a method of this,
     * then <newMethod> takes its first place (<origMethod> is
     * removed).  If <origMethod> is null, then <newMethod> is just
     * added to this.
     *
     * Assumes <origMethod> appears at most once in this (as should
     * all methods.)
     */
    protected void addReplacementMethod(JMethodDeclaration newMethod,
					JMethodDeclaration origMethod) {
	// check if <origMethod> is null
	if (origMethod==null) {
	    // if yes, then just add <newMethod>
	    addMethod(newMethod);
	} else {
	    //otherwise, try swappping the old init function with new
	    int i;
	    for (i=0; i<methods.length; i++) {
		if (methods[i]==origMethod) { 
		    methods[i]=newMethod;
		    break;
		}
	    }
	    // if we didn't find it, then just add <newMethod>
	    if (i==methods.length) {
		addMethod(newMethod);
	    }
	}
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

    abstract public Object clone();
}


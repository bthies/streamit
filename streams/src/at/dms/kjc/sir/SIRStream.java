package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import at.dms.util.Utils;
import java.util.*;

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
     * Can be null
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
	this(null, null, JFieldDeclaration.EMPTY(), JMethodDeclaration.EMPTY());
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
	    newMethods[i] = methods[i];
	}
	for (int i=0; i<m.length; i++) {
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
	checkRep();
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

    public String toString() {
	return (this.getClass()) + " " + getIdent();
    }

    /**
     * Sets the work function.
     */
    public void setWork (JMethodDeclaration newWork) {
	addReplacementMethod(newWork, this.work);
	this.work = newWork;
    }

    /**
     * Get the parameters that were used to initialize this.  Returns
     * null if this is a top-level stream; returns an empty list if
     * the parameters haven't been resolved yet.
     */
    public List getParams() {
	if (parent==null) {
	    return null;
	} else {
	    return parent.getParams(parent.indexOf(this));
	} 
    }

    /**
     * Gets the work function.
     */
    public JMethodDeclaration getWork () {
	return this.work;
    }

    /*
     * Set the methods member variable.  Asserts that all the methods
     * in <m> are non-null.
     */
    public void setMethods (JMethodDeclaration[] m) {
	if(m!=null)
	    for (int i=0; i<m.length; i++) {
		Utils.assert(m[i]!=null, "Detected a null method in SIRStream.setMethods");
	    }
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
     * Returns the number of items pushed out of this stream in one
     * execution of the steady-state, given the steady-state execution
     * counts specified by <counts> (as in SIRScheduler.)  Requires
     * that <counts> contains a firing for each *filter* that
     * contained in the hierarchy of this (does not depend on
     * splitters / joiners.)
     *
     * The intent of this method is that <counts> can be calculated
     * once for a stream graph, then the splitjoins & pipelines of the
     * graph can be refactored, and one can still call this to get the
     * push count of new constructs without re-invoking the scheduler.
     */
    public abstract int getPushForSchedule(HashMap[] counts);

    /**
     * Same as getPushForSchedule, but with pop instead of push.
     */
    public abstract int getPopForSchedule(HashMap[] counts);

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
	if (newInit!=null) {
	    addReplacementMethod(newInit, this.init);
	}
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
     * as a method of this.  Requires that <method> is non-null.
     */
    public void addMethod(JMethodDeclaration method) {
	Utils.assert(method!=null);
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
     * Returns the C type of the object, which is always a
     * stream_context. In the generated C code, there will be a
     * structure by this name to hold the state of the stream.  This
     * is SPECIFIC TO THE UNIPROCESSOR BACKEND.
     */
    public String getTypeNameInC() {
	return getName();
    }
    

    /**
     * gets the init function
     */
    public JMethodDeclaration getInit() {
	return init;
    }

    public boolean insideFeedbackLoop() {
	SIRContainer currentParent = this.getParent();
	
	while (currentParent != null) {
	    if (currentParent instanceof SIRFeedbackLoop)
		return true;
	    currentParent = currentParent.getParent();
	}
	//did not hit a feedback loop 
	return false;
    }

    /**
     * Checks representation of this.
     */
    private void checkRep() {
	// check that <methods> containts init and work if we need it
	// (and that it contains it just once.)
	boolean foundInit = false;
	boolean foundWork = false;
	for (int i=0; i<methods.length; i++) {
	    if (methods[i]==getInit()) {
		Utils.assert(!foundInit);
		foundInit = true;
	    }
	    if (methods[i]==getWork()) {
		Utils.assert(!foundWork);
		foundWork = true;
	    }
	}
	Utils.assert(foundInit || !needsInit() || getInit()==null);
	Utils.assert(foundWork || !needsWork() || getWork()==null);
    }

    /**
     * Returns an empty init function.
     */
    public static JMethodDeclaration makeEmptyInit() {
	return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC, 
				      CStdType.Void, "init",
				      JFormalParameter.EMPTY, CClassType.EMPTY,
				      new JBlock(), null, null);
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRStream other) {
  super.deepCloneInto(other);
  other.fields = (at.dms.kjc.JFieldDeclaration[])at.dms.kjc.AutoCloner.cloneToplevel(this.fields, this);
  other.methods = (at.dms.kjc.JMethodDeclaration[])at.dms.kjc.AutoCloner.cloneToplevel(this.methods, this);
  other.init = (at.dms.kjc.JMethodDeclaration)at.dms.kjc.AutoCloner.cloneToplevel(this.init, this);
  other.work = (at.dms.kjc.JMethodDeclaration)at.dms.kjc.AutoCloner.cloneToplevel(this.work, this);
  other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}


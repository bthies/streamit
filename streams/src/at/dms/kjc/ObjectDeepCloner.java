//===========================================================================
//
//   FILE: ObjectDeepCloner.java:
//   
//   Author: Michael Gordon
//   Date: Wed Oct 17 14:03:33 2001
//
//   Function:  Deep clone using Serializable
//
//===========================================================================

package at.dms.kjc;

import at.dms.kjc.sir.*;
import at.dms.util.*;
import java.io.*;
import java.util.*;

/**
 * This class implements general deep cloning using the serializable interface
 */
public class ObjectDeepCloner
{
    /**
     * List of things that should be cloned on the current pass.
     */
    private static LinkedList toBeCloned;

    /**
     * List of objects we're preserving across a cloning operation.
     */
    private static LinkedList preserved;

    // so that nobody can accidentally create an ObjectCloner object
    private ObjectDeepCloner(){}

    /**
     * Deep copy a stream structure.
     */
    static public Object deepCopy(SIROperator oldObj) {
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	oldObj.accept(visitor);
	toBeCloned = visitor.getToBeCloned();
	return doCopy(oldObj);
    }

    /**
     * Deep copy a KJC structure.
     */
    static public Object deepCopy(JPhylum oldObj) {
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	oldObj.accept(visitor);
	toBeCloned = visitor.getToBeCloned();
	return doCopy(oldObj);
    }

    /**
     * Return a handle for <oldInstance> that it can store to protect
     * its identity across a serialization operation.
     */
    static public Object getHandle(Object oldInstance) {
	if (toBeCloned.contains(oldInstance)) {
	    return new Integer(-1);
	} else {
	    preserved.add(oldInstance);
	    return new Integer(preserved.size() - 1);
	}
    }

    /**
     * Given that <newInstance> finds itself being unserialized, this
     * method returns what its new representation should be given that
     * it was handed <handle> prior to the serialization.
     */
    static public Object getInstance(Object handle, Object newInstance) {
	Utils.assert(handle instanceof Integer,
		     "DeepObjectCloner being called with a handle it didn't "
		     + " give out:  handle is " + handle + " of type " +
		     handle.getClass());
	int index = ((Integer)handle).intValue();
	// if the instance was not preserved, then return current instance
	if (index==-1) {
	    /*
	    System.err.println("Cloning container " + newInstance);
	    */
	    return newInstance;
	} else {
	    /*
	    System.err.println("Preserving container " + preserved.get(index));
	    */
	    // otherwise, return our old preserved version
	    return preserved.get(index);
	}
    }

    /**
     * Returns the deep clone of an object, if <cloneVars> is true
     * then clone vars also...
     */ 
    static private Object doCopy(Object oldObj)
    {
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;
	try
	    {
		// clear the list of objects we're preserving
		preserved = new LinkedList();
		// get an output stream ready
		ByteArrayOutputStream bos = 
		    new ByteArrayOutputStream();
		oos = new ObjectOutputStream(bos);
		// serialize and pass the object
		oos.writeObject(oldObj);  
		oos.flush();              
		ByteArrayInputStream bin = 
		    new ByteArrayInputStream(bos.toByteArray()); 
		ois = new ObjectInputStream(bin);                  
		// return the new object
		oos.close();
		ois.close();
		return ois.readObject(); 
	    }
	catch(Exception e)
	    {
		System.err.println("Exception in ObjectCloner = " + e);
		e.printStackTrace();
		System.exit(-1);
	 
	    }
	return null;
    }
}

class CloningVisitor extends SLIREmptyVisitor implements StreamVisitor {

    /**
     * A list of things that *should* be cloned.  Currently the
     * following types should not be cloned unless they are an
     * element of this list:
     *   - SIRContainer
     *   - JLocalVariable
     */
    private LinkedList toBeCloned;

    public CloningVisitor() {
	this.toBeCloned = new LinkedList();
    }

    /**
     * Return the list of what should be cloned.
     */
    public LinkedList getToBeCloned() {
	return toBeCloned;
    }

    /**
     * Right now the super doesn't visit the variable in a jlocal var,
     * but make sure we don't, either.
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
					     String ident) {
    }

    /**
     * Visits a variable decl.
     */
    public void visitVariableDefinition(JVariableDefinition self,
					int modifiers,
					CType type,
					String ident,
					JExpression expr) {
	super.visitVariableDefinition(self, modifiers, type, ident, expr);
	// record that we should clone this, since we reached it
	toBeCloned.add(self);
    }
    
    /**
     * visits a formal param.
     */
    public void visitFormalParameters(JFormalParameter self,
				      boolean isFinal,
				      CType type,
				      String ident) {
	super.visitFormalParameters(self, isFinal, type, ident);
	// record that we should clone this, since we reached it
	toBeCloned.add(self);
    }

    /**
     * Visits an init statement (recurses into the target stream)
     */
    public void visitInitStatement(SIRInitStatement self,
				   JExpression[] args,
				   SIRStream target) {
	super.visitInitStatement(self, args, target);
	// also recurse into the stream target
	target.accept(this);
    }

    /**
     * PLAIN-VISITS 
     */

    /**
     * For visiting all fields and methods of SIRStreams.
     */
    private void visitStream(SIRStream stream) {
	// visit the methods
	JMethodDeclaration[] methods = stream.getMethods();
	for (int i=0; i<methods.length; i++) {
	    methods[i].accept(this);
	}
	// visit the fields
	JFieldDeclaration[] fields = stream.getFields();
	for (int i=0; i<fields.length; i++) {
	    fields[i].accept(this);
	}
    }
	    
    /* visit a structure */
    public void visitStructure(SIRStructure self,
                               SIRStream parent,
                               JFieldDeclaration[] fields) {
        visitStream(self);
    }
    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init,
			    JMethodDeclaration work,
			    CType inputType, CType outputType) {
	// visit node
	visitStream(self);
    }
  
    /* visit a splitter */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      JExpression[] weights) {
	// don't do anything since a filter isn't an sir stream
    }
	
    /* visit a joiner -- don't do anything since a joiner isn't an
     * SIRStream.  */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    JExpression[] weights) {
	// don't do anything since a filter isn't an sir stream
    }

    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit children
	for (int i=0; i<elements.size(); i++) {
	    ((SIRStream)elements.get(i)).accept(this);
	}
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit splitter
	self.getSplitter().accept(this);
	// visit children
	for (int i=0; i<self.size(); i++) {
	    ((SIRStream)self.get(i)).accept(this);
	}
	// visit joiner
	self.getJoiner().accept(this);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit joiner
	self.getJoiner().accept(this);
	// visit body stream
	self.getBody().accept(this);
	// visit splitter
	self.getSplitter().accept(this);
	// visit loop stream
	self.getLoop().accept(this);
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline -- do nothing, visit on way down */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
    }

    /* post-visit a splitjoin -- do nothing, visit on way down */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
    }

    /* post-visit a feedbackloop -- do nothing, visit on way down */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
    }
    
}

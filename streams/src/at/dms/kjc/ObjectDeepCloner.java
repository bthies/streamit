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
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import java.io.*;
import java.util.*;
import at.dms.compiler.JavaStyleComment;

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
    static public Object deepCopy(SIRStream oldObj) {
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	IterFactory.createIter(oldObj).accept(visitor);
	toBeCloned = visitor.getToBeCloned();
	return doCopy(oldObj);
    }

    /**
     * Deep copy a stream container, except do not clone any of its
     * child streams.  This means that the LIST of children is copied,
     * but the children themselves are not duplicated.  Splitters and
     * joiners are not considered as children - only SIRStreams.  If
     * <oldObj> is not an SIRContainer, then this has the same effect
     * as deepCopy.  
     *
     * This is only intended for use from the iterator package, and
     * should not be called from within the IR.
     */
    static public Object shallowCopy(SIRStream oldObj) {
	// only do something different for containers
	if (!(oldObj instanceof SIRContainer)) {
	    return deepCopy(oldObj);
	} 
	SIRContainer parent = (SIRContainer)oldObj;
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	IterFactory.createIter(parent).accept(visitor);
	toBeCloned = visitor.getToBeCloned();
	// subtract the list of <parent>'s children from the
	// toBeCloned list.
	for (ListIterator it=parent.getChildren().listIterator(); it.hasNext(); ) {
	    Object o = it.next();
	    if (toBeCloned.contains(o)) {
		toBeCloned.remove(o);
	    }
	}
	return doCopy(parent);
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
     * Clone everything starting from this offset of the block
     * Useful in BranchAnalyzer
     */
    static public Object deepCopy(int offset,JBlock oldObj) {
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	visitor.visitBlockStatement(offset,oldObj,oldObj.getComments());
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
     * Used by deepCopy(int offset,JBlock oldObj) above
     */
    public void visitBlockStatement(int offset,
				    JBlock self,
				    JavaStyleComment[] comments) {
	for(;offset<self.size();offset++) {
	    self.getStatement(offset).accept(this);
	}
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
				   SIRStream target) {
	super.visitInitStatement(self, target);
	// also recurse into the stream target
	IterFactory.createIter(target).accept(this);
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
        if (methods != null) {
            for (int i=0; i<methods.length; i++) {
                methods[i].accept(this);
            }
        }
	// visit the fields
	JFieldDeclaration[] fields = stream.getFields();
	for (int i=0; i<fields.length; i++) {
	    fields[i].accept(this);
	}
    }
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	// visit node
	visitStream(self);
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit children
	for (int i=0; i<self.size(); i++) {
	    iter.get(i).accept(this);
	}
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit children
	for (int i=0; i<self.size(); i++) {
	    iter.get(i).accept(this);
	}
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit body stream
	iter.get(SIRFeedbackLoop.BODY).accept(this);
	// visit loop stream
	iter.get(SIRFeedbackLoop.BODY).accept(this);
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline -- do nothing, visit on way down */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
    }

    /* post-visit a splitjoin -- do nothing, visit on way down */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
    }

    /* post-visit a feedbackloop -- do nothing, visit on way down */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
    }
}


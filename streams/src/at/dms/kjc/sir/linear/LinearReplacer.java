package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A LinearReplacer is the base class that all replacers that make
 * use of linear information inherit from.<br>
 *
 * $Id: LinearReplacer.java,v 1.18 2003-06-02 18:19:23 aalamb Exp $
 **/
public abstract class LinearReplacer extends EmptyStreamVisitor implements Constants{
    // in visitors of containers, only make a replacement if we're
    // enabling linear collapsing (note this relies on evaluation
    // order of and-expression), and only clear the children if we
    // make a replacement (to prevent the visitor from descending into
    // disconnected stream components).
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	if (!KjcOptions.nolinearcollapse && makeReplacement(self)) { self.clear(); }
    }
    public void preVisitPipeline(    SIRPipeline self,
				     SIRPipelineIter iter){
	if (!KjcOptions.nolinearcollapse && makeReplacement(self)) { self.clear(); }
    }
    public void preVisitSplitJoin(   SIRSplitJoin self,
				     SIRSplitJoinIter iter){
	if (!KjcOptions.nolinearcollapse && makeReplacement(self)) { self.clear(); }
    }
    public void visitFilter(         SIRFilter self,
				     SIRFilterIter iter){
	makeReplacement(self);
    }

    /**
     * Visit a pipeline, splitjoin or filter, replacing them with a
     * new filter that presumably executes more efficiently.
     * Returns whether or not any replacement was done.
     **/
    public abstract boolean makeReplacement(SIRStream self);
    
    /**
     * Gets all children of the specified stream.
     **/
    HashSet getAllChildren(SIRStream self) {
	// basically, push a new visitor through which keeps track of the
	LinearChildCounter kidCounter = new LinearChildCounter();
	// stuff the counter through the stream
	IterFactory.createIter(self).accept(kidCounter);
	return kidCounter.getKids();
	
    }
    /** Inner class to get a list of all the children streams. **/
    class LinearChildCounter extends EmptyStreamVisitor {
	HashSet kids = new HashSet();
	public HashSet getKids() {return this.kids;}
	public void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {kids.add(self);}
	public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter){kids.add(self);}
	public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter){kids.add(self);}
	public void visitFilter(SIRFilter self, SIRFilterIter iter){kids.add(self);}
    }

    /**
     * Returns an array that is a field declaration for (newField)
     * appended to the original field declarations.
     **/
    public JFieldDeclaration[] appendFieldDeclaration(JFieldDeclaration[] originals,
						      JVariableDefinition newField) {
	
	/* really simple -- make a new array, copy the elements from the
	 * old array and then stick in the new one. */
	JFieldDeclaration[] returnFields = new JFieldDeclaration[originals.length+1];
	for (int i=0; i<originals.length; i++) {
	    returnFields[i] = originals[i];
	}
	/* now, stick in a new field declaration. */
	returnFields[originals.length]   = new JFieldDeclaration(null, newField, null, null);
	return returnFields;
    }


    /** Returns the type to use for buffers. In this case float[]. **/
    public CType getArrayType() {
	return new CArrayType(CStdType.Float, 1);
    }
    
    /** Appends two arrays of field declarations and returns the result. **/
    public JFieldDeclaration[] appendFieldDeclarations(JFieldDeclaration[] decl1,
						       JFieldDeclaration[] decl2) {
	JFieldDeclaration[] allDecls = new JFieldDeclaration[decl1.length + decl2.length];
	// copy delc1
	for (int i=0; i<decl1.length; i++) {
	    allDecls[i] = decl1[i];
	}
	// copy decl2
	for (int i=0; i<decl2.length; i++) {
	    allDecls[decl1.length+i] = decl2[i];
	}
	return allDecls;
    }

    /** Make an array of one java comments from a string. **/
    public JavaStyleComment[] makeComment(String c) {
	JavaStyleComment[] container = new JavaStyleComment[1];
	container[0] = new JavaStyleComment(c,
					    true,   /* isLineComment */
					    false,  /* space before */
					    false); /* space after */
	return container;
    }

    /**
     * Create an array allocation expression. Allocates a one dimensional array of floats
     * for the field of name fieldName of fieldSize.
     **/
    public JStatement makeFieldAllocation(String fieldName, int fieldSize, String commentString) {
	JExpression fieldExpr = makeFieldAccessExpression(fieldName);
	JExpression fieldAssign = new JAssignmentExpression(null, fieldExpr, getNewArrayExpression(fieldSize));
	JavaStyleComment[] comment = makeComment(commentString); 
	return new JExpressionStatement(null, fieldAssign, comment);
    }

    /**
     * Create a field access expression for the field named "name"
     **/
    public JExpression makeFieldAccessExpression(String name) {
	return new JFieldAccessExpression(null, new JThisExpression(null), name);
    }

    /**
     * Creates a statement assigning "right" to "left".
     **/
    public JStatement makeAssignmentStatement(JExpression left, JExpression right) {
	return new JExpressionStatement(null, new JAssignmentExpression(null, left, right), null);
    }
    
    /** Creates a local variable expression. **/
    public JExpression makeLocalVarExpression(JLocalVariable var) {
	return new JLocalVariableExpression(null, var);
    }

    /** Creates a less than expression: left &lt; right. **/
    public JExpression makeLessThanExpression(JExpression left, JExpression right) {
	return new JRelationalExpression(null, OPE_LT, left, right);
    }

    /** Creates a post increment statement: expr++. **/
    public JStatement makeIncrementStatement(JExpression expr) {
	JExpression incExpr = new JPostfixExpression(null, OPE_POSTINC, expr);
	return new JExpressionStatement(null, incExpr, null);
    }
    
    
    /**
     * Initializes a field to a particular integer value.
     **/
    public JStatement makeFieldInitialization(String name, int initValue, String commentString) {
	JExpression fieldExpr = new JFieldAccessExpression(null, new JThisExpression(null), name);
	JExpression fieldAssign = new JAssignmentExpression(null, fieldExpr,
							    new JIntLiteral(null,initValue));
	JavaStyleComment[] comment = makeComment(commentString); 
	return new JExpressionStatement(null, fieldAssign, comment);
    }

    /** make a JNewArrayStatement that allocates size elements of a float array. */
    public JNewArrayExpression getNewArrayExpression(int size) {
	/* make the size array. */
	JExpression[] arrayDims = new JExpression[1];
	arrayDims[0] = new JIntLiteral(size);
	return new JNewArrayExpression(null,           /* token reference */
				       getArrayType(), /* type */
				       arrayDims,      /* size */
				       null);          /* initializer */
    }

    /* Makes a field array access expression of the form this.arrField[index]. */
    public JExpression makeArrayFieldAccessExpr(JLocalVariable arrField, int index) {
	return makeArrayFieldAccessExpr(arrField, new JIntLiteral(index));
    }

    /**
     * Makes a field array access expression of the form
     * prefix-arrField[index], where user can set prefix.  (For
     * example, prefix could be "this", or another array access
     * expression.)
     **/
    public JExpression makeArrayFieldAccessExpr(JLocalVariable arrField, JExpression index) {
	/* first, make the prefix-arr1[index] expression */
	JExpression fieldAccessExpr;
	fieldAccessExpr = new JFieldAccessExpression(null,
						     new JThisExpression(null),
						     arrField.getIdent());
	JExpression fieldArrayAccessExpr;
	fieldArrayAccessExpr = new JArrayAccessExpression(null,
							  fieldAccessExpr,
							  index,
							  arrField.getType());
	
	return fieldArrayAccessExpr;
    }

    /* Makes a field array access expression of the form this.arrField[index]. */
    public JExpression makeArrayFieldAccessExpr(String arrFieldName, int index) {
	JExpression fieldAccessExpr = makeFieldAccessExpression(arrFieldName);
	JExpression arrayIndex = new JIntLiteral(index);
	JExpression arrayAccessExpression = new JArrayAccessExpression(null,
								       fieldAccessExpr,
								       arrayIndex);
	return arrayAccessExpression;
    }
}

package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A LinearFilterDirectReplacer replaces the contents of the work functions for
 * linear filters (as determined by the linear filter analyzer) with an appripriate
 * direct implementation (eg a bunch of push statements with the specified
 * combination of input values. <p>
 * Eg a filter that had linear form [1 2 3]+4 would get a work function:
 * <pre>
 * work {
 *   push(3*peek(0) + 2*peek(1) + 1*peek(2) + 4);
 * }
 * </pre>
 * <p>
 * $Id: LinearFilterDirectReplacer.java,v 1.3 2002-09-11 19:36:07 aalamb Exp $
 **/
public class LinearFilterDirectReplacer extends EmptyStreamVisitor implements Constants{
    LinearFilterAnalyzer linearityInformation;
    public LinearFilterDirectReplacer(LinearFilterAnalyzer lfa) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer passed to constructor!");
	}
	this.linearityInformation = lfa;
    }

    public void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter){}
    public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter){}
    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter){}
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter){}
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter){}
    public void visitFilter(SIRFilter self, SIRFilterIter iter){
	// if we have a linear representation of this filter
	if (linearityInformation.hasLinearRepresentation(self)) {
	    // ensure that we have a literal for the number of pops that this filter does
	    if (!(self.getPop() instanceof JIntLiteral)) {
		throw new RuntimeException("Non integer literal pop count!!!");
	    }
	    System.out.println("Creating direct linear implementation of " +
			       self.getIdent() +
			       "(" + self.getName() + ")");
	    // create a new work function that calculates the linear representation directly
	    JMethodDeclaration newWork = makeDirectImplementation(linearityInformation.getLinearRepresentation(self),
								  self.getInputType(),
								  self.getOutputType(),
								  ((JIntLiteral)self.getPop()).intValue());
	    // set the work function of the filter to be the new work function we just made
	    self.setWork(newWork);
	}
	
    }


    /**
     * Create a method that computes the function represented in the
     * linear form. (Eg it pushes the direct sum of inputs to the output.)
     * inputType is the variable type of the peek/pop expression that this filter uses
     * and output type is the type of the pushExpressions.<p>
     *
     * The basic format of the resulting method is:<p>
     * <pre>
     * push(a1*peek(0) + b1*peek(1) + ... + x1*peek(n));
     * push(a2*peek(0) + b2*peek(1) + ... + x2*peek(n));
     * ...
     * pop();
     * pop();
     * ...
     * </pre>
     **/
    JMethodDeclaration makeDirectImplementation(LinearFilterRepresentation representation,
						CType inputType,
						CType outputType,
						int popCount) {
	// generate the push expressions that will make up the body of the
	// new work method.
	Vector pushStatements = makePushStatementVector(representation, inputType, outputType);
	// make a vector filled with the appropriate number of pop expressions
	Vector popStatements  = new Vector();
	for (int i=0; i<popCount; i++) {
	    SIRPopExpression popExpr = new SIRPopExpression(inputType);
	    // wrap the pop expression so it is a statement.
	    JExpressionStatement popWrapper = new JExpressionStatement(null, // token reference,
								       popExpr, // expr
								       new JavaStyleComment[0]);  // comments
	    popStatements.add(popWrapper);
	}

	// now, generate the body of the new method, concatenating push then pop expressions
	JBlock body = new JBlock();
	body.addAllStatements(pushStatements);
	body.addAllStatements(popStatements);

	// now, assemble the pieces needed for a new JMethod.

	return new JMethodDeclaration(null, // tokenReference
				      ACC_PUBLIC,//modifiers
				      CStdType.Void, // returnType
				      "work",
				      new JFormalParameter[0], // params
				      new CClassType[0], // exceptions
				      body, // body (obviously)
				      null, // javadoc
				      new JavaStyleComment[0]); // comments
				      
				      

    }

    /**
     * Generate a Vector of JExprssionStatements which wrap
     * SIRPushExpressions that implement (directly) the
     * matrix multiplication represented by the linear representation.
     **/
    public Vector makePushStatementVector(LinearFilterRepresentation representation,
					   CType inputType,
					   CType outputType) {
	Vector returnVector = new Vector();

	int peekCount = representation.getPeekCount();
	int pushCount = representation.getPushCount();

	// for each output value (eg push count), construct push expression
	for (int i = 0; i < pushCount; i++) {
	    // the first push will have index pushCount, etc.
	    int currentPushIndex = pushCount - 1 - i;
	    
	    // go through each of the elements in this column of the matrix. If the element
	    // is non zero, then we want to produce a peek(index)*weight term (which we will then add together).
	    // Currently bomb out if we have a non real number (no way to generate non-reals at the present).
	    Vector combinationExpressions = new Vector();

	    // a note about indexes: the matrix [[0] [1] [2]] implies peek(0)*2 + peek(1)*1 + peek(2)*0.
	    for (int j = 0; j < peekCount; j++) {
		int currentPeekIndex = peekCount - 1 - j;
		System.out.println("peekCount: " + peekCount);
		System.out.println("pushCount: " + pushCount);
		System.out.println("currentPeekIndex: " + currentPeekIndex);
		System.out.println("currentPushIndex: " + currentPushIndex);
		ComplexNumber currentWeight = representation.getA().getElement(currentPeekIndex,
									       currentPushIndex);
		// if we have a non real number, bomb Mr. Exception
		if (!currentWeight.isReal()) {
		    throw new RuntimeException("Direct implementation with complex " +
					       "numbers is not yet implemented.");
		}

		// if we have a non zero weight...
		if (!(currentWeight.equals(ComplexNumber.ZERO))) {
		    // make an integer IR node for the current weight
		    JDoubleLiteral weightNode = new JDoubleLiteral(null, currentWeight.getReal());
		    // make an integer IR node for the appropriate peek index (peek (0) corresponds to
		    // to the array row of  at peekSize-1
		    JIntLiteral peekOffsetNode = new JIntLiteral(j);
		    // make a peek expression with the appropriate index
		    SIRPeekExpression peekNode = new SIRPeekExpression(peekOffsetNode, inputType);
		    // make a JMultExpression with weight*peekExpression
		    JMultExpression multNode = new JMultExpression(null,        // tokenReference
								   weightNode,  // left
								   peekNode);   // right
		    combinationExpressions.add(multNode);
		}
	    }

	    // now, we need to create the appropriate constant to represent the offset
	    ComplexNumber currentOffset = representation.getb().getElement(currentPushIndex);
	    if (!currentOffset.isReal()) {throw new RuntimeException("Non real complex number in offset vector");}
	    JDoubleLiteral offsetNode = new JDoubleLiteral(null, currentOffset.getReal());
	    
	    for (int q=0; q<combinationExpressions.size(); q++) {
		System.out.println("comb expr: " +
				   combinationExpressions.get(q));
	    }
	    
	    // now we have all of the combination nodes and the offset node.
	    // What we want to do is to is to combine them all together using addition.
	    // To do this, we create an add expression tree expanding downward to the right as we go.
	    JExpression pushArgument;
	    // if no combination expressions, then the push arg is only the offset
	    if (combinationExpressions.size() == 0) {
		// if we have no combination expressions, it means we should simply output a zero
		pushArgument = offsetNode;
	    } else {
		// combination expressions need to be nested.
		// Start with the right most node
		int numCombos = combinationExpressions.size();
		pushArgument = new JAddExpression(null, // tokenReference
						  ((JExpression)combinationExpressions.get(numCombos-1)), // left
						  offsetNode); // right
		// now, for all of the other combinations, make new add nodes with the
		// comb. exprs as the left argument and the current add expr as the right
		// argument.
		for (int k=2; k<=numCombos; k++) {
		    pushArgument = new JAddExpression(null, // tokenReference,
						      ((JExpression)combinationExpressions.get(numCombos-k)), // left
						      pushArgument); // right (use the previous expression)
		}
	    }

	    System.out.println("Add expression: " + pushArgument);
	    
	    // now, armed with the appropriate push argument, we can
	    // simply generate the appropriate push expression and stick it in our list.
	    SIRPushExpression pushExpr = new SIRPushExpression(pushArgument, // arg
							       outputType); // output tape type (eg push type)
	    // wrap the push expression in a expression statement
	    JExpressionStatement pushWrapper = new JExpressionStatement(null, // tokenReference
									pushExpr, // expr
									new JavaStyleComment[0]); // comments
	    returnVector.add(pushWrapper);
	}
	return returnVector;
    }
    
    
}

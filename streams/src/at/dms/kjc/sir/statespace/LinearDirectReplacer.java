package at.dms.kjc.sir.statespace;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.statespace.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A LinearDirectReplacer replaces the contents of the work functions for
 * linear filters (as determined by the linear filter analyzer) with an appropriate
 * direct implementation (eg a bunch of push statements with the specified
 * combination of input values. <p>
 * Eg a filter that had linear form [1; 2; 3]+4 would get a work function:
 * <pre>
 * work {
 *   push(3*peek(0) + 2*peek(1) + 1*peek(2) + 4);
 * }
 * </pre>
 *
 * It also can replace splitjoins and pipelines with linear representations
 * with a single filter that computes the same function.<br>
 * 
 * $Id: LinearDirectReplacer.java,v 1.15 2004-04-29 21:49:56 sitij Exp $
 **/
public class LinearDirectReplacer extends LinearReplacer implements Constants{
    /** the linear analyzier which keeps mappings from filters-->linear representations**/
    LinearAnalyzer linearityInformation;
    /**
     * the cost calculator which guides us in whether or
     * not we should stream constructs with direct implementations.
     **/
    LinearReplaceCalculator replaceGuide;
    
    protected LinearDirectReplacer(LinearAnalyzer lfa, LinearReplaceCalculator costs) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer!");
	}
	if (costs == null) {
	    throw new IllegalArgumentException("Null linear replace calculator!");
	}
	this.linearityInformation = lfa;
	this.replaceGuide = costs;
    }

    /** Start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str) {
	// calculate the best way to replace linear components.
	LinearReplaceCalculator replaceCosts = new LinearReplaceCalculator(lfa);
	str.accept(replaceCosts);
	LinearPrinter.println("starting replacement pass. Will replace " +
			      replaceCosts.getDoReplace().keySet().size() + " filters:");
	Iterator keyIter = replaceCosts.getDoReplace().keySet().iterator();
	while(keyIter.hasNext()) {
	    Object key = keyIter.next();
	    LinearPrinter.println(" " + key);
	}
	// make a new replacer with the information contained in the analyzer and the costs
	LinearDirectReplacer replacer = new LinearDirectReplacer(lfa, replaceCosts);
	// pump the replacer through the stream graph.
	IterFactory.createFactory().createIter(str).accept(replacer);
    }

    /**
     * Visit a pipeline, splitjoin or filter, replacing them with a new filter
     * that directly implements the linear representation. This only
     * occurs if the replace calculator says that this stream should be replaced.
     **/
    public boolean makeReplacement(SIRStream self) {
	LinearPrinter.println("Creating linear replacement for " + self);
	SIRContainer parent = self.getParent();
	if (parent == null) {
	    // we are done, this is the top level stream
	    LinearPrinter.println(" aborting, top level stream: " + self);
	    LinearPrinter.println(" stop.");
	    return false;
	}
	LinearPrinter.println(" parent: " + parent);
	if (!this.linearityInformation.hasLinearRepresentation(self)) {
	    LinearPrinter.println(" no linear information about: " + self);
	    LinearPrinter.println(" stop.");
	    return false;
	}
	
	// generate a new implementation as a single filter
	LinearFilterRepresentation linearRep;
	linearRep = this.linearityInformation.getLinearRepresentation(self);

	/********** test print for optimization *********/
	LinearPrinter.println("Before optimization: " + linearRep);
	LinearCost oldCost = linearRep.getCost();

	LinearOptimizer opt = new LinearOptimizer(linearRep);
	LinearFilterRepresentation newRep = opt.optimize();
	LinearCost newCost = newRep.getCost();

	LinearPrinter.println("After optimization: " + newRep);
	linearRep = newRep;
	
	LinearPrinter.println("Before Optimization (multiplies, adds) " 
			      + oldCost.getMultiplies() + " " + oldCost.getAdds());
	
	LinearPrinter.println("After Optimization (multiplies, adds) " 
			      + newCost.getMultiplies() + " " + newCost.getAdds());
	
	/***********************************************/



	SIRStream newImplementation;
	newImplementation = makeEfficientImplementation(self, linearRep);
	newImplementation.setParent(parent);
	// do the acutal replacment of the current pipeline with the new implementation
	parent.replace(self, newImplementation);

	LinearPrinter.println("Relative child name: " + newImplementation.getRelativeName());

	// remove linearity mapping for old filter, since you can only
	// have one stream mapping to a given linearity object
	this.linearityInformation.removeLinearRepresentation(self);
	// add a mapping from the new filter to the old linear rep
	// (because it still computes the same thing)
	this.linearityInformation.addLinearRepresentation(newImplementation, linearRep);

	// return that we replaced something
	return true;
    }

    /**
     * Creates a filter that has a work function that directly implements
     * the linear representation that is passed in.<br>
     **/
    protected SIRFilter makeEfficientImplementation(SIRStream oldStream,
						    LinearFilterRepresentation linearRep) {
	// if we have a linear representation of this filter
	if (!linearityInformation.hasLinearRepresentation(oldStream)) {
	    throw new RuntimeException("no linear info");
	}
	
	CType varType;

       
	// use doubles for best precision

	varType = new CDoubleType();

	int numStates = linearRep.getStateCount();
	FilterVector initVector = linearRep.getInit();
	int popCount = linearRep.getPopCount();

	String varName;
	JVariableDefinition vars[] = new JVariableDefinition[numStates];
	JFieldDeclaration fields[] = new JFieldDeclaration[2*numStates];
	JAssignmentExpression assign;
	JFieldAccessExpression fieldExpr;
	JLiteral litExpr;
	Vector assignStatements = new Vector();

	JThisExpression thisExpr = new JThisExpression(null);

	// create field declarations and field accessors
	for(int i=0; i<numStates; i++) {
	    varName = "x" + i;
	    vars[i] = new JVariableDefinition(null, ACC_PUBLIC, varType, varName, null);
	    fields[i] = new JFieldDeclaration(null, vars[i], null, null);
	    fieldExpr = new JFieldAccessExpression(null, thisExpr, varName);
	    litExpr = new JDoubleLiteral(null, initVector.getElement(i).getReal());
	    assign = new JAssignmentExpression(null, fieldExpr, litExpr);

	    // wrap the assign expression in a expression statement
	    JExpressionStatement assignWrapper = new JExpressionStatement(null, // tokenReference
									assign, // expr
									new JavaStyleComment[0]);
	    assignStatements.add(assignWrapper);
	}

	// declare temporary variables (one for each state)
	// this is necessary to do state updates correctly
	for(int i=0; i<numStates;i++) {
	    JVariableDefinition tempVar = new JVariableDefinition(null, ACC_PUBLIC, varType, "temp_x" + i, null);
	    fields[numStates+i] = new JFieldDeclaration(null, tempVar, null, null);
	}

	
	JBlock initBody = new JBlock();
       	initBody.addAllStatements(assignStatements);

	JMethodDeclaration newInit = SIRStream.makeEmptyInit();
	newInit.setBody(initBody);

	//create a prework function that initializes vars with values from the tape
	JMethodDeclaration newPreWork;
	if(linearRep.preworkNeeded()) {
	    newPreWork = makeInitLinearWork(linearRep,
					    oldStream.getInputType(),
					    oldStream.getOutputType(),
					    linearRep.getPreWorkPopCount());
	}
	else
	    newPreWork = SIRStream.makeEmptyInitWork();

	// create a new work function that calculates the linear representation directly
	JMethodDeclaration newWork = makeLinearWork(linearRep,
						    oldStream.getInputType(),
						    oldStream.getOutputType(),
						    linearRep.getPopCount());
	
	// create a new filter with the new prework, work and init functions
       	SIRTwoStageFilter newFilter = new SIRTwoStageFilter("Linear" + oldStream.getIdent());

	newFilter.addFields(fields);
	newFilter.setWork(newWork);
	newFilter.setInitWork(newPreWork);
	newFilter.setInit(newInit);
       
	/* these are rates for the prework function */
	newFilter.setInitPeek(linearRep.getPreWorkPopCount());
	newFilter.setInitPop(linearRep.getPreWorkPopCount());
	newFilter.setInitPush(0);

	/** make peek rate equal to pop rate **/
	newFilter.setPeek(linearRep.getPopCount());
	newFilter.setPop (linearRep.getPopCount());
	newFilter.setPush(linearRep.getPushCount());
	newFilter.setInputType(oldStream.getInputType());
	newFilter.setOutputType(oldStream.getOutputType());

	LinearPrinter.println(" created new filter: " + newFilter);
	return newFilter;
    }


    /**
     * Create a method that computes the function represented in the
     * linear form. (Eg it pushes the direct sum of inputs to the output.)
     * inputType is the variable type of the peek/pop expression that this filter uses
     * and output type is the type of the pushExpressions.<br>
     *
     * The basic format of the resulting method is:<br>
     * <pre>
     * push(a1*peek(0) + b1*peek(1) + ... + x1*peek(n));
     * push(a2*peek(0) + b2*peek(1) + ... + x2*peek(n));
     * ...
     * pop();
     * pop();
     * ...
     * </pre>
     **/
    JMethodDeclaration makeLinearWork(LinearFilterRepresentation representation,
				      CType inputType,
				      CType outputType,
				      int popCount) {
	// generate the push expressions that will make up the body of the
	// new work method.
	Vector pushStatements = makePushStatementVector(representation, inputType, outputType);
	// make a vector filled with the appropriate number of pop expressions
	Vector popStatements  = new Vector();
	SIRPopExpression popExpr = new SIRPopExpression(inputType, popCount);
	// wrap the pop expression so it is a statement.
	JExpressionStatement popWrapper = new JExpressionStatement(null, // token reference,
								   popExpr, // expr
								   new JavaStyleComment[0]);  // comments
	popStatements.add(popWrapper);

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
     * Generate a Vector of JExpressionStatements which wrap
     * SIRPushExpressions that implement (directly) the
     * matrix multiplication represented by the linear representation.
     **/
    public Vector makePushStatementVector(LinearFilterRepresentation representation,
					  CType inputType,
					  CType outputType) {
	Vector returnVector = new Vector();

	int popCount = representation.getPopCount();
	int pushCount = representation.getPushCount();
	int stateCount = representation.getStateCount();

	JThisExpression thisExpr = new JThisExpression(null);
	
	// for each output value (eg push count), construct push expression
	for (int i = 0; i < pushCount; i++) {
	    // the first push will have index 0
	    
	    // go through each of the elements in this row of the matrix. If the element
	    // is non zero, then we want to produce a peek(index)*weight
	    // term (which we will then add together).
	    // Currently bomb out if we have a non real number
	    // (no way to generate non-reals at the present).
	    Vector combinationExpressions = new Vector();

	    for (int j = 0; j < popCount; j++) {
		ComplexNumber currentWeight = representation.getD().getElement(i,j);
		// if we have a non real number, bomb Mr. Exception
		if (!currentWeight.isReal()) {
		    throw new RuntimeException("Direct implementation with complex " +
					       "numbers is not yet implemented.");
		}

		// if we have a non zero weight, add a weight*peek node
		if (currentWeight.equals(ComplexNumber.ZERO)) {
		    // do nothing for a zero weight
		} else {
		    // make an integer IR node for the appropriate peek index (peek (0)
		    // corresponds to the array row of  at peekSize-1
		    JIntLiteral peekOffsetNode = new JIntLiteral(j);
		    // make a peek expression with the appropriate index
		    SIRPeekExpression peekNode = new SIRPeekExpression(peekOffsetNode, inputType);

		    // IR node for the expression (either peek, or weight*peek)
		    JExpression exprNode;
		    // If we have a one, no need to do a multiply
		    if (currentWeight.equals(ComplexNumber.ONE)) {
			exprNode = peekNode;
		    } else {
			// make literal weight (special case if the weight is an integer)
			JLiteral weightNode;
			if (currentWeight.isReal() && currentWeight.isIntegral()) {
			    weightNode = new JIntLiteral(null, (int)currentWeight.getReal());
			} else {
			    weightNode = new JFloatLiteral(null, (float)currentWeight.getReal());
			}
			// make a JMultExpression with weight*peekExpression
			exprNode = new JMultExpression(null,        // tokenReference
						       weightNode,  // left
						       peekNode);   // right
		    }
		    // add in the new expression node
		    combinationExpressions.add(exprNode);
		}
	    }
	    

	    // go through each of the elements in this row of the matrix. If the element
	    // is non zero, then we want to produce a state(index)*weight
	    // term (which we will then add together).
	    // Currently bomb out if we have a non real number
	    // (no way to generate non-reals at the present).
	    
	    for(int k = 0; k < stateCount; k++) {
		ComplexNumber currentWeight = representation.getC().getElement(i,k);
		// if we have a non real number, bomb Mr. Exception
		if (!currentWeight.isReal()) {
		    throw new RuntimeException("Direct implementation with complex " +
					       "numbers is not yet implemented.");
		}


		    // if we have a non zero weight, add a weight*peek node
		    if (currentWeight.equals(ComplexNumber.ZERO)) {
			// do nothing for a zero weight
		    } else {

			String varName = "x" + k;
			JFieldAccessExpression fieldNode = new JFieldAccessExpression(null, thisExpr, varName);
			// IR node for the expression (either var, or weight*var)
			JExpression exprNode;
			// If we have a one, no need to do a multiply
			if (currentWeight.equals(ComplexNumber.ONE)) {
			    exprNode = fieldNode;
			} else {
			    // make literal weight (special case if the weight is an integer)
			    JLiteral weightNode;
			    if (currentWeight.isReal() && currentWeight.isIntegral()) {
				weightNode = new JIntLiteral(null, (int)currentWeight.getReal());
			    } else {
				weightNode = new JFloatLiteral(null, (float)currentWeight.getReal());
			    }
			    // make a JMultExpression with weight*peekExpression
			    exprNode = new JMultExpression(null,        // tokenReference
						       weightNode,  // left
							   fieldNode);   // right
			}
			// add in the new expression node
			combinationExpressions.add(exprNode);
		    }

	    }


	    // now we have all of the combination nodes and the offset node.
	    // What we want to do is to is to combine them all together using addition.
	    // To do this, we create an add expression tree expanding downward
	    // to the right as we go.
	    JLiteral offsetNode = new JDoubleLiteral(null,0.0);
	    JExpression pushArgument;
	    // if no combination expressions, then the push arg is zero
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


	/* for each state, update value based on A and B matrices
	** this update is performed on TEMPORARY variables
	** so that every variable is updated with a variable value from the previous iteration
	*/ 
	
	for(int i=0; i<stateCount; i++) {
	    
	    // go through each of the elements in this row of the matrix. If the element
	    // is non zero, then we want to produce a peek(index)*weight
	    // term (which we will then add together).
	    // Currently bomb out if we have a non real number
	    // (no way to generate non-reals at the present).
	    Vector combinationExpressions = new Vector();

	    for (int j = 0; j < popCount; j++) {
		ComplexNumber currentWeight = representation.getB().getElement(i,j);
		// if we have a non real number, bomb Mr. Exception
		if (!currentWeight.isReal()) {
		    throw new RuntimeException("Direct implementation with complex " +
					       "numbers is not yet implemented.");
		}

		// if we have a non zero weight, add a weight*peek node
		if (currentWeight.equals(ComplexNumber.ZERO)) {
		    // do nothing for a zero weight
		} else {
		    // make an integer IR node for the appropriate peek index (peek (0)
		    // corresponds to the array row of  at peekSize-1
		    JIntLiteral peekOffsetNode = new JIntLiteral(j);
		    // make a peek expression with the appropriate index
		    SIRPeekExpression peekNode = new SIRPeekExpression(peekOffsetNode, inputType);

		    // IR node for the expression (either peek, or weight*peek)
		    JExpression exprNode;
		    // If we have a one, no need to do a multiply
		    if (currentWeight.equals(ComplexNumber.ONE)) {
			exprNode = peekNode;
		    } else {
			// make literal weight (special case if the weight is an integer)
			JLiteral weightNode;
			if (currentWeight.isReal() && currentWeight.isIntegral()) {
			    weightNode = new JIntLiteral(null, (int)currentWeight.getReal());
			} else {
			    weightNode = new JFloatLiteral(null, (float)currentWeight.getReal());
			}
			// make a JMultExpression with weight*peekExpression
			exprNode = new JMultExpression(null,        // tokenReference
						       weightNode,  // left
						       peekNode);   // right
		    }
		    // add in the new expression node
		    combinationExpressions.add(exprNode);
		}
	    }
	    

	    // go through each of the elements in this row of the matrix. If the element
	    // is non zero, then we want to produce a state(index)*weight
	    // term (which we will then add together).
	    // Currently bomb out if we have a non real number
	    // (no way to generate non-reals at the present).
	    
	    for(int k = 0; k < stateCount; k++) {
		ComplexNumber currentWeight = representation.getA().getElement(i,k);
		// if we have a non real number, bomb Mr. Exception
		if (!currentWeight.isReal()) {
		    throw new RuntimeException("Direct implementation with complex " +
					       "numbers is not yet implemented.");
		}


		    // if we have a non zero weight, add a weight*peek node
		    if (currentWeight.equals(ComplexNumber.ZERO)) {
			// do nothing for a zero weight
		    } else {

			String varName = "x" + k;
			JFieldAccessExpression fieldNode = new JFieldAccessExpression(null, thisExpr, varName);
			// IR node for the expression (either var, or weight*var)
			JExpression exprNode;
			// If we have a one, no need to do a multiply
			if (currentWeight.equals(ComplexNumber.ONE)) {
			    exprNode = fieldNode;
			} else {
			    // make literal weight (special case if the weight is an integer)
			    JLiteral weightNode;
			    if (currentWeight.isReal() && currentWeight.isIntegral()) {
				weightNode = new JIntLiteral(null, (int)currentWeight.getReal());
			    } else {
				weightNode = new JFloatLiteral(null, (float)currentWeight.getReal());
			    }
			    // make a JMultExpression with weight*peekExpression
			    exprNode = new JMultExpression(null,        // tokenReference
						       weightNode,  // left
							   fieldNode);   // right
			}
			// add in the new expression node
			combinationExpressions.add(exprNode);
		    }
	    }

	    // now we have all of the combination nodes and the offset node.
	    // What we want to do is to is to combine them all together using addition.
	    // To do this, we create an add expression tree expanding downward
	    // to the right as we go.
	    JLiteral offsetNode = new JDoubleLiteral(null,0.0);
	    JExpression assignArgument;
	    // if no combination expressions, then the push arg is zero
	    if (combinationExpressions.size() == 0) {
		// if we have no combination expressions, it means we should simply output a zero
		assignArgument = offsetNode;
	    } else {
		// combination expressions need to be nested.
		// Start with the right most node
		int numCombos = combinationExpressions.size();
		assignArgument = new JAddExpression(null, // tokenReference
						  ((JExpression)combinationExpressions.get(numCombos-1)), // left
						  offsetNode); // right
		// now, for all of the other combinations, make new add nodes with the
		// comb. exprs as the left argument and the current add expr as the right
		// argument.
		for (int k=2; k<=numCombos; k++) {
		    assignArgument = new JAddExpression(null, // tokenReference,
						      ((JExpression)combinationExpressions.get(numCombos-k)), // left
						      assignArgument); // right (use the previous expression)
		}
	    }

	    String tempVarName = "temp_x" + i;
	    JFieldAccessExpression tempFieldExpr = new JFieldAccessExpression(null,thisExpr,tempVarName);
	    JAssignmentExpression temp_assign = new JAssignmentExpression(null,tempFieldExpr,assignArgument);	    	    	    

	    // wrap the push expression in a expression statement
	    JExpressionStatement assignWrapper = new JExpressionStatement(null, // tokenReference
									temp_assign, // expr
									new JavaStyleComment[0]); // comments
	    returnVector.add(assignWrapper);


	}

	// now make the temporary assignments permanent

	for(int i=0; i<stateCount;i++) {

	    String tempName = "temp_x"+i;
	    String finalName = "x"+i;
	    JFieldAccessExpression tempFieldExpr = new JFieldAccessExpression(null,thisExpr,tempName);
	    JFieldAccessExpression finalFieldExpr = new JFieldAccessExpression(null,thisExpr,finalName);

	    JAssignmentExpression final_assign = new JAssignmentExpression(null,finalFieldExpr,tempFieldExpr);	    	    	    


	    // wrap the push expression in a expression statement
	    JExpressionStatement assignWrapper = new JExpressionStatement(null, // tokenReference
									final_assign, // expr
									new JavaStyleComment[0]); // comments
	    returnVector.add(assignWrapper);


	}

	return returnVector;
    }



    /**
     * Create a method that computes the function represented in the
     * linear form FOR THE INIT. (Eg it pushes the direct sum of inputs to the output.)
     * inputType is the variable type of the peek/pop expression that this filter uses
     * and output type is the type of the pushExpressions.<br>
     *
     * The basic format of the resulting method is:<br>
     * <pre>
     * push(a1*peek(0) + b1*peek(1) + ... + x1*peek(n));
     * push(a2*peek(0) + b2*peek(1) + ... + x2*peek(n));
     * ...
     * pop();
     * pop();
     * ...
     * </pre>
     **/
    JMethodDeclaration makeInitLinearWork(LinearFilterRepresentation representation,
				      CType inputType,
				      CType outputType,
				      int popCount) {
	// generate the state update expressions that will make up the body of the
	// new work method.
	Vector pushStatements = makeInitPushStatementVector(representation, inputType, outputType);
	// make a vector filled with the appropriate number of pop expressions
	Vector popStatements  = new Vector();
	SIRPopExpression popExpr = new SIRPopExpression(inputType, popCount);
	// wrap the pop expression so it is a statement.
	JExpressionStatement popWrapper = new JExpressionStatement(null, // token reference,
								   popExpr, // expr
								   new JavaStyleComment[0]);  // comments
	popStatements.add(popWrapper);

	// now, generate the body of the new method, concatenating state update then pop expressions
	JBlock preWorkBody = new JBlock();
	preWorkBody.addAllStatements(pushStatements);
	preWorkBody.addAllStatements(popStatements);

	JMethodDeclaration newPreWork = SIRStream.makeEmptyInitWork();
	newPreWork.setBody(preWorkBody);

	return newPreWork;
			       
    }

    /**
     * Generate a Vector of JExpressionStatements which wrap
     * SIRPushExpressions that implement (directly) the
     * matrix multiplication represented by the linear representation.
     **/
    public Vector makeInitPushStatementVector(LinearFilterRepresentation representation,
					  CType inputType,
					  CType outputType) {
	Vector returnVector = new Vector();

	int popCount = representation.getPreWorkPopCount();
	int stateCount = representation.getStateCount();

	JThisExpression thisExpr = new JThisExpression(null);
	
	/* for each state, update value based on A and B matrices
	** this update is performed on TEMPORARY variables
	** so that every variable is updated with a variable value from the previous iteration
	*/ 
	
	for(int i=0; i<stateCount; i++) {
	    
	    // go through each of the elements in this row of the matrix. If the element
	    // is non zero, then we want to produce a peek(index)*weight
	    // term (which we will then add together).
	    // Currently bomb out if we have a non real number
	    // (no way to generate non-reals at the present).
	    Vector combinationExpressions = new Vector();

	    for (int j = 0; j < popCount; j++) {
		ComplexNumber currentWeight = representation.getPreWorkB().getElement(i,j);
		// if we have a non real number, bomb Mr. Exception
		if (!currentWeight.isReal()) {
		    throw new RuntimeException("Direct implementation with complex " +
					       "numbers is not yet implemented.");
		}

		// if we have a non zero weight, add a weight*peek node
		if (currentWeight.equals(ComplexNumber.ZERO)) {
		    // do nothing for a zero weight
		} else {
		    // make an integer IR node for the appropriate peek index (peek (0)
		    // corresponds to the array row of  at peekSize-1
		    JIntLiteral peekOffsetNode = new JIntLiteral(j);
		    // make a peek expression with the appropriate index
		    SIRPeekExpression peekNode = new SIRPeekExpression(peekOffsetNode, inputType);

		    // IR node for the expression (either peek, or weight*peek)
		    JExpression exprNode;
		    // If we have a one, no need to do a multiply
		    if (currentWeight.equals(ComplexNumber.ONE)) {
			exprNode = peekNode;
		    } else {
			// make literal weight (special case if the weight is an integer)
			JLiteral weightNode;
			if (currentWeight.isReal() && currentWeight.isIntegral()) {
			    weightNode = new JIntLiteral(null, (int)currentWeight.getReal());
			} else {
			    weightNode = new JFloatLiteral(null, (float)currentWeight.getReal());
			}
			// make a JMultExpression with weight*peekExpression
			exprNode = new JMultExpression(null,        // tokenReference
						       weightNode,  // left
						       peekNode);   // right
		    }
		    // add in the new expression node
		    combinationExpressions.add(exprNode);
		}
	    }
	    

	    // go through each of the elements in this row of the matrix. If the element
	    // is non zero, then we want to produce a state(index)*weight
	    // term (which we will then add together).
	    // Currently bomb out if we have a non real number
	    // (no way to generate non-reals at the present).
	    
	    for(int k = 0; k < stateCount; k++) {
		ComplexNumber currentWeight = representation.getPreWorkA().getElement(i,k);
		// if we have a non real number, bomb Mr. Exception
		if (!currentWeight.isReal()) {
		    throw new RuntimeException("Direct implementation with complex " +
					       "numbers is not yet implemented.");
		}


		    // if we have a non zero weight, add a weight*peek node
		    if (currentWeight.equals(ComplexNumber.ZERO)) {
			// do nothing for a zero weight
		    } else {

			String varName = "x" + k;
			JFieldAccessExpression fieldNode = new JFieldAccessExpression(null, thisExpr, varName);
			// IR node for the expression (either var, or weight*var)
			JExpression exprNode;
			// If we have a one, no need to do a multiply
			if (currentWeight.equals(ComplexNumber.ONE)) {
			    exprNode = fieldNode;
			} else {
			    // make literal weight (special case if the weight is an integer)
			    JLiteral weightNode;
			    if (currentWeight.isReal() && currentWeight.isIntegral()) {
				weightNode = new JIntLiteral(null, (int)currentWeight.getReal());
			    } else {
				weightNode = new JFloatLiteral(null, (float)currentWeight.getReal());
			    }
			    // make a JMultExpression with weight*peekExpression
			    exprNode = new JMultExpression(null,        // tokenReference
						       weightNode,  // left
							   fieldNode);   // right
			}
			// add in the new expression node
			combinationExpressions.add(exprNode);
		    }
	    }

	    // now we have all of the combination nodes and the offset node.
	    // What we want to do is to is to combine them all together using addition.
	    // To do this, we create an add expression tree expanding downward
	    // to the right as we go.
	    JLiteral offsetNode = new JDoubleLiteral(null,0.0);
	    JExpression assignArgument;
	    // if no combination expressions, then the push arg is zero
	    if (combinationExpressions.size() == 0) {
		// if we have no combination expressions, it means we should simply output a zero
		assignArgument = offsetNode;
	    } else {
		// combination expressions need to be nested.
		// Start with the right most node
		int numCombos = combinationExpressions.size();
		assignArgument = new JAddExpression(null, // tokenReference
						  ((JExpression)combinationExpressions.get(numCombos-1)), // left
						  offsetNode); // right
		// now, for all of the other combinations, make new add nodes with the
		// comb. exprs as the left argument and the current add expr as the right
		// argument.
		for (int k=2; k<=numCombos; k++) {
		    assignArgument = new JAddExpression(null, // tokenReference,
						      ((JExpression)combinationExpressions.get(numCombos-k)), // left
						      assignArgument); // right (use the previous expression)
		}
	    }

	    String tempVarName = "temp_x" + i;
	    JFieldAccessExpression tempFieldExpr = new JFieldAccessExpression(null,thisExpr,tempVarName);
	    JAssignmentExpression temp_assign = new JAssignmentExpression(null,tempFieldExpr,assignArgument);	    	    	    

	    // wrap the push expression in a expression statement
	    JExpressionStatement assignWrapper = new JExpressionStatement(null, // tokenReference
									temp_assign, // expr
									new JavaStyleComment[0]); // comments
	    returnVector.add(assignWrapper);
	}


	// now make the temporary assignments permanent

	for(int i=0; i<stateCount;i++) {

	    String tempName = "temp_x"+i;
	    String finalName = "x"+i;
	    JFieldAccessExpression tempFieldExpr = new JFieldAccessExpression(null,thisExpr,tempName);
	    JFieldAccessExpression finalFieldExpr = new JFieldAccessExpression(null,thisExpr,finalName);

	    JAssignmentExpression final_assign = new JAssignmentExpression(null,finalFieldExpr,tempFieldExpr);	    	    	    


	    // wrap the push expression in a expression statement
	    JExpressionStatement assignWrapper = new JExpressionStatement(null, // tokenReference
									final_assign, // expr
									new JavaStyleComment[0]); // comments
	    returnVector.add(assignWrapper);

	}

	return returnVector;
    }
 





    /**
     * This visitor calculates the best way to replace filters in a stream
     * graph with direct implementations. Specifically, it calculates the
     * the replacement that has the lowest cost.<br>
     *
     * The technique in this class isn't quite correct (e.g., it doesn't
     * take into account the number of times that children execute in the
     * steady-state schedule when considering their cost) -- the linear
     * partitioner is now the preferred way to get the lowest cost
     * combination.  --bft
     **/
    static class LinearReplaceCalculator extends EmptyAttributeStreamVisitor {
	/**
	 * Maps SIRStreams-->Boolean. If the value is true, we want to replace this member, and
	 * if the value is false, we do not want to do the replacement.
	 **/
	HashMap doReplace;
	LinearAnalyzer linearInformation;
	public LinearReplaceCalculator(LinearAnalyzer la) {
	    doReplace = new HashMap();
	    linearInformation = la;
	}
	/**
	 * visiting a filter is easy. There are no children, and by assumption we want to
	 * replace the generic code given with our matrix code. Stick in the appropriate
	 * mapping in doReplace and then return.
	 **/
	public Object visitFilter(SIRFilter self,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  JMethodDeclaration work,
				  CType inputType, CType outputType) {
	    if (linearInformation.hasLinearRepresentation(self)) {
		doReplace.put(self, new Boolean(true));
	    } 
	    return self;
	}

	public Object visitFeedbackLoop(SIRFeedbackLoop self,
					JFieldDeclaration[] fields,
					JMethodDeclaration[] methods,
					JMethodDeclaration init,
					JMethodDeclaration initPath) {
	    // we don't really care about feedback loops because we don't include them in our analysis
	    return self;
	}
	/* pre-visit a pipeline */
	public Object visitPipeline(SIRPipeline self,
				    JFieldDeclaration[] fields,
				    JMethodDeclaration[] methods,
				    JMethodDeclaration init) {
	    return visitContainer(self);
	}
	public Object visitSplitJoin(SIRSplitJoin self,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     SIRSplitter splitter,
				     SIRJoiner joiner) {
	    return visitContainer(self);
	}
	
	/**
	 * generic method for visiting container streams:<p>
	 * If we have linear information for the container, we calculate the cost
	 * of using the linear representation of the container, and recursively
	 * calculate the cost of using the linear information of the children.
	 * We then use the solution which generates minimal cost.
	 **/
	public Object visitContainer(SIRContainer self) {
	    LinearPrinter.println(" calculating cost of: " + self);
	    // if we don't know anything about this container, we are done, though we need
	    // to do the recursion to the children
	    if (!linearInformation.hasLinearRepresentation(self)) {
		Iterator childIter = self.getChildren().iterator();
		while (childIter.hasNext()) {
		    ((SIROperator)childIter.next()).accept(this);
		}
		return self;
	    }

	    // calcuate the cost of doing a direct replacement of this container
	    LinearCost containerCost = linearInformation.getLinearRepresentation(self).getCost();

	    // calculate the cost of doing the optimal replacement of the children.
	    LinearReplaceCalculator childCalculator = new LinearReplaceCalculator(linearInformation);
	    Iterator childIter = self.getChildren().iterator();
	    while(childIter.hasNext()) {
		((SIROperator)childIter.next()).accept(childCalculator);
	    }
	    LinearCost childCost = childCalculator.getTotalCost();

	    // now, if the container cost is less than the child cost, use the
	    // container, otherwise use the child
	    if (containerCost.lessThan(childCost)) {
		doReplace.put(self, new Boolean(true));
	    } else {
		doReplace.putAll(childCalculator.getDoReplace()); // remember which children were used
	    }
	    return self;
	}

	/** get the mappings from streams to true if we want to replace them. **/
	HashMap getDoReplace() {return this.doReplace;}

	/** calculate the total cost of doing the replacements that is described in doReplace. **/
	LinearCost getTotalCost() {
	    LinearCost currentCost = LinearCost.ZERO;
	    
	    Iterator keyIter = this.doReplace.keySet().iterator();
	    while(keyIter.hasNext()) {
		// the only mappings that we have in the map are the streams we want to include
		SIRStream currentStream = (SIRStream)keyIter.next();
		LinearFilterRepresentation currentChildRep = linearInformation.getLinearRepresentation(currentStream);
		LinearCost currentChildCost = currentChildRep.getCost();
		currentCost = currentCost.plus(currentChildCost);
	    }
	    return currentCost;
	}

	/**
	 * returns true if we should replace this filter with a direct implementation (eg if
	 * we have a mapping from the stream to Boolean(true) in doReplace.
	 **/
	public boolean shouldReplace(SIRStream str) {
	    return this.doReplace.containsKey(str);
	}
    }
}





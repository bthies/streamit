package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;
import at.dms.kjc.flatgraph2.FilterContent;

public class BufferedCommunication extends RawExecutionCode
    implements Constants
{
   
    
      //in this class we want to treat all the filters as 
    //two stage, so if it is not two stage by type,
    //add one to the init multiplicity of the filter 
    private int initFire;

    public BufferedCommunication(FilterInfo filterInfo) 
    {
	super(filterInfo);
	FilterTraceNode node=filterInfo.traceNode;
	System.out.println("["+node.getX()+","+node.getY()+"] Generating code for " + filterInfo.filter + " using Buffered Comm.");
	//set the unique id to append to each variable name
	//treat all the filters as two stages, i.e.
	//initWork is always called, so add one to non-2 stages
	//init multiplicity
	initFire = filterInfo.initMult;
	if (!filterInfo.isTwoStage()) {
	    initFire++;
	}
	
    }


    //convert the peek and pop expressions for a filter into
    //buffer accesses, do this for all functions just in case helper
    //functions call peek or pop
    private void convertCommExprs() 
    {
	SLIRReplacingVisitor convert;
	
	if (filterInfo.isSimple()) {
	    convert = 
		new ConvertCommunicationSimple(generatedVariables, filterInfo);
	}
	else
	    convert = new ConvertCommunication(generatedVariables, filterInfo);
	
	JMethodDeclaration[] methods = filterInfo.filter.getMethods();

	for (int i = 0; i < methods.length; i++) {
	    //iterate over the statements and call the ConvertCommunication
	    //class to convert peek, pop
	    for (ListIterator it = methods[i].getStatementIterator();
		 it.hasNext(); ){
		((JStatement)it.next()).accept(convert);
	    }
	}
    }

    public JFieldDeclaration[] getVarDecls() 
    {
	Vector decls = new Vector();
	FilterContent filter = filterInfo.filter;
	
	for (int i = 0; i < filter.getFields().length; i++) 
	    decls.add(filter.getFields()[i]);
	
	//index variable for certain for loops
	JVariableDefinition exeIndexVar = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex + uniqueID,
				    null);

	//remember the JVarDef for latter (in the raw main function)
	generatedVariables.exeIndex = exeIndexVar;
	decls.add(new JFieldDeclaration(null, exeIndexVar, null, null));
	
	//index variable for certain for loops
	JVariableDefinition exeIndex1Var = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex1 + uniqueID,
				    null);

	generatedVariables.exeIndex1 = exeIndex1Var;
	decls.add(new JFieldDeclaration(null, exeIndex1Var, null, null));
	
	//only add the receive buffer and its vars if the 
	//filter receives data
	if (!filterInfo.noBuffer()) {
	    //initialize the buffersize to be the size of the 
	    //struct being passed over it
	    int buffersize;
	    
	    int maxpeek = filterInfo.peek;
	    //set up the maxpeek
	    maxpeek = (filterInfo.prePeek > maxpeek) ? filterInfo.prePeek : maxpeek;
	    
	    
	    if (filterInfo.isSimple()) {
		//simple filter (no remaining items)
		if (KjcOptions.ratematch) {
		    //i don't know, the prepeek could be really large, so just in case
		    //include it.  Make the buffer big enough to hold 
		    buffersize = 
			Math.max
			((filterInfo.steadyMult - 1) * 
			 filterInfo.pop + filterInfo.peek, filterInfo.prePeek);
		}
		else //not ratematching and we do not have a circular buffer
		    buffersize = maxpeek;  //this really should be max pop, but peek  == pop
		                           //in this case 

		//define the simple index variable
		JVariableDefinition simpleIndexVar = 
		    new JVariableDefinition(null, 
					    0, 
					    CStdType.Integer,
					    simpleIndex + filterInfo.filter.getName(),
					    new JIntLiteral(-1));
		
		//remember the JVarDef for latter (in the raw main function)
		generatedVariables.simpleIndex = simpleIndexVar;
		decls.add(new JFieldDeclaration(null, simpleIndexVar, null, null));
	    }
	    else { //filter with remaing items on the buffer after initialization 
		buffersize = Util.nextPow2(maxpeek + filterInfo.remaining);
	    }

	    
	    //compute how many times this filter fires
	    //find the stage with the max number of executions and add the primepump
	    //int maxExe = Math.max(filterInfo.initMult, filterInfo.steadyMult) +	    
	    //	filterInfo.primePump;
	    
	    //buffersize *= maxExe;
	    /*
	    
	    FilterTraceNode[] previousFilters = filterInfo.getPreviousFilters();
	    if (previousFilters.length == 1) {
		FilterInfo prevInfo = FilterInfo.getFilterInfo(previousFilters[0]);
		
		//find the stage with the max number of executions
		int maxExe = Math.max(Math.max(prevInfo.initMult, prevInfo.steadyMult),
				      prevInfo.primePump);
		//now mult the previous buffersize calc by the stage with the 
		//greatest number of executions...
		buffersize *= maxExe;
	    }
	    else {
		//splitjoin
		Utils.fail("Split/joins not supported");
	    }
	    */

	    JVariableDefinition recvBufVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					new CArrayType(filter.getInputType(), 
						       1 /* dimension */ ),
					recvBuffer + filterInfo.filter.getName(),
					bufferInitExp
					(filter, filter.getInputType(), 
					 buffersize));
	    
	    
	    //the size of the buffer 
	    JVariableDefinition recvBufferSizeVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					CStdType.Integer,
					recvBufferSize + filterInfo.filter.getName(),
					new JIntLiteral(buffersize));
	    
	    //the size of the buffer 
	    JVariableDefinition recvBufferBitsVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					CStdType.Integer,
					recvBufferBits + filterInfo.filter.getName(),
					new JIntLiteral(buffersize - 1));
	    
	    //the receive buffer index (start of the buffer)
	    JVariableDefinition recvBufferIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					recvBufferIndex + filterInfo.filter.getName(),
					new JIntLiteral(-1));
	    
	    //the index to the end of the receive buffer)
	    JVariableDefinition recvIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					recvIndex + filterInfo.filter.getName(),
					new JIntLiteral(-1));

	    generatedVariables.recvBuffer = recvBufVar;
	    decls.add(new JFieldDeclaration(null, recvBufVar, null, null));
	    generatedVariables.recvBufferSize = recvBufferSizeVar;
	    decls.add(new JFieldDeclaration(null, recvBufferSizeVar, null, null));
	    generatedVariables.recvBufferBits = recvBufferBitsVar;
	    decls.add(new JFieldDeclaration(null, recvBufferBitsVar, null, null));
	    generatedVariables.recvBufferIndex = recvBufferIndexVar;
	    decls.add(new JFieldDeclaration(null, recvBufferIndexVar, null, null));
	    generatedVariables.recvIndex = recvIndexVar;
	    decls.add(new JFieldDeclaration(null, recvIndexVar, null, null));

	}
	
	//if we are rate matching, create the output buffer with its 
	//index
	if (KjcOptions.ratematch && filter.getPushInt() > 0) {
	    //define the send buffer index variable
	    JVariableDefinition sendBufferIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					sendBufferIndex + uniqueID,
					new JIntLiteral(-1));
	    
	    generatedVariables.sendBufferIndex = sendBufferIndexVar;
	    decls.add(new JFieldDeclaration(null, sendBufferIndexVar, null, null));
	     
	    //define the send buffer
	    
	    JExpression[] dims = new JExpression[1];
	    //the size of the output array is number of executions in steady *
	    // number of items pushed * size of item
	    dims[0] = new JIntLiteral(filterInfo.steadyMult * filterInfo.push * 
				      Util.getTypeSize(filter.getOutputType()));

	    JVariableDefinition sendBufVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					new CArrayType(filter.getOutputType(), 
						       1 /* dimension */ ),
					sendBuffer + uniqueID, 
					new JNewArrayExpression(null,
								Util.getBaseType(filter.getOutputType()),
								dims, null));
	    generatedVariables.sendBuffer = sendBufVar;
	    decls.add(new JFieldDeclaration(null, sendBufVar, null, null));
	}
	
	
	//print the declarations for the array indices for pushing and popping
	//if this filter deals with arrays
	if (filter.getInputType().isArrayType() || 
	    filter.getOutputType().isArrayType()) {
	    int inputDim = 0, outputDim = 0, maxDim;
	    //find which array has the greatest dimensionality	   
	    if (filter.getInputType().isArrayType())
		inputDim = 
		    ((CArrayType)filter.getInputType()).getArrayBound();
	    if (filter.getOutputType().isArrayType()) 
		outputDim = 
		    ((CArrayType)filter.getOutputType()).getArrayBound();
	    maxDim = (inputDim > outputDim) ? inputDim : outputDim;
	    
	    generatedVariables.ARRAY_INDEX = new JVariableDefinition[maxDim];
	    
	    //create enough index vars as max dim
	    for (int i = 0; i < maxDim; i++) {
		JVariableDefinition arrayIndexVar = 
		    new JVariableDefinition(null, 
					    0, 
					    CStdType.Integer,
					    ARRAY_INDEX + i + uniqueID,
					    null);
		//remember the array index vars
		generatedVariables.ARRAY_INDEX[i] = arrayIndexVar;
		decls.add(new JFieldDeclaration(null, arrayIndexVar, null, null));
	    }
	}
	
	//convert the pops/peeks into buffer access 
	convertCommExprs();

	return (JFieldDeclaration[])decls.toArray(new JFieldDeclaration[0]);
    }

    public JMethodDeclaration[] getHelperMethods() 
    {
	Vector methods = new Vector();

	//add all helper methods, except work function and initWork
	for (int i = 0; i < filterInfo.filter.getMethods().length; i++) 
	    if (!(filterInfo.filter.getMethods()[i].equals(filterInfo.filter.getWork()) ||
		(filterInfo.isTwoStage() && 
		 filterInfo.filter.getInitWork().
		 equals(filterInfo.filter.getMethods()[i]))))
		methods.add(filterInfo.filter.getMethods()[i]);
	
	return (JMethodDeclaration[])methods.toArray(new JMethodDeclaration[0]);
    }

    public JMethodDeclaration getInitStageMethod() 
    {
	JBlock statements = new JBlock(null, new JStatement[0], null);
	FilterContent filter = filterInfo.filter;
	
	//add the call to initWork
	if (filterInfo.isTwoStage()) {
	    //FilterContent two = filter;
	    JBlock body = 
		(JBlock)ObjectDeepCloner.deepCopy
		(filter.getInitWork().getBody());

	    //add the code to receive the items into the buffer from the network
	    statements.addStatement
		(makeForLoop(receiveCode(filter, filter.getInputType(), 
					 generatedVariables),
			     generatedVariables.exeIndex,
			     new JIntLiteral(filterInfo.prePeek)));
	    
	    //now inline the init work body
	    statements.addStatement(body);
	    //if a simple filter, reset the simpleIndex
	    if (filterInfo.isSimple()) {
		statements.addStatement
		    (new JExpressionStatement(null,
					      (new JAssignmentExpression
					       (null,
						new JFieldAccessExpression
						(null, new JThisExpression(null), 
						 generatedVariables.simpleIndex.getIdent()),
						new JIntLiteral(-1))), null));
	    }
	}
	
	if (initFire - 1 > 0) {
	//add the code to collect enough data necessary to fire the 
	//work function for the first time
	    
	    if (filterInfo.bottomPeek > 0) {
		statements.addStatement
		    (makeForLoop(receiveCode(filter, filter.getInputType(),
					     generatedVariables),
				 generatedVariables.exeIndex,
				 new JIntLiteral(filterInfo.bottomPeek)));
	    }
	    
	    //add the calls for the work function in the initialization stage
	    statements.addStatement(generateInitWorkLoop
				    (filter, generatedVariables));
	}

	//add the code to collect all data produced by the upstream filter 
	//but not consumed by this filter in the initialization stage
	if (filterInfo.remaining > 0) {
	    statements.addStatement
		(makeForLoop(receiveCode(filter, filter.getInputType(),
					 generatedVariables),
			     generatedVariables.exeIndex,
			     new JIntLiteral(filterInfo.remaining))); 
	}

	if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
	    statements.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " Starting Steady-State"),
				       null));
	}
	
	//add the calls to the work function for the priming of the pipeline
	statements.addStatement(getWorkFunctionBlock(filterInfo.primePump));

	return new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
				      CStdType.Void,
				      initStage + uniqueID,
				      JFormalParameter.EMPTY,
				      CClassType.EMPTY,
				      statements,
				      null,
				      null); 
    }
    
    
    /** 
     * Return the block to call the work function in the steady state
     */
    public JBlock getSteadyBlock() 
    {
	return getWorkFunctionBlock(filterInfo.steadyMult);
    }
    

    /**
     * Generate code to receive data and call the work function mult times
     **/
    private JBlock getWorkFunctionBlock(int mult)
    {
	JBlock block = new JBlock(null, new JStatement[0], null);
	FilterContent filter = filterInfo.filter;
	//get the upsteam filter assuming that it does not pass thru another trace
	//this is only here for compatablility with the intra-tile, inter-trace
	//implementation

	//not rate matching
	    
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());

	//reset the simple index
	if (filterInfo.isSimple()) {
	    block.addStatement
		(new JExpressionStatement(null,
					  (new JAssignmentExpression
					   (null,
					    new JFieldAccessExpression
					    (null, new JThisExpression(null),
					     generatedVariables.simpleIndex.getIdent()),
					    new JIntLiteral(-1))), null));
	}
	
	//add the statements to receive pop into the buffer
	//execute this before the for loop that has the work function
	block.addStatement
	    (makeForLoop(receiveCode(filter, filter.getInputType(),
				     generatedVariables),
			 generatedVariables.exeIndex,
			 new JIntLiteral(filterInfo.pop)));

	//if we are in debug mode, print out that the filter is firing
	if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing."),
				       null));
	}

	//add the cloned work function to the block
	block.addStatement(workBlock);
	
	//if we are in decoupled mode do not put the work function in a for loop
	//and add the print statements
	if (KjcOptions.decoupled) {
	    block.addStatementFirst
		(new SIRPrintStatement(null, 
				       new JIntLiteral(0),
				       null));
	    block.addStatement(block.size(), 
			       new SIRPrintStatement(null, 
						     new JIntLiteral(1),
						     null));
	}
	else {
	    //create the for loop that will execute the work function
	    //local variable for the work loop
	    JVariableDefinition loopCounter = new JVariableDefinition(null,
								      0,
								      CStdType.Integer,
								      workCounter,
								      null);
	 	 
	    JStatement loop = makeForLoop(block, loopCounter, new JIntLiteral(mult));
	    block = new JBlock(null, new JStatement[0], null);
	    block.addStatement(new JVariableDeclarationStatement(null,
								 loopCounter,
								 null));
	    block.addStatement(loop);
	}

	return block;
    }


    //generate the loop for the work function firings in the 
    //initialization schedule
    JStatement generateInitWorkLoop(FilterContent filter, 
				    GeneratedVariables generatedVariables) 
    {
	JBlock block = new JBlock(null, new JStatement[0], null);
	
	//if a simple filter, reset the simpleIndex
	if (filterInfo.isSimple()) {
	    block.addStatement
		(new JExpressionStatement(null,
					  (new JAssignmentExpression
					   (null,
					    new JFieldAccessExpression
					    (null, new JThisExpression(null), 
					     generatedVariables.simpleIndex.getIdent()),
					    new JIntLiteral(-1))), null));
	}
	
	JStatement innerReceiveLoop = 
	    makeForLoop(receiveCode(filter, filter.getInputType(),
				    generatedVariables),
			generatedVariables.exeIndex,
			new JIntLiteral(filter.getPopInt()));
	
	JExpression isFirst = 
	    new JEqualityExpression(null,
				    false,
				    new JFieldAccessExpression
				    (null, new JThisExpression(null),
				     generatedVariables.exeIndex1.getIdent()),
				    new JIntLiteral(0));
	JStatement ifStatement = 
	    new JIfStatement(null,
			     isFirst,
			     innerReceiveLoop,
			     null, 
			     null);
	
	//add the if statement
	block.addStatement(ifStatement);
	
	//clone the work function and inline it
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.deepCopy(filter.getWork().getBody());
	
	//if we are in debug mode, print out that the filter is firing
	if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing (init)."),
				       null));
	}
	
	block.addStatement(workBlock);
	
	//return the for loop that executes the block init - 1
	//times
	return makeForLoop(block, generatedVariables.exeIndex1, 
			   new JIntLiteral(initFire - 1));
    }

    JBlock generateRateMatchSteadyState(FilterContent filter)
				
    {
	Utils.fail("This is not supported");
	
	JBlock block = new JBlock(null, new JStatement[0], null);

	//reset the simple index
	if (filterInfo.isSimple()){
	    block.addStatement
		(new JExpressionStatement(null,
					  (new JAssignmentExpression
					   (null,
					    new JLocalVariableExpression
					    (null, generatedVariables.simpleIndex),
					    new JIntLiteral(-1))), null));
	}
	    
	
	//should be at least peek - pop items in the buffer, so
	//just receive pop * filterInfo.steadyMult in the buffer and we can
	//run for an entire filterInfo.steadyMult state
	block.addStatement
	    (makeForLoop(receiveCode(filter, filter.getInputType(),
				     generatedVariables),
			 generatedVariables.exeIndex,
			 new JIntLiteral(filterInfo.pop * filterInfo.steadyMult)));

	
	//now, run the work function steady times...
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());

	//convert all of the push expressions in the steady state work block into
	//stores to the output buffer
	workBlock.accept(new SLIRReplacingVisitor() {
		/**
		 * Visits a push expression.
		 */
		public Object visitPushExpression(SIRPushExpression self,
						  CType tapeType,
						  JExpression arg) {
		    JExpression newExp = (JExpression)arg.accept(this);
		    //the expression is the argument of the call
		    JExpression[] args = new JExpression[1];
		    args[0] = newExp;
		    
		    JMethodCallExpression ratematchsend = 
			new JMethodCallExpression(null, new JThisExpression(null),
						  rateMatchSendMethod,
						  args);
		    //set the type of the method call
		    ratematchsend.setTapeType(tapeType);
		    return ratematchsend;
		}
	    });
	

	//if we are in debug mode, print out that the filter is firing
	if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing."),
				       null));
	}

	//add the cloned work function to the block
	block.addStatement
	    (makeForLoop(workBlock, generatedVariables.exeIndex,
	     new JIntLiteral(filterInfo.steadyMult)));
	
	//now add the code to push the output buffer onto the static network and 
	//reset the output buffer index
	//    for (steady*push*typesize)
	//        push(__SENDBUFFER__[++ __SENDBUFFERINDEX__])
	if (filterInfo.push > 0) {
	    
	    SIRPushExpression pushExp =  new SIRPushExpression
		(new JArrayAccessExpression
		 (null, 
		  new JLocalVariableExpression
		  (null,
		   generatedVariables.sendBuffer),
		  new JLocalVariableExpression
		  (null,
		   generatedVariables.exeIndex)));
	    
	    pushExp.setTapeType(Util.getBaseType(filter.getOutputType()));
	    
	    JExpressionStatement send = new JExpressionStatement(null, pushExp, null);
	    
	    block.addStatement
		(makeForLoop(send, generatedVariables.exeIndex,
			 new JIntLiteral(filterInfo.steadyMult * filterInfo.push * 
					 Util.getTypeSize(filter.getOutputType()))));
	    //reset the send buffer index
	    block.addStatement
		(new JExpressionStatement(null,
					  new JAssignmentExpression(null,
								    new JLocalVariableExpression
								    (null, generatedVariables.sendBufferIndex),
								    new JIntLiteral(-1)),
					  null));
	}
	

	//if we are in decoupled mode do not put the work function in a for loop
	//and add the print statements
	if (KjcOptions.decoupled) {
	    block.addStatementFirst
		(new SIRPrintStatement(null, 
				       new JIntLiteral(0),
				       null));
	    block.addStatement(block.size(), 
				    new SIRPrintStatement(null, 
							  new JIntLiteral(1),
							  null));
	}
	
	return block;
	
    }

    //returns the expression that will create the buffer array.  A JNewArrayExpression
    //with the proper type, dimensions, and size...
    private JExpression bufferInitExp(FilterContent filter, CType inputType,
				      int buffersize) 
    {
	//this is an array type
	if (inputType.isArrayType()) {
	    CType baseType = ((CArrayType)inputType).getBaseType();
	    //create the array to hold the dims of the buffer
	    JExpression baseTypeDims[] = ((CArrayType)inputType).getDims();
	    //the buffer is an array itself, so add one to the size of the input type
	    JExpression[] dims =  new JExpression[baseTypeDims.length + 1];
	    //the first dim is the buffersize
	    dims[0] = new JIntLiteral(buffersize);
	    //copy the dims for the basetype
	    for (int i = 0; i < baseTypeDims.length; i++)
		dims[i+1] = baseTypeDims[i];
	    
	    return new JNewArrayExpression(null, baseType, dims, null);
	}

	

	JExpression dims[] = {new JIntLiteral(buffersize)};
	return new JNewArrayExpression(null, inputType, dims, null);
	
    }

    private JStatement receiveCode(FilterContent filter, CType type, GeneratedVariables generatedVariables) {
	if (filterInfo.noBuffer()) 
	    return null;

	//the name of the method we are calling, this will
	//depend on type of the pop, by default set it to be the scalar receive
	String receiveMethodName = receiveMethod;

	JBlock statements = new JBlock(null, new JStatement[0], null);
	
	//if it is not a scalar receive change the name to the appropriate 
	//method call, from struct.h
	if (type.isArrayType()) 
	    receiveMethodName = arrayReceiveMethod;
	else if (type.isClassType()) {
	    receiveMethodName = structReceiveMethodPrefix  + type.toString();
	}

	//create the array access expression to access the buffer 
	JArrayAccessExpression arrayAccess = 
	    new JArrayAccessExpression(null,
				       new JFieldAccessExpression
				       (null, new JThisExpression(null),
					generatedVariables.recvBuffer.getIdent()),
				       bufferIndex(filter,
						   generatedVariables));
	
	//put the arrayaccess in an array...
	JExpression[] bufferAccess = 
	    {new JParenthesedExpression(null,
					arrayAccess)};
	 
	//the method call expression, for scalars, flatIRtoC
	//changes this into c code thatw will perform the receive...
	JExpression exp =
	    new JMethodCallExpression(null,  new JThisExpression(null),
				      receiveMethodName,
				      bufferAccess);
	
	((JMethodCallExpression)exp).setTapeType(filter.getInputType());

	//return a statement
	return new JExpressionStatement(null, exp, null);
    }


    //return the buffer access expression for the receive code
    //depends if this is a simple filter
    private JExpression bufferIndex(FilterContent filter, 
				    GeneratedVariables generatedVariables) 
    {
	if (filterInfo.isSimple()) {
	    return new JFieldAccessExpression
		(null, new JThisExpression(null),
		 generatedVariables.exeIndex.getIdent());
	}
	else {
	    //create the increment of the index var
	    JPrefixExpression bufferIncrement = 
		new JPrefixExpression(null, 
				      OPE_PREINC,
				      new JFieldAccessExpression
				      (null, new JThisExpression(null),
				       generatedVariables.recvIndex.getIdent()));
	
	    
	    //create the modulo expression
	    JBitwiseExpression indexAnd = 
		new JBitwiseExpression(null, 
				       OPE_BAND,
				       bufferIncrement, 
				       new JFieldAccessExpression
				       (null, new JThisExpression(null),
					generatedVariables.recvBufferBits.getIdent()));
	    
	    return indexAnd;
	}
    }
}

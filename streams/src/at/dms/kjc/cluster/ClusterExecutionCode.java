package at.dms.kjc.cluster;

import at.dms.kjc.raw.Util;
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

public class ClusterExecutionCode extends at.dms.util.Utils 
    implements FlatVisitor, Constants
{
    /*** fields for the var names we introduce ***/
    public static String recvBuffer = "__RECVBUFFER__";
    public static String recvBufferSize = "__RECVBUFFERSIZE__";
    public static String recvBufferBits = "__RECVBUFFERBITS__";

    //the output buffer for ratematching
    public static String sendBuffer = "__SENDBUFFER__";
    public static String sendBufferIndex = "__SENDBUFFERINDEX__";
    public static String rateMatchSendMethod = "__RATEMATCHSEND__";
    
    //recvBufferIndex points to the beginning of the tape
    public static String recvBufferIndex = "__RECVBUFFERINDEX__";
    //recvIndex points to the end of the tape
    public static String recvIndex = "_RECVINDEX__";

    public static String simpleIndex = "__SIMPLEINDEX__";
    
    public static String exeIndex = "__EXEINDEX__";
    public static String exeIndex1 = "__EXEINDEX__1__";

    public static String ARRAY_INDEX = "__ARRAY_INDEX__";
    public static String ARRAY_COPY = "__ARRAY_COPY__";

    public static String initSchedFunction = "__RAWINITSCHED__";
    public static String steadySchedFunction = "__RAWSTEADYSCHED__";
    
    public static String receiveMethod = "static_receive_to_mem";
    public static String structReceiveMethodPrefix = "__popPointer";
    public static String arrayReceiveMethod = "__array_receive__";

    public static String rawMain = "__CLUSTERMAIN__";
    

    //These next fields are set by calculateItems()
    //see my thesis for a better explanation
    //number of items to receive between preWork() and work()
    private int bottomPeek = 0; 
    //number of times the filter fires in the init schedule
    private int initFire = 0;
    //number of items to receive after initialization
    private int remaining = 0;
    
    private int nodeID;

    //class to hold the local variables we create 
    //so we can pass these across functions
    class LocalVariables 
    {
	JVariableDefinition recvBuffer;
	JVariableDefinition recvBufferSize;
	JVariableDefinition recvBufferBits;
	JVariableDefinition recvBufferIndex;
	JVariableDefinition recvIndex;
	JVariableDefinition exeIndex;
	JVariableDefinition exeIndex1;
	JVariableDefinition[] ARRAY_INDEX;
	JVariableDefinition[] ARRAY_COPY;
	JVariableDefinition simpleIndex;
	JVariableDefinition sendBufferIndex;
	JVariableDefinition sendBuffer;
    }
    
    
    public static void doit(FlatNode top) 
    {
	top.accept((new ClusterExecutionCode()), null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){

	    SIRFilter filter = (SIRFilter)node.contents;

	    nodeID = NodeEnumerator.getSIROperatorId(filter);

	    /*
	    System.out.print("ClusterExecCode visiting: "+filter.getName()+ " [FILTER]" );

	    if (isSimple(filter))
		System.out.print("(simple) ");
	    else if (noBuffer(filter))
		System.out.print("(no buffer) ");
	    else {
		if (remaining > 0)
		    System.out.print("(remaining) ");
		if (filter.getPeekInt() > filter.getPopInt())
		    System.out.print("(peeking)");
		
	    }
	    System.out.println();
	    */

	    LocalVariables localVariables = new LocalVariables();	    
	    JBlock block = new JBlock(null, new JStatement[0], null);
	    
	    createLocalVariables(node, block, localVariables);
	    //convertCommExps(filter, 
	    //		    isSimple(filter),
	    //	    localVariables);
	    
	    clusterMainFunction(node, block, localVariables);
	    
	    //create the method and add it to the filter
	    JMethodDeclaration clusterMainFunct = 
		new JMethodDeclaration(null, 
				       at.dms.kjc.Constants.ACC_PUBLIC,
				       CStdType.Void,
				       rawMain,
				       JFormalParameter.EMPTY,
				       CClassType.EMPTY,
				       block,
				       null,
				       null);
	    filter.addMethod(clusterMainFunct);


	} 

    }


    private boolean noBuffer(SIRFilter filter) 
	{
	    //always need a buffer for rate matching.
	    //	    if (KjcOptions.ratematch)
	    //	return false;
	    
	    if (filter.getPeekInt() == 0 &&
		(!(filter instanceof SIRTwoStageFilter) ||
		 (((SIRTwoStageFilter)filter).getInitPeek() == 0)))
		return true;
	    return false;		
	}


    private boolean isSimple(SIRFilter filter) 
    {
 	if (noBuffer(filter))
 	    return false;
	
 	if (filter.getPeekInt() == filter.getPopInt() &&
 	    remaining == 0 &&
 	    (!(filter instanceof SIRTwoStageFilter) ||
 	     ((SIRTwoStageFilter)filter).getInitPop() ==
 	     ((SIRTwoStageFilter)filter).getInitPeek()))
 	    return true;
 	return false;
    }
    


    private void createLocalVariables(FlatNode node, JBlock block,
			      LocalVariables localVariables)
    {
	SIRFilter filter = (SIRFilter)node.contents;

	//index variable for certain for loops
	JVariableDefinition exeIndexVar = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex,
				    null);

	//remember the JVarDef for latter (in the raw main function)
	localVariables.exeIndex = exeIndexVar;
	
	block.addStatement
	    (new JVariableDeclarationStatement(null,
					       exeIndexVar,
					       null));
	
	//index variable for certain for loops
	JVariableDefinition exeIndex1Var = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex1,
				    null);

	localVariables.exeIndex1 = exeIndex1Var;

	block.addStatement
	    (new JVariableDeclarationStatement(null,
					       exeIndex1Var,
					       null));
	
	
	//only add the receive buffer and its vars if the 
	//filter receives data
	if (!noBuffer(filter)) {
	    //initialize the buffersize to be the size of the 
	    //struct being passed over it
	    int buffersize;
	    
	    int prepeek = 0;
	    int maxpeek = filter.getPeekInt();
	    if (filter instanceof SIRTwoStageFilter)
		prepeek = ((SIRTwoStageFilter)filter).getInitPeek();
	    //set up the maxpeek
	    maxpeek = (prepeek > maxpeek) ? prepeek : maxpeek;
	    
	    
	    if (isSimple(filter)) {
		//simple filter (no remaining items)
		if (KjcOptions.ratematch) {
		    //System.out.println(filter.getName());
		    
		    int i = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();

		    //i don't know, the prepeek could be really large, so just in case
		    //include it.  Make the buffer big enough to hold 
		    buffersize = 
			Math.max
			((((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue() - 1) * 
			 filter.getPopInt() + filter.getPeekInt(), prepeek);
		}
		else //not ratematching and we do not have a circular buffer
		    buffersize = maxpeek;

		//define the simple index variable
		JVariableDefinition simpleIndexVar = 
		    new JVariableDefinition(null, 
					    0, 
					    CStdType.Integer,
					    simpleIndex,
					    new JIntLiteral(-1));
		
		//remember the JVarDef for latter (in the raw main function)
		localVariables.simpleIndex = simpleIndexVar;
		
		block.addStatement
		    (new JVariableDeclarationStatement(null,
						       simpleIndexVar,
						       null));
	    }
	    else { //filter with remaing items on the buffer after initialization 
		//see Mgordon's thesis for explanation (Code Generation Section)
		if (KjcOptions.ratematch)
		    buffersize = 
			Util.nextPow2(Math.max((((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue() - 1) * 
					       filter.getPopInt() + filter.getPeekInt(), prepeek) + remaining);
		else
		    buffersize = Util.nextPow2(maxpeek + remaining);
	    }
	    
	    JVariableDefinition recvBufVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					new CArrayType(filter.getInputType(), 
						       1  ),
					recvBuffer,
					bufferInitExp
					(filter, filter.getInputType(), 
					 buffersize));
	    
	    
	    //the size of the buffer 
	    JVariableDefinition recvBufferSizeVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					CStdType.Integer,
					recvBufferSize,
					new JIntLiteral(buffersize));
	    
	    //the size of the buffer 
	    JVariableDefinition recvBufferBitsVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					CStdType.Integer,
					recvBufferBits,
					new JIntLiteral(buffersize - 1));
	    
	    //the receive buffer index (start of the buffer)
	    JVariableDefinition recvBufferIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					recvBufferIndex,
					new JIntLiteral(-1));
	    
	    //the index to the end of the receive buffer)
	    JVariableDefinition recvIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					recvIndex,
					new JIntLiteral(-1));

	    JVariableDefinition[] indices = 
		{		 
		    recvBufferSizeVar,
		    recvBufferBitsVar,
		    recvBufferIndexVar,
		    recvIndexVar
		};
	    
	    localVariables.recvBuffer = recvBufVar;
	    localVariables.recvBufferSize = recvBufferSizeVar;
	    localVariables.recvBufferBits = recvBufferBitsVar;
	    localVariables.recvBufferIndex = recvBufferIndexVar;
	    localVariables.recvIndex = recvIndexVar;

	    block.addStatement
		(new JVariableDeclarationStatement(null,
						   indices,
						   null));

	    block.addStatement
		(new JVariableDeclarationStatement(null,
						   recvBufVar, 
						   null));
	    
	}
	
	//if we are rate matching, create the output buffer with its 
	//index
	if (KjcOptions.ratematch && filter.getPushInt() > 0) {
	    int steady = ((Integer)ClusterBackend.steadyExecutionCounts.
			  get(ClusterBackend.filter2Node.get(filter))).intValue();
	    
	    //define the send buffer index variable
	    JVariableDefinition sendBufferIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					sendBufferIndex,
					new JIntLiteral(-1));
	    
	    localVariables.sendBufferIndex = sendBufferIndexVar;
	    block.addStatement
		(new JVariableDeclarationStatement(null,
						   sendBufferIndexVar,
						   null));
	    //define the send buffer
	    
	    JExpression[] dims = new JExpression[1];
	    //the size of the output array is number of executions in steady *
	    // number of items pushed * size of item
	    dims[0] = new JIntLiteral(steady * filter.getPushInt() * 
				      Util.getTypeSize(filter.getOutputType()));

	    JVariableDefinition sendBufVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					new CArrayType(filter.getOutputType(), 
						          1),
					sendBuffer,
					new JNewArrayExpression(null,
								Util.getBaseType(filter.getOutputType()),
								dims, null));
	    localVariables.sendBuffer = sendBufVar;
	    block.addStatement
		(new JVariableDeclarationStatement(null,
						   sendBufVar,
						   null));
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
	    
	    localVariables.ARRAY_INDEX = new JVariableDefinition[maxDim];
	    
	    //create enough index vars as max dim
	    for (int i = 0; i < maxDim; i++) {
		JVariableDefinition arrayIndexVar = 
		    new JVariableDefinition(null, 
					    0, 
					    CStdType.Integer,
					    ARRAY_INDEX + i,
					    null);
		//remember the array index vars
		localVariables.ARRAY_INDEX[i] = arrayIndexVar;
		
		block.addStatement
		    (new JVariableDeclarationStatement(null,
						       arrayIndexVar, 
						       null));
	    }
	}
    }



    
    //returns the expression that will create the buffer array.  A JNewArrayExpression
    //with the proper type, dimensions, and size...
    private JExpression bufferInitExp(SIRFilter filter, CType inputType,
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


    private void clusterMainFunction(FlatNode node, JBlock statements,
				 LocalVariables localVariables) 
    {
	SIRFilter filter = (SIRFilter)node.contents;
	// don't support file readers, file writers yet
	assert !(filter instanceof SIRPredefinedFilter) ||
            (filter instanceof SIRIdentity):
            "Found a " + filter.getName() +
            ", but cluster backend doesn't yet support pre-defined filters.";
	
	//generate the code to define the local variables
	//defineLocalVars(statements, localVariables);

	//create the params list, for some reason 
	//calling toArray() on the list breaks a later pass
	List paramList = filter.getParams();
	JExpression[] paramArray;
	if (paramList == null || paramList.size() == 0)
	    paramArray = new JExpression[0];
	else
	    paramArray = (JExpression[])paramList.toArray(new JExpression[0]);
	
	//add the call to the init function
	statements.addStatement
	    (new 
	     JExpressionStatement(null,
				  new JMethodCallExpression
				  (null,
				   new JThisExpression(null),
				   filter.getInit().getName(),
				   paramArray),
				  null));
	
	//add the call to initWork
	if (filter instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
	    JBlock body = 
		(JBlock)ObjectDeepCloner.deepCopy
		(two.getInitWork().getBody());

	    //add the code to receive the items into the buffer
	    statements.addStatement
		(makeForLoop(receiveCode(filter, filter.getInputType(), 
					 localVariables),
			     localVariables.exeIndex,
			     new JIntLiteral(two.getInitPeek())));
	    
	    //now inline the init work body
	    statements.addStatement(body);
	    //if a simple filter, reset the simpleIndex
	    if (isSimple(filter)) {
		statements.addStatement
		    (new JExpressionStatement(null,
					      (new JAssignmentExpression
					       (null,
						new JLocalVariableExpression
						(null, localVariables.simpleIndex),
						new JIntLiteral(-1))), null));
	    }
	}
	
		
	    
	if (initFire - 1 > 0) {
	//add the code to collect enough data necessary to fire the 
	//work function for the first time
	    
	    if (bottomPeek > 0) {
		statements.addStatement
		    (makeForLoop(receiveCode(filter, filter.getInputType(),
					     localVariables),
				 localVariables.exeIndex,
				 new JIntLiteral(bottomPeek)));
	    }
	    
	    //add the calls for the work function in the initialization stage
	    statements.addStatement(generateInitWorkLoop
				    (filter, localVariables));
	}

	//add the code to collect all data produced by the upstream filter 
	//but not consumed by this filter in the initialization stage
	if (remaining > 0) {
	   statements.addStatement
		(makeForLoop(receiveCode(filter, filter.getInputType(),
					 localVariables),
			     localVariables.exeIndex,
			     new JIntLiteral(remaining))); 
	}

	if (ClusterBackend.FILTER_DEBUG_MODE) {
	    statements.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " Starting Steady-State"),
				       null));
	}
	
	//add the call to the work function
	statements.addStatement(generateSteadyStateLoop(node,
							localVariables));
	
	
    }



    //convert the peek and pop expressions for a filter into
    //buffer accesses, do this for all functions just in case helper
    //functions call peek or pop
    private void convertCommExps(SIRFilter filter, boolean simple,
				 LocalVariables localVars) 
    {
	SLIRReplacingVisitor convert;
	
	if (simple) {
	    convert = 
		new ConvertCommunicationSimple(localVars);
	}
	else
	    convert = new ConvertCommunication(localVars);
	
	JMethodDeclaration[] methods = filter.getMethods();
	for (int i = 0; i < methods.length; i++) {
	    //iterate over the statements and call the ConvertCommunication
	    //class to convert peek, pop
	    for (ListIterator it = methods[i].getStatementIterator();
		 it.hasNext(); ){
		((JStatement)it.next()).accept(convert);
	    }
	}
    }


    JStatement receiveCode(SIRFilter filter, CType type, LocalVariables localVariables) {
	if (noBuffer(filter)) 
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
				       new JLocalVariableExpression
				       (null,
					localVariables.recvBuffer),
				       bufferIndex(filter,
						   localVariables));
	
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

	//return a statement
	return new JExpressionStatement(null, exp, null);
    }
    
    

    //generate the loop for the work function firings in the 
    //initialization schedule
    JStatement generateInitWorkLoop(SIRFilter filter, 
				    LocalVariables localVariables) 
    {
	JStatement innerReceiveLoop = 
	    makeForLoop(receiveCode(filter, filter.getInputType(),
				    localVariables),
			localVariables.exeIndex,
			new JIntLiteral(filter.getPopInt()));
	
	JExpression isFirst = 
	    new JEqualityExpression(null,
				    false,
				    new JLocalVariableExpression
				    (null, 
				     localVariables.exeIndex1),
				    new JIntLiteral(0));
	JStatement ifStatement = 
	    new JIfStatement(null,
			     isFirst,
			     innerReceiveLoop,
			     null, 
			     null);
	
	JBlock block = new JBlock(null, new JStatement[0], null);

	//if a simple filter, reset the simpleIndex
	if (isSimple(filter)){
	    block.addStatement
		(new JExpressionStatement(null,
					  (new JAssignmentExpression
					   (null,
					    new JLocalVariableExpression
					    (null, localVariables.simpleIndex),
					    new JIntLiteral(-1))), null));
	}
	
	//add the if statement
	block.addStatement(ifStatement);
	
	//clone the work function and inline it
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.deepCopy(filter.getWork().getBody());
	
	//if we are in debug mode, print out that the filter is firing
	if (ClusterBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing (init)."),
				       null));
	}
	
	block.addStatement(workBlock);
	
	//return the for loop that executes the block init - 1
	//times
	return makeForLoop(block, localVariables.exeIndex1, 
			   new JIntLiteral(initFire - 1));
    }
    
    private int lcm(int x, int y) { // least common multiple
	int v = x, u = y;
	while (x != y)
	    if (x > y) {
		x -= y; v += u;
	    } else {
		y -= x; u += v;
	    }
	return (u+v)/2;
    }


     // Returns a for loop that uses field <var> to count
     //<count> times with the body of the loop being <body>.  If count
     //is non-positive, just returns empty (!not legal in the general case)

    private static JStatement makeForLoop(JStatement body,
					  JLocalVariable var,
					  JExpression count) {
	if (body == null)
	    return new JEmptyStatement(null, null);
	
	// make init statement - assign zero to <var>.  We need to use
	// an expression list statement to follow the convention of
	// other for loops and to get the codegen right.
	JExpression initExpr[] = {
	    new JAssignmentExpression(null,
				      new JLocalVariableExpression(null,
								   var),
				      new JIntLiteral(0)) };
	JStatement init = new JExpressionListStatement(null, initExpr, null);
	// if count==0, just return init statement
	if (count instanceof JIntLiteral) {
	    int intCount = ((JIntLiteral)count).intValue();
	    if (intCount<=0) {
		// return assignment statement
		return new JEmptyStatement(null, null);
	    }
	}
	// make conditional - test if <var> less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JLocalVariableExpression(null,
								   var),
				      count);
	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JLocalVariableExpression(null,
								   var));
	JStatement incr = 
	    new JExpressionStatement(null, incrExpr, null);

	return new JForStatement(null, init, cond, incr, body, null);
    }


    //generate the code for the steady state loop
    JStatement generateSteadyStateLoop(FlatNode node, 
				       LocalVariables localVariables) 
    {
	SIRFilter filter = (SIRFilter)node.contents;
	
	//is we are rate matching generate the appropriate code
	//if (KjcOptions.ratematch) 
	//    return generateRateMatchSteadyState(filter, localVariables);

	JBlock block = new JBlock(null, new JStatement[0], null);

	block.addStatement(new JExpressionStatement(null, new JMethodCallExpression(null, "check_messages", new JExpression[0]), null));

	//reset the simple index
	if (isSimple(filter)){
	    block.addStatement
		(new JExpressionStatement(null,
					  (new JAssignmentExpression
					   (null,
					    new JLocalVariableExpression
					    (null, localVariables.simpleIndex),
					    new JIntLiteral(-1))), null));
	}
	    
	
	//add the statements to receive pop items into the buffer
	block.addStatement
	    (makeForLoop(receiveCode(filter, filter.getInputType(),
				     localVariables),
			 localVariables.exeIndex,
			 new JIntLiteral(filter.getPopInt())));

	

	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());

	//if we are in debug mode, print out that the filter is firing
	if (ClusterBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing."),
				       null));
	}

	//add the cloned work function to the block
	block.addStatement(workBlock);
	//	}
	
	//	return block;
	
	//if we are in decoupled mode do not put the work function in a for loop
	//and add the print statements
	

	//if (KjcOptions.decoupled) {
	//    block.addStatementFirst
	//	(new SIRPrintStatement(null, 
	//			       new JIntLiteral(0),
	//			       null));
	//    block.addStatement(block.size(), 
	//			    new SIRPrintStatement(null, 
	//						  new JIntLiteral(1),
	//						  null));
	//    return block;
	//}

	block.addStatement(new JExpressionStatement(null, new JMethodCallExpression(null, "send_credits", new JExpression[0]), null));
	
	JVariableDefinition var = 
	    new JVariableDefinition(null, 0, (CType)CStdType.Integer, 
				    "__counter_"+nodeID, 
				    new JIntLiteral(0));

	JVariableDefinition var2 = 
	    new JVariableDefinition(null, 0, (CType)CStdType.Integer, 
				    "__number_of_iterations", 
				    new JIntLiteral(0));

	JVariableDeclarationStatement var_st = 
	    new JVariableDeclarationStatement( null, var, null);


	JStatement init_stmt = new JExpressionStatement(null,
					  (new JAssignmentExpression
					   (null,
					    new JLocalVariableExpression
					    (null, var),
					    new JIntLiteral(1))), null);

	// num iters = init + num_steady_iterations_specified_by_user * steady
	Integer initCounts = (Integer)ClusterBackend.initExecutionCounts.get(node);
	int init;
	if (initCounts==null) {
	    init = 0;
	} else {
	    init = initCounts.intValue();
	}
	int counts;
	// hack to get identities to work for now -- we might not know
	// how many times they execute (since GraphFlattener inserts
	// some) so in this case just execute them forever
	Integer count = (Integer)ClusterBackend.steadyExecutionCounts.get(node);
	if (count!=null) {
	    counts = count.intValue();
	} else if (node.contents instanceof SIRIdentity) {
	    counts = 100000;
	} else {
	    counts = -1;
	    Utils.fail("don't know how many times to execute node " + node.contents);
	}

	JExpression numIters = new JAddExpression(null,
						  new JIntLiteral(init),
						  new JMultExpression(null,
								      new JLocalVariableExpression(null, var2),
								      new JIntLiteral(counts)));

	JExpression relation = new JRelationalExpression(null, Constants.OPE_LE, new JLocalVariableExpression(null, var), numIters);

	JStatement incr_expr = new JExpressionStatement(null, new JPostfixExpression(null, OPE_POSTINC, new JLocalVariableExpression(null, var)), null);


	JBlock for_block = new JBlock(null, new JStatement[0], null);

	for_block.addStatement(new JExpressionStatement(null, new JMethodCallExpression(null, "send_credits", new JExpression[0]), null));
	for_block.addStatement(new JForStatement(null,  
					init_stmt, 
					relation,
					incr_expr,
					block,
					null));


	return for_block;
	//return the infinite loop
	
	//return new JWhileStatement(null, 
	//			   new JBooleanLiteral(null, true),
	//			   block, 
	//			   null);
    }

    //return the buffer access expression for the receive code
    //depends if this is a simple filter
    private JExpression bufferIndex(SIRFilter filter, 
				     LocalVariables localVariables) 
    {
	if (isSimple(filter)) {
	    return new JLocalVariableExpression
		(null, 
		 localVariables.exeIndex);
	}
	else {
	    //create the increment of the index var
	    JPrefixExpression bufferIncrement = 
		new JPrefixExpression(null, 
				      OPE_PREINC,
				      new JLocalVariableExpression
				      (null,localVariables.recvIndex));
	    
	    //create the modulo expression
	    //JModuloExpression indexMod = 
	    //	new JModuloExpression(null, bufferIncrement, 
	    //			  new JLocalVariableExpression
	    //			  (null,
	    //			   localVariables.recvBufferSize));
	    
	    
	     //create the modulo expression
	    JBitwiseExpression indexAnd = 
		new JBitwiseExpression(null, 
				       OPE_BAND,
				       bufferIncrement, 
				       new JLocalVariableExpression
				       (null,
					localVariables.recvBufferBits));
	    
	    return indexAnd;
	}
    }
    
		    
    // private void defineLocalVars(JBlock block,
// 				 LocalVariables localVariables

    JStatement generateRateMatchSteadyState(SIRFilter filter,
					    LocalVariables localVariables) 
    {
	JBlock block = new JBlock(null, new JStatement[0], null);
	int steady = ((Integer)ClusterBackend.steadyExecutionCounts.
	    get(ClusterBackend.filter2Node.get(filter))).intValue();

	//reset the simple index
	if (isSimple(filter)){
	    block.addStatement
		(new JExpressionStatement(null,
					  (new JAssignmentExpression
					   (null,
					    new JLocalVariableExpression
					    (null, localVariables.simpleIndex),
					    new JIntLiteral(-1))), null));
	}
	    
	
	//should be at least peek - pop items in the buffer, so
	//just receive pop * steady in the buffer and we can
	//run for an entire steady state
	block.addStatement
	    (makeForLoop(receiveCode(filter, filter.getInputType(),
				     localVariables),
			 localVariables.exeIndex,
			 new JIntLiteral(filter.getPopInt() * steady)));

	
	//now, run the work function steady times...
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.
	    deepCopy(filter.getWork().getBody());

	//convert all of the push expressions in the steady state work block into
	//stores to the output buffer
	workBlock.accept(new RateMatchConvertPush());

	//if we are in debug mode, print out that the filter is firing
	if (ClusterBackend.FILTER_DEBUG_MODE) {
	    block.addStatement
		(new SIRPrintStatement(null,
				       new JStringLiteral(null, filter.getName() + " firing."),
				       null));
	}

	//add the cloned work function to the block
	block.addStatement
	    (makeForLoop(workBlock, localVariables.exeIndex,
	     new JIntLiteral(steady)));
	
	//now add the code to push the output buffer onto the static network and 
	//reset the output buffer index
	//    for (steady*push*typesize)
	//        push(__SENDBUFFER__[++ __SENDBUFFERINDEX__])
	if (filter.getPushInt() > 0) {
	    
	    SIRPushExpression pushExp =  new SIRPushExpression
		(new JArrayAccessExpression
		 (null, 
		  new JLocalVariableExpression
		  (null,
		   localVariables.sendBuffer),
		  new JLocalVariableExpression
		  (null,
		   localVariables.exeIndex)));
	    
	    pushExp.setTapeType(Util.getBaseType(filter.getOutputType()));
	    
	    JExpressionStatement send = new JExpressionStatement(null, pushExp, null);
	    
	    block.addStatement
		(makeForLoop(send, localVariables.exeIndex,
			 new JIntLiteral(steady * filter.getPushInt() * 
					 Util.getTypeSize(filter.getOutputType()))));
	    //reset the send buffer index
	    block.addStatement
		(new JExpressionStatement(null,
					  new JAssignmentExpression(null,
								    new JLocalVariableExpression
								    (null, localVariables.sendBufferIndex),
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
	    return block;
	}
	
	//return the infinite loop
	return new JWhileStatement(null, 
				   new JBooleanLiteral(null, true),
				   block, 
				   null);
	
    }
    








  
    class ConvertCommunicationSimple extends SLIRReplacingVisitor 
    {
	LocalVariables localVariables;
	
	
	public ConvertCommunicationSimple(LocalVariables locals) 
	{
	    localVariables = locals;
	}
	
	//for pop expressions convert to the form
	// (recvBuffer[++recvBufferIndex % recvBufferSize])
	public Object visitPopExpression(SIRPopExpression oldSelf,
					 CType oldTapeType) {
	  
	   // do the super
	    SIRPopExpression self = 
		(SIRPopExpression)
		super.visitPopExpression(oldSelf, oldTapeType);

	    //create the increment of the index var
	    JPrefixExpression increment = 
		new JPrefixExpression(null, 
				      OPE_PREINC,
				      new JLocalVariableExpression
				      (null, 
				       localVariables.simpleIndex));
	    
	    //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression
					   (null,
					    localVariables.recvBuffer),
					   increment);

	    //return the parenthesed expression
	    return new JParenthesedExpression(null,
					      bufferAccess);
	}
	
	//convert peek exps into:
	// (recvBuffer[(recvBufferIndex + (arg) + 1) mod recvBufferSize])
	public Object visitPeekExpression(SIRPeekExpression oldSelf,
					  CType oldTapeType,
					  JExpression oldArg) {
	    // do the super
	    SIRPeekExpression self = 
		(SIRPeekExpression)
		super.visitPeekExpression(oldSelf, oldTapeType, oldArg);


	    JAddExpression argIncrement = 
		new JAddExpression(null, self.getArg(), new JIntLiteral(1));
	    
	    JAddExpression index = 
		new JAddExpression(null,
				   new JLocalVariableExpression
				   (null, 
				    localVariables.simpleIndex),
				   argIncrement);

	      //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression
					   (null,
					    localVariables.recvBuffer),
					   index);

	    //return the parenthesed expression
	    return new JParenthesedExpression(null,
					      bufferAccess);
	    
	}
	
    }
    
    

    class ConvertCommunication extends SLIRReplacingVisitor 
    {
	LocalVariables localVariables;
	
	
	public ConvertCommunication(LocalVariables locals) 
	{
	    localVariables = locals;
	}
	
	//for pop expressions convert to the form
	// (recvBuffer[++recvBufferIndex % recvBufferSize])
	public Object visitPopExpression(SIRPopExpression self,
					 CType tapeType) {
	  
	    //create the increment of the index var
	    JPrefixExpression bufferIncrement = 
		new JPrefixExpression(null, 
				      OPE_PREINC,
				      new JLocalVariableExpression
				      (null, 
				       localVariables.recvBufferIndex));
	    
	    JBitwiseExpression indexAnd = 
		new JBitwiseExpression(null, 
				       OPE_BAND,
				       bufferIncrement, 
				       new JLocalVariableExpression
				       (null,
					localVariables.recvBufferBits));
	    
	    //create the modulo expression
	    //JModuloExpression indexMod = 
	//	new JModuloExpression(null, bufferIncrement, 
	//			  new JLocalVariableExpression
	//			  (null,
	//			   localVariables.recvBufferSize));
	    
	    //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression
					   (null,
					    localVariables.recvBuffer),
					   indexAnd);

	    //return the parenthesed expression
	    return new JParenthesedExpression(null,
					      bufferAccess);
	}
	
	//convert peek exps into:
	// (recvBuffer[(recvBufferIndex + (arg) + 1) mod recvBufferSize])
	public Object visitPeekExpression(SIRPeekExpression oldSelf,
					  CType oldTapeType,
					  JExpression oldArg) {
	    // do the super
	    SIRPeekExpression self = 
		(SIRPeekExpression)
		super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
	    

	    //create the index calculation expression
	    JAddExpression argIncrement = 
		new JAddExpression(null, self.getArg(), new JIntLiteral(1));
	    JAddExpression index = 
		new JAddExpression(null,
				   new JLocalVariableExpression
				   (null, 
				    localVariables.recvBufferIndex),
				   argIncrement);
	    
	    JBitwiseExpression indexAnd = 
		new JBitwiseExpression(null, 
				       OPE_BAND,
				       index, 
				       new JLocalVariableExpression
				       (null,
					localVariables.recvBufferBits));
	 
	    
	    //create the mod expression
	    //JModuloExpression indexMod = 
	    //	new JModuloExpression(null, index,
	//			      new JLocalVariableExpression
	//			      (null,
	//			       localVariables.recvBufferSize));
	    

	    //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression
					   (null,
					    localVariables.recvBuffer),
					   indexAnd);

	    //return the parenthesed expression
	    return new JParenthesedExpression(null,
					      bufferAccess);
	}
	
    }


    //this pass will convert all push statements to a method call expression
    //so that in FlatIRtoC we can recognize the call and replace it with a store to
    //the output buffer instead of a send onto the network...
    class RateMatchConvertPush extends SLIRReplacingVisitor 
    {
	LocalVariables localVariables;
	
	
	public RateMatchConvertPush() 
	{
	    
	}
  
	 //
	 //  Visits a push expression.
	 //
	public Object visitPushExpression(SIRPushExpression self,
					  CType tapeType,
					  JExpression arg) {
	    JExpression newExp = (JExpression)arg.accept(this);
	    //the expression is the argument of the call
	    JExpression[] args = new JExpression[1];
	    args[0] = newExp;
	    
	    JMethodCallExpression ratematchsend = 
		new JMethodCallExpression(null, new JThisExpression(null),
					  ClusterExecutionCode.rateMatchSendMethod,
					  args);
	    
	    return ratematchsend;
	}
    }


	/*
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    SIRFilter filter = (SIRFilter)node.contents;
	    //Skip Identities now
	    if(filter instanceof SIRIdentity ||
	       filter instanceof SIRFileWriter ||
	       filter instanceof SIRFileReader)
		return;
	    if (!KjcOptions.decoupled)
		calculateItems(filter);
	    System.out.print("Generating Cluster Code: " + 
			     node.contents.getName() + " ");
	    
	    //attempt to generate direct communication code
	    //(no buffer), if this returns true it was sucessful
	    //and the code was produced 
	    if (bottomPeek == 0 && 
		remaining == 0 &&
		DirectCommunication.doit((SIRFilter)node.contents)) {
		System.out.println("(Direct Communication)");
		return;
	    }
	    
	    //otherwise generate code the old way
	    LocalVariables localVariables = new LocalVariables();
	    
	    JBlock block = new JBlock(null, new JStatement[0], null);
	    
	    if (isSimple(filter))
		System.out.print("(simple) ");
	    else if (noBuffer(filter))
		System.out.print("(no buffer) ");
	    else {
		if (remaining > 0)
		    System.out.print("(remaining) ");
		if (filter.getPeekInt() > filter.getPopInt())
		    System.out.print("(peeking)");
		
	    }
	    System.out.println();
	    
	    createLocalVariables(node, block, localVariables);
	    convertCommExps(filter, 
			    isSimple(filter),
			    localVariables);
	    
	    clusterMainFunction(node, block, localVariables);
	    
	    //create the method and add it to the filter
	    JMethodDeclaration clusterMainFunct = 
		new JMethodDeclaration(null, 
				       at.dms.kjc.Constants.ACC_PUBLIC,
				       CStdType.Void,
				       rawMain,
				       JFormalParameter.EMPTY,
				       CClassType.EMPTY,
				       block,
				       null,
				       null);
	    filter.addMethod(clusterMainFunct);
	}
    } 

    //calcuate bottomPeek, initFire, remaining
    //see my thesis section 5.1.2
    void calculateItems(SIRFilter filter) 
    {
	int pop = filter.getPopInt();
	int peek = filter.getPeekInt();
	
	//set up prePop, prePeek
	int prePop = 0;
	int prePeek = 0;
	
	if (filter instanceof SIRTwoStageFilter) {
	    prePop = ((SIRTwoStageFilter)filter).getInitPop();
	    prePeek = ((SIRTwoStageFilter)filter).getInitPeek();
	}
	
	//the number of times this filter fires in the initialization
	//schedule
	initFire = 0;

	// initExec counts might be null if we're calling for work
	// estimation before exec counts have been determined.
	if (ClusterBackend.initExecutionCounts!=null) {
	    Integer init = (Integer)ClusterBackend.initExecutionCounts.
		get(Layout.getNode(Layout.getTile(filter)));
	
	    if (init != null) 
		initFire = init.intValue();
	} else {
	    // otherwise, we should be doing this only for work
	    // estimation--check that the filter is the only thing in the graph
	    assert filter.getParent()==null ||
                filter.getParent().size()==1 &&
                filter.getParent().getParent()==null:
                "Found null pointer where unexpected.";
	}
	
	//if this is not a twostage, fake it by adding to initFire,
	//so we always think the preWork is called
	if (!(filter instanceof SIRTwoStageFilter))
	    initFire++;

	//the number of items produced by the upstream filter in
	//initialization
	int upStreamItems = 0;
	
	FlatNode node = Layout.getNode(Layout.getTile(filter));
	FlatNode previous = null;
	
	if (node.inputs > 0) {
	    previous = node.incoming[0];
	    //okay the number of items produced by the upstream splitter may not 
	    //equal the number of items that produced by the filter feeding the splitter
	    //now, since I do not map splitter, this discrepancy must be accounted for.  
	    //We do not have an incoming buffer for ths splitter, so the data must
	    //trickle down to the filter(s) that the splitter feeds
	    if (previous.contents instanceof SIRSplitter) {
		upStreamItems = getPrevSplitterUpStreamItems(previous, node);
	    }
	    else {
		//upstream not a splitter, just get the number of executions
		upStreamItems = getUpStreamItems(ClusterBackend.initExecutionCounts,
						 node);
	    }
	}
	    
	//see my thesis for an explanation of this calculation
	if (initFire  - 1 > 0) {
	    bottomPeek = Math.max(0, 
				  peek - (prePeek - prePop));
	}
	else
	    bottomPeek = 0;
	
	remaining = upStreamItems -
	    (prePeek + 
	     bottomPeek + 
	     Math.max((initFire - 2), 0) * pop);
	
	//System.out.println(remaining);
    }
    
    // node is not directly connected upstream to a splitter, this function
    //   will calculate the number of items send to node 
    private int getUpStreamItems(HashMap executionCounts, FlatNode node) 
    {
	//if the node has not incoming channels then just return 0
	if (node.inputs < 1)
	    return 0;
	
	FlatNode previous = node.incoming[0];
	
	int prevPush = 0;
	
	//get the number of times the previous node executes in the 
	//schedule
	int prevInitCount = Util.getCountPrev(executionCounts, 
					      previous, node);
	
	//get the push rate for the previous
	if (prevInitCount > 0) {
	    if (previous.contents instanceof SIRSplitter || 
		previous.contents instanceof SIRJoiner) {
		prevPush = 1;
	    }
	    else
		prevPush = ((SIRFilter)previous.contents).getPushInt();
	}
	
	//push * executions
	int upStreamItems = (prevInitCount * prevPush);

	//System.out.println("previous: " + previous.getName());
	//System.out.println("prev Push: " + prevPush + " prev init: " + prevInitCount);

	//if the previous node is a two stage filter then count its initWork
	//in the initialItemsTo Receive
	if (previous != null && previous.contents instanceof SIRTwoStageFilter) {
	    upStreamItems -= ((SIRTwoStageFilter)previous.contents).getPushInt();
	    upStreamItems += ((SIRTwoStageFilter)previous.contents).getInitPush();
	}

	return upStreamItems;
    }


    //
    // If the filter's upstream neighbor is a splitter we may have a problem 
    // where the filter feeding the splitter produces more data than the splitter
    // passes on in the init stage.  So we need to recognize this and forward the data
    // on to the filter(s) that the splitter feeds.  Remember, we do not map splitters
    // so the data on the splitters input tape after the init stage is over must go somewhere.
    
    private int getPrevSplitterUpStreamItems(FlatNode prev, FlatNode node) 
    {
	double roundRobinMult = 1.0;
		
	//there is nothing feeding this splitter, so just return 0
	if (prev.inputs < 1) 
	    return 0;
	
	//follow the channels backward until we get to a filter or joiner,
	//remembering the weights on the rr splitters that connect
	//the filter (joiner) to node, so we know the number of items passed to <node>
	FlatNode current = prev;
	FlatNode downstream = node;
	while (current.contents instanceof SIRSplitter) {
	    if (!(((SIRSplitter)current.contents).getType() == SIRSplitType.DUPLICATE))
		roundRobinMult *= Util.getRRSplitterWeight(current, downstream);
	    if (current.inputs < 1)
		return 0;
	    downstream = current;
	    current = current.incoming[0];
	}
	
	//now current must be a joiner or filter
	//get the number of item current produces
	int currentUpStreamItems = getUpStreamItems(ClusterBackend.initExecutionCounts,
						 current.edges[0]);

	//return the number of items passed from current to node thru the splitters
	//(some may be roundrobin so we must use the weight multiplier.
	return (int)(currentUpStreamItems * roundRobinMult);
    }
        

	*/
}


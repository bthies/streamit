package at.dms.kjc.raw;

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

public class RawExecutionCodeNoPop extends at.dms.util.Utils 
    implements FlatVisitor, Constants
{
    /*** fields for the var names we introduce ***/
    public static String recvBuffer = "__RECVBUFFER__";
    public static String recvBufferSize = "__RECVBUFFERSIZE__";
    public static String recvBufferBits = "__RECVBUFFERBITS__";
    
    //recvBufferIndex points to the beginning of the tape
    public static String recvBufferIndex = "__RECVBUFFERINDEX__";
    //recvIndex points to the end of the tape
    public static String recvIndex = "_RECVINDEX__";

    
    public static String exeIndex = "__EXEINDEX__";
    public static String exeIndex1 = "__EXEINDEX__1__";

    public static String ARRAY_INDEX = "__ARRAY_INDEX__";
    public static String ARRAY_COPY = "__ARRAY_COPY__";

    public static String initSchedFunction = "__RAWINITSCHED__";
    public static String steadySchedFunction = "__RAWSTEADYSCHED__";
    
    public static String receiveMethod = "static_receive_to_mem";

    public static String rawMain = "__RAWMAIN__";
    

    //These next fields are set by calculateItems()
    //see my thesis for a better explanation
    //number of items to receive between preWork() and work()
    private int bottomPeek = 0; 
    //number of times the filter fires in the init schedule
    private int initFire = 0;
    //number of items to receive after initialization
    private int remaining = 0;
    

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
    }
    
    
    public static void doit(FlatNode top) 
    {
	top.accept((new RawExecutionCodeNoPop()), null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    SIRFilter filter = (SIRFilter)node.contents;
	    //Skip Identities now
	    if(filter instanceof SIRIdentity ||
	       filter instanceof SIRFileWriter ||
	       filter instanceof SIRFileReader)
		return;
	    
	    System.out.print("Generating Raw Code: " + 
			     node.contents.getName() + " ");
	    
	    LocalVariables localVariables = new LocalVariables();
	    
	    JBlock block = new JBlock(null, new JStatement[0], null);
	    calculateItems(filter);
	    
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
	    
	    rawMainFunction(node, block, localVariables);
	    
	    //create the method and add it to the filter
	    JMethodDeclaration rawMainFunct = 
		new JMethodDeclaration(null, 
				       at.dms.kjc.Constants.ACC_PUBLIC,
				       CStdType.Void,
				       rawMain,
				       JFormalParameter.EMPTY,
				       CClassType.EMPTY,
				       block,
				       null,
				       null);
	    filter.addMethod(rawMainFunct);
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
	
	Integer init = (Integer)RawBackend.initExecutionCounts.
	    get(Layout.getNode(Layout.getTile(filter)));
	
	if (init != null) 
	    initFire = init.intValue();
	
	//if this is not a twostage, fake it by adding to initFire,
	//so we always think the preWork is called
	if (!(filter instanceof SIRTwoStageFilter))
	    initFire++;

	//the number of items produced by the upstream filter in
	//initialization
	int upStreamItems = 0;
	//number of times the previous node fires in init
	int prevInitCount = 0;
	//its push rate
	int prevPush = 0;
	
	FlatNode node = Layout.getNode(Layout.getTile(filter));
	FlatNode previous = null;
	
	if (node.inputs > 0) {
	    previous = node.incoming[0];
	    prevInitCount = Util.getCountPrev(RawBackend.initExecutionCounts, 
					 previous, node);
	    if (prevInitCount > 0) {
		if (previous.contents instanceof SIRSplitter || 
		    previous.contents instanceof SIRJoiner) {
		    prevPush = 1;
		}
		else
		    prevPush = ((SIRFilter)previous.contents).getPushInt();
	    }
	    //System.out.println("previous: " + previous.getName());
	    //System.out.println("prev Push: " + prevPush + " prev init: " + prevInitCount);
	}
	
	//calculate the total items produced by the upstream node
	upStreamItems = (prevInitCount * prevPush);
	//if the previous node is a two stage filter then count its initWork
	//in the initialItemsTo Receive
	if (previous != null && previous.contents instanceof SIRTwoStageFilter) {
	    upStreamItems -= ((SIRTwoStageFilter)previous.contents).getPushInt();
	    upStreamItems += ((SIRTwoStageFilter)previous.contents).getInitPush();
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
	
    }
    

    //returns the expression that will create the buffer array.  A JNewArrayExpression
    //with the proper type, dimensions, and size...
    private JExpression bufferInitExp(SIRFilter filter, CType inputType,
				      int buffersize) 
    {
	if (inputType.isArrayType()) {
	    CType baseType = ((CArrayType)inputType).getBaseType();
	    //create the array to hold the dims of the buffer
	    JExpression baseTypeDims[] = ((CArrayType)inputType).getDims();
	    JExpression[] dims =  new JExpression[baseTypeDims.length + 1];
	    //the first dim is the buffersize
	    dims[0] = new JIntLiteral(buffersize);
	    //copy the dims for the basetype
	    for (int i = 1; i < dims.length; i++)
		dims[i] = baseTypeDims[i];
	    
	    return new JNewArrayExpression(null, baseType, dims, null);
	}
	else if (inputType.isClassType()) {
	    Utils.fail("Structures not implemented");
	}

	//else basetype, create the array
	JExpression dims[] = {new JIntLiteral(buffersize)};
	return new JNewArrayExpression(null, inputType, dims, null);
	
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
	    int buffersize;
	    
	    if (isSimple(filter)) {
		int prepeek = 0;
		if (filter instanceof SIRTwoStageFilter) 
		    prepeek = ((SIRTwoStageFilter)filter).getInitPeek();
		
		buffersize = Math.max(prepeek, filter.getPeekInt());

	    }
	    else
		buffersize = CalcBufferSize.getConsBufSize(Layout.getNode(Layout.getTile(filter)));
	    
	    
	    //the receive buffer
	    JVariableDefinition recvBufVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					new CArrayType(filter.getInputType(), 
						       1 /* dimension */ ),
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
    

    //convert the peek and pop expressions for a filter into
    //buffer accesses, do this for all functions just in case helper
    //functions call peek or pop
    private void convertCommExps(SIRFilter filter, boolean simple,
				 LocalVariables localVars) 
    {
	SLIRReplacingVisitor convert;
	
	if (simple) {
	    convert = 
		new ConvertCommunicationSimpleNoPop(localVars);
	}
	else
	    convert = new ConvertCommunicationNoPop(localVars);
	
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

    private boolean noBuffer(SIRFilter filter) 
	{
	    if (filter.getPeekInt() == 0 &&
		(!(filter instanceof SIRTwoStageFilter) ||
		 (((SIRTwoStageFilter)filter).getInitPeek() == 0)))
		return true;
	    return false;		
	}
    
		    
    // private void defineLocalVars(JBlock block,
// 				 LocalVariables localVariables
    

    private void rawMainFunction(FlatNode node, JBlock statements,
				 LocalVariables localVariables) 
    {
	SIRFilter filter = (SIRFilter)node.contents;
	
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
		(makeForLoop(receiveCode(filter, localVariables),
			     localVariables.exeIndex,
			     new JIntLiteral(two.getInitPeek())));
	    
	    //now inline the init work body
	    statements.addStatement(body);
	    
	    //since we are removing all pops, we must increment the
	    //buffer index by the pop amount
	    if (!isSimple(filter) && !noBuffer(filter)) {
		JAddExpression addPop = 
		    new JAddExpression(null,
				       new JLocalVariableExpression
					   (null, 
					    localVariables.recvBufferIndex),
				       new JIntLiteral(two.getInitPop()));
		
		JParenthesedExpression exp = new JParenthesedExpression(null, addPop);

		JBitwiseExpression mask = 
		    new JBitwiseExpression(null, 
					   OPE_BAND,
					   exp, 
					   new JLocalVariableExpression
					       (null,
						localVariables.recvBufferBits));

		JAssignmentExpression assignment = new
		    JAssignmentExpression(null, 
					  new JLocalVariableExpression
					  (null,
					   localVariables.recvBufferIndex),
					  mask);
		statements.addStatement
		    (new JExpressionStatement(null, assignment, null));
	    }
	    
		
		
	}	
	    
	if (initFire - 1 > 0) {
	//add the code to collect enough data necessary to fire the 
	//work function for the first time
	    
	    if (bottomPeek > 0) {
		statements.addStatement
		    (makeForLoop(receiveCode(filter, localVariables),
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
		(makeForLoop(receiveCode(filter, localVariables),
			     localVariables.exeIndex,
			     new JIntLiteral(remaining))); 
	}

	//add the call to the work function
	statements.addStatement(generateSteadyStateLoop(filter,
							localVariables));
	
	
    }

    //generate the loop for the work function firings in the 
    //initialization schedule
    JStatement generateInitWorkLoop(SIRFilter filter, 
				    LocalVariables localVariables) 
    {
	JStatement innerReceiveLoop = 
	    makeForLoop(receiveCode(filter, localVariables),
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
	
	//add the if statement
	block.addStatement(ifStatement);
	
	//clone the work function and inline it
	JBlock workBlock = 
	    (JBlock)ObjectDeepCloner.deepCopy(filter.getWork().getBody());
	block.addStatement(workBlock);
	
	if (!isSimple(filter) && !noBuffer(filter)) {
	    JAddExpression addPop = 
		new JAddExpression(null,
				   new JLocalVariableExpression
				       (null, 
					localVariables.recvBufferIndex),
				       new JIntLiteral(filter.getPopInt()));
	    
	    JParenthesedExpression exp = new JParenthesedExpression(null, addPop);
	    
	    JBitwiseExpression mask = 
		new JBitwiseExpression(null, 
				       OPE_BAND,
				       exp, 
				       new JLocalVariableExpression
					   (null,
					    localVariables.recvBufferBits));
	    
	    JAssignmentExpression assignment = new
		JAssignmentExpression(null, 
				      new JLocalVariableExpression
					  (null,
					   localVariables.recvBufferIndex),
				      mask);
	    
	    block.addStatement
		(new JExpressionStatement(null, assignment, null));
	}
	
	
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


    //generate the code for the steady state loop
    JStatement generateSteadyStateLoop(SIRFilter filter, 
				       LocalVariables localVariables) 
    {
	int unrollFactor = 1;
	
	//if this filter has a buffer, 
	//the unroll factor is equal to the lcm of pop
	//and the buffersize
	if (filter.getPeekInt() > 0) {
	    unrollFactor = 
		lcm(filter.getPopInt(),
		    CalcBufferSize.getConsBufSize
		    (Layout.getNode(Layout.getTile(filter))));
	}
	
	JBlock block = new JBlock(null, new JStatement[0], null);

	//clone and inline the work function
	//	for (int i = 0; i < unrollFactor; i++) {	
 
	//add the statements to receive pop items into the buffer
	block.addStatement
	    (makeForLoop(receiveCode(filter, localVariables),
			 localVariables.exeIndex,
			 new JIntLiteral(filter.getPopInt())));

	

	    JBlock workBlock = 
		(JBlock)ObjectDeepCloner.
		deepCopy(filter.getWork().getBody());
	    block.addStatement(workBlock);

	    
	    if (!isSimple(filter) && !noBuffer(filter)) {
		JAddExpression addPop = 
		    new JAddExpression(null,
				       new JLocalVariableExpression
					   (null, 
					    localVariables.recvBufferIndex),
				       new JIntLiteral(filter.getPopInt()));
		
		JParenthesedExpression exp = new JParenthesedExpression(null, addPop);
		
		JBitwiseExpression mask = 
		    new JBitwiseExpression(null, 
					   OPE_BAND,
					   exp, 
					   new JLocalVariableExpression
					       (null,
						localVariables.recvBufferBits));
	
		JAssignmentExpression assignment = new
		    JAssignmentExpression(null, 
					  new JLocalVariableExpression
					      (null,
					       localVariables.recvBufferIndex),
					  mask);
		
		block.addStatement
		    (new JExpressionStatement(null, assignment, null));
		//we must also wrab the receiving index in the 
		//buffer
		JAssignmentExpression assignment2 = new 
		    JAssignmentExpression(null,
					  new JLocalVariableExpression
					      (null, 
					       localVariables.recvIndex),
					  new JBitwiseExpression
					      (null,
					       OPE_BAND,
					       new JLocalVariableExpression
						   (null,
						    localVariables.recvIndex),
					       new JLocalVariableExpression
						   (null,
						    localVariables.recvBufferBits)));
		
		block.addStatement
		 (new JExpressionStatement(null, assignment2, null));
	    }
	    
	    //	}
		
	    //	return block;
	
	//return the infinite loop
	return new JWhileStatement(null, 
				   new JBooleanLiteral(null, true),
				   block, 
				   null);
    }

    //returns the code to receive one item into the buffer
    //uses the correct variables
    JStatement receiveCode(SIRFilter filter, LocalVariables localVariables) {
	JBlock statements = new JBlock(null, new JStatement[0], null);

	if (noBuffer(filter)) 
	    return null;
	
	if (filter.getInputType().isArrayType()) {
	    //NOT IMPLEMENTED YET      !!!!!!!!!!!!!!!!!!!!!!!!!!!
	    return null;
	}
	else if (filter.getInputType().isClassType()) {
	    //NOT IMPLEMENTED YET      !!!!!!!!!!!!!!!!!!!!!!!!!!!
	    return null;
	    
	} 
	else if (!filter.getInputType().equals(CStdType.Void)) {
	    //we want to create a statement of the form:
	    //static_receive_to_mem((void*)&(recvBuffer[(++recvIndex) &
	    //                                          recvBufferSize]));

	    //the cast to (void*) and the '&' are added by the FlatIRToC pass

	  
	    //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression
					   (null,
					    localVariables.recvBuffer),
					   bufferIndex(filter,
						       localVariables));
	    
	    JExpression[] arg = 
		{new JParenthesedExpression(null,
					    bufferAccess)};
	    
	    //create the method call expression
	    JMethodCallExpression exp =
		new JMethodCallExpression(null,  new JThisExpression(null),
					  receiveMethod,
					  arg);

	    //return the method call
	    return new JExpressionStatement(null, exp, null);
	}
	return null;
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
	    /*
	    //create the modulo expression
	    JModuloExpression indexMod = 
		new JModuloExpression(null, bufferIncrement, 
				  new JLocalVariableExpression
				  (null,
				   localVariables.recvBufferSize));
	    */
	    
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
    
    
    /**
     * Returns a for loop that uses field <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns empty (!not legal in the general case)
     */
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
  
    class ConvertCommunicationSimpleNoPop extends SLIRReplacingVisitor 
    {
	LocalVariables localVariables;
	
	
	public ConvertCommunicationSimpleNoPop(LocalVariables locals) 
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

	    return new JEmptyStatement(null, null);
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

	      //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JLocalVariableExpression
					   (null,
					    localVariables.recvBuffer),
					   self.getArg());

	    //return the parenthesed expression
	    return new JParenthesedExpression(null,
					      bufferAccess);
	    
	}
	
    }
    
    

    class ConvertCommunicationNoPop extends SLIRReplacingVisitor 
    {
	LocalVariables localVariables;
	
	
	public ConvertCommunicationNoPop(LocalVariables locals) 
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

	    fail("with the poptopeek option we should see no " +
		 "pops in the raw backend");
	    
	    return null;
	    //	    return new JEmptyExpression(null, null);
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
	 
	    /*
	    //create the mod expression
	    JModuloExpression indexMod = 
		new JModuloExpression(null, index,
				      new JLocalVariableExpression
				      (null,
				       localVariables.recvBufferSize));
	    */

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
}



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

public class RawExecutionCode extends at.dms.util.Utils 
    implements FlatVisitor, Constants
{
    /*** fields for the var names we introduce ***/
    public static String recvBuffer = "__RECVBUFFER__";
    public static String recvBufferSize = "__RECVBUFFERSIZE__";
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

    //These next fields are set by calculateItems()
    //see my thesis for a better explanation
    //number of items to receive between preWork() and work()
    private int bottomPeek = 0; 
    //number of times the filter fires in the init schedule
    private int initFire = 0;
    //number of items to receive after initialization
    private int remaining = 0;
    
    
    public static void doit(FlatNode top) 
    {
	top.accept((new RawExecutionCode()), null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    createFields(node);
	    convertCommExps((SIRFilter)node.contents);
	    calculateItems((SIRFilter)node.contents);
	    rawInitSchedFunction(node);
	    rawSteadySchedFunction(node);
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
    private JExpression bufferInitExp(SIRFilter filter, CType inputType) 
    {
	int buffersize = 
	    CalcBufferSize.getConsBufSize(Layout.getNode(Layout.getTile(filter)));

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
    

    private void createFields(FlatNode node)
    {
	SIRFilter filter = (SIRFilter)node.contents;

	int buffersize = 
	    CalcBufferSize.getConsBufSize(Layout.getNode(Layout.getTile(filter)));

	//index variable for certain for loops
	JVariableDefinition exeIndexVar = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex,
				    null);
	filter.addField(new JFieldDeclaration(null, 
					      exeIndexVar,
					      null, null));

	//index variable for certain for loops
	JVariableDefinition exeIndex1Var = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex1,
				    null);
	
	filter.addField(new JFieldDeclaration(null, 
					      exeIndex1Var,
					      null, null));
	
	//only add the receive buffer and its vars if the 
	//filter receives data
	if (filter.getPeekInt() > 0) {
	    //the receive buffer
	    JVariableDefinition recvBufVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					new CArrayType(filter.getInputType(), 
						       1 /* dimension */ ),
					recvBuffer,
					bufferInitExp(filter, filter.getInputType()));
	    
	    
	    //the size of the buffer 
	    JVariableDefinition recvBufferSizeVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					CStdType.Integer,
					recvBufferSize,
					new JIntLiteral(buffersize));
	    
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

	    JFieldDeclaration[] fields = 
		{new JFieldDeclaration(null, recvBufVar, null, null),
		 new JFieldDeclaration(null, recvBufferSizeVar, null, null),
		 new JFieldDeclaration(null, recvBufferIndexVar, null, null),
		 new JFieldDeclaration(null, recvIndexVar, null, null),
		};
	    

	    filter.addFields(fields);
	    
	    //print the declarations for the array indices for pushing and popping
	    //if this filter deals with arrays
	    if (filter.getInputType().isArrayType() || 
		filter.getOutputType().isArrayType()) {
		int inputDim = 0, outputDim = 0, maxDim;
		//find which array has the greatest dimensionality	   
		if (filter.getInputType().isArrayType())
		    inputDim = ((CArrayType)filter.getInputType()).getArrayBound();
		if (filter.getOutputType().isArrayType()) 
		    outputDim = ((CArrayType)filter.getOutputType()).getArrayBound();
		maxDim = (inputDim > outputDim) ? inputDim : outputDim;
		//create enough index vars as max dim
		for (int i = 0; i < maxDim; i++) {
		    JVariableDefinition arrayIndexVar = 
			new JVariableDefinition(null, 
						0, 
						CStdType.Integer,
						ARRAY_INDEX + i,
						null);
		    filter.addField(new JFieldDeclaration(null, 
							  arrayIndexVar,
							  null, null));
		}
	    }
	}
    }

    //convert the peek and pop expressions for a filter into
    //buffer accesses, do this for all functions just in case helper
    //functions call peek or pop
    private void convertCommExps(SIRFilter filter) 
    {
	ConvertCommunication convert = new ConvertCommunication();
	
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

    private void rawInitSchedFunction(FlatNode node) 
    {
	SIRFilter filter = (SIRFilter)node.contents;
	
	JBlock statements = new JBlock(null, new JStatement[0], null);
	
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
		(makeForLoop(receiveCode(filter),
			     exeIndex,
			     new JIntLiteral(two.getInitPeek())));
	    		
	    //now inline the init work body
	    statements.addStatement(body);
	}
	
	if (initFire - 1 > 0) {
	//add the code to collect enough data necessary to fire the 
	//work function for the first time
	    
	    if (bottomPeek > 0) {
		statements.addStatement
		    (makeForLoop(receiveCode(filter),
				 exeIndex,
				 new JIntLiteral(bottomPeek)));
	    }
	    
	    //add the calls for the work function in the initialization stage
	    statements.addStatement(generateInitWorkLoop(filter));
	}

	//add the code to collect all data produced by the upstream filter 
	//but not consumed by this filter in the initialization stage
	if (remaining > 0) {
	   statements.addStatement
		(makeForLoop(receiveCode(filter),
			     exeIndex,
			     new JIntLiteral(remaining))); 
	}
	//create the method and add it to the filter
	JMethodDeclaration rawInitSchedFunction = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   initSchedFunction,
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   statements,
				   null,
				   null);
	filter.addMethod(rawInitSchedFunction);
    }
    
    private void rawSteadySchedFunction(FlatNode node) 
    {
	SIRFilter filter = (SIRFilter)node.contents;
	
	JBlock statements = new JBlock(null, new JStatement[0], null);
	//add the call to the work function
	statements.addStatement(generateSteadyStateLoop(filter));
	
	//create the method and add it to the filter
	JMethodDeclaration rawSteadySchedFunction = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   steadySchedFunction,
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   statements,
				   null,
				   null);
	filter.addMethod(rawSteadySchedFunction);
    }

    //generate the loop for the work function firings in the 
    //initialization schedule
    JStatement generateInitWorkLoop(SIRFilter filter) 
    {
	JStatement innerReceiveLoop = 
	    makeForLoop(receiveCode(filter),
			exeIndex1,
			new JIntLiteral(filter.getPopInt()));
	JExpression isFirst = 
	    new JEqualityExpression(null,
				    false,
				    new JFieldAccessExpression
				    (null, 
				     new JThisExpression(null),
				     exeIndex),
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
	
	//return the for loop that executes the block init - 1
	//times
	return makeForLoop(block, exeIndex, 
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
    JStatement generateSteadyStateLoop(SIRFilter filter) 
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
	
	//add the statements to receive pop items into the buffer
	block.addStatement
	    (makeForLoop(receiveCode(filter),
			 exeIndex,
			 new JIntLiteral(filter.getPopInt())));
	
	//clone and inline the work function
	for (int i = 0; i < unrollFactor; i++) {
	    JBlock workBlock = 
		(JBlock)ObjectDeepCloner.
		deepCopy(filter.getWork().getBody());
	    block.addStatement(workBlock);
	}
	//return the infinite loop
	return new JWhileStatement(null, 
				   new JBooleanLiteral(null, true),
				   block, 
				   null);
    }

    //returns the code to receive one item into the buffer
    //uses the correct variables
    JStatement receiveCode(SIRFilter filter) {
	JBlock statements = new JBlock(null, new JStatement[0], null);
	
	if (filter.getInputType().isArrayType()) {
	    //NOT IMPLEMENTED YET      !!!!!!!!!!!!!!!!!!!!!!!!!!!
	    return null;
	}
	else if (filter.getInputType().isClassType()) {
	    //NOT IMPLEMENTED YET      !!!!!!!!!!!!!!!!!!!!!!!!!!!
	    return null;
	    
	} 
	else {
	    //we want to create a statement of the form:
	    //static_receive_to_mem((void*)&(recvBuffer[(++recvIndex) &
	    //                                          recvBufferSize]));

	    //the cast to (void*) and the '&' are added by the FlatIRToC pass

	     //create the increment of the index var
	    JPrefixExpression bufferIncrement = 
		new JPrefixExpression(null, 
				      OPE_PREINC,
				      new JFieldAccessExpression
				      (null, 
				       new JThisExpression(null),
				       RawExecutionCode.recvIndex));
	    //create the modulo expression
	    JModuloExpression indexMod = 
		new JModuloExpression(null, bufferIncrement, 
				  new JFieldAccessExpression
				  (null,
				   new JThisExpression(null),
				   RawExecutionCode.recvBufferSize));
	    //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JFieldAccessExpression
					   (null,
					    new JThisExpression(null),
					    RawExecutionCode.recvBuffer),
					   indexMod);
	    
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
    }
    
    /**
     * Returns a for loop that uses field <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns the initial assignment
     */
    private static JStatement makeForLoop(JStatement body,
					  String var,
					  JExpression count) {
	// make init statement - assign zero to <var>.  We need to use
	// an expression list statement to follow the convention of
	// other for loops and to get the codegen right.
	JExpression initExpr[] = {
	    new JAssignmentExpression(null,
				      new JFieldAccessExpression
				      (null, 
				       new JThisExpression(null),var),
				      new JIntLiteral(0)) };
	JStatement init = new JExpressionListStatement(null, initExpr, null);
	// if count==0, just return init statement
	if (count instanceof JIntLiteral) {
	    int intCount = ((JIntLiteral)count).intValue();
	    if (intCount<=0) {
		// return assignment statement
		return init;
	    }
	}
	// make conditional - test if <var> less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JFieldAccessExpression
				      (null, 
				       new JThisExpression(null),
				       var),
				      count);
	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JFieldAccessExpression
				   (null, 
				    new JThisExpression(null),
				    var));
	JStatement incr = 
	    new JExpressionStatement(null, incrExpr, null);

	return new JForStatement(null, init, cond, incr, body, null);
    }
  
    class ConvertCommunication extends SLIRReplacingVisitor 
    {
	public ConvertCommunication() 
	{
	}
	
	//for pop expressions convert to the form
	// (recvBuffer[++recvBufferIndex % recvBufferSize])
	public Object visitPopExpression(SIRPopExpression self,
					 CType tapeType) {
	    
	    //the cfields corresponding to the fields used in this
	    //conversion
	    CSourceField recvBufferIndexField = 
		new CSourceField(null, 
			   0, 
			   RawExecutionCode.recvBufferIndex,
			   CStdType.Integer,
			   false);
	    CSourceField recvBufferField = 
		new CSourceField(null, 
			   0, 
			   RawExecutionCode.recvBuffer,
			   tapeType,
			   false);
	     CSourceField recvBufferSizeField = 
		new CSourceField(null, 
			   0, 
			   RawExecutionCode.recvBufferSize,
			   CStdType.Integer,
			   false);

	    //create the increment of the index var
	    JPrefixExpression bufferIncrement = 
		new JPrefixExpression(null, 
				      OPE_PREINC,
				      new JFieldAccessExpression
				      (null, 
				       new JThisExpression(null),
				       RawExecutionCode.recvBufferIndex,
				       recvBufferIndexField));
	    //create the modulo expression
	    JModuloExpression indexMod = 
		new JModuloExpression(null, bufferIncrement, 
				  new JFieldAccessExpression
				  (null,
				   new JThisExpression(null),
				   RawExecutionCode.recvBufferSize,
				   recvBufferSizeField));
	    //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JFieldAccessExpression
					   (null,
					    new JThisExpression(null),
					    RawExecutionCode.recvBuffer,
					    recvBufferField),
					   indexMod);

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
	    
	    //the cfields corresponding to the fields used in this
	    //conversion
	    CSourceField recvBufferIndexField = 
		new CSourceField(null, 
			   0, 
			   RawExecutionCode.recvBufferIndex,
			   CStdType.Integer,
			   false);
	    CSourceField recvBufferField = 
		new CSourceField(null, 
			   0, 
			   RawExecutionCode.recvBuffer,
			   self.getType(),
			   false);
	     CSourceField recvBufferSizeField = 
		new CSourceField(null, 
			   0, 
			   RawExecutionCode.recvBufferSize,
			   CStdType.Integer,
			   false);

	    //create the index calculation expression
	    JAddExpression argIncrement = 
		new JAddExpression(null, self.getArg(), new JIntLiteral(1));
	    JAddExpression index = 
		new JAddExpression(null,
				   new JFieldAccessExpression
				   (null, 
				    new JThisExpression(null),
				    RawExecutionCode.recvBufferIndex,
				    recvBufferIndexField),
				   argIncrement);
	    //create the mod expression
	    JModuloExpression indexMod = 
		new JModuloExpression(null, index,
				      new JFieldAccessExpression
				      (null,
				       new JThisExpression(null),
				       RawExecutionCode.recvBufferSize,
				       recvBufferSizeField));      
	    //create the array access expression
	    JArrayAccessExpression bufferAccess = 
		new JArrayAccessExpression(null,
					   new JFieldAccessExpression
					   (null,
					    new JThisExpression(null),
					    RawExecutionCode.recvBuffer,
					    recvBufferField),
					   indexMod);

	    //return the parenthesed expression
	    return new JParenthesedExpression(null,
					      bufferAccess);
	}
	
    }
}



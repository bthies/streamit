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

public class RawExecutionCode extends at.dms.util.Utils 
    implements FlatVisitor 
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

    public static String rawMain = "__RAWMAIN__";
    
    
    public static void doit(FlatNode top) 
    {
	top.accept((new RawExecutionCode()), null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    createFields(node);
	    convertCommExps((SIRFilter)node.contents);
	    createRawMainFunc(node);
	}
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
	    
	   
	    //index variable for certain for loops
	    JVariableDefinition exeIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					exeIndex,
					null);

	    //index variable for certain for loops
	    JVariableDefinition exeIndex1Var = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					exeIndex1,
					null);

	    JFieldDeclaration[] fields = 
		{new JFieldDeclaration(null, recvBufVar, null, null),
		 new JFieldDeclaration(null, recvBufferSizeVar, null, null),
		 new JFieldDeclaration(null, recvBufferIndexVar, null, null),
		 new JFieldDeclaration(null, recvIndexVar, null, null),
		 new JFieldDeclaration(null, exeIndexVar, null, null),
		 new JFieldDeclaration(null, exeIndex1Var, null, null)
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

    private void createRawMainFunc(FlatNode node) 
    {
	//initializeFields();
	//createInitializationCode();
	//createSteadyStateCode();
	SIRFilter filter = (SIRFilter)node.contents;
	
	JBlock statements = new JBlock(null, new JStatement[0], null);
	
	//add the call to the init function
	statements.addStatement
	    (new 
	     JExpressionStatement(null,
				  new JMethodCallExpression
				  (null,
				   new JThisExpression(null),
				   filter.getInit().getName(),
				   (JExpression[])filter.getParams().
				   toArray(new JExpression[1])),
				  null));
	
	//add the call to initWork
	if (filter instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
	    JBlock body = 
		(JBlock)ObjectDeepCloner.deepCopy
		(two.getInitWork().getBody());
	    
	    statements.addStatement(recExeCode(two.getInitPeek(),
						   body));			
	}
	
	//add the code to collect enough data necessary to fire the 
	//work function for the first time

	//add the calls for the work function in the initialization stage

	//add the code to collect all data produced by the upstream filter 
	//but not consumed by this filter in the initialization stage

	//add the call to the work function
	

	//create the method and add it to the filter
	JMethodDeclaration rawMainMethod = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   rawMain,
				   JFormalParameter.EMPTY,
				   CClassType.EMPTY,
				   statements,
				   null,
				   null);
	filter.addMethod(rawMainMethod);
    }

    //return a list of  statements to receive <rec> items and then inline the
    //JBlock
    JStatement recExeCode(int rec, JBlock block) 
    {
	//generate the receive code
	if (rec > 0) {
	    return makeForLoop(block, exeIndex, new JIntLiteral(rec));
	}
	else {	    
	    //do not generate the receive code
	    //simply return the block
	    return block;
	}   
    }


    /**
     * Returns a for loop that uses field <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns the initial assignment statement.
     */
    private static JStatement makeForLoop(JStatement body,
					  String var,
					  JExpression count) {
	// make init statement - assign zero to <var>.  We need to use
	// an expression list statement to follow the convention of
	// other for loops and to get the codegen right.
	JExpression initExpr[] = {
	    new JAssignmentExpression(null,
				      new JFieldAccessExpression(null, var),
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
				      new JFieldAccessExpression(null, var),
				      count);
	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JFieldAccessExpression(null, var));
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



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

public class RawExecutionCode extends at.dms.util.Utils 
    implements Constants
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

    public static String rawMain = "__RAWMAIN__";
    
    //keep a unique integer for each filter in each trace
    //so var names do not clash
    private static int globalID = 0;
    private int uniqueID;

    private LocalVariables localVariables;
    private FilterInfo filterInfo;

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


    public RawExecutionCode(FilterInfo filterInfo) 
    {
	//set the unique id to append to each variable name
	uniqueID = globalID++;
	localVariables = new LocalVariables();
	this.filterInfo = filterInfo;
    }

    public JFieldDeclaration[] getVarDecls() 
    {
	Vector decls = new Vector();
	SIRFilter filter = filterInfo.filter;
	
	//index variable for certain for loops
	JVariableDefinition exeIndexVar = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex + uniqueID,
				    null);

	//remember the JVarDef for latter (in the raw main function)
	localVariables.exeIndex = exeIndexVar;
	decls.add(new JFieldDeclaration(null, exeIndexVar, null, null));
	
	//index variable for certain for loops
	JVariableDefinition exeIndex1Var = 
	    new JVariableDefinition(null, 
				    0, 
				    CStdType.Integer,
				    exeIndex1 + uniqueID,
				    null);

	localVariables.exeIndex1 = exeIndex1Var;
	decls.add(new JFieldDeclaration(null, exeIndex1Var, null, null));
	
	//only add the receive buffer and its vars if the 
	//filter receives data
	if (!noBuffer()) {
	    //initialize the buffersize to be the size of the 
	    //struct being passed over it
	    int buffersize;
	    
	    int maxpeek = filterInfo.peek;
	    //set up the maxpeek
	    maxpeek = (filterInfo.prePeek > maxpeek) ? filterInfo.prePeek : maxpeek;
	    
	    
	    if (isSimple()) {
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
		    buffersize = maxpeek;

		//define the simple index variable
		JVariableDefinition simpleIndexVar = 
		    new JVariableDefinition(null, 
					    0, 
					    CStdType.Integer,
					    simpleIndex + uniqueID,
					    new JIntLiteral(-1));
		
		//remember the JVarDef for latter (in the raw main function)
		localVariables.simpleIndex = simpleIndexVar;
		decls.add(new JFieldDeclaration(null, simpleIndexVar, null, null));
	    }
	    else { //filter with remaing items on the buffer after initialization 
		//see Mgordon's thesis for explanation (Code Generation Section)
		if (KjcOptions.ratematch)
		    buffersize = 
			Util.nextPow2(Math.max((filterInfo.steadyMult - 1) * 
						filterInfo.pop + filterInfo.peek, filterInfo.prePeek)
				      + filterInfo.remaining);
		else
		    buffersize = Util.nextPow2(maxpeek + filterInfo.remaining);
	    }
	    
	    JVariableDefinition recvBufVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					new CArrayType(filter.getInputType(), 
						       1 /* dimension */ ),
					recvBuffer + uniqueID,
					bufferInitExp
					(filter, filter.getInputType(), 
					 buffersize));
	    
	    
	    //the size of the buffer 
	    JVariableDefinition recvBufferSizeVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					CStdType.Integer,
					recvBufferSize + uniqueID,
					new JIntLiteral(buffersize));
	    
	    //the size of the buffer 
	    JVariableDefinition recvBufferBitsVar = 
		new JVariableDefinition(null, 
					at.dms.kjc.Constants.ACC_FINAL, //?????????
					CStdType.Integer,
					recvBufferBits + uniqueID,
					new JIntLiteral(buffersize - 1));
	    
	    //the receive buffer index (start of the buffer)
	    JVariableDefinition recvBufferIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					recvBufferIndex + uniqueID,
					new JIntLiteral(-1));
	    
	    //the index to the end of the receive buffer)
	    JVariableDefinition recvIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					recvIndex + uniqueID,
					new JIntLiteral(-1));

	    localVariables.recvBuffer = recvBufVar;
	    decls.add(new JFieldDeclaration(null, recvBufVar, null, null));
	    localVariables.recvBufferSize = recvBufferSizeVar;
	    decls.add(new JFieldDeclaration(null, recvBufferSizeVar, null, null));
	    localVariables.recvBufferBits = recvBufferBitsVar;
	    decls.add(new JFieldDeclaration(null, recvBufferBitsVar, null, null));
	    localVariables.recvBufferIndex = recvBufferIndexVar;
	    decls.add(new JFieldDeclaration(null, recvBufferIndexVar, null, null));
	    localVariables.recvIndex = recvIndexVar;
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
	    
	    localVariables.sendBufferIndex = sendBufferIndexVar;
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
	    localVariables.sendBuffer = sendBufVar;
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
	    
	    localVariables.ARRAY_INDEX = new JVariableDefinition[maxDim];
	    
	    //create enough index vars as max dim
	    for (int i = 0; i < maxDim; i++) {
		JVariableDefinition arrayIndexVar = 
		    new JVariableDefinition(null, 
					    0, 
					    CStdType.Integer,
					    ARRAY_INDEX + i + uniqueID,
					    null);
		//remember the array index vars
		localVariables.ARRAY_INDEX[i] = arrayIndexVar;
		decls.add(new JFieldDeclaration(null, arrayIndexVar, null, null));
	    }
	}
	return (JFieldDeclaration[])decls.toArray(new JFieldDeclaration[0]);
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

    private boolean noBuffer() 
    {
	//always need a buffer for rate matching.
	//	    if (KjcOptions.ratematch)
	//	return false;
	
	if (filterInfo.peek == 0 &&
	    filterInfo.prePeek == 0)
	    return true;
	return false;		
    }

    private boolean isSimple()
    {
 	if (noBuffer())
 	    return false;
	
 	if (filterInfo.peek == filterInfo.pop &&
 	    filterInfo.remaining == 0 &&
 	    (filterInfo.prePop == filterInfo.prePeek))
 	    return true;
 	return false;
    }
}

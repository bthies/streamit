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
    private static String recvBuffer = "__RECVBUFFER__";
    private static String recvBufferSize = "__RECVBUFFERSIZE__";
    //recvBufferIndex points to the beginning of the tape
    private static String recvBufferIndex = "__RECVBUFFERINDEX__";
    //BUFFER_INDEX points to the end of the tape
    private final String BUFFER_INDEX = "__i__";
    
    private static String exeIndex = "__EXEINDEX__";
    private static String exeIndex1 = "__EXEINDEX__1__";
    private static String recvIndex = "_RECVINDEX__";    
    private static String RECVBITS = "__RECVBITS__";  

    private static String ARRAY_INDEX = "__ARRAY_INDEX__";
    private static String ARRAY_COPY = "__ARRAY_COPY__";
    
    public static void doit(FlatNode top) 
    {
	top.accept((new RawExecutionCode()), null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    createFields(node);
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
	    
	    //the receive buffer index (start of the buffer)
	    JVariableDefinition recvIndexVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					recvBufferIndex,
					new JIntLiteral(-1));
	    
	    //the end of the receive buffer
	    JVariableDefinition BUFFER_INDEXVar = 
		new JVariableDefinition(null, 
					0, 
					CStdType.Integer,
					BUFFER_INDEX,
					null);
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
		 new JFieldDeclaration(null, recvIndexVar, null, null),
		 new JFieldDeclaration(null, BUFFER_INDEXVar, null, null),
		 new JFieldDeclaration(null, exeIndexVar, null, null),
		 new JFieldDeclaration(null, exeIndex1Var, null, null),
		};
	    

	    filter.addFields(fields);
	    
	    //print the declarations for the array indexs for pushing and popping
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
    
    private void createRawMainFunc(FlatNode node) 
    {
	//initializeFields();
	//convertCommunication();
	//createInitializationCode();
	//createSteadyStateCode();
    }
}


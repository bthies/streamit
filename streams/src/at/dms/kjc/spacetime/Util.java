package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.List;
import at.dms.kjc.sir.lowering.*;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.io.*;

/** A class with useful functions that span classes **/
public class Util {
    public static String CSTOINTVAR = "__csto_integer__";
    public static String CSTOFPVAR = "__csto_float__";
    public static String CSTIFPVAR = "__csti_float__";
    public static String CSTIINTVAR = "__csti_integer__";
    public static String CGNOINTVAR = "__slgcc_cgno_integer__";
    
    //unique ID for each file reader/writer used to
    //generate var names...
    private static HashMap fileVarNames;
    private static int fileID = 0;

    static 
    {
	fileVarNames = new HashMap();
    }
    

    public static int nextPow2(int i) {
	String str = Integer.toBinaryString(i);
	if  (str.indexOf('1') == -1)
	    return 0;
	int bit = str.length() - str.indexOf('1');
	int ret = (int)Math.pow(2, bit);
	if (ret == i * 2)
	    return i;
	return ret;
	
    }
    
    /*
      for a given CType return the size (number of elements that need to be sent
      when routing).
    */
    public static int getTypeSize(CType type) {

	if (!(type.isArrayType() || type.isClassType()))
	    return 1;
	else if (type.isArrayType()) {
		int elements = 1;
		int dims[] = Util.makeInt(((CArrayType)type).getDims());
		
		for (int i = 0; i < dims.length; i++) {
		    elements *= dims[i];
		}
		return elements;
	    }
	else if (type.isClassType()) {
	    int size = 0;
	    for (int i = 0; i < type.getCClass().getFields().length; i++) {
		size += getTypeSize(type.getCClass().getFields()[i].getType());
	    }
	    return size;
	}
	Utils.fail("Unrecognized type");
	return 0;
    }

    public static int[] makeInt(JExpression[] dims) {
	int[] ret = new int[dims.length];
	
	for (int i = 0; i < dims.length; i++) {
	    if (!(dims[i] instanceof JIntLiteral))
		Utils.fail("Array length for tape declaration not an int literal");
	    ret[i] = ((JIntLiteral)dims[i]).intValue();
	}
	return ret;
    }

    public static CType getBaseType (CType type) 
    {
	if (type.isArrayType())
	    return ((CArrayType)type).getBaseType();
	return type;
    }

     public static String[] makeString(JExpression[] dims) {
	String[] ret = new String[dims.length];
	
	
	for (int i = 0; i < dims.length; i++) {
	    TraceIRtoC ttoc = new TraceIRtoC();
	    dims[i].accept(ttoc);
	    ret[i] = ttoc.getString();
	}
	return ret;
    }

     public static String staticNetworkReceivePrefix() {
	if(KjcOptions.altcodegen || KjcOptions.decoupled) 
	    return "";
	else 
	    return "/* receive */ asm volatile (\"sw $csti, %0\" : \"=m\" (";
    }

    public static String staticNetworkReceiveSuffix(CType tapeType) {
	if(KjcOptions.altcodegen || KjcOptions.decoupled) {
	    if (tapeType.isFloatingPoint())
		return "= " + CSTIFPVAR + ";";
	    else
		return "= " + CSTIINTVAR + ";";
	}
	else 
	    return "));";
    }

    public static String staticNetworkSendPrefix(CType tapeType) {
	StringBuffer buf = new StringBuffer();
	
	if (KjcOptions.altcodegen || KjcOptions.decoupled) {
	    if (tapeType.isFloatingPoint())
		buf.append(CSTOFPVAR);
	    else
		buf.append(CSTOINTVAR);
	    //temporary fix for type changing filters
	    buf.append(" = (" + tapeType + ")");
	} 
	else {
	    buf.append("(");
	    if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
		if (tapeType.isFloatingPoint()) 
		    buf.append("static_send_print_f");
		else
		    buf.append("static_send_print");
	    }
	    else
		buf.append("static_send");
	    buf.append("(");    

	}
	return buf.toString();
    }

    public static String staticNetworkSendSuffix() {
	if (KjcOptions.altcodegen || KjcOptions.decoupled) 
	    return "";
	else 
	    return "))";
    }
    
    //the size of the buffer between in, out for the steady state
    public static int steadyBufferSize(InputTraceNode in,
				       OutputTraceNode out) 
    {
	int itemsReceived, itemsSent;
	//calculate the items the input trace receives
	FilterTraceNode next = (FilterTraceNode)in.getNext();
	itemsSent = (int)((next.getSteadyMult() * next.getFilter().getPopInt()) *
	    ((double)in.getWeight(out) / in.totalWeights()));
	
	//calculate the items the output trace sends
	FilterTraceNode prev = (FilterTraceNode)out.getPrevious();
	itemsReceived = (int)((prev.getSteadyMult() * prev.getFilter().getPushInt()) *
	    ((double)out.getWeight(in) / out.totalWeights()));
	//see if they are different
	if (itemsSent != itemsReceived)
	    Utils.fail("Calculating steady state: items received != items send on buffer");
	
	return itemsSent * getTypeSize(next.getFilter().getInputType());
    }
    
    //the size of the buffer between in / out for the init stage
    public static int initBufferSize(InputTraceNode in, 
				     OutputTraceNode out) 
    {
	int itemsReceived, itemsSent;
	//calculate the items the input trace receives
	FilterInfo next = FilterInfo.getFilterInfo((FilterTraceNode)in.getNext());
	itemsSent =  (int)(next.initItemsReceived() *
	    ((double)in.getWeight(out) / in.totalWeights()));
	
	//calculate the items the output trace sends
	FilterInfo prev = FilterInfo.getFilterInfo((FilterTraceNode)out.getPrevious());
	itemsReceived = (int)(prev.initItemsSent() *
	    ((double)out.getWeight(in) / out.totalWeights()));
	
	//see if they are different
	if (itemsSent != itemsReceived)
	    Utils.fail("Calculating steady state: items received != items send on buffer");
	
	return itemsSent * getTypeSize(next.filter.getInputType()); 
    }

    //the size of the buffer between in / out for the prime pump stage
    public static int primePumpBufferSize(InputTraceNode in, 
					  OutputTraceNode out) 
    {
	//items received into buffer
	int itemsReceived;

	//calculate the items the output trace sends, because
	//the downstream filter may not receive them all
	FilterTraceNode prev = (FilterTraceNode)out.getPrevious();
	itemsReceived = (int)((prev.getPrimePumpMult() * prev.getFilter().getPushInt()) *
	    ((double)out.getWeight(in) / out.totalWeights()));

	return itemsReceived * getTypeSize(prev.getFilter().getOutputType());
    }
    
    public static int magicBufferSize(InputTraceNode in, 
				 OutputTraceNode out) 
    {
	//i don't remember why I have the + down there,
	//but i am not going to change
	return Math.max(steadyBufferSize(in, out),
			initBufferSize(in, out)) + 
	    primePumpBufferSize(in, out);
    }
    
    public static String getFileVar(PredefinedContent content) 
    {
	if (content instanceof FileInputContent || 
	    content instanceof FileOutputContent) {
	    if (!fileVarNames.containsKey(content))
		fileVarNames.put(content, new String("file" + fileID++));
	    return (String)fileVarNames.get(content);
	}
	else 
	    Utils.fail("Calling getFileVar() on non-filereader/filewriter");
	return null;
    }

    public static String getFileHandle(PredefinedContent content) 
    {
	return "file_" + getFileVar(content);
    }

    public static String getOutputsVar(FileOutputContent out) 
    {
	return "outputs_" + getFileVar(out);
    }

    //given <i> bytes, round <i> up to the nearest cache
    //line divisible int
    public static int cacheLineDiv(int i) 
    {
	return (RawChip.cacheLineBytes - (i % RawChip.cacheLineBytes)) + i;
    }
    
}

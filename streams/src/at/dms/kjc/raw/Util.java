package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
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

/**
 * This class contains various function used by multiple passes
 */
public class Util extends at.dms.util.Utils {
    public static String CSTOINTVAR = "__csto_integer__";
    public static String CSTOFPVAR = "__csto_float__";
    public static String CSTIFPVAR = "__csti_float__";
    public static String CSTIINTVAR = "__csti_integer__";


    //returns true if this filter is mapped
    public static boolean countMe(SIRFilter filter) {
	return !(filter instanceof SIRIdentity ||
		filter instanceof SIRFileWriter ||
		filter instanceof SIRFileReader);
    }
    	
    /*
      get the execution Count of the previous node
    */
    public static int getCount(HashMap counts, FlatNode node) 
    {
	Integer count = ((Integer)counts.get(node));

	if (count == null)
	    return 0;
	return count.intValue();
    }
    
    /*
      get the execution count of the previous node
    */
    public static int getCountPrev(HashMap counts, FlatNode prev, FlatNode node) 
    {
	if (!(prev.contents instanceof SIRSplitter))
	    return getCount(counts, prev);
	
	//	if (((SIRSplitter)prev.contents).getType() == SIRSplitType.DUPLICATE)
	//   return getCount(counts, prev);
	
	//prev is a splitter
	double rate = getRRSplitterWeight(prev, node);
	return ((int)(rate * (double)getCount(counts, prev)));
    }

    //get the percentage of items sent from splitter prev to  node
    public static double getRRSplitterWeight(FlatNode prev, FlatNode node) {
	//prev is a splitter
	int sumWeights = 0;
	for (int i = 0; i < prev.ways; i++) 
	    sumWeights += prev.weights[i];
	int thisWeight = -1;
	for (int i = 0; i < prev.ways; i++) {
	    if (prev.edges[i].equals(node)) {
		thisWeight = prev.weights[i];
		break;
	    }
	}
	
	if (thisWeight == -1)
	    Utils.fail("Splitter not connected to node: "+prev+"->"+node);
	return ((double)thisWeight) / ((double)sumWeights);
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
		
		for (int i = 0; i < dims.length; i++) 
		    elements *= dims[i];
		
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

    public static CType getJoinerType(FlatNode joiner) 
    {
	boolean found;
	//search backward until we find the first filter
	while (!(joiner == null || joiner.contents instanceof SIRFilter)) {
	    found = false;
	    for (int i = 0; i < joiner.inputs; i++) {
		if (joiner.incoming[i] != null) {
		    joiner = joiner.incoming[i];
		    found = true;
		}
	    }
	    if (!found)
		Utils.fail("cannot find any upstream filter from " + joiner.contents.getName());
	}
	if (joiner != null) 
	    return ((SIRFilter)joiner.contents).getOutputType();
	else 
	    return CStdType.Null;
    }
    
    public static CType getOutputType(FlatNode node) {
	if (node.contents instanceof SIRFilter)
	    return ((SIRFilter)node.contents).getOutputType();
	else if (node.contents instanceof SIRJoiner)
	    return getJoinerType(node);
	else if (node.contents instanceof SIRSplitter)
	    return getOutputType(node.incoming[0]);
	else {
	    Utils.fail("Cannot get output type for this node");
	    return null;
	}
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
	    FlatIRToC ftoc = new FlatIRToC();
	    dims[i].accept(ftoc);
	    ret[i] = ftoc.getString();
	}
	return ret;
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
	    if (RawBackend.FILTER_DEBUG_MODE)
		buf.append("static_send_print");
	    else
		buf.append("static_send");
	    buf.append("(");    
	    //temporary fix for type changing filters
	    buf.append("(" + tapeType + ")");
	}
	return buf.toString();
    }

    public static String staticNetworkSendSuffix() {
	if (KjcOptions.altcodegen || KjcOptions.decoupled) 
	    return "";
	else 
	    return "))";
    }

    //return the FlatNodes that are directly downstream of the 
    //given flatnode and are themselves assigned a tile in the
    //layout
    public static HashSet getAssignedEdges(FlatNode node) {
	HashSet set = new HashSet();

	if (node == null)
	    return set;

	for (int i = 0; i < node.edges.length; i++) 
	    getAssignedEdgesHelper(node.edges[i], set);

	return set;
    }

    private static void getAssignedEdgesHelper(FlatNode node, HashSet set) {
	if (node == null) 
	    return;
	else if (Layout.isAssigned(node)) {
	    set.add(node);
	    return;
	}
	else {
	    for (int i = 0; i < node.edges.length; i++) 
		getAssignedEdgesHelper(node.edges[i], set);
	}
    }
}


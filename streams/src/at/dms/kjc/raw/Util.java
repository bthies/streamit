package at.dms.kjc.raw;

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
	    Utils.fail("Splitter not connected to node "+prev+"->"+node);
	double rate = ((double)thisWeight) / ((double)sumWeights);
	
	return ((int)(rate * (double)getCount(counts, prev)));
    }

    
    /*
      for a given CType return the size (number of elements that need to be sent
      when routing).
    */
    public static int getTypeSize(CType type) {

	if (!(type.isArrayType() || type.isClassType()))
	    return 1;
	
	if (type instanceof CArrayType) {
	    int elements = 1;
	    int dims[] = Util.makeInt(((CArrayType)type).getDims());
	    
	    for (int i = 0; i < dims.length; i++) 
		elements *= dims[i];

	    return elements;
	}
	
	System.out.println("should not be here");
	return 1;
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


}


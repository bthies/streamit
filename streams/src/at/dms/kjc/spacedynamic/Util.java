package at.dms.kjc.spacedynamic;

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
    public static String CGNOINTVAR = "__cgno_integer__";
    public static String CGNOFPVAR = "__cgno_float__";
    public static String CGNIFPVAR = "__cgni_float__";
    public static String CGNIINTVAR = "__cgni_integer__";
    

    //returns true if this filter is mapped
    public static boolean countMe(SIRFilter filter) {
	return !(filter instanceof SIRIdentity ||
		filter instanceof SIRFileWriter ||
		filter instanceof SIRFileReader);
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

    public static int getTypeSize(SIRStructure struct) 
    {
	int sum = 0;

	for (int i = 0; i < struct.getFields().length; i++) {
	    sum += getTypeSize(struct.getFields()[i].getType());
	}
	return sum;
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
	    return CStdType.Void;
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

    public static String networkReceivePrefix(boolean dynamic) {
	assert KjcOptions.altcodegen;
	return "";
    }

    public static String networkReceiveSuffix(boolean dynamic, CType tapeType) {
	assert KjcOptions.altcodegen;
	if (dynamic) {
	    if (tapeType.isFloatingPoint())
		return "= " + CGNIFPVAR + ";";
	    else
		return "= " + CGNIINTVAR + ";";    
	}
	else {
	    if (tapeType.isFloatingPoint())
		return "= " + CSTIFPVAR + ";";
	    else
		return "= " + CSTIINTVAR + ";";
	}
    }
    

    public static String networkSendPrefix(boolean dynamic, CType tapeType) {
	assert KjcOptions.altcodegen;
	StringBuffer buf = new StringBuffer();
	if (dynamic) {
	    if (tapeType.isFloatingPoint())
		buf.append(CGNOFPVAR);
	    else
		buf.append(CGNOINTVAR);
	}
	else {
	    if (tapeType.isFloatingPoint())
		buf.append(CSTOFPVAR);
	    else
		buf.append(CSTOINTVAR);
	} 

	buf.append(" = (" + tapeType + ")");
	return buf.toString();
    }

    public static String networkSendSuffix(boolean dynamic) {
	assert KjcOptions.altcodegen;
	return "";
    }

    //return the FlatNodes that are directly downstream of the 
    //given flatnode and are themselves assigned a tile in the
    //layout
    public static HashSet getAssignedEdges(Layout layout, FlatNode node) {
	HashSet set = new HashSet();

	if (node == null)
	    return set;

	for (int i = 0; i < node.edges.length; i++) 
	    getAssignedEdgesHelper(layout, node.edges[i], set);

	return set;
    }

    private static void getAssignedEdgesHelper(Layout layout, FlatNode node, HashSet set) {
	if (node == null) 
	    return;
	else if (layout.isAssigned(node)) {
	    set.add(node);
	    return;
	}
	else {
	    for (int i = 0; i < node.edges.length; i++) 
		getAssignedEdgesHelper(layout, node.edges[i], set);
	}
    }

    //get all filters/joiners that are directly connected downstream to this
    //node, but go thru all splitters.  The node itself is a joiner or 
    //filter,  NOTE, THIS HAS NOT BEEN TESTED BUT IT SHOULD WORK, I DID NOT 
    //NEED IT FOR WHAT I WROTE IT FOR
    public static HashSet getDirectDownstream(FlatNode node) 
    {
	if (node == null || node.isSplitter())
	    Utils.fail("getDirectDownStream(...) error. Node not filter or joiner.");
	if (node.ways > 0)
	    return getDirectDownstreamHelper(node.edges[0]);
	else 
	    return new HashSet();
    }
    
    private static HashSet getDirectDownstreamHelper(FlatNode current) 
    {
	if (current == null)
	    return new HashSet();
	else if (current.isFilter() || current.isJoiner()) {
	    HashSet ret = new HashSet();
	    ret.add(current);
	    return ret;
	}
	else if (current.isSplitter()) {
	    HashSet ret = new HashSet();
	    
	    for (int i = 0; i < current.ways; i++) {
		if(current.weights[i]!=0)
		    SpaceDynamicBackend.addAll(ret, getDirectDownstreamHelper(current.edges[i]));
	    }
	    return ret;
	}
	return null;
    }

    public static SIRFilter getSinkFilter(SIRStream stream) 
    {
	if (stream instanceof SIRFilter)
	    return (SIRFilter)stream;
	else if (stream instanceof SIRPipeline)
	    return getSinkFilter(((SIRPipeline)stream).get(((SIRPipeline)stream).size() - 1));
	else
	    assert false : "Calling getSinkFilter() on Stream with non-filter sink";
	return null;
    }
    
    public static SIRFilter getSourceFilter(SIRStream stream) 
    {
	if (stream instanceof SIRFilter)
	    return (SIRFilter)stream;
	else if (stream instanceof SIRPipeline) 
	    return getSourceFilter(((SIRPipeline)stream).get(0));
	else 
	    assert false : "Calling getSourceFilter() on Stream with non-filter source";
	return null;
    }
}



package at.dms.kjc.spacetime;

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

public class Util {
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

    public static int calculateRemaining(FilterTraceNode node, FilterTraceNode previous) 
    {
	SIRFilter filter = node.getFilter();
	
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
	int initFire = node.getInitMult();
	
	//if this is not a twostage, fake it by adding to initFire,
	//so we always think the preWork is called
	if (!(filter instanceof SIRTwoStageFilter))
	    initFire++;
	
	//the number of items produced by the upstream filter in
	//initialization
	int upStreamItems = 0;
	
	if (previous != null) {
	    //calculate upstream items received during init  FILL ME IN!!!
	    upStreamItems = previous.getFilter().getPushInt() * 
		previous.getInitMult();
	    if (previous.getFilter() instanceof SIRTwoStageFilter) {
		upStreamItems -= ((SIRTwoStageFilter)previous.getFilter()).getPushInt();
		upStreamItems += ((SIRTwoStageFilter)previous.getFilter()).getInitPush();
	    }
	}
	
	int bottomPeek = 0, remaining = 0;

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
	
	return remaining;
    }
}

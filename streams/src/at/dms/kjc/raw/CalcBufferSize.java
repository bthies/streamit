package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;

public class CalcBufferSize extends at.dms.util.Utils implements FlatVisitor
{
    private static HashMap prodBufferSize;
    private static HashMap consBufferSize;
    private HashMap init;
    private HashMap steady;
    
    public static void createBufferSizePow2(FlatNode top) {
	
	
	CalcBufferSize calc = new CalcBufferSize(top);
	top.accept(calc, null, true);
	makePow2();
    }
    
    private static void makePow2() {
	HashMap result[] = {consBufferSize, prodBufferSize};
	
	for (int i=0; i < 2; i++) {
	    Iterator it = result[i].keySet().iterator();
	    while(it.hasNext()){
		FlatNode node = (FlatNode)it.next();
		int oldVal = ((Integer)result[i].get(node)).intValue();
		result[i].put(node, new Integer(nextPow2(oldVal)));
	    }
	}
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

    public static int getProdBufSize(FlatNode node) {
	return ((Integer)prodBufferSize.get(node)).intValue();
    }
    
    public static int getConsBufSize(FlatNode node) {
	//this will not be calculated in decoupled execution mode
	//so just return the peek value
	if (KjcOptions.decoupled) 
	    return ((SIRFilter)node.contents).getPeekInt();
	else 
	    return ((Integer)consBufferSize.get(node)).intValue();
    }
    


    private CalcBufferSize(FlatNode top) {
	prodBufferSize  = new HashMap();
	consBufferSize = new HashMap();
	
	init = (HashMap)RawBackend.initExecutionCounts.clone();
	steady = (HashMap)RawBackend.steadyExecutionCounts.clone();
    }


    //Just a debugging function, not used
    public void visitNode(FlatNode node) {
	if (node.contents instanceof SIRFilter) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    		    
	    if (node.inputs > 0) {
		FlatNode prev = node.incoming[0];
		//if prev is a joiner or a splitter, production is 1
		int prevProd = 1;
		int currentCons = 0;
		currentCons = filter.getPopInt();

		if (prev.contents instanceof SIRFilter) {
		    prevProd = ((SIRFilter)prev.contents).getPushInt();
		    //correct for two stage filters
		    if (prev.contents instanceof SIRTwoStageFilter &&
			((SIRTwoStageFilter)prev.contents).getInitPush() > prevProd)
			prevProd = ((SIRTwoStageFilter)prev.contents).getInitPush();
		}
			
		//if the current is a two stage, 
		if (filter instanceof SIRTwoStageFilter &&
		    ((SIRTwoStageFilter)filter).getInitPop() < currentCons)
		    currentCons = ((SIRTwoStageFilter)filter).getInitPop();

// 		int size = Math.max((Util.getCountPrev(init, prev, node) * prevProd),
// 				    ((Util.getCountPrev(init, prev, node) + 
// 				      Util.getCountPrev(steady, prev, node)) * prevProd));

 		int size = Math.max((Util.getCountPrev(init, prev, node) * prevProd),
 				    ((Util.getCountPrev(init, prev, node) + 
 				      Util.getCountPrev(steady, prev, node)) * prevProd) -
				    Util.getCount(init, node) * currentCons);

		//a buffersize of 1 does not work too well
		size = Math.max(size, 2);
		consBufferSize.put(node, new Integer(size));
	    }
	    else 
		consBufferSize.put(node, new Integer(0));
	    
	    //Production Buffer (outgoing buffer)
	    if (node.ways > 0) {
		int size = Math.max(Util.getCount(init, node), Util.getCount(steady, node)) * 
		    filter.getPushInt();
		
		//a buffersize of 1 does not work too well
		size = Math.max(size, 2);
		prodBufferSize.put(node, new Integer(size));
	    }
	    else 
		prodBufferSize.put(node, new Integer(0));
	}
    }
}

	
	


	

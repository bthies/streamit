package at.dms.kjc.raw;

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
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;

/**
 * This class will unroll and propagate for all sinks in the stream
 * graph.  NOTE that this will not unroll any for loops freshly unless
 * they're marked with their "hasUnrolled==false".  This is because in
 * other places of the compiler, we are careful not to unroll the same
 * loop twice (if we have a partial unrolling factor.)  Consider using
 * Unroller.unrollFilter to completely unroll something.
 */
public class SinkUnroller extends at.dms.util.Utils 
    implements FlatVisitor, Constants 
{
    public static void doit(FlatNode top) {
	top.accept((new SinkUnroller()), null, true);
    }

    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()){
	    //do not unroll file writers...
	    if (node.contents instanceof SIRFileWriter)
		return; 
	    //if this is a sink, unroll its work funtion
	    if (((SIRFilter)node.contents).getPushInt() == 0) {
		System.out.println("Propagating and Unrolling Sink " + 
				   ((SIRFilter)node.contents).getName()+"...");
		// unroll maximally
		int oldUnroll = KjcOptions.unroll;
		KjcOptions.unroll = 100000;
		FieldProp.doPropagate((SIRStream)node.contents, true);
		KjcOptions.unroll = KjcOptions.unroll;
		/*
		Unroller unroller;
		do {
		    do {
			//unroll
			unroller = new Unroller(new Hashtable());
			((SIRFilter)node.contents).getWork().accept(unroller);
		    } while(unroller.hasUnrolled());
		    //propagate constants 
		    ((SIRFilter)node.contents).getWork().
			accept(new Propagator(new Hashtable()));
		    //unroll again
		    unroller = new Unroller(new Hashtable());
		    ((SIRFilter)node.contents).getWork().accept(unroller);
		} while(unroller.hasUnrolled());
		*/
	    }
	}
    }
}

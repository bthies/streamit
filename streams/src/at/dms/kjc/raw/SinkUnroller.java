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
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;

/**
 * This class will unroll and propagate for all sinks 
 * in the stream graph
 **/
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
	    //if this is a sink unroll
	    if (((SIRFilter)node.contents).getPushInt() == 0) {
		System.out.println("Propagating and Unrolling Sink " + 
				   ((SIRFilter)node.contents).getName()+"...");
		for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {
		    Unroller unroller;
		    do {
			do {
			    //unroll
			    unroller = new Unroller(new Hashtable());
			    ((SIRFilter)node.contents).getMethods()[i].accept(unroller);
			} while(unroller.hasUnrolled());
			//propagate constants 
			((SIRFilter)node.contents).getMethods()[i].
			    accept(new Propagator(new Hashtable()));
			//unroll again
			unroller = new Unroller(new Hashtable());
			((SIRFilter)node.contents).getMethods()[i].accept(unroller);
		    } while(unroller.hasUnrolled());	
		}

	    }
	}
    }
}

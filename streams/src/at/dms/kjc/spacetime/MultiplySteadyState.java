package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashSet;
import at.dms.kjc.*;

public class MultiplySteadyState 
{
    public static void doit(Partitioner partitioner, SimpleScheduler scheduler) 
    {

	assert KjcOptions.steadymult > 0 : 
	    "Illegal steadymult argument";
	for (int i = 0; i < partitioner.io.length; i++) {
	    partitioner.io[i].getHead().getNextFilter().getFilter().multSteadyMult(KjcOptions.steadymult);
	}
	
	Iterator traceNodes = Util.traceNodeTraversal(scheduler.getSchedule());
	while (traceNodes.hasNext()) {
	    TraceNode traceNode = (TraceNode)traceNodes.next();
	    if (traceNode.isFilterTrace()) {
		((FilterTraceNode)traceNode).getFilter().multSteadyMult(KjcOptions.steadymult);
	    }
	}
	
    }
}


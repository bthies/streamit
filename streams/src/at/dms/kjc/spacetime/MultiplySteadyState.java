package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashSet;

public class MultiplySteadyState 
{
    private static final int MULTIPLER = 60;
    
    public static void doit(Partitioner partitioner, SimpleScheduler scheduler) 
    {
	for (int i = 0; i < partitioner.io.length; i++) {
	    partitioner.io[i].getHead().getNextFilter().getFilter().multSteadyMult(MULTIPLER);
	}
	
	Iterator traceNodes = Util.traceNodeTraversal(scheduler.getSchedule());
	while (traceNodes.hasNext()) {
	    TraceNode traceNode = (TraceNode)traceNodes.next();
	    if (traceNode.isFilterTrace()) {
		((FilterTraceNode)traceNode).getFilter().multSteadyMult(MULTIPLER);
	    }
	}
	
    }
}


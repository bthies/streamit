package at.dms.kjc.spacetime;

import java.util.List;
import java.util.Iterator;
import java.util.Arrays;


/**
 * This class will insert identity filters to reduce the width of 
 * splitting and joining, so that it

 * It modifies the stream graph in place, adding edges and identities.

 * It must add the new nodes to the init and steady trace list for 
 * other passes.
 **/
public class ReduceSJWidth
{
    private static Trace[] steady;
    private static Trace[] init;

    public static void run(List initList, List steadyList, 
			   RawChip chip, Trace[] files) 
    {
	//keep the old steady traversal around so we can iterate over it...
	Trace[] oldSteady = (Trace[])steadyList.toArray(new Trace[0]);
	//make arrays of the traversals so we can add to them
	steady = (Trace[])steadyList.toArray(new Trace[0]);
	init = (Trace[])initList.toArray(new Trace[0]);

	for (int i = 0; i < oldSteady.length; i++) {
	    reduceIncomingEdges(oldSteady[i].getHead(), chip);
	    reduceOutgoingEdges(oldSteady[i].getTail());
	}
	
	//now reset the list for the remaining passes
	initList = Arrays.asList(init);
	steadyList = Arrays.asList(steady);
    }
    

    private static void reduceIncomingEdges(InputTraceNode input, RawChip chip) 
    {
	//check if there is anything to do
	if (input.getSourceSet().size() <= chip.getNumDev()) 
	    return;
	
	//create new trace
	
	//keep 15, coalesce remaining

	//repeat on the new inputTraceNode
	//by recursively calling, 
    }
    

    private static void reduceOutgoingEdges(OutputTraceNode output)
    {
	
    }
    
}

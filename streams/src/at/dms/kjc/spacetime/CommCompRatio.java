package at.dms.kjc.spacetime;

import java.util.Iterator;

public class CommCompRatio 
{
    //returns computation / communication ratio
    public static double ratio(Partitioner partitioner) 
    {
	int comp = 0, comm = 0;
	//get the trace node travesal
	Iterator traceNodeIt = 
	    Util.traceNodeTraversal(InitSchedule.getInitSchedule(partitioner.topTraces));
	
	while (traceNodeIt.hasNext()) {
	    TraceNode traceNode = (TraceNode)traceNodeIt.next();

	    if (traceNode.isFilterTrace()) {
		FilterTraceNode filter = (FilterTraceNode)traceNode;
		//comm += (filter.getFilter().getSteadyMult() * filter.getFilter().getPushInt());
		comp += (filter.getFilter().getSteadyMult() * partitioner.getFilterWork(filter));
	    }
	    else if (traceNode.isOutputTrace()) {
		OutputTraceNode output = (OutputTraceNode)traceNode;
		FilterTraceNode filter = (FilterTraceNode)output.getPrevious();
		//FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
		//calculate the number of items sent
		
		int itemsReceived = filter.getFilter().getPushInt() *
		    filter.getFilter().getSteadyMult();
		int iterations = (output.totalWeights() != 0 ? itemsReceived / output.totalWeights()
				  : 0);
		
		int itemsSent = 0;
		
		for (int j = 0; j < output.getWeights().length; j++) {
		    for (int k = 0; k < output.getWeights()[j]; k++) {
			//generate the array of compute node dests
			itemsSent += output.getDests()[j].length;
		    }
		}
		
		comm += (iterations * itemsSent);
	    }
	    else {
		InputTraceNode input = (InputTraceNode)traceNode;
		FilterTraceNode filter = (FilterTraceNode)input.getNext();
		
		//calculate the number of items received
		int itemsSent = filter.getFilter().getSteadyMult() * 
		    filter.getFilter().getPopInt();
		
		int iterations = (input.totalWeights() != 0 ? itemsSent / input.totalWeights() : 0);
		int itemsReceived = 0;

		for (int j = 0; j < input.getWeights().length; j++) {
		    //get the source buffer, pass thru redundant buffer(s)
		    itemsReceived += input.getWeights()[j];
		}

		comm += (iterations * itemsReceived);
	    }
	    
	}
	
	if (comm == 0) 
	    return 0.0;
	

	System.out.println("Computation / Communication Ratio: " + ((double)comp)/((double)comm));
	return ((double)comp)/((double)comm);
    }
}

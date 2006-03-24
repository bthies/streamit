package at.dms.kjc.spacetime;

import java.util.Iterator;

/**
 * Calculate the computation to communication ratio.  Poorly named class,
 * the name should be CompCommRatio. 
 * 
 * @author mgordon
 *
 */
public class CommCompRatio {
    
    /**
     * Calculate the computation to communication ratio of the 
     * application.  Where the computation is total work of all the filters
     * in the steady-state and the communication is the 
     * number of items sent between slices.
     * 
     * @param partitioner The partitioner we used to slice the graph.
     * 
     * @return The computation to communication ratio.
     */
    public static double ratio(Partitioner partitioner) {
        int comp = 0, comm = 0;
        // get the trace node travesal
        Iterator traceNodeIt = Util.traceNodeTraversal(DataFlowOrder
                                                       .getTraversal(partitioner.topTraces));

        while (traceNodeIt.hasNext()) {
            TraceNode traceNode = (TraceNode) traceNodeIt.next();

            if (traceNode.isFilterTrace()) {
                FilterTraceNode filter = (FilterTraceNode) traceNode;
                // comm += (filter.getFilter().getSteadyMult() *
                // filter.getFilter().getPushInt());
                comp += (filter.getFilter().getSteadyMult() * partitioner
                         .getFilterWork(filter));
            } else if (traceNode.isOutputTrace()) {
                OutputTraceNode output = (OutputTraceNode) traceNode;
                FilterTraceNode filter = (FilterTraceNode) output.getPrevious();
                // FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
                // calculate the number of items sent

                int itemsReceived = filter.getFilter().getPushInt()
                    * filter.getFilter().getSteadyMult();
                int iterations = (output.totalWeights() != 0 ? itemsReceived
                                  / output.totalWeights() : 0);

                int itemsSent = 0;

                for (int j = 0; j < output.getWeights().length; j++) {
                    for (int k = 0; k < output.getWeights()[j]; k++) {
                        // generate the array of compute node dests
                        itemsSent += output.getDests()[j].length;
                    }
                }

                comm += (iterations * itemsSent);
            } else {
                InputTraceNode input = (InputTraceNode) traceNode;
                FilterTraceNode filter = (FilterTraceNode) input.getNext();

                // calculate the number of items received
                int itemsSent = filter.getFilter().getSteadyMult()
                    * filter.getFilter().getPopInt();

                int iterations = (input.totalWeights() != 0 ? itemsSent
                                  / input.totalWeights() : 0);
                int itemsReceived = 0;

                for (int j = 0; j < input.getWeights().length; j++) {
                    // get the source buffer, pass thru redundant buffer(s)
                    itemsReceived += input.getWeights()[j];
                }

                comm += (iterations * itemsReceived);
            }

        }

        if (comm == 0)
            return 0.0;

        System.out.println("Computation / Communication Ratio: "
                           + ((double) comp) / ((double) comm));
        return ((double) comp) / ((double) comm);
    }
}

package at.dms.kjc.spacetime;

import java.util.Iterator;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*; 
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.slicegraph.Util;

import java.util.*;

/**
 * Calculate the computation to communication ratio.  Poorly named class,
 * the name should be CompCommRatio. 
 * 
 * @author mgordon
 *
 */
public class CompCommRatio {
    
   
    private static int comp = 0;
    private static int comm = 0;
    private static WorkEstimate work;
    private static HashMap<SIRStream, int[]> mults;
    
    public static double ratio(SIRStream str, WorkEstimate work,
            HashMap<SIRStream, int[]> executionCounts) {
        
        comp = 0;
        comm = 0;
        CompCommRatio.work = work;
        mults = executionCounts;
        walkSTR(str);
        
        return ((double)comp)/((double)comm);
 
    }
//  The following structure appears all over the place.  It needs to be abstracted somehow.
    // Walk SIR structure to get down to 
    private static void walkSTR(SIRStream str) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            walkSTR(fl.getBody());
            walkSTR(fl.getLoop());
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                walkSTR(child);
            }
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
            if (mults.containsKey(sj)) {
                comm += sj.getSplitter().getSumOfWeights() * mults.get(sj)[0];
                comm += sj.getJoiner().getSumOfWeights() * mults.get(sj)[0];
            }
            while (iter.hasNext()) {
                SIRStream child = iter.next();
                walkSTR(child);
            }
        }
        //update the comm and comp numbers...
        if (str instanceof SIRFilter) {
           comp += work.getWork((SIRFilter)str);
           comm += ((SIRFilter)str).getPushInt();
        }
    } 
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
        // get the slice node travesal
        Iterator<SliceNode> sliceNodeIt = Util.sliceNodeTraversal(DataFlowOrder
                                                       .getTraversal(partitioner.getTopSlices()));

        while (sliceNodeIt.hasNext()) {
            SliceNode sliceNode = sliceNodeIt.next();

            if (sliceNode.isFilterSlice()) {
                FilterSliceNode filter = (FilterSliceNode) sliceNode;
                // comm += (filter.getFilter().getSteadyMult() *
                // filter.getFilter().getPushInt());
                comp += (filter.getFilter().getSteadyMult() * partitioner
                         .getFilterWork(filter));
            } else if (sliceNode.isOutputSlice()) {
                OutputSliceNode output = (OutputSliceNode) sliceNode;
                FilterSliceNode filter = (FilterSliceNode) output.getPrevious();
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
                InputSliceNode input = (InputSliceNode) sliceNode;
                FilterSliceNode filter = (FilterSliceNode) input.getNext();

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

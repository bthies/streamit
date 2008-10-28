package at.dms.kjc.slicegraph;

import at.dms.kjc.backendSupport.*;

/**
 * Go through all the slices and create and install the schedule of distribution for the
 * init schedule for input and output slice nodes by setting to empty if nothing done or
 * setting to init if something done. 
 *  
 * @author mgordon
 *
 */
public class InstallInitDistributions {

    /**
     * Go through all the slices and create and install the schedule of distribution for the
     * init schedule for input and output slice nodes by setting to empty if nothing done or
     * setting to init if something done. 
     */  
    public static void doit(Slice[] slices) {
        for (Slice slice : slices) {
            FilterInfo fi = FilterInfo.getFilterInfo(slice.getFirstFilter());
            //do input slice node
            if (fi.totalItemsReceived(SchedulingPhase.INIT) > 0) {
                slice.getHead().setInitWeights(slice.getHead().getWeights(SchedulingPhase.STEADY).clone());
                slice.getHead().setInitSources(slice.getHead().getSources(SchedulingPhase.STEADY).clone());
            } else {
                //nothing done by input
                slice.getHead().setInitWeights(new int[0]);
                slice.getHead().setInitSources(new InterSliceEdge[0]);
            }
            
            //do output slice
            if (fi.totalItemsSent(SchedulingPhase.INIT) > 0) {
                slice.getTail().setInitDests(slice.getTail().getDests(SchedulingPhase.STEADY).clone());
                slice.getTail().setInitWeights(slice.getTail().getWeights(SchedulingPhase.STEADY).clone());
            } else {
                //nothing done by output
                slice.getTail().setInitDests(new InterSliceEdge[0][0]);
                slice.getTail().setInitWeights(new int[0]);
            }
        }
    }
}

package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashSet;
import at.dms.kjc.*;

public class MultiplySteadyState {
    public static void doit(SpaceTimeSchedule schedule) {
        
        assert KjcOptions.steadymult > 0 : "Illegal steadymult argument";
        for (int i = 0; i < schedule.partitioner.io.length; i++) {
            schedule.partitioner.io[i].getHead().getNextFilter().getFilter()
                .multSteadyMult(KjcOptions.steadymult);
        }
        
        Iterator traceNodes = Util.traceNodeTraversal(schedule.getSchedule());
        while (traceNodes.hasNext()) {
            TraceNode traceNode = (TraceNode) traceNodes.next();
            if (traceNode.isFilterTrace()) {
                ((FilterTraceNode) traceNode).getFilter().multSteadyMult(
                                                                         KjcOptions.steadymult);
            }
        }

    }
}

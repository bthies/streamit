package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.TraceNode;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashSet;
import at.dms.kjc.*;

public class MultiplySteadyState {
    public static void doit(Trace[] traces) {
        
        assert KjcOptions.steadymult > 0 : "Illegal steadymult argument";
       /*
        for (int i = 0; i < schedule.partitioner.io.length; i++) {
            schedule.partitioner.io[i].getHead().getNextFilter().getFilter()
                .multSteadyMult(KjcOptions.steadymult);
        }
        */
        Iterator<TraceNode> traceNodes = Util.traceNodeTraversal(traces);
        while (traceNodes.hasNext()) {
            TraceNode traceNode = traceNodes.next();
            if (traceNode.isFilterTrace()) {
                ((FilterTraceNode) traceNode).getFilter().multSteadyMult(KjcOptions.steadymult);
            }
        }

    }
}
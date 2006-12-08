package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.*;
import at.dms.kjc.slicegraph.Util;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashSet;
import at.dms.kjc.*;

public class MultiplySteadyState {
    public static void doit(Slice[] traces) {
        
        assert KjcOptions.steadymult > 0 : "Illegal steadymult argument";
       /*
        for (int i = 0; i < schedule.partitioner.io.length; i++) {
            schedule.partitioner.io[i].getHead().getNextFilter().getFilter()
                .multSteadyMult(KjcOptions.steadymult);
        }
        */
        Iterator<SliceNode> sliceNodes = Util.sliceNodeTraversal(traces);
        while (sliceNodes.hasNext()) {
            SliceNode sliceNode = sliceNodes.next();
            if (sliceNode.isFilterSlice()) {
                ((FilterSliceNode) sliceNode).getFilter().multSteadyMult(KjcOptions.steadymult);
            }
        }

    }
}
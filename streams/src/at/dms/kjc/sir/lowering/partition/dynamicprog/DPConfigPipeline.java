package at.dms.kjc.sir.lowering.partition.dynamicprog;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

class DPConfigPipeline extends DPConfigContainer {

    public DPConfigPipeline(SIRPipeline cont, DynamicProgPartitioner partitioner) {
        super(cont, partitioner, getWidths(cont), cont.size());
    }

    protected DPCost get(int tileLimit, int nextToJoiner) {
        // if this pipeline ends in a splitjoin that has a null
        // joiner, then don't need to allocate a tile for the joiner,
        // so count us as already being next to a joiner.
        if (endsInNullJoiner()) {
            nextToJoiner = 1;
        }
    
        return super.get(tileLimit, nextToJoiner);
    }
    
    public SIRStream traceback(LinkedList<PartitionRecord> partitions, PartitionRecord curPartition, int tileLimit, int nextToJoiner, SIRStream str) {
        // if we have null joiner, then don't need to allocate a tile
        // for the joiner, so count us as already being next to a
        // joiner.
        if (endsInNullJoiner()) {
            nextToJoiner = 1;
        }
    
        return super.traceback(partitions, curPartition, tileLimit, nextToJoiner, str);
    }

    protected DPConfig childConfig(int x, int y) {
        SIRStream c1 = cont.get(y), c2;
        // if we're just accessing a hierarchical unit, return it
        if (x==0 && !(c1 instanceof SIRSplitJoin)) {
            c2 = c1;
        } else {
            // otherwise, we're looking inside a hierarchical unit -- must
            // be a splitjoin
            assert c1 instanceof SIRSplitJoin:
                "Trying to get (" + x + ", " + y  + "), which is " +
                c1.getName();
            c2 = ((SIRSplitJoin)c1).get(x);
        }
        return partitioner.getConfig(c2);
    }

    private boolean endsInNullJoiner() {
        if (cont.size()>0 && cont.get(cont.size()-1) instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin)cont.get(cont.size()-1);
            SIRJoiner joiner = ((SIRSplitJoin)sj).getJoiner();
            if (joiner.getType().isNull() || joiner.getSumOfWeights()==0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the width of all child streams... sj's have width of
     * their size; all else has width of 1.
     */
    private static final int[] getWidths(SIRPipeline cont) {
        int[] result = new int[cont.size()];
        for (int i=0; i<result.length; i++) {
            SIRStream child = cont.get(i);
            if (child instanceof SIRSplitJoin) {
                result[i] = ((SIRSplitJoin)child).size();
            } else {
                result[i] = 1;
            }
        }
        return result;
    }
}

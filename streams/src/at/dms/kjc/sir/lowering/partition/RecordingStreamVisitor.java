package at.dms.kjc.sir.lowering.partition;

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

/**
 * Records all filters, splitters, and joiners in a given stream into
 * a partition record.
 */
public class RecordingStreamVisitor extends EmptyStreamVisitor {
    private final PartitionRecord curPartition;
    private final WorkEstimate work;

    public RecordingStreamVisitor(PartitionRecord _curPartition) {
        this(_curPartition, null);
    }
    
    public RecordingStreamVisitor(PartitionRecord _curPartition, WorkEstimate _work) {
        this.curPartition = _curPartition;
        this.work = _work;
    }

    /**
     * Returns work estimate for <op>, if available.
     */
    private int getWork(SIROperator op) {
        if (op instanceof SIRFilter && work!=null) {
            return work.getWork((SIRFilter)op);
        } else {
            // do not count other streams
            return 0;
        }
    }
    
    /**
     * This is called before all visits to a stream structure (Filter,
     * Pipeline, SplitJoin, FeedbackLoop)
     */
    public void preVisitStream(SIRStream self,
                               SIRIterator iter) {
        // add the stream
        if (self instanceof SIRContainer) {
            // containers
            if (!curPartition.contains(self)) {
                curPartition.add((SIRContainer)self, getWork(self));
            }
        } else {
            // filters
            if (!curPartition.contains(self)) {
                curPartition.add(self, getWork(self));
            }
        }
        // also add splitters, joiners
        if (self instanceof SIRSplitJoin) {
            SIRSplitter splitter = ((SIRSplitJoin)self).getSplitter();
            if (!curPartition.contains(splitter)) {
                curPartition.add(splitter, getWork(splitter));
            }

            SIRJoiner joiner = ((SIRSplitJoin)self).getJoiner();
            if (!curPartition.contains(joiner)) {
                curPartition.add(joiner, getWork(joiner));
            }
        }
        if (self instanceof SIRFeedbackLoop) {
            SIRSplitter splitter = ((SIRFeedbackLoop)self).getSplitter();
            if (!curPartition.contains(splitter)) {
                curPartition.add(splitter, getWork(splitter));
            }

            SIRJoiner joiner = ((SIRFeedbackLoop)self).getJoiner();
            if (!curPartition.contains(joiner)) {
                curPartition.add(joiner, getWork(joiner));
            }
        }
    }
}

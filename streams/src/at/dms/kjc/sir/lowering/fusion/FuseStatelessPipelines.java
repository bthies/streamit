package at.dms.kjc.sir.lowering.fusion;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fission.*;

import java.util.List;
import java.util.ArrayList;

/**
 * The goal of this class is to increase the granularity of stateless
 * regions so that they can be more effectively fissed for parallel
 * execution.  To simplify things, it only fuses stateless neighbors
 * in pipelines.  The output of the fusion should also be stateless.
 * If an entire pipeline is fused, then it may be fused further with
 * neighbors in a higher-level pipeline.
 */
public class FuseStatelessPipelines {
    /**
     * Fuses stateless (sub-)pipelines as much as possible in 'str'
     * and returns new stream.
     */
    public static SIRStream doit(SIRStream str) {
        StatelessFuser fuser = new StatelessFuser();
        return (SIRStream)str.accept(fuser);
    }

    // visitor to actually do the work
    static class StatelessFuser extends ReplacingStreamVisitor {
        public Object visitPipeline(SIRPipeline self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init) {
            // first visit children
            self = (SIRPipeline)super.visitPipeline(self, fields, methods, init);

            // 1. DETERMINE WHERE TO FUSE:
            // walk through pipeline, determining who should be fused.
            // We fuse anyone where the result will be stateless.
            // This requires:
            // 1. original filters are stateless
            // 2. only the first filter can peek (otherwise peek
            //    buffer becomes state.)
            ArrayList partitions = new ArrayList();
            // position of start of current fusion segment
            int pos = 0; 
            // length of current fusion segment
            int count = 0;
            while (pos+count < self.size()) {
                SIRStream child = self.get(pos+count);
                if (// can only fuse filters
                    child instanceof SIRFilter &&
                    // must be able to fuse child
                    FusePipe.isFusable(child) &&
                    // child must be stateless
                    !StatelessDuplicate.hasMutableState((SIRFilter)child) &&
                    // only first filter can peek
                    (count==0 || !doesPeeking((SIRFilter)child))) {
                    // keep looping if the partition has room to grow
                    if (pos+count+1 < self.size()) {
                        count++;
                        continue;
                    }
                } else if (count > 0) {
                    // this one should not be included in the count
                    count--;
                }
                // otherwise, should not fuse with child, or we are at
                // the end of the pipeline, so conclude current set of
                // partitions
                partitions.add(new Integer(count+1));
                pos = pos+count+1;
                count = 0;
            }

            // 2. PERFORM FUSION:
            if (partitions.size()==1) {
                // if we are fusing everything, call fuse all due to the
                // bad interface in FusePipe
                SIRPipeline wrapper = FuseAll.fuse(self);
                // should get back a wrapper with a single component
                return wrapper.get(0);
            } else {
                // fuse the pipeline according to the recorded partitions
                int[] partitionArr = new int[partitions.size()];
                for (int i=0; i<partitionArr.length; i++) {
                    partitionArr[i] = ((Integer)partitions.get(i)).intValue();
                }
                PartitionGroup pg = PartitionGroup.createFromArray(partitionArr);
                // return ourself, since we still have more than one
                // component (we were mutated by the fuser)
                FusePipe.fuse(self, pg);
                return self;
            }
        }
        
        // returns whether or not 'filter' does any peeking (in either
        // init or steady stage).
        boolean doesPeeking(SIRFilter filter) {
            // calculate amount of peeking in init stage (if any)
            boolean initPeek;
            if (filter instanceof SIRTwoStageFilter) {
                SIRTwoStageFilter twoStage = (SIRTwoStageFilter)filter;
                initPeek = twoStage.getInitPeekInt() > twoStage.getInitPopInt();
            } else {
                initPeek = false;
            }
            // calculate amount of peeking in steady stage
            boolean steadyPeek = filter.getPeekInt() > filter.getPopInt();
            
            // return whether there is peeking in either stage
            return initPeek || steadyPeek;
        }
    }
}

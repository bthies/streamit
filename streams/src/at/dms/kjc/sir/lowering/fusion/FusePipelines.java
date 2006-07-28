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
public class FusePipelines {
    /**
     * Fuses any two adjacent filters in 'str' so long as:
     *  - the shared parent of the filter is a pipeline
     *  - the result of fusion will be stateless
     *
     * If a complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     */
    public static SIRStream fuseStatelessPipelines(SIRStream str) {
        // first eliminate wrapper pipelines, as they obscure
        // continuous pipeline sections
        Lifter.lift(str);
        PipelineFuser fuser = new PipelineFuser(true);
        return (SIRStream)str.accept(fuser);
    }

    /**
     * Fuses all adjacent filters whos parent is a pipeline.  If a
     * complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     */
    public static SIRStream fusePipelines(SIRStream str) {
        // first eliminate wrapper pipelines, as they obscure
        // continuous pipeline sections
        Lifter.lift(str);
        PipelineFuser fuser = new PipelineFuser(false);
        return (SIRStream)str.accept(fuser);
    }

    // visitor to actually do the work
    static class PipelineFuser extends ReplacingStreamVisitor {
        /**
         * If true, then only apply fusion when the result will be
         * stateless.
         */
        private boolean onlyStateless;

        /**
         * If 'onlyStateless' is true, then only fuses adjacent
         * filters if the result of fusion will be stateless.
         */
        public PipelineFuser(boolean onlyStateless) {
            this.onlyStateless = onlyStateless;
        }

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
                    // if only fusing stateless, test for stateless result
                    (!onlyStateless || statelessResult((SIRFilter)child, count))) {
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
                // don't need to fuse if we have only one child --
                // that would end up fusing splitjoin children too
                if (self.size() > 1) {
                    // if we are fusing everything, call fuse all due to the
                    // bad interface in FusePipe
                    SIRPipeline wrapper = FuseAll.fuse(self);
                    // should get back a wrapper with a single component
                    return wrapper.get(0);
                } else {
                    // return the child itself to remove wrapper pipelines
                    return self.get(0);
                }
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
        
        /**
         * Given that filter 'child' is the i'th to be fused in a
         * pipeline, returns if the resulting fused filter would be
         * stateless (assuming it is fused with a stateless filter
         * higher in the pipeline).
         */
        boolean statelessResult(SIRFilter child, int i) {
            return (// child must be stateless
                    !StatelessDuplicate.hasMutableState((SIRFilter)child) &&
                    // only first filter can peek
                    (i==0 || !doesPeeking((SIRFilter)child)));
        }

        /**
         * Returns whether or not 'filter' does any peeking (in either
         * init or steady stage).
         */
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

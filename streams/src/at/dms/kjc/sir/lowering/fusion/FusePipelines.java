package at.dms.kjc.sir.lowering.fusion;

//import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fission.*;

//import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * This class fuses sub-segments of pipelines with various
 * characteristics.
 *
 * Part of the motivation of this class was to increase the
 * granularity of stateless regions so that they can be more
 * effectively fissed for parallel execution.  To simplify things, it
 * only fuses stateless neighbors in pipelines.  The output of the
 * fusion should also be stateless.  If an entire pipeline is fused,
 * then it may be fused further with neighbors in a higher-level
 * pipeline.
 */
public class FusePipelines {
    /**
     * Mode indicating that pipeline sub-segments should be fused so
     * long as they are filters.
     */
    private static final int FUSE_PIPELINES_OF_FILTERS = 0;
    /**
     * Mode indicating that pipeline segments should be fused so long
     * as they are filters, and the RESULT of the fusion will be
     * stateless.  (Some stateless filters might not be fused, if they
     * peek.)
     */
    private static final int FUSE_PIPELINES_OF_STATELESS_FILTERS = 1;
    /**
     * Mode indicating that pipeline segments should be fused so long
     * as the RESULT of the fusion will be stateless.  This will do a
     * deep-fusion, possibly fusing splitjoin children.
     */
    private static final int FUSE_PIPELINES_OF_STATELESS_STREAMS = 2;
    /**
     * Mode indicating that pipeline segments should be fused so long 
     * as the RESULT of fusion will be naively vectorizable.
     */
    private static final int FUSE_PIPELINES_OF_VECTORIZABLE_FILTERS = 3;
    /**
     * Mode indicating that pipeline segments should be fused so long 
     * as the RESULT of fusion will be naively vectorizable.  This will do a
     * deep-fusion, possibly fusing splitjoin children.
     */
    private static final int FUSE_PIPELINES_OF_VECTORIZABLE_STREAMS = 4;
    
    /**
     * Fuses all adjacent filters whos parent is a pipeline.  If a
     * complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     * @param str input stream to fuse, may be modified by this method.
     * @return  stream with fusion having taken place
     */
    public static SIRStream fusePipelinesOfFilters(SIRStream str) {
        // first eliminate wrapper pipelines, as they obscure
        // continuous pipeline sections
        Lifter.lift(str);
        PipelineFuser fuser = new PipelineFuser(FUSE_PIPELINES_OF_FILTERS);
        return (SIRStream)str.accept(fuser);
    }

    /**
     * Fuses any two adjacent FILTERS in 'str' so long as:
     *  - the shared parent of the filter is a pipeline
     *  - the result of fusion will be stateless
     *
     * If a complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     * @param str input stream to fuse, may be modified by this method.
     * @return  stream with fusion having taken place
     */
    public static SIRStream fusePipelinesOfStatelessFilters(SIRStream str) {
        // first eliminate wrapper pipelines, as they obscure
        // continuous pipeline sections
        Lifter.lift(str);
        PipelineFuser fuser = new PipelineFuser(FUSE_PIPELINES_OF_STATELESS_FILTERS);
        return (SIRStream)str.accept(fuser);
    }

    /**
     * Fuses any two adjacent STREAMS in 'str' so long as:
     *  - the shared parent of the filter is a pipeline
     *  - the result of fusion will be stateless
     *
     * If a complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     * @param str input stream to fuse, may be modified by this method.
     * @return  stream with fusion having taken place
     * 
     */
    public static SIRStream fusePipelinesOfStatelessStreams(SIRStream str) {
        // first eliminate wrapper pipelines, as they obscure
        // continuous pipeline sections
        Lifter.lift(str);
        PipelineFuser fuser = new PipelineFuser(FUSE_PIPELINES_OF_STATELESS_STREAMS);
        SIRStream result = (SIRStream)str.accept(fuser);
        //fuser.predictFusion.debugPrint();
        new PredictFusion(); // clean up static data structures
        return result;
    }
    /**
     * Fuses any two adjacent FILTERS in 'str' so long as:
     *  - the shared parent of the filter is a pipeline
     *  - the result of fusion will be naively vectorizable.
     *
     * If a complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     * @param str input stream to fuse, may be modified by this method.
     * @return  stream with fusion having taken place
     */
    public static SIRStream fusePipelinesOfVectorizableFilters(SIRStream str) {
        // first eliminate wrapper pipelines, as they obscure
        // continuous pipeline sections
        Lifter.lift(str);
        PipelineFuser fuser = new PipelineFuser(FUSE_PIPELINES_OF_VECTORIZABLE_FILTERS);
        return (SIRStream)str.accept(fuser);
    }
    /**
     * Fuses any two adjacent STREAMS in 'str' so long as:
     *  - the shared parent of the filter is a pipeline
     *  - the result of fusion will be naively vectorizable.
     *
     * If a complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     * @param str input stream to fuse, may be modified by this method.
     * @return  stream with fusion having taken place
     */
    public static SIRStream fusePipelinesOfVectorizableStreams(SIRStream str) {
        // first eliminate wrapper pipelines, as they obscure
        // continuous pipeline sections
        Lifter.lift(str);
        PipelineFuser fuser = new PipelineFuser(FUSE_PIPELINES_OF_VECTORIZABLE_STREAMS);
        SIRStream retval = (SIRStream)str.accept(fuser);
        //fuser.predictFusion.debugPrint();
        new PredictFusion(); // clean up static data structures
        return retval;
    }
    
    /**
     * Utility function: returns whether or not 'filter' does any
     * peeking (in either init or steady stage).
     */
    private static boolean filterPeeks(SIRFilter filter) {
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

    // visitor to actually do the work
    static class PipelineFuser extends ReplacingStreamVisitor {
        /**
         * Execution mode that is set to a constant flag defined in
         * FusePipelines.
         */
        private int mode;
        /**
         * Tool needed to predict fusion in complex cases.
         */
        PredictFusion predictFusion;

        /**
         * Inputs an execution mode indicating what should be fused.
         */
        public PipelineFuser(int mode) {
            this.mode = mode;
            if (mode == FUSE_PIPELINES_OF_STATELESS_STREAMS
                    || mode == FUSE_PIPELINES_OF_VECTORIZABLE_STREAMS) {
                predictFusion = new PredictFusion();
            }
        }

        public Object visitPipeline(SIRPipeline self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init) {
            // first visit children
            self = (SIRPipeline)super.visitPipeline(self, fields, methods, init);

            // 1. DETERMINE WHERE TO FUSE:
            // walk through pipeline, determining who should be fused.
            // We fuse anyone where the result will be stateless 
            // (or whatever condition is specified by the mode)
            // This requires:
            // 1. original filters are stateless
            // 2. only the first filter can peek (otherwise peek
            //    buffer becomes state.)
            ArrayList<Integer> partitions = new ArrayList<Integer>();
            // position of start of current fusion segment
            int pos = 0; 
            // length of current fusion segment
            int count = 0;
            while (pos+count < self.size()) {
                SIRStream child = self.get(pos+count);
                if (canFuse(child, count)) {
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
                    partitions.add(count+1);
                // if we indicated some fusion, first make sure that
                // these children have been fused into filters
                if (count>0) {
                    for (int i=pos; i<pos+count+1; i++) {
                        fuseChild(self, i);
                    }
                }

                // reset counters
                pos = pos+count+1;
                count = 0;
            }

            // 2. PERFORM PIPELINE FUSION:
            if (partitions.size()==1) {
                // if we are fusing everything, call fuse all due to the
                // bad interface in FusePipe
                SIRPipeline wrapper = FuseAll.fuse(self);
                // should get back a wrapper with a single component
                SIRStream child = wrapper.get(0);
                // setup parent-child relationship
                child.setParent(wrapper.getParent());
                // check that fusion product stateless
                checkState((SIRFilter)child);
                return child;
            } else {
                // fuse the pipeline according to the recorded partitions
                int[] partitionArr = new int[partitions.size()];
                for (int i=0; i<partitionArr.length; i++) {
                    partitionArr[i] = partitions.get(i).intValue();
                }
                PartitionGroup pg = PartitionGroup.createFromArray(partitionArr);
                // return ourself, since we still have more than one
                // component (we were mutated by the fuser)
                FusePipe.fuse(self, pg);
                // check that products were stateless
                for (int i=0; i<partitionArr.length; i++) {
                    // if we fused something here
                    if (partitionArr[i]>1) {
                        checkState((SIRFilter)self.get(i));
                    }
                }
                return self;
            }
        }
        
        /**
         * Fuses the i'th child of 'pipe' into a single filter,
         * mutating 'pipe' to contain the fusion result.  This is
         * intended for cases where the i'th child is a stream
         * container; otherwise it is a no-op.
         */
        void fuseChild(SIRPipeline pipe, int i) {
            // I'm not sure that FuseAll.fuse removes the wrapper when
            // it mutates 'pipe', so I'm removing it myself
            SIRPipeline wrapper = FuseAll.fuse(pipe.get(i));
            SIRStream child = wrapper.get(0);
            // setup parent-child relationship
            pipe.set(i, child);
            child.setParent(pipe);
        }

        /**
         * Checks that a fusion product is stateless.
         */
        void checkState(SIRFilter filter) {
            // in the event of stateless fusion, make sure we do not
            // introduce state
            if (mode == FUSE_PIPELINES_OF_STATELESS_FILTERS ||
                mode == FUSE_PIPELINES_OF_STATELESS_STREAMS) {
                assert !StatelessDuplicate.hasMutableState(filter) :
                        "Accidentally introduced state into fusion product " + filter;
            }
        }

        /**
         * Given that stream 'child' is the i'th to be fused in a
         * pipeline, returns if the resulting fused filter would meet
         * the criteria for this.mode (would be correct, would be 
         * correct and stateless,  would be correct and vectorizable, 
         * etc...)
         */
        boolean canFuse(SIRStream child, int i) {
            switch(this.mode) {
            case FUSE_PIPELINES_OF_FILTERS:
                return (// can only fuse filters
                        child instanceof SIRFilter &&
                        // must be fusable
                        FusePipe.isFusable(child));
            case FUSE_PIPELINES_OF_STATELESS_FILTERS:
                return (// can only fuse filters
                        child instanceof SIRFilter &&
                        // must be fusable
                        FusePipe.isFusable(child) &&
                        // child must be stateless
                        !StatelessDuplicate.hasMutableState((SIRFilter)child) &&
                        // cannot be a two-stage filter (could introduce fusion state?)
                        !(child instanceof SIRTwoStageFilter) &&
                        // only first filter can peek
                        (i==0 || !FusePipelines.filterPeeks((SIRFilter)child)));
            case FUSE_PIPELINES_OF_STATELESS_STREAMS:
                return (// must be fusable
                        predictFusion.isFusable(child) &&
                        // must be stateless
                        !predictFusion.hasState(child) &&
                        // cannot be a two-stage filter (could introduce fusion state?)
                        !predictFusion.isTwoStage(child) &&
                        // only first filter can peek
                        (i==0 || !predictFusion.doesPeeking(child)));
            case FUSE_PIPELINES_OF_VECTORIZABLE_FILTERS:
                return (// can only fuse filters
                        child instanceof SIRFilter &&
                        // must be fusable
                        FusePipe.isFusable(child) &&
                        // may not be two-stage: certainly 
                        // filters after first can not be two stage
                        // since vectorization will change tapeType for
                        // work function.
                        (! (child instanceof SIRTwoStageFilter)) &&
                        // only first filter can peek
                        (i==0 || !FusePipelines.filterPeeks((SIRFilter)child)) &&
                        // and vectorizable...
                        Vectorizable.vectorizable((SIRFilter)child));
            case FUSE_PIPELINES_OF_VECTORIZABLE_STREAMS:
                return (// must be fusable
                        predictFusion.isFusable(child) &&
                        // must not be two-stage
                        !predictFusion.isTwoStage(child) &&
                        // only first filter can peek
                        (i==0 || !predictFusion.doesPeeking(child)) &&
                        ! predictFusion.hasVState(child));
            }
            assert false : "Unexpected mode";
            return false;
        }
    }

    /**
     * Predicts properties of the fusion output for streams in the
     * graph.  It does this by keeping track (hierarchically, via
     * memoization) of the predicted properties of fused filters.
     * Notes on how to maintain this info:
     *
     * - maintain:
     *   - isTwoStage
     *   - hasState
     *   - doesPeeking
     *   - isFusable
     *
     * - filter:  read directly
     *
     * - pipeline: 
     *    isTwoStage = any child is twoStage, or any child but first does peeking
     *    hasState = any child has state, or any child is twoStage, or any
     *      child but first does peeking
     *    doesPeeking = first child does peeking
     *    isFusable = all children are fusable
     *    isVectorizable = all children are vectorizable
     *
     * - splitjoin:
     *    - isTwoStage = any child is two stage
     *    - hasState = any child has state,
     *              or (roundrobin and child peeks)
     *              or (any child is two stage)
     *    - doesPeeking = any child peeks
     *    - isFusable = all children are fusable
     *    - isVectorizable = all children are vectorizable
     *
     * - feedbackloop:
     *   - can't fuse feedback loops, so return parameters that make them
     *     non-fusable:
     *   - isTwoStage = true
     *   - hasState = true
     *   - doesPeeking = true
     *   - isFusable = false
     *   - isVectorizable = false
     */
    static class PredictFusion {
        /**
         * Memoizes: if a given stream is fused, is there a
         * possibility that the result will be a two-stage filter?
         */
        private static HashMap<SIRStream, Boolean> isTwoStage;
        /**
         * Memoizes: if a given stream is fused, is there a possibility
         * that the result will be stateful?
         */
        private static HashMap<SIRStream, Boolean> hasState;
        /**
         * Memoizes: if a given stream is fused, is there a possibility
         * that the result will do peeking?
         */
        private static HashMap<SIRStream, Boolean> doesPeeking;
        /**
         * Memoizes: are all components of the given stream fusable?
         */
        private static HashMap<SIRStream, Boolean> isFusable;

        /**
         * Memoizes: are all components of the given stream vectorizable?
         */
        private static HashMap<SIRStream, Boolean> isUnVectorizable;

        /** 
         * Initialize / clean up static data structures.
         */
        public PredictFusion() {
            isTwoStage = new HashMap<SIRStream, Boolean>();
            hasState = new HashMap<SIRStream, Boolean>();
            doesPeeking = new HashMap<SIRStream, Boolean>();
            isFusable = new HashMap<SIRStream, Boolean>();
            isUnVectorizable = new HashMap<SIRStream, Boolean>();
        }

        /**
         * Returns: if a given stream is fused, is there a possibility
         * that the result will be a two-stage filter?
         */
        public boolean isTwoStage(SIRStream str) {
            // memoize
            if (isTwoStage.containsKey(str)) {
                return isTwoStage.get(str).booleanValue();
            }

            // otherwise compute
            boolean result;
            if (str instanceof SIRFilter) {
                // base case: test directly
                result = (str instanceof SIRTwoStageFilter);
            } else if (str instanceof SIRPipeline) {
                // any child is two stage, or any child but first does peeking
                result = false;
                SIRPipeline pipe = (SIRPipeline)str;
                for (int i=0; i<pipe.size(); i++) {
                    if (isTwoStage(pipe.get(i)) || 
                        (i>0 && doesPeeking(pipe.get(i)))) {
                        result = true;
                        break;
                    }
                }
            } else if (str instanceof SIRSplitJoin) {
                // any child is two stage
                result = false;
                SIRSplitJoin sj = (SIRSplitJoin)str;
                for (int i=0; i<sj.size(); i++) {
                    if (isTwoStage(sj.get(i))) {
                        result = true;
                        break;
                    }
                }
            } else {
                // feedback loops will never be fused; just count as
                // if they are two stage
                result = true;
            }

            // remember result
            isTwoStage.put(str, new Boolean(result));

            return result;
        }        
        
        /**
         * Returns: if a given stream is fused, is there a possibility
         * that the result will have state?
         */
        public boolean hasState(SIRStream str) {
            // memoize
            if (hasState.containsKey(str)) {
                return hasState.get(str).booleanValue();
            }

            // otherwise compute
            boolean result;
            if (str instanceof SIRFilter) {
                // base case: test directly
                result = StatelessDuplicate.hasMutableState((SIRFilter)str);
            } else if (str instanceof SIRPipeline) {
                // any child has state, or any child is twoStage, or
                // any child but first does peeking
                SIRPipeline pipe = (SIRPipeline)str;
                result = false;
                for (int i=0; i<pipe.size(); i++) {
                    SIRStream child = pipe.get(i);
                    if (hasState(child) ||
                        isTwoStage(child) ||
                        (i>0 && doesPeeking(child))) {
                        result = true;
                        break;
                    }
                }
            } else if (str instanceof SIRSplitJoin) {
                // any child has state, or any child is twoStage, or
                // (roundrobin and any child peeks)
                SIRSplitJoin sj = (SIRSplitJoin)str;
                result = false;
                boolean isRoundRobin = sj.getSplitter().getType().isRoundRobin();
                for (int i=0; i<sj.size(); i++) {
                    SIRStream child = sj.get(i);
                    if (hasState(child) ||
                        isTwoStage(child) ||
                        (isRoundRobin && doesPeeking(child))) {
                        result = true;
                        break;
                    }
                }
            } else {
                // feedback loops will never be fused; just count as
                // if they have state
                result = true;
            }

            // remember result
            hasState.put(str, new Boolean(result));

            return result;
        }        
   
        /**
         * Returns: if a given stream is fused, is there a possibility
         * that the result will not be vectorizable?
         */
        public boolean hasVState(SIRStream str) {
            // memoize
            if (isUnVectorizable.containsKey(str)) {
                return isUnVectorizable.get(str).booleanValue();
            }

            // otherwise compute
            boolean result;
            if (str instanceof SIRFilter) {
                // base case: test directly
                result = ! Vectorizable.vectorizable((SIRFilter)str);
            } else if (str instanceof SIRPipeline) {
                // any child has state, or any child is twoStage, or
                // any child but first does peeking
                SIRPipeline pipe = (SIRPipeline)str;
                result = false;
                for (int i=0; i<pipe.size(); i++) {
                    SIRStream child = pipe.get(i);
                    if (hasVState(child) ||
                        isTwoStage(child) ||
                        (i>0 && doesPeeking(child))) {
                        result = true;
                        break;
                    }
                }
            } else if (str instanceof SIRSplitJoin) {
                // any child has state, or any child is twoStage, or
                // (roundrobin and any child peeks)
                SIRSplitJoin sj = (SIRSplitJoin)str;
                result = false;
                boolean isRoundRobin = sj.getSplitter().getType().isRoundRobin();
                for (int i=0; i<sj.size(); i++) {
                    SIRStream child = sj.get(i);
                    if (hasVState(child) ||
                        isTwoStage(child) ||
                        (isRoundRobin && doesPeeking(child))) {
                        result = true;
                        break;
                    }
                }
            } else {
                // feedback loops will never be fused; just count as
                // if they have state
                result = true;
            }

            // remember result
            isUnVectorizable.put(str, new Boolean(result));

            return result;
        }        

        /**
         * Returns: if a given stream is fused, is there a possibility
         * that the result will do peeking?
         */
        public boolean doesPeeking(SIRStream str) {
            // memoize
            if (doesPeeking.containsKey(str)) {
                return doesPeeking.get(str).booleanValue();
            }

            // otherwise compute
            boolean result;
            if (str instanceof SIRFilter) {
                // base case: test directly
                result = FusePipelines.filterPeeks((SIRFilter)str);
            } else if (str instanceof SIRPipeline) {
                // pipeline peeks if first child peeks
                result = doesPeeking(((SIRPipeline)str).get(0));
            } else if (str instanceof SIRSplitJoin) {
                // splitjoin peeks if any child peeks
                result = false;
                SIRSplitJoin sj = (SIRSplitJoin)str;
                for (int i=0; i<sj.size(); i++) {
                    if (doesPeeking(sj.get(i))) {
                        result = true;
                        break;
                    }
                }
            } else {
                // feedback loops will never be fused; just count as
                // if they peek
                result = true;
            }

            // remember result
            doesPeeking.put(str, new Boolean(result));

            return result;
        }

        /**
         * Returns: are all components of the given stream fusable?
         */
        public boolean isFusable(SIRStream str) {
            // memoize
            if (isFusable.containsKey(str)) {
                return isFusable.get(str).booleanValue();
            }

            // otherwise compute
            boolean result;
            if (str instanceof SIRFilter) {
                // base case: test filters for fusable
                result = FusePipe.isFusable((SIRFilter)str);
            } else if (str instanceof SIRPipeline || str instanceof SIRSplitJoin) {
                // otherwise, container is fusable iff all of children
                // are fusable
                result = true;
                SIRContainer cont = (SIRContainer)str;
                for (int i=0; i<cont.size(); i++) {
                    if (!isFusable(cont.get(i))) {
                        result = false;
                        break;
                    }
                }
            } else {
                // feedback loops: not yet fusable
                result = false;
            }

            // remember result
            isFusable.put(str, new Boolean(result));

            return result;
        }
        
        /**
         * Print some debugging info.
         */
        public void debugPrint() {
            // print two stage
            for (Iterator<SIRStream> it = isTwoStage.keySet().iterator(); it.hasNext(); ) {
                SIRStream str = it.next();
                System.err.println("isTwoStage: " + str + " = " + isTwoStage.get(str));
            }
            // print has state
            for (Iterator<SIRStream> it = hasState.keySet().iterator(); it.hasNext(); ) {
                SIRStream str = it.next();
                System.err.println("hasState: " + str + " = " + hasState.get(str));
            }
            // print has vector state
            for (Map.Entry<SIRStream,Boolean> strEntry : isUnVectorizable.entrySet()) {
                System.err.println("isUnVectorizable: " + strEntry.getKey() + " = " + hasState.get(strEntry.getValue()));
            }
            // print does peeking
            for (Iterator<SIRStream> it = doesPeeking.keySet().iterator(); it.hasNext(); ) {
                SIRStream str = it.next();
                System.err.println("doesPeeking: " + str + " = " + doesPeeking.get(str));
            }
            // print is fusable
            for (Iterator<SIRStream> it = isFusable.keySet().iterator(); it.hasNext(); ) {
                SIRStream str = it.next();
                System.err.println("isFusable: " + str + " = " + isFusable.get(str));
            }
        }
    }
}

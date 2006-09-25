package at.dms.kjc.cluster;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;

/**
 * Calculate buffer sizes for an edge (a tape) that uses a fixed-length buffer.
 * <br/>
 * Calculates buffer size.  
 * Separately, calculates extra size needed for peeks (as int, boolean).
 * Separately calculates whether to use modular buffers.
 * <br/> TODO: determine if ever need size for init and size for steady separately.
 * <br/>This code is specific to the cluster backend.  To generalize would have
 * to make all backends inherit from a common ancestor defining initExecutionCounts and steadyExecutionCounts:
 * doable, but not today.
 * @author allyn from code by mike for rstream
 *
 */
public class FixedBufferTape {
    
    /**
     * Determine whether a tape is is "fused".
     * <br/> This version relies totally on the KjcOptions.standalone flag for its determination.
     * @param src the NetStream number of a SIROperator at source of tape
     * @param dst the NetStream number of a SIROperator at destination of tape
     * @return whether tape uses a "fused" buffer: a linear or modular buffer
     */

    static public boolean isFixedBuffer(int src, int dst) {
        return KjcOptions.standalone;
    }
    
    /**
     * Get execution multiplicity of a FlatNode.
     * <br/> Implicit parameters {@link ClusterBackend#initExecutionCounts}, {@link ClusterBackend#steadyExecutionCounts}
     * @param node : a FlatNode.
     * @param init : true to get init multiplicity, false for work multiplicity
     * @return requested multiplicity or 0 if not found.
     */
    private static int getMult(FlatNode node, boolean init) 
    {
        Integer mult;
        if (init) 
            mult = ClusterBackend.initExecutionCounts.get(node);
        else 
            mult = ClusterBackend.steadyExecutionCounts.get(node);

        if (mult == null) {
            return 0;
        }
    
        return mult.intValue();
    }
   
    /**
     * calculate required size of a tape buffer for an edge.
     * <br/>stolen from mikes code in descendants of rstream.FusionState,
     * theaked to handle TwoStageFilter's
     * @param src upstream FlatNode
     * @param dst downstream FlatNode
     * @return necessary size of buffer in items.
     */
    static private int calcBufferSize(FlatNode src, FlatNode dst) {
        if (src == null || dst == null)
            return 0;
        int size = 0;
        if (dst.isFilter())        size = sizeFilter(src, dst);
        else if (dst.isSplitter()) size = sizeSplitter(src, dst);
        else if (dst.isJoiner())   size = sizeJoiner(src, dst);
        else { assert false; size = -1; }
        return size;
    }
    
    // for a filter: max number of items popped in init or steady phases.
    static private int sizeFilter(FlatNode src, FlatNode dst) {
        SIRFilter f = (SIRFilter)dst.contents;
        // size of init, steady stages
        int initSize, steadySize;
        // calculate initSize
        int mult = getMult(dst,true);
        initSize = mult * f.getPopInt();
        if (f instanceof SIRTwoStageFilter) {
            // must fire in init stage if we're a two-stage filter
            assert mult > 0;
            // first firing is prework, not work
            initSize = ( initSize 
                         + ((SIRTwoStageFilter)f).getInitPopInt() 
                         - f.getPopInt() );
        }
        // calculate steadySize
        steadySize = getMult(dst,false) * f.getPopInt();
 
        // the maximum number of items popped in init or steady phases
        int size = Math.max(initSize, steadySize);

        return size + leftoveritems(src,dst);
    }
   
    // for a splitter: max iterations in init or steady * total weight (if roundrobin)
    //  or * 1 (if duplicate).
    static private int sizeSplitter(FlatNode src, FlatNode dst) {
        // the maximum number of items popped in init or steady phases
        int size = Math.max(getMult(dst,true), getMult(dst,false)) 
                    * distinctRoundRobinItems(dst);
        return size + leftoveritems(src,dst);
    }
    
    // for a joiner: max iterations in init or steady * incoming weight.
    // for a feedback edge, take max with number or enqueued items.
    static private int sizeJoiner(FlatNode src, FlatNode dst) {
        int incomingWeight = dst.getIncomingWeight(src);  // or error if no connection.
        int size =  Math.max(getMult(dst,true), getMult(dst,false)) 
                     *  incomingWeight;
        int enqueued = 0;
        if (dst.isFeedbackJoiner() && src == dst.incoming[1]) {
            // if feedback edge on feedback loop then account for enqueued values
            enqueued = ((SIRFeedbackLoop)((SIRJoiner)dst.contents).getParent())
                            .getDelayInt();
        }
        // Buffer has to have room for enqueued values, but will pop
        // incomingWeight of these on first iteration.
        return Math.max(size, enqueued);
    }
     
    /**
     * {@link #getRemaining(int, int)} for FlatNode's. 
     * @param src : upstream FlatNode
     * @param dst : downstream FlatNode
     * @return max number of items that may be on tape before first (or any) execution of work phase.
     */
    // assume: either src is null or dst is null (resulting in value of 0)
    // or src is connected to dst (resulting in calculated value)
    // else (no such edge) likely assertion error from some called method.
    //
    // if result < 0 then there is a problem with the push / pop / multiplicity
    // or weight data so assert that there is an error.
    static private int leftoveritems (FlatNode src, FlatNode dst) {
       if (src == null || dst == null) return 0;
       int remaining;
       if (dst.isFilter())        remaining = leftoveritemsFilter(src,dst);
       else if (dst.isSplitter()) remaining = leftoveritemsSplitter(src,dst);
       else if (dst.isJoiner())   remaining = leftoveritemsJoiner(src,dst);
       else {assert false; remaining = -1;}
       
       return remaining;
    }

    /**
     * Returns the number of items pushed from <src> to <dst> during
     * the initialization stage.
     */
    static private int getInitItemsPushed(FlatNode src, FlatNode dst) {
        int mult = getMult(src, true);
        if (src.contents instanceof SIRTwoStageFilter) {
            SIRTwoStageFilter t = (SIRTwoStageFilter)src.contents;
            assert mult > 0;
            // replace one steady-state firing with the prework firing
            return t.getInitPushInt() + (mult-1) * t.getPushInt();
        } else {
            // otherwise use the steady-state firing everywhere
            return mult * FlatNode.getItemsPushed(src, dst);
        }
    }

   // filter: leftover that must be accounted for =
   // number produced by all iterations of init at src 
   //   - number consumed by all iterations of init at dst.
    static private int leftoveritemsFilter(FlatNode src, FlatNode dst) {
        assert dst.incoming[0] == src;
        SIRFilter f = (SIRFilter)dst.contents;
        int dstConsume = getMult(dst, true) * f.getPopInt();
        // if two stage filter, then count one execution as prework
        // instead of work
        if (f instanceof SIRTwoStageFilter) {
            SIRTwoStageFilter t = (SIRTwoStageFilter)f;
            dstConsume = dstConsume + t.getInitPopInt() - t.getPopInt();
        }
        int srcProduce = getInitItemsPushed(src,dst);
        int remaining = srcProduce - dstConsume;
        // assertion should be true since if work peeks more than it pops then
        // it requires an init phase.  (However, some dynamic rate managment policies
        // will cause this to be trifferred trying to estimate the initial size
        // of a buffer for dynamic rates.
        assert (remaining >= f.getPeekInt() - f.getPopInt()) && remaining >= 0 :
            remaining +"," + f.getPeekInt() + "," + f.getPopInt();
        return remaining;
    }

    // splitter: leftover that must be accounted for = 
    // number produced by all iterations of init at src 
    //  - number passed on by splitter * iterations of splitter.
    // assumes dst.isSplitter()
    static private int leftoveritemsSplitter(FlatNode src, FlatNode dst) {
        assert dst.incoming[0] == src;
        int dstConsume = getMult(dst, true) * distinctRoundRobinItems(dst);
        int srcProduce = getInitItemsPushed(src,dst);
        int remaining = srcProduce - dstConsume;
        assert remaining >= 0;
        return remaining;
    }
    
    // joiner: leftover that must be accounted for = 
    // number produced by all iterations of init at src 
    // + number produced by enqueues if dst is joiner for feedback loop
    // - number of init iterations of joiner * weight of edge.
    // assumes dst.isJoiner()
    static private int leftoveritemsJoiner(FlatNode src, FlatNode dst) {
        int incomingWeight = dst.getIncomingWeight(src);  // or error if no connection.
        int dstConsume = getMult(dst, true) * incomingWeight;
        int srcProduce = getInitItemsPushed(src,dst);
        int enqueued = 0;
        if (dst.isFeedbackJoiner() && src == dst.incoming[1]) {
            // if feedback edge on feedback loop then account for enqueued values
            enqueued = ((SIRFeedbackLoop)((SIRJoiner)dst.contents).getParent())
                            .getDelayInt();
        }
        int remaining = (srcProduce + enqueued) - dstConsume;
        assert remaining >= 0;
        return remaining;
    }
    
    /** 
     * determine number of items splitter buffers in one iteration of work.
     * <br/>assumes splitter.contents instanceof SIRSplitter.  GIGO.
     * @param splitter : a splitter
     * @return number of items splitter buffers
     */
    static int distinctRoundRobinItems(FlatNode splitter) {
        return (splitter.isDuplicateSplitter() ? 1 : splitter.getTotalOutgoingWeights());
    }
        
    /**
     * Determine the size of largest number of items that may be on a tape before an execution of
     * a work function.
     * @param src the NetStream number of a SIROperator for the source of the tape
     * @param dst the NetStream number of a SIROperator for the destination of the tape
     * @return max(peeks-pops, 0) for a filter, 0 otherwise, for a joiner: number of enqueues not processed in first execution.
     */
    static public int getRemaining(int src, int dst) {
        int remaining = leftoveritems(NodeEnumerator.getFlatNode(src),
                                      NodeEnumerator.getFlatNode(dst));
        return remaining;
    }
        

    /**
     * Return whether a tape buffer should be modular based on destination.
     * <br/>I am not sure that I understand Janis' reasoning here.
     * A buffer needs to be modular if both (1) KjcOptions.modfusion is set
     * and (2) if the number of peeks in the dst work function is != the number of 
     * pops in the dst work function.
     * @param src the NetStream number of a SIROperator for the source of the tape
     * @param dst the NetStream number of a SIROperator for the destination of the tape
     * @return trus of buffer should be modular, false otherwise.
     */
    static public boolean needsModularBuffer (int src, int dst) {
        if (! KjcOptions.modfusion) { return false; }
        SIROperator dst_oper = NodeEnumerator.getOperator(dst);
        if (dst_oper instanceof SIRFilter) {
            SIRFilter f = (SIRFilter)dst_oper;
            if (f.getPeekInt() != f.getPopInt()) { return true; }
        }
        return false;
    }
    
    /**
     * Static method to calculate a buffer size for standalone mode.
     * <br/>
     * Retrieve the calculated info using getters.
     * <br.>Implicit parameters are {@link ClusterBackend.initExecutionCounts} and {@link ClusterBackend.steadyExecutionCounts}
     * @param src from getSource on a NetStream
     * @param dst from getDest on the same NetStream
     * @param p  a CodeGenPrintWriter for generating comments or null.
     * @param printComments whether to generate comments: should be false if p is null.
     * @return max number of items that will be bufferred on this edge. 
     */
    static public int bufferSize (int src, int dst,
            CodegenPrintWriter p, boolean printComments) {
        // printing comments is an artifact from Janis' code that this replaces.
        int size = calcBufferSize(NodeEnumerator.getFlatNode(src),NodeEnumerator.getFlatNode(dst));

        return size;
    }
        
}

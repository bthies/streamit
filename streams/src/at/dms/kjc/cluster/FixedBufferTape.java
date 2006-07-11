package at.dms.kjc.cluster;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;

/**
 * Calculate buffer sizes for an edge in standalone mode.
 * <br/>
 * Calculates sizes of buffers for init and steady state.  
 * Separately, calculates extra size needed for peeks (as int, boolean).
 * Separately calculates whether to use modular buffers.
 * <br/> TODO: determine if ever need size for init and size for steady separately.
 * @author janis (pulled out of line from Fusioncode by Allyn)
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
    
    static private int init_items;   // max number of items in buffer in init
    static private int steady_items; // max number of items in buffer in steady state

    /**
     * Return size of init buffer in items. 
     * <br/> Call only after {@link #calcSizes()}
     * @return size
     */
    static public int getInitItems() {return init_items;}

    /**
     * Return size of steady state buffer in items. 
     * <br/> Call only after {@link #calcSizes()}
     * @return size
     */
    static public int getSteadyItems() {return steady_items;}

    /**
     * Determine the need for an extra peek buffer for a tape based on the destination.
     *<br/>TODO: May want to remove use of this since complexity is same as {@link getExtraPeeks(dst)}.
      * @param src the NetStream number of a SIROperator for the source of the tape
     * @param dst the NetStream number of a SIROperator for the destination of the tape
     * @return true if the SIROperator is a filter that peeks morethan it pops.
     */
    static public boolean hasExtraPeeks(int src, int dst) {
        return getExtraPeeks(src, dst) > 0;
    }
    
    /**
     * Determine the size size of an extra peek buffer.for a tape based on the destination
     * @param src the NetStream number of a SIROperator for the source of the tape
     * @param dst the NetStream number of a SIROperator for the destination of the tape
     * @return max(peeks-pops, 0) for a filter, 0 otherwise, for a joiner: number of enqueues not processed in first execution.
     */
    static public int getExtraPeeks(int src, int dst) {
        SIROperator dst_oper = NodeEnumerator.getOperator(dst);
        if (dst_oper instanceof SIRFilter) {
            SIRFilter f = (SIRFilter)dst_oper;
            return Math.max(f.getPeekInt() - f.getPopInt(), 0);
        } else if (dst_oper instanceof SIRJoiner) {
            SIRJoiner j = (SIRJoiner)dst_oper;
            SIROperator p1 = j.getParent();
            if (! (p1 instanceof SIRFeedbackLoop)) {
                return 0;
            }
            SIRFeedbackLoop p = (SIRFeedbackLoop)p1;
            if (! (NodeEnumerator.getFlatNode(src) == NodeEnumerator.getFlatNode(dst).incoming[1])) {
                return 0;
            }
            int enqueued = p.getDelayInt();
            Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(dst_oper);
            int steady_int = 0;
            if (steady != null) { steady_int = (steady).intValue(); }
            return Math.max(enqueued - steady_int * j.getWeight(1), 0);
        } else {
            return 0;
        }
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
     * @param src from getSource on a NetStream
     * @param dst from getDest on the same NetStream
     * @param p  a CodeGenPrintWriter for generating comments or null.
     * @param printComments whether to generate comments: should be false if p is null.
     */
    static public void calcSizes (int src, int dst,
            CodegenPrintWriter p, boolean printComments) {
        

        SIROperator src_oper = NodeEnumerator.getOperator(src);
        SIROperator dst_oper = NodeEnumerator.getOperator(dst);

        /*
         * Set a maximum buffer size for a connection
         */
        int source_init_items = 0;
        int source_steady_items = 0;
        int dest_init_items = 0;
        int dest_steady_items = 0;

        // steady state, source

        if (src_oper instanceof SIRJoiner) {
            SIRJoiner j = (SIRJoiner)src_oper;
            Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(src));
            int steady_int = 0;
            if (steady != null) { steady_int = (steady).intValue(); }
            int push_n = j.getSumOfWeights();
            int total = (steady_int * push_n);
            if (printComments) p.print("//source pushes: "+total+" items during steady state\n");

            source_steady_items = total;
        }

        if (src_oper instanceof SIRFilter) {
            SIRFilter f = (SIRFilter)src_oper;
            Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(src));
            int steady_int = 0;
            if (steady != null) { steady_int = (steady).intValue(); }
            int push_n = f.getPushInt();
            int total = (steady_int * push_n);
            if (printComments) p.print("//source pushes: "+total+" items during steady state\n");

            source_steady_items = total;
        }

        // init sched, source

        if (src_oper instanceof SIRJoiner) {
            SIRJoiner j = (SIRJoiner)src_oper;
            Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(src));
            int init_int = 0;
            if (init != null) { init_int = (init).intValue(); }
            int push_n = j.getSumOfWeights();
            int total = (init_int * push_n);
            if (printComments) p.print("//source pushes: "+total+" items during init schedule\n");

            source_init_items = total;
        }

        if (src_oper instanceof SIRFilter) {
            SIRFilter f = (SIRFilter)src_oper;
            Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(src));
            int init_int = 0;
            if (init != null) { init_int = (init).intValue(); }
            int push_n = f.getPushInt();
            int total = (init_int * push_n);
            if (printComments) p.print("//source pushes: "+total+" items during init schedule\n");

            source_init_items = total;
        }


        // steady state, dest

        if (dst_oper instanceof SIRFilter) {
            SIRFilter f = (SIRFilter)dst_oper;
            Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
            int steady_int = 0;
            if (steady != null) { steady_int = (steady).intValue(); }
            int pop_n = f.getPopInt();
            int total = (steady_int * pop_n);
            if (printComments) p.print("//destination pops: "+total+" items during steady state\n");    
            dest_steady_items = total;
        }

        if (dst_oper instanceof SIRSplitter) {
            SIRSplitter s = (SIRSplitter)dst_oper;
            Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
            int steady_int = 0;
            if (steady != null) { steady_int = (steady).intValue(); }
            int pop_n = s.getSumOfWeights();
            if (s.getType().isDuplicate()) pop_n = 1;
            int total = (steady_int * pop_n);
            if (printComments) p.print("//destination pops: "+total+" items during steady state\n");
            dest_steady_items = total;
        }

        // init sched, dest

        if (dst_oper instanceof SIRFilter) {
            SIRFilter f = (SIRFilter)dst_oper;
            Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
            int init_int = 0;
            if (init != null) { init_int = (init).intValue(); }
            int pop_n = f.getPopInt();
            int total = (init_int * pop_n);
            if (printComments) p.print("//destination pops: "+total+" items during init schedule\n");    
            dest_init_items = total;
        }

        if (dst_oper instanceof SIRSplitter) {
            SIRSplitter s = (SIRSplitter)dst_oper;
            Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
            int init_int = 0;
            if (init != null) { init_int = (init).intValue(); }
            int pop_n = s.getSumOfWeights();
            if (s.getType().isDuplicate()) pop_n = 1;
            int total = (init_int * pop_n);
            if (printComments) p.print("//destination pops: "+total+" items during init_schedule\n");
            dest_init_items = total;
        }
        
        if (dst_oper instanceof SIRJoiner) {
            SIRJoiner j = (SIRJoiner)dst_oper;
            SIRStream parent = j.getParent();
            if (parent instanceof SIRFeedbackLoop) {
                FlatNode srcNode = NodeEnumerator.getFlatNode(src);
                FlatNode dstNode = NodeEnumerator.getFlatNode(dst);
                FlatNode[] incoming = dstNode.incoming;
                // if this is the tape from the loop back to feedbackloop joiner
                if (incoming.length == 2 && srcNode == incoming[1]) {
                    SIRFeedbackLoop f = (SIRFeedbackLoop)parent;
                    // then add in number of enqueued items.
                    dest_init_items = source_init_items + f.getDelayInt();
                    if (printComments) p.println("//destiniation enqueues: " + f.getDelayInt() + " items during init_schedule");
                }
            }
        }

        steady_items = source_steady_items> dest_steady_items ? source_steady_items 
                : dest_steady_items;
        init_items = source_init_items > dest_init_items ? source_init_items
                : dest_init_items;
    }
}

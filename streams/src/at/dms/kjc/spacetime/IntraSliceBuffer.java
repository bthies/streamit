package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.FilterInfo;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.Util;

/**
 * This class represents the buffer between the sink filter of a slice
 * and outputslicenode or between the inputslicenode and the source filter of a
 * slice. 
 * 
 * @author mgordon
 *
 */
public class IntraSliceBuffer extends OffChipBuffer {
    /** true if this buffer uses static net */
    protected boolean staticNet;
    
    public static IntraSliceBuffer getBuffer(FilterSliceNode src,
                                             OutputSliceNode dst) {
        return getBufferSrcDst(src,dst);
    }

    public static IntraSliceBuffer getBuffer(InputSliceNode src,
                                             FilterSliceNode dst) {
        return getBufferSrcDst(src,dst);
    }
    
    private static IntraSliceBuffer getBufferSrcDst(SliceNode src, SliceNode dst) {
        Edge e = Util.srcDstToEdge(src, dst);
        if (!bufferStore.containsKey(e)) {
            //System.out.println("Creating Buffer from " + src + " to " + dst);
            bufferStore.put(e, new IntraSliceBuffer(e));
        }
        IntraSliceBuffer retval = (IntraSliceBuffer) bufferStore.get(e);
        return retval;
     }

    private IntraSliceBuffer (Edge e) {
        super(e);
        calculateSize();
    }
    
    /**
     * @return Returns true if this buffer uses staticNet.
     */
    public boolean isStaticNet() {
        return staticNet;
    }

    /**
     * @param staticNet The staticNet to set.
     */
    public void setStaticNet(boolean staticNet) {
        this.staticNet = staticNet;
        //perform some sanity checks
        if (!staticNet) {
            if (isInterSlice()) {
                OutputSliceNode output = (OutputSliceNode)this.getDest();
                InputSliceNode input = (InputSliceNode)this.getSource();
                assert (output.oneOutput() || output.noOutputs()) &&
                    (input.noInputs() || input.oneInput()) : 
                        this.toString() + " cannot use the gdn unless it is a singleton.";
            }
        }
    }

  
    public boolean redundant() {
        // if there are no outputs for the output slice
        // then redundant
        if (theEdge.getSrc().isFilterSlice() && theEdge.getDest().isOutputSlice()) {
            if (((OutputSliceNode) theEdge.getDest()).noOutputs())
                return true;
        } else
            // if the inputslice is not necessray
            return unnecessary((InputSliceNode) theEdge.getSrc());
        return false;
    }

    public OffChipBuffer getNonRedundant() {
        if (theEdge.getSrc().isInputSlice() && theEdge.getDest().isFilterSlice()) {
            // if no inputs return null
            if (((InputSliceNode) theEdge.getSrc()).noInputs())
                return null;
            // if redundant get the previous buffer and call getNonRedundant
            if (redundant())
                return InterSliceBuffer.getBuffer(
                                                  ((InputSliceNode) theEdge.getSrc()).getSingleEdge())
                    .getNonRedundant();
            // otherwise return this...
            return this;
        } else { // (theEdge.getSrc().isFilterSlice() && theEdge.getDest().isOutputSlice())
            // if no outputs return null
            if (((OutputSliceNode) theEdge.getDest()).noOutputs())
                return null;
            // the only way it could be redundant (unnecesary) is for there to
            // be no outputs
            return this;
        }
    }

    protected void calculateSize() {
        // we'll make it 32 byte aligned
        if (theEdge.getSrc().isFilterSlice()) {
            // the init size is the max of the multiplicities for init and pp
            // times the push rate
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getSrc());
            int maxItems = fi.initMult;
            maxItems *= fi.push;
            // account for the initpush
            if (fi.push < fi.prePush)
                maxItems += (fi.prePush - fi.push);
            maxItems = Math.max(maxItems, fi.push*fi.steadyMult);
            // steady is just pop * mult
            sizeSteady = (Address.ZERO.add(maxItems)).add32Byte(0);
        } else if (theEdge.getDest().isFilterSlice()) {
            // this is not a perfect estimation but who cares
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getDest());
            int maxItems = fi.initMult;
            maxItems *= fi.pop;
            // now account for initpop, initpeek, peek
            maxItems += (fi.prePeek + fi.prePop + fi.prePeek);
            maxItems = Math.max(maxItems, fi.pop* fi.steadyMult);
            // steady is just pop * mult
            sizeSteady = (Address.ZERO.add(maxItems)).add32Byte(0);
        }
    }

    /**
     * @param t a slice.
     * @return The intrasliceBuffer between the last filter and the outputslicenode
     */
    public static IntraSliceBuffer getDstIntraBuf(Slice t) {
        return getBuffer(t.getTail().getPrevFilter(), t.getTail());
    }

    /**
     * @param t a slice.
     * @return The intraslicebuffer between the inputslicenode 
     * and the first filterslicenode
     */
    public static IntraSliceBuffer getSrcIntraBuf(Slice t) {
        return getBuffer(t.getHead(), t.getHead().getNextFilter());
    }

}

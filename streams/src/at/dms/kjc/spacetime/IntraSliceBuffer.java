package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.*;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;

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
        if (!bufferStore.containsKey(src)) {
            //System.out.println("Creating Buffer from " + src + " to " + dst);
            bufferStore.put(src, new IntraSliceBuffer(src, dst));
        }
        assert (((IntraSliceBuffer) bufferStore.get(src)).getDest() == dst) : "Src connected to different dst in buffer store";

        return (IntraSliceBuffer) bufferStore.get(src);
    }

    public static IntraSliceBuffer getBuffer(InputSliceNode src,
                                             FilterSliceNode dst) {
        if (!bufferStore.containsKey(src)) {
            bufferStore.put(src, new IntraSliceBuffer(src, dst));
            //System.out.println("Creating Buffer from " + src + " to " + dst);
        }
        assert (((IntraSliceBuffer) bufferStore.get(src)).getDest() == dst) : "Src connected to different dst in buffer store";

        return (IntraSliceBuffer) bufferStore.get(src);
    }

    protected IntraSliceBuffer(InputSliceNode src, FilterSliceNode dst) {
        super(src, dst);
        calculateSize();
    }

    protected IntraSliceBuffer(FilterSliceNode src, OutputSliceNode dst) {
        super(src, dst);
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
        if (source.isFilterSlice() && dest.isOutputSlice()) {
            if (((OutputSliceNode) dest).noOutputs())
                return true;
        } else
            // if the inputslice is not necessray
            return unnecessary((InputSliceNode) source);
        return false;
    }

    public OffChipBuffer getNonRedundant() {
        if (source.isInputSlice() && dest.isFilterSlice()) {
            // if no inputs return null
            if (((InputSliceNode) source).noInputs())
                return null;
            // if redundant get the previous buffer and call getNonRedundant
            if (redundant())
                return InterSliceBuffer.getBuffer(
                                                  ((InputSliceNode) source).getSingleEdge())
                    .getNonRedundant();
            // otherwise return this...
            return this;
        } else { // (source.isFilterSlice() && dest.isOutputSlice())
            // if no outputs return null
            if (((OutputSliceNode) dest).noOutputs())
                return null;
            // the only way it could be redundant (unnecesary) is for there to
            // be no outputs
            return this;
        }
    }

    protected void setType() {
        if (source.isFilterSlice())
            type = ((FilterSliceNode) source).getFilter().getOutputType();
        else if (dest.isFilterSlice())
            type = ((FilterSliceNode) dest).getFilter().getInputType();
    }

    protected void calculateSize() {
        // we'll make it 32 byte aligned
        if (source.isFilterSlice()) {
            // the init size is the max of the multiplicities for init and pp
            // times the push rate
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) source);
            int maxItems = fi.initMult;
            maxItems *= fi.push;
            // account for the initpush
            if (fi.push < fi.prePush)
                maxItems += (fi.prePush - fi.push);
            maxItems = Math.max(maxItems, fi.push*fi.steadyMult);
            // steady is just pop * mult
            sizeSteady = (Address.ZERO.add(maxItems)).add32Byte(0);
        } else if (dest.isFilterSlice()) {
            // this is not a perfect estimation but who cares
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) dest);
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

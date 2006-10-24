package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.*;
import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.InputTraceNode;
import at.dms.kjc.slicegraph.OutputTraceNode;

/**
 * This class represents the buffer between the sink filter of a trace
 * and outputtracenode or between the inputtracenode and the source filter of a
 * trace. 
 * 
 * @author mgordon
 *
 */
public class IntraTraceBuffer extends OffChipBuffer {
    /** true if this buffer uses static net */
    protected boolean staticNet;
    
    public static IntraTraceBuffer getBuffer(FilterTraceNode src,
                                             OutputTraceNode dst) {
        if (!bufferStore.containsKey(src)) {
            //System.out.println("Creating Buffer from " + src + " to " + dst);
            bufferStore.put(src, new IntraTraceBuffer(src, dst));
        }
        assert (((IntraTraceBuffer) bufferStore.get(src)).getDest() == dst) : "Src connected to different dst in buffer store";

        return (IntraTraceBuffer) bufferStore.get(src);
    }

    public static IntraTraceBuffer getBuffer(InputTraceNode src,
                                             FilterTraceNode dst) {
        if (!bufferStore.containsKey(src)) {
            bufferStore.put(src, new IntraTraceBuffer(src, dst));
            //System.out.println("Creating Buffer from " + src + " to " + dst);
        }
        assert (((IntraTraceBuffer) bufferStore.get(src)).getDest() == dst) : "Src connected to different dst in buffer store";

        return (IntraTraceBuffer) bufferStore.get(src);
    }

    protected IntraTraceBuffer(InputTraceNode src, FilterTraceNode dst) {
        super(src, dst);
        calculateSize();
    }

    protected IntraTraceBuffer(FilterTraceNode src, OutputTraceNode dst) {
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
            if (isInterTrace()) {
                OutputTraceNode output = (OutputTraceNode)this.getDest();
                InputTraceNode input = (InputTraceNode)this.getSource();
                assert (output.oneOutput() || output.noOutputs()) &&
                    (input.noInputs() || input.oneInput()) : 
                        this.toString() + " cannot use the gdn unless it is a singleton.";
            }
        }
    }

  
    public boolean redundant() {
        // if there are no outputs for the output trace
        // then redundant
        if (source.isFilterTrace() && dest.isOutputTrace()) {
            if (((OutputTraceNode) dest).noOutputs())
                return true;
        } else
            // if the inputtrace is not necessray
            return unnecessary((InputTraceNode) source);
        return false;
    }

    public OffChipBuffer getNonRedundant() {
        if (source.isInputTrace() && dest.isFilterTrace()) {
            // if no inputs return null
            if (((InputTraceNode) source).noInputs())
                return null;
            // if redundant get the previous buffer and call getNonRedundant
            if (redundant())
                return InterTraceBuffer.getBuffer(
                                                  ((InputTraceNode) source).getSingleEdge())
                    .getNonRedundant();
            // otherwise return this...
            return this;
        } else { // (source.isFilterTrace() && dest.isOutputTrace())
            // if no outputs return null
            if (((OutputTraceNode) dest).noOutputs())
                return null;
            // the only way it could be redundant (unnecesary) is for there to
            // be no outputs
            return this;
        }
    }

    protected void setType() {
        if (source.isFilterTrace())
            type = ((FilterTraceNode) source).getFilter().getOutputType();
        else if (dest.isFilterTrace())
            type = ((FilterTraceNode) dest).getFilter().getInputType();
    }

    protected void calculateSize() {
        // we'll make it 32 byte aligned
        if (source.isFilterTrace()) {
            // the init size is the max of the multiplicities for init and pp
            // times the push rate
            FilterInfo fi = FilterInfo.getFilterInfo((FilterTraceNode) source);
            int maxItems = fi.initMult;
            maxItems *= fi.push;
            // account for the initpush
            if (fi.push < fi.prePush)
                maxItems += (fi.prePush - fi.push);
            maxItems = Math.max(maxItems, fi.push*fi.steadyMult);
            // steady is just pop * mult
            sizeSteady = (Address.ZERO.add(maxItems)).add32Byte(0);
        } else if (dest.isFilterTrace()) {
            // this is not a perfect estimation but who cares
            FilterInfo fi = FilterInfo.getFilterInfo((FilterTraceNode) dest);
            int maxItems = fi.initMult;
            maxItems *= fi.pop;
            // now account for initpop, initpeek, peek
            maxItems += (fi.prePeek + fi.prePop + fi.prePeek);
            maxItems = Math.max(maxItems, fi.pop* fi.steadyMult);
            // steady is just pop * mult
            sizeSteady = (Address.ZERO.add(maxItems)).add32Byte(0);
        }
    }

}
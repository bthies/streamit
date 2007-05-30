package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.ProcessInputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.InputSliceNode;

public class CellProcessInputSliceNode extends ProcessInputSliceNode {

    public CellProcessInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        super(inputNode, whichPhase, backEndBits);
    }
}

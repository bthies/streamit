package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.ProcessOutputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.OutputSliceNode;

public class CellProcessOutputSliceNode extends ProcessOutputSliceNode {

    public CellProcessOutputSliceNode(OutputSliceNode outputNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        super(outputNode, whichPhase, backEndBits);
    }
}

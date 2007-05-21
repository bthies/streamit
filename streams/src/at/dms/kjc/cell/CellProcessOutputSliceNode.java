package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.ProcessOutputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.OutputSliceNode;

public class CellProcessOutputSliceNode extends ProcessOutputSliceNode {

    private CellProcessOutputSliceNode(OutputSliceNode outputNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        super(outputNode, whichPhase, backEndBits);
    }
    
    public static void processOutputSliceNode(OutputSliceNode outputNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        // have an instance so we can override methods.
        CellProcessOutputSliceNode self = new CellProcessOutputSliceNode(outputNode,whichPhase,backEndBits);
        self.doit();
    }
    
    @Override
    protected void additionalInitProcessing() {
        
    }
}

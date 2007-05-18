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

        System.out.println("OUTPUT");
        // have an instance so we can override methods.
        CellProcessOutputSliceNode self = new CellProcessOutputSliceNode(outputNode,whichPhase,backEndBits);
        self.doit();
    }
    
    @Override
    protected void additionalInitProcessing() {
        System.out.println("overridden output init");
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
        ppuCS.addFilterDescriptionSetup(outputNode);
        ppuCS.addFilterUnload(outputNode);
    }
}

package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.ProcessInputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.InputSliceNode;

public class CellProcessInputSliceNode extends ProcessInputSliceNode {

    private CellProcessInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        super(inputNode, whichPhase, backEndBits);
    }
    
    public static void processInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {

        // have an instance so we can override methods.
        CellProcessInputSliceNode self = new CellProcessInputSliceNode(inputNode,whichPhase,backEndBits);
        self.doit();
    }
    
    @Override
    protected void additionalInitProcessing() {
        System.out.println("overridden input init");
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
        ppuCS.startNewFilter(inputNode);
        ppuCS.addFilterDescriptionSetup(inputNode);
        ppuCS.addNewGroupStatement(inputNode);
        ppuCS.addFilterLoad(inputNode);
        ppuCS.addInputBufferAllocAttach(inputNode);
    }

}

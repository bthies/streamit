package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.FilterSliceNode;

public class CellProcessFilterSliceNode extends ProcessFilterSliceNode {

    private CellProcessFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        super(filterNode, whichPhase, backEndBits);
    }
    
    public static void processFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {

        // have an instance so we can override methods.
        CellProcessFilterSliceNode self = new CellProcessFilterSliceNode(filterNode,whichPhase,backEndBits);
        self.doit();
    }
    
    @Override
    protected void additionalInitProcessing() {
        System.out.println("overridden init");
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
        ppuCS.addWorkFunctionAddressField(filterNode);
        ppuCS.addSPUFilterDescriptionField(filterNode);
        ppuCS.addFilterDescriptionSetup(filterNode);
        ppuCS.addSPUInitStatements(filterNode);
        ppuCS.addCallBackFunction(filterNode);
    }
    
    @Override
    protected void additionalPrimePumpProcessing() {
        
    }
    
    @Override
    protected void additionalSteadyProcessing() {
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
    }
    
}

package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;

public class CellProcessFilterSliceNode extends ProcessFilterSliceNode {
    
    private CellProcessFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        super(filterNode, whichPhase, backEndBits);
    }
    
    public static void processFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, BackEndFactory backEndBits) {
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
        ppuCS.addSPUInit();
        ppuCS.addCallBackFunction();
        ppuCS.setInit();
        // have an instance so we can override methods.
        CellProcessFilterSliceNode self = new CellProcessFilterSliceNode(filterNode,whichPhase,backEndBits);
        self.doit();
    }
    
    @Override
    protected void additionalInitProcessing() {
        InputSliceNode inputNode = filterNode.getParent().getHead();
        OutputSliceNode outputNode = filterNode.getParent().getTail();
        
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
        
        ppuCS.startNewFilter(inputNode);
        
        ppuCS.addWorkFunctionAddressField(filterNode);
        ppuCS.addSPUFilterDescriptionField(filterNode);
        
        ppuCS.addFilterDescriptionSetup(filterNode);
        ppuCS.addFilterDescriptionSetup(inputNode);
        ppuCS.addFilterDescriptionSetup(outputNode);
        
        ppuCS.setupInputBufferAddresses(inputNode);
        ppuCS.setupOutputBufferAddresses(outputNode);
        ppuCS.setupDataAddress(filterNode);
        
        ppuCS.addNewGroupStatement(inputNode);
        ppuCS.addFilterLoad(inputNode);
        ppuCS.addInputBufferAllocAttach(inputNode);
        ppuCS.addOutputBufferAllocAttach(outputNode);
        
        ppuCS.addNewGroupAndFilterUnload(outputNode);
        
        ppuCS.addIssueGroupAndWait(filterNode);
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

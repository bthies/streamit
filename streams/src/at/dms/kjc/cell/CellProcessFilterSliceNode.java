package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;

public class CellProcessFilterSliceNode extends ProcessFilterSliceNode {
    
    private int cpunum;
    
    public CellProcessFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(filterNode, whichPhase, backEndBits);
        cpunum = backEndBits.getCellPUNumForFilter(filterNode);
    }
    
    public void processFilterSliceNode() {
        
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
        
        System.out.println("steady mult: " + filterNode.getFilter().getSteadyMult());
        
        if (filterNode.isFileInput()) {
            if (whichPhase == SchedulingPhase.INIT) {
                ppuCS.addFileReader(filterNode);
                //doit();
                ppuCS.addWorkFunctionAddressField();
                ppuCS.addSPUFilterDescriptionField();
                ppuCS.addDataAddressField();
                ppuCS.setupInputBufferAddress();
                ppuCS.setupOutputBufferAddress();
                ppuCS.setupDataAddress();
                ppuCS.addFilterDescriptionSetup();
                ppuCS.addNewGroupStatement();
                ppuCS.addIssueGroupAndWait();
                ppuCS.addPSPLayout();
                ppuCS.addSpulibPollWhile();
                ppuCS.addIssueUnload();
            }
        } else if (filterNode.isFileOutput()) {
            if (whichPhase == SchedulingPhase.INIT)
                ppuCS.addFileWriter(filterNode);
        } else {
            doit();
        }
    }
    
    @Override
    protected void additionalInitProcessing() {
        
        // buffer size choice array
        // spuiters array for each spu
        // read in to buffer from file
        // fd.param
        
        //Thread.dumpStack();
        InputSliceNode inputNode = filterNode.getParent().getHead();
        OutputSliceNode outputNode = filterNode.getParent().getTail();
        
        PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
        CellComputeCodeStore ppuCS = ppu.getComputeCode();
        
        //ppuCS.startNewFilter(inputNode);
        
    }
    
    @Override
    protected void additionalPrimePumpProcessing() {
        
    }
    
    @Override
    protected void additionalSteadyProcessing() {
        
    }
    
}

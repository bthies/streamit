package at.dms.kjc.cell;

import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpressionStatement;
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
        
        filterNode.getFilter().getSteadyMult();
        
        // have an instance so we can override methods.
        CellProcessFilterSliceNode self = new CellProcessFilterSliceNode(filterNode,whichPhase,backEndBits);
        if (filterNode.isFileInput()) {
            if (whichPhase == SchedulingPhase.INIT) {
                ppuCS.addFileReader(filterNode);
                //self.doit();
            }
        } else if (filterNode.isFileOutput()) {
            if (whichPhase == SchedulingPhase.INIT)
                ppuCS.addFileWriter(filterNode);
        } else {
            self.doit();
        }
    }
    
    @Override
    protected void additionalInitProcessing() {
        //Thread.dumpStack();
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
        
        //ppuCS.addInputBufferFields(inputNode);
        ppuCS.setupInputBufferAddresses(inputNode);
        //ppuCS.addOutputBufferFields(outputNode);
        ppuCS.setupOutputBufferAddresses(outputNode);
        ppuCS.addDataAddressField(filterNode);
        ppuCS.setupDataAddress(filterNode);
        
        ppuCS.addNewGroupStatement(inputNode);
        //ppuCS.addFilterLoad(inputNode);
        //ppuCS.addInputBufferAllocAttach(inputNode);
        //ppuCS.addOutputBufferAllocAttach(outputNode);
        
        //ppuCS.addNewGroupAndFilterUnload(outputNode);
        
        ppuCS.addIssueGroupAndWait(filterNode);
        
        ppuCS.addPSPLayout(filterNode);
        ppuCS.addDataParallel();
        
        ppuCS.addIssueUnload(filterNode);
    }
    
    @Override
    protected void additionalPrimePumpProcessing() {
        
    }
    
    @Override
    protected void additionalSteadyProcessing() {
        
    }
    
}

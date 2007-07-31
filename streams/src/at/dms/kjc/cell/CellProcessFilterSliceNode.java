package at.dms.kjc.cell;

import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JThisExpression;
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
                ppuCS.initSpulibClock();
                ppuCS.addWorkFunctionAddressField();
                ppuCS.addInitFunctionAddressField();
                ppuCS.addSPUFilterDescriptionField();
                ppuCS.addDataAddressField();
                ppuCS.setupInputBufferAddress();
                ppuCS.setupOutputBufferAddress();
                ppuCS.setupDataAddress();
                ppuCS.addFilterDescriptionSetup();
                ppuCS.addNewGroupStatement();
                ppuCS.addIssueGroupAndWait();
                ppuCS.addStartSpuTicks();
                ppuCS.addPSPLayout();
                ppuCS.addSpulibPollWhile();
                ppuCS.addPrintTicks();
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
    protected void standardInitProcessing() {
        // Have the main function for the CodeStore call out init.
        codeStore.addInitFunctionCall(filter_code.getInitMethod());
        JMethodDeclaration workAtInit = filter_code.getInitStageMethod();
        if (workAtInit != null) {
            // if there are calls to work needed at init time then add
            // method to general pool of methods
            codeStore.addMethod(workAtInit);
            // and add call to list of calls made at init time.
            // Note: these calls must execute in the order of the
            // initialization schedule -- so caller of this routine 
            // must follow order of init schedule.
            codeStore.addInitFunctionCall(workAtInit);
        }

    }
    
    @Override
    protected void additionalPrimePumpProcessing() {
        
    }
    
    @Override
    protected void additionalSteadyProcessing() {
        
    }
    
}

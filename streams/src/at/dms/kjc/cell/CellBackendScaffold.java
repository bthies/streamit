package at.dms.kjc.cell;

import java.util.LinkedList;

import at.dms.kjc.CStdType;
import at.dms.kjc.JBlock;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ComputeNodesI;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;
import at.dms.util.Utils;

public class CellBackendScaffold extends BackEndScaffold {
    
    @Override
    public void beforeScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        ComputeNodesI computeNodes = resources.getComputeNodes();        
        Slice slices[] = schedule.getInitSchedule();
        
        iterateInorder(slices, SchedulingPhase.PREINIT, computeNodes);
        
        CellComputeCodeStore ppuCS = 
            ((CellBackendFactory)resources).getPPU().getComputeCode();
        
        // add wf[] and init_wf[] fields
        ppuCS.addWorkFunctionAddressField();
        ppuCS.addPSPFields();
        ppuCS.addSPUFilterDescriptionField();
        ppuCS.addChannelFields();
        //ppuCS.addInitFunctionAddressField();
    }
    
    @Override
    protected void betweenScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        CellComputeCodeStore ppuCS = 
            ((CellBackendFactory)resources).getPPU().getComputeCode();
        
        for (LinkedList<Integer> group : CellBackend.scheduleLayout) {
            int spuId = 0;
            JBlock body = new JBlock();
            for (int filterId : group) {
                SliceNode sliceNode = CellBackend.filters.get(filterId);
                body.addStatement(ppuCS.setupFilterDescriptionWorkFunc(sliceNode, SchedulingPhase.INIT));
                body.addStatement(ppuCS.setupPSPIOBytes(sliceNode, SchedulingPhase.INIT));
                body.addStatement(ppuCS.setupPSPSpuId(sliceNode, spuId));
                body.addStatement(ppuCS.callExtPSP(sliceNode));
                spuId++;
            }
            body.addStatement(ppuCS.setDone(spuId));
            body.addStatement(ppuCS.addSpulibPollWhile());
            ppuCS.addInitStatement(body);
        }
        
        JBlock steadyLoop = new JBlock();
        for (LinkedList<Integer> group : CellBackend.scheduleLayout) {
            int spuId = 0;
            JBlock body = new JBlock();
            for (int filterId : group) {
                SliceNode sliceNode = CellBackend.filters.get(filterId);
                body.addStatement(ppuCS.setupFilterDescriptionWorkFunc(sliceNode, SchedulingPhase.STEADY));
                body.addStatement(ppuCS.setupPSPIOBytes(sliceNode, SchedulingPhase.STEADY));
                body.addStatement(ppuCS.setupPSPSpuId(sliceNode, spuId));
                body.addStatement(ppuCS.callExtPSP(sliceNode));
                spuId++;
            }
            body.addStatement(ppuCS.setDone(spuId));
            body.addStatement(ppuCS.addSpulibPollWhile());
            steadyLoop.addStatement(body);
        }
        
        JBlock block = new JBlock();
        JVariableDefinition loopCounter = new JVariableDefinition(null,
                0,
                CStdType.Integer,
                CodeStoreHelper.workCounter,
                null);

        JStatement loop = 
            Utils.makeForLoopLocalIndex(steadyLoop, loopCounter, new JIntLiteral(100));
        block.addStatement(new JVariableDeclarationStatement(null,
                loopCounter,
                null));

        block.addStatement(loop);
        ppuCS.addSteadyLoopStatement(block);
    }

}

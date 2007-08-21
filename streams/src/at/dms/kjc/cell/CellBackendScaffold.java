package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.ComputeNodesI;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;

public class CellBackendScaffold extends BackEndScaffold {
    
    @Override
    public void beforeScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        ComputeNodesI computeNodes = resources.getComputeNodes();        
        Slice slices[] = schedule.getInitSchedule();
        
        iterateInorder(slices, SchedulingPhase.PREINIT, computeNodes);
    }
    
    @Override
    protected void betweenScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        CellComputeCodeStore ppuCS = 
            ((CellBackendFactory)resources).getPPU().getComputeCode();
        
        // add wf[] and init_wf[] fields
        ppuCS.addWorkFunctionAddressField();
        ppuCS.addPSPFields();
        ppuCS.addSPUFilterDescriptionField();
        ppuCS.addChannelFields();
        //ppuCS.addInitFunctionAddressField();
    }

}

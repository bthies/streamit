package at.dms.kjc.cell;

import java.util.LinkedList;

import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ProcessOutputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;

public class CellProcessOutputSliceNode extends ProcessOutputSliceNode {

    private CellComputeCodeStore ppuCS;
    
    public CellProcessOutputSliceNode(OutputSliceNode outputNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(outputNode, whichPhase, backEndBits);
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
        
//    public void doit() {
//        if (backEndBits.sliceNeedsSplitterCode(outputNode.getParent())) {
//            splitter_code = CodeStoreHelper.findHelperForSliceNode(outputNode);
//            if (splitter_code == null) {
//                splitter_code = getSplitterCode(outputNode,backEndBits);
//            }
//        }
//        
//        switch (whichPhase) {
//        case INIT:
//            standardInitProcessing();
//            additionalInitProcessing();
//            break;
//        case PRIMEPUMP:
//            standardPrimePumpProcessing();
//            additionalPrimePumpProcessing();
//            break;
//        case STEADY:
//            if (splitter_code != null)
//                standardSteadyProcessing();
//            additionalSteadyProcessing();
//            break;
//        }
//    }
    
    @Override
    public void additionalPreInitProcessing() {
        // Create new filter for RR splitter
        if (outputNode.isRRSplitter()) {
            int filterId = CellBackend.numfilters;
            CellBackend.filters.add(outputNode);
            CellBackend.filterIdMap.put(outputNode, filterId);
            CellBackend.numfilters++;
            ppuCS.setupWorkFunctionAddress(outputNode);
            ppuCS.setupFilterDescription(outputNode);
            ppuCS.setupPSP(outputNode);
            // attach artificial channel created earlier as input
            int channelId = CellBackend.artificialRRSplitterChannels.get(outputNode);
            LinkedList<Integer> inputIds = new LinkedList<Integer>();
            inputIds.add(channelId);
            ppuCS.attachInputChannelArray(filterId, inputIds);
            // attach outputs
            LinkedList<Integer> outputIds = CellBackend.outputChannelMap.get(outputNode);
            ppuCS.attachOutputChannelArray(filterId, outputIds);
        }
    }
    
    @Override
    public void additionalInitProcessing() {

    }
    
    @Override
    public void additionalSteadyProcessing() {
        System.out.println("processing output: " + outputNode.getPrevFilter().getFilter().getName());
        if (outputNode.isRRSplitter()) {

        } else if (outputNode.isDuplicateSplitter()) {
            //ppuCS.duplicateOutput(outputNode);
        }
//        ppuCS.addReadyBuffers(outputNode);
    }
    
    @Override
    protected void setLocationAndCodeStore() {
        location = backEndBits.getLayout().getComputeNode(outputNode);
        assert location != null;
        if (outputNode.isRRSplitter())
            codeStore = ((CellPU)location).getComputeCodeStore(outputNode);
        else codeStore = ((CellPU)location).getComputeCodeStore(outputNode.getPrevFilter());
    }
    
}

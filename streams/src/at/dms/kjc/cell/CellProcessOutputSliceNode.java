package at.dms.kjc.cell;

import java.util.LinkedList;

import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ProcessOutputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;

public class CellProcessOutputSliceNode extends ProcessOutputSliceNode {

    private CellComputeCodeStore ppuCS;
    private CellComputeCodeStore initCodeStore; 
    
    public CellProcessOutputSliceNode(OutputSliceNode outputNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(outputNode, whichPhase, backEndBits);
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
    
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
        
        addToScheduleLayout();
    }
    
    @Override
    public void additionalInitProcessing() {
        initCodeStore.addFields(splitter_code.getUsefulFields());
        initCodeStore.addMethods(splitter_code.getUsefulMethods());
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
    
    private void addToScheduleLayout() {
        if (outputNode.isRRSplitter()) {
            int filterId = CellBackend.filterIdMap.get(outputNode);
            LinkedList<Integer> currentGroup = CellBackend.getLastScheduleGroup();
            int artificialId = CellBackend.artificialRRSplitterChannels.get(outputNode);
            if (CellBackend.readyInputs.contains(artificialId)) {
                // If there is an available SPU, assign it
                if (currentGroup.size() < CellBackend.numspus) {
                    currentGroup.add(filterId);
                } 
                // Otherwise, make a new group
                else {
                    LinkedList<Integer> newGroup = new LinkedList<Integer>();
                    newGroup.add(filterId);
                    CellBackend.scheduleLayout.add(newGroup);
                    // Mark the outputs of the previous group as ready
                    CellBackend.addReadyInputsForCompleted(currentGroup);
                }
            }
            // If not all of the input channels are ready, must wait for
            // current group to complete. Afterwards, make a new group with this
            // joiner
            else {
                LinkedList<Integer> newGroup = new LinkedList<Integer>();
                newGroup.add(filterId);
                CellBackend.scheduleLayout.add(newGroup);
                // Mark the outputs of the previous group as ready
                CellBackend.addReadyInputsForCompleted(currentGroup);
            }
        }
    }
    
    @Override
    protected void setLocationAndCodeStore() {
        location = backEndBits.getLayout().getComputeNode(outputNode);
        assert location != null;
        if (outputNode.isRRSplitter()) {
            codeStore = ((CellPU)location).getComputeCodeStore(outputNode);
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(outputNode);
        } else {
            codeStore = ((CellPU)location).getComputeCodeStore(outputNode.getPrevFilter());
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(outputNode.getPrevFilter());
        }
    }
    
}

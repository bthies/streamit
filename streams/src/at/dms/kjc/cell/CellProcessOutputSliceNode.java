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
    
    /**
     * Create code for a OutputSliceNode.
     */
    @Override
    public void processOutputSliceNode() {
        doit();
    }
    
    public void doit() {
        if (backEndBits.sliceNeedsSplitterCode(outputNode.getParent())) {
            splitter_code = CodeStoreHelper.findHelperForSliceNode(outputNode);
            if (splitter_code == null) {
                splitter_code = getSplitterCode(outputNode,backEndBits);
            }
        }
        
        switch (whichPhase) {
        case INIT:
            standardInitProcessing();
            additionalInitProcessing();
            break;
        case PRIMEPUMP:
            standardPrimePumpProcessing();
            additionalPrimePumpProcessing();
            break;
        case STEADY:
            if (splitter_code != null)
                standardSteadyProcessing();
            additionalSteadyProcessing();
            break;
        }
    }
    
    @Override
    public void additionalInitProcessing() {
        // If it's a duplicate splitter, add only one channel for the output
        // Don't create new filter
        if (outputNode.isDuplicateSplitter()) {
            InterSliceEdge e = new InterSliceEdge(outputNode);
            CellBackend.duplicateSplitters.put(outputNode,CellBackend.numchannels);
            CellBackend.channels.add(e);
            CellBackend.channelIdMap.put(e, CellBackend.numchannels);
            ppuCS.initChannel(CellBackend.numchannels);
            CellBackend.numchannels++;
        }
        // Create new filter for RR splitter
        else if (outputNode.isRRSplitter()) {
            int filterId = CellBackend.numfilters;
            CellBackend.filters.add(outputNode);
            CellBackend.filterIdMap.put(outputNode, filterId);
            CellBackend.numfilters++;
            ppuCS.setupWorkFunctionAddress(outputNode);
            ppuCS.setupFilterDescription(outputNode);
            ppuCS.setupPSP(outputNode);
        }

        LinkedList<Integer> outputnums = new LinkedList<Integer>();
        for (InterSliceEdge e : outputNode.getDestSequence()) {
            if (!CellBackend.channelIdMap.containsKey(e)) {
                CellBackend.channels.add(e);
                CellBackend.channelIdMap.put(e, CellBackend.numchannels);
                outputnums.add(CellBackend.numchannels);
                if (outputNode.isRRSplitter())
                    ppuCS.initChannel(CellBackend.numchannels);
                else if (outputNode.isDuplicateSplitter())
                    ppuCS.duplicateChannel(outputNode, CellBackend.numchannels);
                CellBackend.numchannels++;
            } else {
                outputnums.add(CellBackend.channelIdMap.get(e));
            }
        }
        CellBackend.outputChannelMap.put(outputNode, outputnums);
        
        if (outputNode.isRRSplitter())
            ppuCS.initOutputChannelArray(outputNode, 
                    outputnums, 
                    CellBackend.filterIdMap.get(outputNode));
        else if (outputNode.isDuplicateSplitter()) {
            System.out.println("ppucs: " + ppuCS);
            System.out.println("filterIdMap: " + CellBackend.filterIdMap);
            //ppuCS.initDuplicateOutputChannelArray(CellBackend.filterIdMap.get(outputNode));
        }
        else ppuCS.initOutputChannelArray(outputNode, 
                outputnums, 
                CellBackend.filterIdMap.get(outputNode.getPrevFilter()));

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

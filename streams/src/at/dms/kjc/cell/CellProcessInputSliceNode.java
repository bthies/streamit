package at.dms.kjc.cell;

import java.util.LinkedList;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ProcessInputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;

public class CellProcessInputSliceNode extends ProcessInputSliceNode {

    private CellComputeCodeStore ppuCS;
    
    public CellProcessInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(inputNode, whichPhase, backEndBits);
        this.backEndBits = backEndBits;
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
    
//    /**
//     * Create code for a InputSliceNode.
//     */
//    @Override
//    public void doit() {
//        
//        // No code generated for inputNode if there is no input.
//        if (backEndBits.sliceHasUpstreamChannel(inputNode.getParent())) {
//            joiner_code = CodeStoreHelper.findHelperForSliceNode(inputNode);
//            if (joiner_code == null) {
//                joiner_code = getJoinerCode(inputNode,backEndBits);
//            }
//        }
//        
//        switch (whichPhase) {
//        case PREINIT:
//            standardPreInitProcessing();
//            additionalPreInitProcessing();
//        case INIT:
//            if (joiner_code != null)
//                standardInitProcessing();
//            additionalInitProcessing();
//            break;
//        case PRIMEPUMP:
//            standardPrimePumpProcessing();
//            additionalPrimePumpProcessing();
//            break;
//        case STEADY:
//            if (joiner_code != null)
//                standardSteadyProcessing();
//            additionalSteadyProcessing();
//            break;
//        }
//    }
    
    public void additionalPreInitProcessing() {
        // If InputSliceNode is a joiner (i.e. has at least 2 inputs), add it
        // as a filter.
        if (inputNode.isJoiner()) {
            int filterId = CellBackend.numfilters;
            CellBackend.filters.add(inputNode);
            CellBackend.filterIdMap.put(inputNode, filterId);
            CellBackend.numfilters++;
            // wf[i] = &wf_... and init_wf[i] = &wf_init_...
            ppuCS.setupWorkFunctionAddress(inputNode);
            // fd[i].state_size/num_inputs/num_outputs = ...
            ppuCS.setupFilterDescription(inputNode);
            // setup EXT_PSP_EX_PARAMS/LAYOUT
            ppuCS.setupPSP(inputNode);
            
            // Ids of channels that are inputs to this filter
            LinkedList<Integer> inputIds = new LinkedList<Integer>();
            // Populate Channel-ID mapping, and increase number of channels.
            for (InterSliceEdge e : inputNode.getSourceList()) {
                // Always use src->dest direction for edges
                InterSliceEdge f = CellBackend.getEdgeBetween(e.getSrc(),inputNode);
                if(!CellBackend.channelIdMap.containsKey(f)) {
                    CellBackend.channels.add(f);
                    CellBackend.channelIdMap.put(f,CellBackend.numchannels);
                    inputIds.add(CellBackend.numchannels);
                    ppuCS.initChannel(CellBackend.numchannels);
                    CellBackend.numchannels++;
                } else {
                    inputIds.add(CellBackend.channelIdMap.get(f));
                }
            }
            CellBackend.inputChannelMap.put(inputNode, inputIds);

            ppuCS.attachInputChannelArray(
                    inputNode,
                    inputIds,
                    CellBackend.filterIdMap.get(inputNode).intValue());
        } else {
            
        }
    }
    
    @Override
    public void additionalInitProcessing() {

        
    }
    
    @Override
    public void additionalSteadyProcessing() {
        //System.out.println("processing input: " + inputNode.getNextFilter().getFilter().getName());
        //ppuCS.initOutputBufferFields(inputNode);
        boolean ready = ppuCS.lookupInputBuffers(inputNode);
        if (!ready || CellBackend.SPUassignment.size() == KjcOptions.cell) {
            ppuCS.addSpulibPollWhile();
            CellBackend.SPUassignment.clear();
        }
    }

    @Override
    protected void setLocationAndCodeStore() {
        location = backEndBits.getLayout().getComputeNode(inputNode);
        assert location != null;
        if (inputNode.isJoiner())
            codeStore = ((CellPU)location).getComputeCodeStore(inputNode);
        else codeStore = ((CellPU)location).getComputeCodeStore(inputNode.getNextFilter());
    }
}

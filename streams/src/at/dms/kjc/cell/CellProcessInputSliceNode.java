package at.dms.kjc.cell;

import java.util.LinkedList;

import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.ProcessInputSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;

public class CellProcessInputSliceNode extends ProcessInputSliceNode {

    private CellComputeCodeStore ppuCS;
    private CellComputeCodeStore initCodeStore;
    
    public CellProcessInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(inputNode, whichPhase, backEndBits);
        this.backEndBits = backEndBits;
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
    
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
        }
        
        // Ids of channels that are inputs to this filter
        LinkedList<Integer> inputIds = new LinkedList<Integer>();
        // Populate Channel-ID mapping, and increase number of channels.
        for (InterSliceEdge e : inputNode.getSourceList()) {
            // Always use output->input direction for edges
            InterSliceEdge f = CellBackend.getEdgeBetween(e.getSrc(),inputNode);
            if(!CellBackend.channelIdMap.containsKey(f)) {
                int channelId = CellBackend.numchannels;
                CellBackend.channels.add(f);
                CellBackend.channelIdMap.put(f,channelId);
                inputIds.add(channelId);
                ppuCS.initChannel(channelId);
                CellBackend.numchannels++;
            } else {
                inputIds.add(CellBackend.channelIdMap.get(f));
            }
        }
        CellBackend.inputChannelMap.put(inputNode, inputIds);

        if (inputNode.isJoiner()) {
            int filterId = CellBackend.filterIdMap.get(inputNode);
            // attach all input channels as inputs to the joiner
            ppuCS.attachInputChannelArray(filterId, inputIds);
            //make artificial channel between inputslicenode and filterslicenode
            int channelId = CellBackend.numchannels;
            InterSliceEdge a = new InterSliceEdge(inputNode);
            CellBackend.channels.add(a);
            CellBackend.channelIdMap.put(a, channelId);
            CellBackend.artificialJoinerChannels.put(inputNode, channelId);
            ppuCS.initChannel(channelId);
            CellBackend.numchannels++;
            // attach artificial channel as output of the joiner
            LinkedList<Integer> outputIds = new LinkedList<Integer>();
            outputIds.add(channelId);
            ppuCS.attachOutputChannelArray(filterId, outputIds);
        }
        
        addToScheduleLayout();
    }
    
    @Override
    public void standardInitProcessing() {
        // Have the main function for the CodeStore call our init if any
        initCodeStore.addInitFunctionCall(joiner_code.getInitMethod());
        JMethodDeclaration workAtInit = joiner_code.getInitStageMethod();
        if (workAtInit != null) {
            // if there are calls to work needed at init time then add
            // method to general pool of methods
            initCodeStore.addMethod(workAtInit);
            // and add call to list of calls made at init time.
            // Note: these calls must execute in the order of the
            // initialization schedule -- so caller of this routine 
            // must follow order of init schedule.
            initCodeStore.addInitStatement(new JExpressionStatement(null,
                    new JMethodCallExpression(null, new JThisExpression(null),
                            workAtInit.getName(), new JExpression[0]), null));
        }
    }
    
    @Override
    public void additionalInitProcessing() {

        
    }
    
    @Override
    public void additionalSteadyProcessing() {
//        boolean ready = ppuCS.lookupInputBuffers(inputNode);
//        if (!ready || CellBackend.SPUassignment.size() == KjcOptions.cell) {
//            ppuCS.addSpulibPollWhile();
//            CellBackend.SPUassignment.clear();
//        }
    }
    
    private void addToScheduleLayout() {
        if (inputNode.isJoiner()) {
            int filterId = CellBackend.filterIdMap.get(inputNode);
            LinkedList<Integer> inputIds = CellBackend.inputChannelMap.get(inputNode);
            LinkedList<Integer> currentGroup = CellBackend.getLastScheduleGroup();
            // If all of the input channels are ready
            if (CellBackend.readyInputs.containsAll(inputIds)) {
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
        location = backEndBits.getLayout().getComputeNode(inputNode);
        assert location != null;
        if (inputNode.isJoiner()) {
            codeStore = ((CellPU)location).getComputeCodeStore(inputNode);
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(inputNode);
        } else {
            codeStore = ((CellPU)location).getComputeCodeStore(inputNode.getNextFilter());
            initCodeStore = ((CellPU)location).getInitComputeCodeStore(inputNode.getNextFilter());
        }
    }
}

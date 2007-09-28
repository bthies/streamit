package at.dms.kjc.cell;

import java.util.LinkedList;

import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.FileOutputContent;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SliceNode;

public class CellProcessFilterSliceNode extends ProcessFilterSliceNode {
    
    private CellComputeCodeStore ppuCS;
    private CellComputeCodeStore initCodeStore;
    
    public CellProcessFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(filterNode, whichPhase, backEndBits);
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
    
    public void processFilterSliceNode() {
        
        if (filterNode.isFileInput()) {
            if (whichPhase == SchedulingPhase.PREINIT) {
                setupFileReaderChannel();
                ppuCS.initFileReader(filterNode);
            }
            else if (whichPhase == SchedulingPhase.INIT) {
                if (!KjcOptions.celldyn)
                    ppuCS.addFileReader(filterNode);
            }
            else if (whichPhase == SchedulingPhase.STEADY) {
            }
        } else if (filterNode.isFileOutput()) {
            if (whichPhase == SchedulingPhase.PREINIT) {
                ppuCS.initFileWriter(filterNode);                
            }
            else if (whichPhase == SchedulingPhase.INIT) {

            }
            else if (whichPhase == SchedulingPhase.STEADY) {
                if (!KjcOptions.celldyn)
                    ppuCS.addFileWriter(filterNode);
            }
        } else {
            doit();
        }
    }
    
    private CellPU getLocationFromScheduleLayout(SliceNode sliceNode) {
        int cpu = -1;
        int filterId = CellBackend.filterIdMap.get(sliceNode);
        for (LinkedList<Integer> l : CellBackend.scheduleLayout) {
            if (l.indexOf(filterId) > -1)
                cpu = l.indexOf(filterId);
        }
        if (cpu == -1) return null;
        return (CellPU)backEndBits.getComputeNode(cpu + 1);
    }
    
    @Override
    protected void setLocationAndCodeStore() {
        //if (whichPhase == SchedulingPhase.PREINIT) return;
        location = backEndBits.getLayout().getComputeNode(filterNode);
        assert location != null;
        codeStore = ((CellPU)location).getComputeCodeStore(filterNode);
        initCodeStore = ((CellPU)location).getInitComputeCodeStore(filterNode);
    }
    
    @Override
    protected void additionalPreInitProcessing() {
        InputSliceNode inputNode = filterNode.getParent().getHead();
        OutputSliceNode outputNode = filterNode.getParent().getTail();
        // Add filter to mapping
        int filterId = CellBackend.numfilters;
        CellBackend.filters.add(filterNode);
        CellBackend.filterIdMap.put(filterNode, filterId);
        CellBackend.numfilters++;
        
        ppuCS.setupWorkFunctionAddress(filterNode);
        ppuCS.setupFilter(filterNode);
        if (!KjcOptions.celldyn) ppuCS.setupPSP(filterNode);
        
        LinkedList<Integer> inputIds = CellBackend.inputChannelMap.get(inputNode);
        
        System.out.println("new filter " + filterId + " " + filterNode.getFilter().getName());
        
        // Finish handling inputs
        if (!inputNode.isJoiner()) {
            // if not a joiner, attach all inputs to the filter (should only
            // be one input)
            ppuCS.attachInputChannelArray(filterId, inputIds, whichPhase);
        }
        else {
            // attach artificial channel created earlier as input
            int channelId = CellBackend.artificialJoinerChannels.get(inputNode);
            inputIds = new LinkedList<Integer>();
            inputIds.add(channelId);
            ppuCS.attachInputChannelArray(filterId, inputIds, whichPhase);
        }
        
        LinkedList<Integer> outputIds = new LinkedList<Integer>();
        
        if (outputNode.isRRSplitter()) {
            // make artificial channel between filterslicenode and outputslicenode
            int channelId = CellBackend.numchannels;
            // dummy edge
            InterSliceEdge a = new InterSliceEdge(outputNode);
            CellBackend.channels.add(a);
            CellBackend.channelIdMap.put(a, channelId);
            CellBackend.artificialRRSplitterChannels.put(outputNode, channelId);
            if (!KjcOptions.celldyn) ppuCS.initChannel(channelId);
            CellBackend.numchannels++;
            // attach artificial channel as output of filterslicenode 
            LinkedList<Integer> RROutputIds = new LinkedList<Integer>();
            RROutputIds.add(channelId);
            ppuCS.attachOutputChannelArray(filterId, RROutputIds);
        }

        // Populate Channel-ID mapping, and increase number of channels.
        for (InterSliceEdge e : outputNode.getDestSequence()) {
            if(!CellBackend.channelIdMap.containsKey(e)) {
                int channelId = CellBackend.numchannels;
                CellBackend.channels.add(e);
                CellBackend.channelIdMap.put(e,channelId);
                outputIds.add(channelId);
                // init and allocate buffer if not duplicate splitter
                if (!outputNode.isDuplicateSplitter())
                    ppuCS.initChannel(channelId);
                CellBackend.numchannels++;
            } else {
                outputIds.add(CellBackend.channelIdMap.get(e));
            }
        }
        CellBackend.outputChannelMap.put(outputNode, outputIds);

        if (outputNode.isDuplicateSplitter()) {
            // for duplicate splitters, initialize only the first channel
            // the rest will be duplicated later
            int channelId = outputIds.getFirst();
            ppuCS.initChannel(channelId);
            // remove the already initialized first channel, duplicate the rest
            LinkedList<Integer> dupOutputIds = new LinkedList<Integer>(outputIds);
            dupOutputIds.removeFirst();
            ppuCS.duplicateChannel(channelId, dupOutputIds);
            // attach outputs
            ppuCS.attachOutputChannelArray(filterId, outputIds);
        } else if (!outputNode.isRRSplitter()) {
            ppuCS.attachOutputChannelArray(filterId, outputIds);
        }
        
        if (!KjcOptions.celldyn) addToScheduleLayout();
        
        
        if (KjcOptions.celldyn) {
            PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
            CellComputeCodeStore ppuCS = ppu.getComputeCode();

            //ppuCS.dynSetupFilter(filterNode, CellBackend.numfilters, inputs, outputs, inputnums, outputnums);
        }
    }
    
//    @Override
//    protected void standardInitProcessing() {
//        // Have the main function for the CodeStore call out init.
//        codeStore.addInitFunctionCall(filter_code.getInitMethod());
//        JMethodDeclaration workAtInit = filter_code.getInitStageMethod();
//        if (workAtInit != null) {
//            // if there are calls to work needed at init time then add
//            // method to general pool of methods
//            codeStore.addMethod(workAtInit);
//            // and add call to list of calls made at init time.
//            // Note: these calls must execute in the order of the
//            // initialization schedule -- so caller of this routine 
//            // must follow order of init schedule.
//            codeStore.addInitFunctionCall(workAtInit);
//        }
//    }
    
    @Override
    protected void standardInitProcessing() {
        // Have the main function for the CodeStore call out init.
        initCodeStore.addInitFunctionCall(filter_code.getInitMethod());
        JMethodDeclaration workAtInit = filter_code.getInitStageMethod();
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
        //initCodeStore.addFields(filter_code.getUsefulFields());
        initCodeStore.addMethods(filter_code.getUsefulMethods());

    }
    
    @Override
    protected void additionalInitProcessing() {

    }
    
    @Override
    protected void additionalPrimePumpProcessing() {
        
    }
    
    @Override
    protected void standardSteadyProcessing() {
        JStatement steadyBlock = filter_code.getSteadyBlock();
        // helper has now been used for the last time, so we can write the basic code.
        // write code deemed useful by the helper into the corrrect ComputeCodeStore.
        // write only once if multiple calls for steady state.
        if (!basicCodeWritten.containsKey(filterNode)) {
            codeStore.addFields(filter_code.getUsefulFields());
            codeStore.addMethods(filter_code.getUsefulMethods());
            basicCodeWritten.put(filterNode,true);
        }
        codeStore.addSteadyLoopStatement(steadyBlock);
        
        if (debug) {
            // debug info only: expected splitter and joiner firings.
            System.err.print("(Filter" + filterNode.getFilter().getName());
            System.err.print(" "
                    + FilterInfo.getFilterInfo(filterNode).getMult(
                            SchedulingPhase.INIT));
            System.err.print(" "
                    + FilterInfo.getFilterInfo(filterNode).getMult(
                            SchedulingPhase.STEADY));
            System.err.println(")");
            System.err.print("(Joiner joiner_"
                    + filterNode.getFilter().getName());
            System.err.print(" "
                    + FilterInfo.getFilterInfo(filterNode)
                            .totalItemsReceived(SchedulingPhase.INIT));
            System.err.print(" "
                    + FilterInfo.getFilterInfo(filterNode)
                            .totalItemsReceived(SchedulingPhase.STEADY));
            System.err.println(")");
            System.err.print("(Splitter splitter_"
                    + filterNode.getFilter().getName());
            System.err.print(" "
                    + FilterInfo.getFilterInfo(filterNode).totalItemsSent(
                            SchedulingPhase.INIT));
            System.err.print(" "
                    + FilterInfo.getFilterInfo(filterNode).totalItemsSent(
                            SchedulingPhase.STEADY));
            System.err.println(")");
        }
        
    }

    
    @Override
    protected void additionalSteadyProcessing() {
//        System.out.println("processing filter: " + filterNode.getFilter().getName());
//        int spu = getFreeSPU();
//        assert spu >= 0;
//        CellBackend.SPUassignment.put(spu,filterNode);
//        //if (StatelessDuplicate.sizeOfMutableState(filterNode.getFilter().))
//        System.out.println(spu + " is working on " + filterNode.getFilter().getName());
//        //ppuCS.addNewGroupStatement();
//        //ppuCS.addIssueGroupAndWait();
//        //ppuCS.addPSPLayout();
////        ppuCS.addSpulibPollWhile(filterNode);
////        SPUassignment.remove(spu);
//        //ppuCS.addIssueUnload();
//        ppuCS.newline();
    }
    
    /**
     * Set up a file reader. A file reader is not considered a filter and is
     * therefore not scheduled. File readers are always run on the PPU.
     * All output channels are set up (could be multiple if the next filter
     * was a splitter and was fused to become the file reader's outputslicenode)
     *
     */
    private void setupFileReaderChannel() {
        OutputSliceNode outputNode = filterNode.getParent().getTail();
        // List of channel IDs for all output channels of the file reader
        LinkedList<Integer> outputIds = new LinkedList<Integer>();
        
        boolean firstbuffer = true;
        int firstChannelId = CellBackend.numchannels;
        
        // Populate Channel-ID mapping, and increase number of channels.
        for (InterSliceEdge e : outputNode.getDestSequence()) {
            int channelId;
            // get next available channel ID
            channelId = CellBackend.numchannels;
            // add to list of channels (edges) and map of edges->channels
            CellBackend.channels.add(e);
            CellBackend.channelIdMap.put(e,channelId);
            // set the channel as ready for scheduling purposes
            CellBackend.readyInputs.add(channelId);
            outputIds.add(channelId);
            if (!KjcOptions.celldyn) {
                // always init and allocate the first buffer
                // init any other buffers if not duplicate splitter
                if (!outputNode.isDuplicateSplitter() || firstbuffer) {
                    ppuCS.initChannel(channelId);
                    firstbuffer = false;
                } else {
                    // if it's a duplicate splitter and it's not the first buffer,
                    // duplicate the new buffer from the first one;
                    ppuCS.duplicateChannel(firstChannelId, channelId);
                }
            }
            // increment the channel ID counter
            CellBackend.numchannels++;
        }
        CellBackend.outputChannelMap.put(outputNode, outputIds);
    }
    
    private void addToScheduleLayout() {
        int filterId = CellBackend.filterIdMap.get(filterNode);
        InputSliceNode inputNode = filterNode.getPrevious().getAsInput();
        OutputSliceNode outputNode = filterNode.getNext().getAsOutput();
        LinkedList<Integer> inputIds = CellBackend.inputChannelMap.get(inputNode);
        LinkedList<Integer> currentGroup = CellBackend.getLastScheduleGroup();
        if (inputNode.isJoiner()) {
            int artificialId = CellBackend.artificialJoinerChannels.get(inputNode);
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
        } else {
            // If there are no inputs or if all of the input channels are ready
            if (inputIds == null || CellBackend.readyInputs.containsAll(inputIds)) {
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
    
    private static int getFreeSPU() {
        for (int i=0; i<KjcOptions.cell; i++) {
            if (!CellBackend.SPUassignment.containsKey(i))
                return i;
        }
        return -1;
    }

    
}

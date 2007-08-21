package at.dms.kjc.cell;

import java.util.ArrayList;
import java.util.LinkedList;

import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;

public class CellProcessFilterSliceNode extends ProcessFilterSliceNode {
    
    private int cpunum;
    
    CellComputeCodeStore ppuCS;
    
    public CellProcessFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, CellBackendFactory backEndBits) {
        super(filterNode, whichPhase, backEndBits);
        cpunum = backEndBits.getCellPUNumForFilter(filterNode);
        ppuCS = backEndBits.getPPU().getComputeCode();
    }
    
    public void processFilterSliceNode() {
        
        if (filterNode.isFileInput()) {
            if (whichPhase == SchedulingPhase.INIT) {
                ppuCS.initFileReader(filterNode);
            }
            else if (whichPhase == SchedulingPhase.STEADY) {
                ppuCS.addFileReader(filterNode);
            }
        } else if (filterNode.isFileOutput()) {
            if (whichPhase == SchedulingPhase.INIT) {
                ppuCS.initFileWriter(filterNode);
            }
            else if (whichPhase == SchedulingPhase.STEADY) {
                ppuCS.addFileWriter(filterNode);
            }
        } else {
            doit();
        }
    }
    
    @Override
    protected void setLocationAndCodeStore() {
        location = backEndBits.getLayout().getComputeNode(filterNode);
        assert location != null;
        codeStore = ((CellPU)location).getComputeCodeStore(filterNode);
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
        ppuCS.setupFilterDescription(filterNode);
        ppuCS.setupPSP(filterNode);
        
        LinkedList<Integer> inputIds = CellBackend.inputChannelMap.get(inputNode);
        
        // Finish handling inputs
        if (!inputNode.isJoiner())
            // if not a joiner, attach all inputs to the filter (should only
            // be one input)
            ppuCS.attachInputChannelArray(filterId, inputIds);
        else {
            // attach artificial channel created earlier as input
            int channelId = CellBackend.artificialJoinerChannels.get(inputNode);
            inputIds = new LinkedList<Integer>();
            inputIds.add(channelId);
            ppuCS.attachInputChannelArray(filterId, inputIds);
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
            ppuCS.initChannel(channelId);
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
        
        
        if (KjcOptions.celldyn) {
            PPU ppu = ((CellBackendFactory) backEndBits).getPPU();
            CellComputeCodeStore ppuCS = ppu.getComputeCode();

            //ppuCS.dynSetupFilter(filterNode, CellBackend.numfilters, inputs, outputs, inputnums, outputnums);
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
    protected void additionalInitProcessing() {

    }
    
    @Override
    protected void additionalPrimePumpProcessing() {
        
    }
    
    @Override
    protected void additionalSteadyProcessing() {
        System.out.println("processing filter: " + filterNode.getFilter().getName());
        int spu = getFreeSPU();
        assert spu >= 0;
        CellBackend.SPUassignment.put(spu,filterNode);
        //if (StatelessDuplicate.sizeOfMutableState(filterNode.getFilter().))
        System.out.println(spu + " is working on " + filterNode.getFilter().getName());
        //ppuCS.addNewGroupStatement();
        //ppuCS.addIssueGroupAndWait();
        //ppuCS.addPSPLayout();
//        ppuCS.addSpulibPollWhile(filterNode);
//        SPUassignment.remove(spu);
        //ppuCS.addIssueUnload();
        ppuCS.newline();
    }
    
    private static int getFreeSPU() {
        for (int i=0; i<KjcOptions.cell; i++) {
            if (!CellBackend.SPUassignment.containsKey(i))
                return i;
        }
        return -1;
    }

    
}

package at.dms.kjc.spacetime;

import java.util.ListIterator;
import java.util.Iterator;

import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.ComputeNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.FilterInfo;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.*;
import at.dms.kjc.spacetime.switchIR.*;
import at.dms.util.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.slicegraph.*;

/**
 * This class will rawify the SIR code and it creates the switch code. It does
 * not rawify the compute code in place.  This class will create the code necessary to 
 * execute the init schedule, the prime pump schedule, and the steady state schedule.
 * It will cycle over the schedules and generate the code necessary.
 * 
 */
public class Rawify {
    
   
    // if true try to compress the switch code by creating loops
    public static boolean SWITCH_COMP = true;

    // any joiner or splitter that passes more than SC_THRESHOLD times in the primepump
    // or steady will have its switch instructions placed in a loop
    public static int SC_THRESHOLD = 10;
         
    // a filter that pushes or pops more than SC_INS_THRESH will have these
    // instruction placed in a loop on the switch
    public static int SC_INS_THRESH = 10;
    
    //any filter that pushes and popes more than this number of items per
    //steady-state (or per primepump) will be compressed!
    public static int SC_FILTER_THRESH = 100;

    // regs on the switch that are used for various loops
    private static SwitchReg POP_LOOP_REG = SwitchReg.R0;

    private static SwitchReg PUSH_LOOP_REG = SwitchReg.R1;

    private static SwitchReg FILTER_FIRE_LOOP_REG = SwitchReg.R2;

    private static SwitchReg PP_STEADY_LOOP_REG = SwitchReg.R3;

    private static DRAMCommandDist steadyDRAMCommands;
    
    /** the layout we are using */
    private static Layout<RawTile> layout;
    
    private static Router router;
    
    private static SpaceTimeSchedule spaceTimeSchedule;
    
    /**
     *  The entry of the rawify pass.  This function iterates over the 
     *  schedules for the 3 phases (init, priming, steady) and generates the
     *  compute code and communicate code necessary to execute the schedule!
     *  
     * @param schedule
     * @param rawChip
     * @param layout
     */
    public static void run(SpaceTimeSchedule schedule, RawChip rawChip, Layout<RawTile> layout) {
        Slice traces[];

        spaceTimeSchedule = schedule;
        
        if (KjcOptions.noswitchcomp)
            SWITCH_COMP = false;
        
        Rawify.layout = layout;
        
        ScheduleModel model = 
            new ScheduleModel(schedule, layout, schedule.getScheduleList());
        model.createModel();
        
        
        router = new SmarterRouter(model.getTileCosts(), rawChip);
        
        //determine the number of dram reads and writes to each port in
        //the steady state, this is necessary to throttle the issuing
        //tile during data-redistribution
        steadyDRAMCommands = new DRAMCommandDist(schedule.getSchedule(), 
                rawChip);
        steadyDRAMCommands.calcDRAMDist();
        steadyDRAMCommands.printDramCommands();
        
        //the initialization stage!!
        traces = schedule.getInitSchedule();
        iterateInorder(traces, SchedulingPhase.INIT, rawChip);
//      make sure all the dram command are completed before moving on
        RawComputeCodeStore.presynchAllDramsInInit();
        //the prime pump stage!!
        traces = schedule.getPrimePumpScheduleFlat();
        iterateInorder(traces, SchedulingPhase.PRIMEPUMP, rawChip);
//      make sure all the dram command are completed before moving on
        RawComputeCodeStore.presynchAllDramsInInit();
        //the steady-state!!
        traces = schedule.getSchedule();
        
        if (SpaceTimeBackend.NO_SWPIPELINE) {
            RawComputeCodeStore.barrier(rawChip, false, false);
            iterateNoSWPipe(schedule.getScheduleList(), SchedulingPhase.STEADY, rawChip);
        } else {
            //iterate over the joiners then the filters then 
            //the splitter, this will create a data-redistribution 
            //stage between the iterations that will improve performance 
            iterateJoinFiltersSplit(traces, SchedulingPhase.STEADY, rawChip);
        }
        RawComputeCodeStore.presynchEmptyTilesInSteady();
    }
   
    /**
     * Iterate over the schedule of traces and over each node of each trace and 
     * generate the code necessary to fire the schedule.  Generate splitters and 
     * joiners intermixed with the trace execution...
     * 
     * @param traces The schedule to execute.
     * @param whichPhase True if the init stage.
     * @param rawChip The raw chip
     */
    private static void iterateInorder(Slice traces[], SchedulingPhase whichPhase,
                                RawProcElements rawChip) {
        Slice slice;

        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
            //create code for joining input to the trace
            processInputSliceNode((InputSliceNode)slice.getHead(),
                    whichPhase, rawChip);
            //create the compute code and the communication code for the
            //filters of the trace
            processFilterSlices(slice, whichPhase, rawChip);
            //create communication code for splitting the output
            processOutputSliceNode((OutputSliceNode)slice.getTail(),
                    whichPhase, rawChip);
            
        }
    }
    
    /**
     * Iterate over the schedule of traces and over each node of each trace and 
     * generate the code necessary to fire the schedule.  Generate splitters and 
     * joiners first so that they will data will be redistributed before the filters
     * execute.
     * 
     * @param traces The schedule to execute.
     * @param whichPhase True if the init stage.
     * @param rawChip The raw chip
     */
    private static void iterateJoinFiltersSplit(Slice traces[], SchedulingPhase whichPhase,
                                RawProcElements rawChip) {
        Slice slice;

        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
            //create code for joining input to the trace
            processInputSliceNode((InputSliceNode)slice.getHead(),
                    whichPhase, rawChip);
        }
        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
            //create the compute code and the communication code for the
            //filters of the trace
            processFilterSlices(slice, whichPhase, rawChip);
        }
        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
            //create communication code for splitting the output
            processOutputSliceNode((OutputSliceNode)slice.getTail(),
                    whichPhase, rawChip);
        }
    }
    
    
    
    /**
     * Iterate over the schedule of traces and over each node of each trace and 
     * generate the code necessary to fire the schedule.  Generate splitters and 
     * joiners intermixed with the trace execution...
     * @param whichPhase True if the init stage.
     * @param rawChip The raw chip
     * @param traces The schedule to execute.
     */
    private static void iterateNoSWPipe(LinkedList<Slice> schedule, SchedulingPhase whichPhase,
                                RawChip rawChip) {
        
        HashSet<OutputSliceNode> hasBeenSplit = new HashSet<OutputSliceNode>();
        HashSet<InputSliceNode> hasBeenJoined = new HashSet<InputSliceNode>();
        LinkedList<Slice> scheduled = new LinkedList<Slice>();
        LinkedList<Slice> needToSchedule = (LinkedList<Slice>)schedule.clone();  
        
        while (needToSchedule.size() != 0) {
            {
                //join everyone that can be joined
                for (int n = 0; n < needToSchedule.size(); n++) {
                    Slice notSched = needToSchedule.get(n);
                    if (notSched.getHead().noInputs()) {
                        hasBeenJoined.add(notSched.getHead());
                        continue;
                    }
                    if (notSched.getHead().hasFileInput()) {
                        assert notSched.getHead().oneInput();
                        hasBeenJoined.add(notSched.getHead());
                    }
                        
                    Iterator<InterSliceEdge> inEdges = notSched.getHead().getSourceSet().iterator();
                    boolean canJoin = true;
                    while (inEdges.hasNext()) {
                        InterSliceEdge inEdge = inEdges.next();
                        if (!hasBeenSplit.contains(inEdge.getSrc())) {
                            canJoin = false;
                            break;
                        }
                    }
                    if (canJoin) {
                        //create code for joining input to the trace
                        hasBeenJoined.add(notSched.getHead());
                        //System.out.println("Scheduling join of " + notSched.getHead().getNextFilter());
                        processInputSliceNode(notSched.getHead(),
                                whichPhase, rawChip);
                    }
                }
                
                
                //create the compute code and the communication code for the
                //filters of the trace
                while (needToSchedule.size() != 0) {
                    Slice slice = needToSchedule.get(0);
                    if (hasBeenJoined.contains(slice.getHead())) {
                        scheduled.add(slice);
                        processFilterSlices(slice, whichPhase, rawChip);
                        //System.out.println("Scheduling " + trace.getHead().getNextFilter());
                        needToSchedule.removeFirst();
                    }
                    else {
                        break;
                    }
                }
                
                //split anyone that can be split
                for (int t = 0; t < scheduled.size(); t++) {
                    if (!hasBeenSplit.contains(scheduled.get(t).getTail())) {
                        OutputSliceNode output = 
                            scheduled.get(t).getTail();
                        //System.out.println("Scheduling split of " + output.getPrevFilter()); 
                        processOutputSliceNode(output,
                                whichPhase, rawChip);
                        hasBeenSplit.add(output);
                    }
                }
            }
            
           
            
           
        }
        
        
        //schedule any splits that have not occured!
        if (hasBeenSplit.size() != scheduled.size()) {
            for (int t = 0; t < scheduled.size(); t++) {
                if (!hasBeenSplit.contains(scheduled.get(t).getTail())) {
                    OutputSliceNode output = 
                        scheduled.get(t).getTail();
                    //System.out.println("Scheduling split of " + output.getPrevFilter()); 
                    processOutputSliceNode(output,
                            whichPhase, rawChip);
                    hasBeenSplit.add(output);
                }
            }
        }
        
        //schedule any joins that have not occured
        if (hasBeenJoined.size() != scheduled.size()) {
            for (int t = 0; t < scheduled.size(); t++) {
                if (!hasBeenJoined.contains(scheduled.get(t).getHead())) {
                    InputSliceNode input  = 
                        scheduled.get(t).getHead();
                    //System.out.println("Scheduling join of " + input.getNextFilter()); 
                    processInputSliceNode(input,
                            whichPhase, rawChip);
                    hasBeenJoined.add(input);
                }
            }
        }
        
        
        assert hasBeenJoined.size() == scheduled.size();
        assert hasBeenSplit.size() == scheduled.size();
        assert scheduled.size() == schedule.size();
    }
    
    /**
     * First generate the dram commands for input/output to/from the first/last
     * filters of the trace.  Then iterate over the filters of the trace and generate
     * computation and intra-trace communication (static net) code.
     *   
     * @param slice
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param rawChip
     */
    public static void processFilterSlices(Slice slice, SchedulingPhase whichPhase,
            RawProcElements rawChip) {
        //don't do anything for io because it is handled at other levels
        if (spaceTimeSchedule.getPartitioner().isIO(slice))
            return;
        //create the DRAM commands for trace input and output 
        //this is done before we create the compute code for the filters of the
        //trace
        if (slice.getHead().getNext().isFilterSlice())
            generateInputFilterDRAMCommand(slice.getHead().getNextFilter(), 
                    whichPhase, rawChip);
        if (slice.getTail().getPrevious().isFilterSlice())
            generateFilterOutputDRAMCommand(slice.getTail().getPrevFilter(), 
                    whichPhase);

        
        // revert to two boolean representation of scheduling stages
        // give up and go back to 2-boolean system of showing scheduling stages.
        boolean init = false;
        boolean primepump = false;
        switch (whichPhase) {
        case INIT:
            init = true;
            break;
        case PRIMEPUMP:
            primepump = true;
            break;
        default: 
        }

        // iterate over the filterNodes 
        
        //get the first traceNode that can be a filter
        SliceNode sliceNode = slice.getHead().getNext();
        while (sliceNode != null) {
            CommonUtils.println_debugging("Rawify: " + sliceNode);
            
            // do the appropiate code generation
            if (sliceNode.isFilterSlice()) {
                FilterSliceNode filterNode = (FilterSliceNode) sliceNode;
                assert !filterNode.isPredefined() : 
                    "Predefined filters should not appear in the trace traversal: "
                    + slice.toString();
                RawTile tile = layout.getComputeNode(filterNode); 
                // create the filter info class
                FilterInfo filterInfo = FilterInfo.getFilterInfo(filterNode);
                // add the dram command if this filter trace is an
                // endpoint...
                
                
                /*
                 * if (filterInfo.isLinear()) { //assert
                 * FilterInfo.getFilterInfo(filterNode).remaining == 0 :
                 * //"Items remaining on buffer for init for linear filter";
                 * createSwitchCodeLinear(filterNode,
                 * trace,filterInfo,init,primepump,tile,rawChip); } else {
                 */
                createCommunicationCode(filterNode, slice, filterInfo,
                        whichPhase, filterInfo.isLinear(), tile, rawChip);
                
               
                // }
                
                // tell the tile was is mapped to it!!
                tile.addSliceNode(init, primepump, filterNode);
                // this must come after createswitch code because of
                // compression
                addComputeCode(init, primepump, tile, filterInfo);
            } 
            // get the next tracenode
            sliceNode = sliceNode.getNext();
        }
    }

    /**
     * Create the dram commands and the switch code to implement the splitting described
     * by the output trace node.
     * 
     * @param traceNode
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
      * @param rawChip
     */
    public static void processOutputSliceNode(OutputSliceNode traceNode,
        SchedulingPhase whichPhase, RawProcElements rawChip) {
//        if (KjcOptions.magicdram)
//            return;
        
        assert StreamingDram.differentDRAMs(traceNode) : 
            "outputs for a single OutputSliceNode going to same DRAM";

        // revert to old 2-booleans representation of scheduling stage.
        boolean init = false;
        boolean primepump = false;
        switch (whichPhase) {
        case INIT:
            init = true;
            break;
        case PRIMEPUMP:
            primepump = true;
            break;
        default:
        }
        //handleFileOutput(traceNode, init,
        //        primepump, rawChip);
        // create the switch code to perform the splitting
        splitOutputSlice(traceNode, whichPhase);
        // generate the DRAM command
        // this must come after joinInputSlice because of switch
        // compression
        outputDRAMCommands(traceNode, whichPhase);
    }

    

    /**
     * Create dram commands and switch code to implement the joining described by the
     * input trace node. 
     * 
     * @param traceNode
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param rawChip
     */
    public static void processInputSliceNode(InputSliceNode traceNode, 
            SchedulingPhase whichPhase, RawProcElements rawChip) {
//        if (KjcOptions.magicdram) 
//            return; 
        assert StreamingDram.differentDRAMs(traceNode) : 
            "inputs for a single InputSliceNode coming from same DRAM";
        //handleFileInput(traceNode, init,
        //        primepump, rawChip);
        // create the switch code to perform the joining
        joinInputSlice(traceNode, whichPhase);
        // generate the dram command to execute the joining
        // this must come after joinInputSlice because of switch
        // compression
        generateInputDRAMCommands(traceNode, whichPhase);
    }
    
    /** 
     * Based on what phase we are currently in, generate the compute code 
     * (filter) code to execute the phase at this currently.  This is done 
     * in ComputeCodeStore.java.
     * 
     * @param init
     * @param primepump
     * @param tile
     * @param filterInfo
     */
    private static void addComputeCode(boolean init, boolean primepump,
                                       ComputeNode tile, FilterInfo filterInfo) {
        if (init)
            tile.getComputeCode().addSliceInit(filterInfo, layout);
        else if (primepump)
            tile.getComputeCode().addSlicePrimePump(filterInfo, layout);
        else  //steady
            tile.getComputeCode().addSliceSteady(filterInfo, layout);
    }

    /**
     * For an input trace node of a trace, if it is connected to file reader, 
     * then generate the commands to read from the file reader device.  This is necessary
     * because the file reader is not listed in the schedules.
     * 
     * @param input
     * @param init
     * @param primepump
     * @param chip
     */
    private static void handleFileInput(InputSliceNode input,
            SchedulingPhase whichPhase, RawProcElements chip) {
        // if there are no files, do nothing
        if (!input.hasFileInput())
            return;
        for (int i = 0; i < input.getSources().length; i++) {
            // do nothing for non-file readers
            if (!input.getSources()[i].getSrc().isFileInput())
                continue;

            OutputSliceNode fileO = input.getSources()[i].getSrc();

            assert fileO.getPrevFilter().getFilter() instanceof FileInputContent : "FileReader should be a FileInputContent";

            // now generate the code, both the dram commands and the switch code
            // to perform the splitting, if there is only one output, do nothing
            if (!OffChipBuffer.unnecessary(fileO)) {
                // generate dram command
                outputDRAMCommands(fileO, whichPhase);
                // perform the splitting
                splitOutputSlice(fileO, whichPhase);
            }
        }
    }


    /**
     * Generate the dram commands (on the compute tiles associated with each dram port)
     * necessary to join (if necessary) the input for this input trace node.
     * 
     * @param input
     * @param init
     * @param primepump
     */
    private static void generateInputDRAMCommands(InputSliceNode input,
                                                  SchedulingPhase whichPhase) {
        FilterSliceNode filter = (FilterSliceNode) input.getNext();

        // do not generate the code if it is not necessary
        if (OffChipBuffer.unnecessary(input))
            return;

        // number of total items that are being joined
        int items = FilterInfo.getFilterInfo(filter).totalItemsReceived(whichPhase);

        // do nothing if there is nothing to do
        if (items == 0)
            return;
    
        assert items % input.totalWeights() == 0 : "weights on input trace node does not divide evenly with items received";
        // iterations of "joiner"
        int iterations = items / input.totalWeights();
        int typeSize = Util.getTypeSize(filter.getFilter().getInputType());

        // generate the commands to read from the o/i temp buffer
        // for each input to the input trace node
        Iterator<InterSliceEdge> edges = input.getSourceSet().iterator(); 
        while (edges.hasNext()) {
            InterSliceEdge edge = edges.next();
            // get the first non-redundant buffer         
            OffChipBuffer srcBuffer = 
                InterSliceBuffer.getBuffer(edge).getNonRedundant();
            
            assert srcBuffer != null;
            
            CommonUtils.println_debugging("Generate the DRAM read command for "
                                     + srcBuffer);
            int readWords = iterations * typeSize
                * input.getItems(edge);
            if (srcBuffer.getDest() instanceof OutputSliceNode
                && ((OutputSliceNode) srcBuffer.getDest()).isFileInput())
                srcBuffer.getOwner().getComputeCode().addFileCommand(true,
                        SchedulingPhase.isInitOrPrimepump(whichPhase),
                        readWords, srcBuffer, true);
            else
                srcBuffer.getOwner().getComputeCode().addDRAMCommand(true,
                        whichPhase, Util.cacheLineDiv(readWords * 4), 
                        srcBuffer, true);
        }

        // generate the command to write to the dest of the input trace node
        OffChipBuffer destBuffer = IntraSliceBuffer.getBuffer(input, filter);
        int writeWords = items * typeSize;
        if (input.isFileOutput()) {  // && OffChipBuffer.unnecessary(input))
            destBuffer.getOwner().getComputeCode().addFileCommand(false,
                    SchedulingPhase.isInitOrPrimepump(whichPhase),
                    writeWords, destBuffer, true);
        }
        else
            destBuffer.getOwner().getComputeCode().addDRAMCommand(false,
                    whichPhase, Util.cacheLineDiv(writeWords * 4), 
                    destBuffer, true);
    }

    /**
     * For an output trace node, generate the dram commands to write the data
     * to the temp buffers that are between it and its dest.
     * 
     * @param output
     * @param init
     * @param primepump
     */
    private static void outputDRAMCommands(OutputSliceNode output,
                SchedulingPhase whichPhase) {
        FilterSliceNode filter = (FilterSliceNode) output.getPrevious();
 
        // don't do anything for a redundant buffer
        if (OffChipBuffer.unnecessary(output))
            return;

        OffChipBuffer srcBuffer = IntraSliceBuffer.getBuffer(filter, output);
        int readWords = FilterInfo.getFilterInfo(filter).totalItemsSent(whichPhase)
            * Util.getTypeSize(filter.getFilter().getOutputType());
        if (readWords > 0) {
            CommonUtils.println_debugging("Generating the read command for "
                                     + output + " on " + srcBuffer.getOwner()
                                     + (whichPhase.equals(SchedulingPhase.PRIMEPUMP) ? "(primepump)" : ""));
            // in the primepump stage a real output trace always reads from the
            // init buffers
            // never use stage 2 for reads
            if (output.isFileInput()) {// && OffChipBuffer.unnecessary(output))
                srcBuffer.getOwner().getComputeCode().addFileCommand(true,
                        SchedulingPhase.isInitOrPrimepump(whichPhase),
                        readWords, srcBuffer, true);
            }
            else
                srcBuffer.getOwner().getComputeCode().addDRAMCommand(true,
                        whichPhase, 
                        Util.cacheLineDiv(readWords * 4),
                        srcBuffer, true);
        }

        // now generate the store drm command
        Iterator dests = output.getDestSet().iterator();
        while (dests.hasNext()) {
            InterSliceEdge edge = (InterSliceEdge) dests.next();
            InterSliceBuffer destBuffer = InterSliceBuffer.getBuffer(edge);
            int typeSize = Util.getTypeSize(edge.getType());
            int writeWords = typeSize;
            // do steady-state
            switch (whichPhase) {
            case STEADY:
                writeWords *= edge.steadyItems();
                break;
            case INIT:
                writeWords *= edge.initItems();
                break;
            case PRIMEPUMP:
                writeWords *= edge.primePumpItems();
                break;
            }
            // make write bytes cache line div
            if (writeWords > 0) {
                if (destBuffer.getEdge().getDest().isFileOutput()
                    && OffChipBuffer.unnecessary(destBuffer.getEdge()
                                                 .getDest()))
                    destBuffer.getOwner().getComputeCode().addFileCommand(false, 
                            SchedulingPhase.isInitOrPrimepump(whichPhase),
                            writeWords, destBuffer, true);
                else
                    destBuffer.getOwner().getComputeCode().addDRAMCommand(false, 
                                whichPhase, Util.cacheLineDiv(writeWords * 4),
                                destBuffer, true);
            }
        }
    }


  
    /**
     * Generate the dram command for the input for a filter from the dram after
     * it is joined into the proper dram.
     */
    private static void generateInputFilterDRAMCommand(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, RawProcElements rawChip) {
        // only generate a DRAM command for filters connected to input or output
        // trace nodes
        if (filterNode.getPrevious() != null
            && filterNode.getPrevious().isInputSlice()) {
            //get the buffer, and use it to decide which network to use
            IntraSliceBuffer buffer = 
                IntraSliceBuffer.getBuffer((InputSliceNode) filterNode.getPrevious(), 
                        filterNode);
            
            // get this buffer or this first upstream non-redundant buffer
            // use this for for the address of the buffer we are transfering from
            OffChipBuffer nonRedBuffer = buffer.getNonRedundant();

            if (nonRedBuffer == null)
                return;

            // get the number of items received
            int items = 
                FilterInfo.getFilterInfo(filterNode).totalItemsReceived(whichPhase); 

            // return if there is nothing to receive
            if (items == 0)
                return;

            // the transfer size rounded up to by divisible by a cacheline
            int words = (items * Util.getTypeSize(filterNode.getFilter()
                                                  .getInputType()));

            // in the case of a gdn load that has a different destination 
            // than the owning tile, we must use a special dram command
            if (nonRedBuffer.getOwner() != layout.getComputeNode(filterNode)) {
                assert false : "For InputSliceNode: " + filterNode +  
                "must be at home time of DRAM or use GDN!";
            
                if (Util.onlyFileInput((InputSliceNode)filterNode.getPrevious()))
                    nonRedBuffer.getOwner().getComputeCode().addFileGDNReadCommand
                    (whichPhase, words, nonRedBuffer, 
                            layout.getComputeNode(filterNode));
                else
                    nonRedBuffer.getOwner().getComputeCode().addDRAMGDNReadCommand(whichPhase,
                            Util.cacheLineDiv(words * 4), 
                            nonRedBuffer, true, 
                            layout.getComputeNode(filterNode));
            }
            else {
                //generate commands to get the input for the filter from a dram or from 
                //a file, use the non-redundant upstream buffer for the address, but
                //this buffer's network assignment
                if (Util.onlyFileInput((InputSliceNode)filterNode.getPrevious()))
                    nonRedBuffer.getOwner().getComputeCode().addFileCommand(true,
                            SchedulingPhase.isInitOrPrimepump(whichPhase),
                            words, nonRedBuffer, buffer.isStaticNet());
                else
                    nonRedBuffer.getOwner().getComputeCode().addDRAMCommand(true, 
                            whichPhase, Util.cacheLineDiv(words * 4), 
                            nonRedBuffer, buffer.isStaticNet());
            }
        }
    }

    /**
     * Generate the streaming dram command to send the output from the filter
     * tile to the dram before it is split (if necessary).
     */
    private static void generateFilterOutputDRAMCommand(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase) {
        if (filterNode.getNext() != null
            && filterNode.getNext().isOutputSlice()) {
            // get this buffer or null if there are no inputs and use it 
            // to decide if we should use the gdn or the static network
            OutputSliceNode output = (OutputSliceNode) filterNode.getNext();
            IntraSliceBuffer buffer = IntraSliceBuffer.getBuffer(filterNode,
                                                              output);
            //get the non redundant buffer for the buffer address that we should write into
            OffChipBuffer nonRedBuffer = buffer.getNonRedundant();
            if (nonRedBuffer == null)
                return;

            // get the number of items sent
            int items = FilterInfo.getFilterInfo(filterNode).totalItemsSent(whichPhase);

            //generate the commands to store the outpout of the last
            //filter of the trace to a dram or filter
            //use the non-redundant buffer's address, but this buffer's
            //network assignemnt
            if (items > 0) {
                int words = (items * Util.getTypeSize(filterNode.getFilter()
                                                      .getOutputType()));

                //if we are using the dynamic network and the trace
                //does not occupy the owner tile of this buffer, 
                //then we have to synch the command with the store of the data
                //so we have to issue a command and send a word over the 
                //static network from the issuing tile (owner) to the storing
                //tile
                if (!buffer.isStaticNet() && 
                        !Util.doesSliceUseTile(filterNode.getParent(),
                                nonRedBuffer.getOwner(), layout)) {
                    gdnStoreCommandWithSynch(whichPhase, Util.onlyWritingToAFile(output),
                            words, nonRedBuffer);
                    return;
                }
                    
                //otherwise, the is a normal dram command that does not
                //need to be synchronized
                if (Util.onlyWritingToAFile(output))
                    nonRedBuffer.getOwner().getComputeCode().addFileCommand(false,
                            SchedulingPhase.isInitOrPrimepump(whichPhase),
                            words, nonRedBuffer, buffer.isStaticNet());
                else {
                    CommonUtils
                        .println_debugging("Generating DRAM store command with "
                                 + items
                                 + " items, typesize "
                                 + Util.getTypeSize(filterNode.getFilter()
                                                    .getOutputType()) + " and " + words
                                 + " words");
                    nonRedBuffer.getOwner().getComputeCode().addDRAMCommand(false, 
                            whichPhase,
                            Util.cacheLineDiv(words * 4), 
                            nonRedBuffer, buffer.isStaticNet());
                }
            }
        }
    }

    /**
     * If we have a gdn store command that is issued from the
     * owner tile but the data is written to the dram (or file) from another
     * tile and there is no synchronization between the two, we must
     * send a word from the owner to the generating tile to tell it that
     * the dram is ready to receive data for the store.  
     * 
     * This function will generate the dram or file store command over the gdn
     * and inject the synch word onto the static network and route the word
     * to the dest, generating tile.  
     * 
     * The generating tile will wait for the word before executing the
     * filter that writes the data (see RawExecutionCode, setupGDNStore()
     *  
     * @param init
     * @param primepump
     * @param fileCommand
     * @param words
     * @param buffer
     */
    private static void gdnStoreCommandWithSynch(SchedulingPhase whichPhase, 
            boolean fileCommand, int words,
            OffChipBuffer buffer) {
        //issue the command for storing using either the memory or a file
        //this method will also inject the word on
        if (fileCommand)
            buffer.getOwner().getComputeCode().addGDNFileStoreCommandWithSynch(
                    SchedulingPhase.isInitOrPrimepump(whichPhase),
                    words, buffer);
        else 
            buffer.getOwner().getComputeCode().addGDNStoreCommandWithSynch(whichPhase,
                    Util.cacheLineDiv(words * 4), buffer);
        
        //now we need to create the switch code to route the synch word from 
        //source to dest!!
        Slice srcSlice = buffer.getSource().getParent();
        //get the raw chip that is write the data (sending it over the gdn)...
        RawTile srcTile = layout.getComputeNode(srcSlice.getTail().getPrevFilter());
                
        //generate the switch code to send the item from the owner 
        //to the srcTile of the data
        SwitchCodeStore.generateSwitchCode(router, buffer.getOwner(), 
                new RawComputeNode[]{srcTile}, (SchedulingPhase.isInitOrPrimepump(whichPhase) ? 1 : 2));
    }
    
    /**
     * see if the switch for the filter needs disregard some of the input because 
     * the data that we have received is not a multiple of the cacheline.   
     * We are using the static network, the switch just disregards the data. 
     * 
     * @param rawChip
     * @param traceNode
     * @param init
     * @param primepump
     * @param items The number of items we sent in this stage.
     * @param staticNet true if we are using the static network.
     */
    
    private static void handleUnneededInputStatic(RawProcElements rawChip, FilterSliceNode traceNode,
            SchedulingPhase whichPhase, int items) {
        InputSliceNode in = (InputSliceNode) traceNode.getPrevious();

        FilterInfo filterInfo = FilterInfo.getFilterInfo(traceNode);
        // int items = filterInfo.totalItemsReceived(init, primepump), typeSize;
        int typeSize;

        typeSize = Util.getTypeSize(traceNode.getFilter().getInputType());

        // see if it is a mulitple of the cache line
        if ((items * typeSize) % RawChip.cacheLineWords != 0) {
            int dummyWords = RawChip.cacheLineWords
                - ((items * typeSize) % RawChip.cacheLineWords);
            CommonUtils.println_debugging("Received items (" + (items * typeSize)
                                     + ") not divisible by cache line, disregard " + dummyWords);
            
            SwitchCodeStore.disregardIncoming(IntraSliceBuffer.getBuffer(in,
                    traceNode).getDRAM(), dummyWords, 
                    SchedulingPhase.isInitOrPrimepump(whichPhase));
        }
    }

    /**
     * See if the we need to generate dummy values to fill a cache line in 
     * the streaming dram. This is necessary because all transfers must cache-line 
     * aligned.  We are using the static network, so the switch will send the
     * dummy values.
     * 
     * @param rawChip
     * @param traceNode
     * @param init
     * @param primepump
     * @param items
     */
    private static void fillCacheLineStatic(RawProcElements rawChip, FilterSliceNode traceNode, boolean init,
                                      boolean primepump, int items) {
        OutputSliceNode out = (OutputSliceNode) traceNode.getNext();
     
        // get the number of items sent
        // int items = filterInfo.totalItemsSent(init, primepump), typeSize;
        int typeSize;

        typeSize = Util.getTypeSize(traceNode.getFilter().getOutputType());
        // see if a multiple of cache line, if not generate dummy values...
        if ((items * typeSize) % RawChip.cacheLineWords != 0) {
            int dummyWords = RawChip.cacheLineWords
                - ((items * typeSize) % RawChip.cacheLineWords);
            CommonUtils.println_debugging("Sent items (" + (items * typeSize)
                                     + ") not divisible by cache line, add " + dummyWords);
            
            SwitchCodeStore.dummyOutgoing
            (IntraSliceBuffer.getBuffer(traceNode,
                    out).getDRAM(), dummyWords, init || primepump);
        }
    }
    
    /**
     * Generate the switch code necessary to perform joining
     * of a inputtracenode's input.  At this point all the inputs are in located at their
     * ports and are ready to be joined by the switches (on the bitches).
     * 
     * Sorry for the long function!
     *  
     * @param traceNode
     * @param init
     * @param primepump
     */
    private static void joinInputSlice(InputSliceNode traceNode, SchedulingPhase whichPhase) {
        FilterSliceNode filter = (FilterSliceNode) traceNode.getNext();

        // do not generate the switch code if it is not necessary
        if (OffChipBuffer.unnecessary(traceNode))
            return;

        FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
        // calculate the number of items received
        int items = filterInfo.totalItemsReceived(whichPhase), iterations, stage = 1, typeSize;

        // noting to do for this stage
        if (items == 0)
            return;

        // the stage we are generating code for as used below for
        // generateSwitchCode()
        if (! SchedulingPhase.isInitOrPrimepump(whichPhase)) {
            stage = 2;
        }
        // [also, give up and go back to the old two boolean method for scheduling stages]
        boolean init = false;
        boolean primepump = false;
        switch (whichPhase) {
        case INIT:
            init = true;
            break;
        case PRIMEPUMP:
            primepump = true;
            break;
        default:
        }

        typeSize = Util.getTypeSize(filter.getFilter().getInputType());
        // the numbers of times we should cycle thru this "joiner"
        assert items % traceNode.totalWeights() == 0 : "weights on input trace node does not divide evenly with items received";
        iterations = items / traceNode.totalWeights();

        StreamingDram[] dest = { IntraSliceBuffer.getBuffer(traceNode, filter)
                                 .getDRAM() };
                
        // generate comments to make the code easier to read when debugging
        dest[0].getNeighboringTile().getSwitchCode().appendComment(SchedulingPhase.isInitOrPrimepump(whichPhase),
                                                                   "Start join: This is the dest (" + filter.toString() + ")");

        Iterator<InterSliceEdge> sources = traceNode.getSourceSet().iterator();
        while (sources.hasNext()) {
            StreamingDram dram = 
                InterSliceBuffer.getBuffer((InterSliceEdge) sources.next()).getNonRedundant().getDRAM();
            dram.getNeighboringTile().getSwitchCode().
                    appendComment(SchedulingPhase.isInitOrPrimepump(whichPhase),
                            "Start join: This a source (" + dram.toString() + ")");
        }

        //generate the switch code...
        if (SWITCH_COMP && iterations > SC_THRESHOLD) {
            // create a loop to compress the switch code

            // find all the tiles used in this join
            HashSet<RawComputeNode> tiles = new HashSet<RawComputeNode>();
            for (int j = 0; j < traceNode.getWeights().length; j++) {
                // get the source buffer, pass thru redundant buffer(s)
                StreamingDram source = InterSliceBuffer.getBuffer(
                                                                  traceNode.getSources()[j]).getNonRedundant().getDRAM();
                tiles.addAll(SwitchCodeStore.getTilesInRoutes(router, source, dest));
            }
            // generate the loop header on all tiles involved
            HashMap<RawTile, Label> labels = SwitchCodeStore.switchLoopHeader(tiles,
                                                              iterations, init, primepump);
            // generate the switch instructions
            for (int j = 0; j < traceNode.getWeights().length; j++) {
                // get the source buffer, pass thru redundant buffer(s)
                StreamingDram source = InterSliceBuffer.getBuffer(
                                                                  traceNode.getSources()[j]).getNonRedundant().getDRAM();
                for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                    for (int q = 0; q < typeSize; q++)
                        SwitchCodeStore.generateSwitchCode(router, source, dest, stage);
                }
            }
            // generate the loop trailer
            SwitchCodeStore.switchLoopTrailer(labels, init, primepump);
        } else {
            for (int i = 0; i < iterations; i++) {
                for (int j = 0; j < traceNode.getWeights().length; j++) {
                    // get the source buffer, pass thru redundant buffer(s)
                    StreamingDram source = 
                        InterSliceBuffer.getBuffer(traceNode.getSources()[j]).getNonRedundant()
                        .getDRAM();
                    for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                        for (int q = 0; q < typeSize; q++)
                            SwitchCodeStore.generateSwitchCode(router, source, dest,
                                                               stage);
                    }
                }
            }
        }

        // because transfers must be cache line size divisible...
        // generate dummy values to fill the cache line!
        if ((items * typeSize) % RawChip.cacheLineWords != 0
            && !traceNode.isFileOutput()) {
            int dummy = RawChip.cacheLineWords
                - ((items * typeSize) % RawChip.cacheLineWords);
            SwitchCodeStore.dummyOutgoing(dest[0], dummy, init || primepump);
        }
        // disregard remainder of inputs coming from temp offchip buffers
        Iterator<InterSliceEdge> edges = traceNode.getSourceSet().iterator();
        while (edges.hasNext()) {
            InterSliceEdge edge = edges.next();
            if (edge.getSrc().isFileInput())
                continue;
            int remainder = ((iterations * typeSize * traceNode.getItems(edge)) % 
                    RawChip.cacheLineWords);
            if (remainder > 0
                && !(edge.getSrc().isFileInput() && OffChipBuffer
                     .unnecessary(edge.getSrc())))
                SwitchCodeStore.disregardIncoming(InterSliceBuffer.getBuffer(
                                                                             edge).getDRAM(), RawChip.cacheLineWords - remainder,
                                                  init || primepump);
        }

        // generate comments to make the code easier to read when debugging
        dest[0].getNeighboringTile().getSwitchCode().appendComment(
                                                                   init || primepump,
                                                                   "End join: This is the dest (" + filter.toString() + ")");

       //generate some comments
        sources = traceNode.getSourceSet().iterator();
        while (sources.hasNext()) {
            StreamingDram dram = InterSliceBuffer.getBuffer(
                                                            (InterSliceEdge) sources.next()).getNonRedundant().getDRAM();
            dram.getNeighboringTile().getSwitchCode().appendComment(
                                                                    init || primepump,
                                                                    "End join: This a source (" + dram.toString() + ")");
        }
    }

    
    
    
    /**
     * Generate the switch code to split the output trace into its necessary temp
     * buffers.  This function will create loops (if applicable) and call 
     * performSplitOutputSlice to actually generate each switch instruction.  So this
     * function is responsible for code organization.
     * 
     * Another long function!
     * 
     * @param traceNode
     * @param init
     * @param primepump
     */
    private static void splitOutputSlice(OutputSliceNode traceNode,
                                         SchedulingPhase whichPhase)

    {
        FilterSliceNode filter = (FilterSliceNode) traceNode.getPrevious();
        // check to see if the splitting is necessary
        if (OffChipBuffer.unnecessary(traceNode))
            return;

        FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
        // calculate the number of items sent
        int items = filterInfo.totalItemsSent(whichPhase);
        StreamingDram sourcePort = IntraSliceBuffer
            .getBuffer(filter, traceNode).getDRAM();
        
        // revert to two boolean representation of scheduling stages
        // give up and go back to 2-boolean system of showing scheduling stages.
        boolean init = false;
        boolean primepump = false;
        switch (whichPhase) {
        case INIT:
            init = true;
            break;
        case PRIMEPUMP:
            primepump = true;
            break;
        default: 
        }

        // the numbers of times we should cycle thru this "splitter"
        assert items % traceNode.totalWeights() == 0 : "weights on output trace node does not divide evenly with items sent";
        int iterations = items / traceNode.totalWeights();

        // add some comments to the switch code
        sourcePort.getNeighboringTile().getSwitchCode().appendComment(
                                                                      init || primepump,
                                                                      "Start split: This is the source (" + filter.toString() + ")");
        Iterator dests = traceNode.getDestSet().iterator();
        while (dests.hasNext()) {
            StreamingDram dram = InterSliceBuffer
                .getBuffer((InterSliceEdge) dests.next()).getDRAM();
            dram.getNeighboringTile().getSwitchCode().appendComment(
                                                                    init || primepump,
                                                                    "Start split: This a dest (" + dram.toString() + ")");
        }

        // SpaceTimeBackend.println("Split Output Slice: " + traceNode + "it: "
        // + iterations + " ppSteadyIt: " +
        // ppSteadyIt);
        // System.out.println(traceNode.debugString());

        // see if we want to compress (loop) the switch instructions, we cannot
        if (SWITCH_COMP && iterations > SC_THRESHOLD) {
            assert iterations > 1;
            Iterator<RawComputeNode> tiles = getTilesUsedInSplit(traceNode,
                                                 IntraSliceBuffer.getBuffer(filter, traceNode).getDRAM())
                .iterator();

            HashMap<RawTile, Label> labels = new HashMap<RawTile, Label>();
            while (tiles.hasNext()) {
                RawTile tile = (RawTile) tiles.next();
                // loop me
                // send over both constants
                Util.sendConstFromTileToSwitch(tile, iterations - 1, init,
                                               primepump, FILTER_FIRE_LOOP_REG);
                
                // label 1
                Label label = new Label();
                tile.getSwitchCode().appendIns(label, (init || primepump));
                labels.put(tile, label);
            }

            performSplitOutputSlice(traceNode, filter, filterInfo, init,
                                    primepump, 1);

            // now generate the jump back
            tiles = getTilesUsedInSplit(traceNode,
                                        IntraSliceBuffer.getBuffer(filter, traceNode).getDRAM())
                .iterator();
            while (tiles.hasNext()) {
                RawTile tile = (RawTile) tiles.next();
                Label label = labels.get(tile);
                // add the branch back
                BnezdIns branch = new BnezdIns(FILTER_FIRE_LOOP_REG,
                                               FILTER_FIRE_LOOP_REG, label.getLabel());
                tile.getSwitchCode().appendIns(branch, (init || primepump));
            }

            // end loop

            fillCacheLineSplitOutputSlice(traceNode, filter, filterInfo, init,
                                          primepump, iterations);
        } else { //no compression
            performSplitOutputSlice(traceNode, filter, filterInfo, init,
                                    primepump, iterations);
            fillCacheLineSplitOutputSlice(traceNode, filter, filterInfo, init,
                                          primepump, iterations);

        }

        // because transfers must be cache line size divisible...
        // disregard the dummy values coming out of the dram
        // for the primepump we always read out of the init buffer for real
        // output tracenodes
        int typeSize = Util.getTypeSize(filterInfo.filter.getOutputType());
        int mod = (((iterations) * traceNode.totalWeights() * typeSize) % RawChip.cacheLineWords);
        // don't cache align file readers
        if (mod > 0
            && !traceNode.isFileInput()) {
            int remainder = RawChip.cacheLineWords - mod;
            // System.out.println("Remainder for disregarding input on split
            // trace: " + remainder);
            SwitchCodeStore.disregardIncoming(sourcePort, remainder, init
                                              || primepump);
        }

        // add some comments to the switch code
        sourcePort.getNeighboringTile().getSwitchCode().appendComment(
                                                                      init || primepump,
                                                                      "End split: This is the source (" + filter.toString() + ")");
        dests = traceNode.getDestSet().iterator();
        while (dests.hasNext()) {
            StreamingDram dram = InterSliceBuffer
                .getBuffer((InterSliceEdge) dests.next()).getDRAM();
            dram.getNeighboringTile().getSwitchCode().appendComment(
                                                                    init || primepump,
                                                                    "End split: This a dest (" + dram.toString() + ")");
        }

    }

    /**
     * Generate the actual switch instructions to perform the splitting of the output trace
     * for the given number of iterations in the given stage.  splitOutputSlice above is
     * responsible for code organization (i
     * 
     * @param traceNode
     * @param filter
     * @param filterInfo
     * @param init
     * @param primepump
     * @param iterations
     */
    private static void performSplitOutputSlice(OutputSliceNode traceNode,
                                                FilterSliceNode filter, FilterInfo filterInfo, boolean init,
                                                boolean primepump, int iterations)
    {
        if (iterations > 0) {
            int stage = 1, typeSize;
            // the stage we are generating code for as used below for
            // generateSwitchCode()
            if (!init && !primepump)
                stage = 2;

            typeSize = Util.getTypeSize(filter.getFilter().getOutputType());

            CommonUtils.println_debugging("Generating Switch Code for " + traceNode
                                     + " iterations " + iterations);

            StreamingDram sourcePort = IntraSliceBuffer.getBuffer(filter, traceNode).getDRAM();
          
            for (int i = 0; i < iterations; i++) {
                for (int j = 0; j < traceNode.getWeights().length; j++) {
                    for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                        // generate the array of compute node dests
                        RawComputeNode dests[] = new RawComputeNode[traceNode
                                                              .getDests()[j].length];
                        for (int d = 0; d < dests.length; d++)
                            dests[d] = InterSliceBuffer.getBuffer(
                                                                  traceNode.getDests()[j][d]).getDRAM();
                        for (int q = 0; q < typeSize; q++)
                            SwitchCodeStore.generateSwitchCode(router, sourcePort,
                                                               dests, stage);
                    }
                }
            }
        }
    }

    /**
     * For each temp buffer used in the split, make sure that the data that it is 
     * writing to the dram port is a multiple of the cache line size, if not 
     * then write garbage.
     * 
     * @param traceNode
     * @param filter
     * @param filterInfo
     * @param init
     * @param primepump
     * @param iterations
     */
    private static void fillCacheLineSplitOutputSlice(OutputSliceNode traceNode, 
            FilterSliceNode filter,
            FilterInfo filterInfo, boolean init, boolean primepump,
            int iterations) {
        if (iterations > 0) {
            int typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
            // write dummy values into each temp buffer with a remainder
            Iterator it = traceNode.getDestSet().iterator();
            while (it.hasNext()) {
                InterSliceEdge edge = (InterSliceEdge) it.next();
                if (edge.getDest().isFileOutput())
                    continue;
                int remainder = ((typeSize * iterations * traceNode
                                  .getWeight(edge)) % RawChip.cacheLineWords);
                // don't fill cache line for files
                if (remainder > 0
                    && !(edge.getDest().isFileOutput() && OffChipBuffer
                         .unnecessary(edge.getDest())))
                    SwitchCodeStore.dummyOutgoing(InterSliceBuffer.getBuffer(
                                                                             edge).getDRAM(),
                                                  RawChip.cacheLineWords - remainder, init
                                                  || primepump);
            }
        }
    }

    /**
     * For a split outputtracenode, get the tiles used in the splitting, this includes
     * all tiles whose switches have code on them.
     * 
     * @param traceNode
     * @param sourcePort
     * @return Set of tiles used in the splitting
     */
    public static HashSet<RawComputeNode> getTilesUsedInSplit(OutputSliceNode traceNode,
                                              StreamingDram sourcePort) {
        // find all the tiles used in the split
        HashSet<RawComputeNode> tiles = new HashSet<RawComputeNode>();
        for (int j = 0; j < traceNode.getWeights().length; j++) {
            for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                // generate the array of compute node dests
                RawComputeNode dests[] = new RawComputeNode[traceNode.getDests()[j].length];
                for (int d = 0; d < dests.length; d++)
                    dests[d] = InterSliceBuffer.getBuffer(traceNode.getDests()[j][d]).getDRAM();
                tiles.addAll(SwitchCodeStore.getTilesInRoutes(router, sourcePort, dests));
            }
        }
        return tiles;
    }

    /**
     * This shit is wack yo!
     * 
     * @param node
     * @param filterInfo
     * @param mult
     * @param buffer
     * @param tile
     * @param rawChip
     */
    private static void createInitLinearSwitchCode(FilterSliceNode node,
                                                   FilterInfo filterInfo, int mult, int buffer, RawTile tile,
                                                   RawProcElements rawChip) {
        System.err.println("Creating switchcode linear: " + node + " " + mult);
        RawComputeNode sourceNode = null;
        // Get sourceNode and input port
        if (node.getPrevious().isFilterSlice())
            sourceNode = layout.getComputeNode(((FilterSliceNode) node.getPrevious()));
                                         
        else {
//            if (KjcOptions.magicdram && node.getPrevious() != null
//                && node.getPrevious().isInputSlice() && tile.hasIODevice())
//                sourceNode = tile.getIODevice();
//            else
                sourceNode = IntraSliceBuffer.getBuffer(
                                                        (InputSliceNode) node.getPrevious(), node)
                    .getNonRedundant().getDRAM();
        }
        SwitchIPort src = rawChip.getIPort(sourceNode, tile);
        SwitchIPort src2 = rawChip.getIPort2(sourceNode, tile);
        sourceNode = null;
        // Get destNode and output port
        RawComputeNode destNode = null;
        if (node.getNext().isFilterSlice())
            destNode = layout.getComputeNode(((FilterSliceNode) node.getNext()));
        else {
//            if (KjcOptions.magicdram && node.getNext() != null
//                && node.getNext().isOutputSlice() && tile.hasIODevice())
//                destNode = tile.getIODevice();
//            else {
                destNode = IntraSliceBuffer.getBuffer(node,
                                                      (OutputSliceNode) node.getNext()).getNonRedundant()
                    .getDRAM();
//            }
        }
        SwitchOPort dest = rawChip.getOPort(tile, destNode);
        SwitchOPort dest2 = rawChip.getOPort2(tile, destNode);
        SwitchCodeStore code = tile.getSwitchCode();
        // Get filter properties
        FilterContent content = node.getFilter();
        final int numCoeff = content.getArray().length;
        final int peek = content.getPeek();
        final int pop = content.getPopCount();
        final int numPop = numCoeff / pop;
        final boolean begin = content.getBegin();
        final boolean end = content.getEnd();
        final int pos = content.getPos();
        final int turns = mult - numPop;
        int bufferRemaining = buffer;
        if (begin) {
            // preloop
            FullIns ins = new FullIns(tile);
            if (end)
                ins.addRoute(SwitchIPort.CSTO, dest);
            else
                ins.addRoute(SwitchIPort.CSTO, dest2);
            code.appendIns(ins, true);
            bufferRemaining -= pop * numPop;
            // steadyloop
            for (int i = 0; i < turns; i++) {
                for (int j = 0; j < pop; j++) {
                    if (bufferRemaining > 0) {
                        if (!end) {
                            ins = new FullIns(tile);
                            ins.addRoute(SwitchIPort.CSTO, dest);
                            code.appendIns(ins, true);
                        }
                        bufferRemaining--;
                    } else {
                        ins = new FullIns(tile);
                        ins.addRoute(src, SwitchOPort.CSTI);
                        if (!end)
                            ins.addRoute(src, dest);
                        code.appendIns(ins, true);
                    }
                }
                ins = new FullIns(tile);
                if (end)
                    ins.addRoute(SwitchIPort.CSTO, dest);
                else
                    ins.addRoute(SwitchIPort.CSTO, dest2);
                code.appendIns(ins, true);
            }
            // postloop
            for (int i = 0; i < numPop - 1; i++) {
                for (int j = 0; j < pop; j++) {
                    if (bufferRemaining > 0) {
                        if (!end) {
                            ins = new FullIns(tile);
                            ins.addRoute(SwitchIPort.CSTO, dest);
                            code.appendIns(ins, true);
                        }
                        bufferRemaining--;
                    } else {
                        ins = new FullIns(tile);
                        ins.addRoute(src, SwitchOPort.CSTI);
                        if (!end)
                            ins.addRoute(src, dest);
                        code.appendIns(ins, true);
                    }
                }
                ins = new FullIns(tile);
                if (end)
                    ins.addRoute(SwitchIPort.CSTO, dest);
                else
                    ins.addRoute(SwitchIPort.CSTO, dest2);
                code.appendIns(ins, true);
            }
            // forward values
            final int numForward = pos * numPop;
            for (int i = 0; i < numForward; i++) {
                if (bufferRemaining > 0) {
                    if (!end) {
                        ins = new FullIns(tile);
                        ins.addRoute(SwitchIPort.CSTO, dest);
                        code.appendIns(ins, true);
                    }
                    bufferRemaining--;
                } else {
                    ins = new FullIns(tile);
                    ins.addRoute(src, SwitchOPort.CSTI);
                    if (!end)
                        ins.addRoute(src, dest);
                    code.appendIns(ins, true);
                }
            }
        } else {
            // preloop
            FullIns ins = new FullIns(tile);
            if (end)
                ins.addRoute(SwitchIPort.CSTO, dest);
            else
                ins.addRoute(SwitchIPort.CSTO, dest2);
            code.appendIns(ins, true);
            // steadyloop
            for (int i = 0; i < turns; i++) {
                for (int j = 0; j < pop; j++) {
                    ins = new FullIns(tile);
                    ins.addRoute(src, SwitchOPort.CSTI);
                    if (!end)
                        ins.addRoute(src, dest);
                    code.appendIns(ins, true);
                }
                ins = new FullIns(tile);
                if (end)
                    ins.addRoute(SwitchIPort.CSTO, dest);
                else
                    ins.addRoute(SwitchIPort.CSTO, dest2);
                code.appendIns(ins, true);
            }
            // postloop
            for (int i = 0; i < numPop - 1; i++) {
                for (int j = 0; j < pop; j++) {
                    ins = new FullIns(tile);
                    ins.addRoute(src, SwitchOPort.CSTI);
                    if (!end)
                        ins.addRoute(src, dest);
                    code.appendIns(ins, true);
                }
                ins = new FullIns(tile);
                if (end)
                    ins.addRoute(SwitchIPort.CSTO, dest);
                else
                    ins.addRoute(SwitchIPort.CSTO, dest2);
                code.appendIns(ins, true);
            }
        }
    }

    /**
     * Man fuck this shit!
     * 
     * @param node
     * @param filterInfo
     * @param mult
     * @param tile
     * @param rawChip
     */
    private static void createLinearSwitchCode(FilterSliceNode node,
                                               FilterInfo filterInfo, int mult, RawTile tile, RawProcElements rawChip) {
        System.err.println("Creating switchcode linear: " + node + " " + mult);
        RawComputeNode sourceNode = null;
        // Get sourceNode and input port
        if (node.getPrevious().isFilterSlice())
            sourceNode = layout.getComputeNode(((FilterSliceNode) node.getPrevious()));
                                         
        else {
//            if (KjcOptions.magicdram && node.getPrevious() != null
//                && node.getPrevious().isInputSlice() && tile.hasIODevice())
//                sourceNode = tile.getIODevice();
//            else
                sourceNode = IntraSliceBuffer.getBuffer(
                                                        (InputSliceNode) node.getPrevious(), node)
                    .getNonRedundant().getDRAM();
        }
        SwitchIPort src = rawChip.getIPort(sourceNode, tile);
        SwitchIPort src2 = rawChip.getIPort2(sourceNode, tile);
        sourceNode = null;
        // Get destNode and output port
        RawComputeNode destNode = null;
        if (node.getNext().isFilterSlice())
            destNode = layout.getComputeNode(((FilterSliceNode) node.getNext()));
                                      
        else {
//            if (KjcOptions.magicdram && node.getNext() != null
//                && node.getNext().isOutputSlice() && tile.hasIODevice())
//                destNode = tile.getIODevice();
//            else {
                destNode = IntraSliceBuffer.getBuffer(node,
                                                      (OutputSliceNode) node.getNext()).getNonRedundant()
                    .getDRAM();
//            }
        }
        SwitchOPort dest = rawChip.getOPort(tile, destNode);
        SwitchOPort dest2 = rawChip.getOPort2(tile, destNode);
        // destNode = null;
        // Get filter properties
        FilterContent content = node.getFilter();
        final int numCoeff = content.getArray().length;
        final int peek = content.getPeek();
        final int pop = content.getPopCount();
        final int numPop = numCoeff / pop;
        final boolean begin = content.getBegin();
        final boolean end = content.getEnd();
        final int pos = content.getPos();

        int index = content.getTotal() - pos - 1;

        // int turns=pos*numCoeff; //Default number of turns
        int turns = pos * numPop; // Default number of turns

        final int numTimes = Linear.getMult(numCoeff);
        final int target = filterInfo.steadyMult
            - (int) Math.ceil(((double) peek) / pop);
        final int newSteadyMult = target / numTimes - 1;
        final int remainingExec = target - (newSteadyMult + 1) * numTimes;

        turns += remainingExec; // Remaining executions
        // System.out.println("SRC: "+src);
        // System.out.println("DEST: "+dest);
        // Begin codegen
        SwitchCodeStore code = tile.getSwitchCode();
        // System.err.println("Getting HERE!");
        code.appendIns(new Comment("HERE!"), false);
        // Get loop counter
        FullIns loopCount = new FullIns(tile, new MoveIns(SwitchReg.R3,
                                                          SwitchIPort.CSTO));
        code.appendIns(loopCount, false);
        // Preloop
        if (begin) {
            for (int i = 0; i < numPop; i++)
                for (int j = 0; j < pop; j++) {
                    // Pass first value
                    FullIns ins = new FullIns(tile, new MoveIns(SwitchReg.R1,
                                                                src));
                    ins.addRoute(src, SwitchOPort.CSTI);
                    code.appendIns(ins, false);
                    // Repeat first value
                    for (int k = i - 1; k >= 0; k--) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
                        code.appendIns(newIns, false);
                    }
                }
            if (turns > 0) {
                // Order between values (from peek buffer) and partial sums is
                // reversed
                // So use Reg2 as a buffer to reorder partial sum and values
                // Save partial sum
                FullIns ins = new FullIns(tile, new MoveIns(SwitchReg.R2,
                                                            SwitchIPort.CSTO));
                code.appendIns(ins, false);
                for (int turn = 0; turn < turns; turn++)
                    for (int j = 0; j < pop; j++) {
                        // Pass first value
                        ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
                        ins.addRoute(src, SwitchOPort.CSTI);
                        ins.addRoute(src, dest); // Send to next tile
                        code.appendIns(ins, false);
                        // Repeat first value
                        for (int k = numPop - 2; k >= 0; k--) {
                            FullIns newIns = new FullIns(tile);
                            newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
                            code.appendIns(newIns, false);
                        }
                        if (j == 0) { // Partial sum
                            // Save partial sum
                            FullIns newIns;
                            if (turn < turns - 1)
                                newIns = new FullIns(tile, new MoveIns(
                                                                       SwitchReg.R2, SwitchIPort.CSTO));
                            else
                                newIns = new FullIns(tile); // Don't pull off
                                                            // last partial sum
                            if (end) // Send out partial sum
                                newIns.addRoute(SwitchReg.R2, dest); // Final
                            // output
                            // to
                            // static
                            // net1
                            else
                                newIns.addRoute(SwitchReg.R2, dest2);
                            code.appendIns(newIns, false);
                        }
                    }
            }
        } else {
            for (int i = 0; i < numPop; i++) {
                for (int j = 0; j < pop; j++) {
                    // Pass first value
                    FullIns ins = new FullIns(tile, new MoveIns(SwitchReg.R1,
                                                                src));
                    ins.addRoute(src, SwitchOPort.CSTI);
                    code.appendIns(ins, false);
                    // Repeat first value
                    for (int k = i - 1; k >= 0; k--) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
                        code.appendIns(newIns, false);
                    }
                    // Pass in partial sum
                    if (j == 0) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(src2, SwitchOPort.CSTI2);
                        code.appendIns(newIns, false);
                    }
                }
            }
            for (int turn = 0; turn < turns; turn++)
                for (int j = 0; j < pop; j++) {
                    // Pass first value
                    FullIns ins = new FullIns(tile, new MoveIns(SwitchReg.R1,
                                                                src));
                    ins.addRoute(src, SwitchOPort.CSTI);
                    if (!end)
                        ins.addRoute(src, dest);
                    code.appendIns(ins, false);
                    // Repeat first value
                    for (int k = numPop - 2; k >= 0; k--) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
                        code.appendIns(newIns, false);
                    }
                    // Pass in partial sum
                    if (j == 0) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(src2, SwitchOPort.CSTI2);
                        // Pass out partial sum to next filter
                        if (end)
                            newIns.addRoute(SwitchIPort.CSTO, dest); // Final
                        // sum
                        // goes
                        // to
                        // static1
                        else
                            newIns.addRoute(SwitchIPort.CSTO, dest2);
                        code.appendIns(newIns, false);
                    }
                }
        }
        // Innerloop
        Label label = code.getFreshLabel();
        int pendingSends = 0;
        // int pendingReceives=0;
        int deferredSends = 0; // Add a delay
        FullIns ins = null;
        for (int repeat = 0; repeat < 2; repeat++) {
            int times = 0;
            if (repeat == 1)
                code.appendIns(label, false);
            for (int i = 0; i < numTimes; i++) {
                for (int j = 0; j < pop; j++) {
                    for (int k = 0; k < numPop; k++) {
                        if (j == 0 && k == numPop - 1)
                            // pendingSends++;
                            deferredSends++;
                        times++;
                        if (k == 0) {
                            ins = new FullIns(tile, new MoveIns(SwitchReg.R1,
                                                                src));
                            ins.addRoute(src, SwitchOPort.CSTI);
                            if (!end) {
                                ins.addRoute(src, dest);
                            }
                        } else {
                            ins = new FullIns(tile);
                            ins.addRoute(SwitchReg.R1, SwitchOPort.CSTI); // Temp
                            // reg
                        }
                        // Add Send
                        if (pendingSends > 0) {
                            if (end)
                                ins.addRoute(SwitchIPort.CSTO, dest); // Final
                            // sum
                            // goes
                            // to
                            // static1
                            else
                                ins.addRoute(SwitchIPort.CSTO, dest2);
                            pendingSends--;
                        }
                        code.appendIns(ins, false);
                        if (times == 4) {
                            times = 0;
                            /*
                             * int saveDeferredSends=deferredSends;
                             * while(pendingSends>0||deferredSends>0) { ins=new
                             * FullIns(tile); if(pendingSends>0) { if(end)
                             * ins.addRoute(SwitchIPort.CSTO,dest); //Final sum
                             * goes to static1 else
                             * ins.addRoute(SwitchIPort.CSTO,dest2);
                             * pendingSends--; } if(deferredSends>0) {
                             * if(!begin) {
                             * ins.addRoute(src2,SwitchOPort.CSTI2); }
                             * deferredSends--; } code.appendIns(ins,false); }
                             * pendingSends=saveDeferredSends;
                             */

                            /*
                             * if(pendingSends>0) { for(int l=0;l<pendingSends;l++) {
                             * ins=new FullIns(tile); if(end)
                             * ins.addRoute(SwitchIPort.CSTO,dest); //Final sum
                             * goes to static1 else
                             * ins.addRoute(SwitchIPort.CSTO,dest2);
                             * code.appendIns(ins,false); } pendingSends=0; }
                             */
                            if (deferredSends > 0) {
                                pendingSends = deferredSends;
                                // pendingRecieves=defferredSends;
                                for (int l = 0; l < deferredSends; l++) {
                                    ins = new FullIns(tile); // Put receive
                                    // code here
                                    if (!begin) {
                                        ins.addRoute(src2, SwitchOPort.CSTI2);
                                    }
                                    code.appendIns(ins, false);
                                }
                                deferredSends = 0;
                            }
                        }
                    }
                }
            }
            if (repeat == 1)
                ins.setProcessorIns(new BnezdIns(SwitchReg.R3, SwitchReg.R3,
                                                 label.getLabel()));
        }
        if (pendingSends > 0) {
            for (int l = 0; l < pendingSends; l++) {
                ins = new FullIns(tile);
                if (end)
                    ins.addRoute(SwitchIPort.CSTO, dest); // Final sum goes to
                // static1
                else
                    ins.addRoute(SwitchIPort.CSTO, dest2);
                code.appendIns(ins, false);
            }
            pendingSends = 0;
        }
        // Postloop
        // turns=index*numPop+extra;
        // turns=pos*numPop;
        turns = index * numPop;// +(int)Math.ceil(((double)bufferSize)/pop);
        // //Make sure to fill peekbuffer
        System.out.println("SWITCH TURNS: " + turns);
        if (begin) {
            // int emptySpots=pop*(turns+numPop-1+pos*numPop)-bufferSize;
            if (turns > 0) {
                throw new AssertionError("Shouldn't go in here!");
                // Order between values (from peek buffer) and partial sums is
                // reversed
                // So use Reg2 as a buffer to reorder partial sum and values
                // Save partial sum
                // ins=new FullIns(tile,new
                // MoveIns(SwitchReg.R2,SwitchIPort.CSTO));
                // code.appendIns(ins, false);

                /*
                 * for(int turn=0;turn<turns;turn++) for(int j = 0; j<pop;
                 * j++) { //Pass first value ins=new FullIns(tile, new
                 * MoveIns(SwitchReg.R1, src)); ins.addRoute(src,
                 * SwitchOPort.CSTI); if(!end) ins.addRoute(src,dest); //Send to
                 * next tile code.appendIns(ins, false); //Repeat first value
                 * for(int k=numPop-2;k>=0;k--) { FullIns newIns = new
                 * FullIns(tile); newIns.addRoute(SwitchReg.R1,
                 * SwitchOPort.CSTI); code.appendIns(newIns, false); } if(j==0) {
                 * //Partial sum //Save partial sum FullIns newIns=new
                 * FullIns(tile); if(end) //Send out partial sum
                 * newIns.addRoute(SwitchReg.R2,dest); //Final output to static
                 * net1 else newIns.addRoute(SwitchReg.R2,dest2);
                 * code.appendIns(newIns, false); } }
                 */
            }
            for (int i = 0; i < numPop - 1; i++)
                for (int j = 0; j < pop; j++) {
                    /*
                     * if(emptySpots>0) emptySpots--; else {
                     */
                    // Pass first value
                    ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
                    ins.addRoute(src, SwitchOPort.CSTI);
                    if (!end)
                        ins.addRoute(src, dest);
                    code.appendIns(ins, false);
                    // Don't Repeat first value
                    // Buffered in tile
                    /*
                     * for(int k=numPop-2;k>i;k--) { FullIns newIns = new
                     * FullIns(tile); newIns.addRoute(SwitchReg.R1,
                     * SwitchOPort.CSTI); code.appendIns(newIns, false); }
                     */
                    if (j == 0) {
                        FullIns newIns = new FullIns(tile);
                        if (end)
                            newIns.addRoute(SwitchIPort.CSTO, dest); // Final
                        // sum
                        // goes
                        // to
                        // static1
                        else
                            newIns.addRoute(SwitchIPort.CSTO, dest2);
                        code.appendIns(newIns, false);
                    }
                    // }
                }
            // Pass last partial sum
            ins = new FullIns(tile);
            if (end)
                ins.addRoute(SwitchIPort.CSTO, dest);
            else
                ins.addRoute(SwitchIPort.CSTO, dest2);
            code.appendIns(ins, false);
            // Pass remaining values to filters downstream
            for (int i = 0; i < pos * numPop; i++) {
                ins = new FullIns(tile);
                ins.addRoute(SwitchReg.R1, dest);
                code.appendIns(ins, false);
            }
        } else {
            for (int turn = 0; turn < turns; turn++)
                for (int j = 0; j < pop; j++) {
                    // Pass first value
                    ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
                    ins.addRoute(src, SwitchOPort.CSTI);
                    if (!end)
                        ins.addRoute(src, dest);
                    code.appendIns(ins, false);
                    // Repeat first value
                    for (int k = numPop - 2; k >= 0; k--) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
                        code.appendIns(newIns, false);
                    }
                    // Pass in partial sum
                    if (j == 0) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(src2, SwitchOPort.CSTI2);
                        // Pass out partial sum to next filter
                        if (end)
                            newIns.addRoute(SwitchIPort.CSTO, dest); // Final
                        // sum
                        // goes
                        // to
                        // static1
                        else
                            newIns.addRoute(SwitchIPort.CSTO, dest2);
                        code.appendIns(newIns, false);
                    }
                }
            for (int i = 0; i < numPop - 1; i++) {
                for (int j = 0; j < pop; j++) {
                    // Pass first value
                    ins = new FullIns(tile, new MoveIns(SwitchReg.R1, src));
                    ins.addRoute(src, SwitchOPort.CSTI);
                    if (!end)
                        ins.addRoute(src, dest);
                    code.appendIns(ins, false);
                    // Repeat first value
                    for (int k = numPop - 2; k > i; k--) {
                        FullIns newIns = new FullIns(tile);
                        newIns.addRoute(SwitchReg.R1, SwitchOPort.CSTI);
                        code.appendIns(newIns, false);
                    }
                    // Pass in partial sum
                    if (j == 0) {
                        FullIns newIns = new FullIns(tile);
                        // newIns.addRoute(src2, SwitchOPort.CSTI2);
                        // Pass out partial sum to next filter
                        if (end)
                            newIns.addRoute(SwitchIPort.CSTO, dest); // Final
                        // sum
                        // goes
                        // to
                        // static1
                        else
                            newIns.addRoute(SwitchIPort.CSTO, dest2);
                        code.appendIns(newIns, false);
                    }
                }
            }
            // Pass last partial sum
            ins = new FullIns(tile);
            if (end)
                ins.addRoute(SwitchIPort.CSTO, dest);
            else
                ins.addRoute(SwitchIPort.CSTO, dest2);
            code.appendIns(ins, false);
            // Pass remaining values to filters downstream
            for (int i = 0; i < pos * numPop; i++) {
                ins = new FullIns(tile);
                ins.addRoute(src, dest);
                code.appendIns(ins, false);
            }
        }
    }

    
    /**
     * Create the intra-trace communication code for the filter trace node.  
     * This function will create code for either the gdn or the static network
     * depending on the network desired.  It will also keep transfers cache-line
     * sized when necessary.
     *  
     * @param node
     * @param parent
     * @param filterInfo
     * @param init
     * @param primePump
     * @param linear
     * @param tile
     * @param rawChip
     */
    private static void createCommunicationCode(FilterSliceNode node, Slice parent,
                                             FilterInfo filterInfo, SchedulingPhase whichPhase,
                                             boolean linear, RawTile tile, RawProcElements rawChip) {

        // give up and go back to 2-boolean system of showing scheduling stages.
        boolean init = false;
        boolean primePump = false;
        switch (whichPhase) {
        case INIT:
            init = true;
            break;
        case PRIMEPUMP:
            primePump = true;
            break;
        default: 
        }
        int mult, sentItems = 0;

        // don't cache align if the only source is a file reader
        boolean cacheAlignSource = true;
        // should we generate switch receive code
        boolean switchReceiveCode = true;
        if (node.getPrevious() instanceof InputSliceNode) {
            IntraSliceBuffer buf = 
                IntraSliceBuffer.getBuffer((InputSliceNode) node.getPrevious(), node);

            //don't worry about handling non-cache-sized transfers if the
            //source is a file reader, use the upstream non-redundant buffer
            //for this because it is the one that is connect to the file reader
            if (Util.onlyFileInput((InputSliceNode)node.getPrevious()))
                cacheAlignSource = false;
            
            //don't generate switch code for receiving if this filter is the
            //first in a trace and it gets its input from the dynamic network
            if (buf != null && !buf.isStaticNet())
                switchReceiveCode = false;
        }

        //should we generate switch send code??
        boolean switchSendCode = true;
        //shoud we cache align the sending code       
        boolean cacheAlignDest = true;
        if (node.getNext() instanceof OutputSliceNode) {
            OutputSliceNode output = (OutputSliceNode) node.getNext();
            //don't cache align the dest if the true dest is a file writer
            //that is not split
            if (Util.onlyWritingToAFile(output))
                cacheAlignDest = false;
            
            //don't generate switch code for sending if the buffer is 
            //allocated to use the gdn
            if (!IntraSliceBuffer.getBuffer(node, output).isStaticNet())
                switchSendCode = false;
        }

        
        mult = filterInfo.getMult(whichPhase);


        // should we compress the switch code??
        boolean switchCompression = 
            compressFilterSwitchIns(mult, filterInfo.pop, filterInfo.push, init); 
            //SWITCH_COMP && mult > SC_THRESHOLD && !init;

        if (!(init || primePump || !linear)) { // Linear switch code in
            // steadystate
            /*
             * if(primePump) { int bufferSize; FilterContent
             * content=filterInfo.traceNode.getFilter(); final int
             * pos=content.getPos(); final int peek=content.getPeek(); final int
             * pop = content.getPopCount(); int index=content.getTotal()-pos-1;
             * if(index==0) //If first tile bufferSize=filterInfo.remaining;
             * else { //Find first tile SliceNode curNode=node; for(int
             * i=index;i>0;i--) curNode=curNode.getPrevious(); FilterInfo
             * parentInfo=FilterInfo.getFilterInfo((FilterSliceNode)curNode);
             * bufferSize=parentInfo.remaining; } if(filterInfo.initMult>0)
             * bufferSize+=peek-pop;
             * createInitLinearSwitchCode(node,filterInfo,mult,bufferSize,tile,rawChip); }
             * else
             */
            createLinearSwitchCode(node, filterInfo, mult, tile, rawChip);
            sentItems += mult;
        } else if (switchCompression) {
            sentItems = filterInfo.push * mult;

            filterSwitchCodeCompressed(mult, node, filterInfo, init, primePump,
                                       tile, rawChip, switchSendCode, switchReceiveCode);
        } else {
            for (int i = 0; i < mult; i++) {
                // append the receive code but only if we are using the static net                
                if (switchReceiveCode)
                    createReceiveCode(i, node, filterInfo, init, primePump, tile,
                            rawChip, false);
                // append the send code, but only if we are using the static net
                if (switchSendCode)
                    sentItems += createSendCode(i, node, filterInfo, init,
                                            primePump, tile, rawChip, false);
            }
        }
        
        if (init)
            CommonUtils.println_debugging("REMAINING ITEMS: " + filterInfo.remaining);
        
        // now we must take care of the generating switch code for the remaining items 
        // on the input tape after the initialization phase if the upstream filter 
        // produces more than we consume in init 
        if (init && filterInfo.remaining > 0 && switchReceiveCode) {
            appendReceiveInstructions(node, filterInfo.remaining
                                      * Util.getTypeSize(node.getFilter().getInputType()),
                                      filterInfo, init, false, tile, rawChip);
        }

        // we must add some switch instructions to account for the fact
        // that we must transfer cacheline sized chunks in the streaming dram
        // do it for the init and the steady state, primepump
        
        // generate code to fill the remainder of the cache line
        if (/*!KjcOptions.magicdram && */ node.getNext().isOutputSlice()
            && cacheAlignDest && switchSendCode) {
            //perform the filling in the switch if we are using the static net
            fillCacheLineStatic(rawChip, node, init, primePump, sentItems);
        }
        
        // because all dram transfers must be multiples of cacheline
        // generate code to disregard the remainder of the transfer
        if (/*!KjcOptions.magicdram && */ node.getPrevious().isInputSlice()
            && cacheAlignSource && switchReceiveCode) {
            //perform the disregarding in the switch if we are static
            handleUnneededInputStatic(rawChip, node, whichPhase, 
                    filterInfo.totalItemsReceived(whichPhase)); 
                    
          
        }
    }

    /**
     * Return true if we should compress the switch instructions of the filter
     * with mult, pop, and push.
     * @param mult The multiplicity.
     * @param pop The pop rate.
     * @param push The push rate.
     * @param init Are we in the init stage?
     * @return True if we should compress the switch instructions. 
     */
    public static boolean compressFilterSwitchIns(int mult, int pop, int push, boolean init) {
        if (init || !SWITCH_COMP)
            return false;
        
        int totalItems = mult * (push + pop);
        //return true if the filter has a comm rate > SC_FILTER_THRESH
        return totalItems > SC_FILTER_THRESH;
    }
    
    /**
     * Create compressed switch code for filter execution (intra-trace).
     * 
     * @param mult The multiplicity of whatever stage we are in.
     * @param node
     * @param filterInfo
     * @param init
     * @param primePump
     * @param tile
     * @param rawChip
     * @param switchSendCode true if we are using the static net to for sending
     * @param switchReceiveCode true if we are using the static net to for receiving
     */
    private static void filterSwitchCodeCompressed(int mult,
            FilterSliceNode node, FilterInfo filterInfo, boolean init,
            boolean primePump, RawTile tile, RawProcElements rawChip, 
            boolean switchSendCode, boolean switchReceiveCode) {
        
        assert mult < 65535;
        assert !init;

        //if there is nothing to do, just leave, this filter uses the gdn for both i/o
        if (!switchSendCode && !switchReceiveCode)
            return;
        
        // the items this filter is receiving for this iteration, if we are using the gdn
        // then set to 0
        int itemsReceiving = switchReceiveCode ? filterInfo.itemsNeededToFire(0, init) : 0; 
        // get the number of items sending on this iteration, only matters
        // if init and if twostage
        int itemsSending = switchSendCode ? filterInfo.itemsFiring(0, init) : 0;

        // are we going to compress the individual send and receive
        // instructions?
        boolean sendCompression = (itemsSending > SC_INS_THRESH), 
            receiveCompression = (itemsReceiving > SC_INS_THRESH);

        Label receiveLabel = new Label(), sendLabel = new Label(), multLabel = new Label();

        // the multiplicity of the filter
        sendBoundProcToSwitch(mult, tile, init, primePump, FILTER_FIRE_LOOP_REG);

        // add the label around the entire firing of the node
        tile.getSwitchCode().appendIns(multLabel, (init || primePump));

        // receive on the switch the number of items we are receiving, the proc
        // sends this for each firing of the filter (see bufferedcommunication,
        // directcommunicatio)
        if (receiveCompression)
            recConstOnSwitch(tile, init, primePump, POP_LOOP_REG);
        // receive on the switch the number of items we are sending, the proc
        // sends this for each firing of the filter (see bufferedcommunication,
        // directcommunicatio)
        if (sendCompression)
            recConstOnSwitch(tile, init, primePump, PUSH_LOOP_REG);

        // generate the label for the receive
        if (receiveCompression)
            tile.getSwitchCode().appendIns(receiveLabel, (init || primePump));

        // append the receive code for 1 item if receive compression or pop
        // items if not, don't generate any code if there are no items to receive
        // over static net
        if (itemsReceiving > 0)
            createReceiveCode(0, node, filterInfo, init, primePump, tile, rawChip,
                    receiveCompression);

        // generate the loop back for the receive
        if (receiveCompression)
            generateSwitchLoopTrailer(receiveLabel, tile, init, primePump,
                                      POP_LOOP_REG);

        // generate the label for the send
        if (sendCompression)
            tile.getSwitchCode().appendIns(sendLabel, (init || primePump));

        // append the send ins for 1 item if send compression or push items if
        // not, don't generate any items if there are no items to send over static
        if (itemsSending > 0)
            createSendCode(0, node, filterInfo, init, primePump, tile, rawChip,
                    sendCompression);

        // generate the loop back for the send
        if (sendCompression)
            generateSwitchLoopTrailer(sendLabel, tile, init, primePump,
                                      PUSH_LOOP_REG);
        // generate the loop back for a complete firing of the filter...
        generateSwitchLoopTrailer(multLabel, tile, false, primePump,
                                  FILTER_FIRE_LOOP_REG);
    }

    /**
     * Create the switch instructions necessary to receive the items that are needed
     * for firing.  So call the necessary creation routine with the correct number of 
     * iterations and the correct loop body.
     * 
     * @param iteration
     * @param node
     * @param filterInfo
     * @param init
     * @param primePump
     * @param tile
     * @param rawChip
     * @param compression Are we compressing the receive instructions?
     */
    private static void createReceiveCode(int iteration, FilterSliceNode node,
                                          FilterInfo filterInfo, boolean init, boolean primePump,
                                          RawTile tile, RawProcElements rawChip, boolean compression) {
        // the label used if switch instruction compression is used...
        Label label = null;

        // if this is the init and it is the first time executing
        // and a twostage filter, use initpop and multiply this
        // by the size of the type it is receiving
        int itemsReceiving = filterInfo.itemsNeededToFire(iteration, init);

        // do nothing if there is nothing to do
        if (itemsReceiving == 0)
            return;

        // if we are placing in a loop, only generate 1 item
        if (compression)
            itemsReceiving = 1;

        // account for the size of the type
        itemsReceiving *= Util.getTypeSize(node.getFilter().getInputType());

        appendReceiveInstructions(node, itemsReceiving, filterInfo, init,
                                  primePump, tile, rawChip);
    }

    /**
     * This function creates the actual switch instructions for receiving 
     * items on the switch asssigned to this filter.  Remember that we only support
     * neighbor communication within a trace currently. 
     * 
     * @param node
     * @param itemsReceiving
     * @param filterInfo
     * @param init
     * @param primePump
     * @param tile
     * @param rawChip
     */
    private static void appendReceiveInstructions(FilterSliceNode node,
                                                  int itemsReceiving, FilterInfo filterInfo, boolean init,
                                                  boolean primePump, RawTile tile, RawProcElements rawChip) {
        // the source of the data, either a device or another raw tile
        RawComputeNode sourceNode = null;

        assert itemsReceiving > 0;
        
        if (node.getPrevious().isFilterSlice())
            sourceNode = layout.getComputeNode(((FilterSliceNode) node.getPrevious()));
                                         
        else {
//            if (KjcOptions.magicdram && node.getPrevious() != null
//                && node.getPrevious().isInputSlice() && tile.hasIODevice())
//                sourceNode = tile.getIODevice();
//            else
                sourceNode = IntraSliceBuffer.getBuffer(
                                                        (InputSliceNode) node.getPrevious(), node)
                    .getNonRedundant().getDRAM();
        }

        for (int j = 0; j < itemsReceiving; j++) {
            RouteIns ins = new RouteIns(tile);
            // add the route from the source tile to this
            // tile's compute processor
            ins.addRoute(sourceNode, tile);
            // append the instruction to the appropriate schedule
            // for the primepump append to the end of the init stage
            // so set final arg to true if init or primepump
            tile.getSwitchCode().appendIns(ins, (init || primePump));
            // if we are receiving from an inputtracenode and
            // magic dram is enabled, generate the magic dram load ins
//            if (KjcOptions.magicdram && node.getPrevious() != null
//                && node.getPrevious().isInputSlice())
//                createMagicDramLoad((InputSliceNode) node.getPrevious(), node,
//                                    (init || primePump), rawChip);
        }
    }

    /**
     * Create the switch code necessary to "push" items after a filter fires.
     * So send the items from the compute processor to the neighboring iodevice.
     *  
     * 
     * @param iteration
     * @param node
     * @param filterInfo
     * @param init
     * @param primePump
     * @param tile
     * @param rawChip
     * @param compression
     * @return
     */
    private static int createSendCode(int iteration, FilterSliceNode node,
                                      FilterInfo filterInfo, boolean init, boolean primePump,
                                      RawTile tile, RawProcElements rawChip, boolean compression) {
        // get the number of items sending on this iteration, only matters
        // if init and if twostage
        int items = filterInfo.itemsFiring(iteration, init);

        if (items == 0)
            return 0;

        RawComputeNode destNode = null;

        if (node.getNext().isFilterSlice())
            destNode = layout.getComputeNode(((FilterSliceNode) node.getNext()));
                                       
        else {
//            if (KjcOptions.magicdram && node.getNext() != null
//                && node.getNext().isOutputSlice() && tile.hasIODevice())
//                destNode = tile.getIODevice();
//            else {
                destNode = IntraSliceBuffer.getBuffer(node,
                        (OutputSliceNode) node.getNext()).getNonRedundant()
                    .getDRAM();
//            }

        }

        // the label for the loop if we are compressing
        Label label = null;

        // only send over 1 item, so set words to 1 instead of items
        if (compression)
            items = 1;

        int words = items * Util.getTypeSize(node.getFilter().getOutputType());

        for (int j = 0; j < words; j++) {
            RouteIns ins = new RouteIns(tile);
            // add the route from this tile to the next trace node
            ins.addRoute(tile, destNode);
            // append the instruction
            // for the primepump append to the end of the init stage
            // so set final arg to true if init or primepump
            tile.getSwitchCode().appendIns(ins, (init || primePump));
            // if we are connected to an output trace node and
            // magicdram is enabled, create the magic dram store instuction
//            if (KjcOptions.magicdram && node.getNext() != null
//                && node.getNext().isOutputSlice())
//                createMagicDramStore((OutputSliceNode) node.getNext(), node,
//                                     (init || primePump), rawChip);
        }

        return items;
    }

    
    /**
     * Receive a constant on the switch sent from the proc.
     * 
     * @param tile
     * @param init
     * @param primePump
     * @param reg
     */
    private static void recConstOnSwitch(RawTile tile, boolean init,
                                         boolean primePump, SwitchReg reg) {
        // add the code on the switch to receive the constant
        MoveIns moveIns = new MoveIns(reg, SwitchIPort.CSTO);
        tile.getSwitchCode().appendIns(moveIns, (init || primePump));
    }

    /**
     * Send a const -1 from the proc to switch, generating instructions on both the
     * switch and the compute processors.
     *  
     * @param mult What we want to send.
     * @param tile
     * @param init
     * @param primePump
     * @param reg
     */
    private static void sendBoundProcToSwitch(int mult, RawTile tile,
                                              boolean init, boolean primePump, SwitchReg reg) {
        assert mult > 0;
        // don't have a condition at the header of the loop
        tile.getComputeCode().sendConstToSwitch(mult - 1, (init || primePump));
        recConstOnSwitch(tile, init, primePump, reg);
    }

    /**
     * Generate a header for a loop: generate the instructions to communicate the 
     * bound and generate the loop label on the switch.
     * 
     * @param mult
     * @param tile
     * @param init
     * @param primePump
     * @param reg
     * @return
     */
    private static Label generateSwitchLoopHeader(int mult, RawTile tile,
                                                  boolean init, boolean primePump, SwitchReg reg) {
        sendBoundProcToSwitch(mult, tile, init, primePump, reg);

        Label label = new Label();
        tile.getSwitchCode().appendIns(label, (init || primePump));
        return label;
    }

    /**
     * Generate the conditional branch instruction of a loop on the switch that is using
     * switch reg <pre>reg</pre> as its working register.
     * @param label
     * @param tile
     * @param init
     * @param primePump
     * @param reg The switch register with the loop index.
     */
    private static void generateSwitchLoopTrailer(Label label, RawTile tile,
                                                  boolean init, boolean primePump, SwitchReg reg) {
        // add the branch back
        BnezdIns branch = new BnezdIns(reg, reg, label.getLabel());
        tile.getSwitchCode().appendIns(branch, (init || primePump));
    }

    /*
     * worry about magic stuff later private static void
     * magicHandlePredefined(FilterSliceNode predefined, RawChip rawChip,
     * boolean init) { if (init) { //tell the magic dram that it should open the
     * file and create vars for this file if (predefined.isFileInput()) { //get
     * the filter connected to this file output, just take the first one
     * //because they all should be mapped to the same tile FilterSliceNode next =
     * FilterInfo.getFilterInfo(predefined).getNextFilters()[0]; if
     * (!rawChip.getTile(next.getX(), next.getY()).hasIODevice())
     * Utils.fail("Tile not connected to io device"); MagicDram dram =
     * (MagicDram)rawChip.getTile(next.getX(), next.getY()).getIODevice();
     * dram.inputFiles.add((FileInputContent)predefined.getFilter()); } else if
     * (predefined.isFileOutput()) { //tell the magic dram that it should open
     * the file and create vars for this file
     * 
     * //get the filter connected to this file output, just take the first one
     * //because they all should be mapped to the same tile FilterSliceNode prev =
     * FilterInfo.getFilterInfo(predefined).getPreviousFilters()[0]; //find the
     * iodevice if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice())
     * Utils.fail("Tile not connected to io device"); //get the dram MagicDram
     * dram = (MagicDram)rawChip.getTile(prev.getX(),
     * prev.getY()).getIODevice();
     * dram.outputFiles.add((FileOutputContent)predefined.getFilter()); } } }
     */

    private static void createMagicDramLoad(InputSliceNode node,
                                            FilterSliceNode next, boolean init, RawProcElements rawChip) {
        /*
         * if (!rawChip.getTile(next.getX(), next.getY()).hasIODevice())
         * Utils.fail("Tile not connected to io device");
         * 
         * MagicDram dram = (MagicDram)rawChip.getTile(next.getX(),
         * next.getY()).getIODevice();
         * 
         * LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
         * OutputSliceNode output = SliceBufferSchedule.getOutputBuffer(node);
         * insList.add(new MagicDramLoad(node, output)); dram.addBuffer(output,
         * node);
         */
    }

    /**
     * Generate a single magic dram store instruction for this output trace node
     */
    private static void createMagicDramStore(OutputSliceNode node,
                                             FilterSliceNode prev, boolean init, RawProcElements rawChip)

    {
        /*
         * if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice())
         * Utils.fail("Tile not connected to io device"); //get the dram
         * MagicDram dram = (MagicDram)rawChip.getTile(prev.getX(),
         * prev.getY()).getIODevice(); //get the list we should add to
         * LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
         * //add the instruction insList.add(new MagicDramStore(node,
         * SliceBufferSchedule.getInputBuffers(node)));
         */
    }

    /*
     * private static void generateOutputDRAMCommands(OutputSliceNode output,
     * boolean init, boolean primepump, FilterSliceNode filter, int items, int
     * stage) { if (items == 0) return; int iterations, typeSize;
     * 
     * typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
     * 
     * //the numbers of times we should cycle thru this "splitter" assert items %
     * output.totalWeights() == 0: "weights on output trace node does not divide
     * evenly with items sent"; iterations = items / output.totalWeights();
     * 
     * //generate the command to read from the src of the output trace node
     * OffChipBuffer srcBuffer = IntraSliceBuffer.getBuffer(filter, output); int
     * readBytes = FilterInfo.getFilterInfo(filter).totalItemsSent(init,
     * primepump) * Util.getTypeSize(filter.getFilter().getOutputType()) * 4;
     * readBytes = Util.cacheLineDiv(readBytes);
     * SpaceTimeBackend.println("Generating the read command for " + output + "
     * on " + srcBuffer.getOwner() + (primepump ? "(primepump)" : "")); //in the
     * primepump stage a real output trace always reads from the init buffers
     * //never use stage 2 for reads
     * srcBuffer.getOwner().getComputeCode().addDRAMCommand(true, (stage < 3 ? 1 :
     * 3), readBytes, srcBuffer, true);
     * 
     * //generate the commands to write the o/i temp buffer dest Iterator dests =
     * output.getDestSet().iterator(); while (dests.hasNext()){ InterSliceEdge edge =
     * (InterSliceEdge)dests.next(); OffChipBuffer destBuffer =
     * InterSliceBuffer.getBuffer(edge); int writeBytes = iterations * typeSize *
     * output.getWeight(edge) * 4; writeBytes = Util.cacheLineDiv(writeBytes);
     * destBuffer.getOwner().getComputeCode().addDRAMCommand(false, stage,
     * writeBytes, destBuffer, false); } }
     */
}

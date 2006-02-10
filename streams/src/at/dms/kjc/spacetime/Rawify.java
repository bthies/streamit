package at.dms.kjc.spacetime;

import java.util.ListIterator;
import java.util.Iterator;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.spacetime.switchIR.*;
import at.dms.util.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Vector;
import at.dms.kjc.flatgraph2.*;

/**
 * This class will rawify the SIR code and it creates the switch code. It does
 * not rawify the compute code in place.
 */
public class Rawify {
    // if true try to compress the switch code by creating loops
    public static boolean SWITCH_COMP = true;

    // any filter that executes more than SC_THRESHOLD times in the primepump
    // or steady will have its switch instructions placed in a loop
    public static int SC_THRESHOLD = 5;

    // a filter that pushes or pops more than SC_INS_THRESH will have these
    // instruction placed in a loop on the switch
    public static int SC_INS_THRESH = 5;

    // regs on the switch that are used for various loops
    private static SwitchReg POP_LOOP_REG = SwitchReg.R0;

    private static SwitchReg PUSH_LOOP_REG = SwitchReg.R1;

    private static SwitchReg FILTER_FIRE_LOOP_REG = SwitchReg.R2;

    private static SwitchReg PP_STEADY_LOOP_REG = SwitchReg.R3;

    /**
     *  The entry of the rawify pass.  This function iterates over the 
     *  schedules for the 3 phases (init, priming, steady) and generates the
     *  compute code and communicate code necessary to execute the schedule!
     *  
     * @param schedule
     * @param rawChip
     */
    public static void run(SpaceTimeSchedule schedule, RawChip rawChip) {
        Trace traces[];

        //the initialization stage!!
        traces = schedule.getInitSchedule();
        iterate(traces, true, false, rawChip);
        //the prime pump stage!!
        traces = schedule.getPrimePumpScheduleFlat();
        iterate(traces, false, true, rawChip);
        //the steady-state!!
        traces = schedule.getSchedule();
        iterate(traces, false, false, rawChip);
    }

    /**
     * Iterate over the schedule of traces and over each node of each trace and 
     * generate the code necessary to fire the schedule.
     * 
     * @param traces The schedule to execute.
     * @param init True if the init stage.
     * @param primepump True if the primepump stage
     * @param rawChip The raw chip
     */
    private static void iterate(Trace traces[], boolean init,
                                boolean primepump, RawChip rawChip) {
        Trace trace;

        for (int i = 0; i < traces.length; i++) {
            trace = (Trace) traces[i];
            // iterate over the TraceNodes
            TraceNode traceNode = trace.getHead();
            while (traceNode != null) {
                SpaceTimeBackend.println("Rawify: " + traceNode);
                // do the appropiate code generation
                if (traceNode.isFilterTrace()) {
                    FilterTraceNode filterNode = (FilterTraceNode) traceNode;
                    assert !filterNode.isPredefined() : "Predefined filters should not appear in the trace traversal: "
                        + trace.toString();
                    RawTile tile = rawChip.getTile((filterNode).getX(),
                                                   (filterNode).getY());
                    // create the filter info class
                    FilterInfo filterInfo = FilterInfo
                        .getFilterInfo(filterNode);
                    // add the dram command if this filter trace is an
                    // endpoint...
                    generateFilterDRAMCommand(filterNode, filterInfo, tile,
                                              init, primepump);

                    /*
                     * if (filterInfo.isLinear()) { //assert
                     * FilterInfo.getFilterInfo(filterNode).remaining == 0 :
                     * //"Items remaining on buffer for init for linear filter";
                     * createSwitchCodeLinear(filterNode,
                     * trace,filterInfo,init,primepump,tile,rawChip); } else {
                     */

                    createSwitchCode(filterNode, trace, filterInfo, init,
                                     primepump, filterInfo.isLinear(), tile, rawChip);
                    // }

                    // used for debugging, nothing more
                    tile.addFilterTrace(init, primepump, filterNode);
                    // this must come after createswitch code because of
                    // compression
                    addComputeCode(init, primepump, tile, filterInfo);
                } else if (traceNode.isInputTrace() && !KjcOptions.magicdram) {
                    assert StreamingDram
                        .differentDRAMs((InputTraceNode) traceNode) : "inputs for a single InputTraceNode coming from same DRAM";
                    handleFileInput((InputTraceNode) traceNode, init,
                                    primepump, rawChip);
                    // create the switch code to perform the joining
                    joinInputTrace((InputTraceNode) traceNode, init, primepump);
                    // generate the dram command to execute the joining
                    // this must come after joinInputTrace because of switch
                    // compression
                    generateInputDRAMCommands((InputTraceNode) traceNode, init,
                                              primepump);
                } else if (traceNode.isOutputTrace() && !KjcOptions.magicdram) {
                    assert StreamingDram
                        .differentDRAMs((OutputTraceNode) traceNode) : "outputs for a single OutputTraceNode going to same DRAM";
                    handleFileOutput((OutputTraceNode) traceNode, init,
                                     primepump, rawChip);
                    // create the switch code to perform the splitting
                    splitOutputTrace((OutputTraceNode) traceNode, init,
                                     primepump);
                    // generate the DRAM command
                    // this must come after joinInputTrace because of switch
                    // compression
                    outputDRAMCommands((OutputTraceNode) traceNode, init,
                                       primepump);
                }
                // get the next tracenode
                traceNode = traceNode.getNext();
            }

        }

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
                                       RawTile tile, FilterInfo filterInfo) {
        if (init)
            tile.getComputeCode().addTraceInit(filterInfo);
        else if (primepump)
            tile.getComputeCode().addTracePrimePump(filterInfo);
        else  //steady
            tile.getComputeCode().addTraceSteady(filterInfo);
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
    private static void handleFileInput(InputTraceNode input, boolean init,
                                        boolean primepump, RawChip chip) {
        // if there are no files, do nothing
        if (!input.hasFileInput())
            return;
        for (int i = 0; i < input.getSources().length; i++) {
            // do nothing for non-file readers
            if (!input.getSources()[i].getSrc().isFileReader())
                continue;

            OutputTraceNode fileO = input.getSources()[i].getSrc();

            assert fileO.getPrevFilter().getFilter() instanceof FileInputContent : "FileReader should be a FileInputContent";

            // now generate the code, both the dram commands and the switch code
            // to perform the splitting, if there is only one output, do nothing
            if (!OffChipBuffer.unnecessary(fileO)) {
                // generate dram command
                outputDRAMCommands(fileO, init, primepump);
                // perform the splitting
                splitOutputTrace(fileO, init, primepump);
            }
        }
    }

    /**
     * When we visit an output trace node and it is connected to a file writer (downstream)
     * generate the dram commands necessary to send the output to the port's file.  This is 
     * necessary because the file writers do not appear in the schedules. 
     * 
     * @param output
     * @param init
     * @param primepump
     * @param chip
     */
    private static void handleFileOutput(OutputTraceNode output, boolean init,
                                         boolean primepump, RawChip chip) {
        // if there are no files, do nothing
        if (!output.hasFileOutput())
            return;

      
        Iterator dests = output.getDestSet().iterator();
        while (dests.hasNext()) {
            Edge edge = (Edge) dests.next();
            if (!edge.getDest().isFileWriter())
                continue;
            InputTraceNode fileI = edge.getDest();

            assert fileI.getNextFilter().getFilter() instanceof FileOutputContent : "File Writer shoudlbe a FileOutputContent";

            if (!OffChipBuffer.unnecessary(fileI)) {
                // generate the dram commands
                generateInputDRAMCommands(fileI, init, primepump);
                // generate the switch code
                joinInputTrace(fileI, init, primepump);
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
    private static void generateInputDRAMCommands(InputTraceNode input,
                                                  boolean init, boolean primepump) {
        FilterTraceNode filter = (FilterTraceNode) input.getNext();

        // don't do anything for redundant buffers
        if (IntraTraceBuffer.getBuffer(input, filter).redundant())
            return;

        // number of total items that are being joined
        int items = FilterInfo.getFilterInfo(filter).totalItemsReceived(init,
                                                                        primepump);
        // do nothing if there is nothing to do
        if (items == 0)
            return;

        // add to the init code with the init buffers except in steady
        int stage = 0;
        if (!init && !primepump)
            stage = 3;

        assert items % input.totalWeights() == 0 : "weights on input trace node does not divide evenly with items received";
        // iterations of "joiner"
        int iterations = items / input.totalWeights();
        int typeSize = Util.getTypeSize(filter.getFilter().getInputType());

        // generate the commands to read from the o/i temp buffer
        // for each input to the input trace node
        for (int i = 0; i < input.getSources().length; i++) {
            // get the first non-redundant buffer
            OffChipBuffer srcBuffer = InterTraceBuffer.getBuffer(
                                                                 input.getSources()[i]).getNonRedundant();
            SpaceTimeBackend.println("Generate the DRAM read command for "
                                     + srcBuffer);
            int readWords = iterations * typeSize
                * input.getWeight(input.getSources()[i]);
            if (srcBuffer.getDest() instanceof OutputTraceNode
                && ((OutputTraceNode) srcBuffer.getDest()).isFileReader())
                srcBuffer.getOwner().getComputeCode().addFileCommand(true,
                                                                     init || primepump, readWords, srcBuffer);
            else
                srcBuffer.getOwner().getComputeCode().addDRAMCommand(true,
                                                                     stage, Util.cacheLineDiv(readWords * 4), srcBuffer,
                                                                     true);
        }

        // generate the command to write to the dest of the input trace node
        OffChipBuffer destBuffer = IntraTraceBuffer.getBuffer(input, filter);
        int writeWords = items * typeSize;
        if (input.isFileWriter() && OffChipBuffer.unnecessary(input))
            destBuffer.getOwner().getComputeCode().addFileCommand(false,
                                                                  init || primepump, writeWords, destBuffer);
        else
            destBuffer.getOwner().getComputeCode().addDRAMCommand(false, stage,
                                                                  Util.cacheLineDiv(writeWords * 4), destBuffer, false);
    }

    /**
     * For an output trace node, generate the dram commands to write the data
     * to the temp buffers that are between it and its dest.
     * 
     * @param output
     * @param init
     * @param primepump
     */
    private static void outputDRAMCommands(OutputTraceNode output,
                                           boolean init, boolean primepump) {
        FilterTraceNode filter = (FilterTraceNode) output.getPrevious();
 
        // don't do anything for a redundant buffer
        if (OffChipBuffer.unnecessary(output))
            return;

        // if we are in the init set to zero, 1 to primepump
        // if steady set to 3, used for addDRAMCommand(...)
        int stage;
        if (init)
            stage = 0;
        else if (primepump)
            stage = 1;
        else //steady
            stage = 3;

        OffChipBuffer srcBuffer = IntraTraceBuffer.getBuffer(filter, output);
        int readWords = FilterInfo.getFilterInfo(filter).totalItemsSent(init,
                                                                        primepump)
            * Util.getTypeSize(filter.getFilter().getOutputType());
        if (readWords > 0) {
            SpaceTimeBackend.println("Generating the read command for "
                                     + output + " on " + srcBuffer.getOwner()
                                     + (primepump ? "(primepump)" : ""));
            // in the primepump stage a real output trace always reads from the
            // init buffers
            // never use stage 2 for reads
            if (output.isFileReader() && OffChipBuffer.unnecessary(output))
                srcBuffer.getOwner().getComputeCode().addFileCommand(true,
                                                                     init || primepump, readWords, srcBuffer);
            else
                srcBuffer.getOwner().getComputeCode().addDRAMCommand(true,
                                                                     (stage < 3 ? 0 : 3), Util.cacheLineDiv(readWords * 4),
                                                                     srcBuffer, true);
        }

        // now generate the store drm command
        Iterator dests = output.getDestSet().iterator();
        while (dests.hasNext()) {
            Edge edge = (Edge) dests.next();
            InterTraceBuffer destBuffer = InterTraceBuffer.getBuffer(edge);
            int typeSize = Util.getTypeSize(edge.getType());
            int writeWords = typeSize;
            // do steady-state
            if (stage == 3)
                writeWords *= edge.steadyItems();
            else if (stage == 0)
                writeWords *= edge.initItems();
            else
                writeWords *= edge.primePumpItems();
            // make write bytes cache line div
            if (writeWords > 0) {
                if (destBuffer.getEdge().getDest().isFileWriter()
                    && OffChipBuffer.unnecessary(destBuffer.getEdge()
                                                 .getDest()))
                    destBuffer.getOwner().getComputeCode().addFileCommand(
                                                                          false, init || primepump, writeWords, destBuffer);
                else
                    destBuffer.getOwner().getComputeCode().addDRAMCommand(
                                                                          false, stage, Util.cacheLineDiv(writeWords * 4),
                                                                          destBuffer, false);
            }
        }
    }


    /**
     * Generate the dram commands for the input for a filter and the output from
     * a filter after it is joined and before it is split, respectively. 
     */
    private static void generateFilterDRAMCommand(FilterTraceNode filterNode,
                                                  FilterInfo filterInfo, RawTile tile, boolean init, boolean primepump) {
        generateInputFilterDRAMCommand(filterNode, filterInfo, tile, init,
                                       primepump);
        generateFilterOutputDRAMCommand(filterNode, filterInfo, tile, init,
                                        primepump);
    }

    /**
     * Generate the dram command for the input for a filter from the dram after
     * it is joined into the proper dram.
     */
    private static void generateInputFilterDRAMCommand(FilterTraceNode filterNode, 
            FilterInfo filterInfo, RawTile tile,
            boolean init, boolean primepump) {
        // only generate a DRAM command for filters connected to input or output
        // trace nodes
        if (filterNode.getPrevious() != null
            && filterNode.getPrevious().isInputTrace()) {

            // get this buffer or this first upstream non-redundant buffer
            OffChipBuffer buffer = IntraTraceBuffer.getBuffer(
                                                              (InputTraceNode) filterNode.getPrevious(), filterNode)
                .getNonRedundant();

            if (buffer == null)
                return;

            // get the number of items received
            int items = filterInfo.totalItemsReceived(init, primepump);

            // return if there is nothing to receive
            if (items == 0)
                return;

            int stage = 0;
            if (!init && !primepump)
                stage = 3;

            // the transfer size rounded up to by divisible by a cacheline
            int words = (items * Util.getTypeSize(filterNode.getFilter()
                                                  .getInputType()));

            if (buffer.getDest() instanceof OutputTraceNode
                && ((OutputTraceNode) buffer.getDest()).isFileReader())
                tile.getComputeCode().addFileCommand(true, init || primepump,
                                                     words, buffer);
            else
                tile.getComputeCode().addDRAMCommand(true, stage,
                                                     Util.cacheLineDiv(words * 4), buffer, true);
        }
    }

    /**
     * Generate the streaming dram command to send the output from the filter
     * tile to the dram before it is split (if necessary).
     */
    private static void generateFilterOutputDRAMCommand(FilterTraceNode filterNode, 
            FilterInfo filterInfo, RawTile tile,
            boolean init, boolean primepump) {
        if (filterNode.getNext() != null
            && filterNode.getNext().isOutputTrace()) {
            // get this buffer or null if there are no outputs
            OutputTraceNode output = (OutputTraceNode) filterNode.getNext();
            OffChipBuffer buffer = IntraTraceBuffer.getBuffer(filterNode,
                                                              output).getNonRedundant();
            if (buffer == null)
                return;

            // set to true if the only destination is a file, and
            // everything in between is unnecessary
            boolean fileDest = false;
            if (output.oneOutput()
                && OffChipBuffer.unnecessary(output)
                && output.getSingleEdge().getDest().isFileWriter()
                && OffChipBuffer.unnecessary(output.getSingleEdge()
                                             .getDest()))
                fileDest = true;

            int stage = 0;
            if (!init && !primepump)
                stage = 3;

            // get the number of items sent
            int items = filterInfo.totalItemsSent(init, primepump);

            if (items > 0) {
                int words = (items * Util.getTypeSize(filterNode.getFilter()
                                                      .getOutputType()));
                if (fileDest)
                    tile.getComputeCode().addFileCommand(false,
                                                         init || primepump, words, buffer);
                else {
                    SpaceTimeBackend
                        .println("Generating DRAM store command with "
                                 + items
                                 + " items, typesize "
                                 + Util.getTypeSize(filterNode.getFilter()
                                                    .getOutputType()) + " and " + words
                                 + " words");
                    tile.getComputeCode().addDRAMCommand(false, stage,
                                                         Util.cacheLineDiv(words * 4), buffer, false);
                }
            }
        }
    }

    // see if the switch for the filter needs disregard some of the input
    // because
    // it is not a multiple of the cacheline
    private static void handleUnneededInput(FilterTraceNode traceNode,
                                            boolean init, boolean primepump, int items) {
        InputTraceNode in = (InputTraceNode) traceNode.getPrevious();

        FilterInfo filterInfo = FilterInfo.getFilterInfo(traceNode);
        // int items = filterInfo.totalItemsReceived(init, primepump), typeSize;
        int typeSize;

        typeSize = Util.getTypeSize(traceNode.getFilter().getInputType());

        // see if it is a mulitple of the cache line
        if ((items * typeSize) % RawChip.cacheLineWords != 0) {
            int dummyItems = RawChip.cacheLineWords
                - ((items * typeSize) % RawChip.cacheLineWords);
            SpaceTimeBackend.println("Received items (" + (items * typeSize)
                                     + ") not divisible by cache line, disregard " + dummyItems);
            SwitchCodeStore.disregardIncoming(IntraTraceBuffer.getBuffer(in,
                                                                         traceNode).getDRAM(), dummyItems, init || primepump);
        }
    }

    // see if the switch needs to generate dummy values to fill a cache line in
    // the streaming
    // dram
    private static void fillCacheLine(FilterTraceNode traceNode, boolean init,
                                      boolean primepump, int items) {

        OutputTraceNode out = (OutputTraceNode) traceNode.getNext();
        FilterInfo filterInfo = FilterInfo.getFilterInfo(traceNode);

        // get the number of items sent
        // int items = filterInfo.totalItemsSent(init, primepump), typeSize;
        int typeSize;

        typeSize = Util.getTypeSize(traceNode.getFilter().getOutputType());
        // see if a multiple of cache line, if not generate dummy values...
        if ((items * typeSize) % RawChip.cacheLineWords != 0) {
            int dummyItems = RawChip.cacheLineWords
                - ((items * typeSize) % RawChip.cacheLineWords);
            SpaceTimeBackend.println("Sent items (" + (items * typeSize)
                                     + ") not divisible by cache line, add " + dummyItems);

            SwitchCodeStore.dummyOutgoing(IntraTraceBuffer.getBuffer(traceNode,
                                                                     out).getDRAM(), dummyItems, init || primepump);
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
    private static void joinInputTrace(InputTraceNode traceNode, boolean init,
                                       boolean primepump) {
        FilterTraceNode filter = (FilterTraceNode) traceNode.getNext();

        // do not generate the switch code if it is not necessary
        if (OffChipBuffer.unnecessary(traceNode))
            return;

        FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
        // calculate the number of items received
        int items = filterInfo.totalItemsReceived(init, primepump), iterations, stage = 1, typeSize;

        // noting to do for this stage
        if (items == 0)
            return;

        // the stage we are generating code for as used below for
        // generateSwitchCode()
        if (!init && !primepump)
            stage = 2;

        typeSize = Util.getTypeSize(filter.getFilter().getInputType());
        // the numbers of times we should cycle thru this "joiner"
        assert items % traceNode.totalWeights() == 0 : "weights on input trace node does not divide evenly with items received";
        iterations = items / traceNode.totalWeights();

        StreamingDram[] dest = { IntraTraceBuffer.getBuffer(traceNode, filter)
                                 .getDRAM() };

        // generate comments to make the code easier to read when debugging
        dest[0].getNeighboringTile().getSwitchCode().appendComment(
                                                                   init || primepump,
                                                                   "Start join: This is the dest (" + filter.toString() + ")");

        Iterator sources = traceNode.getSourceSet().iterator();
        while (sources.hasNext()) {
            StreamingDram dram = InterTraceBuffer.getBuffer(
                                                            (Edge) sources.next()).getNonRedundant().getDRAM();
            dram.getNeighboringTile().getSwitchCode().appendComment(
                                                                    init || primepump,
                                                                    "Start join: This a source (" + dram.toString() + ")");
        }

        //generate the switch code...
        if (SWITCH_COMP && iterations > SC_THRESHOLD) {
            // create a loop to compress the switch code

            // find all the tiles used in this join
            HashSet tiles = new HashSet();
            for (int j = 0; j < traceNode.getWeights().length; j++) {
                // get the source buffer, pass thru redundant buffer(s)
                StreamingDram source = InterTraceBuffer.getBuffer(
                                                                  traceNode.getSources()[j]).getNonRedundant().getDRAM();
                tiles.addAll(SwitchCodeStore.getTilesInRoutes(source, dest));
            }
            // generate the loop header on all tiles involved
            HashMap labels = SwitchCodeStore.switchLoopHeader(tiles,
                                                              iterations, init, primepump);
            // generate the switch instructions
            for (int j = 0; j < traceNode.getWeights().length; j++) {
                // get the source buffer, pass thru redundant buffer(s)
                StreamingDram source = InterTraceBuffer.getBuffer(
                                                                  traceNode.getSources()[j]).getNonRedundant().getDRAM();
                for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                    for (int q = 0; q < typeSize; q++)
                        SwitchCodeStore.generateSwitchCode(source, dest, stage);
                }
            }
            // generate the loop trailer
            SwitchCodeStore.switchLoopTrailer(labels, init, primepump);
        } else {
            for (int i = 0; i < iterations; i++) {
                for (int j = 0; j < traceNode.getWeights().length; j++) {
                    // get the source buffer, pass thru redundant buffer(s)
                    StreamingDram source = InterTraceBuffer.getBuffer(
                                                                      traceNode.getSources()[j]).getNonRedundant()
                        .getDRAM();
                    for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                        for (int q = 0; q < typeSize; q++)
                            SwitchCodeStore.generateSwitchCode(source, dest,
                                                               stage);
                    }
                }
            }
        }

        // because transfers must be cache line size divisible...
        // generate dummy values to fill the cache line!
        if ((items * typeSize) % RawChip.cacheLineWords != 0
            && !(traceNode.isFileWriter() && OffChipBuffer
                 .unnecessary(traceNode))) {
            int dummy = RawChip.cacheLineWords
                - ((items * typeSize) % RawChip.cacheLineWords);
            SwitchCodeStore.dummyOutgoing(dest[0], dummy, init || primepump);
        }
        // disregard remainder of inputs coming from temp offchip buffers
        for (int i = 0; i < traceNode.getSources().length; i++) {
            Edge edge = traceNode.getSources()[i];
            int remainder = ((iterations * typeSize * traceNode.getWeight(edge)) % RawChip.cacheLineWords);
            if (remainder > 0
                && !(edge.getSrc().isFileReader() && OffChipBuffer
                     .unnecessary(edge.getSrc())))
                SwitchCodeStore.disregardIncoming(InterTraceBuffer.getBuffer(
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
            StreamingDram dram = InterTraceBuffer.getBuffer(
                                                            (Edge) sources.next()).getNonRedundant().getDRAM();
            dram.getNeighboringTile().getSwitchCode().appendComment(
                                                                    init || primepump,
                                                                    "End join: This a source (" + dram.toString() + ")");
        }
    }

    
    
    
    /**
     * Generate the switch code to split the output trace into its necessary temp
     * buffers.  This function will create loops (if applicable) and call 
     * performSplitOutputTrace to actually generate each switch instruction.  So this
     * function is responsible for code organization.
     * 
     * Another long function!
     * 
     * @param traceNode
     * @param init
     * @param primepump
     */
    private static void splitOutputTrace(OutputTraceNode traceNode,
                                         boolean init, boolean primepump)

    {
        FilterTraceNode filter = (FilterTraceNode) traceNode.getPrevious();
        // check to see if the splitting is necessary
        if (OffChipBuffer.unnecessary(traceNode))
            return;

        FilterInfo filterInfo = FilterInfo.getFilterInfo(filter);
        // calculate the number of items sent
        int items = filterInfo.totalItemsSent(init, primepump);
        StreamingDram sourcePort = IntraTraceBuffer
            .getBuffer(filter, traceNode).getDRAM();
        // the numbers of times we should cycle thru this "splitter"
        assert items % traceNode.totalWeights() == 0 : "weights on output trace node does not divide evenly with items sent";
        int iterations = items / traceNode.totalWeights();

        // add some comments to the switch code
        sourcePort.getNeighboringTile().getSwitchCode().appendComment(
                                                                      init || primepump,
                                                                      "Start split: This is the source (" + filter.toString() + ")");
        Iterator dests = traceNode.getDestSet().iterator();
        while (dests.hasNext()) {
            StreamingDram dram = InterTraceBuffer
                .getBuffer((Edge) dests.next()).getDRAM();
            dram.getNeighboringTile().getSwitchCode().appendComment(
                                                                    init || primepump,
                                                                    "Start split: This a dest (" + dram.toString() + ")");
        }

        // SpaceTimeBackend.println("Split Output Trace: " + traceNode + "it: "
        // + iterations + " ppSteadyIt: " +
        // ppSteadyIt);
        // System.out.println(traceNode.debugString());

        // see if we want to compress (loop) the switch instructions, we cannot
        if (SWITCH_COMP && iterations > SC_THRESHOLD) {
            assert iterations > 1;
            Iterator tiles = getTilesUsedInSplit(traceNode,
                                                 IntraTraceBuffer.getBuffer(filter, traceNode).getDRAM())
                .iterator();

            HashMap labels = new HashMap();
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

            performSplitOutputTrace(traceNode, filter, filterInfo, init,
                                    primepump, 1);

            // now generate the jump back
            tiles = getTilesUsedInSplit(traceNode,
                                        IntraTraceBuffer.getBuffer(filter, traceNode).getDRAM())
                .iterator();
            while (tiles.hasNext()) {
                RawTile tile = (RawTile) tiles.next();
                Label label = (Label) labels.get(tile);
                // add the branch back
                BnezdIns branch = new BnezdIns(FILTER_FIRE_LOOP_REG,
                                               FILTER_FIRE_LOOP_REG, label.getLabel());
                tile.getSwitchCode().appendIns(branch, (init || primepump));
            }

            // end loop

            fillCacheLineSplitOutputTrace(traceNode, filter, filterInfo, init,
                                          primepump, iterations);
        } else { //no compression
            performSplitOutputTrace(traceNode, filter, filterInfo, init,
                                    primepump, iterations);
            fillCacheLineSplitOutputTrace(traceNode, filter, filterInfo, init,
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
            && !(traceNode.isFileReader() && OffChipBuffer
                 .unnecessary(traceNode))) {
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
            StreamingDram dram = InterTraceBuffer
                .getBuffer((Edge) dests.next()).getDRAM();
            dram.getNeighboringTile().getSwitchCode().appendComment(
                                                                    init || primepump,
                                                                    "End split: This a dest (" + dram.toString() + ")");
        }

    }

    /**
     * Generate the actual switch instructions to perform the splitting of the output trace
     * for the given number of iterations in the given stage.  splitOutputTrace above is
     * responsible for code organization (i
     * 
     * @param traceNode
     * @param filter
     * @param filterInfo
     * @param init
     * @param primepump
     * @param iterations
     */
    private static void performSplitOutputTrace(OutputTraceNode traceNode,
                                                FilterTraceNode filter, FilterInfo filterInfo, boolean init,
                                                boolean primepump, int iterations)
    {
        if (iterations > 0) {
            int stage = 1, typeSize;
            // the stage we are generating code for as used below for
            // generateSwitchCode()
            if (!init && !primepump)
                stage = 2;

            typeSize = Util.getTypeSize(filter.getFilter().getOutputType());

            SpaceTimeBackend.println("Generating Switch Code for " + traceNode
                                     + " iterations " + iterations);

            StreamingDram sourcePort = IntraTraceBuffer.getBuffer(filter, traceNode).getDRAM();
          
            for (int i = 0; i < iterations; i++) {
                for (int j = 0; j < traceNode.getWeights().length; j++) {
                    for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                        // generate the array of compute node dests
                        ComputeNode dests[] = new ComputeNode[traceNode
                                                              .getDests()[j].length];
                        for (int d = 0; d < dests.length; d++)
                            dests[d] = InterTraceBuffer.getBuffer(
                                                                  traceNode.getDests()[j][d]).getDRAM();
                        for (int q = 0; q < typeSize; q++)
                            SwitchCodeStore.generateSwitchCode(sourcePort,
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
    private static void fillCacheLineSplitOutputTrace(OutputTraceNode traceNode, 
            FilterTraceNode filter,
            FilterInfo filterInfo, boolean init, boolean primepump,
            int iterations) {
        if (iterations > 0) {
            int typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
            // write dummy values into each temp buffer with a remainder
            Iterator it = traceNode.getDestSet().iterator();
            while (it.hasNext()) {
                Edge edge = (Edge) it.next();
                int remainder = ((typeSize * iterations * traceNode
                                  .getWeight(edge)) % RawChip.cacheLineWords);
                // don't fill cache line for files
                if (remainder > 0
                    && !(edge.getDest().isFileWriter() && OffChipBuffer
                         .unnecessary(edge.getDest())))
                    SwitchCodeStore.dummyOutgoing(InterTraceBuffer.getBuffer(
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
     * @return
     */
    public static HashSet getTilesUsedInSplit(OutputTraceNode traceNode,
                                              StreamingDram sourcePort) {
        // find all the tiles used in the split
        HashSet tiles = new HashSet();
        for (int j = 0; j < traceNode.getWeights().length; j++) {
            for (int k = 0; k < traceNode.getWeights()[j]; k++) {
                // generate the array of compute node dests
                ComputeNode dests[] = new ComputeNode[traceNode.getDests()[j].length];
                for (int d = 0; d < dests.length; d++)
                    dests[d] = InterTraceBuffer.getBuffer(traceNode.getDests()[j][d]).getDRAM();
                tiles.addAll(SwitchCodeStore.getTilesInRoutes(sourcePort, dests));
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
    private static void createInitLinearSwitchCode(FilterTraceNode node,
                                                   FilterInfo filterInfo, int mult, int buffer, RawTile tile,
                                                   RawChip rawChip) {
        System.err.println("Creating switchcode linear: " + node + " " + mult);
        ComputeNode sourceNode = null;
        // Get sourceNode and input port
        if (node.getPrevious().isFilterTrace())
            sourceNode = rawChip.getTile(((FilterTraceNode) node.getPrevious())
                                         .getX(), ((FilterTraceNode) node.getPrevious()).getY());
        else {
            if (KjcOptions.magicdram && node.getPrevious() != null
                && node.getPrevious().isInputTrace() && tile.hasIODevice())
                sourceNode = tile.getIODevice();
            else
                sourceNode = IntraTraceBuffer.getBuffer(
                                                        (InputTraceNode) node.getPrevious(), node)
                    .getNonRedundant().getDRAM();
        }
        SwitchIPort src = rawChip.getIPort(sourceNode, tile);
        SwitchIPort src2 = rawChip.getIPort2(sourceNode, tile);
        sourceNode = null;
        // Get destNode and output port
        ComputeNode destNode = null;
        if (node.getNext().isFilterTrace())
            destNode = rawChip.getTile(((FilterTraceNode) node.getNext())
                                       .getX(), ((FilterTraceNode) node.getNext()).getY());
        else {
            if (KjcOptions.magicdram && node.getNext() != null
                && node.getNext().isOutputTrace() && tile.hasIODevice())
                destNode = tile.getIODevice();
            else {
                destNode = IntraTraceBuffer.getBuffer(node,
                                                      (OutputTraceNode) node.getNext()).getNonRedundant()
                    .getDRAM();
            }
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
    private static void createLinearSwitchCode(FilterTraceNode node,
                                               FilterInfo filterInfo, int mult, RawTile tile, RawChip rawChip) {
        System.err.println("Creating switchcode linear: " + node + " " + mult);
        ComputeNode sourceNode = null;
        // Get sourceNode and input port
        if (node.getPrevious().isFilterTrace())
            sourceNode = rawChip.getTile(((FilterTraceNode) node.getPrevious())
                                         .getX(), ((FilterTraceNode) node.getPrevious()).getY());
        else {
            if (KjcOptions.magicdram && node.getPrevious() != null
                && node.getPrevious().isInputTrace() && tile.hasIODevice())
                sourceNode = tile.getIODevice();
            else
                sourceNode = IntraTraceBuffer.getBuffer(
                                                        (InputTraceNode) node.getPrevious(), node)
                    .getNonRedundant().getDRAM();
        }
        SwitchIPort src = rawChip.getIPort(sourceNode, tile);
        SwitchIPort src2 = rawChip.getIPort2(sourceNode, tile);
        sourceNode = null;
        // Get destNode and output port
        ComputeNode destNode = null;
        if (node.getNext().isFilterTrace())
            destNode = rawChip.getTile(((FilterTraceNode) node.getNext())
                                       .getX(), ((FilterTraceNode) node.getNext()).getY());
        else {
            if (KjcOptions.magicdram && node.getNext() != null
                && node.getNext().isOutputTrace() && tile.hasIODevice())
                destNode = tile.getIODevice();
            else {
                destNode = IntraTraceBuffer.getBuffer(node,
                                                      (OutputTraceNode) node.getNext()).getNonRedundant()
                    .getDRAM();
            }
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
     * Create the intra-trace switch code for the filter trace node.
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
    private static void createSwitchCode(FilterTraceNode node, Trace parent,
                                         FilterInfo filterInfo, boolean init, boolean primePump,
                                         boolean linear, RawTile tile, RawChip rawChip) {
        int mult, sentItems = 0;

        // don't cache align if the only source is a file reader
        boolean cacheAlignSource = true;
        if (node.getPrevious() instanceof InputTraceNode) {
            OffChipBuffer buf = IntraTraceBuffer.getBuffer(
                                                           (InputTraceNode) node.getPrevious(), node)
                .getNonRedundant();
            if (buf != null && buf.getDest() instanceof OutputTraceNode
                && ((OutputTraceNode) buf.getDest()).isFileReader())
                cacheAlignSource = false;
        }

        // don't cache align the dest if the true dest is a file writer
        boolean cacheAlignDest = true;
        if (node.getNext() instanceof OutputTraceNode) {
            OutputTraceNode output = (OutputTraceNode) node.getNext();
            if (output.oneOutput()
                && OffChipBuffer.unnecessary(output)
                && output.getSingleEdge().getDest().isFileWriter()
                && OffChipBuffer.unnecessary(output.getSingleEdge()
                                             .getDest()))
                cacheAlignDest = false;
        }

       mult = filterInfo.getMult(init, primePump);


        // switch Compression only in steady state!!!
        boolean switchCompression = SWITCH_COMP && mult > SC_THRESHOLD && !init;

        if (!(init || primePump || !linear)) { // Linear switch code in
            // steadystate
            /*
             * if(primePump) { int bufferSize; FilterContent
             * content=filterInfo.traceNode.getFilter(); final int
             * pos=content.getPos(); final int peek=content.getPeek(); final int
             * pop = content.getPopCount(); int index=content.getTotal()-pos-1;
             * if(index==0) //If first tile bufferSize=filterInfo.remaining;
             * else { //Find first tile TraceNode curNode=node; for(int
             * i=index;i>0;i--) curNode=curNode.getPrevious(); FilterInfo
             * parentInfo=FilterInfo.getFilterInfo((FilterTraceNode)curNode);
             * bufferSize=parentInfo.remaining; } if(filterInfo.initMult>0)
             * bufferSize+=peek-pop;
             * createInitLinearSwitchCode(node,filterInfo,mult,bufferSize,tile,rawChip); }
             * else
             */
            createLinearSwitchCode(node, filterInfo, mult, tile, rawChip);
            sentItems += mult;
        } else if (switchCompression) {
            assert mult > 1;
            sentItems = filterInfo.push * mult;

            filterSwitchCodeCompressed(mult, node, filterInfo, init, primePump,
                                       tile, rawChip);
        } else {
            for (int i = 0; i < mult; i++) {
                // append the receive code
                createReceiveCode(i, node, filterInfo, init, primePump, tile,
                                  rawChip, false);
                // append the send code
                sentItems += createSendCode(i, node, filterInfo, init,
                                            primePump, tile, rawChip, false);
            }
        }

        // now we must take care of the remaining items on the input tape
        // after the initialization phase if the upstream filter produces more
        // than
        // we consume in init
        if (init)
            System.out.println("REMAINING ITEMS: " + filterInfo.remaining);
        if (init && filterInfo.remaining > 0) {
            appendReceiveInstructions(node, filterInfo.remaining
                                      * Util.getTypeSize(node.getFilter().getInputType()),
                                      filterInfo, init, false, tile, rawChip);
        }

        // we must add some switch instructions to account for the fact
        // that we must transfer cacheline sized chunks in the streaming dram
        // do it for the init and the steady state, primepump
        
        // generate code to fill the remainder of the cache line
        if (!KjcOptions.magicdram && node.getNext().isOutputTrace()
            && cacheAlignDest)
            fillCacheLine(node, init, primePump, sentItems);

        // because all dram transfers must be multiples of cacheline
        // generate code to disregard the remainder of the transfer
        if (!KjcOptions.magicdram && node.getPrevious().isInputTrace()
            && cacheAlignSource)
            handleUnneededInput(node, init, primePump, filterInfo
                                .totalItemsReceived(init, primePump));

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
     */
    private static void filterSwitchCodeCompressed(int mult,
                                                   FilterTraceNode node, FilterInfo filterInfo, boolean init,
                                                   boolean primePump, RawTile tile, RawChip rawChip) {
        assert mult < 65535;

        assert !init && !primePump;

        // the items this filter is receiving for this iteration
        int itemsReceiving = filterInfo.itemsNeededToFire(0, init);
        // get the number of items sending on this iteration, only matters
        // if init and if twostage
        int itemsSending = filterInfo.itemsFiring(0, init);

        // are we going to compress the individual send and receive
        // instructions?
        boolean sendCompression = (itemsSending > SC_INS_THRESH), receiveCompression = (itemsReceiving > SC_INS_THRESH);

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
        // items if not
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
        // not
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
    private static void createReceiveCode(int iteration, FilterTraceNode node,
                                          FilterInfo filterInfo, boolean init, boolean primePump,
                                          RawTile tile, RawChip rawChip, boolean compression) {
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
    private static void appendReceiveInstructions(FilterTraceNode node,
                                                  int itemsReceiving, FilterInfo filterInfo, boolean init,
                                                  boolean primePump, RawTile tile, RawChip rawChip) {
        // the source of the data, either a device or another raw tile
        ComputeNode sourceNode = null;

        if (node.getPrevious().isFilterTrace())
            sourceNode = rawChip.getTile(((FilterTraceNode) node.getPrevious())
                                         .getX(), ((FilterTraceNode) node.getPrevious()).getY());
        else {
            if (KjcOptions.magicdram && node.getPrevious() != null
                && node.getPrevious().isInputTrace() && tile.hasIODevice())
                sourceNode = tile.getIODevice();
            else
                sourceNode = IntraTraceBuffer.getBuffer(
                                                        (InputTraceNode) node.getPrevious(), node)
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
            if (KjcOptions.magicdram && node.getPrevious() != null
                && node.getPrevious().isInputTrace())
                createMagicDramLoad((InputTraceNode) node.getPrevious(), node,
                                    (init || primePump), rawChip);
        }
    }

    /**
     * Create the switch code necessary to fire a filter.  So get the item from the
     * switch processor and then route it to a neighboring tile. 
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
    private static int createSendCode(int iteration, FilterTraceNode node,
                                      FilterInfo filterInfo, boolean init, boolean primePump,
                                      RawTile tile, RawChip rawChip, boolean compression) {
        // get the number of items sending on this iteration, only matters
        // if init and if twostage
        int items = filterInfo.itemsFiring(iteration, init);

        if (items == 0)
            return 0;

        ComputeNode destNode = null;

        if (node.getNext().isFilterTrace())
            destNode = rawChip.getTile(((FilterTraceNode) node.getNext())
                                       .getX(), ((FilterTraceNode) node.getNext()).getY());
        else {
            if (KjcOptions.magicdram && node.getNext() != null
                && node.getNext().isOutputTrace() && tile.hasIODevice())
                destNode = tile.getIODevice();
            else {
                destNode = IntraTraceBuffer.getBuffer(node,
                                                      (OutputTraceNode) node.getNext()).getNonRedundant()
                    .getDRAM();
            }

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
            if (KjcOptions.magicdram && node.getNext() != null
                && node.getNext().isOutputTrace())
                createMagicDramStore((OutputTraceNode) node.getNext(), node,
                                     (init || primePump), rawChip);
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
        assert mult > 1;
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
     * switch reg <reg> as its working register.
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
     * magicHandlePredefined(FilterTraceNode predefined, RawChip rawChip,
     * boolean init) { if (init) { //tell the magic dram that it should open the
     * file and create vars for this file if (predefined.isFileInput()) { //get
     * the filter connected to this file output, just take the first one
     * //because they all should be mapped to the same tile FilterTraceNode next =
     * FilterInfo.getFilterInfo(predefined).getNextFilters()[0]; if
     * (!rawChip.getTile(next.getX(), next.getY()).hasIODevice())
     * Utils.fail("Tile not connected to io device"); MagicDram dram =
     * (MagicDram)rawChip.getTile(next.getX(), next.getY()).getIODevice();
     * dram.inputFiles.add((FileInputContent)predefined.getFilter()); } else if
     * (predefined.isFileOutput()) { //tell the magic dram that it should open
     * the file and create vars for this file
     * 
     * //get the filter connected to this file output, just take the first one
     * //because they all should be mapped to the same tile FilterTraceNode prev =
     * FilterInfo.getFilterInfo(predefined).getPreviousFilters()[0]; //find the
     * iodevice if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice())
     * Utils.fail("Tile not connected to io device"); //get the dram MagicDram
     * dram = (MagicDram)rawChip.getTile(prev.getX(),
     * prev.getY()).getIODevice();
     * dram.outputFiles.add((FileOutputContent)predefined.getFilter()); } } }
     */

    private static void createMagicDramLoad(InputTraceNode node,
                                            FilterTraceNode next, boolean init, RawChip rawChip) {
        /*
         * if (!rawChip.getTile(next.getX(), next.getY()).hasIODevice())
         * Utils.fail("Tile not connected to io device");
         * 
         * MagicDram dram = (MagicDram)rawChip.getTile(next.getX(),
         * next.getY()).getIODevice();
         * 
         * LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
         * OutputTraceNode output = TraceBufferSchedule.getOutputBuffer(node);
         * insList.add(new MagicDramLoad(node, output)); dram.addBuffer(output,
         * node);
         */
    }

    /**
     * Generate a single magic dram store instruction for this output trace node
     */
    private static void createMagicDramStore(OutputTraceNode node,
                                             FilterTraceNode prev, boolean init, RawChip rawChip)

    {
        /*
         * if (!rawChip.getTile(prev.getX(), prev.getY()).hasIODevice())
         * Utils.fail("Tile not connected to io device"); //get the dram
         * MagicDram dram = (MagicDram)rawChip.getTile(prev.getX(),
         * prev.getY()).getIODevice(); //get the list we should add to
         * LinkedList insList = init ? dram.initInsList : dram.steadyInsList;
         * //add the instruction insList.add(new MagicDramStore(node,
         * TraceBufferSchedule.getInputBuffers(node)));
         */
    }

    /*
     * private static void generateOutputDRAMCommands(OutputTraceNode output,
     * boolean init, boolean primepump, FilterTraceNode filter, int items, int
     * stage) { if (items == 0) return; int iterations, typeSize;
     * 
     * typeSize = Util.getTypeSize(filter.getFilter().getOutputType());
     * 
     * //the numbers of times we should cycle thru this "splitter" assert items %
     * output.totalWeights() == 0: "weights on output trace node does not divide
     * evenly with items sent"; iterations = items / output.totalWeights();
     * 
     * //generate the command to read from the src of the output trace node
     * OffChipBuffer srcBuffer = IntraTraceBuffer.getBuffer(filter, output); int
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
     * output.getDestSet().iterator(); while (dests.hasNext()){ Edge edge =
     * (Edge)dests.next(); OffChipBuffer destBuffer =
     * InterTraceBuffer.getBuffer(edge); int writeBytes = iterations * typeSize *
     * output.getWeight(edge) * 4; writeBytes = Util.cacheLineDiv(writeBytes);
     * destBuffer.getOwner().getComputeCode().addDRAMCommand(false, stage,
     * writeBytes, destBuffer, false); } }
     */
}

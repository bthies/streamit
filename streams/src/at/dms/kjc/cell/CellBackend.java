package at.dms.kjc.cell;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import at.dms.kjc.JInterfaceDeclaration;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.CommonPasses;
import at.dms.kjc.backendSupport.DumpSlicesAndChannels;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndPartitioner;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.sir.SIRGlobal;
import at.dms.kjc.sir.SIRHelper;
import at.dms.kjc.sir.SIRInterfaceTable;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.SIRStructure;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.spacetime.AddBuffering;
import at.dms.kjc.spacetime.BasicGenerateSteadyStateSchedule;
import at.dms.kjc.vanillaSlice.EmitStandaloneCode;

public class CellBackend {
    /** holds pointer to BackEndFactory instance during back end portion of this compiler. */
    public static BackEndFactory<CellChip, CellPU, CellComputeCodeStore, Integer> backEndBits = null;

    public static void run(SIRStream str,
            JInterfaceDeclaration[] interfaces,
            SIRInterfaceTable[] interfaceTables,
            SIRStructure[]structs,
            SIRHelper[] helpers,
            SIRGlobal global) {
        System.out.println("Hi, I'm the new back end.");
        
        int numCores = 6;
        
        // The usual optimizations
        CommonPasses commonPasses = new CommonPasses();
        /*Slice[] sliceGraph = */ commonPasses.run(str, interfaces, 
                interfaceTables, structs, helpers, global, numCores);
        // partitioner contains information about the Slice graph.
        Partitioner partitioner = commonPasses.getPartitioner();
        // Create code for predefined content: file readers, file writers.
        partitioner.createPredefinedContent();
        // guarantee that we are not going to hack properties of filters in the future
        FilterInfo.canUse();
        // fix any rate skew introduced in conversion to Slice graph.
        AddBuffering.doit(partitioner,false,numCores);
        // decompose any pipelines of filters in the Slice graph.
        partitioner.ensureSimpleSlices();

        // Set schedules for initialization, priming (if --spacetime), and steady state.
        SpaceTimeScheduleAndPartitioner schedule = new SpaceTimeScheduleAndPartitioner(partitioner);
        // set init schedule in standard order
        schedule.setInitSchedule(DataFlowOrder.getTraversal(partitioner.getSliceGraph()));
        // set prime pump schedule (if --spacetime and not --noswpipe)
        new at.dms.kjc.spacetime.GeneratePrimePumpSchedule(schedule).schedule(partitioner.getSliceGraph());
        // set steady schedule in standard order unless --spacetime in which case in 
        // decreasing order of estimated work
        new BasicGenerateSteadyStateSchedule(schedule, partitioner).schedule();

        // create a collection of (very uninformative) processor descriptions.
        CellChip cellChip = new CellChip(numCores);

        // assign SliceNodes to processors
        Layout<CellPU> layout;
//        if (KjcOptions.spacetime && !KjcOptions.noswpipe) {
//            layout = new BasicGreedyLayout<CellPU>(schedule, cellChip.toArray());
//        } else {
            layout = new CellNoSWPipeLayout(schedule, cellChip);
//        }
        layout.run();
 
        // create other info needed to convert Slice graphs to Kopi code + Channels
        BackEndFactory<CellChip, CellPU, CellComputeCodeStore, Integer> cellBackEndBits  = new CellBackendFactory(cellChip);
        backEndBits = cellBackEndBits;
        backEndBits.setLayout(layout);
        
        // now convert to Kopi code plus channels.  (Javac gives error if folowing two lines are combined)
        BackEndScaffold top_call = backEndBits.getBackEndMain();
        top_call.run(schedule, backEndBits);

        // Dump graphical representation
        DumpSlicesAndChannels.dumpGraph("slicesAndChannels.dot", partitioner, backEndBits);
        
        /*
         * Emit code to structs.h
         */
        String outputFileName = "structs.h";
        try {
            CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
            // write out C code
            EmitStandaloneCode.emitTypedefs(structs,backEndBits,p);
            p.close();
        } catch (IOException e) {
            throw new AssertionError("I/O error on " + outputFileName + ": " + e);
        }
    
        
//        /*
//         * Emit code to str.c
//         */
//        outputFileName = "str.c";
//        try {
//        CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
//        // write out C code
//        EmitStandaloneCode codeEmitter = new EmitStandaloneCode(cellBackEndBits);
//        codeEmitter.generateCHeader(p);
//        // Concat emitted code for all nodes into one file.
//        for (ComputeNode n : cellBackEndBits.getComputeNodes().toArray()) {
//            codeEmitter.emitCodeForComputeNode(n,p);
//        }
//        codeEmitter.generateMain(p);
//        p.close();
//        } catch (IOException e) {
//            throw new AssertionError("I/O error on " + outputFileName + ": " + e);
//        }
        System.exit(0);
    }
}

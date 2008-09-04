package at.dms.kjc.vanillaSlice;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.common.CodegenPrintWriter;
import java.io.*;
/**
 * The entry to the back end for a uniprocesor or cluster.
 */
public class UniBackEnd {
    /** holds pointer to BackEndFactory instance during back end portion of this compiler. */
    public static BackEndFactory<UniProcessors, UniProcessor, UniComputeCodeStore, Integer> backEndBits = null;
    
    /**
     * Top level method for uniprocessor backend, called via reflection from {@link at.dms.kjc.StreaMITMain}.
     * @param str               SIRStream from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaces        JInterfaceDeclaration[] from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaceTables   SIRInterfaceTable[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param structs           SIRStructure[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param helpers           SIRHelper[] from {@link at.dms.kjc.Kopi2SIR}
     * @param global            SIRGlobal from  {@link at.dms.kjc.Kopi2SIR}
     */
    public static void run(SIRStream str,
            JInterfaceDeclaration[] interfaces,
            SIRInterfaceTable[] interfaceTables,
            SIRStructure[]structs,
            SIRHelper[] helpers,
            SIRGlobal global) {

        int numCores = KjcOptions.newSimple;
        
        // The usual optimizations and transformation to slice graph
        CommonPasses commonPasses = new CommonPasses();
        // perform standard optimizations.
        commonPasses.run(str, interfaces, interfaceTables, structs, helpers, global, numCores);
        // perform some standard cleanup on the slice graph.
        commonPasses.simplifySlices();
        // Set schedules for initialization, prime-pump (if KjcOptions.spacetime), and steady state.
        SpaceTimeScheduleAndSlicer schedule = commonPasses.scheduleSlices();
        // partitioner contains information about the Slice graph used by dumpGraph
        SIRSlicer slicer = (SIRSlicer)commonPasses.getSlicer();


        // create a collection of (very uninformative) processor descriptions.
        UniProcessors processors = new UniProcessors(numCores);

        // assign SliceNodes to processors
        Layout<UniProcessor> layout;
        if (KjcOptions.spacetime && !KjcOptions.noswpipe) {
            layout = new BasicGreedyLayout<UniProcessor>(schedule, processors.toArray());
        } else {
            layout = new NoSWPipeLayout<UniProcessor,UniProcessors>(schedule, processors);
        }
        layout.runLayout();
 
        // create other info needed to convert Slice graphs to Kopi code + Channels
        BackEndFactory<UniProcessors, UniProcessor, UniComputeCodeStore, Integer> uniBackEndBits  = new UniBackEndFactory(processors);
        backEndBits = uniBackEndBits;
        backEndBits.setLayout(layout);
        
        // now convert to Kopi code plus channels.  (Javac gives error if folowing two lines are combined)
        BackEndScaffold top_call = backEndBits.getBackEndMain();
        top_call.run(schedule, backEndBits);

        // Dump graphical representation
        DumpSlicesAndChannels.dumpGraph("slicesAndChannels.dot", slicer, backEndBits);
        
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
    
        
        /*
         * Emit code to str.c
         */
        outputFileName = "str.c";
        try {
        CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
        // write out C code
        EmitStandaloneCode codeEmitter = new EmitStandaloneCode(uniBackEndBits);
        codeEmitter.generateCHeader(p);
        // Concat emitted code for all nodes into one file.
        for (ComputeNode n : uniBackEndBits.getComputeNodes().toArray()) {
            codeEmitter.emitCodeForComputeNode(n,p);
        }
        codeEmitter.generateMain(p);
        p.close();
        } catch (IOException e) {
            throw new AssertionError("I/O error on " + outputFileName + ": " + e);
        }
        // return success
        System.exit(0);
    }

}

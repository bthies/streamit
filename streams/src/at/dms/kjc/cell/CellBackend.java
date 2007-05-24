package at.dms.kjc.cell;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import at.dms.kjc.JInterfaceDeclaration;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.CommonPasses;
import at.dms.kjc.backendSupport.DumpSlicesAndChannels;
import at.dms.kjc.backendSupport.EmitCode;
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

    /**
     * Top level method for Cell backend, called via reflection from {@link at.dms.kjc.StreaMITMain}.
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
        System.out.println("Hi, I'm the new back end.");
        
        int numCores = 6;
        
        // The usual optimizations and transformation to slice graph
        CommonPasses commonPasses = new CommonPasses();
        // perform standard optimizations.
        commonPasses.run(str, interfaces, interfaceTables, structs, helpers, global, numCores);
        
        // perform some standard cleanup on the slice graph.
        commonPasses.simplifySlices();
//        // guarantee that we are not going to hack properties of filters in the future
//        FilterInfo.canUse();
//        // fix any rate skew introduced in conversion to Slice graph.
//        AddBuffering.doit(commonPasses.getPartitioner(),false,numCores);
//        // decompose any pipelines of filters in the Slice graph.
//        commonPasses.getPartitioner().ensureSimpleSlices();
        
        // Set schedules for initialization, prime-pump (if KjcOptions.spacetime), and steady state.
        SpaceTimeScheduleAndPartitioner schedule = commonPasses.scheduleSlices();
        // partitioner contains information about the Slice graph used by dumpGraph
        Partitioner partitioner = commonPasses.getPartitioner();

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
        CellBackendFactory cellBackEndBits  = new CellBackendFactory(cellChip);
        backEndBits = cellBackEndBits;
        backEndBits.setLayout(layout);

        CellComputeCodeStore ppuCS = cellBackEndBits.getPPU().getComputeCode();        
        ppuCS.addSPUInit();
        ppuCS.addCallBackFunction();
        ppuCS.addPPUBuffers();
        //ppuCS.addFileReader();
        //ppuCS.addFileWriter();
        
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
    
        
        /*
         * Emit code to strN.c
         */
        for (int n = 0; n < cellBackEndBits.getComputeNodes().size(); n++) {
            outputFileName = "str" + n + ".c";
            try {
                CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
                CellPU nodeN = cellBackEndBits.getComputeNode(n);
                // write out C code
            
                EmitCellCode codeEmitter = new EmitCellCode(cellBackEndBits);
                //codeEmitter.generateCHeader(p);
                if (n == 0) {
                    p.println("#include \"spulib.h\"");
                    p.println("#include \"filters.h\"");
                    p.println("#include \"spusymbols.h\"");
                    p.println("#include <stdio.h>");
                } else {
                    p.println("#include \"filterdefs.h\"");
                    p.println("#include \"filters.h\"");
                    p.println();
                    p.println("#define FILTER_NAME 0");
                    p.println("#define ITEM_TYPE int");
                    p.println("#include \"beginfilter.h\"");
                }
                codeEmitter.emitCodeForComputeNode(nodeN, p);
                if (n == 0) { codeEmitter.generateMain(p); }
                else {
                   p.println("#include \"endfilter.h\"");
                }
                p.close();
            } catch (IOException e) {
                throw new AssertionError("I/O error on " + outputFileName + ": " + e);
            }
        }
        
        System.exit(0);
    }
}

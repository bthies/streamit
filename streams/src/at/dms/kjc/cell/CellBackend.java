package at.dms.kjc.cell;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import at.dms.kjc.JInterfaceDeclaration;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.CommonPasses;
import at.dms.kjc.backendSupport.DumpSlicesAndChannels;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndPartitioner;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.sir.SIRGlobal;
import at.dms.kjc.sir.SIRHelper;
import at.dms.kjc.sir.SIRInterfaceTable;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.SIRStructure;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.kjc.vanillaSlice.EmitStandaloneCode;

public class CellBackend {
    /** holds pointer to BackEndFactory instance during back end portion of this compiler. */
    public static CellBackendFactory backEndBits = null;

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
        
        int numCores = KjcOptions.cell;
        
        System.out.println("Entering Cell backend. Compiling to " + numCores + " SPUs.");
        
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
        layout = new CellNoSWPipeLayout(schedule, cellChip);
        layout.run();
 
        // create other info needed to convert Slice graphs to Kopi code + Channels
        CellBackendFactory cellBackEndBits  = new CellBackendFactory(cellChip);
        backEndBits = cellBackEndBits;
        backEndBits.setLayout(layout);
        
        CellComputeCodeStore ppuCS = cellBackEndBits.getPPU().getComputeCode();
        if (KjcOptions.celldyn) {

        }
        else {
            ppuCS.addSPUInit(schedule);
            ppuCS.addCallBackFunction();
            ppuCS.initSpulibClock();
            ppuCS.addDataAddressField();
            ppuCS.setupInputBufferAddress();
            ppuCS.setupOutputBufferAddress();
            ppuCS.setupDataAddress();
        }        
        // now convert to Kopi code plus channels.  (Javac gives error if folowing two lines are combined)
        CellBackendScaffold top_call = backEndBits.getBackEndMain();
        top_call.run(schedule, backEndBits);
        
        for (SPU spu : cellBackEndBits.getSPUs()) {
            for (CellComputeCodeStore cs : spu.getComputeCodeStores())
                cs.addInitFunctions();
        }
        
        if (KjcOptions.celldyn) {
            ppuCS.dynamic();
        }
        
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
        
        CellPU ppu = cellBackEndBits.getComputeNode(0);
        outputFileName = "str0.c";
        try {
            CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
            // write out C code
    
            EmitCellCode codeEmitter = new EmitCellCode(cellBackEndBits);
            codeEmitter.generatePPUCHeader(p);
            codeEmitter.emitCodeForComputeNode(ppu, p);
            codeEmitter.generateMain(p);
            p.close();
        } catch (IOException e) {
            throw new AssertionError("I/O error on " + outputFileName + ": " + e);
        }
        
        /*
         * Emit code to strN.c
         */
        for (int n = 1; n < cellBackEndBits.getComputeNodes().size(); n++) {
            CellPU nodeN = cellBackEndBits.getComputeNode(n);
            ArrayList<CellComputeCodeStore> codestores = nodeN.getComputeCodeStores();
            for (int c = 0; c < nodeN.getNumComputeCodeStores(); c++) {
                outputFileName = "str" + n + c + ".c";
                CellComputeCodeStore cs = codestores.get(c);
                try {
                    CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
                    // write out C code

                    EmitCellCode codeEmitter = new EmitCellCode(cellBackEndBits);
                    codeEmitter.generateSPUCHeader(p, cs.getSliceNode());

                    codeEmitter.emitCodeForComputeStore(cs, nodeN, p);
                    p.println("#include \"endfilter.h\"");
                    p.close();
                } catch (IOException e) {
                    throw new AssertionError("I/O error on " + outputFileName + ": " + e);
                }
            }
        }
        
        System.exit(0);
    }

    public static int numfilters = 0;
    public static int numchannels = 0;
    
    /**
     * InputSliceNode -> List of input channel IDs
     */
    public static final HashMap<InputSliceNode,LinkedList<Integer>> inputChannelMap = 
        new HashMap<InputSliceNode,LinkedList<Integer>>();
    
    /**
     * OutputSliceNode -> List of output channel IDs
     */
    public static final HashMap<OutputSliceNode,LinkedList<Integer>> outputChannelMap =
        new HashMap<OutputSliceNode,LinkedList<Integer>>();
    
    public static final HashMap<Integer,SliceNode> SPUassignment = 
        new HashMap<Integer,SliceNode>();
    
    public static final HashMap<InterSliceEdge,Integer> channelIdMap = 
        new HashMap<InterSliceEdge,Integer>();
    
    public static final ArrayList<InterSliceEdge> channels = 
        new ArrayList<InterSliceEdge>();
    
    /**
     * List of all the filters in the graph (including RR splitters and joiners 
     * as separate filters)
     */
    public static final ArrayList<SliceNode> filters = 
        new ArrayList<SliceNode>();
    
    public static final HashMap<SliceNode,Integer> filterIdMap = 
        new HashMap<SliceNode,Integer>();
    
    public static final HashMap<OutputSliceNode,Integer> duplicateSplitters = 
        new HashMap<OutputSliceNode,Integer>();
    
    /**
     * InputSliceNode -> ID of the artificial channel connecting it to the 
     * FilterSliceNode
     */
    public static final HashMap<InputSliceNode,Integer> artificialJoinerChannels =
        new HashMap<InputSliceNode,Integer>();
    
    /**
     * OutputSliceNode -> ID of the artificial channel connecting the FilterSliceNode
     * to it
     */
    public static final HashMap<OutputSliceNode,Integer> artificialRRSplitterChannels = 
        new HashMap<OutputSliceNode,Integer>();
    
    public static InterSliceEdge getEdgeBetween(OutputSliceNode src, InputSliceNode dest) {
        for (InterSliceEdge e : src.getDestSequence()) {
            if (e.getDest() == dest)
                return e;
        }
        return null;
    }
}

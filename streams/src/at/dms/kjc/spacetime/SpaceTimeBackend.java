package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.common.ConvertLonelyPops;
import at.dms.kjc.slicegraph.*;
import at.dms.util.Utils;
//import java.util.LinkedList;
//import java.util.ListIterator;
//import at.dms.kjc.iterator.*;
//import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.slicegraph.AdaptivePartitioner;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.FilterInfo;
import at.dms.kjc.slicegraph.FlattenAndPartition;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.SimplePartitioner;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.ComputeNode;
import java.util.*;
//import at.dms.kjc.sir.lowering.FinalUnitOptimize;
//import at.dms.util.SIRPrinter;
//import at.dms.kjc.sir.SIRToStreamIt;

/**
 * The entry to the space time backend for raw.
 */
public class SpaceTimeBackend {
    /** Don't generate the work function code for a filter,
     * instead produce debugging code.
     * (referred to by TraceIRToC, DirectCommunication, BufferredCommunication)
     */
    public static boolean FILTER_DEBUG_MODE = false;
    /** Should we not software pipeline the steady state. 
     * (relationship to KjcOptions.noswpipe? any difference?) */
    public static boolean NO_SWPIPELINE;

    
    //public static boolean FISSION = true;

    private static SIRStructure[] structures;

    private static GreedyBinPacking greedyBinPacking;
    
    // Presumably becomes information about arbitrary multicores
    // in some future incarnation.  For now is very RAW-specific.
    private static RawChip rawChip;
  
    private static double COMP_COMM_RATIO;
    
    // the number of cores that we are compiling for 
    // = rawChip.getTotalTiles() if compiling for RAW.
    
    private static int numCores;
    
    // ComputeNodes for standalone version, 
    // replaces rawChip of Raw version.
    private static ComputeNode[] computeNodes;  
    
    /**
     * Top level method for SpaceTime backend, called via reflection from {@link at.dms.kjc.Main}.
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
	//the original (unmodified) stream graph
	//only used for statistics gathering
	SIRStream origSTR = null;

        structures = structs;
        
        NO_SWPIPELINE = KjcOptions.noswpipe;
        
        WorkEstimate.UNROLL_FOR_WORK_EST = KjcOptions.workestunroll;
        
        //first of all enable altcodegen by default
        //KjcOptions.altcodegen = true;

        // on standalone set numVodes = 1, but there is a sneaky way of specifying some other number.
        if (KjcOptions.standalone) {
            if (KjcOptions.raw > 0) {
                numCores = KjcOptions.raw;
            } else {
                numCores = 1;
            }
            computeNodes = new ComputeNode[numCores];
            for (int i = 0; i < numCores; i++) {
                computeNodes[i] = new ComputeNode();
            }
            KjcOptions.raw = -1;  // set "raw" switch to "missing"
        }
        else if (KjcOptions.raw >= 0) {
            // Set up rawchip data structure if compiling for raw.
            int rawRows = -1;
            int rawColumns = -1;

            //set number of columns/rows
            rawRows = KjcOptions.raw;
            if(KjcOptions.rawcol > -1)
                rawColumns = KjcOptions.rawcol;
            else
                rawColumns = KjcOptions.raw;

            //create the RawChip
            rawChip = new RawChip(rawColumns, rawRows);
            numCores = rawChip.getTotalTiles();
        }
        
        // make arguments to functions be three-address code so can replace max, min, abs
        // and possibly others with macros, knowing that there will be no side effects.
        SimplifyArguments.simplify(str);

        // propagate constants and unroll loop
        System.out.println("Running Constant Prop and Unroll...");
        Set<SIRGlobal> theStatics = new HashSet<SIRGlobal>();
        if (global != null) theStatics.add(global);
        /*Map associatedGlobals =*/ StaticsProp.propagateIntoContainers(str,theStatics);  
        ConstantProp.propagateAndUnroll(str, true);
        System.out.println("Done Constant Prop and Unroll...");

        // convert round(x) to floor(0.5+x) to avoid obscure errors
        RoundToFloor.doit(str);
        // add initPath functions
        EnqueueToInitPath.doInitPath(str);

        // construct stream hierarchy from SIRInitStatements
        ConstructSIRTree.doit(str);

        // VarDecl Raise to move array assignments up

        new VarDeclRaiser().raiseVars(str);

        // do constant propagation on fields
            System.out.println("Running Constant Field Propagation...");
            FieldProp.doPropagate(str);
            System.out.println("Done Constant Field Propagation...");
 
        // expand array initializers loaded from a file
        ArrayInitExpander.doit(str);
        
        if (SIRPortal.findMessageStatements(str)) {
            Utils.fail("Teleport messaging is not yet supported in the Raw backend.");
        }

        if (str instanceof SIRContainer)
            ((SIRContainer)str).reclaimChildren();
        
	//if we are gathering statistics, clone the original stream graph 
	//so that we can gather statictics on it, not on the modified graph
	if (KjcOptions.stats) {
	    origSTR = 
		(SIRStream)ObjectDeepCloner.deepCopy(str);
	}

        // splitjoin optimization on SIR graph can not be
        // done after fusion, and should not affect fusable
        // pipelines, so do it here.
        Lifter.liftAggressiveSync(str);
        
 
        if (KjcOptions.fusion || KjcOptions.dup >= 1 || KjcOptions.noswpipe) {
            // if we are about to fuse filters, we should perform
            // any vectorization now, since vectorization can not work inside
            // fused sections, and vectorization should map pipelines of 
            // stateless filters to pipelines of stateless filters.

            SimplifyPopPeekPush.simplify(str);
            VectorizeEnable.vectorizeEnable(str,null);
        }
        
        //fuse entire str to one filter if possible
        if (KjcOptions.fusion)
            str = FuseAll.fuse(str);
        
        StreamlinedDuplicate duplicate = null;
        /*
        if (!KjcOptions.noswpipe) {
            DuplicateBottleneck.duplicateHeavyFilters(str);
        }
        */
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        work.printGraph(str, "work_estimate.dot");
        WorkList workList = work.getSortedFilterWork();
        for (int i = 0; i < workList.size(); i++) {
            SIRFilter filter = workList.getFilter(i);
            int filterWork = work.getWork(filter); 
            System.out.println("Sorted Work " + i + ": " + filter + " work " 
                    + filterWork + ", is fissable: " + StatelessDuplicate.isFissable(filter));
        }
        
        //for right now, we use the dup parameter to specify the type 
        //of data-parallelization we are using
        //if we want to enable the data-parallelization
        //stuff from asplos 06, use dup == 1
        if (KjcOptions.dup == 1) {
            DuplicateBottleneck dup = new DuplicateBottleneck();
            dup.percentStateless(str);
            str = FusePipelines.fusePipelinesOfStatelessStreams(str);
            StreamItDot.printGraph(str, "after-fuse-stateless.dot");
            dup.smarterDuplicate(str);
            StreamItDot.printGraph(str, "after-data-par.dot");
        } 
        else if (KjcOptions.dup == rawChip.getTotalTiles()) {
            //if we want to use fine-grained parallelization
            //then set dup to be the number of tiles (cores)
            DuplicateBottleneck dup = new DuplicateBottleneck();
            System.out.println("Fine-Grained Data Parallelism...");
            dup.duplicateFilters(str, rawChip.getTotalTiles());
        }

        //if we are no software pipelining, then fuse pipelines of 
        //filters so that the communicate using on-chip tile memory
        if (KjcOptions.noswpipe)
            str = FusePipelines.fusePipelinesOfFilters(str);
        
        StreamItDot.printGraph(str, "canonical-graph.dot");

        //Use the partition_greedier flag to enable the Granularity Adjustment
        //phase, it will try to partition more as long as the critical path
        //is not affected (see asplos 06).
        if (KjcOptions.partition_greedier) {
            StreamItDot.printGraph(str, "before-granularity-adjust.dot");
            str = GranularityAdjust.doit(str, numCores);
            StreamItDot.printGraph(str, "after-granularity-adjust.dot");
        }
        
        /*
        KjcOptions.partition_dp = true;
        str = at.dms.kjc.sir.lowering.partition.Partitioner.doit(str,
                16, true, false, true);
        KjcOptions.partition_dp = false;
        */
        
        
        if (KjcOptions.fission > 1) {
            str = Flattener.doLinearAnalysis(str);
            str = Flattener.doStateSpaceAnalysis(str);

            System.out.println("Running Vertical Fission...");
            FissionReplacer.doit(str, KjcOptions.fission);
            Lifter.lift(str);
            System.out.println("Done Vertical Fission...");
        }
        
      
        // run user-defined transformations if enabled
        if (KjcOptions.optfile != null) {
            System.err.println("Running User-Defined Transformations...");
            str = ManualPartition.doit(str);
            System.err.println("Done User-Defined Transformations...");
            RemoveMultiPops.doit(str);
        }
    
        StaticsProp.propagateIntoFilters(str,theStatics);

        if (KjcOptions.sjtopipe) {
            SJToPipe.doit(str);
        }

        StreamItDot.printGraph(str, "before-partition.dot");

        // str = Partitioner.doit(str, 32);

        // VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(str);

        // VarDecl Raise to move peek index up so
        // constant prop propagates the peek buffer index
        new VarDeclRaiser().raiseVars(str);

        // this must be run now, other pass rely on it...
        RenameAll.renameOverAllFilters(str);

        // SIRPrinter printer1 = new SIRPrinter();
        // IterFactory.createFactory().createIter(str).accept(printer1);
        // printer1.close();

        // Linear Analysis
        LinearAnalyzer lfa = null;
        if (KjcOptions.linearanalysis || KjcOptions.linearpartition) {
            System.out.print("Running linear analysis...");
            lfa = LinearAnalyzer.findLinearFilters(str, KjcOptions.debug, true);
            System.out.println("Done");
            LinearDot.printGraph(str, "linear.dot", lfa);
            LinearDotSimple.printGraph(str, "linear-simple.dot", lfa, null);
            // IterFactory.createFactory().createIter(str).accept(new
            // LinearPreprocessor(lfa));

            // if we are supposed to transform the graph
            // by replacing work functions with their linear forms, do so now
            if (KjcOptions.linearreplacement) {
                System.err.print("Running linear replacement...");
                LinearDirectReplacer.doReplace(lfa, str);
                System.err.println("done.");
                // print out the stream graph after linear replacement
                LinearDot.printGraph(str, "linear-replace.dot", lfa);
            }
        }

        if (KjcOptions.raw >= 0) {
            // make sure SIRPopExpression's only pop one element
            // code generation doesn't handle generating multiple pops
            // from a single SIRPopExpression
            RemoveMultiPops.doit(str);

            //On RAW, lonely pops are converted into a statement with only a register read
            //then they are optimized out by gcc, so convert lonely pops (pops unnested in
            //a larger expression) into an assignment of the pop to a dummy variable
            new ConvertLonelyPops().convertGraph(str);
        }
        
        // We require that no FileReader directly precede a splitter and
        // no joiner directly precede a FileWriter.
	//        System.err.println("Before SafeFileReaderWriterPositions"); 
	//        SIRToStreamIt.run(str,new JInterfaceDeclaration[]{}, new SIRInterfaceTable[]{}, new SIRStructure[]{});
        //SafeFileReaderWriterPositions.doit(str);
	//        System.err.println("After SafeFileReaderWriterPositions");
	//        SIRToStreamIt.run(str,new JInterfaceDeclaration[]{}, new SIRInterfaceTable[]{}, new SIRStructure[]{});
        
        // make sure that push expressions do not contains pop expressions
        // because if they do and we use the gdn, we will generate the header 
        // for the pop expression before the push expression and that may cause 
        // deadlock...
        //at.dms.kjc.common.SeparatePushPop.doit(str);
        
        // Raise all pushes, pops, peeks to statement level
        // (several phases above introduce new peeks, pops, pushes
        //  including but not limited to doLinearAnalysis)
        // needed before vectorization
        SimplifyPopPeekPush.simplify(str);

        // If vectorization enabled, create (fused streams of) vectorized filters.
        // the top level compile script should not allow vectorization to be enabled
        // for processor types that do not support short vectors. 
        VectorizeEnable.vectorizeEnable(str,null);
        
        
        /*KjcOptions.partition_dp = true;
        SIRStream partitionedStr = (SIRStream)ObjectDeepCloner.deepCopy(str);
            partitionedStr = at.dms.kjc.sir.lowering.partition.Partitioner.doit(partitionedStr,
                numCores, true, false, true);
        KjcOptions.partition_dp = false;
        */
        
//        if (KjcOptions.standalone) {
//            // If standalone output desired, then (for now)
//            // fuse all filters in a single appearance schedule.
//            // Partitioning, etc, will be a no-op since there will only
//            // be one filter, but we will still have a slice coming out
//            // so that the code emitter for standalone will work on the 
//            // same representation as the code emitter for RAW.
//            
//            // TODO: eventually do not want to fuse here so can take advantage 
//            // of cache partitioner, more flexible scheduling, etc. 
//            
//            
//            // fuse all into a single filter (precludes support for helper functions,
//            // messaging, dynamic rates, most scheduling algorithms...)
//            at.dms.kjc.rstream.ConvertFileFilters.doit(str);
//            str = FuseAll.fuse(str);
//        }
        
        // get the execution counts from the scheduler
        HashMap[] executionCounts = SIRScheduler.getExecutionCounts(str);

        WorkEstimate workEstimate = WorkEstimate.getWorkEstimate(str); 
        
        greedyBinPacking = new GreedyBinPacking(str, numCores, 
                workEstimate);
        greedyBinPacking.pack();
        
        double CCRatio = CompCommRatio.ratio(str, workEstimate,
                executionCounts[1]);
        
        System.out.println("Comp/Comm Ratio of SIR graph: " + 
                CCRatio);
       
        new CalculateParams(str, CCRatio, executionCounts[1]).doit();
      
        //Util.printExecutionCount(executionCounts[0]);
        //Util.printExecutionCount(executionCounts[1]);
        
        // flatten the graph by running (super?) synch removal
        UnflatFilter[] topNodes = null;
        if (!KjcOptions.nopartition) {
            FlattenGraph.flattenGraph(str, lfa, executionCounts);
            topNodes = FlattenGraph.getTopLevelNodes();
            CommonUtils.println_debugging("Top Nodes:");
            for (int i = 0; i < topNodes.length; i++)
                CommonUtils.println_debugging(topNodes[i].toString());
        }    
        //Slice[] traces = null;
        Slice[] traceGraph = null; 
        
        
        Partitioner partitioner = null;
        if (KjcOptions.autoparams) {
            partitioner = new AdaptivePartitioner(topNodes,
                    executionCounts, lfa, workEstimate, numCores, 
                    getGreedyBinPacking());
        } if (KjcOptions.nopartition) {
            partitioner = new FlattenAndPartition(topNodes,
                    executionCounts, lfa, workEstimate, numCores);
            ((FlattenAndPartition)partitioner).flatten(str, executionCounts);
        }
        else { 
            partitioner = new SimplePartitioner(topNodes,
                    executionCounts, lfa, workEstimate, numCores);
        }

        traceGraph = partitioner.partition();
        System.out.println("Traces: " + traceGraph.length);
        partitioner.dumpGraph("traces.dot");
/////////////////////////////////////////////////////////////////////
        if (KjcOptions.raw >= 0) {
            // Compiling for RAW: 
            
            //We have to create multilevel splits and/or joins if their width
            //is greater than the number of memories of the chip...
            new MultiLevelSplitsJoins(partitioner, rawChip).doit();
            partitioner.dumpGraph("traces-after-multi.dot");
        }
        
        /*
         * System.gc(); System.out.println("MEM:
         * "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
         */
        /*
        StreaMITMain.clearParams();
        FlattenGraph.clear();
        AutoCloner.clear();
        SIRContainer.destroy();
        UnflatEdge.clear();
        str = null;
        interfaces = null;
        interfaceTables = null;
        structs = null;
        structures = null;
        lfa = null;
        executionCounts = null;
        topNodes = null;
        System.gc();
        */
        // ----------------------- This Is The Line -----------------------
        // No Structure, No SIRStreams, Old Stuff Restricted Past This Point
        // Violators Will Be Garbage Collected

        
        //COMP_COMM_RATIO = CompCommRatio.ratio(partitioner);
        
        System.out.println("Multiplying Steady-State...");
        MultiplySteadyState.doit(partitioner.getSliceGraph());
     
        //we can now use filter infos, everything is set
        FilterInfo.canUse();
        
        //create the space/time schedule object to be filled in by the passes 
        SpaceTimeSchedule spaceTimeSchedule = new SpaceTimeSchedule(partitioner, rawChip);
        //check to see if we need to add any buffering before splitters or joiners
        //for correct execution of the init stage and steady state
        if (KjcOptions.raw > 0) {
            AddBuffering.doit(spaceTimeSchedule);
        }
        
        //generate the schedule modeling values for each filter/slice 
        partitioner.calculateWorkStats();
        
        if (KjcOptions.stats) {
            BenchChar.doit(spaceTimeSchedule, origSTR);
        }
        
        //create the layout for each stage of the execution using simulated annealing
        //or manual
        Layout layout = null;
        if (KjcOptions.raw > 0) {
            if (KjcOptions.noswpipe) {
                layout = new NoSWPipeLayout(spaceTimeSchedule);
            } else if (KjcOptions.manuallayout) {
                layout = new ManualSliceLayout(spaceTimeSchedule);
            } else if (KjcOptions.greedysched || (KjcOptions.dup > 1)) {
                layout = new GreedyLayout(spaceTimeSchedule, rawChip);
                //layout = new AnnealedGreedyLayout(spaceTimeSchedule, rawChip, duplicate);
            } else {
                layout = new AnnealedLayout(spaceTimeSchedule);
            }
        } else {
            // for non-raw, just use greedy layout.
            spaceTimeSchedule.getPartitioner().ensureSimpleSlices();
            layout = new BasicGreedyLayout(spaceTimeSchedule, computeNodes);
        }

        layout.run();

        System.out.println("Space/Time Scheduling Steady-State...");
// GenerateSteadyStateSchedule seems to have been cut back to no more than
// BasicGenerateSteadyStateSchedule
// plus an un-called method.
//        if (KjcOptions.raw > 0) {
//            GenerateSteadyStateSchedule spaceTimeScheduler = new GenerateSteadyStateSchedule(
//                    spaceTimeSchedule, layout);
//            spaceTimeScheduler.schedule();
//        } else {
            BasicGenerateSteadyStateSchedule spaceTimeScheduler = new BasicGenerateSteadyStateSchedule(
                    spaceTimeSchedule, partitioner);
            spaceTimeScheduler.schedule();
//        }
  
        /*
        if (partitioner instanceof AdaptivePartitioner &&
                ((AdaptivePartitioner)partitioner).useSpace
                (partitionedStr, spaceTimeSchedule, layout)) { 
            System.out.println("\n\n USE SPACE!");
            //System.exit(0);
        }
        */
                
        //calculate preloop and initialization code
        System.out.println("Creating Initialization Schedule...");
        spaceTimeSchedule.setInitSchedule(DataFlowOrder.getTraversal(spaceTimeSchedule.getPartitioner().getSliceGraph()));
        
        System.out.println("Creating Pre-Loop Schedule...");
        GeneratePrimePumpSchedule preLoopSched = new GeneratePrimePumpSchedule(spaceTimeSchedule);
        preLoopSched.schedule(spaceTimeSchedule.getPartitioner().getSliceGraph());
        
        //System.out.println("Assigning Buffers to DRAMs...");
        //new BufferDRAMAssignment().run(spaceTimeSchedule);
        //ManualDRAMPortAssignment.run(spaceTimeSchedule);
        
        //set the rotation lengths of the buffers
        OffChipBuffer.setRotationLengths(spaceTimeSchedule);

        if (KjcOptions.standalone) {
            EmitStandaloneCode.emitForSingleSlice(partitioner.getSliceGraph());

            System.exit(0);
        }
        

        //communicate the addresses for the off-chip buffers && set up
        // the rotating buffers based on the preloopschedule for software pipelining
//        if (!KjcOptions.magicdram) {
            CommunicateAddrs.doit(rawChip, spaceTimeSchedule);
//        }
 
        //dump some dot graphs!
        SliceDotGraph.dumpGraph(spaceTimeSchedule, spaceTimeSchedule.getInitSchedule(), 
                                "initTraces.dot", layout, true);
        SliceDotGraph.dumpGraph(spaceTimeSchedule, spaceTimeSchedule.getSchedule(), 
                "steadyTraces.dot", layout, true);
        SliceDotGraph.dumpGraph(spaceTimeSchedule, spaceTimeSchedule.getSchedule(), 
                "steadyTraces.nolabel.dot", layout, true, false);
        
        //dump the POV representation of the schedule
        (new POVRAYScheduleRep(spaceTimeSchedule, layout, "schedule.pov")).create(); 
        
        //create the raw execution code and switch code for the initialization
        // phase and the primepump stage and the steady state
        System.out.println("Creating Raw Code...");
        Rawify.run(spaceTimeSchedule, rawChip, layout); 
        
        // generate the switch code assembly files...
        GenerateSwitchCode.run(rawChip);
        // generate the compute code from the SIR
        GenerateComputeCode.run(rawChip);
        // generate the magic dram code if enabled
//        if (KjcOptions.magicdram) {
//            MagicDram.GenerateCode(rawChip);
//        }
        //dump the layout
        LayoutDot.dumpLayout(spaceTimeSchedule, rawChip, "layout.dot");
        
        Makefile.generate(rawChip);
        
        // generate the bc file depending on if we have number gathering enabled
        if (KjcOptions.numbers > 0)
            BCFile.generate(spaceTimeSchedule, rawChip, 
                    NumberGathering.doit(rawChip, partitioner.io));
        else
            BCFile.generate(spaceTimeSchedule, rawChip, null);
        
        System.exit(0);
        
        
        /*
          System.out.println("Scheduling Traces...");
          SimpleScheduler scheduler = new SimpleScheduler(partitioner, rawChip);
          scheduler.schedule();
        
          System.out.println("Calculating Prime Pump Schedule...");
          SchedulePrimePump.doit(scheduler);
          System.out.println("Finished Calculating Prime Pump Schedule.");

          assert !KjcOptions.magicdram : "Magic DRAM support is not working";

          // we can now use filter infos, everything is set
          FilterInfo.canUse();
          System.out.println("Dumping preDRAMsteady.dot...");

          TraceDotGraph.dumpGraph(scheduler.getSchedule(), partitioner.io,
          "preDRAMsteady.dot", false, rawChip, partitioner);
          System.out.println("Assigning Buffers to DRAMs...");
          // assign the buffers not assigned by Jasp to drams
          BufferDRAMAssignment.run(scheduler.getSchedule(), rawChip,
          partitioner.io);
          // communicate the addresses for the off-chip buffers
          if (!KjcOptions.magicdram) {
          // so right now, this pass does not communicate addresses
          // but it generates the declarations of the buffers
          // on the corresponding tile.
          CommunicateAddrs.doit(rawChip);
          }
          TraceDotGraph.dumpGraph(scheduler.getInitSchedule(), partitioner.io,
          "inittraces.dot", true, rawChip, partitioner);
          TraceDotGraph.dumpGraph(scheduler.getSchedule(), partitioner.io,
          "steadyforrest.dot", true, rawChip, partitioner);
    
       
        */
    }
    
    /**
     * @return the RawChip
     */
    public static RawChip getRawChip() {
        return rawChip;
    }

    /**
     * @return the structures
     */
    public static SIRStructure[] getStructures() {
        return structures;
    }

    /**
     * @return the COMP_COMM_RATIO
     */
    public static double getCOMP_COMM_RATIO() {
        return COMP_COMM_RATIO;
    }

    /**
     * @return the FILTER_DEBUG_MODE
     */
    public static boolean isFILTER_DEBUG_MODE() {
        return FILTER_DEBUG_MODE;
    }

    /**
     * @return the greedyBinPacking
     */
    public static GreedyBinPacking getGreedyBinPacking() {
        return greedyBinPacking;
    }

    /**
     * @return the nO_SWPIPELINE
     */
    public static boolean isNO_SWPIPELINE() {
        return NO_SWPIPELINE;
    }
}

// /*

// //software pipeline
// //
// traceForrest=Schedule2Dependencies.findDependencies(spSched,traces,rawRows,rawColumns);
// //SoftwarePipeline.pipeline(spSched,traces,io);
// //for(int i=0;i<traces.length;i++)
// traces[i].doneDependencies();
// System.err.println("TopNodes in Forest: "+traceForrest.length);
// traceForrest=PruneTopTraces.prune(traceForrest);
// System.err.println("TopNodes in Forest: "+traceForrest.length);

// //traceForrest[0] = traces[0];
// /*if(false&&REAL) {
// //System.out.println("TracesGraph: "+traceGraph.length);
// //for(int i=0;i<traceGraph.length;i++)
// //System.out.println(traceGraph[i]);
// traces=traceGraph;
// int index=0;
// traceForrest[0]=traceGraph[0];
// Slice realTrace=traceGraph[0];
// while(((FilterTraceNode)realTrace.getHead().getNext()).isPredefined())
// realTrace=traceGraph[++index];
// TraceNode node=realTrace.getHead();
// FilterTraceNode currentNode=null;
// if(node instanceof InputTraceNode)
// currentNode=(FilterTraceNode)node.getNext();
// else
// currentNode=(FilterTraceNode)node;
// currentNode.setXY(0,0);
// System.out.println("SETTING: "+currentNode+" (0,0)");
// int curX=1;
// int curY=0;
// int forward=1;
// int downward=1;
// //ArrayList traceList=new ArrayList();
// //traceList.add(new Slice(currentNode));
// TraceNode nextNode=currentNode.getNext();
// while(nextNode!=null&&nextNode instanceof FilterTraceNode) {
// currentNode=(FilterTraceNode)nextNode;
// System.out.println("SETTING: "+nextNode+" ("+curX+","+curY+")");
// currentNode.setXY(curX,curY);
// if(curX>=rawColumns-1&&forward>0) {
// forward=-1;
// curY+=downward;
// } else if(curX<=0&&forward<0) {
// forward=1;
// if(curY==0)
// downward=1;
// if(curY==rawRows-1)
// downward=-1;
// if((curY==0)||(curY==rawRows-1)) {
// } else
// curY+=downward;
// } else
// curX+=forward;
// nextNode=currentNode.getNext();
// }
// //traces=new Slice[traceList.size()];
// //traceList.toArray(traces);
// for(int i=1;i<traces.length;i++) {
// traces[i-1].setEdges(new Slice[]{traces[i]});
// traces[i].setDepends(new Slice[]{traces[i-1]});
// }
// //System.out.println(traceList);
// } else */

// if(false&&REAL) {
// len=traceGraph.length;
// newLen=len;
// for(int i=0;i<len;i++)
// if(((FilterTraceNode)traceGraph[i].getHead().getNext()).isPredefined())
// newLen--;
// traces=new Slice[newLen];
// io=new Slice[len-newLen];
// idx=0;
// idx2=0;
// for(int i=0;i<len;i++) {
// Slice trace=traceGraph[i];
// if(!((FilterTraceNode)trace.getHead().getNext()).isPredefined())
// traces[idx++]=trace;
// else
// io[idx2++]=trace;
// }
// System.out.println("Traces: "+traces.length);
// for(int i=0;i<traces.length;i++)
// System.out.println(traces[i]);
// SpaceTimeSchedule sched=TestLayout.layout(traces,rawRows,rawColumns);
// traceForrest=Schedule2Dependencies.findDependencies(sched,traces,rawRows,rawColumns);
// SoftwarePipeline.pipeline(sched,traces,io);
// for(int i=0;i<traces.length;i++)
// traces[i].doneDependencies();
// System.err.println("TopNodes in Forest: "+traceForrest.length);
// traceForrest=PruneTopTraces.prune(traceForrest);
// System.err.println("TopNodes in Forest: "+traceForrest.length);
// }
// */

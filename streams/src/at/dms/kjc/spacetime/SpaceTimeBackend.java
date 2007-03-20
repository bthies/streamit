package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.NoSWPipeLayout;
import at.dms.kjc.common.ConvertLonelyPops;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.backendSupport.CommonPasses;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.Slice;
import java.util.*;

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

    /** Saves output of StaticsProp until time to clean up ComputeCodeStore's */
    public static Map<String,Set<String>> prefixAssociations;
    
    
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
        
        //set number of columns/rows
        int rawRows;
        int rawColumns;
        int numCores;
        
        rawRows = KjcOptions.raw;
        if(KjcOptions.rawcol > -1)
            rawColumns = KjcOptions.rawcol;
        else
            rawColumns = KjcOptions.raw;
        
        numCores = rawRows * rawColumns;

        // Perform all standard optimization passes on SIR graph 
        // and convert into a Slice graph.
        CommonPasses commonPasses = new CommonPasses();
        Slice[] sliceGraph = commonPasses.run(str, interfaces, 
                interfaceTables, structs, helpers, global, numCores);
        prefixAssociations = commonPasses.getAssociatedGlobals();
        
        
        // do RAW-specific stuff with pops:
        // convert all multiple pops sequences of pops.
        // convert all pops that do not return values to writes to a volatile.
        ConvertLonelyPops convertLonelyPops = new ConvertLonelyPops();
        for (Slice slice : sliceGraph) {
            for (FilterSliceNode filterNode : slice.getFilterNodes()) {
                for (JMethodDeclaration method : filterNode.getFilter().getMethods()) {
                    RemoveMultiPops.doit(method);
                    convertLonelyPops.convert(method, filterNode.getFilter().getInputType());
                }
            }
        }
        
        Partitioner partitioner = commonPasses.getPartitioner();

        //create the RawChip
        rawChip = new RawChip(rawColumns, rawRows);

        //We have to create multilevel splits and/or joins if their width
        //is greater than the number of memories of the chip...
        new MultiLevelSplitsJoins(partitioner, rawChip).doit();
        partitioner.dumpGraph("traces-after-multi.dot");
        
        /*
         * System.gc(); System.out.println("MEM:
         * "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
         */
        /*
        commonPasses = null;
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
        if (KjcOptions.noswpipe) {
            layout = new NoSWPipeLayout<RawTile,RawChip>(spaceTimeSchedule, rawChip);
        } else if (KjcOptions.manuallayout) {
            layout = new ManualSliceLayout(spaceTimeSchedule);
        } else if (KjcOptions.greedysched || (KjcOptions.dup > 1)) {
            layout = new GreedyLayout(spaceTimeSchedule, rawChip);
            // layout = new AnnealedGreedyLayout(spaceTimeSchedule, rawChip,
            // duplicate);
        } else {
            layout = new AnnealedLayout(spaceTimeSchedule);
        }
        layout.run();
        new BufferDRAMAssignment().run(spaceTimeSchedule, layout);
        
        
        System.out.println("Space/Time Scheduling Steady-State...");
        BasicGenerateSteadyStateSchedule spaceTimeScheduler = new BasicGenerateSteadyStateSchedule(
                spaceTimeSchedule, partitioner);
        spaceTimeScheduler.schedule();
  
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

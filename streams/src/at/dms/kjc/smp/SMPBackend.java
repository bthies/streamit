package at.dms.kjc.smp;

import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.spacetime.*;
import java.util.*;

public class SMPBackend {
    public static final boolean FAKE_IO = false;

    public static Scheduler scheduler;
    public static SMPMachine chip;
    public static SMPBackEndFactory backEndBits;
    public static Structs_h structs_h;
    public static int[] coreOrder = {0, 8, 1, 9, 2, 10, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};

    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[]structs,
                           SIRHelper[] helpers,
                           SIRGlobal global) {
    	System.out.println("Entry to SMP Backend...");

    	checkArguments();
    	setScheduler();
    	
    	// create cores in desired amount and order
    	int[] cores = new int[KjcOptions.smp];
    	for (int x = 0 ; x < KjcOptions.smp ; x++)
    		cores[x] = coreOrder[x];
        chip = new SMPMachine(cores);
        
        // create a new structs.h file for typedefs etc.
        structs_h = new Structs_h();

        // The usual optimizations and transformation to slice graph
        CommonPasses commonPasses = new CommonPasses();
        // perform standard optimizations, use the number of cores the user wants to target
        commonPasses.run(str, interfaces, interfaceTables, structs, helpers, global, chip.size());
        // perform some standard cleanup on the slice graph.
        commonPasses.simplifySlices();
        // dump slice graph to dot file
        commonPasses.getSlicer().dumpGraph("traces.dot", null);
        
        // partition the slice graph based on the scheduling policy
        SpaceTimeScheduleAndSlicer graphSchedule = new SpaceTimeScheduleAndSlicer(commonPasses.getSlicer());
        scheduler.setGraphSchedule(graphSchedule);
        scheduler.run(chip.size());
        FilterInfo.reset();
        
        // generate schedules for initialization, primepump and steady-state
        scheduleSlices(graphSchedule);

        // generate layout for filters
        scheduler.runLayout();

        // dump final slice graph to dot file
        graphSchedule.getSlicer().dumpGraph("after_slice_partition.dot", scheduler);
        graphSchedule.getSlicer().dumpGraph("slice_graph.dot", scheduler, false);

        // if load balancing, find candidiate fission groups to load balance
        if(KjcOptions.loadbalance) {
            LoadBalancer.findCandidates();
            LoadBalancer.instrumentMainMethods();
        }
        
        // create all buffers and set the rotation lengths
        RotatingBuffer.createBuffers(graphSchedule);
	        
        // now convert to Kopi code plus communication commands
        backEndBits = new SMPBackEndFactory(chip, scheduler);
        backEndBits.getBackEndMain().run(graphSchedule, backEndBits);
	
	//generate code for file writer
	CoreCodeStore.generatePrintOutputCode();
      
	if (KjcOptions.numbers > 0)
            chip.getNthComputeNode(0).getComputeCode().generateNumbersCode();
        
        // emit c code for all cores
        EmitSMPCode.doit(backEndBits);
        
        // dump structs.h file
        structs_h.writeToFile();

        // display final assignment of filters to cores
        System.out.println("Final filter assignments:");
        System.out.println("========================================");
        for(int x = 0 ; x < KjcOptions.smp ; x++) {
            Core core = chip.getNthComputeNode(x);
            Set<FilterSliceNode> filters = core.getComputeCode().getFilters();
            long totalWork = 0;

            System.out.println("Core " + core.getCoreID() + ": ");
            for(FilterSliceNode filter : filters) {
                long work = SliceWorkEstimate.getWork(filter.getParent());
                System.out.format("%16d | " + filter + "\n", work);
                totalWork += work;
            }
            System.out.format("%16d | Total\n", totalWork);
        }

        // calculate computation to communication ratio
        if(KjcOptions.sharedbufs) {
            LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());
            HashSet<Slice> compProcessed = new HashSet<Slice>();
            HashSet<Slice> commProcessed = new HashSet<Slice>();
            
            long comp = 0;
            long comm = 0;
            
            for(Slice slice : slices) {
                if(compProcessed.contains(slice))
                    continue;
                
                comp += SliceWorkEstimate.getWork(slice);
                compProcessed.add(slice);
            }

            /*
            for(Slice slice : slices) {
                if(commProcessed.contains(slice))
                    continue;
                
                FilterInfo info = FilterInfo.getFilterInfo(slice.getFirstFilter());
                int totalItemsReceived = info.totalItemsReceived(SchedulingPhase.STEADY);

                if(totalItemsReceived == 0)
                    continue;

                InputSliceNode input = slice.getHead();
                Set<InterSliceEdge> sources = input.getSourceSet(SchedulingPhase.STEADY);
                int numInputRots = totalItemsReceived / input.totalWeights(SchedulingPhase.STEADY);

                if(!FissionGroupStore.isFizzed(slice)) {
                    for(InterSliceEdge source : sources) {
                        Slice srcSlice = source.getSrc().getParent();

//                         if(srcSlice.getFirstFilter().isFileInput())
//                             continue;

                        if(FissionGroupStore.isFizzed(srcSlice)) {
                            // Filter is not fizzed, source is fizzed
                            // Filter must receive (N-1)/N of inputs from different cores
                            comm += numInputRots * 
                                input.getWeight(source, SchedulingPhase.STEADY) / 
                                KjcOptions.smp * (KjcOptions.smp - 1);
                        }
                        else {
                            // Filter is not fizzed, source is not fizzed
                            // Check to see if on same core
                            // If not, must communicate all elements
                            if(!scheduler.getComputeNode(slice.getFirstFilter()).equals(
                                   scheduler.getComputeNode(srcSlice.getFirstFilter()))) {
                                comm += numInputRots * 
                                    input.getWeight(source, SchedulingPhase.STEADY);
                            }
                        }
                    }
                }
                else {
                    for(InterSliceEdge source : sources) {
                        Slice srcSlice = source.getSrc().getParent();

//                         if(srcSlice.getFirstFilter().isFileInput())
//                             continue;

                        if(FissionGroupStore.isFizzed(srcSlice)) {
                            // Filter is fizzed, source is also fizzed
                            int totalItemsReceivedPerFizzed = totalItemsReceived /
                                FissionGroupStore.getFissionGroup(slice).fizzedSlices.length;
                            int numInputRotsPerFizzed = numInputRots /
                                FissionGroupStore.getFissionGroup(slice).fizzedSlices.length;

                            System.out.println("totalItemsReceivedPerFizzed: " + totalItemsReceivedPerFizzed);
                            System.out.println("numInputRotsPerFizzed: " + numInputRotsPerFizzed);

                            int inputWeightBeforeSrc = 
                                input.weightBefore(source, SchedulingPhase.STEADY);
                            int inputWeightSrc = input.getWeight(source, SchedulingPhase.STEADY);
                            int inputTotalWeight = input.totalWeights(SchedulingPhase.STEADY);

                            System.out.println("inputWeightBeforeSrc: " + inputWeightBeforeSrc);
                            System.out.println("inputWeightSrc: " + inputWeightSrc);
                            System.out.println("copyDown: " + info.copyDown);

                            int numXmit = 0;

                            for(int rot = 0 ; rot < numInputRotsPerFizzed ; rot++) {
                                numXmit += Math.min(inputWeightSrc,
                                                    Math.max(0,
                                                             info.copyDown +
                                                             rot * inputTotalWeight + 
                                                             inputWeightBeforeSrc + inputWeightSrc -
                                                             totalItemsReceivedPerFizzed));
                            }

                            System.out.println("numXmit: " + numXmit);

                            comm += KjcOptions.smp * numXmit;
                        }
                        else {
                            // Filter is fizzed, source is not fizzed
                            // Source must send (N-1)/N of outputs to different cores
                            comm += numInputRots *
                                input.getWeight(source, SchedulingPhase.STEADY) /
                                KjcOptions.smp * (KjcOptions.smp - 1);
                        }
                    }
                }

                commProcessed.add(slice);
            }
            */
            
            // Simple communication estimation
            for(Slice slice : slices) {
                if(commProcessed.contains(slice))
                    continue;
                
                FilterInfo info = FilterInfo.getFilterInfo(slice.getFirstFilter());
                int totalItemsReceived = info.totalItemsReceived(SchedulingPhase.STEADY);

                if(totalItemsReceived == 0)
                    continue;

                comm += totalItemsReceived;

                if(FissionGroupStore.isFizzed(slice)) {
                    assert info.peek >= info.pop;
                    comm += (info.peek - info.pop) * KjcOptions.smp;
                }

                commProcessed.add(slice);
            }

            // Simple communication estimation 2
            /*
            for(Slice slice : slices) {
                if(commProcessed.contains(slice))
                    continue;
                
                FilterInfo info = FilterInfo.getFilterInfo(slice.getFirstFilter());
                int totalItemsReceived = info.totalItemsReceived(SchedulingPhase.STEADY);
                int totalItemsSent = info.totalItemsSent(SchedulingPhase.STEADY);

                comm += totalItemsReceived;
                comm += totalItemsSent;

                if(totalItemsReceived == 0)
                    continue;

                if(FissionGroupStore.isFizzed(slice)) {
                    assert info.peek >= info.pop;
                    comm += (info.peek - info.pop) * KjcOptions.smp;
                }

                commProcessed.add(slice);
            }
            */

            System.out.println("Final Computation: " + comp);
            System.out.println("Final Communication: " + comm);
            System.out.println("Final Comp/Comm Ratio: " + (float)comp/(float)comm);
        }

        System.exit(0);
    }
    
    /**
     * Check arguments to backend to make sure that they are valid
     */
    private static void checkArguments() {
        // if debugging and number of iterations unspecified, limit number of iterations
        if(KjcOptions.debug && KjcOptions.iterations == -1)
            KjcOptions.iterations = 100;

        // if load-balancing, enable shared buffers
        if(KjcOptions.loadbalance)
            KjcOptions.sharedbufs = true;

        // if load-balancing, but only 1 core, disable load-balancing
        if(KjcOptions.loadbalance && KjcOptions.smp == 1)
            KjcOptions.loadbalance = false;

        // if using old TMD, make sure not using sharedbufs since they're incompatible
        if(KjcOptions.partitioner.equals("oldtmd")) {
            if(KjcOptions.sharedbufs) {
                System.out.println("WARNING: Disabling shared buffers due to incompatibility with old TMD scheduler");
                KjcOptions.sharedbufs = false;
            }
        }
    }
    
    /**
     * Set the scheduler field to the correct leaf class that implements a scheduling 
     * policy.
     */
    private static void setScheduler() {
        if (KjcOptions.partitioner.equals("tmd")) {
            scheduler = new TMDBinPackFissAll();
        } else if (KjcOptions.partitioner.equals("oldtmd")) {
            scheduler = new TMD();
        } else if (KjcOptions.partitioner.equals("smd")) {
            scheduler = new SMD();
        } else {
            System.err.println("Unknown Scheduler Type!");
            System.exit(1);
        }
    }
    
    /** 
     * Create schedules for init, prime-pump and steady phases.
     *
     * @return a Scheduler from which the schedules for the phases may be extracted. 
     */
    public static void scheduleSlices(SpaceTimeScheduleAndSlicer schedule) {
        Slicer slicer = schedule.getSlicer();
        
        // set init schedule in standard order
        schedule.setInitSchedule(DataFlowOrder.getTraversal(slicer.getSliceGraph()));
        
        //set the prime pump to be empty
        new GeneratePrimePump(schedule).setEmptySchedule();

        //for space multiplexing on SMP we need to use a different primepump scheduler because
        //we are space multiplexing and we need to prime the pipe more so that everything can fire
        //when ready
        if (at.dms.kjc.smp.SMPBackend.scheduler.isSMD())
            new at.dms.kjc.smp.GeneratePrimePumpScheduleSMD(schedule).schedule(slicer.getSliceGraph());
        else 
            new GeneratePrimePump(schedule).schedule(slicer.getSliceGraph());

        //Still need to generate the steady state schedule!
        schedule.setSchedule(DataFlowOrder.getTraversal(slicer.getTopSlices()));
    }
}

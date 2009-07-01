package at.dms.kjc.smp;

import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.slicegraph.*;
import java.util.LinkedList;

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
        
        // create all buffers and set the rotation lengths
        RotatingBuffer.createBuffers(graphSchedule);
	        
        // now convert to Kopi code plus communication commands
        backEndBits = new SMPBackEndFactory(chip);
        backEndBits.setScheduler(scheduler);
        backEndBits.getBackEndMain().run(graphSchedule, backEndBits);
        
        if (KjcOptions.numbers > 0)
            chip.getNthComputeNode(0).getComputeCode().generateNumbersCode();
        else
            CoreCodeStore.generatePrintOutputCode();
        
        // emit c code for all cores
        EmitSMPCode.doit(backEndBits);
        
        // dump structs.h file
        structs_h.writeToFile();

        System.exit(0);
    }
    
    /**
     * Check arguments to backend to make sure that they are valid
     */
    private static void checkArguments() {
        // if debugging and number of iterations unspecified, limit number of iterations
        if(KjcOptions.debug && KjcOptions.iterations == -1)
            KjcOptions.iterations = 100;
    }
    
    /**
     * Set the scheduler field to the correct leaf class that implements a scheduling 
     * policy.
     */
    private static void setScheduler() {
        if (KjcOptions.partitioner.equals("tmd")) {
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

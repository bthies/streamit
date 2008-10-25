package at.dms.kjc.tilera;

import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.spacetime.BasicGenerateSteadyStateSchedule;

public class TileraBackend {
    public static Scheduler scheduler;
    public static TileraChip chip;
    public static TileraBackEndFactory backEndBits;
    public static Structs_h structs_h;
    /** if true use DMA otherwise remote writes */
    public static boolean DMA = false;
    
    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[]structs,
                           SIRHelper[] helpers,
                           SIRGlobal global) {
	System.out.println("Entry to Tilera Backend...");
        
	setScheduler();
	//always create a chip with 64 tiles, let layout (Scheduler) worry about smaller chips
        chip = new TileraChip();
        //create a new structs.h file for typedefs etc.
        structs_h = new Structs_h();

        // The usual optimizations and transformation to slice graph
        CommonPasses commonPasses = new CommonPasses();
        // perform standard optimizations, use the number of tiles the user wants to target
        commonPasses.run(str, interfaces, interfaceTables, structs, helpers, global, chip.abstractSize());
        // perform some standard cleanup on the slice graph.
        commonPasses.simplifySlices();
        
        SpaceTimeScheduleAndSlicer graphSchedule = new SpaceTimeScheduleAndSlicer(commonPasses.getSlicer());
        scheduler.setGraphSchedule(graphSchedule);
        
        //partition the slice graph based on the scheduling policy
        scheduler.run(chip.abstractSize());
        FilterInfo.reset();
        
        scheduleSlices(graphSchedule);      
       
        scheduler.runLayout();
        backEndBits = new TileraBackEndFactory(chip);
        backEndBits.setLayout(scheduler);
                
        graphSchedule.getSlicer().dumpGraph("after_slice_partition.dot", scheduler);
        
        //create all buffers and set the rotation lengths
        RotatingBuffer.createBuffers(graphSchedule);
	        
        //now convert to Kopi code plus communication commands.  
        backEndBits.getBackEndMain().run(graphSchedule, backEndBits);
        
        if (KjcOptions.verbose)
            chip.getComputeNode(0, 0).getComputeCode().generatePrintOutputCode();
        else
            chip.getComputeNode(0, 0).getComputeCode().generateNumbersCode();
        
        //emit c code for all tiles
        EmitTileCode.doit(backEndBits);
        
        //dump structs.h file
        structs_h.writeToFile();
        
	System.exit(0);
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

        //for space multiplexing on tilera we need to use a different primepump scheduler because
        //we are space multiplexing and we need to prime the pipe more so that everything can fire
        //when ready
        if (at.dms.kjc.tilera.TileraBackend.scheduler.isSMD())
            new at.dms.kjc.tilera.GeneratePrimePumpScheduleSMD(schedule).schedule(slicer.getSliceGraph());
        else 
            new GeneratePrimePump(schedule).schedule(slicer.getSliceGraph());

        //Still need to generate the steady state schedule!
        schedule.setSchedule(DataFlowOrder.getTraversal(slicer.getTopSlices()));
        
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
}

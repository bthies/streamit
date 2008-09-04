package at.dms.kjc.tilera;

import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.slicegraph.*;

public class TileraBackend {
    public static Scheduler scheduler;
    public static TileraChip chip;
    public static TileraBackEndFactory backEndBits;
    public static Structs_h structs_h;
    
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
        // Set schedules for initialization, prime-pump (if KjcOptions.spacetime), and steady state.
        SpaceTimeScheduleAndSlicer graphSchedule = commonPasses.scheduleSlices();
        scheduler.setGraphSchedule(graphSchedule);
        
        //partition the slice graph based on the scheduling policy
        scheduler.run(chip.abstractSize());

        scheduler.runLayout();
        backEndBits = new TileraBackEndFactory(chip);
        backEndBits.setLayout(scheduler);
        
        //create all buffers and set the rotation lengths
        RotatingBuffer.createBuffers(graphSchedule);
	        
        //now convert to Kopi code plus communication commands.  
        backEndBits.getBackEndMain().run(graphSchedule, backEndBits);
        
        chip.getComputeNode(0, 0).getComputeCode().generateNumbersCode();
        
        //emit c code for all tiles
        EmitTileCode.doit(backEndBits);
        
        //dump structs.h file
        structs_h.writeToFile();
        
	System.exit(0);
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

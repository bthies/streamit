package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

public class Partitioner {
    /**
     * Tries to adjust <str> into <num> pieces of equal work.
     */
    public static void doit(SIRStream str, int target) {
	// dump the orig graph
	StreamItDot.printGraph(str, "before.dot");

	// Lift filters out of pipelines if they're the only thing in
	// the pipe
	System.out.print("Lifting filters... ");
	Lifter.lift(str);
	System.out.println("done.");

	// if we have too few tiles, then fizz the big ones
	System.out.print("count tiles... ");
	int count = new RawFlattener(str).getNumTiles();
	System.out.println("found "+count+" tiles.");

	// for statistics gathering
	if (KjcOptions.dpscaling) {
	    DynamicProgPartitioner.saveScalingStatistics(str, 256);
	}

	// do the partitioning
	if (count < target) {
	    // need fission
	    if (KjcOptions.dppartition) {
		new DynamicProgPartitioner(str, target).toplevelFusion();
	    } else {
		new GreedyPartitioner(str, target).toplevelFission(count);
	    }
	} else {
	    // need fusion
	    if (KjcOptions.ilppartition) {
		new ILPPartitioner(str, target).toplevelFusion();
	    } else if (KjcOptions.dppartition) {
		new DynamicProgPartitioner(str, target).toplevelFusion();
	    } else {
		new GreedyPartitioner(str, target).toplevelFusion();
	    }
	}

	// dump the final graph
	StreamItDot.printGraph(str, "after.dot");
    }
}

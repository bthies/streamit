package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;

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

	// make work estimate
	WorkEstimate work = WorkEstimate.getWorkEstimate(str);
	work.printGraph(str, "work-estimate.dot");

	// detect number of tiles we have
	System.out.print("count tiles... ");
	int count = new RawFlattener(str).getNumTiles();
	System.out.println("found "+count+" tiles.");

	// for statistics gathering
	if (KjcOptions.dpscaling) {
	    DynamicProgPartitioner.saveScalingStatistics(str, work, 256);
	}

	// do the partitioning
	if (count < target) {
	    // need fission
	    if (KjcOptions.dppartition) {
		str = new DynamicProgPartitioner(str, work, target).toplevel();
	    } else {
		new GreedyPartitioner(str, work, target).toplevelFission(count);
	    }
	} else {
	    // need fusion
	    if (KjcOptions.ilppartition) {
		new ILPPartitioner(str, work, target).toplevelFusion();
	    } else if (KjcOptions.dppartition) {
		str = new DynamicProgPartitioner(str, work, target).toplevel();
	    } else {
		new GreedyPartitioner(str, work, target).toplevelFusion();
	    }
	}

	// lift the result
	Lifter.lift(str);

	// dump the final graph
	StreamItDot.printGraph(str, "after.dot");
    }
}

package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import at.dms.kjc.flatgraph.*;

public class Partitioner {
    /**
     * Tries to adjust <str> into <num> pieces of equal work, and
     * return new stream.
     */
    public static SIRStream doit(SIRStream str, int target) {
	// Lift filters out of pipelines if they're the only thing in
	// the pipe
	Lifter.lift(str);

	// make work estimate
	WorkEstimate work = WorkEstimate.getWorkEstimate(str);
	work.printGraph(str, "work-before-partition.dot");
	work.getSortedFilterWork().writeToFile("work-before-partition.txt");

	// detect number of tiles we have
	int count = new GraphFlattener(str).getNumTiles();
	System.err.println("  Found "+count+" tiles.");

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

	// get the final work estimate
	work = WorkEstimate.getWorkEstimate(str);
	work.printGraph(str, "work-after-partition.dot");
	work.getSortedFilterWork().writeToFile("work-after-partition.txt");
	work.printWork();

	return str;
    }
}

package at.dms.kjc.sir.lowering.partition;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import java.lang.reflect.*;

/**
 * Contains interface for manually driving partitioning process.
 */
public class ManualPartition {
    /**
     * Invokes the "manualPartition" method in class specified by
     * KjcOptions.manual.  To be called only by the compiler.
     */
    public static SIRStream doit(SIRStream str) {
	// invoke manual optimization via reflection
	try {
	    Class c = Class.forName(KjcOptions.manual);
	    Method manualPartition = c.getMethod("manualPartition", new Class[] { Class.forName("at.dms.kjc.sir.SIRStream") });
	    Object result = manualPartition.invoke(null, new Object[] { str });
	    if (!(result instanceof SIRStream)) {
		Utils.fail("Manual partitioning failed:  class " + KjcOptions.manual + " did not return an SIRStream.");
		return null;
	    } else {
		return (SIRStream)result;
	    }
	} catch (ClassNotFoundException e) {
	    Utils.fail("Manual partitioning failed:  can't find class " + KjcOptions.manual);
	} catch (NoSuchMethodException e) {
	    Utils.fail("Manual partitioning failed:  class " + KjcOptions.manual + " does not contain appropriate manualPartition method.");
	} catch (InvocationTargetException e) {
	    e.printStackTrace();
	    Utils.fail("Manual partitioning failed:  class " + KjcOptions.manual + " threw exception.");
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	    Utils.fail("Manual partitioning failed:  illegal to access class " + KjcOptions.manual);
	}
	return null;
    }

    /**
     * Outputs numbered dot graph for <str>, by name of
     * <filename>.dot.
     */
    public static void printGraph(SIRStream str, String filename) {
	NumberDot.printGraph(str, filename);
    }

    /**
     * Returns stream contained within <str> that has unique id number
     * <num>.  This is the ID number that appears in numbered graphs.
     */
    public static SIRStream getStream(SIRStream str, int num) {
	return str.getStreamWithNumber(num);
    }

    /**
     * Runs dynamic programming partitioner on <str>, aiming to reduce
     * the number of tiles needed to <targetTiles>.
     */
    public static SIRStream partition(SIRStream str, int targetTiles) {
	return internalPartition(str, targetTiles, true);
    }

    /**
     * Runs greedy partitioner on <str>, aiming to reduce the number
     * of tiles needed to <targetTiles>.
     */
    public static SIRStream partitionGreedy(SIRStream str, int targetTiles) {
	return internalPartition(str, targetTiles, false);
    }

    /**
     * Internal partitioning routine.  If <dp> is true, runs dynamic
     * programming partitioner; otherwise runs greedy partitioner.
     */
    private static SIRStream internalPartition(SIRStream str, int targetTiles, boolean dp) {
	checkNull(str);

	// need to make a wrapper, since DP partitioning does best
	// with pipelines
	str = SIRContainer.makeWrapper(str);

	// Lift filters out of pipelines if they're the only thing in
	// the pipe
	Lifter.lift(str);

	// make work estimate
	WorkEstimate work = WorkEstimate.getWorkEstimate(str);
	int curCount = new GraphFlattener(str).getNumTiles();

	// since we're on Raw, the joiners need tiles
	boolean joinersNeedTiles = true;
	// icode is for cluster
	boolean limitICode = false;

	if (dp) {
	    str = new DynamicProgPartitioner(str, work, targetTiles, joinersNeedTiles, limitICode).toplevel();
	} else {
	    if (curCount < targetTiles) {
		// need fission
		new GreedyPartitioner(str, work, targetTiles, joinersNeedTiles).toplevelFission(curCount);
	    } else {
		// need fusion
		new GreedyPartitioner(str, work, targetTiles, joinersNeedTiles).toplevelFusion();
	    }
	} 

	// lift the result
	Lifter.lift(str);

	// remove wrapper if possible
	if (str instanceof SIRPipeline && ((SIRPipeline)str).size()==1) {
	    str = ((SIRPipeline)str).get(0);
	}

	return str;
    }

    /**
     * Fuses all of <str> into a single filter.
     */
    public static SIRStream fuse(SIRStream str) {
	checkNull(str);
	return internalPartition(str, 1, true);
    }

    /**
     * Fuses some components of a pipeline together.  The components
     * are specified according to a PartitionGroup, <partitions>.
     */
    public static SIRStream fuse(SIRPipeline pipeline, PartitionGroup partitions) {
	checkNull(pipeline);
	return FusePipe.fuse(pipeline, partitions);
    }

    /**
     * Fuses some components of a splitjoin together.  The components
     * are specified according to a PartitionGroup, <partitions>.
     */
    public static SIRStream fuse(SIRSplitJoin splitjoin, PartitionGroup partitions) {
	checkNull(splitjoin);
	return FuseSplit.fuse(splitjoin, partitions);
    }

    /**
     * Returns whether or not <filter> is fissable by the StreamIt
     * compiler.  Currently, we can fiss only "stateless" filters that
     * have no internal fields.
     */
    public static boolean isFissable(SIRFilter filter) {
	checkNull(filter);
	return StatelessDuplicate.isFissable(filter);
    }

    /**
     * Splits <filter> into a <reps>-way splitjoin.  This is
     * essentially converting <filter> to operate in a data-parallel
     * form.  Requires that isFissable(<filter>) is true.
     */
    public static SIRSplitJoin fission(SIRFilter filter, int reps) {
	checkNull(filter);
	return StatelessDuplicate.doit(filter, reps);
    }
    
    /**
     * Returns a new pipeline that is like <pipe> but replaces
     * children at indices first...last with a pipeline that contains
     * those children.
     */
    public static SIRPipeline addHierarchicalChild(SIRPipeline pipe, int first, int last) {
	checkNull(pipe);
	return RefactorPipeline.addHierarchicalChild(pipe, first, last);
    }

    /**
     * Given a pipeline <pipe> and a partitioning <partition> of its
     * children, returns a new pipeline that has all the elements of
     * each partition factored into their own pipelines.
     */
    public static SIRPipeline addHierarchicalChildren(SIRPipeline pipe, PartitionGroup partitions) {
	checkNull(pipe);
	return RefactorPipeline.addHierarchicalChildren(pipe, partitions);
    }

    /**
     * Given a splitjoin <sj> and a partitioning <partition> of its
     * children, returns a new splitjoin with each partition factored
     * into its own child splitjoin.
     */
    public static SIRSplitJoin addHierarchicalChildren(SIRSplitJoin sj, PartitionGroup partition) {
	checkNull(sj);
	SIRSplitJoin result = RefactorSplitJoin.addHierarchicalChildren(sj, partition);
	// need to replace in parent, since method is immutable
	if (sj.getParent()!=null) {
	    sj.getParent().replace(sj, result);
	}
	return result;
    }

    /**
     * Given that all of the children of <sj> are pipelines and that
     * <partition> describes a partitioning for such a pipeline,
     * re-arrange <sj> into a pipeline of several splitjoins, each of
     * which has children corresponding to a segment of <partition>:
     *
     *      |                          |
     *      .                          .
     *    / | \                      / | \ 
     *   |  |  |                     | | |
     *   |  |  |         ===>        \ | /
     *   |  |  |                       .
     *    \ | /                      / | \
     *      .                        | | |
     *      |                        \ | /
     *      |                          .
     */
    public static SIRPipeline addSyncPoints(SIRSplitJoin sj, PartitionGroup partitions) {
	checkNull(sj);
	SIRPipeline result = RefactorSplitJoin.addSyncPoints(sj, partitions);
	// need to replace in parent, since method is immutable
	if (sj.getParent()!=null) {
	    sj.getParent().replace(sj, result);
	}
	return result;
    }

    /**
     * Removes all synchronization points between child splitjoins in
     * <pipe>.  Note that this might INCREASE the tile count because
     * more joiners are introduced into the graph.  If this is not
     * desired, use only removeMatchingSyncPoints (below).
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean removeSyncPoints(SIRPipeline pipe) {
	checkNull(pipe);
	return RefactorSplitJoin.removeSyncPoints(pipe);
    }

    /**
     * Does the opposite transformation of <addSyncPoints> above.  If
     * any two adjacent children in <pipe> are splitjoins where the
     * weights of the upstream joiner exactly match the weights of the
     * downstream joiner, then the splitjoins can be combined into a
     * single splitjoin.  If this is the case, then <pipe> is mutated.
     *
     * This is intended only as a reverse routine for the above
     * sync. addition.  In particular, it doesn't deal with duplicate
     * splitters or 1-way splitters, and it doesn't attempt to
     * "duplicate" or "unroll" whole streams in order for
     * synchronization to match up.
     *
     * This guarantees that the tile count is not increased by the
     * procedure.
     *
     * Returns whether or not any change was made.
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean removeMatchingSyncPoints(SIRPipeline pipe) {
	checkNull(pipe);
	return RefactorSplitJoin.removeMatchingSyncPoints(pipe);
    }

    /**
     * Raises as many children of <sj> as it can into <sj>.  That is,
     * if <sj> contains some children that are also splitjoins, tries
     * to promote the children's children into direct children of
     * <sj>.  Attempts both duplicate splitters and roundrobin
     * splitters.
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean raiseSJChildren(SIRSplitJoin sj) {
	checkNull(sj);
	return RefactorSplitJoin.raiseSJChildren(sj);
    }

    /**
     * Exits with nice error if <str> is null.
     */
    private static void checkNull(SIRStream str) {
	if (str==null) {
	    new RuntimeException("Null stream passed to ManualPartition.  You probably tried to \n" + 
				 "retrieve a numbered stream that is not in the graph.").printStackTrace();
	    System.exit(1);
	}
    }
}

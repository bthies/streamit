import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * Manual partitioning for the BeamFormer program.
 */
public class MyPartition2 {

    public static SIRStream manualPartition(SIRStream str) {

	// pull all the InputGenerate's into their own splitjoin, so
	// that we can fuse them all together
	PartitionGroup partitions = PartitionGroup.createFromArray(new int[] {1, 2});
	// get top-most splitjoin
	SIRSplitJoin sj = (SIRSplitJoin)ManualPartition.getStream(str, 34);
	// add sync point after input generate
	ManualPartition.addSyncPoints(sj, partitions);

	// create new graph so we can get number for the new splitjoin
	// on top
	ManualPartition.printGraph(str, "numbered2.dot");

	// the new SJ is #114 -- collapse it into a single filter
	SIRSplitJoin top = (SIRSplitJoin)ManualPartition.getStream(str, 114);
	ManualPartition.fuse(top);

	// ok, now decrease the rest of the splitjoin to a 6-way
	// splitjoin.  We will fuse groups of 4 filters into a single
	// child.
	partitions = PartitionGroup.createFromArray(new int[] {2,2,2,2,2,2});
	// get the second splitjoin -- #115.  It is now the top one
	top = (SIRSplitJoin)ManualPartition.getStream(str, 115);
	ManualPartition.addHierarchicalChildren(top, partitions);

	// print to graph to make sure we got it
	ManualPartition.printGraph(str, "numbered3.dot");

	// fuse each of these 6 children
	for (int i=136; i<=141; i++) {
	    ManualPartition.fuse(ManualPartition.getStream(str, i));
	}

	// print to graph to make sure we got it
	ManualPartition.printGraph(str, "numbered4.dot");

	// fuse bottom pipelines
	for (int i=89; i<=104; i+=5) {
	    SIRStream s = ManualPartition.getStream(str, i);
	    ManualPartition.fuse(s);
	}

	// print final numbered graph
	ManualPartition.printGraph(str, "numbered5.dot");

	return str;
    }
}

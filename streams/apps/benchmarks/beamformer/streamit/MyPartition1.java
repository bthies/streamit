import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * Manual partitioning for the BeamFormer program.
 */
public class MyPartition1 {

    public static SIRStream manualPartition(SIRStream str) {

        // uncomment this and run to discover which streams have which numbers
        // NumberDot.printGraph(str, "numbered.dot");
        // return str;

	// fuse the top pipelines
	for (int i=135; i<=146; i++) {
	    SIRStream s = ManualPartition.getStream(str, i);
	    ManualPartition.fuse(s);
	}

	// fuse bottom pipelines
	for (int i=148; i<=151; i++) {
	    SIRStream s = ManualPartition.getStream(str, i);
	    ManualPartition.fuse(s);
	}

	// fuse the bottom splitjoin to have 2 children instead of 4
	// children
	PartitionGroup partitions = PartitionGroup.createFromArray(new int[] {2, 2});
	SIRSplitJoin bottomSJ = (SIRSplitJoin)ManualPartition.getStream(str, 147);
	ManualPartition.fuse(bottomSJ, partitions);

        // you can view the result of the manual partitioning in "after-partition.dot"
	
	return str;
    }
}

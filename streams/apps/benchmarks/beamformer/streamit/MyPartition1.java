import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * Manual partitioning for the BeamFormer program.
 */
public class MyPartition1 {

    public static SIRStream manualPartition(SIRStream str) {

	// fuse the top pipelines
	for (int i=41; i<=85; i+=4) {
	    SIRStream s = ManualPartition.getStream(str, i);
	    ManualPartition.fuse(s);
	}

	// fuse bottom pipelines
	for (int i=89; i<=104; i+=5) {
	    SIRStream s = ManualPartition.getStream(str, i);
	    ManualPartition.fuse(s);
	}

	// fuse the bottom splitjoin to have 2 children instead of 4
	// children
	PartitionGroup partitions = PartitionGroup.createFromArray(new int[] {2, 2});
	SIRSplitJoin bottomSJ = (SIRSplitJoin)ManualPartition.getStream(str, 40);
	ManualPartition.fuse(bottomSJ, partitions);
	
	return str;
    }
}

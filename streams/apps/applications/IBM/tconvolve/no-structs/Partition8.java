import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * Manual partitioning for the BeamFormer program.
 */
public class Partition8 {

    public static SIRStream manualPartition(SIRStream str) {
	System.out.println("*** Manual Partitioning ***");
	NumberDot.printGraph(str, "numbered.dot");
	//do some fission
	SIRStream cd = ManualPartition.getStream(str, 81);
	ManualPartition.fission((SIRFilter)cd, 8, new int[]{1,1,1,1,1,1,1,1}); 


	int[] fiss2 = new int[]{174,175,176,177,96,111,126,141};
	for (int i = 0; i < fiss2.length; i++) {
	    SIRStream f = ManualPartition.getStream(str, fiss2[i]);
	    ManualPartition.fission((SIRFilter)f, 2, new int[]{1,1}); 
	}

	//do some fusion

	int[] ck = new int[]{61,66,71,76};
	for (int i = 0; i < ck.length; i++) {
	    PartitionGroup partitions = PartitionGroup.createFromArray(new int[]{4,5});
	    SIRSplitJoin conker = (SIRSplitJoin)ManualPartition.getStream(str, ck[i]);
	    //ManualPartition.addHierarchicalChildren(conker, partitions);
	    ManualPartition.fuse(conker, partitions);
	    
	}
	/*
	int[] fuse = new int[]{213,216,219,222};
	for (int i = 0; i < fuse.length; i++) {
	    ManualPartition.fuse(ManualPartition.getStream(str,fuse[i]));
	    //ManualPartition.fuse(ManualPartition.getStream(str,fuse[i] + 1));
	}
	
	fuse = new int[]{214,217,220,223};
	for (int i = 0; i < fuse.length; i++) {
	    ManualPartition.fuse(ManualPartition.getStream(str,fuse[i]));
	    //ManualPartition.fuse(ManualPartition.getStream(str,fuse[i] + 1));
	}    
	*/
	
	NumberDot.printGraph(str, "done.dot");
	return str;
    }
}
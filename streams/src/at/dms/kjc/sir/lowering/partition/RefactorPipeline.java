package at.dms.kjc.sir.lowering.partition;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.Lifter;
import java.util.List;

public class RefactorPipeline {

    /**
     * Mutates <pipe> to replace children at indices first...last with
     * a pipeline that contains those children.
     */
    public static void addHierarchicalChild(SIRPipeline pipe, int first, int last) {

	PartitionDot.printGraph(pipe, "debug1.dot");

	// make partition group to represent this partitioning
	int[] group = new int[pipe.size()+first-last];
	for (int i=0; i<first; i++) {
	    group[i] = 1;
	}
	group[first] = last-first+1;
	for (int i=first+1; i<pipe.size()+first-last; i++) {
	    group[i] = 1;
	}
	// do partitioning
	addHierarchicalChildren(pipe, PartitionGroup.createFromArray(group));

	PartitionDot.printGraph(pipe, "debug2.dot");
    }

    /**
     * Given a pipeline <pipe> and a partitioning <partition> of its
     * children, mutates the pipeline so that all the elements of each
     * partition are factored into their own pipelines.
     */
    public static void addHierarchicalChildren(SIRPipeline pipe, PartitionGroup partition) {
	// get copy of list of old children, and parameters passed to them
	List children = pipe.getChildren();
	List params = pipe.getParams();

	// clear old children
	pipe.clear();

	// for all the partitions...
	for(int i=0;i<partition.size();i++) {
	    int partSize=partition.get(i);
	    if (partSize==1) {
		// if there is only one stream in the partition, then
		// we don't need to do anything; just add the child
		int pos = partition.getFirst(i);
		pipe.add((SIRStream)children.get(pos), (List)params.get(pos));
	    } else {
		// the child pipeline
		SIRPipeline childPipe = new SIRPipeline(pipe,
							pipe.getIdent() + "_child" + i,
							JFieldDeclaration.EMPTY(),
							JMethodDeclaration.EMPTY());
		childPipe.setInit(SIRStream.makeEmptyInit());
		
		// move children into hierarchical pipeline
		for(int k=0,l=partition.getFirst(i);k<partSize;k++,l++) {
		    childPipe.add((SIRStream)children.get(l), (List)params.get(l));
		}

		// update new toplevel pipeline
		pipe.add(childPipe);
	    }
	}
    }
}

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
     * Given a pipeline <pipe> and a partitioning <partition> of its
     * children, returns a new pipeline in which all the elements of
     * each partition are factored into their own pipelines.  The
     * returned element is also replaced in the parent (that is, the
     * parent is mutated.)
     */
    public static SIRPipeline addHierarchicalChildren(SIRPipeline pipe, PartitionGroup partition) {
	// new pipeline
	SIRPipeline newPipe = new SIRPipeline(pipe.getParent(),
					      pipe.getIdent(),
					      pipe.getFields(),
					      pipe.getMethods());
	newPipe.setInit(SIRStream.makeEmptyInit());

	// for all the partitions...
	for(int i=0;i<partition.size();i++) {
	    int partSize=partition.get(i);
	    if (partSize==1) {
		// if there is only one stream in the partition, then
		// we don't need to do anything; just add the child
		int pos = partition.getFirst(i);
		newPipe.add(pipe.get(pos), pipe.getParams(pos));
	    } else {
		// the child pipeline
		SIRPipeline childPipe = new SIRPipeline(newPipe,
							newPipe.getIdent() + "_child" + i,
							JFieldDeclaration.EMPTY(),
							JMethodDeclaration.EMPTY());
		childPipe.setInit(SIRStream.makeEmptyInit());

		// move children into hierarchical pipeline
		for(int k=0,l=partition.getFirst(i);k<partSize;k++,l++) {
		    childPipe.add(pipe.get(l), pipe.getParams(l));
		}

		// update new toplevel pipeline
		newPipe.add(childPipe);
	    }
	}
	// replace in parent
	pipe.getParent().replace(pipe,newPipe);

	// return new pipe
	return newPipe;
    }
}

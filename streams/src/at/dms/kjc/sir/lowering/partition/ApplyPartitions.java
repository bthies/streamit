package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

/*
  This is the class that performs the fusion dictated by a
  partitioner.  The general strategy is this:

  1. make copy of children so you can look them up later
  
  2. visit each of children and replace any of them with what they returned
  
  3. fuse yourself (or pieces of yourself) according to hashmap for your original kids

  4. return the new version of yourself
*/
class ApplyPartitions extends EmptyAttributeStreamVisitor {
    /**
     * The partition mapping.
     */
    private HashMap partitions;

    private ApplyPartitions(HashMap partitions) {
	this.partitions = partitions;
    }

    public static void doit(SIRStream str, HashMap partitions) {
	str.accept(new ApplyPartitions(partitions));
    }

    /******************************************************************/
    // local methods for the ILPFuser

    /**
     * Visits/replaces the children of <cont>
     */
    private void replaceChildren(SIRContainer cont) {
	// visit children
	for (int i=0; i<cont.size(); i++) {
	    SIRStream newChild = (SIRStream)cont.get(i).accept(this);
	    cont.set(i, newChild);
	    // if we got a pipeline, try lifting it.  note that this
	    // will mutate the children array and the init function of
	    // <self>
	    if (newChild instanceof SIRPipeline) {
		Lifter.eliminatePipe((SIRPipeline)newChild);
	    }
	}
    }

    /******************************************************************/
    // these are methods of empty attribute visitor

    /* visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
			 JFieldDeclaration[] fields,
			 JMethodDeclaration[] methods,
			 JMethodDeclaration init) {
	// replace children
	replaceChildren(self);
	// fuse children internally
	FusePipe.fuse(self, PartitionGroup.createFromAssignments(self.getChildren(), partitions));
	return self;
    }

    /* visit a splitjoin */
    public Object visitSplitJoin(SIRSplitJoin self,
			  JFieldDeclaration[] fields,
			  JMethodDeclaration[] methods,
			  JMethodDeclaration init,
			  SIRSplitter splitter,
			  SIRJoiner joiner) {
	//System.err.println("visiting " + self);
	// replace children
	replaceChildren(self);
	PartitionGroup group = PartitionGroup.createFromAssignments(self.getParallelStreams(), partitions);
	// fuse
	SIRStream result = FuseSplit.fuse(self, group);
	// if we got pipelines back, that means we used old fusion,
	// and we should fuse the pipe again
	if (group.size()==1 && result instanceof SIRPipeline) {
	    // if the whole thing is a pipeline
	    FusePipe.fuse((SIRPipeline)result);
	} else {
	    // if we might have component pipelines
	    for (int i=0; i<group.size(); i++) {
		if (group.get(i)>1 && ((SIRSplitJoin)result).get(i) instanceof SIRPipeline) {
		    FusePipe.fuse((SIRPipeline)((SIRSplitJoin)result).get(i));
		}
	    }
	}
	return result;
    }

    /* visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
			     JFieldDeclaration[] fields,
			     JMethodDeclaration[] methods,
			     JMethodDeclaration init,
			     JMethodDeclaration initPath) {
	//System.err.println("visiting " + self);
	// fusing a whole feedback loop isn't supported yet
	Utils.assert(getPartition(self)==-1);
	// replace children
	replaceChildren(self);
	return self;
    }

    /******************************************************************/

    /**
     * Returns int partition for <str>
     */
    private int getPartition(Object str) {
	Utils.assert(partitions.containsKey(str), 
		     "No partition recorded for: " + str);
	return ((Integer)partitions.get(str)).intValue();
    }
}


package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.linprog.*;
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

    /**
     * Returns an array suitable for the fusers that indicates the
     * groupings of children into partitions, according to
     * this.partitions.  For instance, if input is:
     *
     *  <children> = {0, 0, 5, 7, 7, 7}
     *
     * then output is {2, 1, 3}
     */
    private int[] calcChildPartitions(List children) {
	List resultList = new LinkedList();
	int pos = 0;
	while (pos<children.size()) {
	    int count = 0;
	    int cur = getPartition(children.get(pos));
	    do {
		pos++;
		count++;
	    } while (pos<children.size() && 
		     getPartition(children.get(pos))==cur && 
		     // don't conglomerate -1 children, as they are
		     // containers with differing tile content
		     cur!=-1);
	    resultList.add(new Integer(count));
	}
	// copy results into int array
	int[] result = new int[resultList.size()];
	for (int i=0; i<result.length; i++) {
	    result[i] = ((Integer)resultList.get(i)).intValue();
	}
	return result;
    }

    /******************************************************************/
    // these are methods of empty attribute visitor

    /* visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
			 JFieldDeclaration[] fields,
			 JMethodDeclaration[] methods,
			 JMethodDeclaration init) {
	//System.err.println("visiting " + self);
	// build partition array based on orig children
	int[] childPart = calcChildPartitions(self.getChildren());
	// replace children
	replaceChildren(self);
	// fuse children internally
	FusePipe.fuse(self, childPart);
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
	// build partition array based on orig children
	int[] childPart = calcChildPartitions(self.getParallelStreams());
	// replace children
	replaceChildren(self);
	// fuse
	return FuseSplit.fuse(self, childPart);
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


package at.dms.kjc.sir.lowering.partition.linear;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

abstract class LDPConfigContainer extends LDPConfig {
    /**
     * The stream for this container.
     */
    protected SIRContainer cont;
    /**
     * Partitioner corresponding to this.
     */
    protected LinearPartitioner partitioner;
    
    /**
     * <width> and <height> represent the dimensions of the stream.
     */
    protected LDPConfigContainer(SIRContainer cont, LinearPartitioner partitioner, 
				int width, int height) {
	super(partitioner);
	this.cont = cont;
	this.partitioner = partitioner;
	this.A = new int[width][width][height][height][3];
	initA();
    }

    /**
     * Initialize elements of this.A to -1
     */
    private void initA() {
	for (int i1=0; i1<A.length; i1++) {
	    for (int i2=0; i2<A[0].length; i2++) {
		for (int i3=0; i3<A[0][0].length; i3++) {
		    for (int i4=0; i4<A[0][0][0].length; i4++) {
			for (int i5=0; i5<A[0][0][0][0].length; i5++) {
			    A[i1][i2][i3][i4][i5] = -1;
			}
		    }
		}
	    }
	}
    }

    public SIRStream getStream() {
	return cont;
    }

    /**
     * Requires <str> is a container.
     */
    protected void setStream(SIRStream str) {
	Utils.assert(str instanceof SIRContainer);
	this.cont = (SIRContainer)str;
    }

    protected int get(int collapse) {
	// otherwise, compute it
	return get(0, A.length-1, 0, A[0][0].length-1, collapse, this.cont);
    }

    /**
     * <str> is a stream object representing the current sub-segment that we're operating on
     */
    protected int get(int x1, int x2, int y1, int y2, int collapse, SIRStream str) {
	// if we've memoized the value before, return it
	if (A[x1][x2][y1][y2][collapse]>=0) {
	    return A[x1][x2][y1][y2][collapse];
	}

	// if we are down to one child, then descend into child
	if (x1==x2 && y1==y2) {
	    int childCost = childConfig(x1, y1).get(collapse); 
	    A[x1][x2][y1][y2][collapse] = childCost;
	    return childCost;
	}

	// otherwise, going to do some analysis...
	LinearAnalyzer lfa = partitioner.getLinearAnalyzer();
	lfa.findLinearFilters(str, KjcOptions.debug, lfa);

	int savings;
	switch(collapse) {
	case LinearPartitioner.COLLAPSE_ANY: {
	    // if we still have flexibility, do better out of
	    // collapsing or not
	    savings = Math.max(get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_LINEAR, str),
			       Math.max(get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_FREQ, str),
					get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_NONE, str)));
	    break;
	}

	case LinearPartitioner.COLLAPSE_FREQ: {
	    if (!lfa.hasLinearRepresentation(str)) {
		savings = Integer.MIN_VALUE;
	    } else {
		// start with savings from linear collapse
		savings = get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_LINEAR, str);
		// then add the benefit from frequency collapse
		LinearCost cost = lfa.getLinearRepresentation(str).getCost();
		savings += cost.getFrequencyCost() - cost.getDirectCost();
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_LINEAR: {
	    // if we don't have a linear node, return negative infinity
	    if (!lfa.hasLinearRepresentation(str)) {
		savings = Integer.MIN_VALUE;
	    } else {
		savings = 0;
		// make arbitrary horizontal or vertical cut and add
		// savings of children
		int[] arr = { 1, ((SIRContainer)str).size()-1 };
		PartitionGroup pg = PartitionGroup.createFromArray(arr);
		if (x1<x2) {
		    Utils.assert(str instanceof SIRSplitJoin);
		    SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, pg);
		    // get number of times each child of <cont> executes
		    HashMap[] counts = SIRScheduler.getExecutionCounts(sj);
		    // add savings from two children
		    int[] steadyCount = { ((int[])counts[1].get(cont.get(0)))[0],
					  ((int[])counts[1].get(cont.get(1)))[1] };
		    // add savings from two children
		    savings += steadyCount[0] * get(x1, x1, y1, y2, collapse, sj.get(0));
		    savings += steadyCount[1] * get(x1+1, x2, y1, y2, collapse, sj.get(1));
		    savings += getOutermostSavings((SIRContainer)str, lfa, counts);
		} else {
		    Utils.assert(y1<y2 && str instanceof SIRPipeline);
		    SIRPipeline pipe = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
		    // get number of times each child of <cont> executes
		    HashMap[] counts = SIRScheduler.getExecutionCounts(pipe);
		    int[] steadyCount = { ((int[])counts[1].get(cont.get(0)))[0],
					  ((int[])counts[1].get(cont.get(1)))[1] };
		    // add savings from two children
		    savings += steadyCount[0] * get(x1, x2, y1, y1, collapse, pipe.get(0));
		    savings += steadyCount[1] * get(x1, x2, y1+1, y2, collapse, pipe.get(1));
		    savings += getOutermostSavings((SIRContainer)str, lfa, counts);
		}
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    savings = Integer.MIN_VALUE;
	    
	    // try a vertical cut
	    for (int xPivot=x1; xPivot<x2; xPivot++) {
		// break along <xPivot>
		int[] arr = { 1 + (xPivot-x1), 1 + (x2-xPivot) };
		PartitionGroup pg = PartitionGroup.createFromArray(arr);
		SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, pg);
		savings = Math.max( savings, 
				    get(x1, xPivot, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(0)) +
				    get(xPivot+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(1)) );
	    }
	    
	    // try a horizontal cut
	    for (int yPivot=y1; yPivot<y2; yPivot++) {
		// break along <yPivot>
		int[] arr = { 1 + (yPivot-y1), 1 + (y2-yPivot) };
		PartitionGroup pg = PartitionGroup.createFromArray(arr);
		SIRPipeline pipe = RefactorSplitJoin.addSyncPoints((SIRSplitJoin)str, pg);
		savings = Math.max( savings, 
				    get(x1, x2, y1, yPivot, LinearPartitioner.COLLAPSE_ANY, pipe.get(0)) +
				    get(x1, x2, yPivot+1, y2, LinearPartitioner.COLLAPSE_ANY, pipe.get(1)) );
	    }
	    break;
	}
	    
	default: {
	    savings = -1;
	    Utils.fail("Unrecognized collapse value: " + collapse);
	}
	    
	}
	
	A[x1][x2][y1][y2][collapse] = savings;
	return savings;
    }

    /**
     * Traceback function.
     */
    public StreamTransform traceback(int collapse) {
	StreamTransform st = traceback(0, A.length-1, 0, A[0][0].length-1, collapse, this.cont);
	return st;
    }
	
    /**
     * Traceback helper function.
     */
    protected StreamTransform traceback(int x1, int x2, int y1, int y2, int collapse, SIRStream str) {

	// if we're down to one node, then descend into it
	if (x1==x2 && y1==y2) {
	    StreamTransform child = childConfig(x1, y1).traceback(collapse);
	    // if this config container only has one child, then we
	    // should wrap this in an identity so that we don't apply
	    // it to ourself
	    if (A.length==1 && A[0][0].length==1) {
		StreamTransform result = new IdentityTransform();
		result.addSucc(child);
		return result.reduce();
	    } else {
		return child.reduce();
	    }
	}

	switch(collapse) {

	case LinearPartitioner.COLLAPSE_ANY: {
	    // take max of other three options
	    int[] options = { LinearPartitioner.COLLAPSE_FREQ, 
			      LinearPartitioner.COLLAPSE_LINEAR, 
			      LinearPartitioner.COLLAPSE_NONE };
	    for (int i=0; i<options.length; i++) {
		if (A[x1][x2][y1][y2][collapse] == get(x1, x2, y1, y2, options[i], str)) {
		    return traceback(x1, x2, y1, y2, options[i], str);
		}
	    }
	    Utils.fail("Didn't find traceback; was looking for ANY.");
	    break;
	}

	case LinearPartitioner.COLLAPSE_FREQ: {
	    return new FreqReplaceTransform(partitioner.getLinearAnalyzer());
	}
	    
	case LinearPartitioner.COLLAPSE_LINEAR: {
	    return new LinearReplaceTransform(partitioner.getLinearAnalyzer());
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    // try a vertical cut
	    for (int xPivot=x1; xPivot<x2; xPivot++) {
		// break along <xPivot>
		if (A[x1][x2][y1][y2][collapse] == (get(x1, xPivot, y1, y2, LinearPartitioner.COLLAPSE_ANY, 
							/* dummy arg since get operation should just be lookup now */ null) +
						    get(xPivot+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, 
							/* dummy arg since get operation should just be lookup now */ null)) ) {
		    // found the optimum
		    int[] arr = { 1 + (xPivot-x1), 1 + (x2-xPivot) };
		    PartitionGroup pg = PartitionGroup.createFromArray(arr);
		    SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, pg);

		    // generate transform
		    StreamTransform result = new VerticalCutTransform(xPivot-x1);
		    // recurse left and right, adding transforms as post-ops
		    result.addSucc(traceback(x1, xPivot, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(0)));
		    result.addSucc(traceback(x1+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(1)));

		    // all done
		    return result.reduce();
		}
	    }

	    // try a horizontal cut
	    for (int yPivot=y1; yPivot<y2; yPivot++) {
		// break along <yPivot>
		if (A[x1][x2][y1][y2][collapse] == (get(x1, x2, y1, yPivot, LinearPartitioner.COLLAPSE_ANY, str) +
						    get(x1, x2, yPivot+1, y2, LinearPartitioner.COLLAPSE_ANY, str)) ) {
		    // found the optimum
		    int[] arr = { 1 + (yPivot-y1), 1 + (y2-yPivot) };
		    PartitionGroup pg = PartitionGroup.createFromArray(arr);
		    SIRPipeline pipe = RefactorSplitJoin.addSyncPoints((SIRSplitJoin)str, pg);

		    // generate transform
		    StreamTransform result = new HorizontalCutTransform(yPivot-y1);
		    // recurse left and right, adding transforms as post-ops
		    result.addSucc(traceback(x1, x2, y1, yPivot, LinearPartitioner.COLLAPSE_ANY, pipe.get(0)));
		    result.addSucc(traceback(x1, x2, yPivot+1, y2, LinearPartitioner.COLLAPSE_ANY, pipe.get(1)));

		    // all done
		    return result.reduce();
		}
	    }

	    Utils.fail("Trying to find traceback for COLLAPSE_NONE but didn't find path.");
	}
	    
	default: {
	    Utils.fail("Unrecognized collapse type: " + collapse);
	}
	}

	// if we make it this far, then we didn't find our traceback
	Utils.fail("Didn't find traceback.");
	return null;
    }

    /**
     * Returns config for child at index <x, y>
     */
    protected abstract LDPConfig childConfig(int x, int y);

    /**
     * Returns the extra savings that come by combining the children
     * of <cont> into an entire linear node (equivalent to <cont>).
     *
     * Requires that <lfa> contains a linear representation for <cont>
     * as well as all its children.
     */
    private int getOutermostSavings(SIRContainer cont, LinearAnalyzer lfa, HashMap[] counts) {
	// tabulate cost of doing separately
	int separate = 0;
	for (int i=0; i<cont.size(); i++) {
	    int steadyCount = ((int[])counts[1].get(cont.get(i)))[0];
	    separate += steadyCount * lfa.getLinearRepresentation(cont.get(i)).getCost().getDirectCost();
	}

	// get the combined cost
	int combined = lfa.getLinearRepresentation(cont).getCost().getDirectCost();

	// return difference (remember positive is good)
	return separate - combined;
    }
}

package at.dms.kjc.sir.lowering.partition.linear;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.linear.frequency.*;
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
	this.A = new int[width][width][height][height][4];
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
	String callStr = "get(" + x1 + ", " + x2 + ", " + y1 + ", " + y2 + ", " + LinearPartitioner.COLLAPSE_STRING(collapse) + ", " + (str==null ? "null" : str.getIdent());
	if (LinearPartitioner.DEBUG) { System.err.println("calling " + callStr); } 
	// if we've memoized the value before, return it
	if (A[x1][x2][y1][y2][collapse]>=0) {
	    if (LinearPartitioner.DEBUG) { System.err.println(" returning memoized value, " + callStr + " = " + A[x1][x2][y1][y2][collapse]); }
	    return A[x1][x2][y1][y2][collapse];
	}

	// if we are down to one child, then descend into child
	if (x1==x2 && y1==y2) {
	    int childCost = childConfig(x1, y1).get(collapse); 
	    A[x1][x2][y1][y2][collapse] = childCost;
	    if (LinearPartitioner.DEBUG) { System.err.println(" returning child cost, " + callStr + " = " + childCost); }
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
	    if (!lfa.hasLinearRepresentation(str) || !LEETFrequencyReplacer.canReplace(str, lfa)) {
		savings = Integer.MIN_VALUE;
	    } else {
		// start with savings from linear collapse
		savings = get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_LINEAR, str);
		// then add the benefit from frequency collapse
		LinearFilterRepresentation l = lfa.getLinearRepresentation(str);
		savings += getScalingFactor(l, str) * (l.getCost().getDirectCost() - l.getCost().getFrequencyCost() );
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_LINEAR: {
	    // if we don't have a linear node, return negative infinity
	    if (!lfa.hasLinearRepresentation(str)) {
		savings = Integer.MIN_VALUE;
	    } else {
		// otherwise, calculate savings of children, and cost
		// of children as collapsed linear nodes
		int childSavings;
		int childCost;
		// make arbitrary horizontal or vertical cut and add
		// savings of children
		int[] arr = { 1, ((SIRContainer)str).size()-1 };
		PartitionGroup pg = PartitionGroup.createFromArray(arr);
		if (x1<x2) {
		    Utils.assert(str instanceof SIRSplitJoin);
		    SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, pg);
		    // calc savings
		    childSavings = get(x1, x1, y1, y2, collapse, sj.get(0)) + get(x1+1, x2, y1, y2, collapse, sj.get(1));
		    // calc cost
		    LinearAnalyzer.findLinearFilters(sj, KjcOptions.debug, lfa);
		    LinearFilterRepresentation[] l = { lfa.getLinearRepresentation(sj.get(0)), lfa.getLinearRepresentation(sj.get(1)) };
		    childCost = ( getScalingFactor(l[0], sj.get(0)) * l[0].getCost().getDirectCost() + 
				  getScalingFactor(l[1], sj.get(1)) * l[1].getCost().getDirectCost() );
		} else {
		    Utils.assert(y1<y2 && str instanceof SIRPipeline);
		    SIRPipeline pipe = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
		    // calc savings
		    childSavings = get(x1, x2, y1, y1, collapse, pipe.get(0)) + get(x1, x2, y1+1, y2, collapse, pipe.get(1));
		    // calc cost
		    LinearAnalyzer.findLinearFilters(pipe, KjcOptions.debug, lfa);
		    LinearFilterRepresentation[] l = { lfa.getLinearRepresentation(pipe.get(0)), lfa.getLinearRepresentation(pipe.get(1)) };
		    childCost = ( getScalingFactor(l[0], pipe.get(0)) * l[0].getCost().getDirectCost() + 
				  getScalingFactor(l[1], pipe.get(1)) * l[1].getCost().getDirectCost() );
		}
		// put children back in <str>
		((SIRContainer)str).reclaimChildren();

		// get cost of self
		LinearFilterRepresentation l = lfa.getLinearRepresentation(str);
		int myCost = getScalingFactor(l, str) * l.getCost().getDirectCost();

		// calculate savings as child savings, PLUS diff between child cost and my cost
		savings = childSavings + (childCost - myCost);
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    savings = 0;
	    
	    // try a vertical cut
	    for (int xPivot=x1; xPivot<x2; xPivot++) {
		// break along <xPivot>
		int[] arr = { 1 + (xPivot-x1), x2-xPivot };
		PartitionGroup pg = PartitionGroup.createFromArray(arr);
		SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, pg);
		savings = Math.max( savings, 
				    get(x1, xPivot, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(0)) +
				    get(xPivot+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(1)) );
	    }

	    // optimization: don't both with a horizontal cut if we're
	    // dealing with a splitjoin, and all the children under
	    // consideration are linear.  This is because combining
	    // some subsegment of linear splitjoin children will never
	    // lead to a performance improvemnt; instead, we should
	    // consider each pipeline individually.
	    boolean tryHoriz = false;
	    if (x1==x2) {
		// if we have a pipeline, try the horizontal cut always
		tryHoriz = true;
	    } else {
		// if we're in 2-D cut mode, then see if there's a chance to try it
		if (LinearPartitioner.ENABLE_TWO_DIMENSIONAL_CUTS) {
		    // if we have a splijtoin, only try horiz if one of
		    // children is non-linear
		    search: 
		    for (int x=x1; x<=x2; x++) {
			for (int y=y1; y<=y2; y++) {
			    if (!lfa.hasLinearRepresentation(childConfig(x, y).getStream())) {
				tryHoriz = true;
				break search;
			    }
			}
		    }
		} else {
		    tryHoriz = false;
		}
	    }

	    if (tryHoriz) {
		// try a horizontal cut
		for (int yPivot=y1; yPivot<y2; yPivot++) {
		    // break along <yPivot>
		    int[] arr = { 1 + (yPivot-y1), y2-yPivot };
		    PartitionGroup pg = PartitionGroup.createFromArray(arr);
		    SIRContainer factored;
		    // might have either pipeline or splitjoin at this point...
		    if (str instanceof SIRSplitJoin) {
			factored = RefactorSplitJoin.addSyncPoints((SIRSplitJoin)str, pg);
		    } else {
			factored = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
		    }
		    savings = Math.max( savings, 
					get(x1, x2, y1, yPivot, LinearPartitioner.COLLAPSE_ANY, factored.get(0)) +
					get(x1, x2, yPivot+1, y2, LinearPartitioner.COLLAPSE_ANY, factored.get(1)) );
		}
	    }
	    // put children back in <str>
	    ((SIRContainer)str).reclaimChildren();

	    break;
	}
	    
	default: {
	    savings = -1;
	    Utils.fail("Unrecognized collapse value: " + collapse);
	}
	    
	}
	
	A[x1][x2][y1][y2][collapse] = savings;
	if (LinearPartitioner.DEBUG) { System.err.println(" returning " + callStr + " = " + savings); }
	return savings;
    }

    /**
     * Traceback function.
     */
    public StreamTransform traceback(int collapse) {
	if (LinearPartitioner.DEBUG) { printArray(); }
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
		    int[] arr = { 1 + (xPivot-x1), x2-xPivot };
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

	    // try a horizontal cut -- note that we should never get
	    // this far if we skipped the horizontal cut above, as we
	    // should've found the traceback in the vertical cut
	    // section
	    for (int yPivot=y1; yPivot<y2; yPivot++) {
		// break along <yPivot>
		if (A[x1][x2][y1][y2][collapse] == (get(x1, x2, y1, yPivot, LinearPartitioner.COLLAPSE_ANY, str) +
						    get(x1, x2, yPivot+1, y2, LinearPartitioner.COLLAPSE_ANY, str)) ) {
		    // found the optimum
		    int[] arr = { 1 + (yPivot-y1), y2-yPivot };
		    PartitionGroup pg = PartitionGroup.createFromArray(arr);

		    SIRContainer cont;
		    // might have either pipeline or splitjoin at this point...
		    if (str instanceof SIRSplitJoin) {
			cont = RefactorSplitJoin.addSyncPoints((SIRSplitJoin)str, pg);
		    } else {
			cont = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
		    }

		    // generate transform
		    StreamTransform result = new HorizontalCutTransform(yPivot-y1);
		    // recurse left and right, adding transforms as post-ops
		    result.addSucc(traceback(x1, x2, y1, yPivot, LinearPartitioner.COLLAPSE_ANY, cont.get(0)));
		    result.addSucc(traceback(x1, x2, yPivot+1, y2, LinearPartitioner.COLLAPSE_ANY, cont.get(1)));

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
    private int getOutermostSavings(SIRContainer str, LinearAnalyzer lfa, HashMap[] counts) {
	// tabulate cost of doing separately
	int separate = 0;
	for (int i=0; i<str.size(); i++) {
	    int steadyCount = ((int[])counts[1].get(str.get(i)))[0];
	    separate += steadyCount * lfa.getLinearRepresentation(str.get(i)).getCost().getDirectCost();
	}

	// get the combined cost
	int combined = lfa.getLinearRepresentation(str).getCost().getDirectCost();

	// return difference (remember positive is good)
	return separate - combined;
    }
}

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
     * A_s[x1][x2][y1][y2][c] holds savings if children (x1..x2,
     * y1..y2) of stream s given collapse policy <c>.
     */
    private int[][][][][] A;
    /**
     * Specifies the width of the y_i'th component of this
     */
    private int[] width;
    
    /**
     * <width>[] and <height> represent the dimensions of the stream.
     * Requires that width.length==height; width[i] specifies the
     * width of the i'th component of this and is internalized in
     * representation of this.
     */
    protected LDPConfigContainer(SIRContainer cont, LinearPartitioner partitioner, 
				int[] width, int height) {
	super(partitioner);
	this.cont = cont;
	this.partitioner = partitioner;
	this.width = width;
	// find maxWidth
	int maxWidth = -1;
	for (int i=0; i<height; i++) {
	    maxWidth = Math.max(maxWidth, width[i]);
	}
	// for simplicity, allocate the bounding box for A
	this.A = new int[maxWidth][maxWidth][height][height][4];
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

    /**
     * Debugging output.
     */
    private static int indent = 0;
    private void debugMessage(String str) {
	if (LinearPartitioner.DEBUG) {
	    for (int i=0; i<indent; i++) {
		System.err.print(" ");
	    }
	    System.err.println(str);
	}
    }

    protected int get(int collapse) {
	// otherwise, compute it
	return get(0, A.length-1, 0, A[0][0].length-1, collapse, this.cont);
    }

    /**
     * <str> is a stream object representing the current sub-segment that we're operating on
     */
    protected int get(int x1, int x2, int y1, int y2, int collapse, SIRStream str) {
	indent++;
	String callStr = "get(" + x1 + ", " + x2 + ", " + y1 + ", " + y2 + ", " + LinearPartitioner.COLLAPSE_STRING(collapse) + ", " + (str==null ? "null" : str.getIdent());
	debugMessage("calling " + callStr); 

	// if we've exceeded the width of this node, then trim down to actual width
	int maxWidth = width[y1];
	for (int i=y1+1; i<=y2; i++) {
	    maxWidth = Math.max(maxWidth, width[i]);
	}
	if (x2>maxWidth-1) {
	    debugMessage(" scaling x2 back to " + (maxWidth-1));
	    x2 = maxWidth-1;
	}

	// if we've memoized the value before, return it
	if (A[x1][x2][y1][y2][collapse]>=0) {
	    debugMessage(" returning memoized value, " + callStr + " = " + A[x1][x2][y1][y2][collapse]);
	    indent--;
	    return A[x1][x2][y1][y2][collapse];
	}

	// if we are down to one child, then descend into child
	if (x1==x2 && y1==y2) {
	    int childCost = childConfig(x1, y1).get(collapse); 
	    A[x1][x2][y1][y2][collapse] = childCost;
	    System.err.println(" returning child cost, " + callStr + " = " + childCost);
	    indent--;
	    return childCost;
	}

	// otherwise, going to do some analysis...
	LinearAnalyzer lfa = partitioner.getLinearAnalyzer();
	lfa.findLinearFilters(str, KjcOptions.debug, lfa);

	int cost;
	switch(collapse) {
	case LinearPartitioner.COLLAPSE_ANY: {
	    // if we still have flexibility, do better out of
	    // collapsing or not
	    cost = Math.min(get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_LINEAR, str),
			       Math.min(get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_FREQ, str),
					get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_NONE, str)));
	    break;
	}

	case LinearPartitioner.COLLAPSE_FREQ: {
	    if (!lfa.hasLinearRepresentation(str) || !LEETFrequencyReplacer.canReplace(str, lfa) || KjcOptions.nolinearcollapse) {
		cost = Integer.MAX_VALUE;
	    } else {
		// otherwise, return freq costn
		LinearFilterRepresentation l = lfa.getLinearRepresentation(str);
		cost = getScalingFactor(l, str) * l.getCost().getFrequencyCost();
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_LINEAR: {
	    // if we don't have a linear node, return infinity
	    if (!lfa.hasLinearRepresentation(str) || KjcOptions.nolinearcollapse) {
		cost = Integer.MAX_VALUE;
	    } else {
		// otherwise, return cost of collapsed node
		LinearFilterRepresentation l = lfa.getLinearRepresentation(str);
		cost = getScalingFactor(l, str) * l.getCost().getDirectCost();
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    cost = Integer.MAX_VALUE;

	    // see if we can do a vertical cut -- first, that there
	    // are two streams to cut between
	    boolean tryVertical = x1<x2;

	    // then, that all the child streams have same width
	    if (tryVertical) {
		int childWidth = width[y1];
		for (int i=y1+1; i<=y2; i++) {
		    if (width[i]!=childWidth) {
			tryVertical = false;
		    }
		}
	    }

	    // then, that there's a non-linear child stream, or a row
	    // that contains a mixture of linear and non-linear
	    // streams, or linear streams with different costs.  In
	    // this case, we might want to treat the children
	    // differently.
	    if (tryVertical && y1<y2) {
		tryVertical = false;
		search: 
		for (int y=y1; y<=y2; y++) {
		    int rowCost = -1;
		    if (lfa.hasLinearRepresentation(childConfig(x1, y).getStream())) {
			rowCost = lfa.getLinearRepresentation(childConfig(x1, y).getStream()).getCost().getDirectCost();
		    } 
		    for (int x=x1+1; x<=x2; x++) {
			int childCost = -1;
			if (lfa.hasLinearRepresentation(childConfig(x, y).getStream())) {
			    childCost = lfa.getLinearRepresentation(childConfig(x, y).getStream()).getCost().getDirectCost();
			}
			if (childCost!=rowCost) {
			    tryVertical = true;
			    break search;
			}
		    }
		}
	    }
	    
	    // get the object we're doing vertical cuts on, and try to
	    // remove any synchronization
	    SIRSplitJoin verticalObj = null;
	    if (tryVertical) {
		if (str instanceof SIRPipeline) {
		    // make a copy of our pipeline, since we're about
		    // to split across it.  Don't worry about parent
		    // field of children, since they seem to be
		    // switched around a lot during partitioning (and
		    // restored after)
		    SIRPipeline copy = new SIRPipeline(str.getParent(), str.getIdent()+"_copy");
		    for (int i=0; i<((SIRPipeline)str).size(); i++) { copy.add(((SIRPipeline)str).get(i)); }
		    // now remove synchronization in <copy>.
		    RefactorSplitJoin.removeMatchingSyncPoints(copy);
		    // now if we only have one splitjoin left as the child, we can do a cut
		    if (copy.size()==1) {
			verticalObj = (SIRSplitJoin)copy.get(0);
		    } else {
			// otherwise can't do a cut
			tryVertical = false;
		    }
		} else if (str instanceof SIRSplitJoin) {
		    verticalObj = (SIRSplitJoin)str;
		} else {
		    Utils.fail("Didn't expect " + str.getClass() + " as object of vertical cut.");
		}
	    }
	    Utils.assert(!(tryVertical && verticalObj==null));

	    // try a vertical cut if possible
	    if (tryVertical) {
		for (int xPivot=x1; xPivot<x2; xPivot++) {
		    // break along <xPivot>
		    int[] arr = { 1 + (xPivot-x1), x2-xPivot };
		    PartitionGroup pg = PartitionGroup.createFromArray(arr);
		    SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)verticalObj, pg);
		    cost = Math.min( cost, 
				     get(x1, xPivot, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(0)) +
				     get(xPivot+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(1)) );
		}
	    } else {
		debugMessage(" Not trying vertical cut.");
	    }

	    // optimization: don't both with a horizontal cut if we're
	    // dealing with a splitjoin, and all the children under
	    // consideration are linear.  This is because combining
	    // some subsegment of linear splitjoin children will never
	    // lead to a performance improvemnt; instead, we should
	    // consider each pipeline individually.
	    boolean tryHoriz = false;
	    if (x1==x2) {
		// if we have a pipeline or we can't make vertical
		// cut, must try the horizontal cut
		tryHoriz = true;
	    } else {
		// if we're in 2-D cut mode, then see if there's a chance to try it
		if (LinearPartitioner.ENABLE_TWO_DIMENSIONAL_CUTS) {
		    // if we have a splijtoin, only try horiz if one of
		    // children is non-linear
		    search: 
		    for (int y=y1; y<=y2; y++) {
			for (int x=x1; x<=Math.min(x2, width[y]-1); x++) {
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
		    } else if (str instanceof SIRPipeline) {
			factored = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
		    } else if (str instanceof SIRFeedbackLoop) {
			// if we have a feedbackloop, then factored is
			// just the original, since it will have only
			// two children
			factored = (SIRContainer)str;
		    } else {
			factored = null;
			Utils.fail("Unrecognized stream type: " + str);
		    }
		    cost = Math.min( cost, 
				     get(x1, x2, y1, yPivot, LinearPartitioner.COLLAPSE_ANY, factored.get(0)) +
				     get(x1, x2, yPivot+1, y2, LinearPartitioner.COLLAPSE_ANY, factored.get(1)) );
		}
	    } else {
		debugMessage(" Not trying vertical cut.");
	    }
	    break;
	}
	    
	default: {
	    cost = -1;
	    Utils.fail("Unrecognized collapse value: " + collapse);
	}
	    
	}
	
	A[x1][x2][y1][y2][collapse] = cost;
	debugMessage(" returning " + callStr + " = " + cost);
	indent--;
	return cost;
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

	// if we've exceeded the width of this node, then trim down to actual width
	int maxWidth = width[y1];
	for (int i=y1+1; i<=y2; i++) {
	    maxWidth = Math.max(maxWidth, width[i]);
	}
	if (x2>maxWidth-1) {
	    if (LinearPartitioner.DEBUG) { System.err.println("  scaling x2 back to " + (maxWidth-1)); }
	    x2 = maxWidth-1;
	}

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
	    // take min of other three options
	    int[] options = { LinearPartitioner.COLLAPSE_FREQ, 
			      LinearPartitioner.COLLAPSE_LINEAR, 
			      LinearPartitioner.COLLAPSE_NONE };
	    for (int i=0; i<options.length; i++) {
		if (A[x1][x2][y1][y2][collapse] == get(x1, x2, y1, y2, options[i], str)) {
		    return traceback(x1, x2, y1, y2, options[i], str);
		}
	    }
	    Utils.fail("Didn't find traceback; was looking for ANY of " + cont + "[" + x1 + "][" + x2 + "][" + y1 + "][" + y2 + 
		       "][" + LinearPartitioner.COLLAPSE_STRING(collapse) + "] = " + A[x1][x2][y1][y2][collapse]);
	    break;
	}

	case LinearPartitioner.COLLAPSE_FREQ: {
	    return new FreqReplaceTransform(partitioner.getLinearAnalyzer());
	}
	    
	case LinearPartitioner.COLLAPSE_LINEAR: {
	    return new LinearReplaceTransform(partitioner.getLinearAnalyzer());
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    // see if we can do a vertical cut -- first, that there
	    // are two streams to cut between
	    boolean tryVertical = x1<x2;

	    // then, that all the child streams have same width
	    if (tryVertical) {
		int childWidth = width[y1];
		for (int i=y1+1; i<=y2; i++) {
		    if (width[i]!=childWidth) {
			tryVertical = false;
		    }
		}
	    }

	    // then, that there's a non-linear child stream, or a row
	    // that contains a mixture of linear and non-linear
	    // streams, or linear streams with different costs.  In
	    // this case, we might want to treat the children
	    // differently.
	    LinearAnalyzer lfa = partitioner.getLinearAnalyzer();
	    if (tryVertical && y1<y2) {
		tryVertical = false;
		search: 
		for (int y=y1; y<=y2; y++) {
		    int rowCost = -1;
		    if (lfa.hasLinearRepresentation(childConfig(x1, y).getStream())) {
			rowCost = lfa.getLinearRepresentation(childConfig(x1, y).getStream()).getCost().getDirectCost();
		    } 
		    for (int x=x1+1; x<=x2; x++) {
			int childCost = -1;
			if (lfa.hasLinearRepresentation(childConfig(x, y).getStream())) {
			    childCost = lfa.getLinearRepresentation(childConfig(x, y).getStream()).getCost().getDirectCost();
			}
			if (childCost!=rowCost) {
			    tryVertical = true;
			    break search;
			}
		    }
		}
	    }
	    
	    // get the object we're doing vertical cuts on, and try to
	    // remove any synchronization
	    SIRSplitJoin verticalObj = null;
	    if (tryVertical) {
		if (str instanceof SIRPipeline) {
		    // make a copy of our pipeline, since we're about
		    // to split across it.  Don't worry about parent
		    // field of children, since they seem to be
		    // switched around a lot during partitioning (and
		    // restored after)
		    SIRPipeline copy = new SIRPipeline(str.getParent(), str.getIdent()+"_copy");
		    for (int i=0; i<((SIRPipeline)str).size(); i++) { copy.add(((SIRPipeline)str).get(i)); }
		    // now remove synchronization in <copy>.
		    RefactorSplitJoin.removeMatchingSyncPoints(copy);
		    // now if we only have one splitjoin left as the child, we can do a cut
		    if (copy.size()==1) {
			verticalObj = (SIRSplitJoin)copy.get(0);
		    } else {
			// otherwise can't do a cut
			tryVertical = false;
		    }
		} else if (str instanceof SIRSplitJoin) {
		    verticalObj = (SIRSplitJoin)str;
		} else {
		    Utils.fail("Didn't expect " + str.getClass() + " as object of vertical cut.");
		}
	    }
	    Utils.assert(!(tryVertical && verticalObj==null));

	    // try a vertical cut if possible
	    if (tryVertical) {
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
			SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)verticalObj, pg);
			
			// generate transform
			StreamTransform result = new VerticalCutTransform(xPivot-x1);
			// recurse left and right, adding transforms as post-ops
			result.addSucc(traceback(x1, xPivot, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(0)));
			result.addSucc(traceback(x1+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(1)));
			
			// all done
			return result.reduce();
		    }
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
		    } else if (str instanceof SIRPipeline) {
			cont = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
		    } else if (str instanceof SIRFeedbackLoop) {
			// if we have a feedbackloop, then factored is
			// just the original, since it will have only
			// two children
			cont = (SIRContainer)str;
		    } else {
			cont = null;
			Utils.fail("Unrecognized stream type: " + str);
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
     * Prints the array of memoized values of this.
     */
    public void printArray() {
	String msg = "Printing array for " + getStream().getIdent() + " --------------------------";
	System.err.println(msg);
	for (int i1=0; i1<A.length; i1++) {
	    for (int i2=0; i2<A[0].length; i2++) {
		for (int i3=0; i3<A[0][0].length; i3++) {
		    for (int i4=0; i4<A[0][0][0].length; i4++) {
			System.err.println();
			for (int i5=0; i5<4; i5++) {
			    System.err.println(getStream().getIdent() + "[" + i1 + "][" + i2 + "][" + i3 + "][" + i4 + "][" + 
					       LinearPartitioner.COLLAPSE_STRING(i5) + "] = " + A[i1][i2][i3][i4][i5]);
			}
		    }
		}
	    }
	}
	for (int i=0; i<msg.length(); i++) {
	    System.err.print("-");
	}
	System.err.println();
    }
}

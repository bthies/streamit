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
    private long[][][][][] A;
    /**  
     * Streams we've created for a given sub-segment.  Lowest-order
     * dimension is just for aliasing.
     */
    private SIRStream[][][][][] strCache;
    /**
     * Specifies the width of i'th row of this.
     */
    private int[] width;
    /**
     * Whether or not the i'th row of this is uniform -- that is,
     * whether each entry computes a matrix that has zero's and ones
     * at the same location (or computes no matrix at all... is
     * non-linear).
     */
    boolean[] uniform;
    
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
	// calculate uniform
	this.uniform = new boolean[height];
	initUniform();
	// for simplicity, allocate the bounding box for A
	this.A = new long[maxWidth][maxWidth][height][height][4];
	this.strCache = new SIRStream[maxWidth][maxWidth][height][height][1];
	initCaches();
	starttime = System.currentTimeMillis();
    }

    /**
     * Initialize <uniform> array.
     */
    private void initUniform() {
	// find linear parts
	LinearAnalyzer lfa = partitioner.getLinearAnalyzer();
	LinearAnalyzer.findLinearFilters(cont, KjcOptions.debug, lfa);
	for (int i=0; i<uniform.length; i++) {
	    FilterMatrix A = null, b = null;
	    // get A and b of first column
	    if (lfa.hasLinearRepresentation(childConfig(0, i).getStream())) {
		LinearFilterRepresentation linearRep = lfa.getLinearRepresentation(childConfig(0, i).getStream());
		A = linearRep.getA();
		b = linearRep.getb();
	    }
	    // will try disproving this
	    uniform[i] = true;
	    search:
	    for (int j=1; j<width[i]; j++) {
		// get A and b of j'th column
		FilterMatrix A2 = null, b2 = null;
		if (lfa.hasLinearRepresentation(childConfig(j, i).getStream())) {
		    LinearFilterRepresentation linearRep = lfa.getLinearRepresentation(childConfig(j, i).getStream());
		    A2 = linearRep.getA();
		    b2 = linearRep.getb();
		}
		// it's not uniform if only one of A/A2 is null, or if
		// they are different in position of zero's.  Same
		// with b/b2, although they should be zero in exactly
		// the same cases as A.
		if ( ((A==null) != (A2==null)) || (A!=null && (!A.hasEqualZeroOneElements(A2))) || (b!=null &&  (!b.hasEqualZeroOneElements(b2))) ) {
		    uniform[i] = false;
		    break search;
		}
	    }
	}
	for (int i=0; i<uniform.length; i++) {
	    if (uniform[i] && width[i]>1) {
		debugMessage("Found row " + i + "/" + (uniform.length-1) + " of " + cont.getName() + " to be uniform.");
	    }
	}
    }

    /**
     * Initialize elements of this.A to -1.  Setup sharing in A and strCache
     */
    private void initCaches() {
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
	// now find maximal uniform regions with the same width, and alias their memoization tables
	int low=0;
	while (low<uniform.length) {
	    while (low<uniform.length && !uniform[low]) {
		low++;
	    }
	    if (low<uniform.length) {
		int high = low;
		while (high+1<uniform.length && uniform[high+1] && (width[high+1]==width[low])) {
		    high++;
		}
		// alias low..high across the board, to point to values in 0'th column
		for (int i1=low; i1<=high; i1++) {
		    // only aliasing from i2=i1..high matters, but we'd just as well go i2=low...high
		    for (int i2=low; i2<=high; i2++) {
			for (int xWidth=0; xWidth<width[low]-1; xWidth++) {
			    for (int xStart=1; xStart<width[low]-xWidth; xStart++) {
				A[xStart][xStart+xWidth][i1][i2] = A[0][xWidth][i1][i2];
				strCache[xStart][xStart+xWidth][i1][i2] = strCache[0][xWidth][i1][i2];
			    }
			}
		    }
		}
	    }
	    low++;
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

    protected long get(int collapse) {
	// otherwise, compute it
	return get(0, A.length-1, 0, A[0][0].length-1, collapse, this.cont);
    }

    /** debugging fields */
    private static long elapsed = 0;
    private static long starttime = 0;
    /**
     * <str> is a stream object representing the current sub-segment that we're operating on
     */
    protected long get(int x1, int x2, int y1, int y2, int collapse, SIRStream _str) {
	indent++;
	String callStr = "get(" + x1 + ", " + x2 + ", " + y1 + ", " + y2 + ", " + LinearPartitioner.COLLAPSE_STRING(collapse) + ", " + (_str==null ? "null" : _str.getIdent());
	/*
	if (x1==0 && x2==A.length-1) { 
	    for (int i=0; i<indent; i++) {System.err.print(" ");} 
	    System.err.println(callStr + " (spent " + (elapsed/1000) + " secs, or " + 
			       (((float)(100*elapsed))/((float)(System.currentTimeMillis()-starttime))) + "% in LinearAnalyzer.findLinearFilters)");
	}
	*/
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
	} else if (LinearPartitioner.tracingBack) {
	    // we should always hit memoized values on traceback
	    Utils.fail("Didn't find memoized value on traceback for " + callStr);
	}

	LinearAnalyzer lfa = partitioner.getLinearAnalyzer();

	SIRStream str;
	// always prefer ones that have a linear representation
	if (lfa.hasLinearRepresentation(_str) || lfa.isNonLinear(_str)) {
	    strCache[x1][x2][y1][y2][0] = _str;
	    str = _str;
	} else if (strCache[x1][x2][y1][y2][0]!=null ) {
	    str = strCache[x1][x2][y1][y2][0];
	} else if (_str!=null) {
	    strCache[x1][x2][y1][y2][0] = _str;
	    str = _str;
	} else {
	    str = null;
	    Utils.fail("null stream argument and no stream in cache.");
	}

	// if we are down to one child, then descend into child
	if (x1==x2 && y1==y2) {
	    long childCost = childConfig(x1, y1).get(collapse); 
	    Utils.assert(childCost>=0, "childCost = " + childCost);
	    A[x1][x2][y1][y2][collapse] = childCost;
	    debugMessage(" returning child cost, " + callStr + " = " + childCost);
	    indent--;
	    return childCost;
	}

	long cost;
	switch(collapse) {
	case LinearPartitioner.COLLAPSE_ANY: {
	    // if we still have flexibility, do better out of
	    // collapsing or not.  Important to do none first here
	    // because it will setup the linear information.
	    long none = get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_NONE, str);
	    cost = Math.min(none,
			    Math.min(get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_LINEAR, str),
				     get(x1, x2, y1, y2, LinearPartitioner.COLLAPSE_FREQ, str)));
	    break;
	}

	case LinearPartitioner.COLLAPSE_FREQ: {
	    if (!lfa.hasLinearRepresentation(str) || !LEETFrequencyReplacer.canReplace(str, lfa) || KjcOptions.nolinearcollapse 
		// currently don't support frequency implementation on raw
		|| KjcOptions.raw!=-1) {
		cost = Long.MAX_VALUE;
	    } else {
		// otherwise, return freq costn
		LinearFilterRepresentation l = lfa.getLinearRepresentation(str);
		cost = getScalingFactor(l, str) * l.getCost().getFrequencyCost();
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_LINEAR: {
	    if (!lfa.hasLinearRepresentation(str) || KjcOptions.nolinearcollapse) {
		// if we don't have a linear node, return infinity
		cost = Long.MAX_VALUE;
	    } else {
		SIRSplitJoin verticalObj = getVerticalObj(x1, x2, y1, y2, str);
		if (verticalObj!=null && verticalObj.getSplitter().getType()!=SIRSplitType.DUPLICATE) {
		    // also don't consider linear collapse of RR splitjoins -- it only adds overhead
		    cost = Long.MAX_VALUE;
		} else {
		    // otherwise, return cost of collapsed node
		    LinearFilterRepresentation l = lfa.getLinearRepresentation(str);
		    cost = getScalingFactor(l, str) * l.getCost().getDirectCost();
		}
	    }
	    break;
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    cost = Long.MAX_VALUE;

	    // see if we can do a vertical cut -- try getting what
	    // we'd do it on
	    SIRSplitJoin verticalObj = getVerticalObj(x1, x2, y1, y2, str);

	    // try a vertical cut if possible
	    if (verticalObj!=null) {
		// if uniform, do a uniform partition
		boolean allUniform = true;
		for (int i=y1; i<=y2; i++) {
		    allUniform = allUniform && uniform[i];
		}
		if (allUniform) {
		    debugMessage(" Trying uniform vertical cut.");
		    // don't even need to partition it... it has its own pieces already
		    long sum = 0;
		    for (int i=0; i<x2-x1+1; i++) {
			sum += get(x1+i, x1+i, y1, y2, LinearPartitioner.COLLAPSE_ANY, verticalObj.get(i));
		    }
		    cost = Math.min( cost, sum );
		} else {
		    for (int xPivot=x1; xPivot<x2; xPivot++) {
			// break along <xPivot>
			int[] arr = { 1 + (xPivot-x1), x2-xPivot };
			PartitionGroup pg = PartitionGroup.createFromArray(arr);
			SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)verticalObj, pg);
			cost = Math.min( cost, 
					 get(x1, xPivot, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(0)) +
					 get(xPivot+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(1)) );
		    }
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
		// if we have a pipeline, must try the horizontal cut
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

	    SIRContainer factored = null;
	    if (tryHoriz) {
		// try a horizontal cut
		for (int yPivot=y1; yPivot<y2; yPivot++) {
		    // break along <yPivot>
		    int[] arr = { 1 + (yPivot-y1), y2-yPivot };
		    PartitionGroup pg = PartitionGroup.createFromArray(arr);
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
		debugMessage(" Not trying horizontal cut.");
	    }

	    // do linear analysis here, now that children have been
	    // done.  In this way, we should never have to calculate
	    // for a set of children... only two children factoring
	    // into our own linear analysis.
	    if (lfa.hasLinearRepresentation(str)) {
		if (!lfa.hasLinearRepresentation(_str)) {
		    lfa.addLinearRepresentation(_str, lfa.getLinearRepresentation(str));
		}
	    } else if (lfa.hasLinearRepresentation(_str)) {
		if (!lfa.hasLinearRepresentation(str)) {
		    lfa.addLinearRepresentation(str, lfa.getLinearRepresentation(_str));
		}
	    } else if (lfa.isNonLinear(str)) {
		lfa.addNonLinear(_str);
	    } else if (lfa.isNonLinear(_str)) {
		lfa.addNonLinear(str);
	    } else if (factored!=null) {
		long time = System.currentTimeMillis();
		debugMessage("Getting linear rep for <factored> for first time:  [" + x1 + "][" + x2 + "][" + y1 + "][" + y2 + "] ... ");
		LinearAnalyzer.findLinearFilters(factored, KjcOptions.debug, lfa);
		elapsed += System.currentTimeMillis() - time;
		if (lfa.hasLinearRepresentation(factored)) {
		    lfa.addLinearRepresentation(str, lfa.getLinearRepresentation(factored));
		    if (!lfa.hasLinearRepresentation(_str)) {
			lfa.addLinearRepresentation(_str, lfa.getLinearRepresentation(factored));
		    }
		} else {
		    lfa.addNonLinear(str);
		    lfa.addNonLinear(_str);
		}
	    } else {
		long time = System.currentTimeMillis();
		debugMessage("Getting linear rep for <factored> for first time:  [" + x1 + "][" + x2 + "][" + y1 + "][" + y2 + "] ... ");
		LinearAnalyzer.findLinearFilters(str, KjcOptions.debug, lfa);
		elapsed += System.currentTimeMillis() - time;

		if (lfa.hasLinearRepresentation(str)) {
		    if (!lfa.hasLinearRepresentation(_str)) {
			lfa.addLinearRepresentation(_str, lfa.getLinearRepresentation(factored));
		    }
		} else {
		    lfa.addNonLinear(str);
		    if (!lfa.isNonLinear(_str)) {
			lfa.addNonLinear(_str);
		    }
		}
	    }
	    break;
	}
	    
	default: {
	    cost = -1;
	    Utils.fail("Unrecognized collapse value: " + collapse);
	}
	    
	}
	
	A[x1][x2][y1][y2][collapse] = cost;
	Utils.assert(cost>=0, "cost = " + cost);
	debugMessage(" returning " + callStr + " = " + cost);
	indent--;
	return cost;
    }

    /**
     * Return a splitjoin that a vertical cut could be performed on.
     * If it is impossible to perform a vertical cut, returns null.
     */
    private SIRSplitJoin getVerticalObj(int x1, int x2, int y1, int y2, SIRStream str) {
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
	
	// return obj if it's possible to do vert cut
	if (tryVertical) {
	    return verticalObj;
	} else {
	    return null;
	}
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
    protected StreamTransform traceback(int x1, int x2, int y1, int y2, int collapse, SIRStream _str) {
	// if we've exceeded the width of this node, then trim down to actual width
	int maxWidth = width[y1];
	for (int i=y1+1; i<=y2; i++) {
	    maxWidth = Math.max(maxWidth, width[i]);
	}
	if (x2>maxWidth-1) {
	    debugMessage("  scaling x2 back to " + (maxWidth-1));
	    x2 = maxWidth-1;
	}

	// when tracing back, everything should be cached
	SIRStream str = strCache[x1][x2][y1][y2][0];
	Utils.assert(str!=null);

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
		    if (LinearPartitioner.DEBUG) { System.err.println("Tracing back through ANY of " + cont.getName() + " and found best option = " + 
								      LinearPartitioner.COLLAPSE_STRING(options[i]) + " with cost " + get(x1,x2,y1,y2,options[i],str)); }
		    return traceback(x1, x2, y1, y2, options[i], str);
		}
	    }
	    Utils.fail("Didn't find traceback; was looking for ANY of " + cont + "[" + x1 + "][" + x2 + "][" + y1 + "][" + y2 + 
		       "][" + LinearPartitioner.COLLAPSE_STRING(collapse) + "] = " + A[x1][x2][y1][y2][collapse]);
	    break;
	}

	case LinearPartitioner.COLLAPSE_FREQ: {
	    recordChildPartitons(x1, x2, y1, y2);
	    return new FreqReplaceTransform(partitioner.getLinearAnalyzer());
	}
	    
	case LinearPartitioner.COLLAPSE_LINEAR: {
	    recordChildPartitons(x1, x2, y1, y2);
	    return new LinearReplaceTransform(partitioner.getLinearAnalyzer());
	}

	case LinearPartitioner.COLLAPSE_NONE: {
	    // see if we can do a vertical cut -- get what we would do it on
	    SIRSplitJoin verticalObj = getVerticalObj(x1, x2, y1, y2, str);

	    // try a vertical cut if possible
	    if (verticalObj!=null) {
		// if uniform, do a uniform partition
		boolean allUniform = true;
		for (int i=y1; i<=y2; i++) {
		    allUniform = allUniform && uniform[i];
		}
		if (allUniform) {
		    // get the sum of components to compare it to memoized value
		    long sum = 0;
		    for (int i=x1; i<=x2; i++) {
			sum += get(i, i, y1, y2, LinearPartitioner.COLLAPSE_ANY, null);
		    }
		    if (sum==A[x1][x2][y1][y2][collapse]) {
			// found the optimum...
			debugMessage(" Found uniform cut on traceback.");
			// generate transform
			// have to remove sync if there is any
			StreamTransform result = new IdentityTransform();
			// recurse
			for (int i=x1; i<=x2; i++) {
			    result.addSucc(traceback(i, i, y1, y2, LinearPartitioner.COLLAPSE_ANY, verticalObj.get(i)));
			    if (i!=x2) { numAssigned++; }
			}
			if (y1<y2) {
			    StreamTransform sync = new RemoveSyncTransform();
			    sync.addSucc(result);
			    result = sync;
			}
			// all done
			return result.reduce();
		    }
		} else {
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
			    // for dot output
			    numAssigned++;
			    result.addSucc(traceback(x1+1, x2, y1, y2, LinearPartitioner.COLLAPSE_ANY, sj.get(1)));
			    
			    // Here we have a pipeline.  If y1==y2
			    // then we don't need the sync removal,
			    // but we DO need an identity to unwrap
			    // the pipeline.  If y1<y2, need sync
			    // removal.
			    if (y1<y2) {
				StreamTransform newResult = new RemoveSyncTransform();
				newResult.addSucc(result);
				result = newResult;
			    } else {
				StreamTransform newResult = new IdentityTransform();
				newResult.addSucc(result);
				result = newResult;
			    }

			    // all done
			    return result.reduce();
			}
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
		    // for dot output
		    numAssigned++;
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
     * For sake of dot output, records which partitions all children
     * are assigned to.
     */
    private void recordChildPartitons(int x1, int x2, int y1, int y2) {
	for (int j=y1; j<=y2; j++) {
	    for (int i=x1; i<=Math.min(x2,width[j]-1); i++) {
		IterFactory.createFactory().createIter(childConfig(i,j).getStream()).accept(new EmptyStreamVisitor() {
			public void visitFilter(SIRFilter self, SIRFilterIter iter) {
			    LDPConfig.partitions.put(self, new Integer(LDPConfig.numAssigned));
			}
		    });
	    }
	}
    }

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
			    System.err.print(getStream().getIdent() + "[" + i1 + "][" + i2 + "][" + i3 + "][" + i4 + "][" + 
					     LinearPartitioner.COLLAPSE_STRING(i5) + "] = ");
			    if (A[i1][i2][i3][i4][i5]==Long.MAX_VALUE) {
				System.err.println("INFINITY");
			    } else {
				System.err.println(A[i1][i2][i3][i4][i5]);
			    }
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

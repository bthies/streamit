package at.dms.kjc.sir.lowering;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

public class Partitioner {
    /**
     * The toplevel stream we're operating on.
     */
    private SIRStream str;
    /**
     * The target number of tiles this partitioner is going for.
     */
    private final int target;
    
    private Partitioner(SIRStream str, int target) {
	this.str = str;
	this.target = target;
    }
    
    /**
     * Tries to adjust <str> into <num> pieces of equal work.
     */
    public static void doit(SIRStream str, int target) {
	new Partitioner(str, target).doit();
    }

    private void doit() {
	// dump the orig graph
	StreamItDot.printGraph(str, "before.dot");
	// propagate fields to make sure we can resolve the constants
	// in work functions
	FieldProp.doPropagate(str);
	// if we have too few tiles, then fizz the big ones
	int count = new RawFlattener(str).getNumTiles();
	if (count < target) {
	    fissAll();
	} else {
	    fuseAll();
	}
	// dump the final graph
	StreamItDot.printGraph(str, "after.dot");
    }

    /**
     * Do all the fission we can.
     */
    private void fissAll() {
	// get work sorted by work
	WorkList sorted = WorkEstimate.getWorkEstimate(str).getSortedFilterWork();
	// keep track of the position of who we've tried to split
	int pos = sorted.size()-1;

	while (pos>=0) {
	    // make raw flattener for latest version of stream graph
	    RawFlattener flattener = new RawFlattener(str);
	    // get work at <pos>
	    int work = sorted.getWork(pos);
	    // make a list of candidates that have the same amount of work
	    LinkedList candidates = new LinkedList();
	    while (sorted.getWork(pos)==work && pos>=0) {
		candidates.add(sorted.getFilter(pos));
		pos--;
	    }
	    // try fissing the candidates
	    if (canFiss(candidates, flattener)) {
		doFission(candidates);
	    } else {
		// if illegal, quit
		break;
	    }
	}
    }

    /**
     * Returns whether or not it's legal to fiss <candidates>
     */
    private boolean canFiss(LinkedList candidates, RawFlattener flattener) {
	// count the number of new tiles required
	int newTiles = 0;
	// go through the candidates and count tiles, check that can
	// be duplicated
	for (int i=0; i<candidates.size(); i++) {
	    SIRFilter filter = (SIRFilter)candidates.get(i);
	    // check fissable
	    if (!StatelessDuplicate.isFissable(filter)) {
		return false;
	    }
	    // count new copies of the filter
	    newTiles += tilesForFission(filter, flattener, 2);
	}
	// return whether or not there's room for candidates
	return (flattener.getNumTiles()+newTiles<=target);
    }

    /**
     * Returns the number of NEW tiles that will be required under the
     * current configuration of <str> if <filter> is split into <ways>
     * copies.
     */
    private int tilesForFission(SIRFilter filter, RawFlattener flattener, int ways) {
	int result = 0;
	// add tiles for the copies of <filter>
	result += ways-1;

	// add a tile for the joiner if it's not followed by a joiner
	// or a null
	FlatNode successor = flattener.getFlatNode(filter).edges[0];
	if (successor!=null && !(successor.contents instanceof SIRJoiner)) {
	    result += 1;
	}
	
	return result;
    }

    /**
     * Split everyone in <candidates> two ways.
     */
    private void doFission(LinkedList candidates) {
	for (int i=0; i<candidates.size(); i++) {
	    SIRFilter filter = (SIRFilter)candidates.get(i);
	    StatelessDuplicate.doit(filter, 2);
	    // constant prop through new filters
	    ConstantProp.propagateAndUnroll(filter.getParent());
	    //FieldProp.doPropagate((lowPass.getParent()));
	}
    }

    /**
     * Do all the fusion we have to do until we're within our target
     * number of filters.
     */
    private void fuseAll() {
	// how many tiles we have occupied
	int count = 0;
	// whether or not we're done trying to fuse
	boolean done = false;
	do {
	    // get how many tiles we have
	    RawFlattener flattener = new RawFlattener(str);
	    count = flattener.getNumTiles();
	    if (count>target) {
		// get the containers in the order of work for filters
		// immediately contained
		WorkList list = WorkEstimate.getWorkEstimate(str).getSortedContainerWork();
		// work up through this list until we fuse something
		for (int i=0; i<list.size(); i++) {
		    SIRContainer cont = list.getContainer(i);
		    if (cont instanceof SIRSplitJoin) {
			System.out.println("trying to fuse split " + cont);
			SIRStream newstr = FuseSplit.fuse((SIRSplitJoin)cont);
			// if we fused something, quit loop
			if (newstr!=cont) { break; }
		    } else if (cont instanceof SIRPipeline) {
			System.out.println("trying to fuse pipe " + cont);
			int num = FusePipe.fuse((SIRPipeline)cont);
			if (num!=0) { break; }
		    }
		    // if we made it to the end of the last loop, then
		    // we're done trying to fuse (we didn't reach target.)
		    if (i==list.size()-1) { done = true; }
		}
	    }
	} while (count>target && !done);
    }

}

package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

public class GreedyPartitioner {
    /**
     * The toplevel stream we're operating on.
     */
    private SIRStream str;
    /**
     * Most recent work estimate for this.
     */
    private WorkEstimate work;
    /**
     * The target number of tiles this partitioner is going for.
     */
    private int target;
    
    //How many times to attempt fissing till use up all space
    private final int MAX_FISS_ITER=5;

    public GreedyPartitioner(SIRStream str, WorkEstimate work, int target) {
	this.str = str;
	this.target = target;
	this.work = work;
    }

    public void toplevelFission(int startingTileCount) {
	int count = startingTileCount;
	for(int i=0;i<MAX_FISS_ITER;i++) {
	    fissAll();
	    System.out.print("count tiles again... ");
	    count = new RawFlattener(str).getNumTiles();
	    System.out.println("done:"+count);
	    if(count>=target)
		break;
	}
	if(count>target) {
	    fuseAll();
	}
    }
    
    public void toplevelFusion() {
	fuseAll();
    }

    /*************************************************************************/

    private void aggressiveFiss() {
	// get work sorted by work
	WorkList sorted=work.getSortedFilterWork();
	//System.out.println("Sorted:"+sorted);
	//int lowestFissable=-1;
	//Find smallest fissable filter
	for(int i=sorted.size()-1;i>=0;i--) {
	    SIRFilter filter=sorted.getFilter(i);
	    if(StatelessDuplicate.isFissable(filter)) {
		RawFlattener flattener = new RawFlattener(str);
		int dec=tilesForFission(filter, flattener, 2);
		target-=dec;
		if(fuseAll()<=target) {
		    if(StatelessDuplicate.isFissable(filter)) {
			StatelessDuplicate.doit(filter, 2);
			ConstantProp.propagateAndUnroll(filter.getParent());
		    }
		}
		target+=dec;
		break;
	    }
	}
	//fuseAll();
    }

    /**
     * Do all the fission we can.
     */
    private void fissAll() {
	// get work sorted by work
	WorkList sorted = work.getSortedFilterWork();
	//System.out.println("Sorted:"+sorted);
	// keep track of the position of who we've tried to split
	int pos = sorted.size()-1;

	while (pos>=0) {
	    if((sorted.getFilter(pos) instanceof SIRIdentity)||
	       (sorted.getFilter(pos).getIdent().startsWith("FileWriter"))) { //Hack to not fuse file writer
		pos--;
		continue;
	    }
	    StreamItDot.printGraph(str, "during-fission.dot");
	    // make raw flattener for latest version of stream graph
	    RawFlattener flattener = new RawFlattener(str);
	    // get work at <pos>
	    int work = sorted.getWork(pos);
	    // make a list of candidates that have the same amount of work
	    LinkedList candidates = new LinkedList();
	    //System.out.println("Trying:"+pos+" "+sorted.getFilter(pos));
	    while (pos>=0 && sorted.getWork(pos)==work) {
		candidates.add(sorted.getFilter(pos));
		pos--;
	    }
	    // try fissing the candidates
	    if (canFiss(candidates, flattener)) {
		doFission(candidates);
	    } /*else {
		// if illegal, quit
		break;
		}*/
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
	// or a nullshow

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
    private int fuseAll() {
	// how many tiles we have occupied
	int count = 0;
	// whether or not we're done trying to fuse
	boolean done = false;
	int aggressive=0;
	do {
	    // get how many tiles we have
	    RawFlattener flattener = new RawFlattener(str);
	    count = flattener.getNumTiles();
	    System.out.println("Partitioner detects " + count + " tiles.");
	    StreamItDot.printGraph(str, "during-fusion.dot");
	    if (count>target) {
		//boolean tried=false;
		// make a fresh work estimate
		this.work = WorkEstimate.getWorkEstimate(str);
		// get the containers in the order of work for filters
		// immediately contained
		WorkList list = work.getSortedContainerWork();
		//System.out.println(list);
		// work up through this list until we fuse something
		for (int i=0; i<list.size(); i++) {
		    SIRContainer cont = list.getContainer(i);
		    if (cont instanceof SIRSplitJoin) {
			System.out.println("trying to fuse " + cont.size() + "-way split " + ((SIRSplitJoin)cont).getName());
			List children=cont.getChildren();
			if(aggressive==0) {
			    boolean attempt=true;
			    for(int j=0;j<children.size();j++) {
				SIROperator child=(SIROperator)children.get(j);
				if((!(child instanceof SIRSplitter))&&(!(child instanceof SIRJoiner))&&(child instanceof SIRIdentity)) {
				    //System.out.println("FOUND:"+children.get(j));
				    attempt=false;
				}
			    }
			    if(attempt) {
				SIRStream newstr = FuseSplit.semiFuse((SIRSplitJoin)cont);
				// if we fused something, quit loop
				if (newstr!=cont) { 
				    aggressive=0;
				    //tried=true;
				    break;
				}
			    } /*else
				System.out.println("SplitJoin has Identity Child! Ignoring.");*/
			} else if(aggressive==1) {
			    boolean attempt=false;
			    for(int j=0;j<children.size();j++) {
				SIROperator child=(SIROperator)children.get(j);
				if((!(child instanceof SIRSplitter))&&(!(child instanceof SIRJoiner))&&!(child instanceof SIRIdentity)) {
				    //System.out.println("FOUND:"+children.get(j));
				    attempt=true;
				}
			    }
			    if(attempt) {
				SIRStream newstr = FuseSplit.semiFuse((SIRSplitJoin)cont);
				// if we fused something, quit loop
				if (newstr!=cont) { 
				    aggressive=0;
				//tried=true;
				    break;
				}
			    } /*else
				System.out.println("SplitJoin has all Identity Children! Ignoring.");*/
			} else {
			    SIRStream newstr = FuseSplit.semiFuse((SIRSplitJoin)cont);
			    // if we fused something, quit loop
			    if (newstr!=cont) { 
				aggressive=0;
				//tried=true;
				break;
			    }
			}
		    } else if (cont instanceof SIRPipeline) {
			System.out.println("trying to fuse " + (count-target) + " from " 
					   + cont.size() + "-long pipe " + ((SIRPipeline)cont).getName());
			int num = FusePipe.fuse((SIRPipeline)cont, count-target);
			// try lifting
			Lifter.eliminatePipe((SIRPipeline)cont);
			if (num!=0) {
			    aggressive=0;
			    break;
			}
		    }
		    // if we made it to the end of the last loop, then
		    // we're done trying to fuse (we didn't reach target.)
		    if (i==list.size()-1) { 
			if(aggressive>=2)
			    done=true;
			else
			    //if(!tried)
			    aggressive++;
		    }
		}
	    }
	} while (count>target && !done);
	return count;
    }
}


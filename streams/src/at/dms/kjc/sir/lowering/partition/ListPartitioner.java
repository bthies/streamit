package at.dms.kjc.sir.lowering.partition;

import java.rmi.RemoteException;

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

/**
 * This is a partitioner that keeps a canonical list of underlying
 * nodes to help with partitioning.
 */
public abstract class ListPartitioner {
    /**
     * The toplevel stream we're operating on.
     */
    protected SIRStream str;
    /**
     * The target number of tiles this partitioner is going for.
     */
    protected int numTiles;

    /**
     * List of NODES (i.e., filters and joiners) in the stream graph.
     * This list is in the "canonicalized order" (see lp-partition
     * document.)
     */
    protected LinkedList nodes;
    /**
     * Maps a stream container (pipeline, splitjoin, feedbackloop) to
     * an Integer denoting the first index in <nodes> that belongs to
     * the structure.
     */
    protected HashMap first;
    /**
     * Maps a stream container (pipeline, splitjoin, feedbackloop) to
     * an Integer denoting the first index in <nodes> that belongs to
     * the structure.
     */
    protected HashMap last;
    /**
     * The work estimate of the stream.
     */
    protected WorkEstimate work;

    public ListPartitioner(SIRStream str, WorkEstimate work, int numTiles) {
	this.str = str;
	this.work = work;
	this.numTiles = numTiles;
	this.nodes = new LinkedList();
	this.first = new HashMap();
	this.last = new HashMap();
	buildNodesList();
    }

    public int getNumTiles() {
	return numTiles;
    }

    public WorkEstimate getWorkEstimate() {
	return work;
    }

    /**
     * Constructs <nodes>, <first> and <last> out of <str>.
     */
    private void buildNodesList() {
	// add dummy start node
	nodes.add(new DummyNode());

	// add nodes in stream
	SIRIterator it = IterFactory.createFactory().createIter(str);
	it.accept(new EmptyStreamVisitor() {

		public void preVisitStream(SIRStream self,
					   SIRIterator iter) {
		    first.put(self, new Integer(nodes.size()));
		}

		public void postVisitStream(SIRStream self,
					    SIRIterator iter) {
		    last.put(self, new Integer(nodes.size()-1));
		}

		public void visitFilter(SIRFilter self,
					SIRFilterIter iter) {
		    preVisitStream(self, iter);
		    nodes.add(self);
		    postVisitStream(self, iter);
		}

		public void preVisitFeedbackLoop(SIRFeedbackLoop self,
						 SIRFeedbackLoopIter iter) {
		    super.preVisitFeedbackLoop(self, iter);
		    nodes.add(self.getJoiner());
		}
		
		public void postVisitSplitJoin(SIRSplitJoin self,
					       SIRSplitJoinIter iter) {
		    nodes.add(self.getJoiner());
		    super.postVisitSplitJoin(self, iter);
		}
	    });

	// add dummy end node
	nodes.add(new DummyNode());
    }

    class DummyNode extends Object {}

    /**
     * Returns whether or not <str1> and <str2> are equivalent for the
     * purposes of constraining symmetrical partitioning in them.
     */
    protected boolean equivStructure(SIRStream str1, SIRStream str2) {
	// get starting positions
	int first1 = ((Integer)first.get(str1)).intValue();
	int first2 = ((Integer)first.get(str2)).intValue();
	// get sizes
	int size1 =  ((Integer)last.get(str1)).intValue() - first1;
	int size2 = ((Integer)last.get(str2)).intValue() - first2;
	if (size1 != size2) {
	    return false;
	}

	// compare work in streams
	for (int i=0; i<size1; i++) {
	    Object o1 = nodes.get(first1+i);
	    Object o2 = nodes.get(first2+i);
	    // compare types
	    if (o1 instanceof SIRFilter && o2 instanceof SIRFilter) {
		int work1 = work.getWork((SIRFilter)o1);
		int work2 = work.getWork((SIRFilter)o2);
		if (work1!=work2) {
		    /*
		    System.err.println("  failed because " + o1 + " has work " + work1 + 
				       " but " + o2 + " has work " + work2);
		    */
		    return false;
		}
	    } else if (o1 instanceof SIRJoiner &&  o2 instanceof SIRJoiner) {
		continue;
	    } else {
		return false;
	    }
	}

	return true;
    }

}

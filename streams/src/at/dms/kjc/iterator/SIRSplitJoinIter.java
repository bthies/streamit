package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import at.dms.util.*;
import streamit.scheduler2.iriter.*;

public class SIRSplitJoinIter extends SIRIterator implements SplitJoinIter {

    /**
     * Object pointed to by this iterator.
     */
    protected SIRSplitJoin obj;

    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRSplitJoinIter(IterFactory _factory, SIRSplitJoin obj) {
	super(_factory);
	this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRSplitJoinIter(IterFactory _factory, SIRSplitJoin obj, SIRIterator parent, int pos) {
	super(_factory, parent, pos);
	this.obj = obj;
    }

    public SplitJoinIter isSplitJoin() {
	return this;
    }

    /**
     * Return the stream pointed to by this.
     */
    public SIRStream getStream() {
	checkValidity();
	return obj;
    }

    public int getNumChildren () {
	return obj.size();
    }

    public Iterator getChild (int n) {
	return factory.createIter(obj.get(n), this, n);
    }

    /**
     * Same as getChild with different signature.
     */
    public SIRIterator get (int i) {
	return (SIRIterator)getChild(i);
    }
    

    /**
     * Returns the number of ways this Splitter splits data.
     * @return return Splitter fan-out
     */
    public int getFanOut () {
	return obj.size();
    }
    
    /**
     * Returns the number of work functions for this Splitter.
     * @return number of work functions for this Splitter
     */
    public int getSplitterNumWork () {
	return 1;
    }

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Splitter of this Stream.  The 
     * distribution is simply an array of ints, with numChildren 
     * elements.  The value at index 0 corresponds to number of items 
     * pushed out to 1st child, etc.
     * @return distribution of weights on a particular invocation
     * of work function for the Splitter of this Stream.
     */
    public int[] getSplitPushWeights (int nWork) {
	return obj.getSplitter().getWeights();
    }
    
    /**
     * Returns number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.  These are the
     * items that will end up being pushed out to the children of this
     * Stream.
     * @return number of data items consumed by a particular invocation
     * of work function for Splitter of this Stream.
     */
    public int getSplitPop (int nWork) {
	if (obj.getSplitter().getType()==SIRSplitType.DUPLICATE) {
	    return 1;
	} else if (obj.getSplitter().getType()==SIRSplitType.NULL) {
	    return 0;
	} else {
	    return obj.getSplitter().getSumOfWeights();
	}
    }

    /**
     * Returns the number of ways this Joiner joines data.
     * @return return Joiner fan-in
     */
    public int getFanIn () {
	return obj.size();
    }
    
    /**
     * Returns the number of work functions for the Joiner
     * of this Stream.
     * @return number of work functions for the JOiner of this 
     * Stream
     */
    public int getJoinerNumWork () {
	return 1;
    }

    /**
     * Returns n-th work function associated with this Splitter.
     * @return n-th work function for the Splitter
     */
    public Object getSplitterWork (int nWork) {
	return SIRSplitter.WORK_FUNCTION;
    }

    /**
     * Returns n-th work function associated with this Joiner.
     * @return n-th work function for the Joiner
     */
    public Object getJoinerWork(int nWork) {
	return SIRJoiner.WORK_FUNCTION;
    }

    /**
     * Returns distribution of weights on a particular invocation
     * of work function for Joiner of this SplitJoin.  The 
     * distribution is simply an array of ints, with numChildren 
     * elements.  The value at index 0 corresponds to number of items 
     * popped from 1st child, etc.
     * @return distribution of weights on a particular invocation
     * of work function for Joiner of this SplitJoin.
     */
    public int[] getJoinPopWeights (int nWork) {
	return obj.getJoiner().getWeights();
    }
    
    /**
     * Returns number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.  These are the
     * items that were consumed from various children of this SplitJoin.
     * @return number of data items produced by a particular invocation
     * of work function for Joiner of this SplitJoin.
     */
    public int getJoinPush (int nWork) {
	SIRJoiner joiner = obj.getJoiner();
	if (joiner.getType()==SIRJoinType.NULL) {
	    return 0;
	} else {
	    return joiner.getSumOfWeights();
	}
    }

    public void accept(StreamVisitor v) {
	v.preVisitSplitJoin(obj, this);
	for (int i=0; i<getNumChildren(); i++) {
	    ((SIRIterator)getChild(i)).accept(v);
	}
	v.postVisitSplitJoin(obj, this);
    }

    /**
     * This function is needed by the scheduler, but isn't useful from
     * the compiler.
     */
    public Iterator getUnspecializedIter() {
	return this;
    }
}

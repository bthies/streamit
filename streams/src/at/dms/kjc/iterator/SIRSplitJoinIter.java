package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import streamit.scheduler.iriter.*;

class SIRSplitJoinIter extends SIRIterator implements SplitJoinIter {

    /**
     * Object pointed to by this iterator.
     */
    private SIRSplitJoin obj;

    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRSplitJoinIter(SIRSplitJoin obj) {
	this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRSplitJoinIter(SIRSplitJoin obj, SIRIterator parent, int pos) {
	super(parent, pos);
	this.obj = obj;
    }

    public SplitJoinIter isSplitJoin() {
	return this;
    }

    /**
     * Returns the object the iterator points to.
     */
    public Object getObject() {
	checkValidity();
	return obj;
    }

    public int getNumChildren () {
	return obj.size();
    }

    public Iterator getChild (int n) {
	return IterFactory.createIter(obj.get(n), this, n);
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
	new RuntimeException("not implemented yet").printStackTrace();
	return -1;
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
	new RuntimeException("not implemented yet").printStackTrace();
	return -1;
    }
}

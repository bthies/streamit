package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import at.dms.util.*;
import streamit.scheduler.iriter.*;

public class SIRFeedbackLoopIter extends SIRIterator implements FeedbackLoopIter {

    /**
     * Object pointed to by this iterator.
     */
    private SIRFeedbackLoop obj;
    
    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRFeedbackLoopIter(SIRFeedbackLoop obj) {
	this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRFeedbackLoopIter(SIRFeedbackLoop obj, SIRIterator parent, int pos) {
	super(parent, pos);
	this.obj = obj;
    }

    public FeedbackLoopIter isFeedbackLoop() {
	return this;
    }

    /**
     * Return the stream pointed to by this.
     */
    public SIRStream getStream() {
	checkValidity();
	return obj;
    }

    /**
     * Returns delay of feedbackloop
     */ 
    public int getDelaySize() {
	return obj.getDelayInt();
    }

    /**
     * Returns an iterator for the body of the FeedbackLoop.
     * @return iterator for the body of the FeedbackLoop
     */
    public Iterator getBodyChild () { 
	return IterFactory.createIter(obj.getBody(),
				      this,
				      SIRFeedbackLoop.BODY);
    }

    /**
     * Returns an iterator for the loop of the FeedbackLoop.
     * @return iterator for the loop of the FeedbackLoop
     */
    public Iterator getLoopChild () { 
	return IterFactory.createIter(obj.getLoop(),
				      this,
				      SIRFeedbackLoop.LOOP);
    }

    public SIRIterator getLoop() {
	return IterFactory.createIter(obj.getLoop(),
				      this,
				      SIRFeedbackLoop.LOOP);
    }

    public SIRIterator getBody() {
	return IterFactory.createIter(obj.getBody(),
				      this,
				      SIRFeedbackLoop.BODY);
    }

    /**
     * Same as above with different signature
     */
    public SIRIterator get (int i) {
	if (i==SIRFeedbackLoop.LOOP) {
	    return (SIRIterator)getLoop();
	} else if (i==SIRFeedbackLoop.BODY) {
	    return (SIRIterator)getBody();
	} else {
	    Utils.fail("bad arg to get");
	    return null;
	}
    }

    /**
     * Returns the number of ways this Splitter splits data.
     * @return return Splitter fan-out
     */
    public int getFanOut () {
	return 2;
    }
    
    /**
     * Returns the number of work functions for this Splitter.
     * @return number of work functions for this Splitter
     */
    public int getSplitterNumWork () {
	return 1;
    }

    /**
     * Returns n-th work function associated with this Splitter.
     * @return n-th work function for the Splitter
     */
    public Object getSplitterWork (int nWork) {
	Utils.fail("todo - still need to implement");
	return null;
    }

    /**
     * Returns n-th work function associated with this Joiner.
     * @return n-th work function for the Joiner
     */
    public Object getJoinerWork(int nWork) {
	Utils.fail("todo - still need to implement");
	return null;
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
	return 2;
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

    public void accept(StreamVisitor v) {
	v.preVisitFeedbackLoop(obj, this);
	((SIRIterator)getBody()).accept(v);
	((SIRIterator)getLoop()).accept(v);
	v.postVisitFeedbackLoop(obj, this);
    }

    /**
     * This function is needed by the scheduler, but isn't useful from
     * the compiler.
     */
    public Iterator getUnspecializedIter() {
	return this;
    }
}

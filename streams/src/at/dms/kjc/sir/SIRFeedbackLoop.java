package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.lir.LIRStreamType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

/**
 * This represents a feedback loop construct.
 */
public class SIRFeedbackLoop extends SIRContainer implements Cloneable {
    /**
     * These constants are used for addressing the children of a
     * feedback loop.
     */
    public static final int BODY = 0;
    public static final int LOOP = 1;

    /**
     * The joiner, which appears at the top of the feedback loop.
     */
    private SIRJoiner joiner;
    /**
     * The splitter, which appears at the bottom of the feedback loop.
     */
    private SIRSplitter splitter;
    /**
     * The delay of this, which is the number of inputs that are drawn
     * from the initPath function before reading them from the loop
     * tape.
     */
    private JExpression delay;
    /**
     * The function that generates initial items appearing on the
     * feedback loop.  It should input an int i and return the i'th
     * item to be read by the joiner.
     */
    private JMethodDeclaration initPath;

    /**
     * Construct a new SIRPipeline null fields, parent, and methods
     */
    public SIRFeedbackLoop() {
	super();
	// need this to populate the children list
	add(null);
	add(null);
    }
    
    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRFeedbackLoop(SIRContainer parent,
			   String ident,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods) {
	super(parent, ident, fields, methods);
	// need this to populate the children list
	add(null);
	add(null);
    }

    /**
     * Construct a new SIRPipeline with empty fields and methods.
     */
    public SIRFeedbackLoop(SIRContainer parent,
			   String ident) {
	this(parent, ident, JFieldDeclaration.EMPTY(), JMethodDeclaration.EMPTY() );
    }

    /**
     * Returns the output type of this.
     */
    public CType getOutputType() {
	// return output type of body
	return getBody().getOutputType();
    }
    
    /**
     * Returns the input type of this.
     */
    public CType getInputType() {
	// return input type of body
	return getBody().getInputType();
    }

    public int getPushForSchedule(HashMap[] counts) {
	// get what the body pushes
	int bodyPush = getBody().getPushForSchedule(counts);
	// scale it by what is lost in the splitter
	if (splitter.getType()==SIRSplitType.DUPLICATE) {
	    return bodyPush;
	} else {
	    int[] weights = splitter.getWeights();
	    // scale by what is passed out of construct
	    Utils.assert((bodyPush * weights[0]) % (weights[0] + weights[1]) == 0,
			 "Found non-integral splitting ratio in steady-state feedbackloop schedule.");
	    return bodyPush * weights[0] / (weights[0] + weights[1]);
	}
    }

    public int getPopForSchedule(HashMap[] counts) {
	// get what body pops
	int bodyPop = getBody().getPopForSchedule(counts);
	// scale it by what is channeled through the joiner
	int[] weights = joiner.getWeights();
	// scale by what is passed out of construct
	Utils.assert((bodyPop * weights[0]) % (weights[0] + weights[1]) == 0,
		     "Found non-integral joining ratio in steady-state feedbackloop schedule.");
	return bodyPop * weights[0] / (weights[0] + weights[1]);
    }

    /**
     * Returns the type of this stream.
     */
    public LIRStreamType getStreamType() {
	return LIRStreamType.LIR_FEEDBACK_LOOP;
    }

    /**
     * Returns the relative name by which this object refers to child
     * <child>, or null if <child> is not a child of this.
     */
    public String getChildName(SIROperator str) {
	if (str==joiner) {
	    // return joiner
	    return "joiner";
	} else if (str==splitter) {
	    // return splitter
	    return "splitter";
	} else if (str==getBody()) {
	    // return body
	    return "body";
	} else if (str==getLoop()) {
	    // return loop
	    return "loop";
	} else {
	    // otherwise, <str> is not a child--return null
	    return null;
	}
    }

    /**
     * See documentation in SIRContainer.
     */
    public void replace(SIRStream oldStr, SIRStream newStr) {
	newStr.setParent(this);
	if (getBody()==oldStr) {
	    setBody(newStr);
	} else if (getLoop()==oldStr) {
	    setLoop(newStr);
	} else {
	    Utils.fail("Trying to replace child " + oldStr + " of " + this
		       + " but this doesn't contain the child.");
	}
    }

    // reset splits and joins to have right number of elements.
    public void rescale() {
	this.splitter.rescale(2);
	this.joiner.rescale(2);
    }

    /**
     * Returns a list of the children of this.  The children are
     * stream objects that are contained within this.
     */
    public List getChildren() {
	// build result
	LinkedList result = new LinkedList();
	// add the children: the joiner, splitter, body, and loop
	result.add(joiner);
	result.add(getBody());
	result.add(splitter);
	result.add(getLoop());
	// return result
	return result;
    }

    public void reclaimChildren() {
	super.reclaimChildren();
	splitter.setParent(this);
	joiner.setParent(this);
    }

    /**
     * Overrides SIRStream.getSuccessor.  The loop stream should have
     * the joiner as its successor.  All others are in order set by
     * <children>.
     */
    public SIROperator getSuccessor(SIRStream child) {
	if (child==getLoop()) {
	    return joiner;
	} else {
	    return super.getSuccessor(child);
	}
    }

    /**
     * Returns a list of tuples (two-element arrays) of SIROperators,
     * representing a tape from the first element of each tuple to the
     * second.
     */
    public List getTapePairs() {
	// construct result
	SIROperator[][] entries
	    = { {joiner, getBody()},	// connect joiner and body
		{getBody(), splitter},   // connect body and splitter
		{splitter, getLoop()},   // connect splitter and loop
		{getLoop(), joiner} };	// connect loop and joiner
	// return as list
	return Arrays.asList(entries);
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitFeedbackLoop(this,
				   fields,
				   methods,
				   init,
				   initPath);
    }

    /**
     * Set the Body of the feedback loop, and the parent of <body> to
     * this.
     **/
    public void setBody(SIRStream body) 
    {
	myChildren().set(BODY, body);
	body.setParent(this);
    }
    /**
     * Set the Body of the feedback loop 
     **/
    public void setLoop(SIRStream loop) 
    {
	myChildren().set(LOOP, loop);;
	loop.setParent(this);
    }
    /**
     * Set the Joiner of the feedback loop, and sets the parent of
     * <joiner> to be this.
     **/
    public void setJoiner(SIRJoiner joiner) 
    {
	this.joiner = joiner;
	joiner.setParent(this);
    }
    /**
     * Set the Splitter of the feedback loop, and sets the parent of
     * <splitter> to be this.
     **/
    public void setSplitter(SIRSplitter splitter) 
    {
	this.splitter = splitter;
	splitter.setParent(this);
    }
    /**
     * Set the delay of the feedback loop 
     **/
    public void setDelay(JExpression delay) 
    {
	this.delay = delay;
    }
    /**
     * get the delay of the feedback loop 
     **/
    public JExpression getDelay() 
    {
	return this.delay;
    }

    /**
     * Retrieve the delay of this as an int (requires that constants
     * have been propagated).
     */
    public int getDelayInt() {
	if (!(delay instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for DELAY value, " + 
		       "but the constant hasn't been resolved yet.  " + 
		       "It is of class " + delay.getClass());
	}
	return ((JIntLiteral)delay).intValue();
    }

    /**
     * Whether or not <str> is an immediate child of this.
     */
    public boolean contains(SIROperator str) {
	return str==getBody() || str==getLoop() || str==joiner || str==splitter;
    }

    /**
     * Set the Init Path method  of the feedback loop 
     **/
    public void setInitPath(JMethodDeclaration newInitPath)
    {
	addReplacementMethod(newInitPath, this.initPath);
	this.initPath= newInitPath;
    }

    /**
     * Returns body of this.
     */
    public SIRStream getBody() {
	return (SIRStream)myChildren().get(BODY);
    }
    
    /**
     * Returns loop of this.
     */
    public SIRStream getLoop() {
	return (SIRStream)myChildren().get(LOOP);
    }
    
    /**
     * Returns joiner of this.
     */
    public SIRJoiner getJoiner() {
	return joiner;
    }
    
    /**
     * Returns splitter of this.
     */
    public SIRSplitter getSplitter() {
	return splitter;
    }

    /**
     * Returns the path-initialization function of this.
     */
    public JMethodDeclaration getInitPath() {
	return initPath;
    }

    public String toString() {
	return "SIRFeedbackLoop name=" + getName();
    }


/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRFeedbackLoop other = new at.dms.kjc.sir.SIRFeedbackLoop();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRFeedbackLoop other) {
  super.deepCloneInto(other);
  other.joiner = (at.dms.kjc.sir.SIRJoiner)at.dms.kjc.AutoCloner.cloneToplevel(this.joiner, other);
  other.splitter = (at.dms.kjc.sir.SIRSplitter)at.dms.kjc.AutoCloner.cloneToplevel(this.splitter, other);
  other.delay = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.delay, other);
  other.initPath = (at.dms.kjc.JMethodDeclaration)at.dms.kjc.AutoCloner.cloneToplevel(this.initPath, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

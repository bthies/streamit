package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.lir.LIRStreamType;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

/**
 * This represents a feedback loop construct.
 */
public class SIRFeedbackLoop extends SIRContainer implements Cloneable {
    /**
     * The body of this, which appears in the forward path through the
     * feedback loop.
     */
    private SIRStream body;
    /**
     * The loop contents of this, which appears in the backwards path
     * from the splitter to the joiner.
     */
    private SIRStream loop;
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
    private int delay;
    /**
     * The function that generates initial items appearing on the
     * feedback loop.  It should input an int i and return the i'th
     * item to be read by the joiner.
     */
    private JMethodDeclaration initPath;

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRFeedbackLoop(SIRContainer parent,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods) {
	super(parent, fields, methods);
    }

    /**
     * Construct a new SIRPipeline null fields, parent, and methods
     */
    public SIRFeedbackLoop() {
	super();
    }
    
    /**
     * Return a shallow clone of the SIRFeedbackLoop
     */
    public Object clone() 
    {
	SIRFeedbackLoop f = new SIRFeedbackLoop(this.parent,
						this.fields,
						this.methods);
	f.setInit(this.init);
	f.setDelay(this.delay);
	f.setBody(this.body);
	f.setInitPath(this.initPath);
	f.setJoiner(this.joiner);
	f.setLoop(this.loop);
	f.setSplitter(this.splitter);
	return f;
    }
    

    /**
     * Returns the output type of this.
     */
    public CType getOutputType() {
	// return output type of body
	return body.getOutputType();
    }
    
    /**
     * Returns the input type of this.
     */
    public CType getInputType() {
	// return input type of body
	return body.getInputType();
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
	} else if (str==body) {
	    // return body
	    return "body";
	} else if (str==loop) {
	    // return loop
	    return "loop";
	} else {
	    // otherwise, <str> is not a child--return null
	    return null;
	}
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
	result.add(body);
	result.add(splitter);
	result.add(loop);
	// return result
	return result;
    }

    /**
     * Returns a list of tuples (two-element arrays) of SIROperators,
     * representing a tape from the first element of each tuple to the
     * second.
     */
    public List getTapePairs() {
	// construct result
	SIROperator[][] entries
	    = { {joiner, body},	// connect joiner and body
		{body, splitter},   // connect body and splitter
		{splitter, loop},   // connect splitter and loop
		{loop, joiner} };	// connect loop and joiner
	// return as list
	return Arrays.asList(entries);
    }

    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(StreamVisitor v) {
	v.preVisitFeedbackLoop(this,
			       parent,
			       fields,
			       methods,
			       init,
			       delay,
			       initPath);
	/* visit components */
	joiner.accept(v);
	body.accept(v);
	splitter.accept(v);
	loop.accept(v);
	v.postVisitFeedbackLoop(this,
				parent,
				fields,
				methods,
				init,
				delay,
				initPath);
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitFeedbackLoop(this,
				   parent,
				   fields,
				   methods,
				   init,
				   delay,
				   initPath);
    }

    /**
     * Set the Body of the feedback loop 
     **/
    public void setBody(SIRStream body) 
    {
	this.body = body;
    }
    /**
     * Set the Body of the feedback loop 
     **/
    public void setLoop(SIRStream loop) 
    {
	this.loop = loop;
    }
    /**
     * Set the Joiner of the feedback loop 
     **/
    public void setJoiner(SIRJoiner joiner) 
    {
	this.joiner = joiner;
    }
    /**
     * Set the Splitter of the feedback loop 
     **/
    public void setSplitter(SIRSplitter splitter) 
    {
	this.splitter = splitter;
    }
    /**
     * Set the delay of the feedback loop 
     **/
    public void setDelay(int delay) 
    {
	this.delay = delay;
    }
     /**
     * Set the Init Path method  of the feedback loop 
     **/
    public void setInitPath(JMethodDeclaration initPath)
    {
	this.initPath= initPath;
    }
    
    
}

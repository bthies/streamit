package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a feedback loop construct.
 */
public class SIRFeedbackLoop extends SIRStream {
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
    public SIRFeedbackLoop(SIRStream parent,
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

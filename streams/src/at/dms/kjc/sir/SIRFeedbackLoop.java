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
    private SIRJoiner splitter;
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
}

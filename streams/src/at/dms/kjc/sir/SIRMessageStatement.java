package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * Message Sending Statement.
 *
 * This statement sends a message to a portal.
 */
public class SIRMessageStatement extends JStatement {

    /**
     * The portal that is the target of the message.
     */
    private JExpression portal;
    /**
     * The name of the method to invoke in the portal.
     */
    private String ident;
    /**
     * The arguments to the method.
     */
    private JExpression[] args;
    /**
     * The latency with which the message should be delivered.
     */
    private SIRLatency latency;
    /**
     * The index of the method in the interface
     */
    private int index;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public SIRMessageStatement(JExpression portal, int index, JExpression[] args, SIRLatency latency) {
	super(null, null);

	this.portal = portal;
	this.args = args;
	this.index = index;
	this.latency = latency;
    }

    
    /**
     * Construct a node in the parsing tree
     */
    public SIRMessageStatement() {
	super(null, null);

	this.portal = null;
	this.index = -1;
	this.args = null;
	this.latency = null;
    }

    public int getIndex () {
	return index;
    }
    public JExpression getPortal() {
	return portal;
    }
    public SIRLatency getLatency() {
	return latency;
    }
    public JExpression[] getArgs() {
	return args;
    }

    public void setIndex (int index) {
	this.index = index;
    }

    public void setPortal (JExpression p) {
	this.portal = p;
    }
    
    public void setArgs (JExpression[] a) {
	this.args = a;
    }

    public void setLatency (SIRLatency l) {
	this.latency = l;
    }
    


    // ----------------------------------------------------------------------
    // SEMANTIC ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Analyses the statement (semantically) - NOT SUPPORTED YET.
     */
    public void analyse(CBodyContext context) throws PositionedError {
	at.dms.util.Utils.fail("Analysis of SIR nodes not supported yet.");
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitMessageStatement(this, 
						   portal,
						   index,
						   args,
						   latency);
	} else {
	    // otherwise, visit the arguments
	    for (int i=0; i<args.length; i++) {
		args[i].accept(p);
	    }
	}
    }

    /*
     * Accepts the specified attributed visitor - NOT SUPPORTED YET.
     */
    public Object accept(AttributeVisitor p) {
	at.dms.util.Utils.fail("Visitors to SIR nodes not supported yet.");
	return null;
    }
}

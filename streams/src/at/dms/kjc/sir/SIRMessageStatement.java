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
    private SIRPortal portal;
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

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public SIRMessageStatement(TokenReference where, JavaStyleComment[] comments, SIRPortal portal, String ident, JExpression[] args, SIRLatency latency) {
	super(where, comments);

	this.portal = portal;
	this.ident = ident;
	this.args = args;
	this.latency = latency;
    }

    
    /**
     * Construct a node in the parsing tree
     */
    public SIRMessageStatement() {
	super(null, null);

	this.portal = null;
	this.ident = null;
	this.args = null;
	this.latency = null;
    }

    public void setPortal (SIRPortal p) {
	this.portal = p;
    }

    public void setIdent (String i) {
	this.ident = i;
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
						   ident,
						   args,
						   latency);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
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

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
     * Accepts the specified visitor - NOT SUPPORTED YET.
     */
    public void accept(KjcVisitor p) {
	at.dms.util.Utils.fail("Visitors to SIR nodes not supported yet.");
    }

    /**
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }
}

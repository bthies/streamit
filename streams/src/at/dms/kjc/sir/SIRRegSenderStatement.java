package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * Register Sender Statement.
 *
 * This statement declares that the calling structure can send a
 * message to a given portal with a given latency.
 */
public class SIRRegSenderStatement extends JStatement {

    /**
     * The portal to register with.
     */
    private SIRPortal portal;
    /**
     * The latency to register with.  
     */
    private SIRLatency latency;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public SIRRegSenderStatement(TokenReference where, 
				 JavaStyleComment[] comments, 
				 SIRPortal portal, 
				 SIRLatency latency) {
	super(where, comments);

	this.portal = portal;
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

package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * Register Receiver Statement.
 *
 * This statement declares that the calling structure can receive a
 * message from the given portal.
 */
public class SIRRegReceiverStatement extends JStatement {

    /**
     * The name of the portal to register with.
     */
    private String portal;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public SIRRegReceiverStatement(TokenReference where, 
				   JavaStyleComment[] comments, 
				   String portal) {
	super(where, comments);

	this.portal = portal;
    }
    
    /**
     * Construct a node in the parsing tree
     */
    public SIRRegReceiverStatement() {
	super(null, null);

	this.portal = null;
    }
    
    public void setPortal(String p) {
	this.portal = p;
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
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitRegReceiverStatement(this, portal);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }

    /**
     * Accepts the specified attribute visitor.  NOT SUPPORTED YET.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
        at.dms.util.Utils.fail("Visitors to SIRE nodes not supported yet.");
        return null;
    }

    /**
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }
}

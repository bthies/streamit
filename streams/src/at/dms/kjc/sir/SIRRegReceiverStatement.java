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
    private JExpression portal;
    private SIRStream receiver;
    private CMethod[] methods;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public SIRRegReceiverStatement(JExpression portal, SIRStream receiver, CMethod[] methods) {
	super(null, null);
	this.portal = portal;
	this.receiver = receiver;
	this.methods = methods;
    }
    
    /**
     * Construct a node in the parsing tree
     */
    public SIRRegReceiverStatement() {
	super(null, null);

	this.portal = null;
	this.receiver = null;
	this.methods = null;
    }
    
    /**
     * Get the portal for this statement
     */
    public JExpression getPortal() {
	return this.portal;
    }

    
    /**
     * Get the receiver for this statement
     */
    public SIRStream  getReceiver() {
	return this.receiver;
    }

    /**
     * Get the methods  for this statement
     */
    public CMethod[] getMethods() {
	return this.methods;
    }

    
    /**
     * Set the receiver for this statement
     */
    public void setReceiver(SIRStream p) {
	this.receiver = p;
    }


    /**
     * Set the portal for this statement
     */
    public void setPortal(JExpression p) {
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
	    ((SLIRVisitor)p).visitRegReceiverStatement(this, portal, receiver, methods);
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

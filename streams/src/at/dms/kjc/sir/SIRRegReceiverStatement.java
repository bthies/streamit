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

    private JExpression portal;
    private SIRStream receiver;
    private SIRInterfaceTable itable;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public SIRRegReceiverStatement(JExpression portal, SIRStream receiver, SIRInterfaceTable itable) {
	super(null, null);
	this.portal = portal;
	this.receiver = receiver;
	this.itable = itable;
    }
    
    /**
     * Construct a node in the parsing tree
     */
    public SIRRegReceiverStatement() {
	super(null, null);

	this.portal = null;
	this.receiver = null;
        this.itable = null;
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
     * Get the interface table for this statement
     */
    public SIRInterfaceTable getItable() {
        return this.itable;
    }

    /**
     * Get the methods  for this statement
     */
    public JMethodDeclaration[] getMethods() {
	return this.itable.getMethods();
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
	    ((SLIRVisitor)p).visitRegReceiverStatement(this, portal, receiver, getMethods());
	} else {
	    // otherwise, do nothing... this node appears in the body of
	    // work functions, so a KjcVisitor might find it, but doesn't
	    // have anything to do to it.
	}
    }

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).
		visitRegReceiverStatement(this,
					  portal,
					  receiver,
					  getMethods());
	} else {
	    return this;
	}
    }

    /**
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRRegReceiverStatement other = new at.dms.kjc.sir.SIRRegReceiverStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRRegReceiverStatement other) {
  super.deepCloneInto(other);
  other.portal = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.portal, other);
  other.receiver = (at.dms.kjc.sir.SIRStream)at.dms.kjc.AutoCloner.cloneToplevel(this.receiver, other);
  other.itable = (at.dms.kjc.sir.SIRInterfaceTable)at.dms.kjc.AutoCloner.cloneToplevel(this.itable, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

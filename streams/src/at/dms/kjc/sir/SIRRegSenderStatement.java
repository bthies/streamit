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
     * The name of the portal to register with.
     */
    private String portal;
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
				 String portal, 
				 SIRLatency latency) {
	super(where, comments);

	this.portal = portal;
	this.latency = latency;
    }

     /**
     * Construct a node in the parsing tree
     */
    public SIRRegSenderStatement() {
	super(null, null);

	this.portal = null;
	this.latency = null;
    }

    public void setPortal(String p) {
	this.portal = p;
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
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitRegSenderStatement(this, portal, latency);
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
		visitRegSenderStatement(this,
					  portal,
					latency);
	} else {
	    return this;
	}
    }

    /*
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRRegSenderStatement other = new at.dms.kjc.sir.SIRRegSenderStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRRegSenderStatement other) {
  super.deepCloneInto(other);
  other.portal = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.portal, other);
  other.latency = (at.dms.kjc.sir.SIRLatency)at.dms.kjc.AutoCloner.cloneToplevel(this.latency, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

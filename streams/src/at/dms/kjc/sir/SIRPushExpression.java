package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a push expression (can only be used as a statement).
 */
public class SIRPushExpression extends JExpression {

    /**
     * The argument to the push expression--the item to push.
     */
    protected JExpression arg;

    /**
     * Type of the item to push.
     */
    protected CType tapeType;

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected SIRPushExpression() {
	super();
    }
    
    /**
     * Construct a node in the parsing tree
     * @param	where		the line of this node in the source code
     * @param	arg		the argument of the call
     */
    public SIRPushExpression(JExpression arg)
    {
	this(arg, null);
    }

    public SIRPushExpression(JExpression arg, CType tapeType)
    {
	super(null);
        this.tapeType = tapeType;
	this.arg = arg;
    }

    /**
     * Sets the type of the tape being pushed to
     * @param   type             the type of the tape
     */
    public void setTapeType(CType type)
    {
        this.tapeType = type;
    }

    /**
     * Gets the type of the tape.
     */
    public CType getTapeType()
    {
        return tapeType;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * @return the type of this expression
     */
    public CType getType() {
	return CStdType.Void;
    }

    public JExpression getArg() {
	return this.arg;
    }

    public void setArg(JExpression arg) {
	this.arg = arg;
    }

    /**
     * Returns true iff this expression can be used as a statement (JLS 14.8)
     */
    public boolean isStatementExpression() {
	return true;
    }

    // ----------------------------------------------------------------------
    // SEMANTIC ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Throws an exception (NOT SUPPORTED YET)
     */
    public JExpression analyse(CExpressionContext context) throws PositionedError {
	at.dms.util.Utils.fail("Analysis of SIR nodes not supported yet.");
	return this;
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitPushExpression(this, tapeType, arg);
	} else {
	    // otherwise, visit the argument
	    arg.accept(p);
	}
    }

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).visitPushExpression(this,
								 tapeType,
								 arg);
	} else {
	    return this;
	}
    }

    /*
     * Generates JVM bytecode to evaluate this expression.  NOT SUPPORTED YET.
     *
     * @param	code		the bytecode sequence
     * @param	discardValue	discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
	at.dms.util.Utils.fail("Codegen of SIR nodes not supported yet.");
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRPushExpression other = new at.dms.kjc.sir.SIRPushExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRPushExpression other) {
  super.deepCloneInto(other);
  other.arg = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.arg, this);
  other.tapeType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.tapeType, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a peek expression.
 */
public class SIRPeekExpression extends JExpression {

    /**
     * The argument to the peek expression--the index of the item to
     * peek.
     */
    protected JExpression arg;

    /**
     * Type of the item to peek.
     */
    protected CType tapeType;

    /**
     * Construct a node in the parsing tree
     * @param	where		the line of this node in the source code
     * @param	arg		the argument of the call
     */
    public SIRPeekExpression(TokenReference where, JExpression arg)
    {
	super(where);
        this.tapeType = null;
	this.arg = arg;
    }

    /**
     * Construct a node in the parsing tree with null TokenReference
     * @param	arg		the argument of the call
     */
    public SIRPeekExpression(JExpression arg)
    {
	super(null);
        this.tapeType = null;
	this.arg = arg;
    }

    /**
     * Sets the type of the tape being peeked at
     * @param   type             the type of the tape
     */
    public void setTapeType(CType type)
    {
        this.tapeType = type;
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
	at.dms.util.Utils.fail("Analysis of custom nodes not supported yet.");
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
	    ((SLIRVisitor)p).visitPeekExpression(this, tapeType, arg);
	} else {
	    // visit the argument
	    arg.accept(p);
	}
    }

    /**
     * Accepts the specified Attribute visitor.  NOT SUPPORTED YET.
     * @param	p		the visitor
     */
    public Object accept(AttributeVisitor p) {
	at.dms.util.Utils.fail("Visitors to custom nodes not supported yet.");
	return null;
    }


    /**
     * Generates JVM bytecode to evaluate this expression.  NOT SUPPORTED YET.
     *
     * @param	code		the bytecode sequence
     * @param	discardValue	discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
	at.dms.util.Utils.fail("Visitors to custom nodes not supported yet.");
    }
}

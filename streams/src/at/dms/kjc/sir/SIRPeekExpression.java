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
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    protected SIRPeekExpression() {
	super();
    }
    
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
	this(arg, null);
    }

    public SIRPeekExpression(JExpression arg, CType tapeType)
    {
	super(null);
        this.tapeType = tapeType;
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

    /**
     * Sets the arg of this.
     */
    public void setArg(JExpression arg) {
	this.arg = arg;
    }

    /**
     * Returns the argument of this.
     */
    public JExpression getArg() {
	return arg;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * @return the type of this expression
     */
    public CType getType() {
        if (tapeType != null)
            return tapeType;
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
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).visitPeekExpression(this,
								 tapeType,
								 arg);
	} else {
	    return this;
	}
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

    /**
     * Generates a nice readable version of the PeekExpression.
     **/
    public String toString() {
	return ("SIRPeekExpression[" +
		this.arg + "]");
    }
		
}

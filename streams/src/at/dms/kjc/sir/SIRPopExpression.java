package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a pop expression.
 */
public class SIRPopExpression extends JExpression {

    /**
     * Type of the item to peek.
     */
    protected CType tapeType;
    /**
     * The number of items to pop.  If this is anything other than 1,
     * then the value returned by this is undefined.
     */
    private int numPop;
    
    /**
     * Construct a node in the parsing tree
     * @param	where		the line of this node in the source code
     */
    public SIRPopExpression(CType tapeType)
    {
	this(tapeType, 1);
    }

    /**
     * Construct a node that pops N items, and returns an undefined
     * value.  For quick advances of the tape.
     *
     * !!!!!!!!! WARNING !!!!!!!!!!  There is extremely limited
     * support for this in the IR.  Right now it just has the correct
     * code generated for it in the uniprocessor backend.
     */
    public SIRPopExpression(CType tapeType, int N)
    {
	super(null);
        this.tapeType = tapeType;
	this.numPop = N;
    }

    /**
     * Construct a node in the parsing tree
     */
    public SIRPopExpression()
    {
        this(null);
    }

    /**
     * Sets the type of the tape being pushed to
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
     * Returns how many items this pops.
     */
    public int getNumPop() {
	return numPop;
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitPopExpression(this, tapeType); 
	} else {
	    // otherwise, do nothing
	}
    }

    /**
     * Accepts the specified attribute visitor.
     * @param   p               the visitor
     */
    public Object accept(AttributeVisitor p) {
	if (p instanceof SLIRAttributeVisitor) {
	    return ((SLIRAttributeVisitor)p).visitPopExpression(this,
								tapeType);
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

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRPopExpression other = new at.dms.kjc.sir.SIRPopExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRPopExpression other) {
  super.deepCloneInto(other);
  other.tapeType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.tapeType, other);
  other.numPop = this.numPop;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}









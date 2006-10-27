/**
 * $Id$
 */
package at.dms.kjc.sir.lowering;

import at.dms.compiler.PositionedError;
import at.dms.kjc.AttributeVisitor;
import at.dms.kjc.AutoCloner;
import at.dms.kjc.CExpressionContext;
import at.dms.kjc.CNumericType;
import at.dms.kjc.CType;
import at.dms.kjc.CVectorType;
import at.dms.kjc.CodeSequence;
import at.dms.kjc.ExpressionVisitor;
import at.dms.kjc.JExpression;
import at.dms.kjc.JLiteral;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.KjcVisitor;
import at.dms.kjc.SLIRAttributeVisitor;
import at.dms.kjc.SLIRVisitor;

/**
 * Vector literals: a literal of a numeric base type duplicated to fill a vector.
 * This is not a subtype of JLiteral since you can not just generate a string for such
 * a literal.  It is used as a marker in code for reference to a variable that
 * needs to be generated elsewhere.
 * <br/>
 * JVectorLiteral's only exist during the execution of {@link Vectorize#vectorize(at.dms.kjc.sir.SIRFilter) vectorize}
 * Before then, there are no vector literals.  After then there are const (ACC_STATIC | ACC_FINAL) variables initialized
 * to vector values: this is because of a gcc limitation in its vector extensions (as of v4.1.1) that it does not 
 * promote constants to vectors. 
 * <br/>
 * There is no need to clone JVectorLiteral's.
 * @author Allyn Dimock
 *
 */
public class JVectorLiteral extends JExpression {
    private JLiteral scalar;
    private CVectorType type;
    
    private JVectorLiteral() {} // for cloner only.
    
    /**
     * Construct from a scalar literal
     * @param scalar a scalar literal which should have CNumericType, and be a 32-bit quantity.
     */
    public JVectorLiteral(JLiteral scalar) {
        super();
        this.setScalar(scalar);
    }

    /**
     * Get the scalar value that is being widened to a vector.
     * @return scalar value: a JLiteral of CNumericType
     */
    public JLiteral getScalar() { return scalar; }

    /**
     * Set a new value for the scalar literal being duplicated to make the vector.
     * @param scalar :  a JLiteral of CNumericType
     */
    public void setScalar(JLiteral scalar) {
        this.scalar = scalar;
        CType t = scalar.getType();
        assert t instanceof CNumericType;
        type = new CVectorType((CNumericType)t, KjcOptions.vectorize);
    }
    /* (non-Javadoc)
     * @see at.dms.kjc.JExpression#accept(at.dms.kjc.KjcVisitor)
     */
    @Override
    public void accept(KjcVisitor p) {
        if (p instanceof SLIRVisitor) {
            ((SLIRVisitor)p).visitVectorLiteral(this, scalar);
        } else {
            scalar.accept(p);  // introduced in StreamIt, not in Kjc.
        }
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.JExpression#accept(at.dms.kjc.AttributeVisitor)
     */
    @Override
    public Object accept(AttributeVisitor p) {
        if (p instanceof SLIRAttributeVisitor) {
            return ((SLIRAttributeVisitor)p).visitVectorLiteral(this,scalar);
        } else {
            return this;
        }

    }

    /* (non-Javadoc)
     * @see at.dms.kjc.JExpression#accept(at.dms.kjc.ExpressionVisitor, java.lang.Object)
     */
    @Override
    public <S,T> S accept(ExpressionVisitor<S,T> p, T d) {
        return p.visitVectorLiteral(this,d);
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.JExpression#analyse(at.dms.kjc.CExpressionContext)
     */
    @Override
    public JExpression analyse(CExpressionContext context)
            throws PositionedError {
        throw new UnsupportedOperationException(this + " does not support analyze.");
    }

 
    /* (non-Javadoc)
     * @see at.dms.kjc.JExpression#genCode(at.dms.kjc.CodeSequence, boolean)
     */
    @Override
    public void genCode(CodeSequence code, boolean discardValue) {
        throw new UnsupportedOperationException(this + " does not support genCode.");
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.JExpression#getType()
     */
    @Override
    public CType getType() {
        return type;
    }
}

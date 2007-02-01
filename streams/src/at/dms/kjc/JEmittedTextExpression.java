/**
 * 
 */
package at.dms.kjc;

import at.dms.compiler.PositionedError;

/**
 * The sole purpose of this class is to emit text directly from a compiler backend.
 * Should help to deal with mismatch between kjc and C:
 * sould be able to implement infix expressions ".", "->", prefix expressions "&", "*",
 * surroundfix expressions "sizeof(...)" etc.
 * 
 * @author dimock
 *
 */
public class JEmittedTextExpression extends JExpression {

    /**
     * 
     */
    private static final long serialVersionUID = 3881644851055146980L;

    
    private Object[] parts;
    private CType type;
    public void setType(CType type) {
        this.type = type;
    }
    public CType getType() {
        return type;
    }
    public Object[] getParts() {
        return parts;
    }
    
    /** create with a text string */
    JEmittedTextExpression(String text) {
        this.parts = new Object[]{text};
    }
    
    /** create with a misture of text strings, expressions, and types */
    JEmittedTextExpression(Object[] parts) {
        for (Object o : parts) {
            assert (o instanceof String 
                    || o instanceof JExpression
                    || o instanceof CType);
        }
        this.parts = parts;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.JStatement#accept(at.dms.kjc.AttributeVisitor)
     */
    @Override
    public Object accept(AttributeVisitor p) {
        return p.visitEmittedTextExpression(this, parts);
    }
    @Override
    public void accept(KjcVisitor p) {
        p.visitEmittedTextExpression(this, parts);
        
    }
    @Override
    public <S, T> S accept(ExpressionVisitor<S, T> p, T d) {
        return p.visitEmittedText(this,d);
    }
    @Override
    public JExpression analyse(CExpressionContext context) throws PositionedError {
        throw new UnsupportedOperationException();
    }
    @Override
    public void genCode(CodeSequence code, boolean discardValue) {
        throw new UnsupportedOperationException();
    }

}

/**
 * 
 */
package at.dms.kjc;

import at.dms.compiler.PositionedError;

/**
 * The sole purpose of this class is to emit text directly from a compiler backend.
 * 
 * @author dimock
 *
 */
public class JEmittedText extends JStatement {

    private String text = "";
    
    /** create with a text string */
    JEmittedText(String initialText) {
        text = initialText;
    }
    
    /** get test string */
    public String getText() {
        return text;
    }
    
    /** change stored text */
    public void setText(String newText) {
        text = newText;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.JStatement#analyse(at.dms.kjc.CBodyContext)
     */
    @Override
    public void analyse(CBodyContext context) throws PositionedError {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see at.dms.kjc.JStatement#genCode(at.dms.kjc.CodeSequence)
     */
    @Override
    public void genCode(CodeSequence code) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see at.dms.kjc.JStatement#accept(at.dms.kjc.AttributeVisitor)
     */
    @Override
    public Object accept(AttributeVisitor p) {
        return p.visitEmittedText(this);
    }

}

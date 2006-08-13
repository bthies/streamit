/**
 * 
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.SLIRReplacingVisitor;
import at.dms.kjc.SLIREmptyVisitor;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.*;

/**
 * Remove multi-pops.
 * 
 * A single SIRPopExpression may represent multiple pops.
 * This feature is used in fusion, and in some passes writtten 
 * specifically for the Uniprocessor backend.
 * 
 *  This pass removes the multiple pops inserted by fusion.
 *  It may need more work to handle multiple pops in general.
 * 
 * The current implementation expects a SIRPopExpression representing 
 * multiple pops to be wrapped in a JStatmentExpression.  This is true
 * of fusion, but may not be tru in general -- thus the possiblility
 * that this code will need to be extended in the future.
 * 
 * @author dimock
 *
 */
public class RemoveMultiPops extends SLIRReplacingVisitor {

    private RemoveMultiPops() { }


    /**
     * Walk stream structure and replace multiple pops from single SIRPopExpression.
     * 
     * @param str  a stream to work on
     */
    public static void doit(SIRStream str) {
        new RemoveMultiPops().removeMultiProps(str);
        checkEliminated(str);
    }

    /**
     * Replace multiple pops with single pop expression in a kopi IR
     * construct, and returns the result (also mutates the argument).
     */
    public static JPhylum doit(JPhylum phylum) {
        JPhylum result = (JPhylum)phylum.accept(new RemoveMultiPops());
        checkEliminated(phylum);
        return result;
    }
    
    private void removeMultiProps(SIRStream str) {
        final SLIRReplacingVisitor visitor =this;
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    for (int i = 0; i < self.getMethods().length; i++) {
                        self.getMethods()[i].accept(visitor);
                    }
                }});
    }

    public Object visitExpressionStatement(JExpressionStatement self,
                                           JExpression expr) {
        if (expr instanceof SIRPopExpression) {
            SIRPopExpression popExp = (SIRPopExpression) expr;
            return Utils.makeForLoop(new JExpressionStatement(new SIRPopExpression(popExp.getType())), 
                                     popExp.getNumPop());
        } else {
            return self;
        }
    }

    /**
     * Checks that all the multi-pops have been eliminated from a stream.
     */
    private static void checkEliminated(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    for (int i = 0; i < self.getMethods().length; i++) {
                        // for each method...
                        checkEliminated(self.getMethods()[i]);
                    }
                }});
    }

    /**
     * Checks that all the multi-pops have been eliminated from a JPhylum.
     */
    private static void checkEliminated(JPhylum phylum) {
        phylum.accept(new SLIREmptyVisitor() {
                // and each pop expression...
                public void visitPopExpression(SIRPopExpression self,
                                               CType tapeType) {
                    // throw an error if the pop is for more than 1 item
                    if (self.getNumPop()>1) {
                        Utils.fail("Failed to expand multi-pop expression.");
                    }
                }});
    }
}

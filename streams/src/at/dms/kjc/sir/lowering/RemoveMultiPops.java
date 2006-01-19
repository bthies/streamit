/**
 * 
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.SLIRReplacingVisitor;
import at.dms.kjc.*;
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
    }
    
    // The following structure appears all over the place.  It needs to be abstracted somehow.
    // Walk SIR structure to get down to 
    private void removeMultiProps(SIRStream str) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            removeMultiProps(fl.getBody());
            removeMultiProps(fl.getLoop());
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                removeMultiProps(child);
            }
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                removeMultiProps(child);
            }
        }
        if (str instanceof SIRFilter) {
            for (int i = 0; i < str.getMethods().length; i++) {
                str.getMethods()[i].accept(this);
            }
        }
    }
    
    public Object visitExpressionStatement(JExpressionStatement self,
            JExpression expr) {
        if (expr instanceof SIRPopExpression) {
            SIRPopExpression popExp = (SIRPopExpression) expr;
            return Utils.makeForLoop(new JExpressionStatement(null,
                    new SIRPopExpression(popExp.getType()), null), 
                    popExp.getNumPop());
        } else {
            return self;
        }
    }
        
}
    

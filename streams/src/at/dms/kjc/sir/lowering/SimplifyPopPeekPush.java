/**
 * <br/>$Id$
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/**
 * Re-write code so push, pop, and peek are separate statements, and arguments to them are
 * either literal, local, field, or array access.
 * @author Allyn Dimock
 *
 */
public class SimplifyPopPeekPush {
    /**
     * Re-write code so push, pop, and peek are separate statements, and arguments to them are
     * either literal, local, field, or array access. In the case of array access, the array access
     * will not involve the result of another peek or pop.
     * @param str : a SIRStream, rewritten in place.
     */
    public static void simplify (SIRStream str) {
        SimplifyPopPeekPush sppp = new SimplifyPopPeekPush();
        sppp.cvt(str);
    }

    private void cvt (SIRStream str) {
        (new ThreeAddress()).threeAddressCode(str);
    }
    
    private class ThreeAddress extends ThreeAddressCode {
        // should only convert an expression to three-address code if it
        // contains a push, peek or pop internally, or if
        // push, peek, or pop at top level with a complex expression.
        // The visitor will find at top level or internally, we assume that
        // expressions do not grow as program size so no worry about quadratic time
        // for this check.
        
        @Override
        protected boolean shouldConvertExpression(JExpression expr) {
            final int[] count = {0};
            expr.accept(new SLIREmptyVisitor(){
                @Override public void visitPeekExpression(SIRPeekExpression self,
                        CType tapeType,
                        JExpression arg) {
                    super.visitPeekExpression(self,tapeType,arg);
                    count[0]++;
                }
                @Override public void visitPopExpression(SIRPopExpression self,
                        CType tapeType) {
                    super.visitPopExpression(self,tapeType);
                    count[0]++;
                }
                @Override    public void visitPushExpression(SIRPushExpression self,
                        CType tapeType,
                        JExpression arg) {
                    super.visitPushExpression(self,tapeType,arg);
                    count[0]++;
                }});
            
            // if expression is at top level and subexpression is sufficiently simple, then we do not need
            // to convert.
            if (count[0] > 0 && (
                    (expr instanceof SIRPeekExpression && simpleExpression(((SIRPeekExpression)expr).getArg())) ||
                    (expr instanceof SIRPushExpression && simpleExpression(((SIRPushExpression)expr).getArg())) ||
                    (expr instanceof SIRPopExpression))) {
                count[0]--;
            }
            // if push / peek / pop expressions below top level
            // or if push / peek / pop at top level have complex subexpression
            // then require the current expression to be converted.
            return count[0] > 0;
        }
    }
}

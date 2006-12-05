/**
 * $Id$
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/**
 * Re-write code so function arguments are either literal, local, field, 
 * or array access.
 * This should allow us to replace min, max, abs with macros at the right type
 * rather than old practice of casting to float, using the float version
 * and casting back.
 *
 * Do not extend this to arguments to print / println until there is a way
 * of dealing with the concatenation of implicit toString()s that occur only
 * in the argument to these.
 *
 * @author Allyn Dimock
 *
 */
public class SimplifyArguments {
    
    /**
     * Walk over stream simplifying function arguments.
     * @param str : stream to process, munged in place.
     */
    public static void simplify (SIRStream str) {
        SimplifyArguments sa = new SimplifyArguments();
        sa.cvt(str);
    }

    private void cvt (SIRStream str) {
        (new ThreeAddress()).threeAddressCode(str);
    }
    
    private class ThreeAddress extends ThreeAddressCode {
        // should only convert an expression to three-address code if it
        // contains method call with arguments other constant, variable, array or structure ref.
        // The visitor will find at top level or internally, we assume that
        // expressions do not grow as program size so no worry about quadratic time
        // for this check.
        
        @Override
        protected boolean shouldConvertExpression(JExpression expr) {
            if (! super.shouldConvertExpression(expr)) return false;
            // Only get here if ThreeAddressCode.shouldConvertExpression(expr) is true.
            // which includes avery case that we are interested in.
            final boolean[] convert = {false};
            expr.accept(new SLIREmptyVisitor(){
                @Override public void visitMethodCallExpression(JMethodCallExpression self,
                        JExpression prefix,
                        String ident,
                        JExpression[] args) {
                    if (ident.equals("print") || ident.equals("println")) {
                        // ignore prints since we can not handle them and they
                        // are not the focus of this pass...
                        return;
                    }
                    for (JExpression arg : args) {
                        if (! simpleExpression(arg)) {
                            convert[0] = true;;
                            break;
                        }
                        // no need to descent into arg: either is
                        // simple or we are already converting.
                    }
                }
                });
            
            return convert[0];
        }
    }
}


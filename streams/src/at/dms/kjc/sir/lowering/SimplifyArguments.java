/**
 * $Id$
 */
package at.dms.kjc.sir.lowering;

import java.util.*;
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
     * Walk over stream simplifying arguments to function calls.
     * Rules: (1) If an expression is a complicated argument to a function 
     * then it must be three-address coverted at the top level (so that an
     * expression with side effects only executes the side-effect once).
     * (2) If an expression contains a function call, then it needs to be 
     * three-address converted (so that the arguments can be converted at
     * the correct time relative to its calling context).
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
        // if a method call has a non-trivial argument expression then
        // (1) make sure all non-trivial argument expressions are converted 
        // to three-address code, and
        // (2) make sure the context in which the method call occurs is converted
        // to three-address code ((a) so that any side-effects in the context and in
        // the argument evaluation remain in the same order, (b) since ThreeAddressCode
        // only looks at marked expressions for conversion so will never find the
        // argument needing conversion unless it has converted down to the argument.).
 
        @Override
        protected boolean shouldConvertTopExpression(JExpression expr) {
            if (simpleExpression(expr)) {
                return false;
            }
            ThreeAddressExpressionCheck markSubexprsToConvert = 
                new ThreeAddressExpressionCheck(){
                private void addone(JExpression expr) {
                    exprsToExpand.add(expr);
                }
                private void addcontext(Stack<JExpression> context) {
                    for (JExpression expr : context) {
                        addone(expr);
                    }
                }
                @Override
                protected Object preCheck(Stack<JExpression> context, JExpression self) {
                    if (self instanceof JMethodCallExpression) {
                        JMethodCallExpression mexp = (JMethodCallExpression)self;
                        boolean anyNonSimpleArgs = false;
                        for (JExpression arg : mexp.getArgs()) {
                            if (! simpleExpression(arg)) {
                                addone(arg);
                                anyNonSimpleArgs = true;
                            }
                        }
                        if (statementBeingChecked[0] instanceof SIRPrintStatement
                                || statementBeingChecked[0] instanceof JVariableDeclarationStatement
                                || anyNonSimpleArgs) {
                            addcontext(context);
                            addone(self);
                        }
                    }
                    return null;
                }
            };
            exprsToExpand.clear();
            expr.accept(markSubexprsToConvert,new Stack<JExpression>());
            return ! exprsToExpand.isEmpty();
        }
        
        /**
         * Be more forgiving of JExpressionListStatement than ThreeAddressCode is.
         * If no expression in a JExpressionListStatement needs converting then
         * don't convert the statement.
         */
        @Override
        protected boolean shouldConvertStatement(JStatement stmt) {
            // requirement from ThreeAddressCode to set statementBeingChecked[0]
            statementBeingChecked[0] = stmt;
            
            if (stmt instanceof JExpressionListStatement) {
                boolean needToConvert = false;
                for (JExpression expr : ((JExpressionListStatement) stmt)
                        .getExpressions()) {
                    needToConvert |= shouldConvertTopExpression(expr); 
                }
                return needToConvert;
            } else if (stmt instanceof JVariableDeclarationStatement) {
                boolean needToConvert = false;
                for (JVariableDefinition def : ((JVariableDeclarationStatement)stmt).getVars()) {
                    needToConvert |= shouldConvertTopExpression(def.getValue());
                }
                return needToConvert;
            } else if (stmt instanceof SIRPrintStatement) {
                boolean needToConvert = shouldConvertTopExpression(((SIRPrintStatement)stmt).getArg());
                return needToConvert;
            } else {
                return super.shouldConvertStatement(stmt);
            }
        }
    }
}


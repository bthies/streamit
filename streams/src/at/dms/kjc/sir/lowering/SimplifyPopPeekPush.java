/**
 * <br/>$Id$
 */
package at.dms.kjc.sir.lowering;

import java.util.Stack;

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
        // contains method call with arguments other constant, variable, array or structure ref.
        // The visitor will find at top level or internally, we assume that
        // expressions do not grow as program size so no worry about quadratic time
        // for this check.
        
        @Override
        protected boolean shouldConvertTopExpression(JExpression expr) {
            if (expr == null || simpleExpression(expr)) {
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
                    // peek(exp)
                    // (1) Convert any context so peek appears immediately under assignment.
                    // (2) Convert argument to peek so peek argument only gets "simple" expressions.
                    if (self instanceof SIRPeekExpression) {
                        JExpression exp = ((SIRPeekExpression)self).getArg();
                        boolean argNeedsConversion = ! simpleExpression(exp);
                        if (argNeedsConversion ||
                                statementBeingChecked[0] instanceof SIRPrintStatement ||
                                statementBeingChecked[0] instanceof JVariableDeclarationStatement ||
                                !(context.size() == 1 && context.peek() instanceof JAssignmentExpression)) {
                            addcontext(context);
                            addone(self);
                            if (argNeedsConversion) {
                                addone(exp);
                            }
                        }
                        // pop()
                        // (1) only need to convert if returning a value, so not if it is at top level.
                        // (2) if converting, convert all the way so pop appears immediately under assignment.
                    } else if (self instanceof SIRPopExpression) {
                            if (statementBeingChecked[0] instanceof SIRPrintStatement
                                || statementBeingChecked[0] instanceof JVariableDeclarationStatement
                                || (context.size() > 0 && !(context.size() == 1 && context
                                        .peek() instanceof JAssignmentExpression))) {
                            addcontext(context);
                            addone(self);
                        } 
                    } else if (self instanceof SIRPushExpression) {
                        // push(exp)
                        // (1) Push occurs only immediately inside a JExpressionStatement, and is treated
                        // by ThreeAddressCode as if it were a separate Statement (which it should have been).
                        // (2) Convert argument to pushd so push argument only gets "simple" expressions.
                        JExpression exp = ((SIRPushExpression)self).getArg();
                        if (! simpleExpression(exp)) {
                            addone(exp);
                        }
                    }
                    return null;
                }
            };
            exprsToExpand.clear();
            if (expr != null) expr.accept(markSubexprsToConvert,new Stack<JExpression>());
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
                return shouldConvertTopExpression(((SIRPrintStatement)stmt).getArg());
            } else {
                return super.shouldConvertStatement(stmt);
            }
        }
    }
}

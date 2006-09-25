package at.dms.kjc.common;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.*;
import java.io.*;

/**
 * This class converts small functions to macros.
 */
public class MacroConversion {
    /**
     * Max number of expressions (including variable refs, literals,
     * etc.) allowed in a macro.
     */
    static final int MAX_SIZE = 32;

    /**
     * Returns whether or not <decl> can be converted to a MACRO.
     * Currently returns true for functions with the following
     * properties:
     *
     * - The KjcOptions.macro option is enabled.
     *
     * - The only statement in the body of the method is a return
     * statement.
     *
     * - Each parameter to the method is referenced at most once.
     * (Otherwise, parameters with side effects [like pop()] would be
     * replicated).
     *
     * - All the expressions in the return statement are either:
     *
     * 1. JParenthesedExpression
     *
     * 2. JConditionalExpression
     *
     * 3. JBinaryExpressions other than JAssignmentExpressions, i.e.:
     *
     *   JBinaryArithmeticExpression,
     *   JConditionalAndExpression, 
     *   JConditionalOrExpression,
     *   JEqualityExpression, 
     *   JRelationalExpression
     *
     * 4. JArrayAccessExpression
     *
     * 5. JFieldAccessExpression
     *
     * 6. JLocalVariableExpression
     *
     * 7. JCastExpression
     *
     * 8. JLiteral
     *
     * Also, there are at most MAX_SIZE such expressions.
     *
     * Note that postfix/prefix is not allowed since it might not end
     * up acting on a variable after inlining (leading to syntax
     * error).
     *
     */
    public static boolean shouldConvert(JMethodDeclaration decl) {
        if (!KjcOptions.macros) { return false; }

        JExpression expr = extractMacroExpr(decl);

        if (expr==null) {
            // couldn't identify return expression 
            return false;
        } else {
            // sees if expression is legal for macros
            return new MacroLegalityTest().test(expr);
        }
    }

    /**
     * Performs conversion of <decl> into a macro.  If <declOnly> is
     * true, returns the macro definition (in place of what would have
     * been the prototype).  Otherwise returns an empty string (as
     * nothing goes in place of the actual method; macro decls need to
     * be at the top of the file, before they are used.)
     *
     * <toC> is the code generator that was working on the program,
     * will be used to generate code for macro expression.
     */
    public static void doConvert(JMethodDeclaration decl, boolean declOnly, CodeGenerator toC) {
        CodegenPrintWriter p = toC.getPrinter();
        // replace actual method with nothing
        if (!declOnly) { return; }

        // print status
        System.err.println("Converting " + decl.getName() + " to a macro.");

        // get macro expression
        JExpression expr = extractMacroExpr(decl);
        assert expr!=null : "Could not identify return expression in macro conversion.";

        // wrap every variable reference in <expr> with parantheses,
        // to make macro safe
        expr.accept(new SLIRReplacingVisitor() {
                public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                                           String ident) {
                    return new JParenthesedExpression(self);
                }
            });
    
        // "#define foo"
        p.print("\n#define ");
        p.print(decl.getName());

        // param names (without types!)
        p.print("(");
        JFormalParameter[] params = decl.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (i!=0) { p.print(", "); }
            p.print(params[i].getIdent());
        }
        p.print(") ");
    
        // body
        expr.accept(toC);
        p.print("\n");
    }

    /**
     * Extracts the return expression out of a given method.  If the
     * body contains more than a single return expression, returns
     * null.
     */
    private static JExpression extractMacroExpr(JMethodDeclaration decl) {
        // one return statement
        JBlock body = decl.getBody();
        // allow nested blocks at first
        while (body.size()==1 && body.getStatement(0) instanceof JBlock) {
            body = (JBlock)body.getStatement(0);
        }
    
        // if 0 statements or more than 1 statement, fail
        if (body.size()==0 || body.size()>1) { return null; }
        // something other than return, fail
        if (!(body.getStatement(0) instanceof JReturnStatement)) { return null; }
    
        // get contents of return
        JExpression expr = ((JReturnStatement)body.getStatement(0)).getExpression();

        return expr;
    }

    static class MacroLegalityTest extends SLIREmptyVisitor {
        /**
         * Whether or not conversion is legal.
         */
        private boolean isLegal;
        /**
         * Count of number of expressions seen.
         */
        private int exprCount;
        /**
         * Set of JLocalVariables that have been referenced so far. 
         */
        private HashSet<JLocalVariable> varRefs;
    
        public MacroLegalityTest() {
            isLegal = true;
            exprCount = 0;
            varRefs = new HashSet<JLocalVariable>();
        }

        /**
         * Returns whether or not a given JExpression is legal to convert
         * to a macro, using rules stipulated in
         * MacroConversion.shouldConvert().
         */
        public boolean test(JExpression expr) {
            doVisit(expr);
            isLegal = isLegal && exprCount <= MacroConversion.MAX_SIZE;
            return isLegal;
        }

        /**
         * The visit primitive.  If it hits a disallowed expression type,
         * marks <isLegal> as false.  Otherwise increments expression
         * count and descends.
         */
        private void doVisit(JExpression expr) {
            boolean allowed = (expr instanceof JParenthesedExpression ||
                               expr instanceof JConditionalExpression ||
                               expr instanceof JBinaryArithmeticExpression ||
                               expr instanceof JConditionalAndExpression ||
                               expr instanceof JConditionalOrExpression ||
                               expr instanceof JEqualityExpression ||
                               expr instanceof JRelationalExpression ||
                               expr instanceof JArrayAccessExpression ||
                               expr instanceof JFieldAccessExpression ||
                               expr instanceof JLocalVariableExpression ||
                               expr instanceof JCastExpression ||
                               expr instanceof JLiteral);
            if (allowed) {
                exprCount++;
                expr.accept(this);
            } else {
                isLegal = false;
            }
        }

        // ---------------------------------------------------------------
        // Now for each of the legal types, we need to call doVisit() on
        // children instead of calling accept directly.
        // ---------------------------------------------------------------
    
        public void visitParenthesedExpression(JParenthesedExpression self,
                                               JExpression expr) {
            doVisit(expr);
        }

        public void visitBinaryExpression(JBinaryExpression self,
                                          String oper,
                                          JExpression left,
                                          JExpression right) {
            doVisit(left);
            doVisit(right);
        }

        public void visitConditionalExpression(JConditionalExpression self,
                                               JExpression cond,
                                               JExpression left,
                                               JExpression right) {
            doVisit(cond);
            doVisit(left);
            doVisit(right);
        }
    
        public void visitEqualityExpression(JEqualityExpression self,
                                            boolean equal,
                                            JExpression left,
                                            JExpression right) {
            doVisit(left);
            doVisit(right);
        }
    
        public void visitRelationalExpression(JRelationalExpression self,
                                              int oper,
                                              JExpression left,
                                              JExpression right) {
            doVisit(left);
            doVisit(right);
        }

        public void visitArrayAccessExpression(JArrayAccessExpression self,
                                               JExpression prefix,
                                               JExpression accessor) {
            doVisit(prefix);
            doVisit(accessor);
        }

        public void visitFieldExpression(JFieldAccessExpression self,
                                         JExpression left,
                                         String ident)
        {
            doVisit(left);
        }

        public void visitLocalVariableExpression(JLocalVariableExpression self,
                                                 String ident) {
    
            JLocalVariable var = self.getVariable();
            if (varRefs.contains(var)) {
                // referenced a variable twice -- illegal
                isLegal = false;
            } else {
                // remember that we referenced this var
                varRefs.add(var);
            }
        }

        public void visitCastExpression(JCastExpression self,
                                        JExpression expr,
                                        CType type)
        {
            doVisit(expr);
        }
    }
}

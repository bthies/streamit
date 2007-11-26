/**
  * <br/>$Id$
  */
package at.dms.kjc.sir.lowering;

import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.lir.*;
import java.util.*;

import streamit.misc.Pair;

/**
 * Turn statements into three-address code optionally depending on overridable simpleExpression 
 * and shouldConvertExpression methods.
 * <br/>Allowing the user to override shouldConvertExpression forces this class to be non-static.
 * <br/>Notes:
 * <ul><li>
 * If simpleExpression(exp) returns true then shouldConvertExpression(exp) is not called.
 * the supplied simpleExpression(exp) checks that an expression is made up of constants, local variables
 * field variables, simple structure references, array references where the offset is simple, and a few
 * odd cases such as range expressions and portals.
 * </li><li>
 * This pass is of limited utility in the current compiler: To convert to three-address code, this class
 * introduces temporary variables, which require types.  Much of our compiler does not preserve types, so
 * conversion to three-address code can only happen before most of the compiler operations that lose type 
 * information or after a pass to restore type information. (Note that this pass assigns code directly to fields
 * without using an intermidiate calculation into a temp, so field types are not the problem.)
 * </li><li>
 * This does not handle one undocumented langauge feature used by some of our programs: the ability to print
 * mixed types using string concatenation:
 * <pre>
 * int -> void filter ... {
 *   work pop 1 {
 *     println("Got " + pop());
 *   }
 * }
 * </pre>
 * Would yield nonsense code:
 * <pre>
 * java.lang.String tmp_1;
 * int tmp_2;
 * tmp_2 = pop();
 * tmp_1 = "Got " + tmp_2;
 * </pre>
 * This could be handled with some effort, but has not been deemed necessary since the source code
 * <ul><li>
 * would not run on raw anyway, so is not proper source code
 * </li><li>
 * could be rewritten as "print("Got "); println(pop());"
 * </li></ul>
 * </li></ul>
 * @author Allyn Dimock
 */
public class ThreeAddressCode {
    
    /** If using  ThreeAddressCode to convert just parts of the code
     *  you will want to override ThreeAddressExpressionCheck 
     */
    protected final Set<JExpression> exprsToExpand;
    
    /**
     * Statement on latest call to shouldConvertStatement.
     * If you override shouldConvertStatement, keep this up-to-date.
     * Is final, and value is kept as a one-element array in case
     * it is needed in an inner class.
     */
    protected final JStatement[] statementBeingChecked = {null};
    
    /**
     * Constructor, initializes @{link #exprsToExpand}.
     */
    public ThreeAddressCode() {
        exprsToExpand = new HashSet<JExpression> ();
    }
    
    
    /**
     * Turn all filters in stream into three-address code.
     * @param str : stream in which all filters are processed.
     * @return str for convenience: filters munged in place.
     */
    public SIRStream threeAddressCode(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(
                new EmptyStreamVisitor() {
                    /* visit a filter */
                    public void visitFilter(SIRFilter self,
                                                     SIRFilterIter iter) {
                        threeAddressCodeFilter(self);
                    }
                });
        return str;
    }
    
    /**
     * Turn a filter into three-address code.
     * @param filter : filter to process.
     * @return filter for convenience: munged in place.
     */
    public SIRFilter threeAddressCodeFilter(SIRFilter filter) {
        JMethodDeclaration[] ms = filter.getMethods();
        CType inputType = filter.getInputType();
        for (JMethodDeclaration method : ms) {
            threeAddressCodeMethod(method, inputType);
        }
        return filter;
    }
    
    /**
     * Turn a method declaration into three-address code.
     * @param method : method to process the body of.
     * @param inputTapeType : type returned by peek or pop.
     * @return method for convenience: munged in place.
     */
    
    public JMethodDeclaration threeAddressCodeMethod(JMethodDeclaration method,
            CType inputTapeType) {
        JBlock body = method.getBody();
        if (body == null) return method;
        List<JStatement> stmts = threeAddressCodeStatement(body, inputTapeType);
        assert ! stmts.isEmpty();
        JBlock newbody;
        if (stmts.size() == 1 && stmts.get(0) instanceof JBlock) {
            newbody = (JBlock)stmts.get(0);
        } else {
            newbody = new JBlock(body.getTokenReference(),stmts,body.getComments());
        }
        method.setBody(newbody);
        return method;
    }
    
    /**
     * Turn a statement into three-address code.
     * @param stmt : statement to convert to three-address code
     * @param inputTapeType : type returned by peek or pop.
     * @return list of statements that the given statement expands into; may share parts of passed statment.
     */
    public List<JStatement> threeAddressCodeStatement(JStatement stmt,
            CType inputTapeType) {
        return (List<JStatement>)stmt.accept(new S());
    }
    
    /**
     * Determine whether to convert a statement to 3-address code.
     * <br/>
     * Should be overridable for your needs, but you are more likely to override 
     * shouldConvertExpression(JExpression).
     * 
     * Returns that a statement should be converted if any expression directly
     * under the statement in the AST should be converted.  For instance, a "for"
     * statement should be converted if its initialization of check needs conversion;
     * but does not check the sub-statements that are the "for" body or the update, 
     * since these could be converted directly.
     * 
     * If overriding, make sure that you keep statementBeingChecked[0] up-to-date.
     * 
     * One statement that you may want to override: ExpressionListStatement should
     * be converted unless it has a single expression which is simple or should
     * not be converted. 
     * 
     * @param exp : Statement to check as to whether to convert to 3-address code.
     * @return true : override this to return what you need.
     */
    protected boolean shouldConvertStatement(JStatement stmt) {
        statementBeingChecked[0] = stmt;

        // statements mixing other statements and expressions:
        // just check if the top-level expressions need conversion
        if (stmt instanceof JIfStatement) {
            JExpression cond = ((JIfStatement)stmt).getCondition();
            return shouldConvertTopExpression(cond);
        }
        if (stmt instanceof JWhileStatement) {
            JExpression cond = ((JWhileStatement)stmt).getCond();
            return shouldConvertTopExpression(cond);
        }
        if (stmt instanceof JDoStatement) {
            JExpression cond = ((JDoStatement)stmt).getCondition();
            return shouldConvertTopExpression(cond);
      
        }
        if (stmt instanceof JForStatement) {
            JForStatement forstmt = (JForStatement)stmt;
            JExpression cond = forstmt.getCondition();
            // TODO: check. if code generation handles a CompoundStatement in 'init' then no need to convert whole 'for'
            JStatement init = forstmt.getInit();
            JStatement incr = forstmt.getIncrement();
            if (shouldConvertTopExpression(cond)) { 
                return true; 
            }
            return shouldConvertStatement(init) || shouldConvertStatement(incr);
//            // expanded below for breakpoints when debugging
//            if (shouldConvertStatement(init)) {
//                return true;
//            }
//            if (shouldConvertStatement(incr)) {
//                return true;
//            }
//            return false;
        }
        /* RMR { lower return statements if necessary */
        if (stmt instanceof JReturnStatement) {
            JExpression expr = ((JReturnStatement)stmt).getExpression();
            return shouldConvertTopExpression(expr);
        }
        /* } RMR */
        if (stmt instanceof JExpressionStatement) {
            // often in init or incl position of a 'for' statement.
            JExpression expr = ((JExpressionStatement)stmt).getExpression();
            return shouldConvertTopExpression(expr);
        }
        if (stmt instanceof JVariableDeclarationStatement) {
            // convert JVariableDeclarationStatement if any initializer
            // needs to be converted.
            boolean needToConvert = false;
            for (JVariableDefinition def : ((JVariableDeclarationStatement)stmt).getVars()) {
                JExpression initial = def.getValue();
                needToConvert |= (initial != null && shouldConvertTopExpression(def.getValue()));
            }
            return needToConvert;
        }
        if (stmt instanceof JExpressionListStatement) {
            JExpression[] exprs = ((JExpressionListStatement)stmt).getExpressions();
            return exprs.length != 1
             || shouldConvertTopExpression(exprs[0]);
        }
        
        // mark all others to require conversion, and let calls in S
        // to shouldConvertTopExpression sort out what actually needs conversion.
        return true;
    }
     
    /**
     * Determine if should convert expression directly under statement level.
     * If overriding this statement, you need to pass the expression to a
     * subclass of ThreeAddressExpressionCheck that (1) clears any old contents
     * of @{link #exprsToExpand}, (2) Walks the expression and decides whether there are
     * any expressions that it needs to expand, and (3) adds all such expressions
     * to @{link #exprsToExpand}.
     * 
     * ThreeAddressCode will then call another visitor to expand the expressions to
     * three-address code.  Note: if you require an expression to be expanded, then 
     * you should also require all expressions in its context to be expanded, otherwise
     * ThreeAddressCode may (will) produce incorrect code.
     * @param expr Expression to check for expansion
     * @return true if the top-level expression or any subexpressions (recursively) need expanding.
     */
    protected boolean shouldConvertTopExpression(JExpression expr) {
        // case: expression is too simple to ever want to convert.
        if (expr == null || simpleExpression(expr)) {
            return false;
        }
        ThreeAddressExpressionCheck markSubexprsToConvert = 
            new ThreeAddressExpressionCheck(){
            @Override
            protected Object preCheck(Stack<JExpression> context, JExpression self) {
                if (! simpleExpression(self)) {
                    exprsToExpand.add(self);
                }
                return null;
            }
        };
        exprsToExpand.clear();
        expr.accept(markSubexprsToConvert,new Stack<JExpression>());
        return ! exprsToExpand.isEmpty();
    }
    
    /**
     * Determine whether to convert an expression to 3-address code.
     * <br/>
     * Is overridable for your needs, but you are more likely to just
     * override the shouldConvertTopExpression and leave shouldConvertExpression alone.
     * 
     * The default case claims that every expression in exprsToExpand
     * should be converted unless it is an assignment from a simpleExpression() 
     * to a simpleExpression().
     * 
     * @param exp : Expression to check as to whether to convert to 3-address code.
     * @return true : override this to return what you need.
     */
    protected boolean shouldConvertExpression(JExpression exp) {
//        // do not want to xlate "field = simpleexpr" 
//        // into "tmp = simpleexpr; field = tmp"
//        if (exp instanceof JAssignmentExpression) {
//            return ! (simpleExpression(((JAssignmentExpression)exp).getLeft())
//                      && simpleExpression(((JAssignmentExpression)exp).getRight()));
//        }
//        if (exp instanceof JCompoundAssignmentExpression) {
//            return ! (simpleExpression(((JCompoundAssignmentExpression)exp).getLeft())
//                      && simpleExpression(((JCompoundAssignmentExpression)exp).getRight()));
//        }
//        if (exp instanceof JParenthesedExpression) {
//            return shouldConvertExpression(((JParenthesedExpression)exp).getExpr());
//        }
//        return true;
        return exprsToExpand.contains(exp);
    }

    /**
     * Simple expression returns true if an expression should shoud not be passed to E[[]].
     * I.e. the expression is a literal, variable, name, or a field or array
     * access specified using only a simple expression.
     * @param exp : expression to check
     * @return true if no need to convert.
     */
    protected boolean simpleExpression(JExpression exp) {
        if (exp instanceof JLiteral) return true;
        if (exp instanceof JLocalVariableExpression) return true;
        if (exp instanceof JNameExpression) return true;
        if (exp instanceof JThisExpression) return true;

        /* In case of accessing arrays / fields of arrays / fields want to decompose outer accesses
         * so only innermost accesses are considered simple. */

        if (exp instanceof JFieldAccessExpression) {
            return  literalOrVariable(((JFieldAccessExpression)exp).getPrefix());
        }
        if (exp instanceof JClassExpression) {
            return simpleExpression(((JClassExpression)exp).getPrefix());
        }
        if (exp instanceof JArrayAccessExpression) {
            return literalOrVariable(((JArrayAccessExpression)exp).getAccessor())
                && literalOrVariable(((JArrayAccessExpression)exp).getPrefix());
        }
        if (exp instanceof SIRDynamicToken) {
            return true;
        }
        if (exp instanceof JUnqualifiedInstanceCreation) {
            return true;
        }
        if (exp instanceof SIRRangeExpression) {
            return simpleExpression(((SIRRangeExpression)exp).getMin())
                && simpleExpression(((SIRRangeExpression)exp).getAve())
                && simpleExpression(((SIRRangeExpression)exp).getMax());
        }
        return false;
    }

    /**
     * is an expression a literal or a variable?
     * @param exp
     * @return
     */
    public boolean literalOrVariable(JExpression exp) {
        if (exp instanceof JLiteral) return true;
        if (exp instanceof JLocalVariableExpression) return true;
        if (exp instanceof JNameExpression) return true;
        if (exp instanceof JThisExpression) return true;
        return false;
    }

    private static int lastTemp = 0;
    /**
     * nextTemp returns a fresh variable name (hopefully)
     * @return a fresh name.
     */
    public static String nextTemp() {
        lastTemp++;
        return "__tmp" + lastTemp;
    }
    
    /**
     * utility to create a statement assigning an expression to a variable.
     * @param tmp
     * @param expr
     * @return
     */
    private static final JStatement newAssignment(JLocalVariableExpression tmp,
            JExpression expr) {
        return new JExpressionStatement(new JAssignmentExpression(tmp, expr));
    }
    
    /**
     * utility to create a statement list assigning an expression to a variable.
     * @param tmp
     * @param expr
     * @return
     */
    private static final List<JStatement> newAssignmentAsList(JLocalVariableExpression tmp,
            JExpression expr) {
        return singletonStatementList(newAssignment(tmp, expr));
    }
    
    /** 
     * Create a list with a single statement.
     * The list may be added to, later 
     * @param s
     * @return
     */
    private static List<JStatement> singletonStatementList(JStatement s) {
        List<JStatement> stmts = new LinkedList<JStatement>();
        stmts.add(s);
        return stmts;
    }

    
    /**
     * Return list of statements from a single statement by opening up a block
     * body. Also works with a JCompoundStatement. <br/>Warning: side effects to
     * statements will show up in original block.
     * 
     * @param maybeBlock
     *            a block, or other single statement, or null.
     * @return a list of 0 or more statements.
     */
    public static List<JStatement> destructureOptBlock (JStatement maybeBlock) {
        if (maybeBlock instanceof JBlock) {
            return ((JBlock)maybeBlock).getStatements();
        }
        if (maybeBlock instanceof JEmptyStatement || maybeBlock == null) {
            return new LinkedList<JStatement>();
        }
        List<JStatement> retval = new LinkedList<JStatement>();
        retval.add(maybeBlock);
        return retval;
    }
    
    /**
     * Return a single statement from a list of statements by creating a block if needed.
     * <br/>Warning: side effects to statements will show up in created block.
     * @param stmts : a list of statements
     * @return a single statement.
     */
    public static JStatement structureOptBlock (List<JStatement> stmts) {
        int n = stmts.size();
        if (n == 0) {
            return new JEmptyStatement();
        }
        if (n == 1) {
            return stmts.get(0);
        }
        return new JBlock(stmts);
    }

    
    /**
     * Return a single statement from a list of statements by creating a JCompoundStatement if needed.
     * <br/>Warning: side effects to statements will show up in created JCompoundStatement.
     * @param stmts : a list of statements
     * @return a single statement.
     */
    public static JStatement structureOptCompound (List<JStatement> stmts) {
        int n = stmts.size();
        if (n == 0) {
            return new JEmptyStatement();
        }
        if (n == 1) {
            return stmts.get(0);
        }
        return new JCompoundStatement(null,stmts.toArray(new JStatement[stmts.size()]));
    }

    /**
     * Copy TokenReference and JavaStyleComments from one statement to another and return the munged statement.
     * @param old   : statement that has TokenReference and JavaStyleComments that we wish to preserve
     * @param newer : statement that will be munged to have old's TokenReference and JavaStyleComments
     * @return : the munged (newer) statement as a convenience.
     */
    JStatement setWhereAndComments(JStatement old, JStatement newer) {
        if (old != null) {
            newer.setTokenReference(old.getTokenReference());
            newer.setComments(old.getComments());
        }
        return newer;
    }
  
   /**
    * Constructor: override a method with the right arity to make a constructor.
    * Anamorphisms in Java -- what a thrill. 
    */
    abstract class C1 {
        public abstract JExpression mk(JExpression oldSelf, JExpression e1);
    }
    abstract class C2 {
        public abstract JExpression mk(JExpression oldSelf, JExpression e1, JExpression e2);
    }
   
   
   /**
    * Expression visitor.  Only visit when sure that expression requires conversion.
    * E[[expr]](tmp)  ==> list of statements. 
    * Only have expr accept E if ! simpleExpression(expr)
    */
    
    private class E extends ExpressionVisitorBase<List<JStatement>,JLocalVariableExpression> 
    implements ExpressionVisitor<List<JStatement>,JLocalVariableExpression> {
        
        /**
         * Constructor
         */
        E() {
            // all subtypes of Literal act as if visiting Literal.
            redispatchLiteral = true; 
        }
        
        /**
         * Convert a list of expressions to 3-address code.
         * Converts using recurrTopExpression (creates a new tmp for expression evaluation if converting)
         * @param exprs a list of expressions to convert.
         * @return a Pair of a list of statements to preceed the list and a list of expressions to replace the list.
         */
        
        private Pair<List<JStatement>,List<JExpression>>  recurrExpressions (List<JExpression> exprs) {
            List<JStatement> stmts = new LinkedList<JStatement>();
            List<JExpression> newexprs = new LinkedList<JExpression>();
            for (JExpression expr : exprs) {
                Pair<List<JStatement>,JExpression> cvtone = recurrTopExpression(expr);
                stmts.addAll(cvtone.getFirst());
                newexprs.add(cvtone.getSecond());
            }
            return new Pair<List<JStatement>,List<JExpression>>(stmts,newexprs);
        }
        
        /**
         * Convert an array of expressions to 3-address code.
         * Converts using recurrTopExpression. (creates a new tmp for expression evaluation if converting)
         * @param exprs an array of expressions to convert.
         * @return a Pair of a list of statements to preceed the list and a list of expressions to replace the list.
         */
        
        private Pair<List<JStatement>,List<JExpression>>  recurrExpressionsArr (JExpression[] exprs) {
            List<JStatement> stmts = new LinkedList<JStatement>();
            List<JExpression> newexprs = new LinkedList<JExpression>();
            for (JExpression expr : exprs) {
                Pair<List<JStatement>,JExpression> cvtone = recurrTopExpression(expr);
                stmts.addAll(cvtone.getFirst());
                newexprs.add(cvtone.getSecond());
            }
            return new Pair<List<JStatement>,List<JExpression>>(stmts,newexprs);
        }
        
        /**
         * Convert a top-level expression to 3-address code.
         * Checks shouldConvertExpression(expr). 
         * If false returns empty list and passed expression.
         * If true, creates a temp, and returns statements declaring temp and converting into temp,
         * and returns an expression referencing the created temp.
         * @param an expression to convert.
         * @return a Pair of a list of statements to preceed the converted expression and the converted expression.
         */
        
        private Pair<List<JStatement>,JExpression> recurrTopExpression (JExpression expr) {
            List<JStatement> stmts = new LinkedList<JStatement>();
            if (! shouldConvertExpression(expr)) {
                return new Pair<List<JStatement>,JExpression>(stmts,expr);
            } else {
                JVariableDefinition tmp = new JVariableDefinition(expr.getType(), 
                        nextTemp());
                JVariableDeclarationStatement decl = new JVariableDeclarationStatement(
                        tmp);
                JLocalVariableExpression v = new JLocalVariableExpression(
                        tmp);
                List<JStatement> newstmts = new LinkedList<JStatement>();
                newstmts.add(decl);
                newstmts.addAll(recurrExpression(expr,v));
                return new Pair<List<JStatement>,JExpression>(newstmts,v);
            }
        }   
        
        /**
         * Convert an expression to 3-address code.
         * @param an expression to convert.
         * @param a temporary to get the value of the converted expression.
         * @return a list of statements to calculate the value of the expression into the temporary.
         */
        
        private List<JStatement> recurrExpression(JExpression expr, JLocalVariableExpression tmp) {
            // convenient place to hang a breakpoint on top-level conversion.
            return ((List<JStatement>)expr.accept(this,tmp));
        }

        /**
         * Create statement list for an expression with a single subexpression.
         * Performs shouldConvertExpression as part of conversion.
         * @param tmp : temporary variable that is result of evaluating expression
         * @param subexpr : sub-expression that needs to be translated into statements first.
         * @param expr : the whole expression
         * @param constructor : a C1 with mk overridden to create a new expression of the correct type.
         * @return List of statements for subexpr, followed by creation of new expression and assignment to tmp
         */
        private List<JStatement> recurrOneExpr(JLocalVariableExpression tmp,
                JExpression subexpr, JExpression expr, C1 constructor) {
            if (!shouldConvertExpression(expr)) {
                return newAssignmentAsList(tmp,expr);
            } else {
                List<JStatement> newstmts = new LinkedList<JStatement>();
                JExpression v = maybeConvertOneExpr(newstmts, subexpr);
                JExpression newexpr = constructor.mk(expr,v);
                newexpr.setType(expr.getType());
                newstmts.add(newAssignment(tmp, newexpr));
                return newstmts;
            }
        }
        
        /**
         * Create statement list for an expression with two subexpressions.
         * Performs check for shouldConvertExpression as part of conversion.
         * @param tmp : temporary variable that is result of evaluating expression
         * @param subexp1 : sub-expression that needs to be translated into statements first.
         * @param subexp2 : sub-expression that needs to be translated into statements first.
         * @param expr : the whole expression
         * @param constructor : a C2 with mk overridden to create a new expression of the correct type.
         * @return List of statements for subexpr 1,2; followed by creation of new expression and assignment to tmp
         */
         private List<JStatement> recurrTwoExprs(JLocalVariableExpression tmp,
                JExpression subexp1, JExpression subexp2, JExpression expr, C2 constructor) {
            if (! shouldConvertExpression(expr)) {
                return newAssignmentAsList(tmp, expr);
            } else {
                List<JStatement> newstmts = new LinkedList<JStatement>();

                JExpression cvt1 = maybeConvertOneExpr(newstmts,subexp1);
                JExpression cvt2 = maybeConvertOneExpr(newstmts,subexp2);

                JExpression newexpr = constructor.mk(expr,cvt1,cvt2);
                newexpr.setType(expr.getType());
               
                newstmts.add(newAssignment(tmp, newexpr));
                return newstmts;
            }
        }
        
         /**
          * Utility: no check, convert expression using new temp and add to existing list.
          * @param stmts : list of statements (not null)
          * @param exp : JExpression to convert (not null)
          * @return an expression to replace the existing expression.
          */
         private JExpression convertOneExpr(List<JStatement>stmts, JExpression exp) {
             JVariableDefinition defn = new JVariableDefinition(exp.getType(), nextTemp());
             JVariableDeclarationStatement decl = 
                 new JVariableDeclarationStatement(defn);
             JLocalVariableExpression v = new JLocalVariableExpression(defn);
             stmts.add(decl);
             stmts.addAll((List<JStatement>) exp.accept(this, v));
            return v;
         }
         
         /**
          * Utility: check and convert expression if it should be converted.
          * @param stmts
          * @param exp
          * @return
          */
         private JExpression maybeConvertOneExpr(List<JStatement>stmts, JExpression exp) {
             if (shouldConvertExpression(exp)) {
                 return convertOneExpr(stmts,exp);
             } else {
                 return exp;
             }
         }
         
         /**
          * Create singleton statement list for an expression with no subexpressions needing conversion.
          * @param tmp : temporary variable that is result of evaluating expression
          * @param expr : the expression
          * @return creation of new expression and assignment to tmp
          */
  
        private List<JStatement> recurrBase(JLocalVariableExpression tmp, JExpression expr) {
            return newAssignmentAsList(tmp, expr);
        }


        @Override
        public List<JStatement> visitAdd(JAddExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JAddExpression(self.getTokenReference(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitArrayAccess(JArrayAccessExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getPrefix(), self.getAccessor(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression prefix, JExpression accessor) 
                        {return new JArrayAccessExpression(self.getTokenReference(), prefix, accessor, 
                                self.getType());}});
        }

        // No: we should not do this since will fall outside form of C array initializer if
        // tries to init with temps.  Fix by special case in visitArrayDeclaration.
        @Override
        public List<JStatement> visitArrayInitializer(JArrayInitializer self, JLocalVariableExpression tmp) {
            JExpression[] elemArray = self.getElems();
            assert false: "Should not descend into array initializer";
            Pair<List<JStatement>,List<JExpression>> elemresults = this.recurrExpressionsArr(elemArray);
            List<JStatement> newstmts = elemresults.getFirst();
            List<JExpression> newelems = elemresults.getSecond();
            newstmts.add(newAssignment(tmp, 
                            new JArrayInitializer(self.getTokenReference(),
                                    newelems.toArray(new JExpression[newelems.size()]))));
            return newstmts;
        }

        @Override
        public List<JStatement> visitArrayLength(JArrayLengthExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getPrefix(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression prefix) 
                        {return new JArrayLengthExpression(self.getTokenReference(), prefix);}});
        }

        @Override
        public List<JStatement> visitAssignment(JAssignmentExpression self, JLocalVariableExpression tmp) {
            assert false : "Assignment should be handled at statement level";
            return recurrTwoExprs(tmp,self.getLeft(), self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression prefix, JExpression accessor) 
                        {return new JAssignmentExpression(self.getTokenReference(), prefix, accessor);}});

        }

        @Override
        public List<JStatement> visitBinary(JBinaryExpression self, JLocalVariableExpression tmp) {
            assert false : "BinaryExpression should be handled at subtypes";
            return null;
        }

        @Override
        public List<JStatement> visitBinaryArithmetic(JBinaryArithmeticExpression self, JLocalVariableExpression tmp) {
            assert false : "BinaryArithmeticExpression should be handled at subtypes";
            return null;
        }

        @Override
        public List<JStatement> visitBitwise(JBitwiseExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JBitwiseExpression(self.getTokenReference(),
                            ((JBitwiseExpression)self).getOper(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitBitwiseComplement(JBitwiseComplementExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JBitwiseComplementExpression(self.getTokenReference(), expr);}});
        }

        @Override
        public List<JStatement> visitCast(JCastExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JCastExpression(self.getTokenReference(), expr,
                                ((JCastExpression)self).getType());}});
        }

        @Override
        public List<JStatement> visitChecked(JCheckedExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Checked";
            return null;
        }

        @Override
        public List<JStatement> visitClass(JClassExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Class";
            return null;
        }

        @Override
        public List<JStatement> visitCompoundAssignment(JCompoundAssignmentExpression self, JLocalVariableExpression tmp) {
            assert false : "CompoundAssignment should be handled at statement level";
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JCompoundAssignmentExpression(self.getTokenReference(),
                            ((JCompoundAssignmentExpression)self).getOper(),left,right);}});                    
        }

        /**
         * E[[ e1 ? e2 : e3 ]](tmp) = boolean v; E[[e1]](v); if (v) then E[[e2]](tmp) else E[[e3]](tmp).
         * @param self a e1 ? e2 : e3 expression
         * @param tmp a location for the result of evaluating the expression
         * @return
         */
        @Override
        public List<JStatement> visitConditional(JConditionalExpression self, JLocalVariableExpression tmp) {
            if (! shouldConvertExpression(self)) {
                return recurrBase(tmp,self);
            }

            JExpression econd = self.getCond();
            JExpression left = self.getLeft();
            JExpression right = self.getRight();
            boolean convertecond =  shouldConvertExpression(econd);
            boolean convertleft = shouldConvertExpression(left);
            boolean convertright = shouldConvertExpression(right);
            
            List<JStatement> newstmts = new LinkedList<JStatement>();

            JExpression cond;
            JStatement thenPart;
            JStatement elsePart;
            
            if (convertecond) {
                JVariableDefinition tmp2 = new JVariableDefinition(
                        CStdType.Boolean, nextTemp());
                JVariableDeclarationStatement decl = new JVariableDeclarationStatement(
                        tmp2);
                JLocalVariableExpression v = new JLocalVariableExpression(tmp2);
                newstmts.add(decl);
                newstmts.addAll((List<JStatement>) econd.accept(this, v));
                cond = v;
            } else {
                cond = econd;
            }

            if (convertleft) {
                thenPart = structureOptBlock((List<JStatement>) left.accept(this, tmp));
            } else {
                thenPart = structureOptBlock(recurrBase(tmp,left));
            }

            if (convertright) {
                elsePart = structureOptBlock((List<JStatement>) right.accept(this, tmp));
            } else {
                elsePart = structureOptBlock(recurrBase(tmp,right));
            }

            newstmts.add(new JIfStatement(self.getTokenReference(),cond,thenPart,elsePart,null));
            return newstmts;
        }

        /**
         * E[[ e1 && e2 ]](tmp) = E[[e1]](tmp); if (tmp) { E[[e2]](tmp) } else {}
         * @param self  (e1 && e2)
         * @param tmp   a boolean variable
         * @return
         */
        @Override
        public List<JStatement> visitConditionalAnd(JConditionalAndExpression self, JLocalVariableExpression tmp) {
            if (! shouldConvertExpression(self)) {
                // nothing to convert: tmp = self;
                return newAssignmentAsList(tmp, self);
            } else {
                JExpression subexp1 = self.getLeft();
                JExpression subexp2 = self.getRight();
                boolean convert1 = shouldConvertExpression(subexp1);
                boolean convert2 = shouldConvertExpression(subexp2);

                List<JStatement> newstmts = new LinkedList<JStatement>();
                JExpression cond;
                JStatement elseBranch;
                JStatement thenBranch;
                if (convert1) {
                    // need to convert first subexpression => if (tmp) then ... else {}
                    newstmts.addAll((List<JStatement>)subexp1.accept(this,tmp));
                    cond = tmp;
                    elseBranch = new JEmptyStatement();
                } else {
                    // no need to convert first => if (subexpr1) then ... else {tmp = false;}
                    cond = subexp1;
                    elseBranch = newAssignment(tmp, new JBooleanLiteral(null,false));
                }
                if (convert2) {
                    // second subexpression also converts into tmp.
                    thenBranch = structureOptBlock((List<JStatement>)subexp2.accept(this,tmp));
                } else {
                    thenBranch = newAssignment(tmp,subexp2);
                }
                newstmts.add(
                        new JIfStatement(self.getTokenReference(),cond,thenBranch,elseBranch,null));
                return newstmts;
            }
        }

        /**
         * E[[ e1 || e2 ]](tmp) = E[[e1]](tmp); if (tmp) {} else { E[[e2]](tmp) }
         * @param self  (e1 || e2)
         * @param tmp   a boolean variable
         * @return
         */
        @Override
        public List<JStatement> visitConditionalOr(JConditionalOrExpression self, JLocalVariableExpression tmp) {
            if (! shouldConvertExpression(self)) {
                // nothing to convert: tmp = self;
                return newAssignmentAsList(tmp, self);
            } else {
                JExpression subexp1 = self.getLeft();
                JExpression subexp2 = self.getRight();
                boolean convert1 = ! simpleExpression(subexp1) && shouldConvertExpression(subexp1);
                boolean convert2 = ! simpleExpression(subexp2) && shouldConvertExpression(subexp2);

                List<JStatement> newstmts = new LinkedList<JStatement>();
                JExpression cond;
                JStatement elseBranch;
                JStatement thenBranch;
                if (convert1) {
                    // need to convert first subexpression => if (tmp) then {} else ...
                    newstmts.addAll((List<JStatement>)subexp1.accept(this,tmp));
                    cond = new JLogicalComplementExpression(null,tmp);
                    thenBranch = new JEmptyStatement();
                } else {
                    // no need to convert first => if (subexpr1) then {tmp = true} else ...
                    cond = subexp1;
                    thenBranch = newAssignment(tmp, new JBooleanLiteral(null,true));
                }
                if (convert2) {
                    // second subexpression also converts into tmp.
                    elseBranch = structureOptBlock((List<JStatement>)subexp2.accept(this,tmp));
                } else {
                    elseBranch = newAssignment(tmp,subexp2);
                }
                newstmts.add(
                        new JIfStatement(self.getTokenReference(),cond,thenBranch,elseBranch,null));
                return newstmts;
            }
        }

        @Override
        public List<JStatement> visitConstructorCall(JConstructorCall self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression ConstructorCall";
            return null;
        }

        @Override
        public List<JStatement> visitCreatePortal(SIRCreatePortal self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        @Override
        public List<JStatement> visitDivide(JDivideExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JDivideExpression(self.getTokenReference(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitDynamicToken(SIRDynamicToken self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        @Override
        public List<JStatement> visitEquality(JEqualityExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JEqualityExpression(self.getTokenReference(),
                            ((JEqualityExpression)self).getEqual(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitFieldAccess(JFieldAccessExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getPrefix(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression prefix) 
                        {return new JFieldAccessExpression(self.getTokenReference(), prefix, 
                                self.getIdent());}});
        }

        @Override
        public List<JStatement> visitInstanceof(JInstanceofExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Instanceof";
            return null;
        }

        @Override
        public List<JStatement> visitInterfaceTable(SIRInterfaceTable self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression InterfaceTable";
            return null;
        }

        @Override
        public List<JStatement> visitLiteral(JLiteral self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        @Override
        public List<JStatement> visitLocalVariable(JLocalVariableExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        @Override
        public List<JStatement> visitLogicalComplement(JLogicalComplementExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JLogicalComplementExpression(self.getTokenReference(), expr);}});
        }


        @Override
        public List<JStatement> visitMethodCall(JMethodCallExpression self, JLocalVariableExpression tmp) {
            if (! shouldConvertExpression(self)) {
                // nothing to convert: tmp = self;
                return newAssignmentAsList(tmp, self);
            }
            List<JExpression> exprs = Arrays.asList(self.getArgs());
            Pair<List<JStatement>,List<JExpression>> cvtdexprs = this.recurrExpressions(exprs);
            List<JStatement> newstmts = cvtdexprs.getFirst();
            List<JExpression> newexprs = cvtdexprs.getSecond();
            JMethodCallExpression newself = new JMethodCallExpression(self.getTokenReference(),
                    self.getPrefix(),self.getIdent(),newexprs.toArray(new JExpression[newexprs.size()]));
            newself.setType(self.getType());
            newstmts.add(newAssignment(tmp,newself));
            return newstmts;
        }

        @Override
        public List<JStatement> visitMinus(JMinusExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JMinusExpression(self.getTokenReference(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitModulo(JModuloExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JModuloExpression(self.getTokenReference(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitMult(JMultExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JMultExpression(self.getTokenReference(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitName(JNameExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        @Override
        public List<JStatement> visitNewArray(JNewArrayExpression self, JLocalVariableExpression tmp) {
            if (! shouldConvertExpression(self)) {
                // nothing to convert: tmp = self;
                return newAssignmentAsList(tmp, self);
            }            // recur into initializer at this level to keep from using additional temps if the
            // initialization is simple.
            Pair<List<JStatement>,List<JExpression>> cvtddims = recurrExpressionsArr(self.getDims());
            List<JStatement> newstmts = cvtddims.getFirst();
            List<JExpression> newdims = cvtddims.getSecond();
            JArrayInitializer init = self.getInit();
//            JExpression[] initexps = (init == null) ? new JExpression[0] : init.getElems();
//            Pair<List<JStatement>,List<JExpression>> cvtdinits = recurrExpressionsArr(initexps);
//            List<JStatement> initstmts = cvtdinits.getFirst();
//            List<JExpression> initexprs = cvtdinits.getSecond();
            
            JNewArrayExpression newarray;
//            if (initstmts.isEmpty()) {
                // can use old initializer
                newarray = new JNewArrayExpression(self.getTokenReference(), self.getType(),
                        newdims.toArray(new JExpression[newdims.size()]),init);
//            } else {
//                // need a new initializer (and can guarantee old one was not null).
//                newstmts.addAll(initstmts);
//                newarray = new JNewArrayExpression(self.getTokenReference(), self.getType(),
//                        newdims.toArray(new JExpression[newdims.size()]),
//                        new JArrayInitializer(init.getTokenReference(),
//                                initexprs.toArray(new JExpression[initexprs.size()])));
//            }
            newstmts.add(newAssignment(tmp,newarray));
            return newstmts;
                    
        }

        @Override
        public List<JStatement> visitParenthesed(JParenthesedExpression self, JLocalVariableExpression tmp) {
            return (List<JStatement>)self.getExpr().accept(this,tmp);
        }

        @Override
        public List<JStatement> visitPeek(SIRPeekExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getArg(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) {return new SIRPeekExpression(v);}});
        }

        @Override
        public List<JStatement> visitPop(SIRPopExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        @Override
        public List<JStatement> visitPostfix(JPostfixExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) 
                    {return new JPostfixExpression(self.getTokenReference(),
                            ((JPostfixExpression)self).getOper(),v);}});
        }

        @Override
        public List<JStatement> visitPrefix(JPrefixExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) 
                    {return new JPrefixExpression(self.getTokenReference(),
                            ((JPrefixExpression)self).getOper(),v);}});
       }

        @Override
        public List<JStatement> visitPush(SIRPushExpression self, JLocalVariableExpression tmp) {
            assert false : "Push should be handled at statement level";
            return recurrOneExpr(tmp,self.getArg(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) 
                    {return new SIRPushExpression(v,
                            ((SIRPushExpression)self).getTapeType());}});
       }

        @Override
        public List<JStatement> visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression QualifiedAnonymousCreation";
            return null;
        }

        @Override
        public List<JStatement> visitQualifiedInstanceCreation(JQualifiedInstanceCreation self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression QualifiedInstanceCreation";
            return null;
        }

        @Override
        public List<JStatement> visitRange(SIRRangeExpression self, JLocalVariableExpression tmp) {
            if (! shouldConvertExpression(self)) {
                // nothing to convert: tmp = self;
                return newAssignmentAsList(tmp, self);
            }
            
            List<JStatement> newstmts = new LinkedList<JStatement>(); 
            
            JExpression newmin = maybeConvertOneExpr(newstmts,self.getMin());
            JExpression newave = maybeConvertOneExpr(newstmts,self.getAve());
            JExpression newmax = maybeConvertOneExpr(newstmts,self.getMax());
            newstmts.add(
                    newAssignment(tmp,
                            new SIRRangeExpression(newmin,newave,newmax)));
            return newstmts;
        }

        @Override
        public List<JStatement> visitRelational(JRelationalExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JRelationalExpression(self.getTokenReference(),
                            ((JRelationalExpression)self).getOper(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitShift(JShiftExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JShiftExpression(self.getTokenReference(),
                            ((JShiftExpression)self).getOper(),left,right);}});                    
        }

        @Override
        public List<JStatement> visitSuper(JSuperExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Super";
            return null;
        }

        @Override
        public List<JStatement> visitThis(JThisExpression self, JLocalVariableExpression tmp) {
            // there can actually be an expression inside of this "p.this" but shoud not ocur in StreamIt.
            return recurrBase(tmp,self); 
        }

        @Override
        public List<JStatement> visitTypeName(JTypeNameExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self); 
        }

        @Override
        public List<JStatement> visitUnary(JUnaryExpression self, JLocalVariableExpression tmp) {
            assert false : "Unary should be handled at its subtypes";
            return null;
        }

        @Override
        public List<JStatement> visitUnaryMinus(JUnaryMinusExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JUnaryMinusExpression(self.getTokenReference(), expr);}});
        }

        @Override
        public List<JStatement> visitUnaryPlus(JUnaryPlusExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JUnaryPlusExpression(self.getTokenReference(), expr);}});
        }

        @Override
        public List<JStatement> visitUnaryPromote(JUnaryPromote self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JUnaryPromote(expr, self.getType());}});        }

        @Override
        public List<JStatement> visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression UnqualifiedAnonymousCreation";        
            return null;
        }

        @Override
        public List<JStatement> visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self, JLocalVariableExpression tmp) {
            // "new Complex()"  -- should be handled be simpleExpression
            assert false: "UnqualifiedInstanceCreation should be handled before getting here";
            return null;
        }

        
    }

    /**
     * Override some stuff for R-values
     * @author dimock
     *
     */
    private class EL extends E {
        private E rValueProcessor;
        
        EL(E rValueProcessor) {
            this.rValueProcessor = rValueProcessor;
        }
        /**
         * Convert a top-level expression to 3-address code.
         * Checks shouldConvertExpression(expr). 
         * If false returns empty list and passed expression.
         * If true, creates a temp, and returns statements declaring temp and converting into temp,
         * and returns an expression referencing the created temp.
         * @param an expression to convert.
         * @return a Pair of a list of statements to preceed the converted expression and the converted expression.
         */
        
        Pair<List<JStatement>,JExpression> recurrTopExpressionL (JExpression expr) {
            if (! shouldConvertExpression(expr)) {
                return new Pair(new LinkedList<JStatement>(),expr);
            } else {
                return new Pair((List<JStatement>)expr.accept(this,null), expr);
            }
        }
        
        /**
         * For L-values: ArrayAccess is never converted into a tmp.
         * Instead, any prefix is converted as a L-value, accessor is converted as an R-value.
         * Assume unique instance since mung JArrayAccessExpression in place: calling sequence
         * does not allow for a returned value.
         */
        @Override
        public List<JStatement> visitArrayAccess(JArrayAccessExpression self, JLocalVariableExpression tmp) {
            JExpression  prefix = self.getPrefix();
            JExpression  accessor = self.getAccessor();
            boolean convert1 = shouldConvertExpression(prefix);
            boolean convert2 = shouldConvertExpression(accessor);
            if (! convert1 && ! convert2) {
                return new LinkedList<JStatement>();
            }
            
            // prefix converts as an L-value
            List<JStatement> newstmts = new LinkedList<JStatement>();
            JExpression newprefix;
            JExpression newaccessor;
            if (convert1) {
                Pair<List<JStatement>,JExpression> cvtdprefix = 
                    recurrTopExpressionL (prefix);
                newstmts.addAll(cvtdprefix.getFirst());
                newprefix = cvtdprefix.getSecond();
            } else {
                newprefix = prefix; 
            }
                
            // accessor converts as an R-value
            if (convert2) {
                Pair<List<JStatement>,JExpression> cvtdaccessor = 
                    rValueProcessor.recurrTopExpression (accessor);
                newstmts.addAll(cvtdaccessor.getFirst());
                newaccessor = cvtdaccessor.getSecond();
            } else {
                newaccessor = accessor; 
            }
          
            self.setPrefix(newprefix);
            self.setAccessor(newaccessor);
            return newstmts;
        }
   }

   /**
     * Statement visitor.
     * @author dimock
     *
     */
    private class S implements SLIRAttributeVisitor<List<JStatement>> {
        
        
        /**
         * Given a block or compound statement, recurr.
         * If passed a JCompoundStatement and recursion produces > 1 statement
         * then returns a JCompoundStatement.
         * Else if recursion produces > 1 statement returns a JBlock.
         * @param s : a single statement.
         * @return : a statement
         */
        public JStatement recurrStmt(JStatement s) {
            List<JStatement> ss = destructureOptBlock(s);
            List<JStatement> cvtd= recurrStmts(ss);
            JStatement cvtds;
            if (s instanceof JCompoundStatement) {
                cvtds = structureOptCompound(cvtd);
            } else {
                cvtds = structureOptBlock(cvtd);
            }
            setWhereAndComments(s,cvtds);
            return cvtds;
        }
        
        /**
         * Recurr processing a list of statements.
         * @param ss : list of statements
         * @return list of processed statements.
         */
        public List<JStatement> recurrStmts(List<JStatement> ss) {
            List<JStatement> cvtd = new LinkedList<JStatement>();
            for (JStatement s : ss) {
                    cvtd.addAll((List<JStatement>)s.accept(this));
            }
            return cvtd;
        }


        public List<JStatement> visitCreatePortalExpression(SIRCreatePortal self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitDynamicToken(SIRDynamicToken self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitFileReader(LIRFileReader self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitFileWriter(LIRFileWriter self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitFunctionPointer(LIRFunctionPointer self, String name) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitIdentity(LIRIdentity self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitInitStatement(SIRInitStatement self, SIRStream target) {
            if (shouldConvertStatement(self)) {
                List<JExpression> args = self.getArgs();
                if (args == null) { args = Collections.emptyList(); }
                Pair<List<JStatement>,List<JExpression>> cvtdargs = (new E()).recurrExpressions(args);
                List<JStatement> newstmts = cvtdargs.getFirst();
                SIRInitStatement newself = new SIRInitStatement(cvtdargs.getSecond(),target);
                setWhereAndComments(self,newself);
                newstmts.add(newself);
                return newstmts;
            } else {
                return singletonStatementList((JStatement)self);
            }
        }

        public List<JStatement> visitInterfaceTable(SIRInterfaceTable self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitLatency(SIRLatency self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitLatencyMax(SIRLatencyMax self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitLatencyRange(SIRLatencyRange self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitLatencySet(SIRLatencySet self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitMainFunction(LIRMainFunction self, String typeName, LIRFunctionPointer init, List<JStatement> initStatements) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[marker]] = marker
         */
        public List<JStatement> visitMarker(SIRMarker self) {
            return singletonStatementList((JStatement)self);
        }

        /**
         * S[[message(E1,name,ident,[E2...En],latency]] = type1 v1 E[[E1]](v1) ... typen vn E[[En]](vn) 
         *                                                message(v1,name,ident,[v2...vn],latency 
         */
        public List<JStatement> visitMessageStatement(SIRMessageStatement self,
                JExpression portal, String iname, String ident,
                JExpression[] args, SIRLatency latency) {
            if (shouldConvertStatement(self)) {
                List<JExpression> es = new ArrayList<JExpression>(args.length+1); 
                es.add(0, portal);
                es.addAll(Arrays.asList(args));
                Pair<List<JStatement>,List<JExpression>> forexprs = (new E()).recurrExpressions(es);
                List<JStatement> newstmts = forexprs.getFirst();
                List<JExpression> vs = forexprs.getSecond();

                JExpression portalv = vs.get(0);
                vs.remove(0);
                JExpression[] newargs = vs.toArray(new JExpression[vs.size()]);

                SIRMessageStatement newmsg = new SIRMessageStatement(portalv,
                        iname, ident, newargs, latency);
                setWhereAndComments(self,newmsg);
                newstmts.add(newmsg);
                return newstmts;
            } else {
                return singletonStatementList((JStatement)self);
            }
        }

        public List<JStatement> visitNode(LIRNode self) {
            assert false : "ThreeAddressCode does not accept LIRNode";
            return Collections.emptyList();
        }

        public List<JStatement> visitPeekExpression(SIRPeekExpression self, CType tapeType, JExpression arg) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitPopExpression(SIRPopExpression self, CType tapeType) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitPortal(SIRPortal self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitPrintStatement(SIRPrintStatement self, JExpression arg) {
            if (shouldConvertStatement(self)) {
                Pair<List<JStatement>,JExpression> cvtd = (new E()).recurrTopExpression(arg);
                List<JStatement> newstmts = new LinkedList<JStatement>();
                newstmts.addAll(cvtd.getFirst());
                JExpression newexpr = cvtd.getSecond();
                newstmts.add( 
                    new SIRPrintStatement(self.getTokenReference(),newexpr,self.getComments()));
                return newstmts;
            } else {
                return singletonStatementList((JStatement)self);
            }
        }

        public List<JStatement> visitPushExpression(SIRPushExpression self, CType tapeType, JExpression arg) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitRangeExpression(SIRRangeExpression self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[receiverStmt(portal,receiver,itable)]] = type tmp; E[[portal]](tmp); receiverStmt(tmp,receiver,itable)
         * If the method declarations referred to in the iTable need converting, it is the job of something else...
         * @param self
         * @param portal
         * @param receiver
         * @param methods
         * @return xlated
         */
        public List<JStatement> visitRegReceiverStatement(SIRRegReceiverStatement self, JExpression portal, SIRStream receiver, JMethodDeclaration[] methods) {
            if (shouldConvertStatement(self)) {
                Pair<List<JStatement>,JExpression> cvtd = (new E()).recurrTopExpression(portal);
                List<JStatement> newstmts = cvtd.getFirst();
                JExpression newexpr = cvtd.getSecond();
                JStatement newstm = new SIRRegReceiverStatement (newexpr,receiver,self.getItable());
                setWhereAndComments(self,newstm);
                newstmts.add(newstm);
                return newstmts;
            } else {
                return singletonStatementList((JStatement)self);
            }
        }

        public List<JStatement> visitRegSenderStatement(SIRRegSenderStatement self, String portal, SIRLatency latency) {
            return singletonStatementList((JStatement)self);
        }

        public List<JStatement> visitRegisterReceiver(LIRRegisterReceiver self, JExpression streamContext, SIRPortal portal, String childName, SIRInterfaceTable itable) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetBodyOfFeedback(LIRSetBodyOfFeedback self, JExpression streamContext, JExpression childContext, CType inputType, CType outputType, int inputSize, int outputSize) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetChild(LIRSetChild self, JExpression streamContext, String childType, String childName) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetDecode(LIRSetDecode self, JExpression streamContext, LIRFunctionPointer fp) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetDelay(LIRSetDelay self, JExpression data, JExpression streamContext, int delay, CType type, LIRFunctionPointer fp) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetEncode(LIRSetEncode self, JExpression streamContext, LIRFunctionPointer fp) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetJoiner(LIRSetJoiner self, JExpression streamContext, SIRJoinType type, int ways, int[] weights) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetLoopOfFeedback(LIRSetLoopOfFeedback self, JExpression streamContext, JExpression childContext, CType inputType, CType outputType, int inputSize, int outputSize) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetParallelStream(LIRSetParallelStream self, JExpression streamContext, JExpression childContext, int position, CType inputType, CType outputType, int inputSize, int outputSize) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetPeek(LIRSetPeek self, JExpression streamContext, int peek) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetPop(LIRSetPop self, JExpression streamContext, int pop) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetPush(LIRSetPush self, JExpression streamContext, int push) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetSplitter(LIRSetSplitter self, JExpression streamContext, SIRSplitType type, int ways, int[] weights) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetStreamType(LIRSetStreamType self, JExpression streamContext, LIRStreamType streamType) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetTape(LIRSetTape self, JExpression streamContext, JExpression srcStruct, JExpression dstStruct, CType type, int size) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSetWork(LIRSetWork self, JExpression streamContext, LIRFunctionPointer fn) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitWorkEntry(LIRWorkEntry self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitWorkExit(LIRWorkExit self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitArrayAccessExpression(JArrayAccessExpression self, JExpression prefix, JExpression accessor) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitArrayInitializer(JArrayInitializer self, JExpression[] elems) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitArrayLengthExpression(JArrayLengthExpression self, JExpression prefix) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitAssignmentExpression(JAssignmentExpression self, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitBinaryExpression(JBinaryExpression self, String oper, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitBitwiseComplementExpression(JUnaryExpression self, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitBitwiseExpression(JBitwiseExpression self, int oper, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[Block {S1; ... Sn}]] = Block { S[[S1]]; ... S[[Sn]] }
         */
        public List<JStatement> visitBlockStatement(JBlock self,
                JavaStyleComment[] comments) {
            return singletonStatementList(recurrStmt(self));
        }

        public List<JStatement> visitBooleanLiteral(boolean value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[break]] = break
         */
        public List<JStatement> visitBreakStatement(JBreakStatement self, String label) {
            return singletonStatementList((JStatement)self);
        }

        public List<JStatement> visitByteLiteral(byte value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitCastExpression(JCastExpression self, JExpression expr, CType type) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitCatchClause(JCatchClause self, JFormalParameter exception, JBlock body) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitCharLiteral(char value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitClassBody(JTypeDeclaration[] decls, JFieldDeclaration[] fields, JMethodDeclaration[] methods, JPhylum[] body) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitClassDeclaration(JClassDeclaration self, int modifiers, String ident, String superName, CClassType[] interfaces, JPhylum[] body, JFieldDeclaration[] fields, JMethodDeclaration[] methods, JTypeDeclaration[] decls) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitClassExpression(JClassExpression self, CType type) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitClassImport(String name) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitComment(JavaStyleComment comment) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitComments(JavaStyleComment[] comments) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitCompilationUnit(JCompilationUnit self, JPackageName packageName, JPackageImport[] importedPackages, JClassImport[] importedClasses, JTypeDeclaration[] typeDeclarations) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitCompoundAssignmentExpression(JCompoundAssignmentExpression self, int oper, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[ compound(S1 ... Sn)]] = compound(S[[S1]] ... S[[Sn]])
         */
        public List<JStatement> visitCompoundStatement(JCompoundStatement self,
                JStatement[] body) {
            return singletonStatementList(recurrStmt(self));
        }

        public List<JStatement> visitConditionalExpression(JConditionalExpression self, JExpression cond, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitConstructorCall(JConstructorCall self, boolean functorIsThis, JExpression[] params) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitConstructorDeclaration(JConstructorDeclaration self, int modifiers, String ident, JFormalParameter[] parameters, CClassType[] exceptions, JConstructorBlock body) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[continue;]] = continue;
         */
        public List<JStatement> visitContinueStatement(JContinueStatement self, String label) {
            return singletonStatementList((JStatement)self);
        }       
        
        /**
         * S[[ do S while E ]] = boolean v do S[[ {S, E[[E]](v)} ]] while v
         */
        public List<JStatement> visitDoStatement(JDoStatement self, JExpression cond, JStatement body) {
            if (shouldConvertStatement(self)) {
                JVariableDefinition tmp = new JVariableDefinition(CStdType.Boolean, nextTemp());
                JVariableDeclarationStatement decl = new JVariableDeclarationStatement(tmp);
                JLocalVariableExpression v = new JLocalVariableExpression(tmp);

                List<JStatement> convertedBody = recurrStmts(destructureOptBlock(body));
                convertedBody.addAll((new E()).recurrExpression(cond, v));
                
                JStatement newbody = structureOptBlock(convertedBody);
                JDoStatement newdo = new JDoStatement(self.getTokenReference(),v,newbody,self.getComments());

                List<JStatement> newstmts = new LinkedList<JStatement>();
                newstmts.add(decl);
                newstmts.add(newdo);
                return newstmts;
            } else {
                return singletonStatementList((JStatement)
                        new JDoStatement(self.getTokenReference(), cond,
                                recurrStmt(body),self.getComments()));
            }
        }

        public List<JStatement> visitDoubleLiteral(double value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[;]] =       // empty list.
         */
        public List<JStatement> visitEmptyStatement(JEmptyStatement self) {
            return new LinkedList<JStatement>();
        }

        public List<JStatement> visitEqualityExpression(JEqualityExpression self, boolean equal, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[expressionListStatement(E1; ... EN)]] = type v1; E[[E1]](v1) ... type vn; E[[En]](vn) 
         */
        public List<JStatement> visitExpressionListStatement(
                JExpressionListStatement self, JExpression[] expr) {
            List<JStatement> newstmts = new LinkedList<JStatement>();
            for (JExpression e : expr) {
                // convert to individual JExpressionStatement's so can handle
                // any interesting cases there.
                newstmts.addAll((List<JStatement>)(new JExpressionStatement(self
                        .getTokenReference(), e, self.getComments())).accept(this));
            }

            return newstmts;
        }
        
        /**
         * S[[E]] = type v; E[[E]](v)
         */
        public List<JStatement> visitExpressionStatement(JExpressionStatement self, JExpression expr) {
            if (shouldConvertStatement(self)) {
                List<JStatement> newstmts = new LinkedList<JStatement>();
                if (expr instanceof SIRPushExpression) {
                    // Push should have been a statement.
                    SIRPushExpression pexpr = (SIRPushExpression)expr;
                    JExpression subexpr = pexpr.getArg();
                    // Decide on converting whole push expression, not just its argument.
                    if (shouldConvertTopExpression(pexpr)) {
                        JExpression v = (new E()).convertOneExpr(newstmts,subexpr);
                        newstmts.add(new JExpressionStatement(self.getTokenReference(),
                                new SIRPushExpression(v,pexpr.getTapeType()),
                                self.getComments()));
                        return newstmts;
                    } else {
                        return singletonStatementList((JStatement)self);
                    }
                } 
                
                if (expr instanceof JPrefixExpression || expr instanceof JPostfixExpression) {
                    // no value, only side effect.
                    return singletonStatementList((JStatement)self);
                }
 
                if (expr instanceof JAssignmentExpression) {
                    // assignment used at statement level.
                    // (only usable at statement level since it has void type!)
                    if (expr instanceof JCompoundAssignmentExpression) {
                        // "compound assignment" = assignment + unary operation.
                        JCompoundAssignmentExpression aexpr = (JCompoundAssignmentExpression)expr;
                        E expressionProcessor = new E();
                        Pair<List<JStatement>,JExpression> cnvtleft = 
                            (new EL(expressionProcessor)).recurrTopExpressionL(aexpr.getLeft());
                        Pair<List<JStatement>,JExpression> cnvtright =  
                            expressionProcessor.recurrTopExpression(aexpr.getRight());
                        newstmts.addAll(cnvtleft.getFirst());
                        newstmts.addAll(cnvtright.getFirst());
                        newstmts.add(new JExpressionStatement(self.getTokenReference(),
                                new JCompoundAssignmentExpression(aexpr.getTokenReference(),
                                        aexpr.getOper(),
                                        cnvtleft.getSecond(),cnvtright.getSecond()),
                                self.getComments()));
                        return newstmts;
                    }
                    // basic assignment
                    JAssignmentExpression aexpr = (JAssignmentExpression)expr;
                    E expressionProcessor = new E();
                    Pair<List<JStatement>,JExpression> cnvtleft = 
                        (new EL(expressionProcessor)).recurrTopExpressionL(aexpr.getLeft());
                    Pair<List<JStatement>,JExpression> cnvtright =  
                        expressionProcessor.recurrTopExpression(aexpr.getRight());
                    newstmts.addAll(cnvtleft.getFirst());
                    newstmts.addAll(cnvtright.getFirst());
                    newstmts.add(new JExpressionStatement(self.getTokenReference(),
                            new JAssignmentExpression(aexpr.getTokenReference(),
                                    cnvtleft.getSecond(),cnvtright.getSecond()),
                            self.getComments()));
                    return newstmts;
                }

            
                // not a special case of JExpressionStatement and requires conversion
                (new E()).convertOneExpr(newstmts,expr);
                return newstmts;
            } else {
                // statement does not require conversion.
                return singletonStatementList((JStatement)self);
            }
        }

        public List<JStatement> visitFieldDeclaration(JFieldDeclaration self, int modifiers, CType type, String ident, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitFieldExpression(JFieldAccessExpression self, JExpression left, String ident) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitFloatLiteral(float value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[for (S1,E,S2) S3]] = S[[S1]] S[[while E {S2; S3}]]
         */
        public List<JStatement> visitForStatement(JForStatement self, JStatement init, JExpression cond, JStatement incr, JStatement body) {
            if (shouldConvertStatement(self)) {
                List<JStatement> newstmts = new LinkedList<JStatement>();
                newstmts.addAll((List<JStatement>) init.accept(this));
                
                List<JStatement> bodystmts = destructureOptBlock(body);
                bodystmts.addAll(destructureOptBlock(incr));
                JStatement newbody = structureOptBlock(bodystmts);
                JWhileStatement wstmt = new JWhileStatement(self.getTokenReference(),cond,newbody,self.getComments());
                newstmts.addAll((List<JStatement>) wstmt.accept(this));
                return newstmts;
            } else {
                return singletonStatementList((JStatement)
                        new JForStatement(
                                self.getTokenReference(),
                                init, cond, incr,
                                recurrStmt(body),
                                self.getComments()
                        ));
            }
        }

        public List<JStatement> visitFormalParameters(JFormalParameter self, boolean isFinal, CType type, String ident) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[if E then S1 else S2]] = boolean v; E[[E]](v); if v then S[[S1]] else S[[S2]]
         */
        public List<JStatement> visitIfStatement(JIfStatement self, JExpression cond, JStatement thenClause, JStatement elseClause) {
            if (shouldConvertStatement(self)) {
                Pair<List<JStatement>,JExpression> xlateCond = (new E()).recurrTopExpression(cond);
                List<JStatement> newstmts = xlateCond.getFirst();
                JExpression newexpr = xlateCond.getSecond();

                JIfStatement newIf = new JIfStatement(self.getTokenReference(),
                        newexpr,
                        recurrStmt(thenClause),
                        recurrStmt(elseClause),
                        self.getComments());

                newstmts.add(newIf);
                return newstmts;
            } else {
                return singletonStatementList((JStatement)
                        new JIfStatement(
                                self.getTokenReference(),cond,
                                recurrStmt(thenClause),
                                recurrStmt(elseClause),
                                self.getComments()));
            }
        }

        public List<JStatement> visitInnerClassDeclaration(JClassDeclaration self, int modifiers, String ident, String superName, CClassType[] interfaces, JTypeDeclaration[] decls, JPhylum[] body, JFieldDeclaration[] fields, JMethodDeclaration[] methods) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitInstanceofExpression(JInstanceofExpression self, JExpression expr, CType dest) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitIntLiteral(int value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitInterfaceDeclaration(JInterfaceDeclaration self, int modifiers, String ident, CClassType[] interfaces, JPhylum[] body, JMethodDeclaration[] methods) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitJavadoc(JavadocComment comment) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitLabeledStatement(JLabeledStatement self, String label, JStatement stmt) {
            assert false : "LabeledStatement not in StreamIt language";
            return Collections.emptyList();
        }

        public List<JStatement> visitLocalVariableExpression(JLocalVariableExpression self, String ident) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitLogicalComplementExpression(JUnaryExpression self, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitLongLiteral(long value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitMethodCallExpression(JMethodCallExpression self, JExpression prefix, String ident, JExpression[] args) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitMethodDeclaration(JMethodDeclaration self, int modifiers, CType returnType, String ident, JFormalParameter[] parameters, CClassType[] exceptions, JBlock body) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitNameExpression(JNameExpression self, JExpression prefix, String ident) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitNewArrayExpression(JNewArrayExpression self, CType type, JExpression[] dims, JArrayInitializer init) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitNullLiteral() {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitPackageImport(String name) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitPackageName(String name) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitParenthesedExpression(JParenthesedExpression self, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitPostfixExpression(JPostfixExpression self, int oper, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitPrefixExpression(JPrefixExpression self, int oper, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self, JExpression prefix, String ident, JExpression[] params, JClassDeclaration decl) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitQualifiedInstanceCreation(JQualifiedInstanceCreation self, JExpression prefix, String ident, JExpression[] params) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitRelationalExpression(JRelationalExpression self, int oper, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitReturnStatement(JReturnStatement self, JExpression expr) {
            if (shouldConvertStatement(self)) {
                Pair<List<JStatement>,JExpression> xlateCond = (new E()).recurrTopExpression(expr);
                List<JStatement> newstmts = xlateCond.getFirst();
                JExpression newexpr = xlateCond.getSecond();
                newstmts.add(new JReturnStatement(
                        self.getTokenReference(),
                        newexpr,
                        self.getComments()));
                return newstmts;
            } else {
                return singletonStatementList((JStatement)self);
            }
        }

        public List<JStatement> visitShiftExpression(JShiftExpression self, int oper, JExpression left, JExpression right) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitShortLiteral(short value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitStringLiteral(String value) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSuperExpression(JSuperExpression self) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSwitchGroup(JSwitchGroup self, JSwitchLabel[] labels, JStatement[] stmts) {
            assert false : "Switch groups not in StreamIt language";
            return Collections.emptyList();
        }

        public List<JStatement> visitSwitchLabel(JSwitchLabel self, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitSwitchStatement(JSwitchStatement self, JExpression expr, JSwitchGroup[] body) {
            assert false : "Switch not in StreamIt language";
            return Collections.emptyList();
        }

        public List<JStatement> visitSynchronizedStatement(JSynchronizedStatement self, JExpression cond, JStatement body) {
            assert false : "Synchronized not in StreamIt language";
            return Collections.emptyList();
        }

        public List<JStatement> visitThisExpression(JThisExpression self, JExpression prefix) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitThrowStatement(JThrowStatement self, JExpression expr) {
            assert false : "Throw not in StreamIt language";
            return Collections.emptyList();
        }

        public List<JStatement> visitTryCatchStatement(JTryCatchStatement self, JBlock tryClause, JCatchClause[] catchClauses) {
            assert false : "TryCatch not in StreamIt language";
            return Collections.emptyList();
        }

        public List<JStatement> visitTryFinallyStatement(JTryFinallyStatement self, JBlock tryClause, JBlock finallyClause) {
            assert false : "TryFinally not in StreamIt language";
            return Collections.emptyList();
        }

        public List<JStatement> visitTypeDeclarationStatement(JTypeDeclarationStatement self, JTypeDeclaration decl) {
            assert false : "Class declarations not in StreamIt language.";
            return Collections.emptyList();
        }

        public List<JStatement> visitTypeNameExpression(JTypeNameExpression self, CType type) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitUnaryMinusExpression(JUnaryExpression self, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitUnaryPlusExpression(JUnaryExpression self, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitUnaryPromoteExpression(JUnaryPromote self, JExpression expr, CType type) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self, CClassType type, JExpression[] params, JClassDeclaration decl) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        public List<JStatement> visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self, CClassType type, JExpression[] params) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[type1 v1 = E1 ... typen vn = En]] = type1 v1; v1 = E[[E1]](v1) .. typen vn; vn = E[[En]](vn)
         * If E[[E]]v produces a single statement, we could recombine declaration and init into a 
         * single declaration, but this is not guaranteed.
         */
        public List<JStatement> visitVariableDeclarationStatement(JVariableDeclarationStatement self, JVariableDefinition[] vars) {
            if (shouldConvertStatement(self)) {
                List<JStatement> newstmts = new LinkedList<JStatement>();
                for (JVariableDefinition var : vars) {
                    JExpression initializer = var.getValue();
                    if (initializer != null && shouldConvertExpression(initializer)
                            // this final check is because C can only initialize an array at declaration time.
                            && ! (initializer instanceof JArrayInitializer)) {
                        
                        // wipe out initializer in place in case there is still code checking == on JVariableDefinition
                        var.setValue(null);
                        newstmts.add(new JVariableDeclarationStatement(self.getTokenReference(),
                                var, self.getComments()));
                        List<JStatement> initstmts = (List<JStatement>)
                            initializer.accept(new E(), new JLocalVariableExpression(var));
                        newstmts.addAll(initstmts);
                    } else { // don't need to convert this definition.
                        JVariableDeclarationStatement decl = new JVariableDeclarationStatement(self.getTokenReference(),
                                var, self.getComments());
                        newstmts.add(decl);
                    }
                }
                return newstmts;
            } else {
                return singletonStatementList((JStatement)self);
            }
        }

        public List<JStatement> visitVariableDefinition(JVariableDefinition self, int modifiers, CType type, String ident, JExpression expr) {
            assert false : "Not a statement";
            return Collections.emptyList();
        }

        /**
         * S[[while (E) S]] = S[[do {if ! E then break; S} while true]]
         * This particluar expansion so that we do not increase code size for
         * large expressions E.  More standard would be:
         * S[[while (E) S]] = E[[E]](tmp); while (tmp) {S[[S]]; E[[E]](tmp)}
         * 
         * problem with "if ! E then break" being removed by Propagator in some
         * odd circumstances (examples/cookbook/BPFProgram) inside "do".
         * S[[while (E) S]] = while (true) { if ! E then break; S } to
         * seems to work with propagator, so using that.
         */
        public List<JStatement> visitWhileStatement(JWhileStatement self, JExpression cond, JStatement body) {
            if (shouldConvertStatement(self)) {
                
//                JBreakStatement brk = new JBreakStatement(null,null,null);
//                JLogicalComplementExpression notE = new JLogicalComplementExpression(null,cond);
//                JIfStatement breakif = new JIfStatement(null,notE,brk,new JEmptyStatement(),null);
//                List<JStatement> dobodylist = destructureOptBlock(body);
//                dobodylist.add(0, breakif);
//                JStatement dobody = structureOptBlock(dobodylist);
//                JDoStatement doit = new JDoStatement(self.getTokenReference(),
//                        new JBooleanLiteral(null,true),
//                        dobody, self.getComments());
//                
//                return (List<JStatement>) doit.accept(this);

                JBreakStatement brk = new JBreakStatement(null,null,null);
                JLogicalComplementExpression notE = new JLogicalComplementExpression(null,cond);
                JIfStatement breakif = new JIfStatement(null,notE,brk,new JEmptyStatement(),null);
                List<JStatement> dobodylist = destructureOptBlock(body);
                dobodylist.add(0, breakif);
                JStatement dobody = structureOptBlock(dobodylist);
                JWhileStatement doit = new JWhileStatement(self.getTokenReference(),
                        new JBooleanLiteral(null,true),
                        dobody, self.getComments());
                
                return (List<JStatement>) doit.accept(this);
               
            } else {
                return singletonStatementList((JStatement)
                        new JWhileStatement(self.getTokenReference(),cond,
                                recurrStmt(body),
                                self.getComments()));
            }
        }

        public List<JStatement> visitBooleanLiteral(JBooleanLiteral self, boolean value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitByteLiteral(JByteLiteral self, byte value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitCharLiteral(JCharLiteral self, char value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitDoubleLiteral(JDoubleLiteral self, double value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitEmittedTextExpression(JEmittedTextExpression self, Object[] parts) {
            throw new AssertionError("Not a statement");
        }


        public List<JStatement> visitFloatLiteral(JFloatLiteral self, float value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitIntLiteral(JIntLiteral self, int value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitLongLiteral(JLongLiteral self, long value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitNullLiteral(JNullLiteral self) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitShortLiteral(JShortLiteral self, short value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitStringLiteral(JStringLiteral self, String value) {
            assert false: "Not a statement";
            return null;
        }

        public List<JStatement> visitVectorLiteral(JVectorLiteral self, JLiteral scalar) {
            assert false: "Not a statement";
            return null;
        }

    }
}

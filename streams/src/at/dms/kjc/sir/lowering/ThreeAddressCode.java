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
 * Turn statments into three-address code optionally depending on overridable shouldConvertExpression method.
 * <br/>Allowing the user to override shouldConvertExpression forces this class to be non-static.
 * <br/>Note:
 * <ul><li>
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
 * would not run on raw.
 * </li><li>
 * could be rewritten as "print("Got "); println(pop());"
 * </li></ul>
 * </li></ul>
 * @author Allyn Dimock
 */
public class ThreeAddressCode {
    
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
     * Determine whether to convert a statment to 3-address code.
     * <br/>
     * Should be overridable for your needs, but you are more likely to override 
     * shouldConvertExpression(JExpression).
     * 
     * @param exp : Statement to check as to whether to convert to 3-address code.
     * @return true : override this to return what you need.
     */
    protected boolean shouldConvertStatement(JStatement stmt) {
        // statements mixing other statements and expressions:
        // just check if the top-level expressions need conversion
        if (stmt instanceof JIfStatement) {
            JExpression cond = ((JIfStatement)stmt).getCondition();
            return ! simpleExpression(cond) && shouldConvertExpression(cond);
        }
        if (stmt instanceof JWhileStatement) {
            JExpression cond = ((JWhileStatement)stmt).getCond();
            return ! simpleExpression(cond) && shouldConvertExpression(cond);
        }
        if (stmt instanceof JDoStatement) {
            JExpression cond = ((JDoStatement)stmt).getCondition();
            return ! simpleExpression(cond) && shouldConvertExpression(cond);
      
        }
        if (stmt instanceof JForStatement) {
            JForStatement forstmt = (JForStatement)stmt;
            JExpression cond = forstmt.getCondition();
            // TODO: check. if code generation handles a CompoundStatement in 'init' then no need to convert whole 'for'
            JStatement init = forstmt.getInit();
            JStatement incr = forstmt.getIncrement();
            if (! simpleExpression(cond) && shouldConvertExpression(cond)) { return true; }
            return shouldConvertStatement(init) || shouldConvertStatement(incr);
        }
        if (stmt instanceof JExpressionStatement) {
            // often in init or incl position of a 'for' statement.
            JExpression expr = ((JExpressionStatement)stmt).getExpression();
            return ! simpleExpression(expr) && shouldConvertExpression(expr);
        }
        if (stmt instanceof JVariableDeclarationStatement) {
            // occurs in init of for: if simple enough don't convert.
            JVariableDefinition[] vars = ((JVariableDeclarationStatement)stmt).getVars();
            if (vars.length == 1) {
                JVariableDefinition var = vars[0];
                if (var.getValue() == null) {
                    return false;
                } else {
                    JExpression ini = var.getValue();
                    return ! simpleExpression(ini) && shouldConvertExpression(ini); 
                } 
            } else {
                return true; // complex enough to not affect to many for statements.
            }
        }
        // mark all others to require conversion, and let calls in S
        // to shouldConvertExpression sort out what actually needs conversion.
        return true;
    }
 
    
    /**
     * Determine whether to convert an expression to 3-address code.
     * <br/>
     * Should be overridable for your needs -- which is the only reason this
     * class is not static.
     * @param exp : Expression to check as to whether to convert to 3-address code.
     * @return true : override this to return what you need.
     */
    protected boolean shouldConvertExpression(JExpression expr) {
        return true;
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
        if (exp instanceof JFieldAccessExpression) {
            return  simpleExpression(((JFieldAccessExpression)exp).getPrefix());
        }
        if (exp instanceof JClassExpression) {
            return simpleExpression(((JClassExpression)exp).getPrefix());
        }
        if (exp instanceof JArrayAccessExpression) {
            return simpleExpression(((JArrayAccessExpression)exp).getAccessor())
                && simpleExpression(((JArrayAccessExpression)exp).getPrefix());
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
        // do not want to xlate "field = simpleexpr" 
        // into "tmp = simpleexpr; field = tmp"
        if (exp instanceof JAssignmentExpression) {
            return simpleExpression(((JAssignmentExpression)exp).getLeft())
                && simpleExpression(((JAssignmentExpression)exp).getRight());
        }
        if (exp instanceof JCompoundAssignmentExpression) {
            return simpleExpression(((JCompoundAssignmentExpression)exp).getLeft())
            && simpleExpression(((JCompoundAssignmentExpression)exp).getRight());
        }
        return false;
    }


    private static int lastTemp = 0;
    /**
     * nextTemp returns a fresh variable name (hopefully)
     */
    public static String nextTemp() {
        lastTemp++;
        return "__tmp" + lastTemp;
    }
    
    /**
     * Return list of statements from a single statement by opening up a block body.
     * Also works with a JCompoundStatement.
     * <br/>Warning: side effects to statements will show up in original block.
     * @param maybeBlock a block, or other single statement, or null.
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
     * @return : the munged (newer) statment as a convenience.
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
   
    private List<JStatement> singletonStatementList(JStatement s) {
        List<JStatement> stmts = new LinkedList<JStatement>();
        stmts.add(s);
        return stmts;
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
        E() { redispatchLiteral = true; }
        
        /**
         * Convert a list of expressions to 3-address code.
         * Converts using recurrTopExpression.
         * @param exprs a list of expressions to convert.
         * @return a Pair of a list of statements to preceed the list and a list of expressions to replace the list.
         */
        
        Pair<List<JStatement>,List<JExpression>> recurrExpressions(List<JExpression> exprs) {
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
         * Converts using recurrTopExpression.
         * @param exprs an array of expressions to convert.
         * @return a Pair of a list of statements to preceed the list and a list of expressions to replace the list.
         */
        
        Pair<List<JStatement>,List<JExpression>> recurrExpressionsArr(JExpression[] exprs) {
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
         * If true, creates a temp, and returns statments declaring temp and converting into temp,
         * and returns an expression referencing the created temp.
         * @param an expression to convert.
         * @return a Pair of a list of statements to preceed the converted expression and the converted expression.
         */
        
        Pair<List<JStatement>,JExpression> recurrTopExpression (JExpression expr) {
            List<JStatement> stmts = new LinkedList<JStatement>();
            if (simpleExpression(expr) || ! shouldConvertExpression(expr)) {
                return new Pair(stmts,expr);
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
                return new Pair(newstmts,v);
            }
        }   
        
        /**
         * Convert an expression to 3-address code.
         * Checks for simpleExpression() (but not for shouldConvertExpression)
         * If false returns empty list and passed expression.
         * If true, creates a temp, and returns statments declaring temp and converting into temp,
         * and returns an expression referencing the created temp.
         * @param an expression to convert.
         * @return a Pair of a list of statements to preceed the converted expression and the converted expression.
         */
        
        List<JStatement> recurrExpression(JExpression expr, JLocalVariableExpression tmp) {
            if (simpleExpression(expr)) {
                return singletonStatementList((JStatement)
                        (new JExpressionStatement(
                                new JAssignmentExpression(tmp, expr))));
            } else {
                return ((List<JStatement>)expr.accept(this,tmp));
            }
        }

        /**
         * Create statement list for an expression with a single subexpression.
         * Performs check for simpleExpression and shouldConvertExpression as part of conversion.
         * @param tmp : temporary variable that is result of evaluating expression
         * @param subexpr : sub-expression that needs to be translated into statements first.
         * @param expr : the whole expression
         * @param constructor : a C1 with mk overridden to create a new expression of the correct type.
         * @return List of statments for subexpr, followed by creation of new expression and assignment to tmp
         */
        List<JStatement> recurrOneExpr(JLocalVariableExpression tmp,
                JExpression subexpr, JExpression expr, C1 constructor) {
            if (simpleExpression(subexpr) || !shouldConvertExpression(subexpr)) {
                return singletonStatementList((JStatement) 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp, expr)));
            } else {
                List<JStatement> newstmts = new LinkedList<JStatement>();
                JExpression v = convertOneExpr(newstmts, subexpr);
                newstmts.add(new JExpressionStatement(
                        new JAssignmentExpression(tmp, constructor.mk(expr,v))));
                return newstmts;
            }
        }
        
        /**
         * Create statement list for an expression with a single subexpression.
         * Performs check for simpleExpression and shouldConvertExpression as part of conversion.
         * @param tmp : temporary variable that is result of evaluating expression
         * @param subexp1 : sub-expression that needs to be translated into statements first.
         * @param subexp2 : sub-expression that needs to be translated into statements first.
         * @param expr : the whole expression
         * @param constructor : a C2 with mk overridden to create a new expression of the correct type.
         * @return List of statments for subexpr 1,2; followed by creation of new expression and assignment to tmp
         */
         List<JStatement> recurrTwoExprs(JLocalVariableExpression tmp,
                JExpression subexp1, JExpression subexp2, JExpression expr, C2 constructor) {
            boolean convert1 = ! simpleExpression(subexp1) && shouldConvertExpression(subexp1);
            boolean convert2 = ! simpleExpression(subexp2) && shouldConvertExpression(subexp2);
            if (! convert1 && ! convert2) {
                return singletonStatementList((JStatement) 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp, expr)));
            } else {
                List<JStatement> newstmts = new LinkedList<JStatement>();

                JExpression cvt1 = convert1 ?
                        convertOneExpr(newstmts,subexp1)
                        : subexp1;
                
                JExpression cvt2 = convert2 ?
                        convertOneExpr(newstmts,subexp2)
                        : subexp2;
                
                newstmts.add(new JExpressionStatement(
                        new JAssignmentExpression(tmp, constructor.mk(expr,cvt1,cvt2))));
                return newstmts;
            }
        }
        
         /**
          * Utility: no check, convert expression using new temp and add to existing list.
          * @param stmts : list of statements (not null)
          * @param exp : JExpression to convert (not null)
          * @return an expression to replace the existing expression.
          */
         JExpression convertOneExpr(List<JStatement>stmts, JExpression exp) {
             JVariableDefinition defn = new JVariableDefinition(exp.getType(), nextTemp());
             JVariableDeclarationStatement decl = 
                 new JVariableDeclarationStatement(defn);
             JLocalVariableExpression v = new JLocalVariableExpression(defn);
             stmts.add(decl);
             stmts.addAll((List<JStatement>) exp.accept(this, v));
            return v;
         }
         
         /**
          * Create singleton statement list for an expression with no subexpressions needing conversion.
          * Performs check for simpleExpression and shouldConvertExpression as part of conversion.
          * @param tmp : temporary variable that is result of evaluating expression
          * @param expr : the expression
          * @return creation of new expression and assignment to tmp
          */
  
        List<JStatement> recurrBase(JLocalVariableExpression tmp, JExpression expr) {
            return singletonStatementList((JStatement)
                    new JExpressionStatement(
                            new JAssignmentExpression(tmp, expr)));
        }


     
        public List<JStatement> visitAdd(JAddExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JAddExpression(self.getTokenReference(),left,right);}});                    
        }

        public List<JStatement> visitArrayAccess(JArrayAccessExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getPrefix(), self.getAccessor(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression prefix, JExpression accessor) 
                        {return new JArrayAccessExpression(self.getTokenReference(), prefix, accessor, 
                                self.getType());}});
        }

        public List<JStatement> visitArrayInitializer(JArrayInitializer self, JLocalVariableExpression tmp) {
            JExpression[] elemArray = self.getElems();
            Pair<List<JStatement>,List<JExpression>> elemresults = this.recurrExpressionsArr(elemArray);
            List<JStatement> newstmts = elemresults.getFirst();
            List<JExpression> newelems = elemresults.getSecond();
            newstmts.add(new JExpressionStatement(
                    new JAssignmentExpression(tmp, 
                            new JArrayInitializer(self.getTokenReference(),
                                    newelems.toArray(new JExpression[newelems.size()])))));
            return newstmts;
        }

        public List<JStatement> visitArrayLength(JArrayLengthExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getPrefix(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression prefix) 
                        {return new JArrayLengthExpression(self.getTokenReference(), prefix);}});
        }

        public List<JStatement> visitAssignment(JAssignmentExpression self, JLocalVariableExpression tmp) {
            assert false : "Assignment should be handled at statement level";
            return recurrTwoExprs(tmp,self.getLeft(), self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression prefix, JExpression accessor) 
                        {return new JAssignmentExpression(self.getTokenReference(), prefix, accessor);}});

        }

        public List<JStatement> visitBinary(JBinaryExpression self, JLocalVariableExpression tmp) {
            assert false : "BinaryExpression should be handles at subtypes";
            return null;
        }

        public List<JStatement> visitBinaryArithmetic(JBinaryArithmeticExpression self, JLocalVariableExpression tmp) {
            assert false : "BinaryArithmeticExpression should be handles at subtypes";
            return null;
        }

        public List<JStatement> visitBitwise(JBitwiseExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JBitwiseExpression(self.getTokenReference(),
                            ((JBitwiseExpression)self).getOper(),left,right);}});                    
        }

        public List<JStatement> visitBitwiseComplement(JBitwiseComplementExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JBitwiseComplementExpression(self.getTokenReference(), expr);}});
        }

        public List<JStatement> visitCast(JCastExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JCastExpression(self.getTokenReference(), expr,
                                ((JCastExpression)self).getType());}});
        }

        public List<JStatement> visitChecked(JCheckedExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Checked";
            return null;
        }

        public List<JStatement> visitClass(JClassExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Class";
            return null;
        }

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
        public List<JStatement> visitConditional(JConditionalExpression self, JLocalVariableExpression tmp) {
            JExpression econd = self.getCond();
            JExpression left = self.getLeft();
            JExpression right = self.getRight();
            boolean convertecond = ! simpleExpression(econd) && shouldConvertExpression(econd);
            boolean convertleft = ! simpleExpression(left) && shouldConvertExpression(left);
            boolean convertright = ! simpleExpression(right) && shouldConvertExpression(right);
            
            if (! (convertecond || convertleft || convertright)) {
                return recurrBase(tmp,self);
            }

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
        public List<JStatement> visitConditionalAnd(JConditionalAndExpression self, JLocalVariableExpression tmp) {
            JExpression subexp1 = self.getLeft();
            JExpression subexp2 = self.getRight();
            boolean convert1 = ! simpleExpression(subexp1) && shouldConvertExpression(subexp1);
            boolean convert2 = ! simpleExpression(subexp2) && shouldConvertExpression(subexp2);
            if (! convert1 && ! convert2) {
                // nothing to convert: tmp = self;
                return singletonStatementList((JStatement) 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp, self)));
            } else {
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
                    elseBranch = 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp,
                                        new JBooleanLiteral(null,false)));
                }
                if (convert2) {
                    // second subexpression also converts into tmp.
                    thenBranch = structureOptBlock((List<JStatement>)subexp2.accept(this,tmp));
                } else {
                    thenBranch  = 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp,subexp2));
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
        public List<JStatement> visitConditionalOr(JConditionalOrExpression self, JLocalVariableExpression tmp) {
            JExpression subexp1 = self.getLeft();
            JExpression subexp2 = self.getRight();
            boolean convert1 = ! simpleExpression(subexp1) && shouldConvertExpression(subexp1);
            boolean convert2 = ! simpleExpression(subexp2) && shouldConvertExpression(subexp2);
            if (! convert1 && ! convert2) {
                // nothing to convert: tmp = self;
                return singletonStatementList((JStatement) 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp, self)));
            } else {
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
                    thenBranch = 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp,
                                        new JBooleanLiteral(null,true)));
                }
                if (convert2) {
                    // second subexpression also converts into tmp.
                    elseBranch = structureOptBlock((List<JStatement>)subexp2.accept(this,tmp));
                } else {
                    elseBranch  = 
                        new JExpressionStatement(
                                new JAssignmentExpression(tmp,subexp2));
                }
                newstmts.add(
                        new JIfStatement(self.getTokenReference(),cond,thenBranch,elseBranch,null));
                return newstmts;
            }
        }

        public List<JStatement> visitConstructorCall(JConstructorCall self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression ConstructorCall";
            return null;
        }

        public List<JStatement> visitCreatePortal(SIRCreatePortal self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        public List<JStatement> visitDivide(JDivideExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JDivideExpression(self.getTokenReference(),left,right);}});                    
        }

        public List<JStatement> visitDynamicToken(SIRDynamicToken self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        public List<JStatement> visitEquality(JEqualityExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JEqualityExpression(self.getTokenReference(),
                            ((JEqualityExpression)self).getEqual(),left,right);}});                    
        }

        public List<JStatement> visitFieldAccess(JFieldAccessExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getPrefix(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression prefix) 
                        {return new JFieldAccessExpression(self.getTokenReference(), prefix, 
                                self.getIdent());}});
        }

        public List<JStatement> visitInstanceof(JInstanceofExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Instanceof";
            return null;
        }

        public List<JStatement> visitInterfaceTable(SIRInterfaceTable self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression InterfaceTable";
            return null;
        }

        public List<JStatement> visitLiteral(JLiteral self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        public List<JStatement> visitLocalVariable(JLocalVariableExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        public List<JStatement> visitLogicalComplement(JLogicalComplementExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JLogicalComplementExpression(self.getTokenReference(), expr);}});
        }


        public List<JStatement> visitMethodCall(JMethodCallExpression self, JLocalVariableExpression tmp) {
            List<JExpression> exprs = Arrays.asList(self.getArgs());
            Pair<List<JStatement>,List<JExpression>> cvtdexprs = this.recurrExpressions(exprs);
            List<JStatement> newstmts = cvtdexprs.getFirst();
            List<JExpression> newexprs = cvtdexprs.getSecond();
            JMethodCallExpression newself = new JMethodCallExpression(self.getTokenReference(),
                    self.getPrefix(),self.getIdent(),newexprs.toArray(new JExpression[newexprs.size()]));
            newstmts.add(new JExpressionStatement(new JAssignmentExpression(tmp,newself)));
            return newstmts;
        }

        public List<JStatement> visitMinus(JMinusExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JMinusExpression(self.getTokenReference(),left,right);}});                    
        }

        public List<JStatement> visitModulo(JModuloExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JModuloExpression(self.getTokenReference(),left,right);}});                    
        }

        public List<JStatement> visitMult(JMultExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JMultExpression(self.getTokenReference(),left,right);}});                    
        }

        public List<JStatement> visitName(JNameExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        public List<JStatement> visitNewArray(JNewArrayExpression self, JLocalVariableExpression tmp) {
            // recur into initializer at this level to keep from using additional temps if the
            // initialization is simple.
            Pair<List<JStatement>,List<JExpression>> cvtddims = recurrExpressionsArr(self.getDims());
            List<JStatement> newstmts = cvtddims.getFirst();
            List<JExpression> newdims = cvtddims.getSecond();
            JArrayInitializer init = self.getInit();
            JExpression[] initexps = (init == null) ? new JExpression[0] : init.getElems();
            Pair<List<JStatement>,List<JExpression>> cvtdinits = recurrExpressionsArr(initexps);
            List<JStatement> initstmts = cvtdinits.getFirst();
            List<JExpression> initexprs = cvtdinits.getSecond();
            
            JNewArrayExpression newarray;
            if (initstmts.isEmpty()) {
                // can use old initializer
                newarray = new JNewArrayExpression(self.getTokenReference(), self.getType(),
                        newdims.toArray(new JExpression[newdims.size()]),init);
            } else {
                // need a new initializer (and can guarantee old one was not null).
                newstmts.addAll(initstmts);
                newarray = new JNewArrayExpression(self.getTokenReference(), self.getType(),
                        newdims.toArray(new JExpression[newdims.size()]),
                        new JArrayInitializer(init.getTokenReference(),
                                initexprs.toArray(new JExpression[initexprs.size()])));
            }
            newstmts.add(new JExpressionStatement(
                    new JAssignmentExpression(tmp,newarray)));
            return newstmts;
                    
        }

        public List<JStatement> visitParenthesed(JParenthesedExpression self, JLocalVariableExpression tmp) {
            return (List<JStatement>)self.getExpr().accept(this,tmp);
        }

        public List<JStatement> visitPeek(SIRPeekExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getArg(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) {return new SIRPeekExpression(v);}});
        }

        public List<JStatement> visitPop(SIRPopExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self);
        }

        public List<JStatement> visitPostfix(JPostfixExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) 
                    {return new JPostfixExpression(self.getTokenReference(),
                            ((JPostfixExpression)self).getOper(),v);}});
        }

        public List<JStatement> visitPrefix(JPrefixExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) 
                    {return new JPrefixExpression(self.getTokenReference(),
                            ((JPrefixExpression)self).getOper(),v);}});
       }

        public List<JStatement> visitPush(SIRPushExpression self, JLocalVariableExpression tmp) {
            assert false : "Push should be handled at statment level";
            return recurrOneExpr(tmp,self.getArg(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression v) 
                    {return new SIRPushExpression(v,
                            ((SIRPushExpression)self).getTapeType());}});
       }

        public List<JStatement> visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression QualifiedAnonymousCreation";
            return null;
        }

        public List<JStatement> visitQualifiedInstanceCreation(JQualifiedInstanceCreation self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression QualifiedInstanceCreation";
            return null;
        }

        public List<JStatement> visitRange(SIRRangeExpression self, JLocalVariableExpression tmp) {
            JExpression min = self.getMin();
            JExpression ave = self.getAve();
            JExpression max = self.getMax();
            boolean convertMin = ! simpleExpression(min) && shouldConvertExpression(min);
            boolean convertAve = ! simpleExpression(ave) && shouldConvertExpression(ave);
            boolean convertMax = ! simpleExpression(max) && shouldConvertExpression(max);
            if (! (convertMin || convertAve || convertMax)) {
                return recurrBase(tmp,self);
            }
            List<JStatement> newstmts =  new LinkedList<JStatement>();
            JExpression newmin = min;
            JExpression newave = ave;
            JExpression newmax = max;
            if (convertMin) {
                Pair<List<JStatement>,JExpression> cvtdmin = recurrTopExpression(min);
                newstmts.addAll(cvtdmin.getFirst());
                newmin = cvtdmin.getSecond();
            }
            if (convertAve) {
                Pair<List<JStatement>,JExpression> cvtdave = recurrTopExpression(ave);
                newstmts.addAll(cvtdave.getFirst());
                newave = cvtdave.getSecond();
            }
            if (convertMax) {
                Pair<List<JStatement>,JExpression> cvtdmax = recurrTopExpression(max);
                newstmts.addAll(cvtdmax.getFirst());
                newmax = cvtdmax.getSecond();
            }
            newstmts.add(
                    new JExpressionStatement(
                            new JAssignmentExpression(tmp,
                                    new SIRRangeExpression(newmin,newave,newmax))));
            return newstmts;
        }

        public List<JStatement> visitRelational(JRelationalExpression self, JLocalVariableExpression tmp) {
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JRelationalExpression(self.getTokenReference(),
                            ((JRelationalExpression)self).getOper(),left,right);}});                    
        }

        public List<JStatement> visitShift(JShiftExpression self, JLocalVariableExpression tmp) {
            final JShiftExpression finalself = self;
            return recurrTwoExprs(tmp,self.getLeft(),self.getRight(),self,
                    new C2(){public JExpression mk(JExpression self, JExpression left, JExpression right)
                    {return new JShiftExpression(self.getTokenReference(),
                            ((JShiftExpression)self).getOper(),left,right);}});                    
        }

        public List<JStatement> visitSuper(JSuperExpression self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression Super";
            return null;
        }

        public List<JStatement> visitThis(JThisExpression self, JLocalVariableExpression tmp) {
            // there can actually be an expression inside of this "p.this" but shoud not ocur in StreamIt.
            return recurrBase(tmp,self); 
        }

        public List<JStatement> visitTypeName(JTypeNameExpression self, JLocalVariableExpression tmp) {
            return recurrBase(tmp,self); 
        }

        public List<JStatement> visitUnary(JUnaryExpression self, JLocalVariableExpression tmp) {
            assert false : "Unary should be handled at its subtypes";
            return null;
        }

        public List<JStatement> visitUnaryMinus(JUnaryMinusExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JUnaryMinusExpression(self.getTokenReference(), expr);}});
        }

        public List<JStatement> visitUnaryPlus(JUnaryPlusExpression self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JUnaryPlusExpression(self.getTokenReference(), expr);}});
        }

        public List<JStatement> visitUnaryPromote(JUnaryPromote self, JLocalVariableExpression tmp) {
            return recurrOneExpr(tmp,self.getExpr(),self,
                    new C1(){public JExpression mk(JExpression self, JExpression expr) 
                        {return new JUnaryPromote(expr, self.getType());}});        }

        public List<JStatement> visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self, JLocalVariableExpression tmp) {
            assert false: "Unexpected expression UnqualifiedAnonymousCreation";        
            return null;
        }

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
         * If true, creates a temp, and returns statments declaring temp and converting into temp,
         * and returns an expression referencing the created temp.
         * @param an expression to convert.
         * @return a Pair of a list of statements to preceed the converted expression and the converted expression.
         */
        
        Pair<List<JStatement>,JExpression> recurrTopExpressionL (JExpression expr) {
            if (simpleExpression(expr) || ! shouldConvertExpression(expr)) {
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
            boolean convert1 = ! simpleExpression(prefix) && shouldConvertExpression(prefix);
            boolean convert2 = ! simpleExpression(accessor) && shouldConvertExpression(accessor);
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
         * Given a block or compound statment, recurr.
         * If passed a JCompoundStatement and recursion produces > 1 statement
         * then returns a JCompoundStatement.
         * Else if recursion produces > 1 statement returns a JBlock.
         * @param s : a single statement.
         * @return : a statment
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
                return singletonStatementList((JStatement) self);
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
            if (! simpleExpression(cond) && shouldConvertStatement(self)) {
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
            if (! simpleExpression(expr) && shouldConvertStatement(self) && shouldConvertExpression(expr)) {
                List<JStatement> newstmts = new LinkedList<JStatement>();
                if (expr instanceof SIRPushExpression) {
                    // Push should have been a statement.
                    SIRPushExpression pexpr = (SIRPushExpression)expr;
                    JExpression subexpr = pexpr.getArg();
                    if (shouldConvertExpression(subexpr)) {
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
                    // (only usable at statment level since it has void type!)
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
            if (! simpleExpression(cond) && shouldConvertStatement(self)) {
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
            if (! simpleExpression(expr) && shouldConvertStatement(self)) {
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
            boolean shouldConvert = false;
            for (JVariableDefinition var : vars) {
                JExpression initializer = var.getValue();
                if (initializer != null && ! simpleExpression(initializer) && shouldConvertExpression(initializer)) {
                    shouldConvert = true; 
                    break;
                }
            }
            
            if (shouldConvert) {
                List<JStatement> newstmts = new LinkedList<JStatement>();
                for (JVariableDefinition var : vars) {
                    JExpression initializer = var.getValue();
                    if (initializer != null && ! simpleExpression(initializer) && shouldConvertExpression(initializer)
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
         * odd circumstances (examples/cookbook/BPFProgram).
         * Try  S[[while (E) S]] = while (true) { if ! E then break; S } to
         * check Propagator behavior.
         */
        public List<JStatement> visitWhileStatement(JWhileStatement self, JExpression cond, JStatement body) {
            if (! simpleExpression(cond) && shouldConvertStatement(self)) {
                
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

        /**
         * S[[statement text]] = statement text
         */
        public List<JStatement> visitEmittedText(JEmittedText self) {
            return singletonStatementList((JStatement)self);
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

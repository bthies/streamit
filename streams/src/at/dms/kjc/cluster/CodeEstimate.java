
package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.HashMap;
import at.dms.kjc.common.RawUtil;


/**
 * Estimates the code size and size of local variables for a {@link SIRFilter}
 */

public class CodeEstimate extends SLIREmptyVisitor {

    private static HashMap<SIRFilter, Integer> saved_locals = new HashMap<SIRFilter, Integer>();
    private static HashMap<SIRFilter, Integer> saved_code = new HashMap<SIRFilter, Integer>();

    static int METHOD_CALL_EXPR = 16;
    static int METHOD_CALL_PER_PARAM = 4;

    static int FOR_EXPR = 6;

    static int PEEK_EXPR = 10; // always an array reference (was:14)
    static int POP_EXPR = 6; // takes small space if fused (was:20)
    static int PUSH_EXPR = 6; // takse small space if fused (was:57)
    static int PRINT_EXPR = 10;

    static int ARITH_INT = 4;
    static int ARITH_FLOAT = 5;

    static int UNARY_EXPR = 2;
    static int PREFIX_EXPR = 4;
    static int POSTFIX_EXPR = 4;

    static int COND_EXPR = 8;

    static int FIELD_EXPR = 4;
    static int ARRAY_ACCESS = 10;

    /**
     * Creates a new instance of CodeEstimate and passes
     * a filter to it. This will result in calculating 
     * the size of code and local variables. The size of
     * code and locals will be saved in a cache.
     * @param filter the filter
     * @return instance of CodeEstimate with code and locals estimates
     */

    public static CodeEstimate estimate(SIRFilter filter) {
        CodeEstimate est = new CodeEstimate(filter);
        est.visitFilter(filter);
        saved_code.put(filter, new Integer(est.code_size));
        saved_locals.put(filter, new Integer(est.locals_size));
        return est;
    }
 
    /**
     * Returns estimated code size. If this has already been
     * calculated look up the value in cache. Otherwise do calculation.
     * @param filter the filter
     * @return estimated size of code
     */
    
    public static int estimateCode(SIRFilter filter) {

        if (saved_code.containsKey(filter)) {
            return saved_code.get(filter).intValue(); 
        }

        return estimate(filter).getCodeSize();
    }

    /**
     * Returns estimated size of locals. If this has already been
     * calculated look up the value in cache. Otherwise do calculation.
     * @param filter the filter
     * @return estimated size of locals
     */

    public static int estimateLocals(SIRFilter filter) {

        if (saved_locals.containsKey(filter)) {
            return saved_locals.get(filter).intValue(); 
        }

        return estimate(filter).getLocalsSize();
    }

    private int code_size;        // size of code
    private int locals_size;      // size of local variables

    private SIRFilter filter;

    private HashMap<String,Boolean> methodsToVisit;

    private int for_loop_level;
    private int code_at_level[];

    private CodeEstimate(SIRFilter filter) {
        this.filter = filter;
        code_size = 0;
        locals_size = 0;
        code_at_level = new int[64];
    }

    /**
     * Returns size of locals
     * @return size of locals
     */

    public int getLocalsSize() {
    
        return locals_size;
    }

    /**
     * Returns size of code
     * @return size of code
     */

    public int getCodeSize() {
    
        /*
          System.out.println("code at levels: ("+code_at_level[0]+
          ","+code_at_level[1]+
          ","+code_at_level[2]+
          ","+code_at_level[3]+
          ","+code_at_level[4]+
          ","+code_at_level[5]+
          ")");
        */

        return code_size;
    }

    /**
     * visit a {@link SIRFilter}
     * @param self the filter
     */

    public void visitFilter(SIRFilter self) {

        int old_size = 0;

        for (int i = 0; i < 64; i++) code_at_level[i] = 0;
        for_loop_level = 0;

        JMethodDeclaration work = self.getWork();
        JMethodDeclaration init = self.getInit();
        JMethodDeclaration[] methods = self.getMethods();

        if (work == null) {
            System.out.println("WARNING! Filter ["+self+"] has no work function!");
            return;
        }

        methodsToVisit = new HashMap();
        methodsToVisit.put(work.getName(), new Boolean(false));

        while (methodsToVisit.size() != old_size) {
            old_size = methodsToVisit.size();
            for (int i = 0; i < methods.length; i++) {
                String currMethod = methods[i].getName();
                if (methodsToVisit.containsKey(currMethod)) {
                    Boolean done = methodsToVisit.get(currMethod);
                    if (!done.booleanValue()) {
                        methods[i].accept(this);
                        methodsToVisit.put(currMethod, new Boolean(true));
                    }
                }
            }
        }

        // for this operation, currently ignore dynamic rates
        // (consider peek-pop == 0).
        SIRDynamicRateManager.pushConstantPolicy(0);
        // get peek-pop
        int peekMinusPop = filter.getPeekInt() - filter.getPopInt();
        // restore old policy
        SIRDynamicRateManager.popPolicy();

        code_size += ARRAY_ACCESS * 2 * peekMinusPop;
    }





    /**
     * visits a variable definition, this increase size of locals
     */

    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {

        int size = 0;
    
        if (type.isArrayType()) {
        
            String dims[] = (new FlatIRToCluster()).makeArrayStrings(((CArrayType)self.getType()).getDims());
            CType base = ((CArrayType)type).getBaseType();
        
            if (dims != null && dims[0] != null) {
                size = DataEstimate.getTypeSize(base) * Integer.valueOf(dims[0]).intValue();
            }

        } else if (type instanceof CNullType) {
            size = 0;
        } else if (type instanceof CClassNameType && type.toString().equals("java.lang.String")) {
            size = 4; // kluge for now, to keep from crashing on strings.
        } else {

            size = DataEstimate.getTypeSize(type);
        }
    
        //System.out.print("local variable: "+ident+" size: "+size);

        //System.out.println("filter: "+filter+" variable: "+ident+" size: "+size);
        locals_size += size;
    
    }


    
    /**
     * visits a method call expression
     */

    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {

        int old_level = for_loop_level;
    
        for_loop_level = 0;
        super.visitMethodCallExpression(self, prefix, ident, args);
        for_loop_level = old_level;

        code_size += METHOD_CALL_EXPR + METHOD_CALL_PER_PARAM * args.length;
        code_at_level[for_loop_level] += METHOD_CALL_EXPR + METHOD_CALL_PER_PARAM * args.length; 
    
        if (!methodsToVisit.containsKey(ident)) {
            methodsToVisit.put(ident, new Boolean(false));
        }
    }

    /**
     * visits a for statement
     */

    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
    
        code_size += FOR_EXPR;
        code_at_level[for_loop_level] += FOR_EXPR;

        if (init != null) {
            init.accept(this);
        }
        if (cond != null) {
            cond.accept(this);
        }
        if (incr != null) {
            incr.accept(this);
        }

        for_loop_level++;
        body.accept(this);
        for_loop_level--;
    }

    
    /**
     * visits a peek expression.
     */

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression arg) {
        super.visitPeekExpression(self, tapeType, arg);
        code_size += PEEK_EXPR;
        code_at_level[for_loop_level] += PEEK_EXPR; 
    }

    /**
     * visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType) {
        //assert self.getNumPops() == 1: "Need support here for multiple pop"; only if have MULTIPOP_EXPR
        super.visitPopExpression(self, tapeType);
        code_size += POP_EXPR;
        code_at_level[for_loop_level] += POP_EXPR; 
    }

    /**
     * visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression arg) {
        super.visitPrintStatement(self, arg);
        code_size += PRINT_EXPR;
        code_at_level[for_loop_level] += PRINT_EXPR; 
    }

    /**
     * visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression arg) {
        super.visitPushExpression(self, tapeType, arg);
        code_size += PUSH_EXPR;
        code_at_level[for_loop_level] += PUSH_EXPR; 
    }

    /**
     * Adds to work estimate an amount for an arithmetic op of type
     * expr.  Assumes <expr> is integral unless the type is explicitly
     * float or double.
     */
    private void countArithOp(JExpression expr) {

        if (expr.getType()==CStdType.Float ||
            expr.getType()==CStdType.Double) {
    
            code_size += ARITH_FLOAT;
            code_at_level[for_loop_level] += ARITH_FLOAT; 

        } else {
        
            code_size += ARITH_INT;
            code_at_level[for_loop_level] += ARITH_INT;     
        }
    }


    /**
     * visits an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr) {
        super.visitUnaryPlusExpression(self, expr);
        code_size += UNARY_EXPR;
        code_at_level[for_loop_level] += UNARY_EXPR; 
    }

    /**
     * visits an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr) {
        super.visitUnaryMinusExpression(self, expr);
        code_size += UNARY_EXPR;
        code_at_level[for_loop_level] += UNARY_EXPR; 

    }

    /**
     * visits a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        super.visitBitwiseComplementExpression(self, expr);
        code_size += UNARY_EXPR;
        code_at_level[for_loop_level] += UNARY_EXPR; 

    }

    /**
     * visits a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        super.visitLogicalComplementExpression(self, expr);
        code_size += UNARY_EXPR;
        code_at_level[for_loop_level] += UNARY_EXPR; 

    }

    /**
     * visits a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
        super.visitPrefixExpression(self, oper, expr);
        code_size += PREFIX_EXPR;
        code_at_level[for_loop_level] += PREFIX_EXPR; 

    }

    /**
     * visits a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
        super.visitPostfixExpression(self, oper, expr);
        code_size += POSTFIX_EXPR;
        code_at_level[for_loop_level] += POSTFIX_EXPR; 

    }

    /**
     * visits a binary expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
        super.visitBinaryExpression(self, oper, left, right);
        countArithOp(self);
    }

    /**
     * visits a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
        super.visitConditionalExpression(self, cond, left, right);

        code_size += COND_EXPR;
        code_at_level[for_loop_level] += COND_EXPR; 

    }

    /**
     * visits a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident) {
        super.visitFieldExpression(self, left, ident);

        code_size += FIELD_EXPR;
        code_at_level[for_loop_level] += FIELD_EXPR; 
    }

    /**
     * visits an array access expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        super.visitArrayAccessExpression(self, prefix, accessor);
        // the work estimate gets worse (e.g. for beamformer 4x4) if
        // we include array expressions, oddly enough.

        code_size += ARRAY_ACCESS;
        code_at_level[for_loop_level] += ARRAY_ACCESS; 

    }

}

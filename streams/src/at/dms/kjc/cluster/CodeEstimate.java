
package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.lang.*;
import java.util.HashMap;

public class CodeEstimate extends SLIREmptyVisitor {

    static int METHOD_CALL_EXPR = 16;
    static int METHOD_CALL_PER_PARAM = 4;

    static int FOR_EXPR = 6;

    static int PEEK_EXPR = 14;
    static int POP_EXPR = 20;
    static int PUSH_EXPR = 57;
    static int PRINT_EXPR = 10;

    static int ARITH_INT = 4;
    static int ARITH_FLOAT = 5;

    static int UNARY_EXPR = 2;
    static int PREFIX_EXPR = 4;
    static int POSTFIX_EXPR = 4;

    static int COND_EXPR = 8;

    static int FIELD_EXPR = 4;
    static int ARRAY_ACCESS = 10;

    /// Also count Literals and Local Variables
    
    public static int estimate(SIRFilter filter) {
	int result;
	CodeEstimate est = new CodeEstimate(filter);
	est.visitFilter(filter);
	result = est.getCodeSize();
    	//System.out.println("Filter: "+filter.getName()+" code size: "+result);
	return result;
    }

    private int code_size;
    SIRFilter filter;

    private HashMap methodsToVisit;

    private int for_loop_level;
    private int code_at_level[];

    CodeEstimate(SIRFilter filter) {
	this.filter = filter;
	code_size = 0;
	code_at_level = new int[64];
    }

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

    public void visitFilter(SIRFilter self) {

	int old_size = 0;

	for (int i = 0; i < 64; i++) code_at_level[i] = 0;
	for_loop_level = 0;

	JMethodDeclaration work = self.getWork();
	JMethodDeclaration init = self.getInit();
	JMethodDeclaration[] methods = self.getMethods();

	methodsToVisit = new HashMap();
	methodsToVisit.put(work.getName(), new Boolean(false));

	while (methodsToVisit.size() != old_size) {
	    old_size = methodsToVisit.size();
	    for (int i = 0; i < methods.length; i++) {
		String currMethod = methods[i].getName();
		if (methodsToVisit.containsKey(currMethod)) {
		    Boolean done = (Boolean)methodsToVisit.get(currMethod);
		    if (!done.booleanValue()) {
			methods[i].accept(this);
			methodsToVisit.put(currMethod, new Boolean(true));
		    }
		}
	    }
	}
    }







    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {

	int size = 0;
	
	if (type.isArrayType()) {
	    
	    String dims[] = ArrayDim.findDim((SIRFilter)filter, ident);
	    CType base = ((CArrayType)type).getBaseType();
	    
	    if (dims != null && dims[0] != null) {
		size = DataEstimate.getTypeSize(base) * Integer.valueOf(dims[0]).intValue();
	    }

	} else {

	    size = DataEstimate.getTypeSize(type);
	}
	
	//System.out.println("filter: "+filter+" variable: "+ident+" size: "+size);
	code_size += size;
	
    }


    






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

    

    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	super.visitPeekExpression(self, tapeType, arg);
	code_size += PEEK_EXPR;
	code_at_level[for_loop_level] += PEEK_EXPR; 
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	super.visitPopExpression(self, tapeType);
	code_size += POP_EXPR;
	code_at_level[for_loop_level] += POP_EXPR; 
    }

    /**
     * Visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
				    JExpression arg) {
	super.visitPrintStatement(self, arg);
	code_size += PRINT_EXPR;
	code_at_level[for_loop_level] += PRINT_EXPR; 
    }

    /**
     * Visits a push expression.
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
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
					 JExpression expr) {
	super.visitUnaryPlusExpression(self, expr);
	code_size += UNARY_EXPR;
	code_at_level[for_loop_level] += UNARY_EXPR; 
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
					  JExpression expr) {
	super.visitUnaryMinusExpression(self, expr);
	code_size += UNARY_EXPR;
	code_at_level[for_loop_level] += UNARY_EXPR; 

    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitBitwiseComplementExpression(self, expr);
	code_size += UNARY_EXPR;
	code_at_level[for_loop_level] += UNARY_EXPR; 

    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitLogicalComplementExpression(self, expr);
	code_size += UNARY_EXPR;
	code_at_level[for_loop_level] += UNARY_EXPR; 

    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
				      int oper,
				      JExpression expr) {
	super.visitPrefixExpression(self, oper, expr);
	code_size += PREFIX_EXPR;
	code_at_level[for_loop_level] += PREFIX_EXPR; 

    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
				       int oper,
				       JExpression expr) {
	super.visitPostfixExpression(self, oper, expr);
	code_size += POSTFIX_EXPR;
	code_at_level[for_loop_level] += POSTFIX_EXPR; 

    }

    /**
     * prints a binary expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
				      String oper,
				      JExpression left,
				      JExpression right) {
	super.visitBinaryExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a conditional expression
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
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
				     JExpression left,
				     String ident) {
	super.visitFieldExpression(self, left, ident);

	code_size += FIELD_EXPR;
	code_at_level[for_loop_level] += FIELD_EXPR; 
    }

    /**
     * prints an array length expression
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


package at.dms.kjc.cluster;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.lang.*;
import java.util.HashMap;

class DataEstimate {

    public static int getTypeSize(CType type) {

	if (type.getTypeID() == CType.TID_INT) return 4;
	if (type.getTypeID() == CType.TID_FLOAT) return 4;
	if (type.getTypeID() == CType.TID_DOUBLE) return 8;
	if (type.getTypeID() == CType.TID_BOOLEAN) return 1;

	assert (1 == 0);
	return 0;
    }

    public static int filterDataEstimate(SIRFilter filter) {

    	JFieldDeclaration[] fields = filter.getFields();
	int data_size = 0;

	for (int i = 0; i < fields.length; i++) {

	    CType type = fields[i].getType();
	    String ident = fields[i].getVariable().getIdent();
	    int size = 0;

	    if (type.isArrayType()) {

		String dims[] = ArrayDim.findDim((SIRFilter)filter, ident);
		CType base = ((CArrayType)type).getBaseType();
		
		if (dims != null && dims[0] != null) {
		    size = getTypeSize(base) * Integer.valueOf(dims[0]).intValue();
		}
	    } else {

		size = getTypeSize(type);
	    }

	    //System.out.println("filter: "+filter+" field: "+ident+" size: "+size);
	    data_size += size;
	}    

	return data_size;
    }
}


class CodeEstimate extends SLIREmptyVisitor {

    public static int estimate(SIRFilter filter) {
	CodeEstimate est = new CodeEstimate(filter);
	est.visitFilter(filter);
	return est.getCodeSize();
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
	
	System.out.println("code at levels: ("+code_at_level[0]+
			   ","+code_at_level[1]+
			   ","+code_at_level[2]+
			   ","+code_at_level[3]+
			   ","+code_at_level[4]+
			   ","+code_at_level[5]+
			   ")");

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


    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {

	int old_level = for_loop_level;
	
	for_loop_level = 0;
	super.visitMethodCallExpression(self, prefix, ident, args);
	for_loop_level = old_level;

	code_size += 16 + args.length * 4;
	code_at_level[for_loop_level] += 16 + args.length * 4; 
    
	if (!methodsToVisit.containsKey(ident)) {
	    methodsToVisit.put(ident, new Boolean(false));
	}
    }

    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
	
	code_size += 6;
	code_at_level[for_loop_level] += 6;

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

    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	super.visitPeekExpression(self, tapeType, arg);
	code_size += 14;
	code_at_level[for_loop_level] += 14; 
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	super.visitPopExpression(self, tapeType);
	code_size += 20;
	code_at_level[for_loop_level] += 20; 
    }

    /**
     * Visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
				    JExpression arg) {
	super.visitPrintStatement(self, arg);
	code_size += 10;
	code_at_level[for_loop_level] += 10; 
    }

    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	super.visitPushExpression(self, tapeType, arg);
	code_size += 57;
	code_at_level[for_loop_level] += 57; 
    }

    /**
     * Adds to work estimate an amount for an arithmetic op of type
     * expr.  Assumes <expr> is integral unless the type is explicitly
     * float or double.
     */
    private void countArithOp(JExpression expr) {

	if (expr.getType()==CStdType.Float ||
	    expr.getType()==CStdType.Double) {
	
	    code_size += 5;
	    code_at_level[for_loop_level] += 5; 

	} else {
	    
	    code_size += 4;
	    code_at_level[for_loop_level] += 4; 	
	}
    }


    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
					 JExpression expr) {
	super.visitUnaryPlusExpression(self, expr);
	code_size += 2;
	code_at_level[for_loop_level] += 2; 
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
					  JExpression expr) {
	super.visitUnaryMinusExpression(self, expr);
	code_size += 2;
	code_at_level[for_loop_level] += 2; 

    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitBitwiseComplementExpression(self, expr);
	countArithOp(expr);

    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitLogicalComplementExpression(self, expr);
	countArithOp(expr);

    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
				     int oper,
				     JExpression left,
				     JExpression right) {
	super.visitShiftExpression(self, oper, left, right);
	countArithOp(self);

    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
					  int oper,
					  JExpression left,
					  JExpression right) {
	super.visitRelationalExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
				      int oper,
				      JExpression expr) {
	super.visitPrefixExpression(self, oper, expr);
	code_size += 4;
	code_at_level[for_loop_level] += 4; 

    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
				       int oper,
				       JExpression expr) {
	super.visitPostfixExpression(self, oper, expr);
	code_size += 4;
	code_at_level[for_loop_level] += 4; 

    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
				    JExpression prefix,
				    String ident) {
	super.visitNameExpression(self, prefix, ident);
        //code_size += 3;
	//code_at_level[for_loop_level] += 3; 

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
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
					boolean equal,
					JExpression left,
					JExpression right) {
	super.visitEqualityExpression(self, equal, left, right);
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

	code_size += 8;
	code_at_level[for_loop_level] += 8; 

    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						  int oper,
						  JExpression left,
						  JExpression right) {
	super.visitCompoundAssignmentExpression(self, oper, left, right);

	// no work count for assignments, as most arith ops, memory
	// ops, etc., have a destination that takes care of the assign

	//code_size += 2;
	//code_at_level[for_loop_level] += 2; 

    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
				     JExpression left,
				     String ident) {
	super.visitFieldExpression(self, left, ident);

	code_size += 4;
	code_at_level[for_loop_level] += 4; 
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
				       int oper,
				       JExpression left,
				       JExpression right) {
	super.visitBitwiseExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
					  JExpression left,
					  JExpression right) {
	super.visitAssignmentExpression(self, left, right);

	code_size += 4;
	code_at_level[for_loop_level] += 4; 
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

	code_size += 10;
	code_at_level[for_loop_level] += 10; 

    }

}




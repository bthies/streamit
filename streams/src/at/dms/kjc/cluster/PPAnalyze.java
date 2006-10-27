
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.compiler.*;
import java.lang.*;
import java.util.*;

public class PPAnalyze extends SLIREmptyVisitor {

    private class PPInfo {
	public boolean nul; // undeterminate balance!
	public int bal;
	public int min;
    
	public PPInfo() {
	    nul = false;
	    bal = 0;
	    min = 0;
	}

	public void inc(int i) {
	    if (!nul) {
		if (bal + i < min) min = bal + i;
		bal += i;
	    }
	}

	public void add(int i, int m) {
	    if (!nul) {
		if (bal + m < min) min = bal + m; 
		bal += i;
	    }
	}

	public void setNul() {
	    nul = true;
	}

	public String toString() {
	    if (nul) {
		return "NULL";
	    } else {
		return "["+bal+","+min+"]";
	    }
	}
    }

    Stack<PPInfo> info_stack; // stack stores Push(-1)/Pop(+1) balance at each recursive level

    public PPAnalyze() {
	info_stack = new Stack<PPInfo>();
	newLevel();
    }

    private void newLevel() {
	info_stack.push(new PPInfo());
    }
    
    private PPInfo peekBalance() {
	return info_stack.peek();
    }

    private PPInfo popBalance() {
	return info_stack.pop();
    }

    private void incBalance(int offs) {
	PPInfo info = info_stack.peek();
	info.inc(offs);
    }

    private void addBalance(int offs, int min) {
	PPInfo info = info_stack.peek();
	info.add(offs, min);
    }

    private void nullBalance() {
	PPInfo info = info_stack.peek();
	info.setNul();
    }

    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {

	super.visitMethodDeclaration(self, modifiers, returnType, ident,
                                     parameters, exceptions, body);

    if (ClusterBackend.debugging)
        System.out.println("=======> PP method:"+ident+" balance:"+peekBalance());

    }

    public void visitPopExpression(SIRPopExpression self, CType tapeType) {

	incBalance(1);
	//System.out.println("------- pop newBalance="+peekBalance());

	super.visitPopExpression(self, tapeType);
    }

    public void visitPushExpression(SIRPushExpression self, CType tapeType, JExpression arg) {

	super.visitPushExpression(self, tapeType, arg);

	incBalance(-1);
	//System.out.println("------- push newBalance="+peekBalance());
    }
 

    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {

        cond.accept(this);

	newLevel();
        thenClause.accept(this);

	newLevel();
        if (elseClause != null) {
            elseClause.accept(this);
        }

	PPInfo i1 = popBalance();
	PPInfo i2 = popBalance();

	if (i1.nul || i2.nul) {
	    nullBalance();
	} else {
	    int min_bal = 0, min_min = 0;
	    if (i1.bal < i2.bal) min_bal = i1.bal; else min_bal = i2.bal;
	    if (i1.min < i2.min) min_min = i1.min; else min_min = i2.min;
	    addBalance(min_bal, min_min);
	}	    

	//System.out.println("------- If stmt balance1="+i1+" balance2="+i2+" newBalance="+peekBalance());
	
    }

    

    public void/*Object*/ visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {


	// WARNING: A problem with the current code is that 
	// it does not check to make sure that the loop induction 
	// variable is not being modified inside of the loop body !

	newLevel();

	/*return*/ super.visitForStatement(self, init, cond, incr, body);

	PPInfo info = popBalance();
	
	//System.out.println("PPAnalyze::visitForStmt <======= balance="+balance);

	//System.out.println(init.getClass().getName());
	//System.out.println(cond.getClass().getName());
	//System.out.println(incr.getClass().getName());

	JExpression a_left = null, a_right = null;
	JExpression r_left = null, r_right = null;
	JExpression i_expr = null;

	int post_op = 0;
	int rel_op = 0;

	if (init instanceof JExpressionListStatement) {
	    JExpressionListStatement list = (JExpressionListStatement)init;
	    //System.out.println("visitFor:init = JListStmt "+list.getExpressions().length);
	    JExpression expr = list.getExpression(0);
	    if (expr instanceof JAssignmentExpression) {
		JAssignmentExpression ae = (JAssignmentExpression)expr;
		a_left = ae.getLeft();
		a_right = ae.getRight();
		//System.out.println(a_left.getClass().getName());
		//System.out.println(a_right.getClass().getName());
	    }
	}

	if (cond instanceof JRelationalExpression) {
	    JRelationalExpression rel = (JRelationalExpression)cond;
	    //System.out.println("visitFor:cond = JRelExpr");
	    r_left = rel.getLeft();
	    r_right = rel.getRight();
	    rel_op = rel.getOper();
	    //System.out.println(r_left.getClass().getName());
	    //System.out.println(r_right.getClass().getName());
	}

	if (incr instanceof JExpressionListStatement) {
	    JExpressionListStatement list = (JExpressionListStatement)incr;
	    //System.out.println("visitFor:incr = JListStmt "+list.getExpressions().length);
	    JExpression expr = list.getExpression(0);
	    if (expr instanceof JPostfixExpression) {
		JPostfixExpression post = (JPostfixExpression)expr; 
		i_expr = post.getExpr();
		post_op = post.getOper();
		//System.out.println(i_expr.getClass().getName());
	    }
	}

	JLocalVariable a_var = null, r_var = null, i_var = null;

	int from = 0, to = 0, range = -1;

	if (a_left != null && a_right != null &&
	    r_left != null && r_right != null &&
	    i_expr != null &&
	    a_left instanceof JLocalVariableExpression &&
	    r_left instanceof JLocalVariableExpression &&
	    i_expr instanceof JLocalVariableExpression &&
	    a_right instanceof JIntLiteral &&
	    r_right instanceof JIntLiteral) {

	    a_var = ((JLocalVariableExpression)a_left).getVariable();
	    r_var = ((JLocalVariableExpression)r_left).getVariable();
	    i_var = ((JLocalVariableExpression)i_expr).getVariable();
	    from = ((JIntLiteral)a_right).intValue();
	    to = ((JIntLiteral)r_right).intValue();

	    if (post_op == Constants.OPE_POSTINC) {
		if (rel_op == Constants.OPE_LT) range = to-from;
		if (rel_op == Constants.OPE_LE) range = to-from+1;
	    }

	    if (post_op == Constants.OPE_POSTDEC) {
		if (rel_op == Constants.OPE_GT) range = from-to;
		if (rel_op == Constants.OPE_GE) range = from-to+1;
	    }

	    if (a_var == r_var && r_var == i_var) {
		//System.out.println("All variables are equal to: "+a_var.getIdent()+" Range is from="+from+" to="+to);
	    }
	} else {

	    //System.out.println("PPAnalysis: WARNING! Could not analyze a for statement!");
	}

	if (range == -1) {

	    //System.out.println("PPAnalysis: WARNING! Could not extract range!");

	    if (info.nul) {
		nullBalance();
	    } else {
		if (info.bal < 0) {
		    // if balance < 0 then can reduce balance arbitrarily
		    nullBalance();
		} else {
		    // if min < 0 then worst case is one execution
		    // if min == 0 then no effect on overall balance!
		    if (info.min < 0) {			
			addBalance(info.bal, info.min);
		    } 
		}
	    }

	} else {
	
	    //System.out.println("=========> Range is: "+range+" <=========");

	    if (info.nul) {
		nullBalance();
	    } else {
		for (int w = 0; w < range; w++) {
		    addBalance(info.bal, info.min);
		}
	    }	    	    
	}

	//System.out.println("------- For stmt balance="+info+" range="+range+" newBalance="+peekBalance());
	
    }

    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {

	newLevel();
	super.visitDoStatement(self, cond, body);
	PPInfo info = popBalance();

	if (info.nul) {
	    nullBalance();
	} else {
	    if (info.bal < 0) {
		// if balance < 0 then can reduce balance arbitrarily
		nullBalance();
	    } else {
		// if min < 0 then worst case is one execution
		// if min == 0 then worst case is no executions
		if (info.min < 0) {			
		    addBalance(info.bal, info.min);
		} 
	    }
	}

	//System.out.println("------- Do stmt balance="+info+" newBalance="+peekBalance());
    }

    public void visitWhileStatement(JWhileStatement self,
                                 JExpression cond,
                                 JStatement body) {

	newLevel();
	super.visitWhileStatement(self, cond, body);
	PPInfo info = popBalance();

	if (info.nul) {
	    nullBalance();
	} else {
	    if (info.bal < 0) {
            // if balance < 0 then can reduce balance arbitrarily
            nullBalance();
	    } else {
            // if min < 0 then worst case is one execution
            // if min == 0 then worst case is no executions
            if (info.min < 0) {			
                addBalance(info.bal, info.min);
            } 
	    }
	}

	//System.out.println("------- While stmt balance="+info+" newBalance="+peekBalance());
    }
    
    

}

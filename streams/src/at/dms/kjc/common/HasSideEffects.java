package at.dms.kjc.common;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.Vector;
import at.dms.util.Utils;


/**
 * This class determines, given an expression, if the expression
 * has any side effects.
 *
 *
 * @author Michael Gordon
 */

public class HasSideEffect extends SLIREmptyVisitor 
{
    private boolean sideEffect;

    
    /**
     * Return true if an expression has side effects, 
     * a method call, a prefix/postfix expression, an assignment expression
     *
     *
     * @param entry The expression we are interested in
     *
     *
     * @return true if the expression has side effects, false otherwise.
     * 
     */
    public static boolean hasSideEffects(JExpression entry) 
    {
	HasSideEffect hse = new HasSideEffect();
	entry.accept(hse);
	return hse.sideEffect;
    }
    

    private HasSideEffect() 
    {
	sideEffect = false;
    }


    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
				      int oper,
				      JExpression expr) {
	sideEffect = true;
	expr.accept(this);
    }
    
    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
					  JExpression prefix,
					  String ident,
					  JExpression[] args) {
	sideEffect = true;
	if (prefix != null) {
	    prefix.accept(this);
	}
	visitArgs(args);
    }
    
    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
					  JExpression left,
					  JExpression right) {
	sideEffect = true;
	left.accept(this);
	right.accept(this);
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						  int oper,
						  JExpression left,
						  JExpression right) {
	sideEffect = true;
	left.accept(this);
	right.accept(this);
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
				       int oper,
				       JExpression expr) {
	sideEffect = true;
	expr.accept(this);
    }

    public void visitCreatePortalExpression(SIRCreatePortal self) {
	sideEffect = true;
    }

    /**
     * Visits a peek expression.
     */
    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	arg.accept(this);
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	sideEffect = true;
    }

    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	sideEffect = true;
	arg.accept(this);
    }
  
    /**
     * Visits InlineAssembly
     */
    public void visitInlineAssembly(InlineAssembly self,String[] asm,
				    String[] input,String[] clobber) {
	sideEffect = true;
	
    }

    
}

package at.dms.kjc.common;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;

/**
 * Find all the vars that are possible assigned given a tree in the
 * IR.
 * 
 * @author Michael Gordon
 * 
 */

public class VarsAssigned extends SLIREmptyVisitor
{
    private HashSet vars;

    /**
     * Given <entry>, the lhs of an assignment expression, or
     * the exp of a prefix or posfix increment/decrement exp
     * return the var(s) assigned in this expression
     *
     * @param entry See above
     *
     *
     * @return A Hashset containing JLocalVariables for assigned locals 
     * or Strings for assigned fields
     *
     */
    public static HashSet getVarsAssigned(JPhylum entry)
    {
	VarsAssigned assigned = new VarsAssigned();
	
	entry.accept(assigned);
	
	return assigned.vars;
    }
    
    private VarsAssigned() 
    {
	vars = new HashSet();
    }
    
    private HashSet lValues(JExpression exp) 
    {
	HashSet vars = new HashSet();
	
	exp = Utils.passThruParens(exp);
	
	//array access, only worry about the prefix...
	if (exp instanceof JArrayAccessExpression) {
	    vars = VariablesDefUse.getVars(((JArrayAccessExpression)exp).getPrefix());
	}
	else if (exp instanceof JLocalVariableExpression) {
	    //simple local variable access, return the local var...
	    vars.add(((JLocalVariableExpression)exp).getVariable());
	}
	else if (exp instanceof JFieldAccessExpression) {
	    //simple field access, record the string...
	    vars.add(((JFieldAccessExpression)exp).getIdent());
	}
	else {    
	    //don't know what it is, so assume every var accessed 
	    //is assigned...
	    vars = VariablesDefUse.getVars(exp);
	}
	
	return vars;
    }
    

    public void visitAssignmentExpression(JAssignmentExpression self,
					  JExpression left,
					  JExpression right) {
	vars.addAll(lValues(left));
	left.accept(this);
	right.accept(this);
    }

    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						  int oper,
						  JExpression left,
						  JExpression right) {
	vars.addAll(lValues(left));
	vars.addAll(lValues(right));
	left.accept(this);
	right.accept(this);
    }
	
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
	vars.addAll(lValues(expr));
    }

    public void visitPostfixExpression(JPostfixExpression self,
				       int oper,
				       JExpression expr) {
	vars.addAll(lValues(expr));
    }
}

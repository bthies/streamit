package at.dms.kjc.common;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.Vector;
import at.dms.util.Utils;

/**
 * Class to remove unused variables from the IR. Unused variables are defined
 * as vars that are never used, only def'ed and only if the defs do not have
 * any side effects.
 *
 * @author Michael Gordon
 */

public class RemoveUnusedVars extends SLIRReplacingVisitor implements FlatVisitor
{
    /** holds alls vars referenced in the filter
	see VariablesUsed **/
    private HashSet varsUsed;
    /** Holds idents of arrays that are fields have have zero dimensionality **/
    private HashSet zeroDimArrays;

    
    /**
     * Remove dead variables from all code in <node>.  See class definition.
     *
     *
     *
     * @param node The top level flatnode.
     *
     */
    public static void doit(FlatNode node) 
    {
	System.out.println("Removing Dead Variables...");
	new RemoveUnusedVars(node);
    }

    private RemoveUnusedVars(FlatNode node) 
    {
	varsUsed = null;
	zeroDimArrays = new HashSet();
	node.accept(this, null, true);
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.isFilter()) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    
	    varsUsed = VariablesUsed.getVars(node, true);


	    for (int i = 0; i < filter.getMethods().length; i++) {
		filter.getMethods()[i].accept(this);
	    }
	    
	    //now check the fields
	    Vector newFields = new Vector();
	    for (int i = 0; i < filter.getFields().length; i++) {
		if (varsUsed.contains(filter.getFields()[i].getVariable().getIdent())) 
		    newFields.add(filter.getFields()[i]);
	    }
	    filter.setFields((JFieldDeclaration[])newFields.toArray(new JFieldDeclaration[0]));
	}
    }
    
    public Object visitAssignmentExpression(JAssignmentExpression self,
					    JExpression left,
					    JExpression right) {
	//remove an assignment expression if it 
	if (!varsUsed.contains(getVariable(left)) &&
	    !HasSideEffects.hasSideEffects(right) &&
	    !HasSideEffects.hasSideEffects(left))
	    return null;
	    
	return doBinaryExpression(self, left, right);
    }

    public Object getVariable(Object access) 
    {
	if (access instanceof JFieldAccessExpression) {
	    JFieldAccessExpression facc = (JFieldAccessExpression)access;
	    if (facc.getPrefix() instanceof JThisExpression)
		return facc.getIdent();
	    else {
		return getVariable(facc.getPrefix());
	    }
	}
	else if (access instanceof JLocalVariableExpression) {
	    return ((JLocalVariableExpression)access).getVariable();
	}
	else if (access instanceof JArrayAccessExpression) {
	    return getVariable(((JArrayAccessExpression)access).getPrefix());
	}
	
	assert false;
	return null;
    }
    
    
    /**
     * prints an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
					   JExpression expr) {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp == null) {
	    return new JEmptyStatement(null, null);
	}
	if (newExp!=expr) {
	    self.setExpression(newExp);
	}
	return self;
    }

    /**
     * prints a variable declaration statement
     */
    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
						    JVariableDefinition[] vars) {
	Vector newDecls = new Vector();
	for (int i = 0; i < vars.length; i++) {
	    JVariableDefinition result = 
		(JVariableDefinition)vars[i].accept(this);
	    if (result != null) 
		newDecls.add(result);
	}
	
	if (newDecls.size() == 0)
	    return new JEmptyStatement(null, null);

	return new JVariableDeclarationStatement(null, 
						 (JVariableDefinition[])
						 newDecls.toArray(new JVariableDefinition[0]),
						 null);
    }

    /**
     * prints a variable declaration statement
     */
    public Object visitVariableDefinition(JVariableDefinition self,
					  int modifiers,
					  CType type,
					  String ident,
					  JExpression expr) {
	if (varsUsed.contains(self))
	    return self;
	else return null;
    }
    
    /**
     * this is a private method for visiting binary expressions
     */
    protected Object doBinaryExpression(JBinaryExpression self, 
				    JExpression left, 
				    JExpression right) {
	JExpression newExp = (JExpression)left.accept(this);
	if (newExp!=null && newExp!=left) {
	    self.setLeft(newExp);
	}
	
	newExp = (JExpression)right.accept(this);
	if (newExp!=null && newExp!=right) {
	    self.setRight(newExp);
	}

	return self;
    }
}



package at.dms.kjc.common;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.Vector;
import at.dms.util.Utils;

public class RemoveUnusedVars extends SLIRReplacingVisitor implements FlatVisitor
{
    /** holds alls vars referenced in the filter
	see VariablesUsed **/
    private HashSet varsUsed;
    /** Holds idents of arrays that are fields have have zero dimensionality **/
    private HashSet zeroDimArrays;

    public static void doit(FlatNode node) 
    {
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
	    
	    //get all the vars used in this filter
	    varsUsed = VariablesUsed.getVars(node);
	    //check the local variable defs and also get
	    //all the names of zero dimensional arrays
	    for (int i = 0; i < filter.getMethods().length; i++) {
		filter.getMethods()[i].accept(this);
	    }
	    //remove zero dimensional arrays
	    varsUsed.removeAll(zeroDimArrays);
	    //now check the fields
	    Vector newFields = new Vector();
	    for (int i = 0; i < filter.getFields().length; i++) {
		if (varsUsed.contains(filter.getFields()[i].getVariable().getIdent())) 
		    newFields.add(filter.getFields()[i]);
	    }
	    filter.setFields((JFieldDeclaration[])newFields.toArray(new JFieldDeclaration[0]));
	}
    }
    
    /**
     * Find all arrays that are initialized with all zero bounds and 
     * remember them so they can be removed...
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
					    JExpression left,
					    JExpression right) {
	if (Utils.passThruParens(right) instanceof JNewArrayExpression) {
	    JNewArrayExpression newArray = (JNewArrayExpression)Utils.passThruParens(right);
	    for (int i = 0; i < newArray.getDims().length; i++) {
		if (!((newArray.getDims()[i] instanceof JIntLiteral &&
		      ((JIntLiteral)newArray.getDims()[i]).intValue() == 0))) {
		    //non int 0 so we cannot remove this array, call normal visitor
		    return doBinaryExpression(self, left, right);
		}		    
		//if we get here all the dims are zero...
		if (Utils.passThruParens(left) instanceof JFieldAccessExpression)
		    zeroDimArrays.add(((JFieldAccessExpression)Utils.passThruParens(left)).getIdent());
		return null;
	    }    
	    
	} 
	return doBinaryExpression(self, left, right);
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

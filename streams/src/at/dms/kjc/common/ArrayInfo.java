package at.dms.kjc.common;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;

public class ArrayInfo extends SLIREmptyVisitor implements FlatVisitor 
{
    private static boolean initialized = false;
    private static HashMap newArrs;
    private static HashMap arrayAssignments;
    
    

    public static void initialize(FlatNode top) 
    {
	newArrs = new HashMap();
	arrayAssignments = new HashMap();
	top.accept(new ArrayInfo(), null, true);
	initialized = true;
    }
    
    public void visitNode (FlatNode node) 
    {
	if (node.isFilter()) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    
	    for (int i = 0; i < filter.getMethods().length; i++) {
		filter.getMethods()[i].accept(this);
	    }

	    for (int i = 0; i < filter.getFields().length; i++) {
		filter.getFields()[i].accept(this);
	    }
	}
    }
    

    /**
     * get the new array expression for a local variable
     **/
    public static JNewArrayExpression getNewArr(JVariableDefinition var) 
    {
	assert initialized;
	return (JNewArrayExpression)newArrs.get(var);
    }
    
    /** 
     *get the new array expression for a field 
     **/
    public static JNewArrayExpression getNewArr(String var) 
    {
	assert initialized;
	return (JNewArrayExpression)newArrs.get(var);
    }

    public static JExpression getArrAss(JVariableDefinition var) 
    {
	assert initialized;
	return (JExpression)arrayAssignments.get(var);
    }
    
    public static JExpression getArrAss(String var) 
    {
	assert initialized;
	return (JExpression)arrayAssignments.get(var);
    }
    
    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
				      int modifiers,
				      CType type,
				      String ident,
				      JExpression expr) {

	if (Utils.passThruParens(expr) instanceof JNewArrayExpression) {
	    newArrs.put(ident, Utils.passThruParens(expr));
	    return;   
	}
	    
	if (expr != null) {
	    expr.accept(this);
	}
	// also descend into the vardef
	self.getVariable().accept(this);
    }
    
    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
					int modifiers,
					CType type,
					String ident,
					JExpression expr) {
	if (Utils.passThruParens(expr) instanceof JNewArrayExpression) {
	    newArrs.put(self, Utils.passThruParens(expr));
	    return;   
	}
	
	if (expr != null) {
	    expr.accept(this);
	}
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
					  JExpression left,
					  JExpression right) {
	
	//	assert left.getType() != null || right.getType() != null;
	
	if (Utils.passThruParens(right) instanceof JNewArrayExpression) {
	    JExpression newleft = Utils.passThruParens(left);
	    if (newleft instanceof JFieldAccessExpression) {
		newArrs.put(((JFieldAccessExpression)newleft).getIdent(),
			    Utils.passThruParens(right));
		
	    } else if (newleft instanceof JLocalVariableExpression) {
		newArrs.put(((JLocalVariableExpression)newleft).getVariable(),
			    Utils.passThruParens(right));
	    }
	    
	}
	
	if (Utils.passThruParens(left) instanceof JFieldAccessExpression) {
	    arrayAssignments.put(((JFieldAccessExpression)Utils.passThruParens(left)).getIdent(),
				 Utils.passThruParens(right));
	}
	if (Utils.passThruParens(left) instanceof JLocalVariableExpression) {
	    arrayAssignments.put(((JLocalVariableExpression)Utils.passThruParens(left)).getVariable(),
				 Utils.passThruParens(right));
	}

	left.accept(this);
	right.accept(this);
    }
}


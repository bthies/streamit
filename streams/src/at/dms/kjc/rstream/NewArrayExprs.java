package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import at.dms.util.*;



/**
 * This class finds the (or creates a) corresponding new array expression 
 * for each array declared.  The new array expressions are accessed using
 * the public methods below.  A given array may not have a new array expression
 * and the accessor method will return null.  Then one must use getArrayAss() to
 * see what array it was assigned, then one can use the new array expression from
 * the array it was assigned.  This may have to be done multiple times.  
 * @author Michael Gordon
 * 
 */
//also store any whole array assignments
//remove any jnewarrayexpression not in a declaration
public class NewArrayExprs extends SLIRReplacingVisitor 
{
    //hash map holding left -> right in left = right, where right and
    //left are arrays
    private HashMap arrayAssignments;
    //hash map from var -> JNewArrayExpression where var is a string for
    //fields and a JVariableDefinition for locals
    private HashMap newArray;

    /**
     * Create an object and perform the anaylsis.  Now one can access
     * The new array expression from the access methods.
     * Note, this not visit the stream graph, just examine <node>
     *
     * @param node The flat node we want to examine. 
     */

    public NewArrayExprs(FlatNode node) 
    {
	if (node.isFilter()) {
	    initialize((SIRFilter)node.contents);
	}
    }
    
    public NewArrayExprs(SIRFilter filter) 
    {	
	initialize(filter);
    }

    private void initialize(SIRFilter filter)
    {	    
	arrayAssignments = new HashMap();
	newArray = new HashMap();

	for (int i = 0; i < filter.getFields().length; i++)
	    filter.getFields()[i].accept(this);
	
	for (int i = 0; i < filter.getMethods().length; i++)
	    filter.getMethods()[i].accept(this);
    }
    
    
    /**
     * Given a variable return the corresponding new array expression
     * that was found.  If none was found, return null.
     *
     *
     * @param Var either a string for a field or a JLocalVariable for a
     * local.
     *
     * @return The corresponding JNewArrayExpression or null if one
     * does not exist.
     */
    public  JNewArrayExpression getNewArr(Object var) 
    {
	assert (var instanceof String || var instanceof JLocalVariable) :
	    "Calling getNewArr with " + var;
	return (JNewArrayExpression)newArray.get(var);
    }
    
    /**
     * Given an array variable (eithere a string for fields are a JLocalVariable),
     * return an array that was assigned to it.    
     *
     * @param Var either a string for a field or a JLocalVariable for a
     * local.
     * 
     * @return An array variable that the given variable was assigned.
     */
    public Object getArrAss(Object var) 
    {
	assert (var instanceof String || var instanceof JLocalVariable) :
	    "Calling getArrAss with " + var;
	return arrayAssignments.get(var);
    }

    /**
     * prints a field declaration
     */
    public Object visitFieldDeclaration(JFieldDeclaration self,
					int modifiers,
					CType type,
					String ident,
					JExpression expr) {
	if (Utils.passThruParens(expr) instanceof JNewArrayExpression) {
	    newArray.put(ident, Utils.passThruParens(expr));
	}
	return self; 
    }

    /**
     * 
     */
    public Object visitVariableDefinition(JVariableDefinition self,
					  int modifiers,
					  CType type,
					  String ident,
					  JExpression expr) {
	if (Utils.passThruParens(expr) instanceof JNewArrayExpression) {
	    newArray.put(self, Utils.passThruParens(expr));
	}
	
	return self;
    }
    
    /**
     * 
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
					    JExpression left,
					    JExpression right) {
		
	if (Utils.passThruParens(right) instanceof JNewArrayExpression) {
	    JExpression newleft = Utils.passThruParens(left);
	    if (newleft instanceof JFieldAccessExpression) {
		newArray.put(((JFieldAccessExpression)newleft).getIdent(),
			    Utils.passThruParens(right));
		//remove all jnewarrayexpressions not in a declaration
		return null;
		
	    } else if (newleft instanceof JLocalVariableExpression) {
		newArray.put(((JLocalVariableExpression)newleft).getVariable(),
			    Utils.passThruParens(right));
		//remove all jnewarrayexpressions not in a declaration
		return null;
	    }
	    
	}
	
	//just put all assignments where the top level is a variable access
	//this will *not* add assignments of the form z[i] = b[i] because
	//they have the variable access wrapped in a JArrayAccessExpression
	if (Utils.passThruParens(left) instanceof JFieldAccessExpression &&
	    Utils.passThruParens(right) instanceof JFieldAccessExpression)
	    arrayAssignments.put(((JFieldAccessExpression)Utils.passThruParens(left)).getIdent(),
				 ((JFieldAccessExpression)Utils.passThruParens(right)).getIdent());
	
	if (Utils.passThruParens(left) instanceof JFieldAccessExpression &&
	    Utils.passThruParens(right) instanceof JLocalVariableExpression) {
	    arrayAssignments.put(((JFieldAccessExpression)Utils.passThruParens(left)).getIdent(),
				 ((JLocalVariableExpression)Utils.passThruParens(right)).getVariable());
	    
	}
	if (Utils.passThruParens(left) instanceof JLocalVariableExpression &&
	    Utils.passThruParens(right) instanceof JFieldAccessExpression) {
	    arrayAssignments.put(((JLocalVariableExpression)Utils.passThruParens(left)).getVariable(),
				 ((JFieldAccessExpression)Utils.passThruParens(right)).getIdent());
	    
	}
	
	if (Utils.passThruParens(left) instanceof JLocalVariableExpression &&
	    Utils.passThruParens(right) instanceof JLocalVariableExpression) {
	    arrayAssignments.put(((JLocalVariableExpression)Utils.passThruParens(left)).getVariable(),
				 ((JLocalVariableExpression)Utils.passThruParens(right)).getVariable());
	}

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

    /**
     *
     */
    public Object visitExpressionStatement(JExpressionStatement self,
					   JExpression expr) {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp!=null && newExp!=expr) {
	    self.setExpression(newExp);
	}
	else if (newExp == null) {
	    return new JEmptyStatement(null, null);
	}
	
	return self;
    }

    /**
     * prints an array allocator expression
     */
    public Object visitNewArrayExpression(JNewArrayExpression self,
					  CType type,
					  JExpression[] dims,
					  JArrayInitializer init)
    {
	assert false : "This visitor should not see JNewArrayExpressions";
	return self;
    }
}

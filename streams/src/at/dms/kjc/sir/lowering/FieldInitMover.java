package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.*;

/**
 * This class converts joint field definition/assignment statements to a field decl
 * and a corresponding field assignment statement in the init function. Eg
 * <br>
 * <pre>
 * int i = 5;
 * </pre>
 * into <br>
 * <pre>
 * int i;
 * i = 5;
 * </pre>
 * $Id: FieldInitMover.java,v 1.1 2002-06-21 20:01:26 aalamb Exp $
 **/
public class FieldInitMover extends EmptyStreamVisitor{

    public static void moveStreamInitialAssignments(SIRStream str) {
	FieldInitMover mover = new FieldInitMover();
	str.accept(mover);
    }

    /*
     * Visit a filter -- move any field initialization out of the field
     * declaration into the body of the init.
     */
    public void visitFilter(SIRFilter filter,
		     SIRStream parent,
		     JFieldDeclaration[] fields,
		     JMethodDeclaration[] methods,
		     JMethodDeclaration init,
		     JMethodDeclaration work,
		     CType inputType, CType outputType) {

	// assignment statements that need to be added to the init function 
	final Vector assignmentStatements = new Vector();
	
	for (int i=0; i<fields.length; i++) {
	    	    
	    // go and visit all of the field declarations
	    fields[i].accept(new SLIRReplacingVisitor() {
		    public Object visitFieldDeclaration(JFieldDeclaration self,
							int modifiers,
							CType type,
							String ident,
							JExpression expr)
		    {
			// if this field declaration has an initial value,
			// make an assignment expression to stick in the
			// init function
			System.out.println("Initial expression for field: " + expr);
			if (expr != null) {
			    // build up the this.field = initalValue expression
			    
			    
			    JThisExpression thisExpr = new JThisExpression (self.getTokenReference());
			    JFieldAccessExpression fieldExpr;
			    fieldExpr = new JFieldAccessExpression(self.getTokenReference(),
								   thisExpr,
								   ident);
			    
			    JAssignmentExpression assignExpr;
			    assignExpr = new JAssignmentExpression(self.getTokenReference(),
								   fieldExpr,
								   expr);
			    
			    JExpressionStatement assignStmt;
			    assignStmt = new JExpressionStatement(self.getTokenReference(),
								  assignExpr,
								  null);
			    // add the new statement to the list of statements we are going to
			    // add to the init function
			    assignmentStatements.add(assignStmt);
			    
			    // mutate the field so that it has no initializer expression
			    self.getVariable().setExpression(null);
			}
		    return self;
		    }
		}); // end crazy anonymous class
	} // end for loop
	
	// now, we have to add the initializing assignment statements
	// to the beginning of the init function.
	// Reverses the order of the initializations (because we see the fields
	// in reverse order above)
	if(filter.getInit()!=null) {
	    JBlock body = filter.getInit().getBody();
	    for (int i=0; i<assignmentStatements.size(); i++) {
		body.addStatement(0,
				  (JStatement)assignmentStatements.elementAt(i));
	    }
	}
    }
}

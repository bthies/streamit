package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
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
 * $Id: FieldInitMover.java,v 1.11 2004-07-15 00:07:24 thies Exp $
 **/
public class FieldInitMover extends EmptyStreamVisitor {
    public static final int MOVE_ARRAY_INITIALIZERS = 0;
    public static final int COPY_ARRAY_INITIALIZERS = 1;
    public static final int IGNORE_ARRAY_INITIALIZERS = 2;

    private final int moveArrayInitializers;
  
    private FieldInitMover(int moveArrayInitializers) {
	this.moveArrayInitializers = moveArrayInitializers;
    }

    public static void moveStreamInitialAssignments(SIRStream str, int moveArrayInitializers) {
	FieldInitMover mover = new FieldInitMover(moveArrayInitializers);
	IterFactory.createFactory().createIter(str).accept(mover);
    }


    /*
     * Visit each stream construct (eg Filter, Pipeline, SplitJoin or
     * FeedBackLoop) and move any field initialization out of the field
     * declaration into the body of the init.
     */
    public void preVisitStream(SIRStream self,
			       SIRIterator iter) {
	//System.out.println("!!visiting stream: " + self);
	
	moveFieldInitializations(self);
	
    }

    /**
     * Does the actual moving of field initializations
     * from their declarations to the init function.
     **/
    private void moveFieldInitializations(SIRStream filter) {
	// get a visitor that will walk down the filter, replacing fields as it goes
	FieldInitMoverVisitor harvester = new FieldInitMoverVisitor(moveArrayInitializers);

	// for each field declaration, harvest any initialization expressions
	JFieldDeclaration[] fields = filter.getFields();
	for (int i=0; i<fields.length; i++) {
	    fields[i].accept(harvester);
	}
	
	// now, we have to add the initializing assignment statements
	// to the beginning of the init function.
	// Reverses the order of the initializations (because we see the fields
	// in reverse order above)
	Vector newStatements = harvester.getAssignmentStatements();
	if(filter.getInit()!=null) {
	    JBlock body = filter.getInit().getBody();
	    for (int i=0; i<newStatements.size(); i++) {
		body.addStatement(0,
				  (JStatement)newStatements.elementAt(i));
	    }
	}
    }

    /**
     * A visitor class that goes throught the field declarations
     * of a filter removing any initialization statements
     * and generating a list of equivalent statements to add
     * to the start of the init function.
     **/

    static class FieldInitMoverVisitor extends SLIRReplacingVisitor {
	/** Assignments that need to be added to the initializations statements **/
	Vector assignmentStatements;
	private int moveArrayInitializers;

	FieldInitMoverVisitor(int moveArrayInitializers) {
	    super();
	    this.assignmentStatements = new Vector();
	    this.moveArrayInitializers = moveArrayInitializers;
	}

	public Vector getAssignmentStatements() {
	    return this.assignmentStatements;
	}

	/**
	 * Visit a field declaration. Mutates the initialization expr if
	 * present and creates the appropriate initialization statement instead.
	 **/
	public Object visitFieldDeclaration(JFieldDeclaration self,
					    int modifiers,
					    CType type,
					    String ident,
					    JExpression expr)
	{
	    //System.out.println("!!Visiting field: " + self);
	    // if this field declaration has an initial value,
	    // make an assignment expression to stick in the
	    // init function
	    //System.out.println("Initial expression for field: " + expr);
	    if (expr != null &&
		(moveArrayInitializers!=FieldInitMover.IGNORE_ARRAY_INITIALIZERS || !(expr instanceof JArrayInitializer))) {
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
		
		// only move if specified
		if (!(expr instanceof JArrayInitializer) || moveArrayInitializers==FieldInitMover.MOVE_ARRAY_INITIALIZERS) {
		    // mutate the field so that it has no initializer expression
		    self.getVariable().setExpression(null);
		}
	    }
	    return self;
	}
    }

}

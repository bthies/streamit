package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Removes what dead code we can detect within a filter.
 */
public class DeadCodeElimination {
    
    public static void doit(SIRFilter filter) {
	// for now, only removes dead field declarations and local
	// declarations
	removeDeadFieldDecls(filter);
	removeDeadLocalDecls(filter);
    }

    /**
     * Removes local variables that are never referenced.
     */
    private static void removeDeadLocalDecls(SIRFilter filter) {
	// variables used
	final HashSet varsUsed = new HashSet();

	// in all the methods...
	JMethodDeclaration[] methods = filter.getMethods();
	for (int i=0; i<methods.length; i++) {
	    // take a recording pass, seeing what's used
	    methods[i].accept(new SLIREmptyVisitor() {
		    /**
		     * only descend into RHS of assignment expression.
		     * If something is assigned but never referenced,
		     * it's still dead.
		     */
		    public void visitAssignmentExpression(JAssignmentExpression self,
							  JExpression left,
							  JExpression right) {
			right.accept(this);
		    }

		    public void visitLocalVariableExpression(JLocalVariableExpression self,
							     String ident) {
			super.visitLocalVariableExpression(self, ident);
			varsUsed.add(self.getVariable());
		    }
		});
	}
	    
	for (int i=0; i<methods.length; i++) {
	    // take a deleting pass on assignment expressions.  if we
	    // find an assignment that has both used and unused vars
	    // (e.g., something compound) then mark all variables
	    // enclosed as used so that we don't delete their declaration.

	    // first only detect dead assignments, then remove them
	    for (int j=0; j<2; j++) {
		final boolean REMOVING = (j==1);
		methods[i].accept(new SLIRReplacingVisitor() {
			/**
			 * visits a for statement.  Only recurse into body
			 * (don't want to remove var decls from init, incr).
			 */
			public Object visitForStatement(JForStatement self,
							JStatement init,
							JExpression cond,
							JStatement incr,
							JStatement body) {
			    if (!REMOVING) {
				return super.visitForStatement(self, init, cond, incr, body);
			    } else {
				// only remove in body
				JStatement newBody = (JStatement)body.accept(this);
				if (newBody!=null && newBody!=body) {
				    self.setBody(newBody);
				}
				return self;
			    }
			}
		    
			/**
			 * remove assignments to dead vars.
			 */
			public Object visitExpressionStatement(JExpressionStatement self,
							       JExpression expr) {
			    if (expr instanceof JAssignmentExpression) {
				JAssignmentExpression assign = (JAssignmentExpression)expr;
				final boolean assigningToDeadVar[] = new boolean[1];
				final boolean assigningToLiveVar[] = new boolean[1];
				final LinkedList dead = new LinkedList();
				assign.getLeft().accept(new SLIREmptyVisitor() {
					public void visitLocalVariableExpression(JLocalVariableExpression self,
										 String ident) {
					    if (!(varsUsed.contains(self.getVariable()))) {
						assigningToDeadVar[0] = true;
						dead.add(self.getVariable());
					    } else {
						assigningToLiveVar[0] = true;
					    }
					}
				    });
				// don't currently support assigning to
				// both live and dead var (could happen in
				// nested assignments, etc.?)
				if (assigningToDeadVar[0] && assigningToLiveVar[0]) {
				    // mark dead as live
				    varsUsed.addAll(dead);
				    /*
				      at.dms.util.Utils.fail("There is a nested assignment where one variable is\n" + 
				      "live and one variable is dead; this is not currently\n" +
				      "supported by DeadCodeElimination.");
				    */
				} else if (assigningToDeadVar[0] && REMOVING) {
				    // replace with RHS instead of
				    // empty statement since there
				    // might be side effects on right
				    // side (method calls, increments,
				    // pop expressions, etc.)
				    return new JExpressionStatement(null, assign.getRight(), null);
				}
			    }
			    return super.visitExpressionStatement(self, expr);
			}
		    });
	    }
	}

	for (int i=0; i<methods.length; i++) {
	    // take a deleting pass, removing declarations that were not used
	    methods[i].accept(new SLIRReplacingVisitor() {
		    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
								  JVariableDefinition[] vars) {
			ArrayList newVars = new ArrayList();
			// see if vars used
			for (int i=0; i<vars.length; i++) {
			    if (varsUsed.contains(vars[i])) {
				newVars.add(vars[i]);
			    }
			}

			if (newVars.size()>0) {
			    // if we have some vars, adjust us
			    self.setVars((JVariableDefinition[])newVars.toArray(new JVariableDefinition[0]));
			    return self;
			} else {
			    // otherwise, replace us with empty statement
			    return new JEmptyStatement(null, null);
			}
		    }});
	}
    }

    /**
     * Removes fields that are never referenced.
     */
    private static void removeDeadFieldDecls(SIRFilter filter) {
	// keep track of field names referenced
	final HashSet fieldsUsed = new HashSet();

	// get field references in all the methods
	JMethodDeclaration[] methods = filter.getMethods();
	for (int i=0; i<methods.length; i++) {
	    methods[i].accept(new SLIREmptyVisitor() {
		    public void visitFieldExpression(JFieldAccessExpression self,
						     JExpression left,
						     String ident) {
			super.visitFieldExpression(self, left, ident);
			fieldsUsed.add(ident);
		    }
		});
	}
	
	// remove any field that was not referenced
	JFieldDeclaration[] fields = filter.getFields();
	ArrayList fieldsToKeep = new ArrayList();
	int removed = 0;
	for (int i=0; i<fields.length; i++) {
	    JFieldDeclaration decl = (JFieldDeclaration)fields[i];
	    if (fieldsUsed.contains(decl.getVariable().getIdent())) {
		fieldsToKeep.add(decl);
	    } else {
		removed++;
	    }
	}
	filter.setFields((JFieldDeclaration[])fieldsToKeep.toArray(new JFieldDeclaration[0]));
	/*
	if (removed>0) {
	    System.err.println("  Removed " + removed + " dead fields.");
	}
	*/
    }
}

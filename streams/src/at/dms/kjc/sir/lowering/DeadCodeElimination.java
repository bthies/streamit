package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.JavaStyleComment;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Removes what dead code we can detect within a filter.
 */
public class DeadCodeElimination {
    
    public static void doit(SIRFilter filter) {
	// for now, only removes dead field declarations and local
	// declarations
	removeDeadFieldDecls(filter);
	removeDeadLocalDecls(filter);
	removeEmptyStatements(filter);
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
			// if not a local variable must descend into left
			if (!(left instanceof JLocalVariableExpression)) {
			    left.accept(this);
			}
			right.accept(this);
		    }

		    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
								  int oper,
								  JExpression left,
								  JExpression right) {
			// if not a local variable must descend into left
			if (!(left instanceof JLocalVariableExpression)) {
			    left.accept(this);
			}
			right.accept(this);
		    }


		    public void visitLocalVariableExpression(JLocalVariableExpression self,
							     String ident) {
			super.visitLocalVariableExpression(self, ident);
			System.err.println("var used: " + ident);
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


			private boolean eliminateAssignment(JExpression left,
							    JExpression right) {

			    final boolean assigningToDeadVar[] = new boolean[1];
			    final boolean assigningToLiveVar[] = new boolean[1];
			    final LinkedList dead = new LinkedList();
			    left.accept(new SLIREmptyVisitor() {
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
				return true;
			    }
			    return false;
			}

			public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						    int oper,
						    JExpression left,
						    JExpression right) {
			    if (eliminateAssignment(left, right)) { 
				// replace with RHS instead of
				// empty statement since there
				// might be side effects on right
				// side (method calls, increments,
				// pop expressions, etc.)
				return right;
			    }
			    return self;
			}


			public Object visitAssignmentExpression(JAssignmentExpression self,
								JExpression left,
								JExpression right) {   
			    if (eliminateAssignment(left, right)) { 
				// replace with RHS instead of
				// empty statement since there
				// might be side effects on right
				// side (method calls, increments,
				// pop expressions, etc.)
				return right;
			    }
			    return self;
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

    /**
     * Removes empty statements
     */
    private static void removeEmptyStatements(SIRFilter filter) {
	// get field references in all the methods
	JMethodDeclaration[] methods = filter.getMethods();
	for (int i=0; i<methods.length; i++) {
	    JBlock newBody = (JBlock)methods[i].getBody().accept(new SLIRReplacingVisitor() {
		    public Object visitBlockStatement(JBlock self,
						      JavaStyleComment[] comments) {
			ArrayList newStatements = new ArrayList();
			for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
			    JStatement oldBody = (JStatement)it.next();
			    Object newBody = oldBody.accept(this);
			    if (newBody != null) newStatements.add(newBody);
			}
			return new JBlock(null,(JStatement[])newStatements.toArray(new JStatement[0]),null);
		    }
		    public Object visitEmptyStatement(JEmptyStatement self) {
			return null;
		    }
		    public Object visitExpressionStatement(JExpressionStatement self,
							   JExpression expr) {
			if (expr instanceof JLiteral) { return null; }
			if (expr instanceof JLocalVariableExpression) { return null; }
			if (expr instanceof JFieldAccessExpression) { return null; }
			return self;
		    }
		    public Object visitExpressionListStatement(JExpressionListStatement self,
							       JExpression[] expr) {
			ArrayList newList = new ArrayList();
			for (int i = 0; i < expr.length; i++) {
			    if (expr[i] instanceof JLiteral) continue;
			    if (expr[i] instanceof JLocalVariableExpression) continue;
			    if (expr[i] instanceof JFieldAccessExpression) continue;
			    Object newExpr = expr[i].accept(this);
			    if (newExpr != null) {
				newList.add(newExpr);
			    }
			}
			if (newList.size() == 0) return null;
			return new JExpressionListStatement(null, (JExpression[])newList.toArray(new JExpression[0]), null);
		    }
		    public Object visitCompoundStatement(JCompoundStatement self,
							 JStatement[] body) {

			ArrayList newList = new ArrayList();
			for (int i = 0; i < body.length; i++) {
			    Object newExpr = body[i].accept(this);
			    if (newExpr != null) {
				newList.add(newExpr);
			    }
			}
			if (newList.size() == 0) return null;
			return new JCompoundStatement(null, (JStatement[])newList.toArray(new JStatement[0]));
		    }
		    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
								  JVariableDefinition[] vars) {
			if (vars.length == 0) return null; else return self;
		    }
		});
	    methods[i].setBody(newBody);
	}
    }

}




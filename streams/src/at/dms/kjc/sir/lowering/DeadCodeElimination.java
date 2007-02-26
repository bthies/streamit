package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.JavaStyleComment;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Removes what dead code we can detect within a filter.  
 * For local variables only, don't remove volatile variables.
 * 
 * Note: uses a set of variables in the calculation, so requires
 * identical hash codes for JVariableDefinition's for the declaration
 * and all uses.  Since Java uses identity-based hash codes by default,
 * this means that the <b>same JVariableDefinition</b>must be used for the
 * declaration and all uses.
 */
public class DeadCodeElimination {
    
    public static void doit(SIRCodeUnit unit) {
        // for now, only removes dead field declarations and local
        // declarations
        removeDeadFieldDecls(unit);
        removeDeadLocalDecls(unit);
        removeEmptyStatements(unit);
    }

    /**
     * Visitor to find un-dead locals.
     * (local variables that occur as r-values).
     */
    
    private static class FindLocals extends SLIREmptyAttributeVisitor {
        Set<JLocalVariable> varsUsed;

        /** 
         * Constructor
         * @param varsUsed  reference to set of variables, to be updated with local variables that occur as r-values
         */
        public FindLocals(Set<JLocalVariable> varsUsed) {
            this.varsUsed = varsUsed;
        }
        
        // visitor bits:
        
        /**
         * only descend into RHS of assignment expression.
         * If something is assigned but never referenced,
         * it's still dead.
         */
        public Object visitAssignmentExpression(JAssignmentExpression self,
                                              JExpression left,
                                              JExpression right) {
            // if not a local variable must descend into left
            if (!(left instanceof JLocalVariableExpression)) {
                left.accept(this);
            }
            right.accept(this);
            return null;
        }

        public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                      int oper,
                                                      JExpression left,
                                                      JExpression right) {
            // if not a local variable must descend into left
            if (!(left instanceof JLocalVariableExpression)) {
                left.accept(this);
            }
            right.accept(this);
            return null;
        }


        // If we get here, we have a variable reference that is user as a r-value.
        // add it to set of used variables.
        public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                                 String ident) {
            super.visitLocalVariableExpression(self, ident);
            varsUsed.add(self.getVariable());
            return null;
        }
        
    }
    
    /**
     * Removes local variables that are never referenced.
     */
    public static void removeDeadLocalDecls(SIRCodeUnit unit) {
        // variables used
        final Set<JLocalVariable> varsUsed = new HashSet<JLocalVariable>();

        // in all the methods...
        JMethodDeclaration[] methods = unit.getMethods();
        FindLocals findLocals = new FindLocals(varsUsed);
        for (int i=0; i<methods.length; i++) {
            // take a recording pass, seeing what's used,
            // accumulate un-dead variables to get from FindLocals.getVarsUsed()
            methods[i].accept(findLocals);
        }
        
        if (unit instanceof SIRStream) {
            StaticsProp.IterOverAllFieldsAndMethods.iterOverFieldsAndMethods((SIRStream)unit, false, false, true, findLocals);
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
                            final LinkedList<JLocalVariable> dead = new LinkedList<JLocalVariable>();
                            left.accept(new SLIREmptyVisitor() {
                                    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                                                             String ident) {
                                        if (!(varsUsed.contains(self.getVariable()) ||
                                                self.getVariable().isVolatile())) {
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
                        ArrayList<JVariableDefinition> newVars = new ArrayList<JVariableDefinition>();
                        // see if vars used
                        for (int i=0; i<vars.length; i++) {
                            if (varsUsed.contains(vars[i]) || 
                                    vars[i].isVolatile()) {
                                newVars.add(vars[i]);
                            }
                        }

                        if (newVars.size()>0) {
                            // if we have some vars, adjust us
                            self.setVars(newVars.toArray(new JVariableDefinition[0]));
                            return self;
                        } else {
//                            System.err.println("removing declaration");
//                            for (int i=0; i<vars.length; i++) {
//                            System.err.println("  var in declaration: " + vars[i].getIdent() + " " + vars[i].hashCode());
//                            }
                            // otherwise, replace us with empty statement
                            return new JEmptyStatement(null, null);
                        }
                    }});
        }
    }

    /**
     * Removes fields that are never referenced.
     */
    private static void removeDeadFieldDecls(SIRCodeUnit unit) {
        // you probably don't want to run this on a static block,
        // since other streams are referencing those fields and this
        // property is not checked here
        if (unit instanceof SIRGlobal) {
            System.err.println("WARNING:  Removing dead fields within a static block.  This will");
            System.err.println("  remove static fields even if they are used in other streams.");
        }

        // keep track of field names referenced
        final HashSet<String> fieldsUsed = new HashSet<String>();

        // get field references in all the methods
        JMethodDeclaration[] methods = unit.getMethods();
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
        JFieldDeclaration[] fields = unit.getFields();
        ArrayList<JFieldDeclaration> fieldsToKeep = new ArrayList<JFieldDeclaration>();
        int removed = 0;
        for (int i=0; i<fields.length; i++) {
            JFieldDeclaration decl = (JFieldDeclaration)fields[i];
            if (fieldsUsed.contains(decl.getVariable().getIdent())) {
                fieldsToKeep.add(decl);
            } else {
                removed++;
            }
        }
        unit.setFields(fieldsToKeep.toArray(new JFieldDeclaration[0]));
        /*
          if (removed>0) {
          System.err.println("  Removed " + removed + " dead fields.");
          }
        */
    }

    /**
     * Removes empty statements
     */
    private static void removeEmptyStatements(SIRCodeUnit unit) {
        // get field references in all the methods
        JMethodDeclaration[] methods = unit.getMethods();
        for (int i=0; i<methods.length; i++) {
            JBlock newBody = (JBlock)methods[i].getBody().accept(new SLIRReplacingVisitor() {
                    public Object visitBlockStatement(JBlock self,
                                                      JavaStyleComment[] comments) {
                        ArrayList<Object> newStatements = new ArrayList<Object>();
                        for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
                            JStatement oldBody = (JStatement)it.next();
                            Object newBody = oldBody.accept(this);
                            if (newBody != null) newStatements.add(newBody);
                        }
                        return new JBlock(null,newStatements.toArray(new JStatement[0]),null);
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
                        ArrayList<Object> newList = new ArrayList<Object>();
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
                        return new JExpressionListStatement(null, newList.toArray(new JExpression[0]), null);
                    }
                    public Object visitCompoundStatement(JCompoundStatement self,
                                                         JStatement[] body) {

                        ArrayList<Object> newList = new ArrayList<Object>();
                        for (int i = 0; i < body.length; i++) {
                            Object newExpr = body[i].accept(this);
                            if (newExpr != null) {
                                newList.add(newExpr);
                            }
                        }
                        if (newList.size() == 0) return null;
                        return new JCompoundStatement(null, newList.toArray(new JStatement[0]));
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




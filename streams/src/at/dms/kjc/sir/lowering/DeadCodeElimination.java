package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.HashSet;
import java.util.ArrayList;

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
	// in all the methods...
	JMethodDeclaration[] methods = filter.getMethods();
	for (int i=0; i<methods.length; i++) {
	    // variables used
	    final HashSet varsUsed = new HashSet();

	    // take a recording pass, seeing what's used
	    methods[i].accept(new SLIREmptyVisitor() {
		    public void visitLocalVariableExpression(JLocalVariableExpression self,
							     String ident) {
			super.visitLocalVariableExpression(self, ident);
			varsUsed.add(self.getVariable());
		    }
		});

	    // take a deleting pass, removing things that were not used
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

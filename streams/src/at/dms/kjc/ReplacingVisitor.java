package at.dms.kjc;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This class descends through the tree, and tests if any of the
 * returned STATEMENTS are different from old statements in the tree.
 * If a difference is detected, then the new statement is substituted
 * for the original.
 *
 * It would be desirable to extend this class so that it's a complete
 * replacing visitor--i.e., it also replaces expressions and
 * everything else that is modified in the tree.  However, this would
 * be kind of tedious and we haven't had need for it yet--but it you
 * end up needing that code, let's put it in here instead of in a
 * class that's specific to some compiler pass.
 *
 */
public class ReplacingVisitor extends EmptyAttributeVisitor {
    
    /**
     * Creates a new one of these.
     */
    public ReplacingVisitor() {}

    /**
     * prints a labeled statement
     */
    public Object visitLabeledStatement(JLabeledStatement self,
					String label,
					JStatement stmt) {
	JStatement newStmt = (JStatement)stmt.accept(this);
	if (newStmt!=null && newStmt!=stmt) {
	    self.setBody(newStmt);
	}
	return self;
    }

    /**
     * prints a if statement
     */
    public Object visitIfStatement(JIfStatement self,
				   JExpression cond,
				   JStatement thenClause,
				   JStatement elseClause) {
	cond.accept(this);
	JStatement newThen = (JStatement)thenClause.accept(this);
	if (newThen!=null && newThen!=thenClause) {
	    self.setThenClause(newThen);
	}
	if (elseClause != null) {
	    JStatement newElse = (JStatement)elseClause.accept(this);
	    if (newElse!=null && newElse!=elseClause) {
		self.setElseClause(newElse);
	    }
	}
	return self;
    }

    /**
     * prints a compound statement
     */
    public Object visitCompoundStatement(JCompoundStatement self,
					 JStatement[] body) {
	for (int i = 0; i < body.length; i++) {
	    JStatement newBody = (JStatement)body[i].accept(this);
	    if (newBody!=null && newBody!=body[i]) {
		body[i] = newBody;
	    }
	}
	return self;
    }

    /**
     * prints a do statement
     */
    public Object visitDoStatement(JDoStatement self,
				   JExpression cond,
				   JStatement body) {
	JStatement newBody = (JStatement)body.accept(this);
	if (newBody!=null && newBody!=body) {
	    self.setBody(newBody);
	}
	cond.accept(this);
	return self;
    }

    /**
     * prints an expression statement
     */
    public Object visitBlockStatement(JBlock self,
				      JavaStyleComment[] comments) {
	for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
	    JStatement oldBody = (JStatement)it.next();
	    JStatement newBody = (JStatement)oldBody.accept(this);
	    if (newBody!=null && newBody!=oldBody) {
		it.set(newBody);
	    }
	}
	visitComments(comments);
	return self;
    }

    /**
     * prints an array length expression
     */
    public Object visitSwitchGroup(JSwitchGroup self,
				   JSwitchLabel[] labels,
				   JStatement[] stmts) {
	for (int i = 0; i < labels.length; i++) {
	    labels[i].accept(this);
	}
	for (int i = 0; i < stmts.length; i++) {
	    JStatement newStmt = (JStatement)stmts[i].accept(this);
	    if (newStmt!=null && newStmt!=stmts[i]) {
		stmts[i] = newStmt;
	    }
	}
	return self;
    }

    /**
     * visits a for statement
     */
    public Object visitForStatement(JForStatement self,
				    JStatement init,
				    JExpression cond,
				    JStatement incr,
				    JStatement body) {
	// recurse into init
	JStatement newInit = (JStatement)init.accept(this);
	if (newInit!=null && newInit!=init) {
	    self.setInit(newInit);
	}
	// recurse into cond
	cond.accept(this);
	// recurse into incr
	JStatement newIncr = (JStatement)incr.accept(this);
	if (newIncr!=null && newIncr!=incr) {
	    self.setIncr(newIncr);
	}
	// recurse into body
	JStatement newBody = (JStatement)body.accept(this);
	if (newBody!=null && newBody!=body) {
	    self.setBody(newBody);
	}
	return self;
    }
}

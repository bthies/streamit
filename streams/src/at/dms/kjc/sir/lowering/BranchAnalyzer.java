package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import java.lang.Math;
import at.dms.compiler.TokenReference;

/**
 * This class aggressively analyzes branches in control flow for information
 * gained and calls constant prop on the rest of the method with the
 * new information gained.
 */
public class BranchAnalyzer extends Propagator {
    public BranchAnalyzer() {
	//Want to just build up constants in the beginning
	//Only when we gather new information do we start writing
	super(new Hashtable(),false);
    }

    // ----------------------------------------------------------------------
    // Analyzing branches
    // ----------------------------------------------------------------------

    public void analyzeBranches(SIRStream str) {
	if (str instanceof SIRFeedbackLoop)
	    {
		SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
		analyzeBranches(fl.getBody());
		analyzeBranches(fl.getLoop());
	    }
        if (str instanceof SIRPipeline)
	    {
		SIRPipeline pl = (SIRPipeline)str;
		Iterator iter = pl.getChildren().iterator();
		while (iter.hasNext())
		    {
			SIRStream child = (SIRStream)iter.next();
			analyzeBranches(child);
		    }
	    }
        if (str instanceof SIRSplitJoin)
	    {
		SIRSplitJoin sj = (SIRSplitJoin)str;
		Iterator iter = sj.getParallelStreams().iterator();
		while (iter.hasNext())
		    {
			SIRStream child = (SIRStream)iter.next();
			analyzeBranches(child);
		    }
	    }
	if (str instanceof SIRFilter)
	    for (int i = 0; i < str.getMethods().length; i++) {
		str.getMethods()[i].accept(this);
	    }
    }

    /**
     * prints an expression statement
     */
    /*public Object visitBlockStatement(JBlock self,
				      JavaStyleComment[] comments) {
	for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
	    JStatement oldBody = (JStatement)it.next();
	    Object newBody = oldBody.accept(this);
	    if (!(newBody instanceof JStatement))
		continue;
	    if (newBody!=null && newBody!=oldBody) {
		it.set((JStatement)newBody);
	    }
	}
	visitComments(comments);
	return self;
	}*/

    /**
     * Analyzes an if statement
     */
    /*public Object visitIfStatement(JIfStatement self,
				   JExpression cond,
				   JStatement thenClause,
				   JStatement elseClause) {
	
	return self;
	}*/
}

package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import java.util.*;

/**
 * This class adds LIR hooks to work functions.
 */
public class LowerWorkFunctions implements StreamVisitor 
{
    /**
     * Lowers the work functions in <str>.
     */
    public static void lower(SIRStream str)
    {
        str.accept(new LowerWorkFunctions());
    }
    
    private void addEntryExit(JMethodDeclaration method)
    {
        // add the entry node after all of the variable declarations
        int pos = 0;
        Iterator statements = method.getStatementIterator();
        while (statements.hasNext()) {
            JStatement stmt = (JStatement)statements.next();
            if (!(stmt instanceof JVariableDeclarationStatement))
            {
                List newStmts = new LinkedList();
                newStmts.add(new LIRWorkEntry
                             (LoweringConstants.getStreamContext()));
                method.addAllStatements(pos, newStmts);
                break;
            }
            pos++;
        }
        // append an exit node
        method.addStatement(new LIRWorkExit
                            (LoweringConstants.getStreamContext()));
        // prepend exit nodes before any return statements, too
        method.accept(new SLIRReplacingVisitor() {
                public Object visitReturnStatement(JReturnStatement self,
                                                   JExpression expr)
                {
                    JStatement[] stmts = new JStatement[2];
                    stmts[0] =
                        new LIRWorkExit(LoweringConstants.getStreamContext());
                    stmts[1] = self;
                    return new JBlock(self.getTokenReference(), stmts, null);
                }
            });
    }
    
    /**
     * PLAIN-VISITS 
     */
     
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init,
			    JMethodDeclaration work,
			    CType inputType, CType outputType) {
	// only worry about actual SIRFilter's, not special cases like
	// FileReader's and FileWriter's
	if (!self.getClass().toString().endsWith("at.dms.kjc.sir.SIRFilter")) {
	    return;
	}
        // add entry/exit nodes to work function
        addEntryExit(work);
    }
  
    /* visit a splitter */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      JExpression[] weights) {
	// create struct type (no - not needed anymore by runtime)
	// createStruct(self.getName(), JFieldDeclaration.EMPTY, EMPTY_LIST);
    }
  
    /* visit a joiner */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    JExpression[] weights) {
	// create struct type (no - not needed anymore by runtime)
	// createStruct(self.getName(), JFieldDeclaration.EMPTY, EMPTY_LIST);
    }

    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {
	// don't do anything--visit on the way up
    }
  
    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	// don't do anything--visit on the way up
    }
  
    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	// don't do anything--visit on the way up
    }
  
    /**
     * POST-VISITS 
     */

    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
    }
  
    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
    }
  
    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
    }
}

package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.List;

/**
 * This visitor is for visiting stream structures in the SIR.  It does
 * not visit statement-level constructs like SIRInitStatement,
 * SIRPushStatement, etc.
 *
 * The recursing process from one stream construct to another is
 * automatic--one does not need to write code to visit the children.
 */
public interface StreamVisitor {
    /**
     * PLAIN-VISITS 
     */
	    
    /* visit a filter */
    void visitFilter(SIRFilter self,
		     SIRStream parent,
		     JFieldDeclaration[] fields,
		     JMethodDeclaration[] methods,
		     JMethodDeclaration init,
		     JMethodDeclaration work,
		     CType inputType, CType outputType);
  
    /* visit a splitter */
    void visitSplitter(SIRSplitter self,
		       SIRStream parent,
		       SIRSplitType type,
		       JExpression[] weights);
    
    /* visit a joiner */
    void visitJoiner(SIRJoiner self,
		     SIRStream parent,
		     SIRJoinType type,
		     JExpression[] weights);

    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    void preVisitPipeline(SIRPipeline self,
			  SIRStream parent,
			  JFieldDeclaration[] fields,
			  JMethodDeclaration[] methods,
			  JMethodDeclaration init,
			  List elements);

    /* pre-visit a splitjoin */
    void preVisitSplitJoin(SIRSplitJoin self,
			   SIRStream parent,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods,
			   JMethodDeclaration init);

    /* pre-visit a feedbackloop */
    void preVisitFeedbackLoop(SIRFeedbackLoop self,
			      SIRStream parent,
			      JFieldDeclaration[] fields,
			      JMethodDeclaration[] methods,
			      JMethodDeclaration init,
			      int delay,
			      JMethodDeclaration initPath);

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    void postVisitPipeline(SIRPipeline self,
			   SIRStream parent,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods,
			   JMethodDeclaration init,
			   List elements);

    /* post-visit a splitjoin */
    void postVisitSplitJoin(SIRSplitJoin self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init);

    /* post-visit a feedbackloop */
    void postVisitFeedbackLoop(SIRFeedbackLoop self,
			       SIRStream parent,
			       JFieldDeclaration[] fields,
			       JMethodDeclaration[] methods,
			       JMethodDeclaration init,
			       int delay,
			       JMethodDeclaration initPath);
}

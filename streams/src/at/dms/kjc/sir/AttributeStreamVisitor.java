package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.List;

/**
 * This visitor is for visiting stream structures in the SIR.  It
 * allows each visit to return an object for the caller to see.  It
 * does not visit statement-level constructs like SIRInitStatement,
 * SIRPushStatement, etc, nor (UNLIKE StreamVisitor) does it do the
 * recursing to component streams automatically.  
 */
public interface AttributeStreamVisitor {
    /* visit a filter */
    Object visitFilter(SIRFilter self,
		       SIRStream parent,
		       JFieldDeclaration[] fields,
		       JMethodDeclaration[] methods,
		       JMethodDeclaration init,
		       int peek, int pop, int push,
		       JMethodDeclaration work,
		       CType inputType, CType outputType);
  
    /* visit a splitter */
    Object visitSplitter(SIRSplitter self,
			 SIRStream parent,
			 SIRSplitType type,
			 int[] weights);
    
    /* visit a joiner */
    Object visitJoiner(SIRJoiner self,
			SIRStream parent,
			SIRJoinType type,
			int[] weights);
    
    /* pre-visit a pipeline */
    Object visitPipeline(SIRPipeline self,
			 SIRStream parent,
			 JFieldDeclaration[] fields,
			 JMethodDeclaration[] methods,
			 JMethodDeclaration init,
			 List elements);

    /* pre-visit a splitjoin */
    Object visitSplitJoin(SIRSplitJoin self,
			  SIRStream parent,
			  JFieldDeclaration[] fields,
			  JMethodDeclaration[] methods,
			  JMethodDeclaration init,
			  List elements,
			  SIRSplitter splitter,
			  SIRJoiner joiner);

    /* pre-visit a feedbackloop */
    Object visitFeedbackLoop(SIRFeedbackLoop self,
			     SIRStream parent,
			     JFieldDeclaration[] fields,
			     JMethodDeclaration[] methods,
			     JMethodDeclaration init,
			     int delay,
			     JMethodDeclaration initPath);
}

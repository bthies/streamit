package at.dms.kjc.sir;

import at.dms.kjc.*;

public interface SIRVisitor {

  /* visit a pipeline */
  void visitPipeline(SIRPipeline self,
		     JFieldDeclaration[] fields,
		     JMethodDeclaration[] methods,
		     JMethodDeclaration init);

  /* visit a splitjoin */
  void visitSplitJoin(SIRSplitJoin self,
		      JFieldDeclaration[] fields,
		      JMethodDeclaration[] methods,
		      JMethodDeclaration init);

  /* visit a feedbackloop */
  void visitFeedbackLoop(SIRFeedbackLoop self,
			 JFieldDeclaration[] fields,
			 JMethodDeclaration[] methods,
			 JMethodDeclaration init,
			 int delay,
			 JMethodDeclaration initPath);

  /* visit a filter */
  void visitFilter(SIRFilter self,
		   JFieldDeclaration[] fields,
		   JMethodDeclaration[] methods,
		   JMethodDeclaration init,
		   int peek, int pop, int push,
		   JMethodDeclaration work,
		   CType inputType, CType outputType);
  
  /* visit a splitter */
  void visitSplitter(SIRSplitter self,
		     SIRSplitType type,
		     int[] weights);

  /* visit a joiner */
  void visitJoiner(SIRJoiner self,
		   SIRJoinType type,
		   int[] weights);

}

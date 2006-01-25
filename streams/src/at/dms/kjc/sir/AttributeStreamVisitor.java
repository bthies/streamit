package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.List;

/**
 * This visitor is for visiting stream structures in the SIR.  It
 * allows each visit to return an object for the caller to see.  It
 * does not visit statement-level constructs like SIRInitStatement,
 * SIRPushStatement, etc, nor (UNLIKE StreamVisitor) does it do the
 * recursing to component streams automatically.  
 *
 * The differences between this and StreamVisitor is that:
 *  
 *  1. Each method returns an object that can be used by the caller.
 *
 *  2. As a result of this, recursion is not automatic; you call the
 *     sub-visit methods if you want to visit one of them.
 *
 *  3. The recursion is done through the stream classes instead of
 *     through iterators.  Consequently, you don't get a reference
 *     to the parent in the visitor.
 *
 *  4. This visits splitters/joiners, whereas StreamVisitor only visits 
 *     streams.
 *
 */
public interface AttributeStreamVisitor {
    /* visit a structure */
    Object visitStructure(SIRStructure self,
                          JFieldDeclaration[] fields);

    /* visit a filter */
    Object visitFilter(SIRFilter self,
                       JFieldDeclaration[] fields,
                       JMethodDeclaration[] methods,
                       JMethodDeclaration init,
                       JMethodDeclaration work,
                       CType inputType, CType outputType);
  
    /* visit a phased filter */
    Object visitPhasedFilter(SIRPhasedFilter self,
                             JFieldDeclaration[] fields,
                             JMethodDeclaration[] methods,
                             JMethodDeclaration init,
                             JMethodDeclaration work,
                             JMethodDeclaration[] initPhases,
                             JMethodDeclaration[] phases,
                             CType inputType, CType outputType);
  
    /* visit a splitter */
    Object visitSplitter(SIRSplitter self,
                         SIRSplitType type,
                         JExpression[] weights);
    
    /* visit a joiner */
    Object visitJoiner(SIRJoiner self,
                       SIRJoinType type,
                       JExpression[] weights);
    
    /* pre-visit a pipeline */
    Object visitPipeline(SIRPipeline self,
                         JFieldDeclaration[] fields,
                         JMethodDeclaration[] methods,
                         JMethodDeclaration init);

    /* pre-visit a splitjoin */
    Object visitSplitJoin(SIRSplitJoin self,
                          JFieldDeclaration[] fields,
                          JMethodDeclaration[] methods,
                          JMethodDeclaration init,
                          SIRSplitter splitter,
                          SIRJoiner joiner);

    /* pre-visit a feedbackloop */
    Object visitFeedbackLoop(SIRFeedbackLoop self,
                             JFieldDeclaration[] fields,
                             JMethodDeclaration[] methods,
                             JMethodDeclaration init,
                             JMethodDeclaration initPath);
}

package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.List;
import java.util.Iterator;

/**
 * This is an empty attribute stream visitor that does nothing
 * except the recursion.
 */
public class EmptyAttributeStreamVisitor implements AttributeStreamVisitor {
    /* visit a structure */
    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields) {
        return self;
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType) {
        return self;
    }
  
    /* visit a phased filter */
    public Object visitPhasedFilter(SIRPhasedFilter self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration work,
                                    JMethodDeclaration[] initPhases,
                                    JMethodDeclaration[] phases,
                                    CType inputType, CType outputType)
    {
        return self;
    }
  
    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] weights) {
        return self;
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] weights) {
        return self;
    }

    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init) {
        /** visit each child. **/
        Iterator childIter = self.getChildren().iterator();
        while(childIter.hasNext()) {
            SIROperator currentChild = (SIROperator)childIter.next();
            currentChild.accept(this);
        }
        return self;
    }

    /* pre-visit a splitjoin */
    public Object visitSplitJoin(SIRSplitJoin self,
                                 JFieldDeclaration[] fields,
                                 JMethodDeclaration[] methods,
                                 JMethodDeclaration init,
                                 SIRSplitter splitter,
                                 SIRJoiner joiner) {
        // visit splitter
        self.getSplitter().accept(this);
        // visit children
        Iterator childIter = self.getParallelStreams().iterator();
        while(childIter.hasNext()) {
            SIROperator currentChild = (SIROperator)childIter.next();
            currentChild.accept(this);
        }
        // visit joiner 
        self.getJoiner().accept(this);

        return self;
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath) {
        // visit joiner 
        self.getJoiner().accept(this);
        // visit the body
        self.getBody().accept(this);
        // visit the loop
        self.getLoop().accept(this);
        // visit splitter
        self.getSplitter().accept(this);

        return self;
    }
}

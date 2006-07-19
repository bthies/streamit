package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.List;
import java.util.Iterator;

/**
 * Visits all stream structures and replaces children with result from
 * recursion.
 */
public class ReplacingStreamVisitor implements AttributeStreamVisitor {
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
        for (int i=0; i<self.size(); i++) {
            self.set(i, (SIRStream)self.get(i).accept(this));
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
        self.setSplitter((SIRSplitter)self.getSplitter().accept(this));
        // visit children
        for (int i=0; i<self.size(); i++) {
            self.set(i, (SIRStream)self.get(i).accept(this));
        }
        // visit joiner 
        self.setJoiner((SIRJoiner)self.getJoiner().accept(this));

        return self;
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath) {
        // visit joiner 
        self.setJoiner((SIRJoiner)self.getJoiner().accept(this));
        // visit the body
        self.setBody((SIRStream)self.getBody().accept(this));
        // visit the loop
        self.setLoop((SIRStream)self.getLoop().accept(this));
        // visit splitter
        self.setSplitter((SIRSplitter)self.getSplitter().accept(this));

        return self;
    }
}

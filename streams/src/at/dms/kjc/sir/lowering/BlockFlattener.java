package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;


/**
 * This class flattens nested blocks in methods.
 * 
 * <p>Having single flat blocks for the bodies of methods, and
 * the branches of ifs, and bodies of fors makes some optimizations
 * more effective.<br/>
 * {@link BranchAnalyzer} assumes / requires that blocks have been flattenned.<br/>
 * N.B. BlockFlattener does not raise declarations.
 * BlockFlattener loses scope information -- that is its purpose --
 * so (like almost all optimization passes) it should not be run before 
 * variables have been renamed to unique names.</p>
 */
public class BlockFlattener extends SLIRReplacingVisitor implements FlatVisitor {
    public BlockFlattener() {
        super();
    }

    public void visitNode(FlatNode node) {
        flattenBlocks(node.contents);
    }

    // ----------------------------------------------------------------------
    // Flatten blocks
    // ----------------------------------------------------------------------

    public void flattenBlocks(SIROperator str) {
        if (str instanceof SIRFeedbackLoop)
            {
                SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
                flattenBlocks(fl.getBody());
                flattenBlocks(fl.getLoop());
            }
        if (str instanceof SIRPipeline)
            {
                SIRPipeline pl = (SIRPipeline)str;
                Iterator iter = pl.getChildren().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = (SIRStream)iter.next();
                        flattenBlocks(child);
                    }
            }
        if (str instanceof SIRSplitJoin)
            {
                SIRSplitJoin sj = (SIRSplitJoin)str;
                Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = iter.next();
                        flattenBlocks(child);
                    }
            }
        if (str instanceof SIRFilter || str instanceof SIRPhasedFilter)
            for (int i = 0; i < ((SIRStream)str).getMethods().length; i++) {
                ((SIRStream)str).getMethods()[i].accept(this);
            }
    }

    public Object visitBlockStatement(JBlock self,
                                      JavaStyleComment[] comments) {
        int size=self.size();
        for(int i=0;i<size;i++) {
            JStatement statement=self.getStatement(i);
            if(statement instanceof JBlock) {
                visitBlockStatement((JBlock)statement,comments);
                self.removeStatement(i);
                self.addAllStatements(i,((JBlock)statement).getStatements());
                size+=((JBlock)statement).size()-1;
            } else
                statement.accept(this);
        }
        return self;
    }

    /**
     * prints a method declaration
     */
    public Object visitMethodDeclaration(JMethodDeclaration self,
                                         int modifiers,
                                         CType returnType,
                                         String ident,
                                         JFormalParameter[] parameters,
                                         CClassType[] exceptions,
                                         JBlock body) {
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
            }
        }
        if (body != null) {
            body.accept(this);
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
        body.accept(this);
        return self;
    }

    /**
     * prints a if statement
     */
    public Object visitIfStatement(JIfStatement self,
                                   JExpression cond,
                                   JStatement thenClause,
                                   JStatement elseClause) {
        thenClause.accept(this);
        if(elseClause!=null)
            elseClause.accept(this);
        return self;
    }
}

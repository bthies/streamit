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
import at.dms.kjc.raw.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;


/**
 * This class flattens blocks which makes it easier for some optimizations
 * Necessary for BranchAnalyzer
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
		Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext())
		{
		    SIRStream child = (SIRStream)iter.next();
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

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
 * This class flattens blocks which makes it easier for some optimizations
 * Necessary for BranchAnalyzer
 */
public class BlockFlattener extends SLIRReplacingVisitor {
    public BlockFlattener() {
	super();
    }

    // ----------------------------------------------------------------------
    // Flatten blocks
    // ----------------------------------------------------------------------

    public void flattenBlocks(SIRStream str) {
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
	if (str instanceof SIRFilter)
	    for (int i = 0; i < str.getMethods().length; i++) {
		str.getMethods()[i].accept(this);
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
	    }
	}
	return self;
    }
}

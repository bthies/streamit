package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;

/**
 * This class inputs a stream representation where child/parent
 * relationships and initialization arguments are given via
 * SIRInitStatements.  It converts this tree into one without any
 * SIRInitStatements, but instead with the children and arguments
 * contained within the SIRContainer classes.  If the input already
 * has some children added to pipelines (e.g. to facilitate field
 * prop.) then these children are cleared before reconstructing them.
 */
public class ConstructSIRTree {

    public static void doit(SIRStream str) {
	// visit the hoister in all containers
	IterFactory.createIter(str).accept(new EmptyStreamVisitor() {
		/* pre-visit a pipeline */
		public void preVisitPipeline(SIRPipeline self,
					     SIRPipelineIter iter) {
		    self.clear();
		    self.getInit().accept(new InitializationHoister(self));
		}
		
		/* pre-visit a splitjoin */
		public void preVisitSplitJoin(SIRSplitJoin self,
					      SIRSplitJoinIter iter) {
		    self.clear();
		    self.getInit().accept(new InitializationHoister(self));
		}
		
		/* pre-visit a feedbackloop */
		public void preVisitFeedbackLoop(SIRFeedbackLoop self,
						 SIRFeedbackLoopIter iter) {
		    self.clear();
		    self.getInit().accept(new InitializationHoister(self));
		    // if we don't have enough children, add null loop
		    if (self.size()<2) {
			self.add(null);
		    }
		}
	    });
    }
}

class InitializationHoister extends SLIRReplacingVisitor {
    /**
     * The immediate parent.
     */
    private SIRContainer parent;

    /**
     * Make a hoister with parent <parent>.
     */
    public InitializationHoister(SIRContainer parent) {
	this.parent = parent;
    }

    public Object visitInitStatement(SIRInitStatement oldSelf,
				     SIRStream oldTarget) {
	// visit children
	SIRInitStatement self = 
	    (SIRInitStatement)
	    super.visitInitStatement(oldSelf, oldTarget);
				
	// add <child, params> to parent
	parent.add(self.getTarget(), self.getArgs());

	// Since all of the args seem to be constant for now (and I
	// think the RAW backend assumes it), this is a nice place to
	// check it
	for (int i=0; i<self.getArgs().size(); i++) {
	    Utils.assert((self.getArgs().get(i) instanceof JLiteral)||(self.getArgs().get(i) instanceof JLocalVariableExpression),
			 "Expected constant arguments to init, but found non-constant " +
			 self.getArgs().get(i) + " in parent " + parent + "\n");
	}
	
	// return an empty statement to eliminate the init
	// statement
	return new JEmptyStatement(null, null);
    }
}


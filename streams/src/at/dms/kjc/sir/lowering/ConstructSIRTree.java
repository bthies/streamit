package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.util.*;
import at.dms.kjc.*;
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
	str.accept(new EmptyStreamVisitor() {
		/* pre-visit a pipeline */
		public void preVisitPipeline(SIRPipeline self,
					     SIRStream parent,
					     JFieldDeclaration[] fields,
					     JMethodDeclaration[] methods,
					     JMethodDeclaration init) {
		    self.clear();
		    init.accept(new InitializationHoister(self));
		}
		
		/* pre-visit a splitjoin */
		public void preVisitSplitJoin(SIRSplitJoin self,
					      SIRStream parent,
					      JFieldDeclaration[] fields,
					      JMethodDeclaration[] methods,
					      JMethodDeclaration init) {
		    self.clear();
		    init.accept(new InitializationHoister(self));
		}
		
		/* pre-visit a feedbackloop */
		public void preVisitFeedbackLoop(SIRFeedbackLoop self,
						 SIRStream parent,
						 JFieldDeclaration[] fields,
						 JMethodDeclaration[] methods,
						 JMethodDeclaration init,
						 int delay,
						 JMethodDeclaration initPath) {
		    self.clear();
		    init.accept(new InitializationHoister(self));
		}
	    });
    }
}

class InitializationHoister extends SLIRReplacingVisitor {
    /**
     * Stack of parents we've descended through.  Modified from
     * outside this visitor.
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
	    Utils.assert(self.getArgs().get(i) instanceof JLiteral,
			 "Expected constant arguments to init, but found non-constant:\n" +
			 self.getArgs().get(i) + "\n");
	}
	
	// return an empty statement to eliminate the init
	// statement
	return new JEmptyStatement(null, null);
    }
}


package at.dms.kjc.sir.lowering.fusion;

import streamit.scheduler.*;
import streamit.scheduler.simple.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

public class Lifter {

    /**
     * Given that <pipe> is a pipeline containing only a single
     * filter, eliminate the pipeline.  For now, don't call this on
     * the outer-most pipeline--<pipe> must have a non-null parent.
     */
    public static void eliminatePipe(final SIRPipeline pipe) {
	// assert the clauses
	Utils.assert(pipe.size()==1 && 
		     pipe.get(0) instanceof SIRFilter &&
		     pipe.getParent()!=null);
	// find the filter of interest
	final SIRFilter filter = (SIRFilter)pipe.get(0);

	// rename the contents of <filter>
	new RenameAll().renameFilterContents(filter);

	// replace call from <pipe's> init to <filter> with a method call
        pipe.getInit().accept(new SLIRReplacingVisitor() {
                public Object visitInitStatement(SIRInitStatement oldSelf,
                                                 JExpression[] oldArgs,
                                                 SIRStream oldTarget)
                {
		    // do the super
		    SIRInitStatement self = 
			(SIRInitStatement)
			super.visitInitStatement(oldSelf, oldArgs, oldTarget);

		    // change the sir init statement into a call to
		    // the init function
		    if (self.getTarget()==filter) {
			return new JExpressionStatement(null,
							new JMethodCallExpression(null, 
						     new JThisExpression(null),
						     self.getTarget().getInit().getName(),
									      self.getArgs()),
							null);
		    } else {
			return self;
		    }
		}
	    });

	// add all the methods and fields of <pipe> to <filter>
	filter.addFields(pipe.getFields());
	filter.addMethods(pipe.getMethods());
	filter.setInitWithoutReplacement(pipe.getInit());

	// set <pipe>'s old init function as <filter>'s init function

	// in parent, replace <pipe> with <filter>
	pipe.getParent().replace(pipe, filter);

	// in parent, replace initialization of <pipe> with initialization of <filter>
	pipe.getParent().getInit().accept(new SLIRReplacingVisitor() {
		public Object visitInitStatement(SIRInitStatement oldSelf,
						 JExpression[] oldArgs,
						 SIRStream oldTarget) {
		    // do the super
		    SIRInitStatement self = 
			(SIRInitStatement)
			super.visitInitStatement(oldSelf, oldArgs, oldTarget);
		    
		    // if we're the target pipe, change target to be
		    // <filter>.
		    if (self.getTarget()==pipe) {
			self.setTarget(filter);
			return self;
		    } else {
			// otherwise, return self
			return self;
		    }
		}
	    });
    }
}

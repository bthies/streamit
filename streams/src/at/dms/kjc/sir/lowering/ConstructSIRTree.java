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
    /**
     * If we find a non-constant argument while building the SIR tree,
     * we will set this exception to the first error that is thrown.
     */
    private static RuntimeException nonConstantArgError = null;

    public static void doit(SIRStream str) {
	// visit the hoister in all containers
	IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
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
	// dump the dot graph of <str>
	SimpleDot.printGraph(str, "first-sir-tree.dot");
	// if we had an exception, throw it
	if (nonConstantArgError!=null) {
	    throw nonConstantArgError;
	}
    }

    static class InitializationHoister extends SLIRReplacingVisitor {
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

	    // Since all of the args seem to be constant for now (and I
	    // think the RAW backend assumes it), this is a nice place to
	    // check it.  Check that we have either literals or array
	    // references.
	    try {
		for (int i=0; i<self.getArgs().size(); i++) {
		    JExpression arg = (JExpression)self.getArgs().get(i);
		    Utils.assert(isConstantArg(arg),
				 "Expected constant arguments to init, but found non-constant " +
				 self.getArgs().get(i) + " in parent " + parent + "\n");
		}
		
		// to simplify compilation, remove constant arguments.
		if (self.getTarget().needsInit()) {
		    removeConstantArgs(self);
		}
	    } catch (RuntimeException e) {
		// don't throw this exception until we're done
		// building the SIR tree, so that we can see what
		// the graph might look like even if there are
		// some non-constant args.
		if (nonConstantArgError==null) {
		    nonConstantArgError = e;
		}
	    }
	    
	    // add <child, params> to parent
	    parent.add(self.getTarget(), self.getArgs());

	    // return an empty statement to eliminate the init
	    // statement
	    return new JEmptyStatement(null, null);
	}

	/**
     * Removes constant args from <self>.
     */
    private void removeConstantArgs(SIRInitStatement self) {
	List args = self.getArgs();
	LinkedList newArgs = new LinkedList();
	JMethodDeclaration init = self.getTarget().getInit();
	final JFormalParameter[] params = init.getParameters();
	JBlock initBlock = init.getBody();

	// go through args and only keep non-constant ones
	for (int i=0; i<args.size(); i++) {
	    final int iter = i;
	    if (!isConstantArg((JExpression)args.get(i))) {
		// keep track of new arg
		newArgs.add((JExpression)args.get(i));
	    } else {
		// for arrays, change parameter into local decl -- in
		// case we were using parameter name to hold the
		// constants that we're propagating in.
		if (params[i].getType().isArrayType()) {
		    final JVariableDefinition def = new JVariableDefinition(null, 0, 
									    params[i].getType(), 
									    params[i].getIdent(),
									    null);
		    // replace all references to <params[i]> with references to <def>
		    initBlock.accept(new SLIRReplacingVisitor() {
			    public Object visitLocalVariableExpression(JLocalVariableExpression myself,
								       String ident) {
				if (myself.getVariable()==params[iter]) {
				    return new JLocalVariableExpression(null, def);
				} else {
				    return myself;
				}
			    }
			});
		    // add definition at front of init block
		    JVariableDefinition defs[] = { def } ;
		    initBlock.addStatementFirst(new JVariableDeclarationStatement(null, defs, null));
		}
	    }
	}

	// set new args in caller
	self.setArgs(newArgs);
	// set new params in callee
	init.setParameters((JFormalParameter[])newArgs.toArray(new JFormalParameter[0]));
    }

    /**
     * Judges whether or not <exp> counts as a constant argument.  For
     * now we count all literals and all array references as constant
     * (array refs should only count as constant if they've been
     * constant-propped, but we haven't bothered with this yet.)
     */
    private boolean isConstantArg (JExpression arg) {
	return (arg instanceof JLiteral) || isArrayArg(arg) || arg.getType().toString().endsWith("Portal");
    }

    private boolean isArrayArg(JExpression arg) {
	return (arg instanceof JLocalVariableExpression &&
		((JLocalVariableExpression)arg).getType().isArrayType());
    }
}

}

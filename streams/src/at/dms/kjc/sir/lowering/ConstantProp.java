package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This class propagates constants and unrolls loops.  Currently only
 * works for init functions.
 */
public class ConstantProp {

    private ConstantProp() {
    }

    /**
     * Propagates constants as far as possible in <str> and also
     * unrolls loops.
     */
    public static void propagateAndUnroll(SIRStream str) {
	// start at the outermost loop with an empty set of constants
	new ConstantProp().propagateAndUnroll(str, new Hashtable());
    }

    /**
     * Does the work on <str>, given that <constants> maps from
     * a JLocalVariable to a JLiteral for all constants that are known.
     */
    private void propagateAndUnroll(SIRStream str, Hashtable constants) {
	Unroller unroller;
	do {
	    // make a propagator
	    Propagator propagator = new Propagator(constants);
	    // propagate constants within init function of <str>
	    str.getInit().accept(propagator);
	    // propagate into fields of <str>
	    propagateFields(propagator, str);
	    // unroll loops within init function of <str>
	    unroller = new Unroller(constants);
	    str.getInit().accept(unroller);
	    // patch list of children for splitjoins and pipelines
	    if (unroller.hasUnrolled()) {
		if (str instanceof SIRPipeline) {
		    ((SIRPipeline)str).setChildren(GetChildren.
						   getChildren(str));
		} else if (str instanceof SIRSplitJoin) {
		    ((SIRSplitJoin)str).setParallelStreams(GetChildren.
							   getChildren(str));
		}
	    }
	    // iterate until nothing unrolls
	} while (unroller.hasUnrolled());
	// recurse into sub-streams
	recurse(str, constants);
    }

    /**
     * Given a propagator <propagator>, this propagates constants
     * through the fields of <str>.
     */
    private void propagateFields(Propagator propagator, SIRStream str) {
	if (str instanceof SIRFilter) {
	    propagateFilterFields(propagator, (SIRFilter)str);
	} if (str instanceof SIRSplitJoin) {
	    // for split-joins, resolve the weights of splitters and
	    // joiners
	    propagator.visitArgs(((SIRSplitJoin)str).
				 getJoiner().getInternalWeights());
	    propagator.visitArgs(((SIRSplitJoin)str).
				 getSplitter().getInternalWeights());
	} else if (str instanceof SIRFeedbackLoop) {
	    // for feedback loops, resolve the weights of splitters
	    // and joiners
	    propagator.visitArgs(((SIRFeedbackLoop)str).
				 getJoiner().getInternalWeights());
	    propagator.visitArgs(((SIRFeedbackLoop)str).
				 getSplitter().getInternalWeights());
	}
    }

    /**
     * Use <propagator> to propagate constants into the fields of <filter>
     */
    private void propagateFilterFields(Propagator propagator, 
				       SIRFilter filter) {
	// propagate to pop expression
	JExpression newPop = (JExpression)filter.getPop().accept(propagator);
	if (newPop!=null && newPop!=filter.getPop()) {
	    filter.setPop(newPop);
	}
	// propagate to peek expression
	JExpression newPeek = (JExpression)filter.getPeek().accept(propagator);

	/*
	System.out.println("inspecting " + filter.getPeek());
	SIRPrinter t;
	filter.getPeek().accept(t = new SIRPrinter());
	t.close();
	System.out.println("found " + newPeek);
	System.out.println("const? " + (newPeek instanceof JLiteral));
	*/

	if (newPeek!=null && newPeek!=filter.getPeek()) {
	    filter.setPeek(newPeek);
	}
	// propagate to push expression
	JExpression newPush = (JExpression)filter.getPush().accept(propagator);
	if (newPush!=null && newPush!=filter.getPush()) {
	    filter.setPush(newPush);
	}
    }

    /**
     * Recurses from <str> into all its substreams.
     */
    private void recurse(SIRStream str, final Hashtable constants) {
	// if we're at the bottom, we're done
	if (str.getInit()==null) {
	    return;
	}
	// iterate through statements of init function, looking for SIRInit's
	str.getInit().accept(new SLIREmptyVisitor() {
		public void visitInitStatement(SIRInitStatement self,
					       JExpression[] args,
					       SIRStream target) {
		    recurse(self, constants);
		}
	    });
    }

    /**
     * Recurses using <init> given that <constants> were built for
     * the parent.
     */
    private void recurse(SIRInitStatement initStatement, Hashtable constants) {
	// get the init function of the target--this is where analysis
	// will continue
	JMethodDeclaration initMethod = initStatement.getTarget().getInit();
	// if there is no init function, we're done
	if (initMethod==null) {
	    return;
	}
	// otherwise, augment the hashtable mapping the parameters of
	// the init function to any constants that appear in the call...
	// get args to init function
	JExpression[] args = initStatement.getArgs();
	// get parameters of init function
	JFormalParameter[] parameters = initMethod.getParameters();
	// build new constants
	for (int i=0; i<args.length; i++) {
	    // if we are passing an arg to the init function...
	    if (args[i] instanceof JLiteral) {
		// if it's already a literal, just record it
		constants.put(parameters[i], args[i]);
	    } else if (constants.get(args[i])!=null) {
		// otherwise if it's associated w/ a literal, then record that
		constants.put(parameters[i], constants.get(args[i]));
	    }
	}
	// recurse into sub-stream
	propagateAndUnroll(initStatement.getTarget(), constants);
    }
}

/**
 * This class is for rebuilding the list of children in a parent
 * stream following unrolling that could have modified the stream
 * structure.
 */
class GetChildren extends SLIREmptyVisitor {
    /**
     * List of children of parent stream.
     */
    private LinkedList children;
    /**
     * The parent stream.
     */
    private SIRStream parent;
        
    /**
     * Makes a new one of these.
     */
    private GetChildren(SIRStream str) {
	this.children = new LinkedList();
	this.parent = str;
    }

    /**
     * Re-inspects the init function of <str> to see who its children
     * are.
     */
    public static LinkedList getChildren(SIRStream str) {
	GetChildren gc = new GetChildren(str);
	if (str.getInit()!=null) {
	    str.getInit().accept(gc);
	}
	return gc.children;
    }

    /**
     * Visits an init statement -- adds <target> to list of children.
     */
    public void visitInitStatement(SIRInitStatement self,
				   JExpression[] args,
				   SIRStream target) {
	// remember <target> as a child
	children.add(target);
	// reset parent of <target> 
	target.setParent((SIRContainer)parent);
    }
}


package at.dms.kjc.sir.lowering.fusion;

import streamit.scheduler.*;
import streamit.scheduler.simple.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

public class Lifter implements StreamVisitor {

    private Lifter() {}

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

	// add a method call to filter's <init> from <pipe's> init
	// function 
	if (pipe.getInit()!=null) {
	    pipe.getInit().addStatement(
		    new JExpressionStatement(null,
			     new JMethodCallExpression(null, 
			     new JThisExpression(null),
			     filter.getInit().getName(),
			     (JExpression[])pipe.getParams(pipe.indexOf(filter)).toArray(new JExpression[0])),
				     null));
	    filter.setInitWithoutReplacement(pipe.getInit());
	}

	// add all the methods and fields of <pipe> to <filter>
	filter.addFields(pipe.getFields());
	filter.addMethods(pipe.getMethods());

	SIRContainer parent = pipe.getParent();
	// in parent, replace <pipe> with <filter>
	parent.replace(pipe, filter);

	// set new arguments to <filter>
	parent.setParams(parent.indexOf(filter), 
			 pipe.getParams(pipe.indexOf(filter)));
    }

    /**
     * Lift everything we can in <str> and its children
     */
    public static void lift(SIRStream str) {
	IterFactory.createIter(str).accept(new Lifter());
    }

    /**
     * PLAIN-VISITS 
     */
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	liftChildren(self);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	liftChildren(self);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	liftChildren(self);
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
	liftChildren(self);
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
	liftChildren(self);
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
	liftChildren(self);
    }

    private void liftChildren(SIRContainer str) {
	for (ListIterator it = str.getChildren().listIterator(); it.hasNext(); ) {
	    SIROperator child = (SIROperator)it.next();
	    if (child instanceof SIRPipeline) {
		SIRPipeline pipe = (SIRPipeline)child;
		if (pipe.size()==1 && pipe.get(0) instanceof SIRFilter && pipe.getParent()!=null) {
		    eliminatePipe(pipe);
		}
	    }
	}
    }
}

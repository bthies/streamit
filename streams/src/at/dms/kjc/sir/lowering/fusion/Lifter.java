package at.dms.kjc.sir.lowering.fusion;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

public class Lifter implements StreamVisitor {
    /**
     * Constants for how much sync. to remove.
     */
    private static final int SYNC_REMOVAL_NONE = 0;
    private static final int SYNC_REMOVAL_NO_NEW_JOINERS = 1;
    private static final int SYNC_REMOVAL_MAX_STRUCTURED = 2;
    /**
     * How much sync we're removing.
     */
    private int syncRemoval;

    private Lifter(int syncRemoval) {
	this.syncRemoval = syncRemoval;
    }

    /**
     * Lift everything we can in <str> and its children.  Do sync
     * removal, but not any kind that will introduce new joiners into
     * the graph.
     */
    public static void lift(SIRStream str) {
	IterFactory.createIter(str).accept(new Lifter(SYNC_REMOVAL_NO_NEW_JOINERS));
    }

    /**
     * Lift everything we can, but don't eliminate matching sync
     * points.
     */
    public static void liftPreservingSync(SIRStream str) {
	IterFactory.createIter(str).accept(new Lifter(SYNC_REMOVAL_NONE));
    }

    /**
     * Lift everything we can, using aggressive sync removal that
     * could possibly add joiners to the graph.
     */
    public static void liftAggressiveSync(SIRStream str) {
	IterFactory.createIter(str).accept(new Lifter(SYNC_REMOVAL_MAX_STRUCTURED));
    }

    /**
     * Removes all non-essential identity filters from <str>.  (At
     * least, removes the ones that would be inserted by partitioning.)
     */
    public static void eliminateIdentities(SIRStream str) {
	IterFactory.createIter(str).accept(new EmptyStreamVisitor() {
		public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {
		    for (int i=self.size()-1; i>=0; i--) {
			if (self.get(i) instanceof SIRIdentity) {
			    // don't completely wipe out the pipeline,
			    // since it might be in a splitjoin or
			    // something.  This could be more
			    // sophisticated to figure out exactly
			    // when it should remove something, but
			    // for now is good enough to remove what's
			    // inserted by partitioning.
			    if (self.size()>1) {
				self.remove(i);
			    }
			}
		    }
		}});
    }

    /**
     * If <str> is a pipeline that either:
     *  1) Has a pipeline parent, or
     *  2) Contains only a single stream
     *
     * then eliminate the pipeline (adjusting pipeline's parent
     * accordingly).  Returns whether or not the lifting was done.
     */
    public static boolean eliminatePipe(final SIRPipeline str) {
	// get parent
	SIRContainer parent = str.getParent();
	Utils.assert(parent==null || parent.contains(str), "Inconsistency in IR:  this stream has a parent which doesn't contain it:\n  stream: " + str + "\n  parent: " + parent);

	// consider only if we have have a parent that's a pipe or if
	// we only have a single child
	if (parent!=null && (str.size()==1 || parent instanceof SIRPipeline)) {

	    // this assumes that we're not worrying about fields and
	    // methods in containers -- otherwise we need renaming and
	    // better handling of possible init function arguments?
	    Utils.assert(str.getFields()==null || str.getFields().length==0,
			 "Not expecting to find fields in container in Lifter.");
	    if (!(str.getMethods()==null || str.getMethods().length==0 ||
		  (str.getMethods().length==1 && str.getMethods()[0]==str.getInit()))) {
		System.err.println("Not expecting to find methods in container in Lifter, but found " + 
				   str.getMethods().length + " in " + str.getName() + " when str.getInit() = " + str.getInit() + " [" + 
				   str.getInit().hashCode() + "]");
		for (int i=0; i<str.getMethods().length; i++) {
		    System.err.println(str.getMethods()[i] + " [" + str.getMethods()[i].hashCode() + "]");
		}
		Utils.fail("Aborting.");
	    }
	    
	    int index = parent.indexOf(str);
	    for (int i=0; i<str.size(); i++) {
		parent.add(index+1+i, str.get(i), str.getParams(i));
		Utils.assert(str.get(i).getParent()==parent);
	    }

	    parent.remove(index);
	    return true;

	} else {
	    return false;
	}
    }

    /**
     * If <str> is a splitjoin that contains only one child, then
     * eliminate it (adjusting splitjoin's parent appropriately.)
     * Returns whether or not the lifting was done.
     */
    public static boolean eliminateSJ(final SIRSplitJoin str) {
	// get parent
	SIRContainer parent = str.getParent();

	if (parent!=null && str.size()==1) {
	    // this assumes that we're not worrying about fields and
	    // methods in containers -- otherwise we need renaming and
	    // better handling of possible init function arguments?
	    Utils.assert(str.getFields()==null || str.getFields().length==0,
			 "Not expecting to find fields in container in Lifter.");
	    Utils.assert(str.getMethods()==null || str.getMethods().length==0 ||
			 (str.getMethods().length==1 && str.getMethods()[0]==str.getInit()),
			 "Not expecting to find methods in container in Lifter, but found " + str.getMethods().length + " in " + str.getName() + ":\n" 
			 + str.getMethods()[0]);
	    
	    parent.replace(str, str.get(0));
	    Utils.assert(str.get(0).getParent()==parent);
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * PLAIN-VISITS 
     */
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
    }

    /* visit a phased filter */
    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	liftChildren(self);
	if (syncRemoval==SYNC_REMOVAL_NO_NEW_JOINERS) {
	    RefactorSplitJoin.removeMatchingSyncPoints(self);
	} else if (syncRemoval==SYNC_REMOVAL_MAX_STRUCTURED) {
	    RefactorSplitJoin.removeSyncPoints(self);
	}
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	visitSplitJoin(self, iter);
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
	if (syncRemoval==SYNC_REMOVAL_NO_NEW_JOINERS) {
	    RefactorSplitJoin.removeMatchingSyncPoints(self);
	} else if (syncRemoval==SYNC_REMOVAL_MAX_STRUCTURED) {
	    RefactorSplitJoin.removeSyncPoints(self);
	}
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
	// have to visit on way up, because pipeline child might have
	// done a sync during descent.
	visitSplitJoin(self, iter);
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
    }

    /**
     * Fundamental lifting op for splitjoins.
     */
    private void visitSplitJoin(SIRSplitJoin self,
				SIRSplitJoinIter iter) {
	boolean changing;
	do {
	    changing = false;
	    changing = liftChildren(self) || changing;
	    changing = liftIntoSplitJoin(self) || changing;
	} while (changing);
    }

    /**
     * Does various types of lifting on <str>.  Returns whether or not
     * anything changed.  Should be iterated for <str> until nothing
     * changes.
     */
    private boolean liftChildren(SIRContainer str) {
	boolean changed = false;
	changed = changed || liftPipelineChildren(str);
	//changed = changed || simplifyTwoStageChildren(str);
	return changed;
    }

    /**
     * Returns whether or not something changed.
     */
    private boolean liftPipelineChildren(SIRContainer str) {
	boolean anyChange = false;
	boolean changing;
	do {
	    changing = false;
	    for (int i=0; i<str.size(); i++) {
		SIROperator child = (SIROperator)str.get(i);
		if (child instanceof SIRPipeline) {
		    changing = eliminatePipe((SIRPipeline)child) || changing;
		} else if (child instanceof SIRSplitJoin) {
		    changing = eliminateSJ((SIRSplitJoin)child) || changing;
		}
	    }
	    anyChange = anyChange || changing;
	} while (changing);
	return anyChange;
    }

    /**
     * If any children of <str> are two stage filters with
     * initPop==initPush==0, then replace them with a single phase
     * filter, appending the contents of initWork to init in the
     * simplified filter.
     */
    private boolean simplifyTwoStageChildren(SIRContainer str) {
	boolean changed = false;
	for (int i=0; i<str.size(); i++) {
	    if (str.get(i) instanceof SIRTwoStageFilter) {
		SIRTwoStageFilter twoStage = (SIRTwoStageFilter)str.get(i);
		if (twoStage.getInitPush()==0 &&
		    twoStage.getInitPop()==0) {
		    // right now we don't support the case where
		    // there's a peek expression without pushing or
		    // popping
		    final boolean[] ok = { true };
		    twoStage.getInitWork().getBody().accept(new SLIREmptyVisitor() {
			    public void visitPeekExpression(SIRPeekExpression self,
							    CType tapeType,
							    JExpression arg) {
				ok[0] = false;
			    }
			});
		    Utils.assert(ok[0], 
				 "\nDetected peek expression from an initWork function that doesn't" +
				 "\npop anything.  This isn't supported right now because the Raw backend" + 
				 "\ndoesn't fire the node even if it is scheduled for execution (as of 2/7/03).");
		    // then we're going to change it
		    changed = true;
		    // construct replacement
		    SIRFilter filter = new SIRFilter(twoStage.getParent(),
						     twoStage.getIdent() + "_simp",
						     twoStage.getFields(),
						     twoStage.getMethods(),
						     twoStage.getPeek(),
						     twoStage.getPop(),
						     twoStage.getPush(),
						     twoStage.getWork(),
						     twoStage.getInputType(),
						     twoStage.getOutputType());
		    // append contents of <twoStage> initWork to filter work
		    JMethodDeclaration newInit = twoStage.getInit();
		    JBlock initWorkContents = twoStage.getInitWork().getBody();
		    newInit.getBody().addStatement(initWorkContents);
		    filter.setInit(newInit);
		    str.set(i, filter);
		}
	    }
	}
	return changed;
    }

    /**
     * Returns whether or not something changed.
     */
    private boolean liftIntoSplitJoin(SIRSplitJoin sj) {
	boolean anyChange = false;
	boolean changing;
	do {
	    changing = RefactorSplitJoin.raiseSJChildren(sj);
	    anyChange = anyChange || changing;
	} while (changing);
	return anyChange;
    }
}

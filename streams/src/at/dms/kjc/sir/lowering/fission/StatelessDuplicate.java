package at.dms.kjc.sir.lowering.fission;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import java.util.*;

/**
 * This class splits a stateless filter with arbitrary push/pop/peek
 * ratios into a duplicate/round-robin split-join of a user-specified
 * width.
 */
public class StatelessDuplicate {
    /**
     * The filter we're duplicating.
     */
    private SIRFilter origFilter;
    
    /**
     * The number of repetitions to duplicate
     */
    private int reps;

    /**
     * The list of resulting filters
     */
    private LinkedList newFilters;
    
    private StatelessDuplicate(SIRFilter origFilter, int reps) {
	this.origFilter = origFilter;
	this.reps = reps;
	this.newFilters = new LinkedList();
    }

    /**
     * Duplicates <filter> into a <reps>-way SplitJoin and replaces
     * the filter with the new construct in the parent.
     */
    public static void doit(SIRFilter origFilter, int reps) {
	if (isFissable(origFilter)) {
	    new StatelessDuplicate(origFilter, reps).doit();
	} else {
	    Utils.fail("Trying to split an un-fissable filter: " + origFilter);
	}
    }

    /**
     * We don't yet support fission of two-stage filters that peek.
     */
    public static boolean isFissable(SIRFilter filter) {
	if (filter instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter twoStage = (SIRTwoStageFilter)filter;
	    if (twoStage.getInitPop()>0) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Carry out the duplication on this instance.
     */
    private void doit() {
	// make new filters
	makeDuplicates();

	// make result
	SIRSplitJoin result 
	    = new SIRSplitJoin(origFilter.getParent(),
			       origFilter.getName() + "_SplitJoin",
			       new JFieldDeclaration[0], /* fields */
			       new JMethodDeclaration[0] /* methods */);

	// make an init function
	JMethodDeclaration init = makeSJInit(result);

	// create the splitter
	/*
	if (origFilter.getPeekInt()==origFilter.getPopInt()) {
	    // without peeking, it's a round-robin 
	    JExpression[] splitWeight = { new JIntLiteral(origFilter.getPopInt()) } ;
	    result.setSplitter(SIRSplitter.
			       createUniformRR(result, splitWeight));
	} else {
	*/
	    // with peeking, it's just a duplicate splitter
	    result.setSplitter(SIRSplitter.
			       create(result, SIRSplitType.DUPLICATE, reps));
	    //	}

	// create the joiner
	int pushCount = origFilter.getPushInt();
	if (pushCount > 0) {
	    JExpression[] joinWeight = { new JIntLiteral(pushCount) } ;
	    result.setJoiner(SIRJoiner.
			     createUniformRR(result, joinWeight));
	} else {
	    result.setJoiner(SIRJoiner.create(result,
					      SIRJoinType.NULL, 
					      reps));
	}
	// set the init function
	result.setInit(init);
	origFilter.getParent().replace(origFilter, result);
    }

    /**
     * Returns an init function for the containing splitjoin.
     */
    private JMethodDeclaration makeSJInit(SIRSplitJoin sj) {
	// start by cloning the original init function, so we can get
	// the signature right
	JMethodDeclaration result = (JMethodDeclaration)
	    ObjectDeepCloner.deepCopy(origFilter.getInit());
	// get the parameters
	JFormalParameter[] params = result.getParameters();
	// now build up the body as a series of calls to the sub-streams
	LinkedList bodyList = new LinkedList();
	for (ListIterator it = newFilters.listIterator(); it.hasNext(); ) {
	    // build up the argument list
	    LinkedList args = new LinkedList();
	    for (int i=0; i<params.length; i++) {
		args.add(new JLocalVariableExpression(null, params[i]));
	    }
	    // add the child and the argument to the parent
	    sj.add((SIRStream)it.next(), args);
	}
	// replace the body of the init function with statement list
	// we just made
	result.setBody(new JBlock(null, bodyList, null));
	// return result
	return result;
    }

    /**
     * Fills <newFilters> with the duplicate filters for the result.
     */
    private void makeDuplicates() {
	// make the first one just a copy of the original, since
	// otherwise we have a stage in the two-stage filter that
	// doesn't consume anything at all
	//newFilters.add(ObjectDeepCloner.deepCopy(origFilter));
	for (int i=0; i<reps; i++) {
	    newFilters.add(makeDuplicate(i));
	    System.err.println("making a duplicate of " + origFilter + " " + origFilter.getName());
	}
    }

    /**
     * Makes the <i>'th duplicate filter in this.
     */
    private SIRFilter makeDuplicate(int i) {
	// start by cloning the original filter, and copying the state
	// into a new two-stage filter.
	SIRFilter cloned = (SIRFilter)ObjectDeepCloner.deepCopy(origFilter);
	// if there is no peeking, then we can returned <cloned>.
	// Otherwise, we need a two-stage filter.
	/*
	if (origFilter.getPeekInt()==origFilter.getPopInt()) {
	    return cloned;
	} else {
	*/
	    SIRTwoStageFilter result = new SIRTwoStageFilter();
	    result.copyState(cloned);
	    // make the work function
	    makeDuplicateWork(result);
	    // set I/O rates
	    setRates(result, i);
	    // make the initial work function for the two-staged filter
	    makeDuplicateInitWork(result, i);
	    // return result
	    return result;
	    //	}
    }

    private void setRates(SIRTwoStageFilter filter, int i) { 
	// set the peek, pop, and push ratios...

	int origPop = filter.getPopInt();
	int extraPopCount = (reps-1)*origPop;

	// the push amount stays the same
	// change the peek and pop amount
	filter.setPeek(Math.max(filter.getPeekInt(), extraPopCount+filter.getPopInt()));
	filter.setPop(extraPopCount+filter.getPopInt());

	// we push zero, since we're just clearing the input line
	filter.setInitPush(0);
	// we pop <i> * the original pop amount
	filter.setInitPop(i*origPop);
	// we peek the original peek amount (plus the pop)
	filter.setInitPeek(filter.getInitPop()+filter.getPeekInt()-filter.getPopInt());
    }

    /**
     * Install an initWork function, along with init pop/push/peek
     * ratios, for filter <filter> that is the <i>'th to be
     * duplicated.
     */
    private void makeDuplicateInitWork(SIRTwoStageFilter filter, int i) {
	// the number of items to pop
	int count = i*origFilter.getPopInt();
	// the body of init work
	JStatement[] body = { makePopLoop(count) };
	// set init work
	filter.setInitWork(new JMethodDeclaration(null,
				     at.dms.kjc.Constants.ACC_PUBLIC,
				     CStdType.Void,
				     FusePipe.INIT_WORK_NAME, 
				     JFormalParameter.EMPTY,
				     CClassType.EMPTY,
				     new JBlock(null, body, null),
				     null,
				     null));
    }

    /**
     * Returns a loop that pops <i> items.
     */
    private JStatement makePopLoop(int i) {
	JStatement[] popStatement = 
	    { new JExpressionStatement(null, new SIRPopExpression(), null) } ;
	// wrap it in a block and a for loop
	JBlock popBlock = new JBlock(null, popStatement, null);
	return Utils.makeForLoop(popBlock, i);
    }

    /**
     * Mutate the steady-state work function of <filter> so that it is
     * appropriate for inclusion in the <reps>-way splitjoin.  This
     * involves simply popping an extra (reps-1)*POP at the end.
     */
    private void makeDuplicateWork(SIRTwoStageFilter filter) {
	int extraPopCount = (reps-1)*filter.getPopInt();
	// append the popLoop to the statements in the work function
	filter.getWork().getBody().addStatement(makePopLoop(extraPopCount));
    }
}

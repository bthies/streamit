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
	new StatelessDuplicate(origFilter, reps).doit();
    }

    /**
     * Carry out the duplication on this instance.
     */
    private void doit() {
	// make new filters
	makeDuplicates();

	// make an init function
	JMethodDeclaration init = makeSJInit();

	// make result
	SIRSplitJoin result 
	    = new SIRSplitJoin(origFilter.getParent(),
			       origFilter.getName() + "_SplitJoin",
			       new JFieldDeclaration[0], /* fields */
			       new JMethodDeclaration[0] /* methods */);
	// set the splitter, joiner
	result.setSplitter(SIRSplitter.
			   create(result, SIRSplitType.DUPLICATE, reps));
	JExpression[] joinWeight = { new JIntLiteral(1) } ;
	result.setJoiner(SIRJoiner.
			 createUniformRR(result, joinWeight));
	// set the init function
	result.setInit(init);
	// set the child filters
	result.setParallelStreams(newFilters);
	// replace original in parent
	replace(origFilter.getParent(), result, origFilter);
    }

    /**
     * Returns an init function for the containing splitjoin.
     */
    private JMethodDeclaration makeSJInit() {
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
	    JExpression[] args = new JExpression[params.length];
	    for (int i=0; i<args.length; i++) {
		args[i] = new JLocalVariableExpression(null, params[i]);
	    }
	    // make an init statement for the child
	    bodyList.add(new SIRInitStatement(null, null, 
					      args, (SIRStream)it.next()));
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
	newFilters.add(ObjectDeepCloner.deepCopy(origFilter));
	for (int i=1; i<reps; i++) {
	    newFilters.add(makeDuplicate(i));
	}
    }

    /**
     * Makes the <i>'th duplicate filter in this.
     */
    private SIRFilter makeDuplicate(int i) {
	// start by cloning the original filter, and copying the state
	// into a new two-stage filter.
	SIRFilter cloned = (SIRFilter)ObjectDeepCloner.deepCopy(origFilter);
	SIRTwoStageFilter result = new SIRTwoStageFilter();
	result.copyState(cloned);
	// make the initial work function for the two-staged filter
	makeDuplicateInitWork(result, i);
	// make the work function
	makeDuplicateWork(result);
	// return result
	return result;
    }

    /**
     * Install an initWork function, along with init pop/push/peek
     * ratios, for filter <filter> that is the <i>'th to be
     * duplicated.
     */
    private void makeDuplicateInitWork(SIRTwoStageFilter filter, int i) {
	// set the peek, pop, and push ratios...

	// we push zero, since we're just clearing the input line
	filter.setInitPush(0);
	// we pop <i> * the original pop amount
	filter.setInitPop(i*filter.getPopInt());
	// we peek the original peek amount (plus the pop)
	filter.setInitPeek((i-1)*filter.getPopInt()+filter.getPeekInt());

	// to build <initWork>, first clone the statements in the work
	// function of <filter>
	JBlock initBlock = 
	    (JBlock)ObjectDeepCloner.deepCopy(filter.getWork().getBody());
	// now extract the statements and replace the push statements
	// with empty statements; the purpose of the initWork is just
	// to pop the elements that the predecessor has processed.
	initBlock.accept(new SLIRReplacingVisitor() {
		public Object 
		    visitExpressionStatement(JExpressionStatement self,
					     JExpression expr) {
		    if (expr instanceof SIRPushExpression) {
			return new JEmptyStatement(null, null);
		    } else {
			return super.visitExpressionStatement(self, expr);
		    }
		}});
	// now wrap the block in a for loop that goes to <i>
	JStatement[] loopContents = 
	    { Utils.makeForLoop(initBlock, i) } ;
	// make an init function with <loopedBlock> as the body
	filter.setInitWork(new JMethodDeclaration(null,
				     at.dms.kjc.Constants.ACC_PUBLIC,
				     CStdType.Void,
				     FusePipe.INIT_WORK_NAME, 
				     JFormalParameter.EMPTY,
				     CClassType.EMPTY,
				     new JBlock(null, loopContents, null),
				     null,
				     null));
    }

    /**
     * Mutate the steady-state work function of <filter> so that it is
     * appropriate for inclusion in the <reps>-way splitjoin.  This
     * involves simply popping an extra (reps-1)*POP at the end.
     */
    private void makeDuplicateWork(SIRTwoStageFilter filter) {
	// make a pop statement
	JStatement[] popStatement = 
	    { new JExpressionStatement(null, new SIRPopExpression(), null) } ;
	// wrap it in a block and a for loop
	JBlock popBlock = new JBlock(null, popStatement, null);
	JStatement popLoop = 
	    Utils.makeForLoop(popBlock, (reps-1)*filter.getPopInt());
	// append the popLoop to the statements in the work function
	filter.getWork().getBody().addStatement(popLoop);
    }

    /**
     * In <parent>, replace <origFilter> with <result>
     */
    private static void replace(SIRContainer parent, 
				final SIRSplitJoin result,
				final SIRFilter origFilter) {
	// replace <filterList> with <fused>
	parent.replace(origFilter, result);

	// replace the SIRInitStatement in the parent
	parent.getInit().accept(new SLIRReplacingVisitor() {
		public Object visitInitStatement(SIRInitStatement oldSelf,
						 JExpression[] oldArgs,
						 SIRStream oldTarget) {
		    // do the super
		    SIRInitStatement self = 
			(SIRInitStatement)
			super.visitInitStatement(oldSelf, oldArgs, oldTarget);
		    
		    if (self.getTarget()==origFilter) {
			self.setTarget(result);
		    } 
		    return self;
		}
	    });
    }


}

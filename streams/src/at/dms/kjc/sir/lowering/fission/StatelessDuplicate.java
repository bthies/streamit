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
     * Toggle that indicates whether a round-robing splitter should be
     * used in the resulting splitjoin if pop==peek for the filter
     * we're fissing.  This seems like it would be a good idea, but it
     * turns out to usually be faster to duplicate all data and
     * decimate at the nodes.
     */
    private static final boolean USE_ROUNDROBIN_SPLITTER = false;
    
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
    public static SIRSplitJoin doit(SIRFilter origFilter, int reps) {
	if (isFissable(origFilter)) {
	    return new StatelessDuplicate(origFilter, reps).doit();
	} else {
	    Utils.fail("Trying to split an un-fissable filter: " + origFilter);
	    return null;
	}
    }

    /**
     * Checks whether or not <filter> can be fissed.
     */
    public static boolean isFissable(SIRFilter filter) {
	//do not fiss sinks
	if (filter.getPushInt() == 0)
	    return false;

	// check that it is stateless
	if (!isStateless(filter)) {
	    return false;
	}
	
	//Hack to prevent fissing file writers
	if(filter.getIdent().startsWith("FileWriter"))
	    return false;

	//Don't fiss identities
	if(filter instanceof SIRIdentity)
	    return false;

	// don't split a filter with a feedbackloop as a parent, just
	// as a precaution, since feedbackloops are hard to schedule
	// when stuff is blowing up in the body
	SIRContainer[] parents = filter.getParents();
	for (int i=0; i<parents.length; i++) {
	    if (parents[i] instanceof SIRFeedbackLoop) {
		return false;
	    }
	}
 	// We don't yet support fission of two-stage filters that peek.
	if (filter instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter twoStage = (SIRTwoStageFilter)filter;
	    if (twoStage.getInitPop()>0) {
		return false;
	    }
	}

	//This seems to break fission too
	if(filter.getPopInt()==0)
	    return false;

	return true;
    }

    /**
     * Returns whether or not <filter> is stateless.
     */
    private static boolean isStateless(SIRFilter filter) {
	// for now just do a conservative check -- if it has no
	// fields, then it is stateless.  This actually isn't too bad
	// if fieldprop has removed the field references; otherwise we
	// could use something more robust.
	//if(filter.getFields().length!=0)
	final boolean[] out=new boolean[1];
	out[0]=true;
	JMethodDeclaration[] methods=filter.getMethods();
	JMethodDeclaration init=filter.getInit();
	for(int i=0;i<methods.length;i++)
	    if(methods[i]!=init)
		methods[i].accept(new EmptyAttributeVisitor() {
			public Object visitFieldExpression(JFieldAccessExpression self,
							   JExpression left,
							   String ident) {
			    out[0]=false;
			    return self;
			}
		    });
	//return filter.getFields().length==0;
	return out[0];
    }

    /**
     * Carry out the duplication on this instance.
     */
    private SIRSplitJoin doit() {
	// make new filters
	makeDuplicates();

	// make result
	SIRSplitJoin result = new SIRSplitJoin(origFilter.getParent(), origFilter.getIdent() + "_Fiss");

	// replace in parent
	origFilter.getParent().replace(origFilter, result);

	// make an init function
	JMethodDeclaration init = makeSJInit(result);

	// create the splitter
	if (USE_ROUNDROBIN_SPLITTER && origFilter.getPeekInt()==origFilter.getPopInt()) {
	    // without peeking, it's a round-robin 
	    JExpression splitWeight = new JIntLiteral(origFilter.getPopInt());
	    result.setSplitter(SIRSplitter.
			       createUniformRR(result, splitWeight));
	} else {
	    // with peeking, it's just a duplicate splitter
	    result.setSplitter(SIRSplitter.
			       create(result, SIRSplitType.DUPLICATE, reps));
	}

	// create the joiner
	int pushCount = origFilter.getPushInt();
	if (pushCount > 0) {
	    JExpression joinWeight = new JIntLiteral(pushCount);
	    result.setJoiner(SIRJoiner.
			     createUniformRR(result, joinWeight));
	} else {
	    result.setJoiner(SIRJoiner.create(result,
					      SIRJoinType.NULL, 
					      reps));
	}
	// rescale the joiner to the appropriate width
	result.rescale();
	// set the init function
	result.setInit(init);

	/*
	// propagate constants through the new stream, to resolve
	// arguments to init functions
	SIRContainer toplevel = result.getParent();
	while (toplevel.getParent()!=null) {
	    toplevel = toplevel.getParent();
	}
	ConstantProp.propagateAndUnroll(toplevel);
	*/
	return result;
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
	List params = sj.getParams();
	// now build up the body as a series of calls to the sub-streams
	LinkedList bodyList = new LinkedList();
	for (ListIterator it = newFilters.listIterator(); it.hasNext(); ) {
	    // build up the argument list
 	    LinkedList args = new LinkedList();
	    for (ListIterator pit = params.listIterator(); pit.hasNext(); ) {
		args.add((JExpression)pit.next());
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
	//System.err.println("Duplicating " + origFilter.getName() + " into a " + reps + "-way SplitJoin.");
	for (int i=0; i<reps; i++) {
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
	// if there is no peeking, then we can returned <cloned>.
	// Otherwise, we need a two-stage filter.
	if (USE_ROUNDROBIN_SPLITTER && origFilter.getPeekInt()==origFilter.getPopInt()) {
	    return cloned;
	} else {
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
	}
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

package at.dms.kjc.sir.lowering.fusion;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.lowering.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import streamit.*;
import streamit.scheduler.*;
import streamit.scheduler.simple.*;

/**
 * This flattens certain cases of split/joins into a single filter.
 */
public class FuseSplit {
    /**
     * Performs a depth-first traversal of an SIRStream tree, and
     * calls flatten() on any SIRSplitJoins as a post-pass.
     */
    public static SIRStream doFlatten(SIRStream str)
    {
        // First, visit children (if any).
        if (str instanceof SIRFeedbackLoop)
        {
            SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
            fl.setBody((SIRStream)doFlatten(fl.getBody()));
            fl.setLoop((SIRStream)doFlatten(fl.getLoop()));
        }
        if (str instanceof SIRPipeline)
        {
            SIRPipeline pl = (SIRPipeline)str;
	    for (int i=0; i<pl.size(); i++) {
		// TODO this has some needless recursion since
		// we might mutate pipe in flattening, but okay
		// for now; don't want to make brittle by adding
		// extra increment.
		doFlatten(pl.get(i));
            }
        }
        if (str instanceof SIRSplitJoin)
        {
            SIRSplitJoin sj = (SIRSplitJoin)str;
            // This is painful, but I don't feel like adding code to
            // SIRSplitJoin right now.
            LinkedList ll = new LinkedList();
            Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext())
            {
                SIRStream child = (SIRStream)iter.next();
                ll.add(doFlatten(child));
            }
            // Replace the children of the split/join with the flattened list.
            sj.setParallelStreams(ll);
        }
        
        // Having recursed, do the flattening, if it's appropriate.
        if (str instanceof SIRSplitJoin) {
            str = fuse((SIRSplitJoin)str);
	}
        
        // All done, return the object.
        return str;
    }
    
    /**
     * Flattens a split/join, subject to the following constraints:
     *  1. Each parallel component of the stream is a filter.  Further, it is
     *     not a two-stage filter that peeks.
     *  2. If the splitter is anything other than duplicate, then
     *     there is no peeking from within the parallel streams.
     *  3. The SplitJoin is contained in a pipeline, to ease our mending
     *     of init functions.  This is easy to lift later.
     */
    public static SIRStream fuse(SIRSplitJoin sj)
    {
	// check the parent
	if (!(sj.getParent() instanceof SIRPipeline)) {
	    return sj;
	}
        // Check the ratios.
        Iterator childIter = sj.getParallelStreams().iterator();
        while (childIter.hasNext()) {
            SIRStream str = (SIRStream)childIter.next();
            if (!(str instanceof SIRFilter))
                return sj;
            SIRFilter filter = (SIRFilter)str;
	    // don't allow two-stage filters that peek, since we
	    // aren't dealing with how to fuse their initWork
	    // functions.
            if (filter instanceof SIRTwoStageFilter &&
		((SIRTwoStageFilter)filter).getInitPop() > 0) {
//		filter.getPeekInt() > filter.getPopInt()) {
                return sj;
	    }
        }

	System.err.println("Fusing " + (sj.size()) + " SplitJoin filters!");

        // Rename all of the child streams of this.
        RenameAll ra = new RenameAll();
        List children = sj.getParallelStreams();
        Iterator iter = children.iterator();
        while (iter.hasNext()) {
	    ra.renameFilterContents((SIRFilter)iter.next());
        }
	
	// calculate the repetitions for the split-join
	RepInfo rep = RepInfo.calcReps(sj);

        // So at this point we have an eligible split/join; flatten it.
        JMethodDeclaration newWork = makeWorkFunction(sj, children, rep);
        JFieldDeclaration[] newFields = makeFields(sj, children);
        JMethodDeclaration[] newMethods = makeMethods(sj, children);

        /* Make names of things within the split/join unique.
        ra.findDecls(newFields);
        ra.findDecls(newMethods);
        for (int i = 0; i < newFields.length; i++)
            newFields[i] = (JFieldDeclaration)newFields[i].accept(ra);
        for (int i = 0; i < newMethods.length; i++)
            newMethods[i] = (JMethodDeclaration)newMethods[i].accept(ra);
        newWork = (JMethodDeclaration)newWork.accept(ra);
	*/

        // Get the init function now, using renamed names of things.
        JMethodDeclaration newInit = makeInitFunction(sj, children);

	// calculate the push/pop/peek ratio
	int push = rep.joiner * sj.getJoiner().getSumOfWeights();
	int pop;
	if (sj.getSplitter().getType()==SIRSplitType.DUPLICATE) {
	    pop = rep.splitter;
	} else {
	    pop = rep.splitter * sj.getSplitter().getSumOfWeights();;
	}

	// calculate the peek amount...
	// get the max peeked by a child, in excess of its popping
        children = sj.getParallelStreams();
        iter = children.iterator();
	int maxPeek = 0;
        while (iter.hasNext()) {
	    SIRFilter filter = (SIRFilter)iter.next();
	    maxPeek = Math.max(maxPeek, 
			       filter.getPeekInt()-
			       filter.getPopInt());
	}
	// calculate the peek as the amount we'll look into the input
	// during execution
	int peek = pop+maxPeek;

        // Build the new filter.
        SIRFilter newFilter = new SIRFilter(sj.getParent(),
                                            "Fused_" + sj.getIdent(),
                                            newFields,
                                            newMethods,
                                            new JIntLiteral(peek),
                                            new JIntLiteral(pop),
                                            new JIntLiteral(push),
                                            newWork,
                                            sj.getInputType(),
                                            sj.getOutputType());
        // Use the new init function.
        newFilter.setInit(newInit);

	// make a splitFilter only if it's not a duplicate and it's
	// not a null split
	SIRFilter splitFilter = null;
	if (sj.getSplitter().getType()!=SIRSplitType.DUPLICATE &&
	    rep.splitter!=0) {
	    splitFilter = makeFilter(sj.getParent(),
				     "Pre_" + sj.getIdent(),
				     makeSplitFilterBody(sj.getSplitter(), 
							 rep,
							 sj.getInputType()), 
				     pop, pop, pop,
				     sj.getInputType());
	}
	
	// make a joinFilter only if it's not a not a null join
	SIRFilter joinFilter = null;
	if (rep.joiner!=0) {
	    joinFilter = makeFilter(sj.getParent(),
					  "Post_" + sj.getIdent(),
					  makeJoinFilterBody(sj.getJoiner(), 
							     rep,
							     sj.getOutputType()),
					  push, push, push,
					  sj.getOutputType());
	}

	// add these filters to the parent, which we require is a pipeline for now
	SIRPipeline parent = (SIRPipeline)sj.getParent();

	// get the index where we did the replace
	int index = parent.indexOf(sj);
	parent.replace(sj, newFilter);
	// add <joinFilter> after <newFilter>
	if (joinFilter!=null) {
	    parent.add(index+1, joinFilter);
	}
	// add <splitFilter> before <newFilter>, if it's not a duplicate
	if (splitFilter!=null) {
	    parent.add(index, splitFilter);
	}

	// we also have <newfilter> replacing <sj>
        replaceParentInit(sj, newFilter, splitFilter, joinFilter);

        return newFilter;
    }


    /**
     * Replaces parent init function calls to <oldStr> with calls to
     * <newStr>, adding calls to <preStr> and <postStr> with no args.
     */
    private static void replaceParentInit(final SIRSplitJoin oldStr,
					  final SIRStream newStr,
					  final SIRStream preStr,
					  final SIRStream postStr) {

        SIRStream parent = oldStr.getParent();
	// replace the SIRInitStatements in the parent
	parent.getInit().accept(new SLIRReplacingVisitor() {
		public Object visitInitStatement(SIRInitStatement oldSelf,
						 JExpression[] oldArgs,
						 SIRStream oldTarget) {
		    // do the super
		    SIRInitStatement self = 
			(SIRInitStatement)
			super.visitInitStatement(oldSelf, oldArgs, oldTarget);

		    // if we're not <oldstr>, just return <self>
		    if (self.getTarget()!=oldStr) {
			return self;
		    }

		    // otherwise, start by making target of <self> to <newStr>
		    self.setTarget(newStr);

		    // then make a jblock initializing newStr, preStr, postStr
		    LinkedList statements = new LinkedList();

		    // ignore the preStr if we don't have one
		    if (preStr!=null) {
			statements.add(new SIRInitStatement(null, null, new JExpression[0], preStr));
		    }
		    statements.add(self);
		    // ignore the postStr if we don't have one
		    if (postStr!=null) {
			statements.add(new SIRInitStatement(null, null, new JExpression[0], postStr));
		    }

		    // return a block
		    return new JBlock(null, statements, null);
		}
	    });
    }

    /**
     * Returns a filter with empty init function and work function
     * having parent <parent>, name <ident>, statements <block>, i/o
     * type <type>, and i/o rates <peek>, <pop>, <push> suitable for
     * reorderers that emulate splitters and joiners in the fused
     * construct.
     */
    private static SIRFilter makeFilter(SIRContainer parent,
					String ident,
					JBlock block, 
					int peek, int pop, int push,
					CType type) {

	// make empty init function
	JMethodDeclaration init = new JMethodDeclaration(null,
							 at.dms.kjc.Constants.ACC_PUBLIC,
							 CStdType.Void,
							 "init",
							 JFormalParameter.EMPTY,
							 CClassType.EMPTY,
							 new JBlock(null, new LinkedList(), null),
							 null,
							 null);
	
	// make work function
	JMethodDeclaration work = new JMethodDeclaration(null,
							 at.dms.kjc.Constants.ACC_PUBLIC,
							 CStdType.Void,
							 "work",
							 JFormalParameter.EMPTY,
							 CClassType.EMPTY,
							 block,
							 null,
							 null);

	// make method array
	JMethodDeclaration[] methods = { init, work} ;

	// return new filter
	SIRFilter result = new SIRFilter(parent,
					 ident,
					 JFieldDeclaration.EMPTY(),
					 methods, 
					 new JIntLiteral(peek), 
					 new JIntLiteral(pop), 
					 new JIntLiteral(push), 
					 work, 
					 type,
					 type);
	result.setInit(init);

	return result;
    }

    /**
     * Makes the body of the work function for a reordering filter
     * equivalent to <split>.
     */
    private static JBlock makeSplitFilterBody(SIRSplitter split, 
					      RepInfo rep,
					      CType type) {
	// get splitter weights
	int[] weights = split.getWeights();
	int[] partialSum = new int[weights.length];
	// calculate partial sums of weights
	for (int i=1; i<weights.length; i++) {
	    partialSum[i] = partialSum[i-1] + weights[i-1];
	}
	// get total weights
	int sumOfWeights = split.getSumOfWeights();
	// make list of statements for work function
	LinkedList list = new LinkedList();
	for (int k=0; k<weights.length; k++) {
	    for (int i=0; i<rep.splitter; i++) {
		for (int j=0; j<weights[k]; j++) {
		    // calculate index of this peek
		    int index = i*sumOfWeights + partialSum[k] + j;
		    // make a peek expression
		    JExpression peek = new SIRPeekExpression(new JIntLiteral(index), type);
		    // make a push expression
		    JExpression push = new SIRPushExpression(peek, type);
		    // make an expression statement
		    list.add(new JExpressionStatement(null, push, null));
		}
	    }
	}
	// pop the right number of items at the end
	list.add(Utils.makeForLoop(new JExpressionStatement(null,
						      new SIRPopExpression(type),
						      null),
			     sumOfWeights * rep.splitter));
	return new JBlock(null, list, null);
    }

    /**
     * Makes the body of the work function for a reordering filter
     * equivalent to <join>.
     */
    private static JBlock makeJoinFilterBody(SIRJoiner join, RepInfo rep, CType type) {
	// get splitter weights
	int[] weights = join.getWeights();
	int[] partialSum = new int[weights.length];
	// calculate partial sums of outputs
	for (int i=1; i<weights.length; i++) {
	    partialSum[i] = partialSum[i-1] + rep.joiner * weights[i-1];
	}
	// get total weights
	int sumOfWeights = join.getSumOfWeights();
	// make list of statements for work function
	LinkedList list = new LinkedList();
	for (int k=0; k<rep.joiner; k++) {
	    for (int i=0; i<weights.length; i++) {
		for (int j=0; j<weights[i]; j++) {
		    int index = partialSum[i] + k*weights[i] + j;
		    // make a peek expression
		    JExpression peek = new SIRPeekExpression(new JIntLiteral(index),
							     type);
		    // make a push expression
		    JExpression push = new SIRPushExpression(peek, type);
		    // make an expression statement
		    list.add(new JExpressionStatement(null, push, null));
		}
	    }
	}
	// pop the right number of items at the end
	list.add(Utils.makeForLoop(new JExpressionStatement(null,
						      new SIRPopExpression(type),
						      null),
			     sumOfWeights * rep.joiner));
	return new JBlock(null, list, null);
    }

    private static JMethodDeclaration makeInitFunction(SIRSplitJoin sj,
						       List children) { 
        // Start with the init function from the split/join.
	JMethodDeclaration md = sj.getInit();
	// replace init statements with calls to the init function
        md.accept(new SLIRReplacingVisitor() {
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
		    return new JExpressionStatement(null,
						    new JMethodCallExpression(null, 
						     new JThisExpression(null),
						     self.getTarget().getInit().getName(),
									      self.getArgs()),
						    null);
                }
            });
        return md;
    }

    private static JMethodDeclaration makeWorkFunction(SIRSplitJoin sj,
						       List children,
						       RepInfo rep) {
	// see whether or not we have a duplicate
	boolean isDup = sj.getSplitter().getType()==SIRSplitType.DUPLICATE;
        // Build the new work function; add the list of statements
        // from each of the component filters.
        JBlock newStatements = new JBlock(null, new LinkedList(), null);
        Iterator childIter = children.iterator();
	int i=0; 
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            // Get the statements of the old work function
            JBlock statements = filter.getWork().getBody();
            // Make a for loop that repeats these statements according
	    // to reps
	    JStatement loop = Utils.makeForLoop(statements, rep.child[i]);
	    // if we have a duplicating splitter, we need to make
	    // pops into peeks
	    if (isDup) {
		loop = popToPeek(loop);
	    }
	    // add the loop to the new work function
            newStatements.addStatement(loop);
	    i++;
        }

	// add a pop loop to statements that pops the right number of
	// times for the splitjoin
	if (isDup) {
	    newStatements.
		addStatement(Utils.
			     makeForLoop(new JExpressionStatement(null,
								  new SIRPopExpression(sj.
										       getInputType()),
								  null),
					 rep.splitter));
	}

        // Now make a new work function based on this.
        JMethodDeclaration newWork =
            new JMethodDeclaration(null,
                                   at.dms.kjc.Constants.ACC_PUBLIC,
                                   CStdType.Void,
                                   "work",
                                   JFormalParameter.EMPTY,
                                   CClassType.EMPTY,
                                   newStatements,
                                   null,
                                   null);

        return newWork;
    }

    /**
     * Replace pops in <orig> with peeks to a local counter that keeps
     * track of the current index.  Also adjust peeks accordingly.
     */
    private static JStatement popToPeek(JStatement orig) {
	// define a variable to be our counter of pop position
	final JVariableDefinition var = 
	    new JVariableDefinition(/* where */ null,
				    /* modifiers */ 0,
				    /* type */ CStdType.Integer,
				    /* ident */ 
				    LoweringConstants.getUniqueVarName(),
				    /* initializer */
				    new JIntLiteral(0));
	// make a declaration statement for our new variable
	JVariableDeclarationStatement varDecl =
	    new JVariableDeclarationStatement(null, var, null);

	// adjust the contents of <orig> to be relative to <var>
	orig.accept(new SLIRReplacingVisitor() {
                    public Object visitPopExpression(SIRPopExpression oldSelf,
                                                     CType oldTapeType) {
                        // Recurse into children.
                        SIRPopExpression self = (SIRPopExpression)
                            super.visitPopExpression(oldSelf,
                                                     oldTapeType);
			// reference our var
			JLocalVariableExpression ref = new JLocalVariableExpression(null,
										    var);
                        // Return new peek expression.
                        return new SIRPeekExpression(new JPrefixExpression(null,
									   OPE_PREINC,
									   ref),
						     oldTapeType);
                    }
                    public Object visitPeekExpression(SIRPeekExpression oldSelf,
						      CType oldTapeType,
						      JExpression arg) {
                        // Recurse into children.
                        SIRPeekExpression self = (SIRPeekExpression)
                            super.visitPeekExpression(oldSelf,
                                                     oldTapeType,
						     arg);
			// reference our var
			JLocalVariableExpression ref = new JLocalVariableExpression(null,
										    var);
                        // Return new peek expression.
                        return new SIRPeekExpression(new JAddExpression(null, ref, arg),
						     oldTapeType);
                    }
                });

	// return the block
	JStatement[] statements = {varDecl, orig};
	return new JBlock(null, statements, null);
    }	
        
    private static JFieldDeclaration[] makeFields(SIRSplitJoin sj,
                                                 List children)
    {
        Iterator childIter;
        
        // Walk the list of children to get the total field length.
        int numFields = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            numFields += filter.getFields().length;
        }
        
        // Now make the field array...
        JFieldDeclaration[] newFields = new JFieldDeclaration[numFields];
        
        // ...and copy all of the fields in.
        numFields = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            for (int i = 0; i < filter.getFields().length; i++)
                newFields[numFields + i] = filter.getFields()[i];
            numFields += filter.getFields().length;
        }
        
        // All done.
        return newFields;
    }

    private static JMethodDeclaration[] makeMethods(SIRSplitJoin sj,
                                                   List children)
    {
        // Just copy all of the methods into an array.
        Iterator childIter;
        
        // Walk the list of children to get the total number of
        // methods.  Skip work functions wherever necessary.
        int numMethods = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            numMethods += filter.getMethods().length - 1;
        }
        
        // Now make the method array...
        JMethodDeclaration[] newMethods = new JMethodDeclaration[numMethods];
        
        // ...and copy all of the methods in.
        numMethods = 0;
        childIter = children.iterator();
        while (childIter.hasNext())
        {
            SIRFilter filter = (SIRFilter)childIter.next();
            for (int i = 0; i < filter.getMethods().length; i++)
            {
                JMethodDeclaration method = filter.getMethods()[i];
                if (method != filter.getWork()) {
                    newMethods[numMethods++] = method;
		}
            }
        }
        
        // All done.
        return newMethods;
    }
}

/**
 * Represents how many times the components of a splitjoin should
 * execute in the steady state.
 */
class RepInfo {

    public int[] child;
    public int joiner;
    public int splitter;

    private RepInfo(int numChildren) {
	this.child = new int[numChildren];
    }

    /**
     * Returns repInfo giving number of repetitions of streams in <sj>
     * Requires that all children of <sj> be filters.
     */
    public static RepInfo calcReps(SIRSplitJoin sj) {
	for (int i=0; i<sj.size(); i++) {
	    Utils.assert(sj.get(i) instanceof SIRFilter);
	}
	RepInfo result = new RepInfo(sj.size());
	result.compute(sj);
	return result;
    }

    /**
     * Makes the weights valid for the given <sj>
     */
    private void compute(SIRSplitJoin sj) {
	// interface with the scheduler...
	Scheduler scheduler = new SimpleHierarchicalScheduler();
	SchedSplitJoin schedSplit = scheduler.newSchedSplitJoin(sj);
	// keep track of the sched objects for the filters so we can
	// retrieve their multiplicities
	SchedFilter[] schedFilter = new SchedFilter[sj.size()];
	for (int i=0; i<sj.size(); i++) {
	    // get the filter
	    SIRFilter filter = (SIRFilter)sj.get(i);
	    // build scheduler representation of child
	    schedFilter[i] = scheduler.newSchedFilter(filter, 
						      filter.getPushInt(), 
						      filter.getPopInt(),
						      filter.getPeekInt());
	    // add child to pipe
	    schedSplit.addChild(schedFilter[i]);
	}
	// get the splitter, joiner
	SIRSplitter splitter = sj.getSplitter();
	SIRJoiner joiner = sj.getJoiner();
	int[] splitWeights = splitter.getWeights();
	int[] joinWeights = joiner.getWeights();
	// set split type
	schedSplit.setSplitType(scheduler.newSchedSplitType(splitter.getType().toSchedType(), 
							    Utils.intArrayToList(splitWeights), 
							    splitter));

	// set join type
	schedSplit.setJoinType(scheduler.newSchedJoinType(joiner.getType().toSchedType(), 
							  Utils.intArrayToList(joinWeights),
							  joiner));
	
	// compute schedule
	scheduler.useStream(schedSplit);
	scheduler.computeSchedule();
	
	// fill in the info for this
	for (int i=0; i<sj.size(); i++) {
	    this.child[i] = schedFilter[i].getNumExecutions().intValue();
	}
	// infer how many times the splitter, joiner runs
	
	// beware of sources in splits
	int index = -1;
	boolean nullSplit = false, nullJoin = false;
	for (int i=0; i<child.length;i++) {
	    if (child[i]!=0 && splitWeights[i]!=0 && joinWeights[i]!=0) {
		index = i;
		break;
	    }
	    if (i==child.length-1) {
		// trying to fuse null split or join -- assume weight
		// on opposite is okay
		index = i;
		if (splitWeights[i]==0) {
		    nullSplit = true;
		} else {
		    nullJoin = true;
		}
		//		Utils.fail("Think we're trying to fuse a null split or something--error");
	    }
	}
	if (nullSplit) {
	    this.splitter = 0;
	} else {
	    this.splitter = child[index] * ((SIRFilter)sj.get(index)).getPopInt() / splitWeights[index];
	    // make sure we came out even
	    Utils.assert(this.splitter * splitWeights[index] == 
			 this.child[index] * ((SIRFilter)sj.get(index)).getPopInt());
	}
	// now for joiner
	if (nullJoin) {
	    this.joiner = 0;
	} else {
	    this.joiner = this.child[index] * ((SIRFilter)sj.get(index)).getPushInt() / joinWeights[index];
	    // make sure we come out even
	    Utils.assert(this.joiner * joinWeights[index] == 
			 this.child[index] * ((SIRFilter)sj.get(index)).getPushInt());
	}
    }


}


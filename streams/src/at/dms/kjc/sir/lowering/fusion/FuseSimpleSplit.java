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
public class FuseSimpleSplit {
    /**
     * Flattens a split/join, subject to the following constraints:
     *  1. Each parallel component of the stream is a filter.  Further, it is
     *     not a two-stage filter that peeks.
     *  2. If the splitter is anything other than duplicate, then
     *     there is no peeking from within the parallel streams.
     */
    public static SIRStream fuse(SIRSplitJoin sj)
    {
	if (!isFusable(sj)) {
	    return sj;
	} else {
	    System.err.println("Fusing " + (sj.size()) + " SplitJoin filters!"); 
	}

	// get copy of child streams
        List children = sj.getParallelStreams();

	// rename components
	doRenaming(children);
	// calculate the repetitions for the split-join
	SRepInfo rep = SRepInfo.calcReps(sj);

        // So at this point we have an eligible split/join; flatten it.
        JMethodDeclaration newWork = makeWorkFunction(sj, children, rep);
        JFieldDeclaration[] newFields = makeFields(sj, children);
        JMethodDeclaration[] newMethods = makeMethods(sj, children);
        JMethodDeclaration newInit = makeInitFunction(sj, children);

	SRate rate = calcSRate(sj, rep);

        // Build the new filter.
        SIRFilter newFilter = new SIRFilter(sj.getParent(), "Fused_" + sj.getIdent(),
                                            newFields, newMethods, new JIntLiteral(rate.peek),
                                            new JIntLiteral(rate.pop), new JIntLiteral(rate.push),
                                            newWork, sj.getInputType(), sj.getOutputType());
        // Use the new init function
        newFilter.setInit(newInit);

	// make new pipeline representing fused sj
	SIRPipeline fused = makeFusedPipe(sj, rep, rate, newFilter);

	// replace in parent
	replaceInParent(sj, fused);

        return newFilter;
    }

    /**
     * Create a pipelin containing prelude and postlude (and new
     * filter) to represent the fused splitjoin.
     */
    private static SIRPipeline makeFusedPipe(SIRSplitJoin sj, 
					     SRepInfo rep,
					     SRate rate,
					     SIRFilter newFilter) {
	SIRPipeline pipe = new SIRPipeline("Wrapper_for_" + newFilter.getName());
	// make a dummy init function
	pipe.setInit(new JMethodDeclaration(null, 0, CStdType.Void, "init",
					    JFormalParameter.EMPTY, CClassType.EMPTY,
					    new JBlock(), null, null));

	// make a splitFilter only if it's not a duplicate and it's
	// not a null split
	if (sj.getSplitter().getType()!=SIRSplitType.DUPLICATE &&
	    rep.splitter!=0) {
	    pipe.add(makeFilter(sj.getParent(),
				"Pre_" + sj.getIdent(),
				makeSplitFilterBody(sj.getSplitter(), 
						    rep,
						    sj.getInputType()), 
				rate.pop, rate.pop, rate.pop,
				sj.getInputType()));
	}
	
	int index = sj.getParent().indexOf(sj);
	pipe.add(newFilter, new LinkedList(sj.getParent().getParams(index)));
	
	// make a joinFilter only if it's not a not a null join
	if (rep.joiner!=0) {
	    pipe.add(makeFilter(sj.getParent(),
				"Post_" + sj.getIdent(),
				makeJoinFilterBody(sj.getJoiner(), 
						   rep,
						   sj.getOutputType()),
				rate.push, rate.push, rate.push,
				sj.getOutputType()));
	}
	
	return pipe;
    }

    private static void replaceInParent(SIRSplitJoin sj, 
					SIRPipeline fused) {
	// if parent of <sj> is a pipeline, then copy these children
	// in, in place of the original filter
	SIRContainer parent = sj.getParent();
	// DON'T CONSIDER THIS CASE SINCE IT HINDERS
	// PARTITIONING--would like things to end up in their own
	// nested pipeline so that pipeline can be fused
	if (false) { //parent instanceof SIRPipeline) {
	    int index = parent.indexOf(sj);
	    parent.remove(index);
	    for (int i=fused.size()-1; i>=0; i--) {
		parent.add(index, fused.get(i), fused.getParams(i));
	    }
	} else {
	    // if <fused> has just one filter, add it in place of <sj>
	    if (fused.size()==1) {
		parent.replace(sj, fused.get(0));
	    } else {
		// otherwise, just add <fused> to parent in place of <sj>
		parent.replace(sj, fused);
		// clear the param list pased to <fused>
		parent.getParams(parent.indexOf(fused)).clear();
	    }
	}
    }

    private static SRate calcSRate(SIRSplitJoin sj, SRepInfo rep) {
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
	int maxPeek = -1;
	for (int i=0; i<sj.size(); i++) {
	    SIRFilter filter = (SIRFilter)sj.get(i);
	    maxPeek = Math.max(maxPeek, 
			       filter.getPeekInt()-
			       filter.getPopInt());
	}
	// calculate the peek as the amount we'll look into the input
	// during execution
	int peek = pop+maxPeek;

	return new SRate(push, pop, peek);
    }

    private static void doRenaming(List children) {
        // Rename all of the child streams of this.
        RenameAll ra = new RenameAll();
        Iterator iter = children.iterator();
        while (iter.hasNext()) {
	    ra.renameFilterContents((SIRFilter)iter.next());
        }
    }
	
    /**
     * Returns whether or not <sj> is fusable.
     */
    private static boolean isFusable(SIRSplitJoin sj) {
        // Check the ratios.
        Iterator childIter = sj.getParallelStreams().iterator();
        while (childIter.hasNext()) {
            SIRStream str = (SIRStream)childIter.next();
            if (!(str instanceof SIRFilter))
                return false;
            SIRFilter filter = (SIRFilter)str;
	    // don't allow two-stage filters, since we aren't dealing
	    // with how to fuse their initWork functions.
            if (filter instanceof SIRTwoStageFilter) {
		System.err.println("Didn't fuse SJ because this child is a 2-stage filter: " + filter);
                return false;
	    }
        }
	return true;
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
					int push, int pop, int peek,
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
					      SRepInfo rep,
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
    private static JBlock makeJoinFilterBody(SIRJoiner join, SRepInfo rep, CType type) {
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
	JMethodDeclaration init = sj.getInit();
	// add calls to init functions
	for (int i=0; i<sj.size(); i++) {
	    SIRStream child = sj.get(i);
	    List params = sj.getParams(i);
	    if (child.needsInit()) {
		init.addStatement(new JExpressionStatement(null,
							   new JMethodCallExpression(null, 
										     new JThisExpression(null),
										     child.getInit().getName(),
										     (JExpression[])params.toArray(new JExpression[0])),
							   null));
	    }
	}
        return init;
    }

    private static JMethodDeclaration makeWorkFunction(SIRSplitJoin sj,
						       List children,
						       SRepInfo rep) {
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
                        return new SIRPeekExpression(new JPostfixExpression(null,
									   OPE_POSTINC,
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
class SRepInfo {

    public int[] child;
    public int joiner;
    public int splitter;

    private SRepInfo(int numChildren) {
	this.child = new int[numChildren];
    }

    /**
     * Returns repInfo giving number of repetitions of streams in <sj>
     * Requires that all children of <sj> be filters.
     */
    public static SRepInfo calcReps(SIRSplitJoin sj) {
	for (int i=0; i<sj.size(); i++) {
	    Utils.assert(sj.get(i) instanceof SIRFilter);
	}
	SRepInfo result = new SRepInfo(sj.size());
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

class SRate {
    public int push;
    public int pop;
    public int peek;

    public SRate(int push, int pop, int peek) {
	this.push = push;
	this.pop = pop;
	this.peek = peek;
    }
}




package at.dms.kjc.sir.lowering.fusion;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;

import java.util.*;

/**
 * This flattens any set of filters into a single splitjoin.
 */
public class FuseSplit {
    /**
     * The number statements in an unrolled piece of code for a
     * splitter or joiner before it is put in a loop instead of being
     * unrolled.
     */
    public static final int SPLITTER_JOINER_LOOP_THRESHOLD() { return KjcOptions.unroll; }

    private static SIRStream lastFused;
    
    /**
     * Names for variables introduced in this pass.
     */
    private static final String PEEK_BUFFER_NAME = "___PEEK_BUFFER";
    private static final String PEEK_READ_NAME = "___PEEK_READ";
    private static final String PEEK_WRITE_NAME = "___PEEK_WRITE";
    private static final String PUSH_BUFFER_NAME = "___PUSH_BUFFER";
    private static final String PUSH_READ_NAME = "___PUSH_READ";
    private static final String PUSH_WRITE_NAME = "___PUSH_WRITE";

    /**
     * Fuses half the children of <sj> together instead of fusing the
     * entire construct.  Returns result, which might be mutated
     * version of <sj> if it could fuse without creating fresh
     * construct.
     */
    public static SIRStream semiFuse(SIRSplitJoin sj) {
	if(sj.size()<=3)
	    return fuse(sj);
	else if(sj.size()%2==0) {
	    int half=sj.size()/2;
	    int[] partition=new int[half];
	    for (int i=0;i<half;i++) {
		partition[i]=2;
	    }
	    return fuse(sj, PartitionGroup.createFromArray(partition));
	} else {
	    int half=sj.size()/2;
	    int[] partition=new int[half+1];
	    for(int i=0;i<half;i++)
		partition[i]=2;
	    partition[half]=1;
	    return fuse(sj, PartitionGroup.createFromArray(partition));
	}
    }


    //Uses partition to partion children according to <partition>
    public static SIRStream fuse(SIRSplitJoin sj, PartitionGroup partition) {
	{
	    // clear possible wrapper pipelines in children
	    Lifter.lift(sj);
	    //Quick check
	    assert partition.getNumChildren()==sj.size():
                "More children in partitioning than in splitjoin " + sj;
	    for(int i=0;i<partition.size();i++) {
		// if we're trying to fuse something that's not a filter, just return
		if (partition.get(i)>1) {
		    for (int j=partition.getFirst(i); j<=partition.getLast(i); j++) {
			if (!(sj.get(j) instanceof SIRFilter)) {
			    /*
			      System.err.println("Tried to fuse non-filter " + 
			      sj.get(j).getClass() + " " + sj.get(j).getName() + 
			      " in SJ; returning original SJ: " + sj.getName());
			    */
			    return sj;
			}
		    }
		}
	    }
	}

	int numParts = partition.size();
	if (numParts==1) {
	    // if fusing whole thing
	    SIRStream fused=fuse(sj);
	    if(fused instanceof SIRFilter)
		lastFused=(SIRFilter)fused;
	    else {
		if(KjcOptions.partition_greedier)
		    if(fused instanceof SIRPipeline) {
			FusePipe.fuse((SIRPipeline)fused);
			Lifter.eliminatePipe((SIRPipeline)fused);
		    } else
			Lifter.eliminateSJ((SIRSplitJoin)fused);
		lastFused=((SIRContainer)fused).get(0);
	    }
	    return fused;
	} else if (numParts==sj.size()) {
	    // if not fusing at all
	    return sj;
	}

	//System.err.println("Fusing " + (sj.size()) + " SplitJoin filters into " + numParts + " filters..."); 

        // first refactor the splitjoin so we can do partial fusing
        SIRSplitJoin newSj = RefactorSplitJoin.addHierarchicalChildren(sj, partition);
	sj.getParent().replace(sj, newSj);
	sj = newSj;
        // then fuse the eligible components
        for (int i=0; i<partition.size(); i++) {
            // leave the 1-way components alone, since they weren't
            // getting fused originally
            if (partition.get(i)>1) {
                SIRStream fused=fuse((SIRSplitJoin)sj.get(i));
		if(fused instanceof SIRFilter)
		    lastFused=(SIRFilter)fused;
		else {
		    if(KjcOptions.partition_greedier)
			if(fused instanceof SIRPipeline) {
			    FusePipe.fuse((SIRPipeline)fused);
			    Lifter.eliminatePipe((SIRPipeline)fused);
			} else
			    Lifter.eliminateSJ((SIRSplitJoin)fused);
		    lastFused=((SIRContainer)fused).get(0);
		}
            }
        }
	Lifter.eliminateSJ(sj);
	return sj;
    }

    public static SIRStream getLastFused() {
	return lastFused;
    }

    /**
     * Returns result of fusion, which might be mutated version of
     * <sj> if it could fuse without creating fresh construct.
     */
    public static SIRStream fuse(SIRSplitJoin sj) {
 	// clear possible wrapper pipelines in children
	Lifter.lift(sj);
	// dispatch to simple fusion
	SIRStream dispatchResult = dispatchToSimple(sj);
	if (dispatchResult!=null) {
	    return dispatchResult;
	}

	if (!isFusable(sj)) {
	    return sj;
	} else {
	    //System.err.println("Fusing " + (sj.size()) + " SplitJoin filters."); 
	}

	// get copy of child streams and rename them
        List children = sj.getParallelStreams();
	doRenaming(children);

	// calculate some helper information
	RepInfo rep = RepInfo.calcReps(sj);
	Rate rate = calcRate(sj, rep);
	SJChildInfo[] childInfo = makeSJChildInfo(children, rep, rate);

        // make some components
        JFieldDeclaration[] newFields = makeFields(sj, childInfo);
        JMethodDeclaration[] newMethods = makeMethods(sj, children);
        JMethodDeclaration newInit = makeInitFunction(sj, childInfo);

	// make the new initwork, work functions
	JMethodDeclaration newInitWork = makeInitWorkFunction(sj, childInfo, rep, rate);
        JMethodDeclaration newWork = makeWorkFunction(sj, childInfo, rep, rate);

	String newName = FusePipe.getFusedName(sj.getParallelStreams());
	
        // Build the new filter.  If there's no pushing or peeking
        // from initwork, then just inline the initwork in the init function
	SIRFilter newFilter;
	if (rate.initPeek==0 && rate.initPush==0) {
	    newInit.getBody().addAllStatements(newInitWork.getBody().getStatements());
	    newFilter = new SIRFilter(sj.getParent(), newName, 
				      newFields, newMethods, 
				      new JIntLiteral(rate.peek),
				      new JIntLiteral(rate.pop), 
				      new JIntLiteral(rate.push),
				      newWork, 
				      sj.getInputType(), sj.getOutputType());
	} else {
	    newFilter = new SIRTwoStageFilter(sj.getParent(), newName,
					      newFields, newMethods, 
					      new JIntLiteral(rate.peek),
					      new JIntLiteral(rate.pop), 
					      new JIntLiteral(rate.push),
					      newWork, 
					      rate.initPeek, rate.initPop, rate.initPush,
					      newInitWork,
					      sj.getInputType(), sj.getOutputType());
	}

        // Use the new init function
        newFilter.setInit(newInit);

	// replace in parent
	sj.getParent().replace(sj, newFilter);

        return newFilter;
    }

    /**
     * Dispatches the fusion to the simple routine if possible.  If no
     * dispatch was possible, returns null.  Otherwise, returns the
     * result of the simple fusion.
     */
    private static SIRStream dispatchToSimple(SIRSplitJoin sj) {
	// revert to the older version of fusion if either:
	//   - the simplesjfusion tag is specified
	//   - there are no two stage filters AND there is no round-robin with peeking
	if (KjcOptions.simplesjfusion) {
	    return FuseSimpleSplit.fuse(sj);
	} else {
	    boolean noTwoStage = true;
	    boolean noRRPeek = sj.getSplitter().getType()==SIRSplitType.WEIGHTED_RR || 
		sj.getSplitter().getType()==SIRSplitType.ROUND_ROBIN;
	    for (int i=0; i<sj.size(); i++) {
		if (sj.get(i) instanceof SIRFilter) {
		    SIRFilter filter = (SIRFilter)sj.get(i);
		    if (filter instanceof SIRTwoStageFilter) {
			noTwoStage = false;
			break;
		    }
		    if (filter.getPeekInt() > filter.getPopInt()) {
			noRRPeek = false;
			break;
		    }
		}
	    }
	    if (noTwoStage && noRRPeek) {
		return FuseSimpleSplit.fuse(sj);
	    } else {
		return null;
	    }
	}
    }
    
    /**
     * Makes list of child info.
     */
    private static SJChildInfo[] makeSJChildInfo(List filterList, RepInfo rep, Rate rate) {
	// make the result
	SJChildInfo[] result = new SJChildInfo[filterList.size()];

	// for each filter...
	ListIterator it = filterList.listIterator();
	for (int i=0; it.hasNext(); i++) {
	    SIRFilter filter = (SIRFilter)it.next();

	    // the peek buffer
	    JVariableDefinition peekBufferVar = 
		new JVariableDefinition(null,
					at.dms.kjc.Constants.ACC_FINAL,
					new CArrayType(Utils.voidToInt(filter.
								       getInputType()), 
						       1 /* dimension */ ),
					PEEK_BUFFER_NAME + "_" + i,
					null);

	    JFieldDeclaration peekBuffer = new JFieldDeclaration(null,
								 peekBufferVar,
								 null,
								 null);
	    
	    // the peek buffer size
	    int peekSize = rep.child[i] * filter.getPopInt() + (filter.getPeekInt() - filter.getPopInt());
	    // if we have a two-stage filter, need to peek the items it consumes the first time through
	    if (filter instanceof SIRTwoStageFilter) {
		peekSize += ((SIRTwoStageFilter)filter).getInitPop();
	    }
	    // added buffer, since initPeek has to match a steady execution.  Think about this...
	    peekSize += rate.initPeek;

	    // the peek counter
	    JFieldDeclaration peekRead = new JFieldDeclaration(null,
							       new JVariableDefinition(null, 0, CStdType.Integer,
										       PEEK_READ_NAME + "_" + i,
										       new JIntLiteral(-1)),
							       null, null);
	    
	    // the peek counter
	    JFieldDeclaration peekWrite = new JFieldDeclaration(null,
								new JVariableDefinition(null, 0, CStdType.Integer,
											PEEK_WRITE_NAME + "_" + i,
											new JIntLiteral(-1)),
								null, null);
	    
	    // push buffer
	    JVariableDefinition pushBufferVar = 
		new JVariableDefinition(null,
					at.dms.kjc.Constants.ACC_FINAL,
					new CArrayType(Utils.voidToInt(filter.
								       getOutputType()), 
						       1 /* dimension */ ),
					PUSH_BUFFER_NAME + "_" + i,
					null);

	    JFieldDeclaration pushBuffer = new JFieldDeclaration(null,
								 pushBufferVar,
								 null,
								 null);
	    
	    // the push buffer size
	    int pushSize = rep.child[i] * filter.getPushInt();
	    // if we have a two-stage filter, add its push amount
	    if (filter instanceof SIRTwoStageFilter) {
		pushSize += ((SIRTwoStageFilter)filter).getInitPush();
	    }

	    // the push read counter
	    JFieldDeclaration pushRead = new JFieldDeclaration(null,
							       new JVariableDefinition(null, 0, CStdType.Integer,
										       PUSH_READ_NAME + "_" + i,
										       new JIntLiteral(-1)),
							       null, null);

	    // the push write counter
	    JFieldDeclaration pushWrite = new JFieldDeclaration(null,
								new JVariableDefinition(null, 0, CStdType.Integer,
											PUSH_WRITE_NAME + "_" + i,
											new JIntLiteral(-1)),
								null, null);

	    result[i] = new SJChildInfo(filter, 
					new BufferInfo(peekBuffer, peekSize, peekRead, peekWrite),
					new BufferInfo(pushBuffer, pushSize, pushRead, pushWrite));
	}
	// return result
	return result;
    }
	
    private static Rate calcRate(SIRSplitJoin sj, RepInfo rep) {
	// intuitively, the (peek-pop) will be the maximum of:
	//   1. the steady-state (peek-pop) required by any stream
	//   2. the initPeek of any stream.
	// --> since we have invariant that
	// (peek-pop)==(initPeek-initPop), initPop cannot be negative,
	// and newInitPop==0, this leaves #2 as the only determinant
	// of the new (peek-pop) amount.

	// so, find maximum initPeek of any stream.  Find this by
	// looking at (peek-pop) and adding initPop if it exists.
	// Keep in mind that this maximum is in terms of the input
	// buffer, not the localized buffer of a given child.
	
	// to calculate: the index in the combined stream that each
	// child stream peeks to
	int[] peekIndex = new int[sj.size()];
	// get splitter weights
	int[] weights = sj.getSplitter().getWeights();
	int[] partialSum = new int[sj.size()];
	// calculate partial sums of weights
	for (int i=1; i<weights.length; i++) {
	    partialSum[i] = partialSum[i-1] + weights[i-1];
	}
	// get total weights
	int sumOfWeights = sj.getSplitter().getSumOfWeights();
	boolean isDup = sj.getSplitter().getType()==SIRSplitType.DUPLICATE;
	// calculate <peekindex>
	for (int i=0; i<sj.size(); i++) {
	    SIRFilter filter = (SIRFilter)sj.get(i);
	    int localPeek = filter.getPeekInt() - filter.getPopInt();
	    if (filter instanceof SIRTwoStageFilter) {
		localPeek += ((SIRTwoStageFilter)filter).getInitPop();
	    }
	    if (isDup || localPeek==0) {
		peekIndex[i] = localPeek;
	    } else {
		peekIndex[i] = sumOfWeights * (int)Math.floor(localPeek / weights[i]) 
		    + partialSum[i] + (localPeek % weights[i]);
	    }
	}

	// the (peek-pop)==newInitPeek amount is the maximum of all the peekindex's
	int peekMinusPop = 0;
	for (int i=0; i<peekIndex.length; i++) {
	    if (peekIndex[i] > peekMinusPop) {
		peekMinusPop = peekIndex[i];
	    }
	}
	// scale up peekMinusPop to the next round execution of the splitter
	if (!isDup) {
	    peekMinusPop = sumOfWeights * (int)Math.ceil(((float)peekMinusPop) / ((float)sumOfWeights));
	}

	// in this design there is no pushing or popping the first time through
	int newInitPush = 0;
	int newInitPeek = peekMinusPop;
	int newInitPop = 0;

	// calculate the push/pop/peek ratio
	int newPush = rep.joiner * sj.getJoiner().getSumOfWeights();
	int newPop;
	if (isDup) {
	    newPop = rep.splitter;
	} else {
	    newPop = rep.splitter * sj.getSplitter().getSumOfWeights();;
	}
	int newPeek = newPop+peekMinusPop;

	return new Rate(newInitPush, newInitPop, newInitPeek, newPush, newPop, newPeek);
    }

    private static void doRenaming(List children) {
        // Rename all of the child streams of this.
        Iterator iter = children.iterator();
        while (iter.hasNext()) {
	    RenameAll.renameFilterContents((SIRFilter)iter.next());
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
            if (!(str instanceof SIRFilter)) {
                return false;
	    }
            SIRFilter filter = (SIRFilter)str;
	    // don't allow two-stage filters, since we aren't dealing
	    // with how to fuse their initWork functions.
	    /*
	      if (filter instanceof SIRTwoStageFilter) {
	      System.err.println("Didn't fuse SJ because this child is a 2-stage filter: " + filter);
	      return false;
	      }
	    */
        }
	return true;
    }

    /**
     * Makes a block to copy peek data into the buffers.  If
     * <initMode> is true, then it only fills the first rate.initPeek
     * items, and does not pop anything.  Otherwise it fills rate.peek
     * items and pops rate.pop items afterwards.
     */
    private static JBlock doPeeking(SIRSplitter split,
				    SJChildInfo[] childInfo,
				    RepInfo rep,
				    Rate rate,
				    CType type,
				    boolean initMode) {
	// get splitter weights
	int[] weights = split.getWeights();
	boolean isDup = split.getType()==SIRSplitType.DUPLICATE;
	int[] partialSum = new int[weights.length];
	JExpression[] partialSumExpression = new JExpression[weights.length];
	JExpression[] weightsExpression = new JExpression[weights.length];
	// calculate partial sums of weights
	partialSumExpression[0] = new JIntLiteral(0);
	weightsExpression[0] = new JIntLiteral(weights[0]);
	for (int i=1; i<weights.length; i++) {
	    partialSum[i] = partialSum[i-1] + weights[i-1];
	    partialSumExpression[i] = new JIntLiteral(partialSum[i]);
	    weightsExpression[i] = new JIntLiteral(weights[i]);
	}
	// get total weights
	int sumOfWeights = split.getSumOfWeights();
	// calculate number of times the split executes in this mode
	int numExec;
	if (initMode) {
	    numExec = (int) Math.ceil(((float)rate.initPeek) / ((float) (isDup ? 1 : sumOfWeights)));
	} else {
	    numExec = rep.splitter;
	}
	// make list of statements for work function
	LinkedList list = new LinkedList();
	// in initMode, don't worry about code size
	if (initMode) {
	    for (int k=0; k<weights.length; k++) {
		for (int i=0; i<numExec; i++) {
		    for (int j=0; j<weights[k]; j++) {
			// calculate index of this peek
			int index = i * (isDup ? 1 : sumOfWeights) + (isDup ? 0 : partialSum[k]) + j;
			// if we're in initmode, then don't look past an index of <rate.initPeek>
			if (index<rate.initPeek) {
			    //&& (childInfo[i].filter instanceof SIRTwoStageFilter) &&
			    //	      i*weights[k]+j < ((SIRTwoStageFilter)childInfo[i].filter).getInitPeek())) {
			    // make a peek expression
			    JExpression peekVal = new SIRPeekExpression(new JIntLiteral(index), type);
			    // make an assignment to the proper peek array
			    JAssignmentExpression assign = makeBufferPush(childInfo[k].peekBuffer,
									  peekVal);
			    // make an expression statement
			    list.add(new JExpressionStatement(null, assign, null));
			}
		    }
		}
	    }
	} else {
	    // see how many statements we would generate
	    int numStatements = 0;
	    for (int k=0; k<weights.length; k++) {
		for (int i=0; i<numExec; i++) {
		    for (int j=0; j<weights[k]; j++) {	    
			numStatements++;
		    }
		}
	    }
	    if (numStatements<=SPLITTER_JOINER_LOOP_THRESHOLD()) {
		//System.err.println("FuseSplit: unrolling " + numStatements + " ( <= " + SPLITTER_JOINER_LOOP_THRESHOLD());
		// if only have a few, generate them plain
		for (int k=0; k<weights.length; k++) {
		    for (int i=0; i<numExec; i++) {
			for (int j=0; j<weights[k]; j++) {
			    int index;
			    if (isDup) {
				// calculate index of this peek
				index = i + j + rate.initPeek;
			    } else {
				index = i * sumOfWeights + partialSum[k] + j + rate.initPeek;
			    }
			    // make a peek expression
			    JExpression peekVal = new SIRPeekExpression(new JIntLiteral(index), type);
			    // make an assignment to the proper peek array
			    JAssignmentExpression assign = makeBufferPush(childInfo[k].peekBuffer, peekVal);
			    list.add(new JExpressionStatement(null, assign, null));
			}
		    }
		}
	    } else {
		//System.err.println("FuseSplit: compacting " + numStatements + " ( > " + SPLITTER_JOINER_LOOP_THRESHOLD());
		// otherwise, optimize for imem...
		// _weights[N] = { , , }
		JArrayInitializer _weightsInit = new JArrayInitializer(null, weightsExpression);
		JVariableDefinition _weights = new JVariableDefinition(null, 0, new CArrayType(CStdType.Integer, 1), "_weights", _weightsInit);
		list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_weights}, null));
		// _partialSum[N] = { , , }
		JArrayInitializer _partialSumInit = new JArrayInitializer(null, partialSumExpression);
		JVariableDefinition _partialSum = new JVariableDefinition(null, 0, new CArrayType(CStdType.Integer, 1), "_partialSum", _partialSumInit);
		list.add(new JVariableDeclarationStatement(null, new JVariableDefinition[] {_partialSum}, null));
		// it's non-trivial to move the k loop into dynamically-generated code because we reference childInfo[k]
		for (int k=0; k<weights.length; k++) {
		    // make loop variables
		    JVariableDefinition _i = new JVariableDefinition(null, 0, CStdType.Integer, "_i", new JIntLiteral(0));
		    JVariableDefinition _j = new JVariableDefinition(null, 0, CStdType.Integer, "_j", new JIntLiteral(0));
		    JExpression index;
		    if (isDup) {
			index = new JAddExpression(null, 
						   new JLocalVariableExpression(null, _i),
						   new JAddExpression(null,
								      new JLocalVariableExpression(null, _j),
								      new JIntLiteral(rate.initPeek)));
		    } else {
			index = new JAddExpression(null,
						   new JMultExpression(null,
								       new JLocalVariableExpression(null, _i),
								       new JIntLiteral(sumOfWeights)),
						   new JAddExpression(null,
								      new JAddExpression(null, 
											 new JArrayAccessExpression(null, 
														    new JLocalVariableExpression(null, _partialSum),
														    new JIntLiteral(k)),
											 new JLocalVariableExpression(null, _j)),
								      new JIntLiteral(rate.initPeek)));
		    }
		    JExpression peekVal = new SIRPeekExpression(index, type);
		    JStatement assign = new JExpressionStatement(null, makeBufferPush(childInfo[k].peekBuffer, peekVal), null);
		    // add i loop
		    JStatement jLoop = Utils.makeForLoop(assign, new JArrayAccessExpression(null, new JLocalVariableExpression(null, _weights), new JIntLiteral(k)), _j);
		    JStatement iLoop = Utils.makeForLoop(jLoop, new JIntLiteral(numExec), _i);
		    list.add(iLoop);
		}
	    }
	}
	// pop items at end
	int popCount = initMode ? rate.initPeek : rate.pop;
	if (!initMode) {
	    list.add(Utils.makeForLoop(new JExpressionStatement(null,
								new SIRPopExpression(type),
								null),
				       popCount));
	}
	return new JBlock(null, list, null);
    }
    
    
    /**
     * Returns an expression that inrements the read index of
     * <buffer> by <amount>, wrapping it around the edge of the
     * buffer.
     */
    private static JStatement incrementReadIndex(BufferInfo buffer, int amount) {
	return new JExpressionStatement(null,
					new JAssignmentExpression
					(null,
					 new JFieldAccessExpression(null, new JThisExpression(null),
								    buffer.readIndex.getVariable()
								    .getIdent()),
					 new JModuloExpression(null,
							       new JAddExpression
							       (null,
								new JFieldAccessExpression(null, 
											   new JThisExpression(null),
											   buffer.
											   readIndex.getVariable()
											   .getIdent()),
								new JIntLiteral(amount)), 
							       new JIntLiteral(buffer.size))),
					null);
    }
    
    /**
     * Makes a block to copy data out of push buffers.
     */
    private static JBlock doPushing(SIRJoiner join,
				    SJChildInfo[] childInfo,
				    RepInfo rep,
				    Rate rate,
				    CType type) {
	// get joiner weights
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

	// increment the read indices by the pop amounts
	/*
	  for (int i=0; i<weights.length; i++) {
	  list.add(incrementReadIndex(childInfo[i].peekBuffer, rep.splitter*weights[i]));
	  }
	*/

	// do pushing

	for (int k=0; k<rep.joiner; k++) {
	    for (int i=0; i<weights.length; i++) {
		JExpression rhs = makeBufferPop(childInfo[i].pushBuffer);
		JExpression push = new SIRPushExpression(rhs, type);
		// only unroll
		if (weights[i]<=SPLITTER_JOINER_LOOP_THRESHOLD()) {
		    //System.err.println("FuseSplit: unrolling joiner " + weights[i] + " ( <= " + SPLITTER_JOINER_LOOP_THRESHOLD());
		    for (int j=0; j<weights[i]; j++) {
			list.add(new JExpressionStatement(null, push, null));
		    }
		} else {
		    //System.err.println("FuseSplit: compacting joiner " + weights[i] + " ( > " + SPLITTER_JOINER_LOOP_THRESHOLD());
		    list.add(Utils.makeForLoop(new JExpressionStatement(null, push, null),
					       weights[i]));
		}
	    }
	}
	return new JBlock(null, list, null);
    }
    
    /**
     * Generates a push of <val> to <buffer>.
     */
    protected static JAssignmentExpression makeBufferPush(BufferInfo buffer,
							  JExpression val) {
	// index of the buffer
	JExpression index = 
	    new JModuloExpression(null,
				  new JPrefixExpression(null, 
							Constants.OPE_PREINC, 
							new JFieldAccessExpression
							(null,
							 new JThisExpression(null),
							 buffer.writeIndex.getVariable().getIdent())),
				  new JIntLiteral(buffer.size));
	
	// array access on left
	JExpression lhs = 
	    new JArrayAccessExpression(null,
				       new JFieldAccessExpression(null,
								  new JThisExpression(null),
								  buffer.target.getVariable().getIdent()),
				       index);
	
	// assignment
	return new JAssignmentExpression(null, lhs, val);
    }

    /**
     * Generates a pop expression that gives the next value in
     * <buffer>, also incrementing the internal read index.
     */
    protected static JExpression makeBufferPop(BufferInfo buffer) {
	// index of the buffer
	JExpression index = 
	    new JModuloExpression(null,
				  new JPrefixExpression(null, 
							Constants.OPE_PREINC, 
							new JFieldAccessExpression
							(null,
							 new JThisExpression(null),
							 buffer.readIndex.getVariable().getIdent())),
				  new JIntLiteral(buffer.size));
	return new JArrayAccessExpression(null,
					  new JFieldAccessExpression(null,
								     new JThisExpression(null),
								     buffer.target.getVariable().getIdent()),
					  index);
    }

    /**
     * Generates an expression that gives the value at offset <offset>
     * in <buffer>.
     */
    protected static JExpression makeBufferPeek(BufferInfo buffer,
						JExpression offset) {
	// index of the buffer
	JExpression index = 
	    new JModuloExpression(null,
				  new JAddExpression(null,
						     new JFieldAccessExpression
						     (null,
						      new JThisExpression(null),
						      buffer.readIndex.getVariable().getIdent()),
						     // need to add 1 since the counter points to the last item popped
						     new JAddExpression(null,
									offset,
									new JIntLiteral(1))),
				  new JIntLiteral(buffer.size));
	return new JArrayAccessExpression(null,
					  new JFieldAccessExpression(null,
								     new JThisExpression(null),
								     buffer.target.getVariable().getIdent()),
					  index);
    }

    private static JMethodDeclaration makeInitFunction(SIRSplitJoin sj,
						       SJChildInfo[] childInfo) { 
        // Start with the init function from the split/join.
	JMethodDeclaration init = sj.getInit();
	if(init==null)
	    init=SIRStream.makeEmptyInit();
	// add allocations of peek and push buffers
	for (int i=0; i<childInfo.length; i++) {
	    for (int j=0; j<2; j++) {
		BufferInfo buffer = (j==0 ? childInfo[i].peekBuffer : childInfo[i].pushBuffer);
		// calculate dimensions of the buffer
		JExpression[] dims = { new JIntLiteral(null, buffer.size) };
		//get the type for the buffer, it will be the output type for the
		//push buffer, and the input type for peek buffer
		CType type = (j==0 ? Utils.voidToInt(childInfo[i].filter.getInputType()) :
			      Utils.voidToInt(childInfo[i].filter.getOutputType()));
		// add a statement initializing the buffer

		init.addStatementFirst
		    (new JExpressionStatement(null,
					      new JAssignmentExpression
					      (null,
					       new JFieldAccessExpression(null,
									  new JThisExpression(null),
									  buffer.target.getVariable().getIdent()),
					       new JNewArrayExpression(null,
								       type,
								       dims,
								       null)), null));
	    }
	}
	// add calls to init functions
	for (int i=0; i<sj.size(); i++) {
	    SIRStream child = sj.get(i);
	    List params = sj.getParams(i);
	    if (!child.needsInit()) {
		continue;
	    }
	    init.addStatement(new JExpressionStatement(null,
						       new JMethodCallExpression(null, 
										 new JThisExpression(null),
										 child.getInit().getName(),
										 (JExpression[])params.toArray(new JExpression[0])),
						       null));
	}
        return init;
    }

    private static JMethodDeclaration makeInitWorkFunction(SIRSplitJoin sj,
							   SJChildInfo[] childInfo,
							   RepInfo rep,
							   Rate rate) {
        // new statements for result
        JBlock newStatements = new JBlock();

	// do peeking/popping
	newStatements.addStatement(doPeeking(sj.getSplitter(), childInfo, rep, rate, sj.getInputType(), true));

	// do work
	for (int i=0; i<childInfo.length; i++) {
            // if we have a twostage filter, get the contents of its initwork function
	    if (childInfo[i].filter instanceof SIRTwoStageFilter) {
		JBlock statements = ((SIRTwoStageFilter)childInfo[i].filter).getInitWork().getBody().copy();
		// adjust statements to access arrays instead of peek/pop/push
		statements.accept(new FuseSplitVisitor(childInfo[i]));
		newStatements.addStatement(statements);
		// increment the read index by the initpop amount
		/*
		  newStatements.addStatement(incrementReadIndex(childInfo[i].peekBuffer, 
		  ((SIRTwoStageFilter)childInfo[i].filter).getInitPop()));
		*/
	    }
	}

        // make the work function based on statements
        JMethodDeclaration newWork =
            new JMethodDeclaration(null,
                                   at.dms.kjc.Constants.ACC_PUBLIC,
                                   CStdType.Void,
                                   "initWork",
                                   JFormalParameter.EMPTY,
                                   CClassType.EMPTY,
                                   newStatements,
                                   null,
                                   null);

        return newWork;
    }

    private static JMethodDeclaration makeWorkFunction(SIRSplitJoin sj,
						       SJChildInfo[] childInfo,
						       RepInfo rep,
						       Rate rate) {
        // Build the new work function; add the list of statements
        // from each of the component filters.
        JBlock newStatements = new JBlock();

	// do peeking/popping
	newStatements.addStatement(doPeeking(sj.getSplitter(), childInfo, rep, rate, sj.getInputType(), false));

	// do work
	for (int i=0; i<childInfo.length; i++) {
            // Get the statements of the old work function
            JBlock statements = childInfo[i].filter.getWork().getBody().copy();
	    // adjust statements to access arrays instead of peek/pop/push
	    statements.accept(new FuseSplitVisitor(childInfo[i]));
            // Make a for loop that repeats these statements according
	    // to reps
	    JStatement loop = Utils.makeForLoop(statements, rep.child[i]);
	    // add the loop to the new work function
            newStatements.addStatement(loop);
        }

	// do pushing
	newStatements.addStatement(doPushing(sj.getJoiner(), childInfo, rep, rate, sj.getOutputType()));

        // make the work function based on statements
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
						  SJChildInfo[] childInfo)
    {
        Iterator childIter;
        
        // Walk the list of children to get the total field length.
        int numFields = 0;
	for (int i=0; i<childInfo.length; i++) {
            numFields += childInfo[i].filter.getFields().length;
	    numFields += childInfo[i].getFields().length;
        }
        
        // Now make the field array...
        JFieldDeclaration[] newFields = new JFieldDeclaration[numFields];
        
        // ...and copy all of the fields in.
        numFields = 0;
	for (int i=0; i<childInfo.length; i++) {
	    JFieldDeclaration[] fields = childInfo[i].filter.getFields();
            for (int j = 0; j < fields.length; j++, numFields++) {
                newFields[numFields] = fields[j];
	    }
	    fields = childInfo[i].getFields();
	    for (int j = 0; j < fields.length; j++, numFields++) {
                newFields[numFields] = fields[j];
	    }
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
		if (filter instanceof SIRTwoStageFilter) {
		    // going to remove initWork, too
		    numMethods--;
		}
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
			if (method != filter.getWork() && 
			    !(filter instanceof SIRTwoStageFilter && method==((SIRTwoStageFilter)filter).getInitWork())) {
			    newMethods[numMethods++] = method;
			}
		    }
	    }
        
        // All done.
        return newMethods;
    }

    /**
     * Represents how many times the components of a splitjoin should
     * execute in the steady state.
     */
    static class RepInfo {

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
		assert sj.get(i) instanceof SIRFilter;
	    }
	    RepInfo result = new RepInfo(sj.size());
	    result.compute(sj);
	    return result;
	}

	/**
	 * Makes the weights valid for the given <sj>
	 */
	private void compute(SIRSplitJoin sj) {

	    // fill in the execution count info for this
	    HashMap[] execCount = SIRScheduler.getExecutionCounts(sj);
	    for (int i=0; i<sj.size(); i++) {
		// get the steady-state count
		int[] count = (int[])execCount[1].get(sj.get(i));
		if (count==null) {
		    this.child[i] = 0;
		} else {
		    this.child[i] = count[0];
		}
	    }

	    // infer how many times the splitter, joiner runs
	    int[] splitWeights = sj.getSplitter().getWeights();
	    int[] joinWeights = sj.getJoiner().getWeights();

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
		assert this.splitter * splitWeights[index] == 
                    this.child[index] * ((SIRFilter)sj.get(index)).getPopInt();
	    }
	    // now for joiner
	    if (nullJoin) {
		this.joiner = 0;
	    } else {
		this.joiner = this.child[index] * ((SIRFilter)sj.get(index)).getPushInt() / joinWeights[index];
		// make sure we come out even
		assert this.joiner * joinWeights[index] == 
                    this.child[index] *
                    ((SIRFilter)sj.get(index)).getPushInt();
	    }
	}
    }

    /**
     * Contains information that is relevant to a given filter's
     * inclusion in a fused splitjoin.
     */
    static class SJChildInfo {
	/**
	 * The filter itself.
	 */
	public final SIRFilter filter;

	/**
	 * Peek buffer info.
	 */
	public final BufferInfo peekBuffer;

	/**
	 * Push buffer info.
	 */
	public final BufferInfo pushBuffer;

	public SJChildInfo(SIRFilter filter, BufferInfo peekBuffer, BufferInfo pushBuffer) {
	    this.filter = filter;
	    this.peekBuffer = peekBuffer;
	    this.pushBuffer = pushBuffer;
	}

	/**
	 * Returns fields that should be included as part of this
	 */
	public JFieldDeclaration[] getFields() {
	    JFieldDeclaration[] result = new JFieldDeclaration[6];
	    result[0] = peekBuffer.target;
	    result[1] = peekBuffer.writeIndex;
	    result[2] = peekBuffer.readIndex;
	    result[3] = pushBuffer.target;
	    result[4] = pushBuffer.writeIndex;
	    result[5] = pushBuffer.readIndex;
	    return result;
	}
    }

    static class BufferInfo {
	/**
	 * The actual buffer field.
	 */
	public final JFieldDeclaration target;
    
	/**
	 * Buffer size.
	 */
	public final int size;
    
	/**
	 * Index of the last item that was written to the buffer.
	 */
	public final JFieldDeclaration writeIndex;

	/**
	 * Index of the last item that was read from the buffer.
	 */
	public final JFieldDeclaration readIndex;

	public BufferInfo(JFieldDeclaration target, int size, 
			  JFieldDeclaration readIndex, JFieldDeclaration writeIndex) {
	    this.target = target;
	    this.size = size;
	    this.readIndex = readIndex;
	    this.writeIndex = writeIndex;
	}
    }

    static class Rate {
	public int initPush;
	public int initPop;
	public int initPeek;
	public int push;
	public int pop;
	public int peek;

	public Rate(int initPush, int initPop, int initPeek,
		    int push, int pop, int peek) {
	    this.initPush = initPush;
	    this.initPop = initPop;
	    this.initPeek = initPeek;
	    this.push = push;
	    this.pop = pop;
	    this.peek = peek;
	}
    }

    static class FuseSplitVisitor extends SLIRReplacingVisitor {
    
	SJChildInfo childInfo;

	public FuseSplitVisitor(SJChildInfo childInfo) {
	    this.childInfo = childInfo;
	}

	/**
	 * Visits a push expression -- convert to pushing to array.
	 */
	public Object visitPushExpression(SIRPushExpression oldSelf,
					  CType oldTapeType,
					  JExpression oldArg) {
	    SIRPushExpression self = (SIRPushExpression)super.visitPushExpression(oldSelf, oldTapeType, oldArg);
	    return FuseSplit.makeBufferPush(childInfo.pushBuffer, self.getArg());
	}

	/**
	 * Visits a pop expression.
	 */
	public Object visitPopExpression(SIRPopExpression oldSelf,
					 CType oldTapeType) {
	    SIRPopExpression self = (SIRPopExpression)super.visitPopExpression(oldSelf, oldTapeType);
	    return FuseSplit.makeBufferPop(childInfo.peekBuffer);
	}

	/**
	 * Visits a peek expression - convert to peek in buffer.
	 */
	public Object visitPeekExpression(SIRPeekExpression oldSelf,
					  CType oldTapeType,
					  JExpression oldArg) {
	    SIRPeekExpression self = (SIRPeekExpression)super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
	    return FuseSplit.makeBufferPeek(childInfo.peekBuffer, self.getArg());
	}
    }
}

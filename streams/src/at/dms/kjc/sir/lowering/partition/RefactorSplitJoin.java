package at.dms.kjc.sir.lowering.partition;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import java.util.List;
import java.util.HashMap;

public class RefactorSplitJoin {

    /**
     * Converts <sj> to a pipeline, where the left-most children of
     * the <sj> are the top-most elements of the pipeline.  Initial
     * input is passed through so that lower stages of the pipe can
     * operate on it.
     *
     * Only works on splitjoins that have filters for children, that
     * these filters have an integral pop/push ratio (this could
     * possibly be lifted with more clever scheduling) and that have
     * the same input and output type.  Otherwise this returns the
     * original <sj>.
     *
     * Note that this is an immutable method -- does not modify <sj>
     * or its parent.
     */
    public static SIRStream convertToPipeline(SIRSplitJoin sj) {
	if (sj.getInputType()!=sj.getOutputType()) {
	    return sj;
	}
	// only deal with filters
	for (int i=0; i<sj.size(); i++) {
	    if (sj.get(i) instanceof SIRFilter) {
		// for now, require that the pop/push ratio is an
		// integer.
		if ((((SIRFilter)sj.get(i)).getPopInt() % ((SIRFilter)sj.get(i)).getPushInt()) !=0) {
		    return sj;
		}
	    } else { 
		return sj;
	    }
	}
	// make result pipe
	SIRPipeline pipe = new SIRPipeline(sj.getParent(),
					   sj.getIdent() +"_ToPipe");
	pipe.setInit(SIRStream.makeEmptyInit());
	// clone the children <size> times.  Then, in the i'th child
	// of the resulting pipeline, replace all elements except the
	// i'th child of the splitjoin with an identity.  Set the
	// split weights of children 0..i-1 (inclusive) to the join
	// weights of the original; the rest to the split weights of
	// the original.  Set the join weights of children 0..i
	// (inclusive) to the join rates of the original; set the rest
	// of thqe join weights to the split rates of hte original.
	for (int i=0; i<sj.size(); i++) {
	    SIRSplitJoin child = new SIRSplitJoin(pipe,
						  sj.getIdent() + "_Fiss" + i);
	    child.setInit(SIRStream.makeEmptyInit());
	    // make split weights, join weights
	    JExpression[] jwOrig = sj.getJoiner().getInternalWeights();
	    JExpression[] swOrig = sj.getSplitter().getInternalWeights();
	    JExpression[] jw = new JExpression[sj.size()];
	    JExpression[] sw = new JExpression[sj.size()];
	    // in first splitjoin, do computation of left-most child,
	    // and expand the other children by the <popPushRati>
	    // factor that they need to compute their join weights
	    // later
	    int popPushRatio = ((SIRFilter)sj.get(i)).getPopInt() / ((SIRFilter)sj.get(i)).getPushInt();
	    if (i==0) {
		// add the one doing computation
		child.add(sj.get(i));
		sw[i] = swOrig[i];
		jw[i] = jwOrig[i];
		// add the placeholders -- expanding data to do lower down
		for (int j=i+1; j<sj.size(); j++) {
		    child.add(new SIRIdentity(sj.getInputType()));
		    sw[j] = swOrig[j];
		    jw[j] = new JIntLiteral(popPushRatio * ((JIntLiteral)jwOrig[j]).intValue());
		}
	    } else {
		// add copies for preserving output values computed above
		for (int j=0; j<i; j++) {
		    child.add(new SIRIdentity(sj.getInputType()));
		    sw[j] = jwOrig[j];
		    jw[j] = jwOrig[j];
		}
		// add guy doing computation
		child.add(sj.get(i));
		sw[i] = new JIntLiteral(popPushRatio * ((JIntLiteral)jwOrig[i]).intValue());
		jw[i] = jwOrig[i];
		// add more placeholders
		for (int j=i+1; j<sj.size(); j++) {
		    child.add(new SIRIdentity(sj.getInputType()));
		    sw[j] = new JIntLiteral(popPushRatio * ((JIntLiteral)jwOrig[j]).intValue());
		    jw[j] = new JIntLiteral(popPushRatio * ((JIntLiteral)jwOrig[j]).intValue());
		}
	    }
	    // set splitter, joiner... if <sj>'s splitter is
	    // duplicate, then the first child splitter needs to be
	    // duplicate.  But all subsequent children start with RR.
	    if (i==0 && sj.getSplitter().getType()==SIRSplitType.DUPLICATE) {
		child.setSplitter(SIRSplitter.create(child, SIRSplitType.DUPLICATE, sw));
	    } else {
		child.setSplitter(SIRSplitter.create(child, SIRSplitType.WEIGHTED_RR, sw));
	    }
	    child.setJoiner(SIRJoiner.create(child, SIRJoinType.WEIGHTED_RR, jw));
	    // add to result pipeline
	    pipe.add(child);
	    // fuse child completely
	    SIRStream result = FuseSplit.fuse(child);
	    if (result instanceof SIRPipeline) {
		FusePipe.fuse((SIRPipeline)result);
	    }
	}
	return pipe;
    }

    /**
     * Given a splitjoin <sj> and a partitioning <partition> of its
     * children, returns a new splitjoin with each partition factored
     * into its own child splitjoin.
     *
     * Note that this is an immutable method -- does not modify <sj>
     * or its parent.
     */
    public static SIRSplitJoin addHierarchicalChildren(SIRSplitJoin sj, PartitionGroup partition) {
	// make result
	SIRSplitJoin result = new SIRSplitJoin(sj.getParent(), sj.getIdent() + "_Hier");
	result.setInit(SIRStream.makeEmptyInit());

	// get copy of children and params
	List children = sj.getParallelStreams();
	List params = sj.getParams();
	
	// the new and old weights for the splitter and joiner
	int[] oldSplit=sj.getSplitter().getWeights();
	int[] oldJoin=sj.getJoiner().getWeights();
	JExpression[] newSplit=new JExpression[partition.size()];
	JExpression[] newJoin=new JExpression[partition.size()];

	// for all the partitions...
	for(int i=0;i<partition.size();i++) {
	    int partSize=partition.get(i);
	    if (partSize==1) {
		// if there is only one stream in the partition, then
		// we don't need to do anything; just add the children
		int pos = partition.getFirst(i);
		result.add((SIRStream)children.get(pos), (List)params.get(pos));
		newSplit[i]=new JIntLiteral(oldSplit[pos]);
		newJoin[i]=new JIntLiteral(oldJoin[pos]);
	    } else {
		int sumSplit=0;
		int sumJoin=0;
		// child split and join weights
		JExpression[] childSplit=new JExpression[partSize];
		JExpression[] childJoin=new JExpression[partSize];
		// the child splitjoin
		SIRSplitJoin childSplitJoin=new SIRSplitJoin(sj,
							     sj.getIdent() + "_child" + i);
		childSplitJoin.setInit(SIRStream.makeEmptyInit());

		// move children into hierarchical splitjoin
		for(int k=0,l=partition.getFirst(i);k<partSize;k++,l++) {
		    sumSplit+=oldSplit[l];
		    sumJoin+=oldJoin[l];
		    childSplit[k]=new JIntLiteral(oldSplit[l]);
		    childJoin[k]=new JIntLiteral(oldJoin[l]);
		    childSplitJoin.add((SIRStream)children.get(l), (List)params.get(l));
		}
		// in the case of a duplicate splitter, <create>
		// disregards the weights array and makes them all 1
		childSplitJoin.setSplitter(SIRSplitter.create(childSplitJoin,sj.getSplitter().getType(),childSplit));
		childSplitJoin.setJoiner(SIRJoiner.create(childSplitJoin,sj.getJoiner().getType(),childJoin));

		// update new toplevel splitjoin
		newSplit[i]=new JIntLiteral(sumSplit);
		newJoin[i]=new JIntLiteral(sumJoin);
		result.add(childSplitJoin);
	    }
	}
	// set the splitter and joiner types according to the new weights
	result.setSplitter(SIRSplitter.create(result,sj.getSplitter().getType(),newSplit));
	result.setJoiner(SIRJoiner.create(result,sj.getJoiner().getType(),newJoin));

	// return new sj
	return result;
    }

    /**
     * Mutates <str> into one in which all splitjoins contained in any
     * children are made rectangular and have synchronization after
     * each child.
     */
    public static void addDeepRectangularSyncPoints(SIRStream str) {
	// first lift everything
	Lifter.lift(str);
	// make all splitjoins rectangular and add synchronization
	IterFactory.createIter(str).accept(new EmptyStreamVisitor() {
		public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {
		    super.preVisitSplitJoin(self, iter);
		    self.makeRectangular();
		}
		public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {
		    super.postVisitSplitJoin(self, iter);
		    // don't need to do anything if only one level in <self>
		    if (self.getRectangularHeight()>1) {
			// make partitiongroup, putting each child in its own group
			SIRPipeline synced = addSyncPoints(self, PartitionGroup.createUniformPartition(self.getRectangularHeight()));
			self.getParent().replace(self, synced);
		    }
		}});
	// lift all but sync points we added
	Lifter.liftPreservingSync(str);
    }
    static int ij=0;

    /**
     * Given that all of the children of <sj> are pipelines and that
     * <partition> describes a partitioning for such a pipeline,
     * re-arrange <sj> into a pipeline of several splitjoins, each of
     * which has children corresponding to a segment of <partition>:
     *
     *      |                          |
     *      .                          .
     *    / | \                      / | \ 
     *   |  |  |                     | | |
     *   |  |  |         ===>        \ | /
     *   |  |  |                       .
     *    \ | /                      / | \
     *      .                        | | |
     *      |                        \ | /
     *      |                          .
     *
     * Note that this is an immutable method -- does not modify <sj>
     * or its parent.
     */
    public static SIRPipeline addSyncPoints(SIRSplitJoin sj, PartitionGroup partition) {
	// check what we're getting
	checkSymmetry(sj);
	Utils.assert(partition.getNumChildren()==((SIRPipeline)sj.get(0)).size());

	// get execution counts for <sj>
	HashMap[] sched = SIRScheduler.getExecutionCounts(sj);

	// make result pipeline
	SIRPipeline result = new SIRPipeline(sj.getParent(), 
					     sj.getIdent()+"_par");
	result.setInit(SIRStream.makeEmptyInit());

	for (int i=0; i<partition.size(); i++) {
	    // new i'th splitjoin.  Replace init function in <sj>
	    // before we clone the methods
	    SIRSplitJoin newSJ = new SIRSplitJoin(result, 
						  sj.getIdent()+"_"+i);
	    newSJ.setInit(SIRStream.makeEmptyInit());

	    // new pipe's for the i'th splitjoin
	    for (int j=0; j<sj.size(); j++) {
		SIRPipeline origPipe = (SIRPipeline)sj.get(j);
		// reset init function in origPipe before we clone it
		SIRPipeline pipe = new SIRPipeline(newSJ,
						   origPipe.getIdent()+"_"+i+"_"+j);
		pipe.setInit(SIRStream.makeEmptyInit());

		// add requisite streams to pipe
		for (int k=partition.getFirst(i); k<=partition.getLast(i); k++) {
		    pipe.add(origPipe.get(k), origPipe.getParams(k));
		}
		// add pipe to the sj for the current partition
		newSJ.add(pipe, sj.getParams(j));
		// try lifting in case we can
		Lifter.eliminatePipe(pipe);
	    }

	    // make the splitter and joiner for <newSJ>.  In the end
	    // cases, this is the same as for <sj>; otherwise it's
	    // round robin's according to the steady-state rates of components
	    SIRSplitter split;
	    if (i==0) {
		split = (sj.getSplitter().getType()==SIRSplitType.DUPLICATE ? 
			 SIRSplitter.create(newSJ, SIRSplitType.DUPLICATE, sj.size()) :
			 SIRSplitter.createWeightedRR(newSJ, (JExpression[])sj.getSplitter().getInternalWeights().clone()));
	    } else {
		JExpression[] weights = new JExpression[sj.size()];
		for (int j=0; j<sj.size(); j++) {
		    weights[j] = new JIntLiteral(newSJ.get(j).getPopForSchedule(sched));
		}
		split = SIRSplitter.createWeightedRR(newSJ, weights);
	    }
	    SIRJoiner join;
	    if (i==partition.size()-1) {
		join = SIRJoiner.createWeightedRR(newSJ, (JExpression[])sj.getJoiner().getInternalWeights().clone());
	    } else {
		JExpression[] weights = new JExpression[sj.size()];
		for (int j=0; j<sj.size(); j++) {
		    weights[j] = new JIntLiteral(newSJ.get(j).getPushForSchedule(sched));
		}
		join = SIRJoiner.createWeightedRR(newSJ, weights);
	    }
	    newSJ.setSplitter(split);
	    newSJ.setJoiner(join);

	    // add sj for this partition to the overall pipe
	    result.add(newSJ);
	}

	return result;
    }

    /**
     * Removes all sync points that it can in a structued way in
     * <pipe>.  Note that this might INCREASE the tile count because
     * more joiners are introduced into the graph.  If this is not
     * desired, use only removeMatchingSyncPoints (below).
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean removeSyncPoints(SIRPipeline pipe) {
	boolean madeChange = false;
	for (int i=0; i<pipe.size()-1; i++) {
	    // look for adjacent splitjoins
	    if (pipe.get(i) instanceof SIRSplitJoin && 
		pipe.get(i+1) instanceof SIRSplitJoin) {
		// look for matching weights
		SIRSplitJoin sj1 = (SIRSplitJoin)pipe.get(i);
		SIRSplitJoin sj2 = (SIRSplitJoin)pipe.get(i+1);
		// if only one stream on one side, not clear what we could do
		if (sj1.size()==1 || sj2.size()==1) {
		    continue;
		}
		// different sum of weights, and can't do anything
		// (anything obvious, at least) in a structured way
		int sum1 = sj1.getJoiner().getSumOfWeights();
		int sum2 = sj2.getSplitter().getSumOfWeights();
		if (sum1!=sum2) {
		    continue;
		}
		// get weights
		int[] w1 = sj1.getJoiner().getWeights();
		int[] w2 = sj2.getSplitter().getWeights();
		// keep track of the index of current matching
		// segments in top and bottom streams.  
		int partition = 0;
		// map each child of top and bottom to their partition
		HashMap map1 = new HashMap();
		HashMap map2 = new HashMap();
		// go through weights and assign to different
		// partitions based on where the weights line up
		// evenly.
		// our index in the top and bottom streams
		int i1 = 0, i2 = 0;
		// the sum of weights in the top and bottom streams
		int s1 = 0, s2 = 0;
		do {
		    // increment the lesser weight, assigning nodes to
		    // partitions as we go
		    if (s1<s2) {
			map1.put(sj1.get(i1), new Integer(partition));
			s1 += w1[i1];
			i1++;
		    } else {
			map2.put(sj2.get(i2), new Integer(partition));
			s2 += w2[i2];
			i2++;
		    }
		    // if we found alignment bewteen sums, increment
		    // partition
		    if (s1==s2) {
			partition++;
		    }
		} while (s1!=sum1 || s2!=sum2);
		// if we only ended up with one partition, then
		// there's no sync removal opportunity, so quit
		if (partition==1) {
		    continue;
		}
		// otherwise, we just need to factor the upper and
		// lower splitjoins according to our partitioning, and
		// then the matching rate removal will take care of
		// it.  Only do this if we're actually grouping
		// something; otherwise we're already all set.
		if (partition<sj1.size()) {
		    pipe.set(i, addHierarchicalChildren(sj1, PartitionGroup.createFromAssignments(sj1.getParallelStreams(), map1)));
		}
		if (partition<sj2.size()) {
		    pipe.set(i+1, addHierarchicalChildren(sj2, PartitionGroup.createFromAssignments(sj2.getParallelStreams(), map2)));
		}
	    }
	}
	boolean result = removeMatchingSyncPoints(pipe);
	return result;
    }

    /**
     * Does the opposite transformation of <addSyncPoints> above.  If
     * any two adjacent children in <pipe> are splitjoins where the
     * weights of the upstream joiner exactly match the weights of the
     * downstream joiner, then the splitjoins can be combined into a
     * single splitjoin.  If this is the case, then <pipe> is mutated.
     *
     * This is intended only as a reverse routine for the above
     * sync. addition.  In particular, it doesn't deal with duplicate
     * splitters or 1-way splitters, and it doesn't attempt to
     * "duplicate" or "unroll" whole streams in order for
     * synchronization to match up.
     *
     * This guarantees that the tile count is not increased by the
     * procedure.
     *
     * Returns whether or not any change was made.
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean removeMatchingSyncPoints(SIRPipeline pipe) {
	boolean madeChange = false;
	for (int i=0; i<pipe.size()-1; i++) {
	    // look for adjacent splitjoins
	    if (pipe.get(i) instanceof SIRSplitJoin && 
		pipe.get(i+1) instanceof SIRSplitJoin) {
		// look for matching weights
		SIRSplitJoin sj1 = (SIRSplitJoin)pipe.get(i);
		SIRSplitJoin sj2 = (SIRSplitJoin)pipe.get(i+1);
		// diff number of weights, can't do anything
		if (sj1.size()!=sj2.size() || sj1.size()==1) {
		    continue;
		}
		// can't do it if second splitjoin is a duplicate
		if (sj2.getSplitter().getType()==SIRSplitType.DUPLICATE) {
		    continue;
		}
		// if the splitjoin above has more than one splitjoin
		// component, then this has the potential to add
		// tiles, since the joiners in the splitjoin above
		// will not be collapsed
		int upperSJCount = 0;
		boolean shouldAbort = false;
		for (int j=0; j<sj1.size() && !(shouldAbort); j++) {
		    if (sj1.get(j) instanceof SIRSplitJoin) {
			upperSJCount++;
			if (upperSJCount>1) {
			    shouldAbort = true;
			}
		    }
		}
		if (shouldAbort) {
		    continue;
		}
		// get weights
		int[] w1 = sj1.getJoiner().getWeights();
		int[] w2 = sj2.getSplitter().getWeights();
		// test for equality
		boolean equal = true;
		for (int j=0; j<w1.length; j++) {
		    if (w1[j]!=w2[j]) {
			equal = false;
		    }
		}
		// don't do anything if a weight is unequal
		if (!equal) {
		    continue;
		}
		// otherwise, we have a case where we can combine the
		// streams.
		madeChange = true;
		// make new splitjoin to replace sj1 and sj2
		SIRSplitJoin sj3 =  new SIRSplitJoin(pipe,
						     sj1.getIdent()+"_"+sj2.getIdent());
		sj3.setInit(SIRStream.makeEmptyInit());
		sj3.setSplitter(sj1.getSplitter());
		sj3.setJoiner(sj2.getJoiner());
		// add children
		for (int j=0; j<sj1.size(); j++) {
		    SIRPipeline child = new SIRPipeline(sj1,
							sj1.get(j).getIdent()+"_"+sj2.get(j).getIdent());
		    child.setInit(SIRStream.makeEmptyInit());
		    child.add(sj1.get(j), sj1.getParams(j));
		    child.add(sj2.get(j), sj2.getParams(j));
		    // eliminate pipe's if applicable
		    for (int k=0; k<2; k++) {
			if (child.get(k) instanceof SIRPipeline) {
			    Lifter.eliminatePipe((SIRPipeline)child.get(k));
			}
		    }
		    sj3.add(child);
		}
		// replace sj1 and sj2 with sj3
		pipe.remove(sj1);
		pipe.remove(sj2);
		pipe.add(i, sj3);
		// subtract one from i since we shrunk the size of the
		// pipe and need to reconsider this position above
		i--;
	    }
	}
	return madeChange;
    }

    /**
     * Checks that <sj> has symmetrical pipeline children.
     */
    private static void checkSymmetry(SIRSplitJoin sj) {
	SIRStream child_0 = sj.get(0);
	Utils.assert(child_0 instanceof SIRPipeline);
	
	int size_0 = ((SIRPipeline)child_0).size();
	for (int i=1; i<sj.size(); i++) {
	    SIRStream child_i = sj.get(i);
	    Utils.assert(child_i instanceof SIRPipeline &&
			 ((SIRPipeline)child_i).size() == size_0);
	}
    }

    /**
     * Raises as many children of <sj> as it can into <sj>, using
     * helper functions below.
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean raiseSJChildren(SIRSplitJoin sj) {
	if (sj.getSplitter().getType()==SIRSplitType.DUPLICATE) {
	    return raiseDupDupSJChildren(sj);
	} else {
	    return raiseRRSJChildren(sj);
	}
    }

    /**
     * Given a split-join sj with a duplicate splitter, raise any
     * children of sj that also have duplicate splitters, provided
     * that there is no buffering required for the child's outputs.
     * Returns whether or not any raising was done.
     *
     * Good: (outputs A1, A2, B, B)
     *
     *        |
     *       DUP
     *      /   \               |
     *   DUP     \             DUP
     *  /   \     |          /  |  \
     * A1   A2    B   -->  A1   A2  B
     *  \   /     |          \  |  /
     *  RR(1)    /         WRR(1,1,2)
     *      \   /               |
     *      RR(2)
     *        |
     *
     * Bad: (outputs A1, B, A2, B)
     *
     *        |
     *       DUP
     *      /   \
     *   DUP     \
     *  /   \     |
     * A1   A2    B
     *  \   /     |
     *  RR(1)    /
     *      \   /
     *      RR(1)
     *        |
     *
     * Note that this method MUTATES its argument.
     *
     */
    private static boolean raiseDupDupSJChildren(SIRSplitJoin sj)
    {
	boolean didRaising = false;
        // Check that sj's splitter is duplicate:
        if (sj.getSplitter().getType() != SIRSplitType.DUPLICATE)
            return false;
        // For sanity, confirm that we have a round-robin joiner.
        if (sj.getJoiner().getType() != SIRJoinType.ROUND_ROBIN &&
            sj.getJoiner().getType() != SIRJoinType.WEIGHTED_RR)
            return false;

        // Whee.  Let's look at sj's children:
        for (int index = 0; index < sj.size(); index++)
        {
	    // get these in the loop because they could change as we're adjusting children
	    int[] joinWeights = sj.getJoiner().getWeights();
        
            SIRStream child = sj.get(index);
            // To continue, child must be a splitjoin with a duplicate
            // splitter.
            if (!(child instanceof SIRSplitJoin))
                continue;
            SIRSplitJoin sjChild = (SIRSplitJoin)child;
            if (sjChild.getSplitter().getType() != SIRSplitType.DUPLICATE)
                continue;

            // The useful output rate, for our purposes, is the
            // sum of the output weights of the joiner.
            int outCount = sjChild.getJoiner().getSumOfWeights();
            if (outCount != joinWeights[index])
                continue;

            // Okay, we can raise the child.  This involves setting a new
            // (weighted round robin) joiner, and moving the child's children
            // into sj.  Do the joiner first:
	    didRaising = true;
            JExpression[] oldWeights = sj.getJoiner().getInternalWeights();
            JExpression[] newWeights =
                new JExpression[sj.size() + sjChild.size() - 1];
            JExpression[] childWeights =
                sjChild.getJoiner().getInternalWeights();
            int i;
            for (i = 0; i < index; i++)
                newWeights[i] = oldWeights[i];
            for (int j = 0; j < childWeights.length; i++, j++)
                newWeights[i] = childWeights[j];
            for (int j = 1; j < oldWeights.length - index; i++, j++)
                newWeights[i] = oldWeights[index + j];
            SIRJoiner newJoiner =
                SIRJoiner.create(sj, SIRJoinType.WEIGHTED_RR, newWeights);
            SIRSplitter newSplitter =
                SIRSplitter.create(sj, SIRSplitType.DUPLICATE,
                                   newWeights.length);

            // ...and raise the children.
            while (sjChild.size() > 0)
            {
                SIRStream child2 = sjChild.get(0);
                List params = sjChild.getParams(0);
                sjChild.remove(0);
                sj.add(index, child2, params);
                index++;
            }
            sj.remove(index);
            index--;
            sj.setSplitter(newSplitter);
            sj.setJoiner(newJoiner);
        }

        return didRaising;
    }

    /**
     * In the case of round-robin splitters, performs the opposite
     * transformation as addHierarchicalChildren above.  Lifts a child
     * splitjoin <child_i> of <sj> into sj if the sum of <child_i>'s
     * split weights equals the i'th split weight in <sj>, and if the
     * sum of <child_i>'s join weights equals the i'th join weight in
     * <sj>.
     *
     * Note that this method MUTATES its argument.
     */
    private static boolean raiseRRSJChildren(SIRSplitJoin sj) {
	boolean didRaising = false;
        // Check that sj's splitter is not a duplicate:
        if (sj.getSplitter().getType() == SIRSplitType.DUPLICATE) {
	    return false;
	}
        // For sanity, confirm that we have a round-robin joiner.
        if (sj.getJoiner().getType() != SIRJoinType.ROUND_ROBIN &&
            sj.getJoiner().getType() != SIRJoinType.WEIGHTED_RR) {
            return false;
	}

        // Whee.  Let's look at sj's children:
        for (int index = 0; index < sj.size(); index++) {
	    int splitWeight = sj.getSplitter().getWeight(index);
	    int joinWeight = sj.getJoiner().getWeight(index);
        
            SIRStream child = sj.get(index);
            // To continue, child must be a splitjoin with a round-robin
            if (!(child instanceof SIRSplitJoin))
                continue;
            SIRSplitJoin sjChild = (SIRSplitJoin)child;
            if (sjChild.getSplitter().getType() == SIRSplitType.DUPLICATE)
                continue;

            // The useful rates, for our purposes, is the sum of the
            // splitter and joiner weights
	    int inCount = sjChild.getSplitter().getSumOfWeights();
            int outCount = sjChild.getJoiner().getSumOfWeights();
            if (inCount != splitWeight || outCount != joinWeight)
                continue;

            // Okay, we can raise the child.  This involves setting
            // new (weighted round robin) splitter and joiners, and
            // moving the child's children into sj.  Do the splitters
            // and joiners first:
	    didRaising = true;
            JExpression[] oldWeights, newWeights, childWeights;
            int i;
	    // do splitter
            oldWeights = sj.getSplitter().getInternalWeights();
            newWeights = new JExpression[sj.size() + sjChild.size() - 1];
            childWeights = sjChild.getSplitter().getInternalWeights();
            for (i = 0; i < index; i++)
                newWeights[i] = oldWeights[i];
            for (int j = 0; j < childWeights.length; i++, j++)
                newWeights[i] = childWeights[j];
            for (int j = 1; j < oldWeights.length - index; i++, j++)
                newWeights[i] = oldWeights[index + j];
            SIRSplitter newSplitter =
                SIRSplitter.create(sj, SIRSplitType.WEIGHTED_RR, newWeights);
	    // do joiner
            oldWeights = sj.getJoiner().getInternalWeights();
            newWeights = new JExpression[sj.size() + sjChild.size() - 1];
            childWeights = sjChild.getJoiner().getInternalWeights();
            for (i = 0; i < index; i++)
                newWeights[i] = oldWeights[i];
            for (int j = 0; j < childWeights.length; i++, j++)
                newWeights[i] = childWeights[j];
            for (int j = 1; j < oldWeights.length - index; i++, j++)
                newWeights[i] = oldWeights[index + j];
            SIRJoiner newJoiner =
                SIRJoiner.create(sj, SIRJoinType.WEIGHTED_RR, newWeights);

            // ...and raise the children.
            while (sjChild.size() > 0)
            {
                SIRStream child2 = sjChild.get(0);
                List params = sjChild.getParams(0);
                sjChild.remove(0);
                sj.add(index, child2, params);
                index++;
            }
            sj.remove(index);
            index--;
            sj.setSplitter(newSplitter);
            sj.setJoiner(newJoiner);
        }

        return didRaising;
    }

}

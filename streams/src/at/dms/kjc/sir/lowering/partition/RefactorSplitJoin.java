package at.dms.kjc.sir.lowering.partition;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.Lifter;
import java.util.List;

public class RefactorSplitJoin {
    /**
     * Given a splitjoin <sj> and a partitioning <partition> of its
     * children, mutates <sj> so that all the elements of each
     * partition are factored into their own splitjoin.
     */
    public static void addHierarchicalChildren(SIRSplitJoin sj, PartitionGroup partition) {
	// get copy of children and params
	List children = sj.getParallelStreams();
	List params = sj.getParams();
	// clear old children
	sj.clear();
	
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
		sj.add((SIRStream)children.get(pos), (List)params.get(pos));
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
							     sj.getIdent() + "_child" + i,
							     JFieldDeclaration.EMPTY(),
							     JMethodDeclaration.EMPTY());
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
		sj.add(childSplitJoin);
	    }
	}
	// set the splitter and joiner types according to the new weights
	sj.setSplitter(SIRSplitter.create(sj,sj.getSplitter().getType(),newSplit));
	sj.setJoiner(SIRJoiner.create(sj,sj.getJoiner().getType(),newJoin));
    }

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
     * The returned element is also replaced in the parent (that is,
     * the parent is mutated.)
     */
    public static SIRPipeline addSyncPoints(SIRSplitJoin sj, PartitionGroup partition) {
	// check what we're getting
	checkSymmetry(sj);
	Utils.assert(partition.getNumChildren()==((SIRPipeline)sj.get(0)).size());

	// make result pipeline
	SIRPipeline result = new SIRPipeline(sj.getParent(), 
					     sj.getIdent()+"_par",
					     JFieldDeclaration.EMPTY(),
					     JMethodDeclaration.EMPTY());
	result.setInit(SIRStream.makeEmptyInit());

	for (int i=0; i<partition.size(); i++) {
	    // new i'th splitjoin.  Replace init function in <sj>
	    // before we clone the methods
	    SIRSplitJoin newSJ = new SIRSplitJoin(result, 
						  sj.getIdent()+"_"+i,
						  JFieldDeclaration.EMPTY(),
						  JMethodDeclaration.EMPTY());
	    newSJ.setInit(SIRStream.makeEmptyInit());

	    // new pipe's for the i'th splitjoin
	    for (int j=0; j<sj.size(); j++) {
		SIRPipeline origPipe = (SIRPipeline)sj.get(j);
		// reset init function in origPipe before we clone it
		SIRPipeline pipe = new SIRPipeline(newSJ,
						   origPipe.getIdent()+"_"+i+"_"+j,
						   JFieldDeclaration.EMPTY(),
						   JMethodDeclaration.EMPTY());
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
	    // the template RR splits and joins
	    SIRSplitter split = (i==0 ? 
				 sj.getSplitter() : 
				 SIRSplitter.createUniformRR(newSJ, new JIntLiteral(1)));
	    SIRJoiner join = (i==partition.size()-1 ? 
			      sj.getJoiner() : 
			      SIRJoiner.createUniformRR(newSJ, new JIntLiteral(1)));
	    newSJ.setSplitter(split);
	    newSJ.setJoiner(join);

	    // add sj for this partition to the overall pipe
	    result.add(newSJ);
	}

	// replace in parent
	sj.getParent().replace(sj,result);

	return result;
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
     */
    public static boolean raiseDupDupSJChildren(SIRSplitJoin sj)
    {
	boolean didRaising = false;
        // Check that sj's splitter is duplicate:
        if (sj.getSplitter().getType() != SIRSplitType.DUPLICATE)
            return false;
        // For sanity, confirm that we have a round-robin joiner.
        if (sj.getJoiner().getType() != SIRJoinType.ROUND_ROBIN &&
            sj.getJoiner().getType() != SIRJoinType.WEIGHTED_RR)
            return false;
        int[] joinWeights = sj.getJoiner().getWeights();
        
        // Whee.  Let's look at sj's children:
        for (int index = 0; index < sj.size(); index++)
        {
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
}

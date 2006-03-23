package at.dms.kjc.sir.lowering.partition;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.Lifter;
import java.util.List;

/**
 * This class is for refactoring pipelines.
 *
 * All methods in this class are written in an immutable way.  Calling
 * them does not mutate the arguments, or the parents of the
 * arguments.  (This is to facilitate their use in partitioning.
 *
 * !!! NOTE that this implies that, if you're trying to replace a
 * stream in the stream graph, you'll need to do this manually after
 * return from the method.
 */
public class RefactorPipeline {

    /**
     * Returns a new pipeline that is like <pre>pipe</pre> but replaces
     * children at indices first...last with a pipeline that contains
     * those children.
     */
    public static SIRPipeline addHierarchicalChild(SIRPipeline pipe, int first, int last) {
        // make partition group to represent this partitioning
        int[] group = new int[pipe.size()+first-last];
        for (int i=0; i<first; i++) {
            group[i] = 1;
        }
        group[first] = last-first+1;
        for (int i=first+1; i<pipe.size()+first-last; i++) {
            group[i] = 1;
        }
        // do partitioning
        return addHierarchicalChildren(pipe, PartitionGroup.createFromArray(group));
    }

    /**
     * Given a pipeline <pre>pipe</pre> and a partitioning <pre>partition</pre> of its
     * children, returns a new pipeline that has all the elements of
     * each partition factored into their own pipelines.
     */
    public static SIRPipeline addHierarchicalChildren(SIRPipeline pipe, PartitionGroup partition) {
        // make result
        SIRPipeline result = new SIRPipeline(pipe.getParent(), pipe.getIdent() + "_Hier");
        result.setInit(SIRStream.makeEmptyInit());

        // get copy of list of old children, and parameters passed to them
        List children = pipe.getChildren();
        List params = pipe.getParams();

        // for all the partitions...
        for(int i=0;i<partition.size();i++) {
            int partSize=partition.get(i);
            if (partSize==1) {
                // if there is only one stream in the partition, then
                // we don't need to do anything; just add the child
                int pos = partition.getFirst(i);
                result.add((SIRStream)children.get(pos), (List)params.get(pos));
            } else {
                // the child pipeline
                SIRPipeline childPipe = new SIRPipeline(pipe,
                                                        pipe.getIdent() + "_child" + i);
                childPipe.setInit(SIRStream.makeEmptyInit());
        
                // move children into hierarchical pipeline
                for(int k=0,l=partition.getFirst(i);k<partSize;k++,l++) {
                    childPipe.add((SIRStream)children.get(l), (List)params.get(l));
                }

                // update new toplevel pipeline
                result.add(childPipe);
            }
        }
    
        return result;
    }
}

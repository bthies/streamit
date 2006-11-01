package at.dms.kjc.sir.lowering.partition;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents an interface to the StreamIt compiler that allows the
 * programmer to specify how the partitioning / load balancing is done.
 * 
 * To use the interface, follow these steps:
 * 
 * ------------------
 * 
 * 1. Run the StreamIt compiler once on the input file:
 * 
 * strc MyInput.str
 * 
 * This will produce a "numbered.dot" file which assigns a unique number
 * to each filter and stream container in the graph.  (You can view dot
 * with "dotty numbered.dot" or by converting to postscript, "dot -Tps
 * numbered.dot -o numbered.ps.)
 * 
 * ------------------
 * 
 * 2. Start writing your own class that implements the following
 * function:
 * 
 * public static void manualPartition(SIRStream str) { ... }
 * 
 * Let's say you wrote this in "MyPartitioner.java".
 * 
 * ------------------
 * 
 * 3. Use the API in ManualPartition, given below, to implement your
 * manualPartition function.
 * 
 * The basic idea is that you can lookup any stream based on its number
 * (which you get from the graph).  This will give you back an SIRStream
 * object.  With these objects, you can do the following operations:
 * 
 * - fusion
 *   - collapse a whole stream container into 1
 *   - collapse only certain children of a given container
 * - fission:  split a filter into a splitjoin
 * - refactoring
 *   - create / eliminate hierarchical pipelines or splitjoins
 *   - create / eliminate synchronization points in symmetrical
 *     splitjoins
 * - partitioning
 *   - you can run our own greedy or dynamic-programming partitioner on a
 *     hierarchical unit, asking it to automatically fuse or fiss to a
 *     given number of tiles
 * 
 * There is also a function "printGraph" for printing a new numbered
 * graph, in case you want to check that your transformations are working
 * as expected.  This also lets you see the numbers assigned to
 * newly-created streams.
 * 
 * ------------------
 * 
 * 4. Run your manual partitioning with the following command line:
 * 
 * strc -r4 -manual MyPartitioner MyFile.str
 * 
 * This will call your MyPartitioner.manualPartition function using
 * reflection.  It will not do any automatic partitioning for the given
 * stream.  [This command line also targets a 4x4 Raw machine; you could
 * target other configurations, too.]
 * 
 * ------------------
 * 
 * EXAMPLES.  We have implemented two examples in for our Beamformer
 * benchmark, which is in the following directory of CVS:
 * 
 * streams/apps/benchmarks/beamformer/streamit
 * 
 * The files are called:
 * 
 * MyPartition1.java
 * MyPartition2.java
 * 
 * The first one implements a simple partitioning in which each pipeline
 * is fused, and then the width of a splitjoin is decreased from 4 to 2.
 * The second one is more sophisticated: it adds a synchronization point,
 * collapsing the top of the splitjoin from 12 to 1 and the bottom from
 * 12 to 6.
 */
public class ManualPartition {
    /**
     * Invokes the "manualPartition" method in class specified by
     * KjcOptions.manual.  To be called only by the compiler.
     */
    public static SIRStream doit(SIRStream str) {
        // invoke manual optimization via reflection
        try {
            Class c = Class.forName(KjcOptions.optfile);
            Method manualPartition = c.getMethod("manualPartition", new Class[] { Class.forName("at.dms.kjc.sir.SIRStream") });
            Object result = manualPartition.invoke(null, new Object[] { str });
            if (!(result instanceof SIRStream)) {
                Utils.fail("Manual partitioning failed:  class " + KjcOptions.optfile + " did not return an SIRStream.");
                return null;
            } else {
                return (SIRStream)result;
            }
        } catch (ClassNotFoundException e) {
            Utils.fail("Manual partitioning failed:  can't find class " + KjcOptions.optfile);
        } catch (NoSuchMethodException e) {
            Utils.fail("Manual partitioning failed:  class " + KjcOptions.optfile + " does not contain appropriate manualPartition method.");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Utils.fail("Manual partitioning failed:  class " + KjcOptions.optfile + " threw exception.");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Utils.fail("Manual partitioning failed:  illegal to access class " + KjcOptions.optfile);
        }
        return null;
    }

    /**
     * Outputs numbered dot graph for 'str', by name of
     * 'filename'.dot.
     */
    public static void printGraph(SIRStream str, String filename) {
        NumberDot.printGraph(str, filename);
    }

    /**
     * Returns stream contained within 'str' that has unique id number
     * 'num'.  This is the ID number that appears in numbered graphs.
     */
    public static SIRStream getStream(SIRStream str, int num) {
        return str.getStreamWithNumber(num);
    }

    /**
     * Returns set of all child streams of 'str' (including 'str',
     * possibly) that have a name beginning with a given prefix.
     */
    public static SIRStream[] getStreams(SIRStream str, final String prefix) {
        final ArrayList<SIRStream> result = new ArrayList<SIRStream>();
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void preVisitStream(SIRStream self,
                                           SIRIterator iter) {
                    if (self.getName().startsWith(prefix)) {
                        result.add(self);
                    }
                }
            });
        return result.toArray(new SIRStream[0]);
    }

    /**
     * Returns stream by given <name> that is deep child of 'str'.
     * Returns null if no such stream exists.
     */
    public static SIRStream getStream(SIRStream str, final String name) {
        final ArrayList<SIRStream> result = new ArrayList<SIRStream>();
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void preVisitStream(SIRStream self,
                                           SIRIterator iter) {
                    if (self.getName().equals(name)) {
                        result.add(self);
                    }
                }
            });
        assert result.size()<=1;
        if (result.size()>0) {
            return result.get(0);
        } else {
            return null;
        }
    }

    /**
     * Runs dynamic programming partitioner on 'str', aiming to reduce
     * the number of tiles needed to 'targetTiles'.
     */
    public static SIRStream partition(SIRStream str, int targetTiles) {
        return internalPartition(str, targetTiles, true);
    }

    /**
     * Runs greedy partitioner on 'str', aiming to reduce the number
     * of tiles needed to 'targetTiles'.
     */
    public static SIRStream partitionGreedy(SIRStream str, int targetTiles) {
        return internalPartition(str, targetTiles, false);
    }

    /**
     * Internal partitioning routine.  If 'dp' is true, runs dynamic
     * programming partitioner; otherwise runs greedy partitioner.
     */
    private static SIRStream internalPartition(SIRStream str, int targetTiles, boolean dp) {
        checkNull(str);

        // need to make a wrapper, since DP partitioning does best
        // with pipelines
        str = SIRContainer.makeWrapper(str);

        // Lift filters out of pipelines if they're the only thing in
        // the pipe
        Lifter.lift(str);

        // make work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        int curCount = new GraphFlattener(str).getNumTiles();

        // since we're on Raw, the joiners need tiles
        boolean joinersNeedTiles = true;
        // icode is for cluster
        boolean limitICode = false;
        // assume they want strict number of tiles
        boolean strict = true;

        if (dp) {
            str = new DynamicProgPartitioner(str, work, targetTiles, joinersNeedTiles, limitICode, true).toplevel();
        } else {
            if (curCount < targetTiles) {
                // need fission
                new GreedyPartitioner(str, work, targetTiles, joinersNeedTiles).toplevelFission(curCount);
            } else {
                // need fusion
                new GreedyPartitioner(str, work, targetTiles, joinersNeedTiles).toplevelFusion();
            }
        } 

        // lift the result
        Lifter.lift(str);

        // remove wrapper if possible
        if (str instanceof SIRPipeline && ((SIRPipeline)str).size()==1) {
            str = ((SIRPipeline)str).get(0);
        }

        return str;
    }

    /**
     * Fuses all of 'str' into a single filter.
     */
    public static SIRStream fuse(SIRStream str) {
        checkNull(str);
        return internalPartition(str, 1, true);
    }

    /**
     * Fuses some components of a pipeline together.  The components
     * are specified according to a PartitionGroup, 'partitions'.
     */
    public static SIRStream fuse(SIRPipeline pipeline, PartitionGroup partitions) {
        checkNull(pipeline);
        return FusePipe.fuse(pipeline, partitions);
    }

    /**
     * Fuses some components of a splitjoin together.  The components
     * are specified according to a PartitionGroup, 'partitions'.
     */
    public static SIRStream fuse(SIRSplitJoin splitjoin, PartitionGroup partitions) {
        checkNull(splitjoin);
        return FuseSplit.fuse(splitjoin, partitions);
    }

    /**
     * Fuses any two adjacent FILTERS in 'str' so long as:
     *  - the shared parent of the filter is a pipeline
     *  - the result of fusion will be stateless
     *
     * If a complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     */
    public static SIRStream fusePipelinesOfStatelessFilters(SIRStream str) {
        checkNull(str);
        return FusePipelines.fusePipelinesOfStatelessFilters(str);
    }

    /**
     * Fuses any two adjacent STREAMS in 'str' so long as:
     *  - the shared parent of the filter is a pipeline
     *  - the result of fusion will be stateless
     *
     * If a complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     */
    public static SIRStream fusePipelinesOfStatelessStreams(SIRStream str) {
        checkNull(str);
        return FusePipelines.fusePipelinesOfStatelessStreams(str);
    }

    /**
     * Fuses all adjacent filters whos parent is a pipeline.  If a
     * complete pipeline is fused, then it is treated as a single
     * filter in considering fusion within the pipeline's parent.
     */
    public static SIRStream fusePipelinesOfFilters(SIRStream str) {
        checkNull(str);
        return FusePipelines.fusePipelinesOfFilters(str);
    }

    /**
     * Returns whether or not 'filter' is fissable by the StreamIt
     * compiler.  Currently, we can fiss only "stateless" filters that
     * have no internal fields.
     */
    public static boolean isFissable(SIRFilter filter) {
        checkNull(filter);
        return StatelessDuplicate.isFissable(filter);
    }

    /**
     * Splits 'filter' into a 'reps'-way splitjoin.  This is
     * essentially converting 'filter' to operate in a data-parallel
     * form.  Requires that isFissable('filter') is true.
     */
    public static SIRSplitJoin fission(SIRFilter filter, int reps) {
        checkNull(filter);
        return StatelessDuplicate.doit(filter, reps);
    }
    
    /**
     * Splits 'filter' into a 'reps'-way splitjoin, and divides work
     * among the resulting filters according to 'workRatio'.  For
     * example, if workRatio = {1, 2}, then the second fission product
     * will do twice as much work as the first fission product.
     * Requires that isFissable('filter') is true.
     */
    public static SIRSplitJoin fission(SIRFilter filter, int reps, int[] workRatio) {
        checkNull(filter);
        return StatelessDuplicate.doit(filter, reps, workRatio);
    }
    
    /**
     * Returns a new pipeline that is like 'pipe' but replaces
     * children at indices first...last with a pipeline that contains
     * those children.
     */
    public static SIRPipeline addHierarchicalChild(SIRPipeline pipe, int first, int last) {
        checkNull(pipe);
        return RefactorPipeline.addHierarchicalChild(pipe, first, last);
    }

    /**
     * Given a pipeline 'pipe' and a partitioning 'partition' of its
     * children, returns a new pipeline that has all the elements of
     * each partition factored into their own pipelines.
     */
    public static SIRPipeline addHierarchicalChildren(SIRPipeline pipe, PartitionGroup partitions) {
        checkNull(pipe);
        return RefactorPipeline.addHierarchicalChildren(pipe, partitions);
    }

    /**
     * Given a splitjoin 'sj' and a partitioning 'partition' of its
     * children, returns a new splitjoin with each partition factored
     * into its own child splitjoin.
     */
    public static SIRSplitJoin addHierarchicalChildren(SIRSplitJoin sj, PartitionGroup partition) {
        checkNull(sj);
        SIRSplitJoin result = RefactorSplitJoin.addHierarchicalChildren(sj, partition);
        // need to replace in parent, since method is immutable
        if (sj.getParent()!=null) {
            sj.getParent().replace(sj, result);
        }
        return result;
    }

    /**
     * Given that all of the children of 'sj' are pipelines and that
     * 'partition' describes a partitioning for such a pipeline,
     * re-arrange 'sj' into a pipeline of several splitjoins, each of
     * which has children corresponding to a segment of 'partition':
     *
     *      |                          |
     *      .                          .
     *    / | \                      / | \ 
     *   |  |  |                     | | |
     *   |  |  |         ===&gt;        \ | /
     *   |  |  |                       .
     *    \ | /                      / | \
     *      .                        | | |
     *      |                        \ | /
     *      |                          .
     */
    public static SIRPipeline addSyncPoints(SIRSplitJoin sj, PartitionGroup partitions) {
        checkNull(sj);
        SIRPipeline result = RefactorSplitJoin.addSyncPoints(sj, partitions);
        // need to replace in parent, since method is immutable
        if (sj.getParent()!=null) {
            sj.getParent().replace(sj, result);
        }
        return result;
    }

    /**
     * Removes all synchronization points between child splitjoins in
     * 'pipe'.  Note that this might INCREASE the tile count because
     * more joiners are introduced into the graph.  If this is not
     * desired, use only removeMatchingSyncPoints (below).
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean removeSyncPoints(SIRPipeline pipe) {
        checkNull(pipe);
        return RefactorSplitJoin.removeSyncPoints(pipe);
    }

    /**
     * Does the opposite transformation of 'addSyncPoints' above.  If
     * any two adjacent children in 'pipe' are splitjoins where the
     * weights of the upstream joiner exactly match the weights of the
     * downstream joiner, then the splitjoins can be combined into a
     * single splitjoin.  If this is the case, then 'pipe' is mutated.
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
        checkNull(pipe);
        return RefactorSplitJoin.removeMatchingSyncPoints(pipe);
    }

    /**
     * Raises as many children of 'sj' as it can into 'sj'.  That is,
     * if 'sj' contains some children that are also splitjoins, tries
     * to promote the children's children into direct children of
     * 'sj'.  Attempts both duplicate splitters and roundrobin
     * splitters.
     *
     * Note that this method MUTATES its argument.
     */
    public static boolean raiseSJChildren(SIRSplitJoin sj) {
        checkNull(sj);
        return RefactorSplitJoin.raiseSJChildren(sj);
    }

    /**
     * Tries to convert 'sj' into a pipeline.  If the operation is
     * successful, mutates the parent of 'sj' (if any) in the stream
     * graph to contain the pipeline and returns the new pipeline.
     * Otherwise, does not change anything and returns the original
     * splitjoin.
     */
    public static SIRStream convertToPipeline(SIRSplitJoin sj) {
        checkNull(sj);

        // try converting
        SIRStream str = RefactorSplitJoin.convertToPipeline(sj);

        // if conversion failed, we get back the original
        if (str == sj) {
            return sj;
        }
        // otherwise, we got a pipeline
        SIRPipeline pipe = (SIRPipeline)str;

        // replace <sj> with <pipe>
        if (sj.getParent()!=null) {
            sj.getParent().replace(sj, pipe);
        }

        return pipe;
    }

    /**
     * Exits with nice error if 'str' is null.
     */
    private static void checkNull(SIRStream str) {
        if (str==null) {
            new RuntimeException("Null stream passed to ManualPartition.  You probably tried to \n" + 
                                 "retrieve a numbered stream that is not in the graph.").printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Performs loop unrolling up to 'limit' in all methods of all
     * deep children within 'str'.
     */
    public static void unroll(SIRStream str, int limit) {
        // switch unroll limit
        int oldUnroll = KjcOptions.unroll;
        KjcOptions.unroll = limit;

        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void preVisitStream(SIRStream self,
                                           SIRIterator iter) {
                    // unroll in all methods
                    JMethodDeclaration[] methods = self.getMethods();
                    for (int i=0; i<methods.length; i++) {
                        doUnroll(methods[i]);           }
                }
            });
    
        // restore unroll limit
        KjcOptions.unroll = oldUnroll;
    }

    /**
     * Private method to actually do unrolling on a method.
     */
    private static void doUnroll(JMethodDeclaration method) {
        Unroller unroller;
        do {
            do {
                //System.out.println("Unrolling..");
                unroller = new Unroller(new Hashtable());
                method.accept(unroller);
            } while(unroller.hasUnrolled());
            //System.out.println("Constant Propagating..");
            method.accept(new Propagator(new Hashtable()));
            //System.out.println("Unrolling..");
            unroller = new Unroller(new Hashtable());
            method.accept(unroller);
        } while(unroller.hasUnrolled());
        //System.out.println("Flattening..");
        method.accept(new BlockFlattener());
        //System.out.println("Analyzing Branches..");
        //method.accept(new BranchAnalyzer());
        //System.out.println("Constant Propagating..");
        method.accept(new Propagator(new Hashtable()));
        method.accept(new VarDeclRaiser());
    }

    /**
     * Attempts to break down arrays in all children of 'str' into
     * local variables, and to remove array declarations that are
     * unneeded.
     */
    public static void destroyArrays(SIRStream str) {
        // set option
        boolean oldOpt = KjcOptions.destroyfieldarray;
        KjcOptions.destroyfieldarray = true;

        // try destroying arrays
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRIterator iter) {
                    new ArrayDestroyer().destroyFieldArrays(self);
                    // try to eliminate dead code
                    DeadCodeElimination.doit(self);
                }
            });
        
        // restore option
        KjcOptions.destroyfieldarray = true;
    }

}

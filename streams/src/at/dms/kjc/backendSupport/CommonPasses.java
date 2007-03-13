/**
 * 
 */
package at.dms.kjc.backendSupport;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.ArrayInitExpander;
import at.dms.kjc.sir.lowering.ConstantProp;
import at.dms.kjc.sir.lowering.ConstructSIRTree;
import at.dms.kjc.sir.lowering.EnqueueToInitPath;
import at.dms.kjc.sir.lowering.FieldProp;
import at.dms.kjc.sir.lowering.Flattener;
import at.dms.kjc.sir.lowering.RenameAll;
import at.dms.kjc.sir.lowering.RoundToFloor;
import at.dms.kjc.sir.lowering.SIRScheduler;
import at.dms.kjc.sir.lowering.SimplifyArguments;
import at.dms.kjc.sir.lowering.SimplifyPopPeekPush;
import at.dms.kjc.sir.lowering.StaticsProp;
import at.dms.kjc.sir.lowering.VarDeclRaiser;
import at.dms.kjc.sir.lowering.VectorizeEnable;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.fission.FissionReplacer;
import at.dms.kjc.sir.lowering.partition.ManualPartition;
import at.dms.kjc.sir.lowering.partition.SJToPipe;
import at.dms.kjc.sir.lowering.partition.WorkEstimate;
import at.dms.kjc.sir.lowering.partition.WorkList;
import at.dms.kjc.spacetime.DuplicateBottleneck;
import at.dms.kjc.spacetime.GranularityAdjust;
import at.dms.kjc.spacetime.GreedyBinPacking;
import at.dms.kjc.spacetime.CompCommRatio;
import at.dms.kjc.spacetime.CalculateParams;
import at.dms.kjc.spacetime.StreamlinedDuplicate;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.common.CommonUtils;
/**
 * Common passes, useful in new back ends.
 * @author dimock
 *
 */
public class CommonPasses {
    /** field that may be useful later */
    private Partitioner partitioner;
    
    /** field that may be useful later */
    private WorkEstimate workEstimate;
    
    /** stores pre-modified str for statistics gathering */
    private SIRStream origSTR;
    
    
    
    /**
     * Top level method for executing passes common to some current and all future StreamIt compilers.
     * @param str               SIRStream from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaces        JInterfaceDeclaration[] from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaceTables   SIRInterfaceTable[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param structs           SIRStructure[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param helpers           SIRHelper[] from {@link at.dms.kjc.Kopi2SIR}
     * @param global            SIRGlobal from  {@link at.dms.kjc.Kopi2SIR}
     * @param numCores          Number of {@link at.dms.kjc.backendSupport.ComputeNode}'s to use in partitioning. 
     *
     * @return a slice graph: the optimized program in {@link at.dms.kjc.slicegraph.Slice Slice} representation.
     */
    public Slice[] run(SIRStream str,
            JInterfaceDeclaration[] interfaces,
            SIRInterfaceTable[] interfaceTables,
            SIRStructure[]structs,
            SIRHelper[] helpers,
            SIRGlobal global,
            int numCores) {

        // make arguments to functions be three-address code so can replace max, min, abs
        // and possibly others with macros, knowing that there will be no side effects.
        SimplifyArguments.simplify(str);

        // propagate constants and unroll loop
        System.out.println("Running Constant Prop and Unroll...");
        Set<SIRGlobal> theStatics = new HashSet<SIRGlobal>();
        if (global != null) theStatics.add(global);
        /*Map associatedGlobals =*/ StaticsProp.propagateIntoContainers(str, theStatics);
        ConstantProp.propagateAndUnroll(str, true);
        System.out.println("Done Constant Prop and Unroll...");

        // convert round(x) to floor(0.5+x) to avoid obscure errors
        RoundToFloor.doit(str);
        // add initPath functions for feedback loops
        EnqueueToInitPath.doInitPath(str);

        // construct stream hierarchy from SIRInitStatements
        ConstructSIRTree.doit(str);

        // VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(str);
      
        // do constant propagation on fields
        System.out.println("Running Constant Field Propagation...");
        FieldProp.doPropagate(str);
        System.out.println("Done Constant Field Propagation...");
 
        // expand array initializers loaded from a file
        ArrayInitExpander.doit(str);
        
        // Currently do not support messages in these back ends.
        // TODO: add support for mesages.
        if (SIRPortal.findMessageStatements(str)) {
            throw new AssertionError("Teleport messaging is not yet supported in the Raw backend.");
        }
        
        // I _think_ this is not needed, that parent pointers
        // in SIRStreams can not be incorrect at this point,
        // but leaving from old code.
        if (str instanceof SIRContainer) {
            ((SIRContainer)str).reclaimChildren();
        }
        
	    // if we are gathering statistics, clone the original stream graph
        // so that we can gather statictics on it, not on the modified graph
        if (KjcOptions.stats) {
            origSTR = (SIRStream) ObjectDeepCloner.deepCopy(str);
        }

        // splitjoin optimization on SIR graph can not be
        // done after fusion, and should not affect fusable
        // pipelines, so do it here.
        Lifter.liftAggressiveSync(str);
        
 
        if (KjcOptions.fusion || KjcOptions.dup >= 1 || KjcOptions.noswpipe) {
            // if we are about to fuse filters, we should perform
            // any vectorization now, since vectorization can not work inside
            // fused sections, and vectorization should map pipelines of 
            // stateless filters to pipelines of stateless filters.

            SimplifyPopPeekPush.simplify(str);
            VectorizeEnable.vectorizeEnable(str,null);
        }
        
        //fuse entire str to one filter if possible
        if (KjcOptions.fusion)
            str = FuseAll.fuse(str);
        
        StreamlinedDuplicate duplicate = null;
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        work.printGraph(str, "work_estimate.dot");
        WorkList workList = work.getSortedFilterWork();
        for (int i = 0; i < workList.size(); i++) {
            SIRFilter filter = workList.getFilter(i);
            int filterWork = work.getWork(filter); 
            System.out.println("Sorted Work " + i + ": " + filter + " work " 
                    + filterWork + ", is fissable: " + StatelessDuplicate.isFissable(filter));
        }
        
        //for right now, we use the dup parameter to specify the type 
        //of data-parallelization we are using
        //if we want to enable the data-parallelization
        //stuff from asplos 06, use dup == 1
        if (KjcOptions.dup == 1) {
            DuplicateBottleneck dup = new DuplicateBottleneck();
            dup.percentStateless(str);
            str = FusePipelines.fusePipelinesOfStatelessStreams(str);
            StreamItDot.printGraph(str, "after-fuse-stateless.dot");
            dup.smarterDuplicate(str);
        } else if (KjcOptions.dup == numCores) {
            //if we want to use fine-grained parallelization
            //then set dup to be the number of tiles (cores)
            DuplicateBottleneck dup = new DuplicateBottleneck();
            System.out.println("Fine-Grained Data Parallelism...");
            dup.duplicateFilters(str, numCores);
	}
        
        // If not software-pipelining, don't expect to
        // split the stream graph horizontally so fuse
        // pipelines down into individual filters.
        if (KjcOptions.noswpipe)
            str = FusePipelines.fusePipelinesOfFilters(str);
        
        // Print stream graph after fissing and fusing.
        StreamItDot.printGraph(str, "canonical-graph.dot");

        //Use the partition_greedier flag to enable the Granularity Adjustment
        //phase, it will try to partition more as long as the critical path
        //is not affected (see asplos 06).
        if (KjcOptions.partition_greedier) {
            StreamItDot.printGraph(str, "before-granularity-adjust.dot");
            str = GranularityAdjust.doit(str, numCores);
            StreamItDot.printGraph(str, "after-granularity-adjust.dot");
        }
        
        // vertical fission requested.
        if (KjcOptions.fission > 1) {
            // First transform for linear or statespace
            // representations.
            str = Flattener.doLinearAnalysis(str);
            str = Flattener.doStateSpaceAnalysis(str);

            System.out.println("Running Vertical Fission...");
            FissionReplacer.doit(str, KjcOptions.fission);
            Lifter.lift(str);
            System.out.println("Done Vertical Fission...");
        }
        
        // run user-defined transformations if enabled
        if (KjcOptions.optfile != null) {
            System.err.println("Running User-Defined Transformations...");
            str = ManualPartition.doit(str);
            System.err.println("Done User-Defined Transformations...");
        }
        
    
        StaticsProp.propagateIntoFilters(str,theStatics);

        // If requiested, convert splitjoins (below top level)
        // to pipelines of filters.
        if (KjcOptions.sjtopipe) {
            SJToPipe.doit(str);
        }

        StreamItDot.printGraph(str, "before-partition.dot");

        // VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(str);
        // VarDecl Raise to move peek index up so
        // constant prop propagates the peek buffer index
        // ?? does this really need to be done twice?
        new VarDeclRaiser().raiseVars(str);
        
        // Make sure all variables have different names.
        // This must be run now, later pass rely on distinct names.
        RenameAll.renameOverAllFilters(str);
        
        // Linear Analysis
        LinearAnalyzer lfa = null;
        if (KjcOptions.linearanalysis || KjcOptions.linearpartition) {
            System.out.print("Running linear analysis...");
            lfa = LinearAnalyzer.findLinearFilters(str, KjcOptions.debug, true);
            System.out.println("Done");
            LinearDot.printGraph(str, "linear.dot", lfa);
            LinearDotSimple.printGraph(str, "linear-simple.dot", lfa, null);
            // IterFactory.createFactory().createIter(str).accept(new
            // LinearPreprocessor(lfa));

            // if we are supposed to transform the graph
            // by replacing work functions with their linear forms, do so now
            if (KjcOptions.linearreplacement) {
                System.err.print("Running linear replacement...");
                LinearDirectReplacer.doReplace(lfa, str);
                System.err.println("done.");
                // print out the stream graph after linear replacement
                LinearDot.printGraph(str, "linear-replace.dot", lfa);
            }
        }

        // Raise all pushes, pops, peeks to statement level
        // (several phases above introduce new peeks, pops, pushes
        //  including but not limited to doLinearAnalysis)
        // needed before vectorization
        SimplifyPopPeekPush.simplify(str);

        // If vectorization enabled, create (fused streams of) vectorized filters.
        // the top level compile script should not allow vectorization to be enabled
        // for processor types that do not support short vectors. 
        VectorizeEnable.vectorizeEnable(str,null);
        
        setWorkEstimate(WorkEstimate.getWorkEstimate(str)); 
        
        // get the execution counts from the scheduler
        HashMap[] executionCounts = SIRScheduler.getExecutionCounts(str);
        if (numCores > 1) {

            // Print out computation to communication ratio.
            double CCRatio = CompCommRatio.ratio(str, getWorkEstimate(),
                    executionCounts[1]);
            System.out.println("Comp/Comm Ratio of SIR graph: " + CCRatio);
            // and average max slice size.
            new CalculateParams(str, CCRatio, executionCounts[1]).doit();
        }
        
        // Convert to SliceGraph representation.
        
        // flatten the graph by running (super?) synch removal
        UnflatFilter[] topNodes = null;
        if (!KjcOptions.nopartition) {
            FlattenGraph.flattenGraph(str, lfa, executionCounts);
            topNodes = FlattenGraph.getTopLevelNodes();
            CommonUtils.println_debugging("Top Nodes:");
            for (int i = 0; i < topNodes.length; i++)
                CommonUtils.println_debugging(topNodes[i].toString());
        }    

        Slice[] sliceGraph = null; 
        
        
        setPartitioner(null);
        if (KjcOptions.autoparams) {
            GreedyBinPacking greedyBinPacking = new GreedyBinPacking(str,
                    numCores, getWorkEstimate());
            greedyBinPacking.pack();

            setPartitioner(new AdaptivePartitioner(topNodes, executionCounts,
                    lfa, getWorkEstimate(), numCores, greedyBinPacking));
        }
        if (KjcOptions.nopartition) {
            setPartitioner(new FlattenAndPartition(topNodes,
                    executionCounts, lfa, getWorkEstimate(), numCores));
            ((FlattenAndPartition)getPartitioner()).flatten(str, executionCounts);
        }
        else { 
            setPartitioner(new SimplePartitioner(topNodes,
                    executionCounts, lfa, getWorkEstimate(), numCores));
        }

        sliceGraph = getPartitioner().partition();
        System.out.println("Traces: " + sliceGraph.length);
        getPartitioner().dumpGraph("traces.dot");
        
        // Need to make slice graph, partitioner accessible.
        return sliceGraph;
    }



    /**
     * Allows you to change the value returned by {@link #getPartitioner() getWorkEstimate} after
     * the initial value has been set by {@link #run(SIRStream, JInterfaceDeclaration[], SIRInterfaceTable[], SIRStructure[], SIRHelper[], SIRGlobal, int) run}.
     * @param partitioner the partitioner to set
     */
    private void setPartitioner(Partitioner partitioner) {
        this.partitioner = partitioner;
    }



    /**
     * Get the Partitioner used in {@link #run(SIRStream, JInterfaceDeclaration[], SIRInterfaceTable[], SIRStructure[], SIRHelper[], SIRGlobal, int) run}.
     * @return the partitioner
     */
    public Partitioner getPartitioner() {
        return partitioner;
    }



    /**
     * Allows you to change the value returned by {@link #getWorkEstimate() getWorkEstimate} after
     * the initial value has been set by {@link #run(SIRStream, JInterfaceDeclaration[], SIRInterfaceTable[], SIRStructure[], SIRHelper[], SIRGlobal, int) run}.
     * @param workEstimate the workEstimate to set
     */
    private void setWorkEstimate(WorkEstimate workEstimate) {
        this.workEstimate = workEstimate;
    }



    /**
     * Get the WorkEstimate used in {@link #run(SIRStream, JInterfaceDeclaration[], SIRInterfaceTable[], SIRStructure[], SIRHelper[], SIRGlobal, int) run}.
     * @return the workEstimate
     */
    public WorkEstimate getWorkEstimate() {
        return workEstimate;
    }
    
    
    /**
     * Get the original stream for statistics gathering
     * @return the stream before any graph structure modifications.
     */
    public SIRStream getOrigSTR() {
        return origSTR;
    }
    
}

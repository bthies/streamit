// $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/src/at/dms/kjc/cluster/ClusterBackend.java,v 1.130 2008-01-30 00:38:02 thies Exp $
package at.dms.kjc.cluster; 

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.DumpSymbolicGraph;
import at.dms.kjc.flatgraph.GraphFlattener;
//import at.dms.kjc.flatgraph.*;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.common.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.partition.cache.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import at.dms.kjc.sir.lowering.fusion.*;
import java.util.*;
//import java.io.*;
//import streamit.scheduler2.print.PrintProgram;
//import streamit.scheduler2.*;
//import streamit.scheduler2.constrained.*;

/**
 * Top level of back ends for cluster and uniprocessor based on cluster.
 * For a cluster creates computation nodes connected with pipes.
 * For a uniprocessor creates computation nodes connected by buffers.
 * <br/>
 * Starts with: Standard sequence of optimization passes.
 * Followed by: Dynamic region handling and partitioning, still standard in that it is similar to SpaceDynamic.
 * Followed by: Code generation for cluster or uniprocessor.
 */
public class ClusterBackend {

    //public static Simulator simulator;
    // get the execution counts from the scheduler

    /**
     * Print out some debugging info if true.
     */
    public static boolean debugging = false;

    /**
     * If true have each filter print out each value it is pushing
     * onto its output tape.
     */
    public static final boolean FILTER_DEBUG_MODE = false;

    
    /**
     * Given a flatnode, map to the init execution count.
     */
    static HashMap<FlatNode,Integer> initExecutionCounts;
    /**
     * Given a flatnode, map to steady-state execution count.
     *
     * <br/> Also read in several other modules.
     */
    static HashMap<FlatNode,Integer> steadyExecutionCounts;
    
    /**
     * Given a filter, map to original push/pop/peek rates
     */
    static HashMap<SIRFilter,Integer[]> originalRates;
    
    /**
     * Given a joiner, map to following filter
     */
    static HashMap<SIRJoiner,SIRFilter> joinerWork;

    /**
     * Holds passed structures until they can be handeed off to {@link StructureIncludeFile}.
     */
    private static SIRStructure[] structures;

//    /**
//     * Used to iterate over str structure ignoring flattening.
//     * <br/> Also used in {@link ClusterCodeGenerator} and {@link FlatIrToCluster2}
//     */
//    static streamit.scheduler2.iriter.Iterator topStreamIter; 
    
    /**
     * This keeps the stream graph split up into static rate subgraphs.
     * Generated in ClusterBackend, used in TapeBase.
     */
    static ClusterStreamGraph streamGraph;
    
    /**
     * The cluster backend.
     * Called via reflection.
     */
    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[] structs,
                           SIRHelper[] helpers,
                           SIRGlobal global) {

//        HashMap[] exec_counts1;
//        HashMap[] exec_counts2;

        boolean doCacheOptimization = KjcOptions.cacheopt;
        int code_cache = KjcOptions.l1i * 1024;
        int data_cache = KjcOptions.l1d * 1024;

        // if (debugPrint)
        //    System.out.println("Cluster Backend SIRGlobal: "+global);

        System.out.println("Entry to Cluster Backend"
                           + ((KjcOptions.standalone && KjcOptions.cluster == 1) ? " (uniprocessor)": ""));
        // System.out.println("  --cluster parameter is: "+KjcOptions.cluster);
        // if (debugPrint)
        //     System.out.println("  peekratio is: "+KjcOptions.peekratio);
        // System.out.println("  rename1 is: "+KjcOptions.rename1);
        // System.out.println("  rename2 is: "+KjcOptions.rename2);

        if (debugging) 
        {
            SIRGlobal[] globals;
            if (global != null) {
                globals = new SIRGlobal[1];
                globals[0] = global;
            } else globals = new SIRGlobal[0];
            System.err.println("// str on entry to Cluster backend");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs,globals);
            System.err.println("// END str on entry to Cluster backend");
       }
        structures = structs;

        if (KjcOptions.dynamicRatesEverywhere) {
            // force there to be only 1 static sub-graph by making all
            // rates static for now.
            SIRDynamicRateManager.pushConstantPolicy(1);
        }
    
        // Testing. (new ThreeAddressCode()).threeAddressCode(str);
        
        // make arguments to functions be three-address code so can replace max, min, abs
        // and possibly others with macros, knowing that there will be no side effects.
        SimplifyArguments.simplify(str);
        // Pull pop, push, peek into own statements (must be done eventually before
        // generating C code, and is best done here while type info is still available
        // for tmps.
        SimplifyPopPeekPush.simplify(str);
        
        // Perform propagation on fields from 'static' sections.
        Set<SIRGlobal> statics = new HashSet<SIRGlobal>();
        if (global != null)
            statics.add(global);
        StaticsProp.propagate/*IntoContainers*/(str, statics);

        if (debugging) {
            System.err.println("// str before Constant Prop");
            SIRGlobal[] globals;
            if (global != null) {
                globals = new SIRGlobal[1];
                globals[0] = global;
            } else {
                globals = new SIRGlobal[0];
            }
            SIRToStreamIt.run(str, interfaces, interfaceTables, structs,
                              globals);
            System.err.println("// END str before Constant Prop");
        }

        // propagate constants and unroll loop
        System.err.print("Running Constant Prop and Unroll...");

        // Constant propagate and unroll.
        // Set unrolling factor to <= 4 for loops that don't involve
        //  any tape operations.
        Unroller.setLimitNoTapeLoops(true, 4);
    
        ConstantProp.propagateAndUnroll(str);
//        if (debugPrint) {
//            System.err.println("// str after ConstantProp1");
//            SIRToStreamIt.run(str,interfaces,interfaceTables,structs);
//            System.err.println("// END str after ConstantProp1");
//        }
        System.err.println(" done.");

        // do constant propagation on fields
        System.err.print("Running Constant Field Propagation...");
        ConstantProp.propagateAndUnroll(str, true);
        if (debugging) {
            System.err.println("// str after ConstantProp");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs);
            System.err.println("// END str after ConstantProp");
        }

        // Introduce Multiple Pops where programmer
        // didn't take advantage of them (after parameters are propagated).
        IntroduceMultiPops.doit(str);
        
        // convert round(x) to floor(0.5+x) to avoid obscure errors
        RoundToFloor.doit(str);

        // add initPath functions
        EnqueueToInitPath.doInitPath(str);

        // construct stream hierarchy from SIRInitStatements

        ConstructSIRTree.doit(str);

        //this must be run now, Further passes expect unique names!!!
        RenameAll.renameAllFilters(str);

        //SIRPrinter printer1 = new SIRPrinter();
        //str.accept(printer1);
        //printer1.close();

        //VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(str);


        // expand array initializers loaded from a file
        ArrayInitExpander.doit(str);
        System.err.println(" done.");   // announce end of ConstantProp and Unroll

        //System.err.println("Analyzing Branches..");
        //new BlockFlattener().flattenBlocks(str);
        //new BranchAnalyzer().analyzeBranches(str);

        SIRPortal.findMessageStatements(str);

        // canonicalize stream graph, reorganizing some splits and joins
        Lifter.liftAggressiveSync(str);

        // Unroll and propagate maximally within each (not phased) filter.
        // Initially justified as necessary for IncreaseFilterMult which is
        // now obsolete.
        Optimizer.optimize(str); 
        if (debugging) {
            System.err.println("// str after Optimizer");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs);
            System.err.println("// END str after Optimizer");
        }
 
        // estimate code and local variable size for each filter (and store where???)
        Estimator.estimate(str);

        StreamItDot.printGraph(str, "canonical-graph.dot");

        // gather application-characterization statistics
        if (KjcOptions.stats) {
            StatisticsGathering.doit(str);
        }

        // Flattener is a misnomer here.
        // Rewrite str for linearreplacement, frequencyreplacement,
        // or redundantreplacement
        str = Flattener.doLinearAnalysis(str);
        // statespace.
        str = Flattener.doStateSpaceAnalysis(str);

        // Raise all pushes, pops, peeks to statement level
        // (several phases above introduce new peeks, pops, pushes
        //  including but not limited to doLinearAnalysis)
        if (!(KjcOptions.linearreplacement || KjcOptions.linearreplacement2 ||
              KjcOptions.linearreplacement3 || KjcOptions.atlas || KjcOptions.linearpartition ||
              KjcOptions.frequencyreplacement || KjcOptions.redundantreplacement)) {
            // for now, do not run in combination with linear replacements
            // because some linear expressions do not have type set
            SimplifyPopPeekPush.simplify(str);
        } else if (KjcOptions.vectorize>0) {
            System.err.println("Linear analysis + vectorization unsupported, because 3-address\n" +
                               "code cannot infer types of some expressions created.  Can fix\n" +
                               "by setting types in linear analysis, or by adding type inference.");
            System.exit(1);
        }

        // Code relating to IncreaseFilterMult removed here.
        
        Optimizer.optimize(str);

        // set up for future estimateCode / estimateLocals calls.
        Estimator.estimate(str);

        
        // How many systems will be running this code.
        int hosts = KjcOptions.cluster;
        
        // put markers on operator boundaries before we mung the names
        // too much.
        MarkFilterBoundaries.doit(str);

        StreamItDot.printGraph(str, "before-subgraphs.dot");

        if (debugging) {
            System.err.println("// str before subgraphs");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs);
            System.err.println("// END str before subgraphs");
        }
        streamGraph = new ClusterStreamGraph((new GraphFlattener(str)).top);
        //create the static stream graphs cutting at dynamic rate boundaries
        streamGraph.createStaticStreamGraphs();
        //ClusterStaticStreamGraph[] ssgs = (ClusterStaticStreamGraph [])(streamGraph.getStaticSubGraphs());
        int numSsgs = streamGraph.getStaticSubGraphs().length;
        Utils.setupDotFileName(numSsgs);

        if (numSsgs > 1 && debugging) {
            streamGraph.dumpStaticStreamGraph();
        }

        if (doCacheOptimization) {
            if (numSsgs > 1) {
                System.err.println("Warning: Cache optimizations do not currently work correctly with dynamic rates.");
            }
            if (hosts > 1) {
                System.err.println("Warning: Cache optimizations not designed for executing on multiple cluster nodes.");
            }
        }

        // Cumulative partition information over all SSGs
        HashMap<SIROperator,Integer> partitionMap = new HashMap<SIROperator,Integer>();
        // Cumulative schedule information over all SSGs
        initExecutionCounts = new HashMap<FlatNode,Integer>();
        steadyExecutionCounts = new HashMap<FlatNode,Integer>();
        originalRates = new HashMap<SIRFilter,Integer[]>();
        joinerWork = new HashMap<SIRJoiner, SIRFilter>();

        for (int k = 0; k < numSsgs; k++) {
            ClusterStaticStreamGraph ssg = (ClusterStaticStreamGraph)streamGraph.getStaticSubGraphs()[k];
            if (numSsgs>1) {
                System.out.println("\nCompiling static sub-graph " + k + " (" + ssg.toString() + ")...");
            }

            // Schedule the Static-rate subgraph
            if (!KjcOptions.dynamicRatesEverywhere) {
                ssg.scheduleAndCreateMults();
            }

            if (KjcOptions.optfile != null) {
                System.err.println("Running User-Defined Transformations...");
                ssg.setTopLevelSIR(ManualPartition.doit(ssg.getTopLevelSIR()));
                System.err.println("User-Defined Transformations End.");
            }

            // TODO: interaction of partitioners with StreamGraph: how to
            // partition n ways given m StaticStreamGraph's
        
            System.err.println("Running Partitioning... target number of threads: "+hosts);

            HashSet doNotHorizFuse = ssg.getIOFilters();
            
            StreamItDot.printGraph(ssg.getTopLevelSIR(), 
                                   Utils.makeDotFileName("before-partition", ssg.getTopLevelSIR()));

            Map<SIROperator,Integer> ssgPartitionMap = new HashMap<SIROperator,Integer>();

            //        // sets filter steady counts, which are needed by cache partitioner
            //        filter_steady_counts = ssg.getFlatNodeExecutions(false);

            if ( doCacheOptimization ) {
                ssg.setTopLevelSIR(new CachePartitioner(ssg.getTopLevelSIR(), 
                                                        WorkEstimate.getWorkEstimate(ssg.getTopLevelSIR()), 0, code_cache, 
                                                        data_cache).calcPartitions(ssgPartitionMap));
                //            filter_steady_counts = ssg.getFlatNodeExecutions(false); 
                // Calculate SIRSchedule after increasing multiplicity (Does CachePartitioner do this?)
                StreamItDot.printGraph(ssg.getTopLevelSIR(), 
                                       Utils.makeDotFileName("after-peekmult",ssg.getTopLevelSIR()));
                // code relating to IncreaseFilterMult removed here.
            }

            if ( doCacheOptimization ) {
                // this performs the Cache Aware Fusion (CAF) pass from
                // LCTES'05.  This fuses filters for targeting a uniprocessor.
                System.err.println("Running cache partition 1:");
                ssg.setTopLevelSIR(CachePartitioner.doit(ssg.getTopLevelSIR(), code_cache, data_cache));
            } else if (KjcOptions.partition_dp || 
                       KjcOptions.partition_greedy || 
                       KjcOptions.partition_greedier) {
                System.err.println("Running Partitioning... target number of threads: "+hosts);
                // if these are turned on, then fuse filters as if
                // targeting a multiprocessor
                // TODO: can we use curcount (param2) and targetCount (param3) to make partitioning
                // interact with dynamic regions?
                ssg.setTopLevelSIR(Partitioner.doit(ssg.getTopLevelSIR(), 0, hosts, false, false, false, doNotHorizFuse));
                // from now on, 'hosts' is used to count the number of
                // filters in the graph.  (For if we fused further than
                // needed?)
                hosts = Partitioner.countFilters(ssg.getTopLevelSIR());
            }

            //HashMap partitionMap = new HashMap();
            ssgPartitionMap.clear();

            if ( doCacheOptimization ) {
                System.err.println("Running cache partition 2:");
                ssg.setTopLevelSIR(new CachePartitioner(ssg.getTopLevelSIR(), 
                                                        WorkEstimate.getWorkEstimate(ssg.getTopLevelSIR()), 0, code_cache, 
                                                        data_cache).calcPartitions(ssgPartitionMap));

                // Still needed? 
                ssg.getTopLevelSIR().setParent(null); 
            
                ssg.setTopLevelSIR(new DynamicProgPartitioner(ssg.getTopLevelSIR(),
                                                              WorkEstimate.getWorkEstimate(ssg.getTopLevelSIR()), hosts, false, 
                                                              false, true).calcPartitions(ssgPartitionMap));   
            } else {
                // if mapping to 1 machine, then just map everyone to
                // partition 0 as an optimization (the partitioner would
                // do the same thing, but would take longer)
                if (hosts==1) {
                    mapToPartitionZero(ssg.getTopLevelSIR(), ssgPartitionMap);
                } else if (KjcOptions.partition_greedy || KjcOptions.partition_greedier) {
                    // map the operators into partitions
                    GreedyPartitioner.makePartitionMap(ssg.getTopLevelSIR(), ssgPartitionMap, hosts);
                } else {
                    // Fix up a bug that might be caused by previous 
                    // pass of partitioner
                    ssg.getTopLevelSIR().setParent(null);
                    ssg.setTopLevelSIR(new DynamicProgPartitioner(ssg.getTopLevelSIR(), 
                                                                  WorkEstimate.getWorkEstimate(ssg.getTopLevelSIR()), hosts, false, 
                                                                  false, true).calcPartitions(ssgPartitionMap));   
                    ssg.getTopLevelSIR().setParent(null); 
                }
            }

            /*taticsProp.propagateIntoFilters(ssg.getTopLevelSIR(), statics);*/

            if (KjcOptions.sjtopipe) {
                // may replace SIROperators!
                // might be safer to update this to understand
                // dynamic boundaries, and never update border
                // SIROperators.
                SJToPipe.doit(ssg.getTopLevelSIR());
            }


            //VarDecl Raise to move array assignments down?
            new VarDeclRaiser().raiseVars(ssg.getTopLevelSIR());

//            // debugging:
//            if (debugging) {
//                System.err.println("// str before vector fusing");
//                SIRGlobal[] globals;
//                if (global != null) {
//                    globals = new SIRGlobal[1];
//                    globals[0] = global;
//                } else {
//                    globals = new SIRGlobal[0];
//                }
//                SIRToStreamIt.run(ssg.getTopLevelSIR(), interfaces, interfaceTables, structs,
//                                  globals);
//                System.err.println("// END str before vector fusing");
//            }
            if (KjcOptions.compressed) {
                final int framesize = KjcOptions.frameheight * KjcOptions.framewidth * 4;
                ssg.getTopLevelSIR().accept(new EmptyAttributeStreamVisitor() {
                    @Override
                    public Object visitFilter(SIRFilter self, JFieldDeclaration[] fields, JMethodDeclaration[] methods, JMethodDeclaration init, JMethodDeclaration work, CType inputType, CType outputType) {
                        originalRates.put(self, new Integer[]{
                                self.getPushInt(),
                                self.getPopInt(),
                                self.getPeekInt()});    
                        if (self.getPopInt() > 0) {
                            self.setPop(framesize);
                            self.setPeek(framesize);
                        }
                        if (self.getPushInt() > 0) {
                            self.setPush(framesize);
                        }
                        return self;
                    }
                    
                    @Override
                    public Object visitJoiner(SIRJoiner self, SIRJoinType type, JExpression[] weights) {
                        assert self.getParent() instanceof SIRSplitJoin : self.getParent();
                        SIRSplitJoin parent = (SIRSplitJoin) self.getParent();
                        parent.setJoiner(SIRJoiner.createUniformRR(parent, new JIntLiteral(framesize)));
                        return parent.getJoiner();
                    }
                });
                
                ssg.getTopLevelSIR().accept(new EmptyAttributeStreamVisitor() {
                    @Override
                    public Object visitSplitJoin(SIRSplitJoin self, JFieldDeclaration[] fields, JMethodDeclaration[] methods, JMethodDeclaration init, SIRSplitter splitter, SIRJoiner joiner) {
                        if (!(self.getParent() instanceof SIRPipeline)) {
                            return self;
                        }
                        ListIterator<SIROperator> opIter = self.getParent().getChildren().listIterator();
                        while (opIter.hasNext()) {
                            if (opIter.next() == self && opIter.hasNext()) {
                                SIROperator op = opIter.next();
                                if (op instanceof SIRFilter) {
                                    SIRFilter filter = (SIRFilter) op;
                                    if (Arrays.equals(ClusterBackend.originalRates.get(filter), new Integer[]{1,2,2}) ||
                                        Arrays.equals(ClusterBackend.originalRates.get(filter), new Integer[]{4,8,8})) {
                                        joinerWork.put(self.getJoiner(), filter);
                                        self.getParent().remove(filter);
                                        break;
                                    }
                                } else {
                                    opIter.previous();
                                }
                            }
                        }
                        return self;
                    }
                });
                
                
            }
            
            // Vectorize here w.r.t. partition map, will invalidate schedule.
            ssg.setTopLevelSIR(
            VectorizeEnable.vectorizeEnable(ssg.getTopLevelSIR(),ssgPartitionMap));

            // debugging:
            if (debugging) {
                System.err.println("// str after vector fusing");
                SIRToStreamIt.run(ssg.getTopLevelSIR(), interfaces, interfaceTables, structs);
                System.err.println("// END str after vector fusing");
            }
                
            // Accumulate partition info for later code generation.
            partitionMap.putAll(ssgPartitionMap);
            ClusterFusion.setPartitionMap(partitionMap);

            if (KjcOptions.fusion) {
                FlatNode TopBeforeFusion = ssg.getTopLevel();
                TopBeforeFusion.accept(new ClusterFusion(), new HashSet<FlatNode>(), true);
                // needed before next use of this ssg: ssg.cleanupForFused();
                // streamGraph.cleanupForFused() will clean up all.
            }

            // OK: why set this here?
            Unroller.setLimitNoTapeLoops(false, 0);

            StreamItDot.printGraph(ssg.getTopLevelSIR(), Utils.makeDotFileName("after-partition", ssg.getTopLevelSIR()));

            //VarDecl Raise to move array assignments up
            new VarDeclRaiser().raiseVars(ssg.getTopLevelSIR());

    
            //VarDecl Raise to move peek index up so
            //constant prop propagates the peek buffer index
            new VarDeclRaiser().raiseVars(ssg.getTopLevelSIR());

            // Accumulate schedule info for later code generation.
            // Note that any use of ssg.setTopLevelSIR rewrites nodes and
            // thus invalidates mappings from nodes to execution counts!
            // so put at end.
            initExecutionCounts.putAll(ssg.getExecutionCounts(true));
            steadyExecutionCounts.putAll(ssg.getExecutionCounts(false));
        }  // end of operations on individual Static Stream Graphs

        // Any future reference to a dynamic rate should xlate
        // to a static rate of 1.
        SIRDynamicRateManager.pushConstantPolicy(1);

        if (KjcOptions.fusion) { streamGraph.cleanupForFused(); }

        // The str and flatgraph representations are not allowed to change from
        // here on.
        str = streamGraph.recreateSIR();
        FlatNode strTop = streamGraph.getTopLevel().getTopLevel();
       
        StreamItDot.printGraph(str, "after-subgraphs.dot");

        if (KjcOptions.localstoglobals) {
            ConvertLocalsToFields.doit(str);
        }

        // optionally print a version of the source code that we're
        // sending to the scheduler
        if (KjcOptions.print_partitioned_source) {
            new streamit.scheduler2.print.PrintProgram().printProgram(IterFactory.createFactory().createIter(str)); 
        }

        System.err.println("Done Partitioning...");

        //run constrained scheduler

        //System.err.print("Constrained Scheduler Begin...");

        //new streamit.scheduler2.print.PrintGraph().printProgram(topStreamIter);
        //new streamit.scheduler2.print.PrintProgram().printProgram(topStreamIter);

        //System.err.println(" done.");

        // end constrained scheduler

        if (debugging) {
            System.err.println("// str before Cluster-specific code");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs);
            System.err.println("// END str before Cluster-specific code");
        }

        
        // if going to standalone without fusion, expand the main
        // method names to include the filter name, so we can identify them
        if (KjcOptions.standalone && !KjcOptions.fusion) {
            RenameAll.expandFunctionNames(str);
        }

        // output a .gph graph description file for our collaborators from France
        // (only designed for when there is a single static graph)
        if (numSsgs==1) {
            ClusterStaticStreamGraph ssg = (ClusterStaticStreamGraph)streamGraph.getStaticSubGraphs()[0];
            new DumpSymbolicGraph().dumpGraph(str, strTop, "graph-description.gph", 
                                              ssg.getExecutionCounts(true),
                                              ssg.getExecutionCounts(false));
        }

        ////////////////////////////////////////////////
        // the cluster specific code begins here

        NodeEnumerator.reset();
        NodeEnumerator.init(strTop);    // xlate between node numbers and SIROperators / FlatNodes 

        RegisterStreams.reset();
        RegisterStreams.init(strTop);   // set up NetStreams and associate vectors of NetStreams with node numbers

        /*
        // Remove globals pass is broken in cluster !!!
        if (KjcOptions.removeglobals) { RemoveGlobals.doit(graphFlattener.top); }
        */

        // calculate latency constraints for all portals
        // and save for later query.
        LatencyConstraints.detectConstraints(SIRPortal.getPortals());
    
        /// end output portals

    
        // topological sort or SIROperators for sequencing calls to init.
        DiscoverSchedule d_sched = new DiscoverSchedule();
        d_sched.findPhases(strTop.contents);

        //generating code for partitioned nodes
        //ClusterExecutionCode.doit(top);

        System.err.println("Generating cluster code...");

        StructureIncludeFile.doit(structures, streamGraph); // structs.h

        ClusterCode.generateCode(strTop);   // thread*.cpp

        FusionCode.generateFusionHeader(str, doCacheOptimization); // fusion.h
        GenerateGlobalDotH.generateGlobalDotH(global, helpers); // global.h
        if (KjcOptions.standalone) {
            FusionCode.generateFusionFile(d_sched /*, implicit_mult*/); // fusion.cpp
        } else {
            GenerateMasterDotCpp.generateMasterDotCpp();    // master.cpp
            GenerateGlobalDotCpp.generateGlobalDotCpp(global, helpers); // global.cpp
        }
        GenerateClusterDotH.generateClusterDotH();      // master.h

        GenerateMakefile.generateMakefile(helpers);     // Makefile.cluster
        // not needed in standalone mode
        if (!KjcOptions.standalone) {
            GenerateConfigFile.generateConfigFile();        // cluster-config.txt
        }
        GenerateSetupFile.generateSetupFile();          // cluster-setup.txt

        if (at.dms.kjc.sir.linear.frequency.LEETFrequencyReplacer.didTransform) {
            // if we are referencing FFTW functions, don't bother
            // generating a work estimate because we haven't profiled
            // those functions for an accurate count.  Delete the file
            // to avoid confusion on part of user.
            GenerateWorkEst.clearWorkEst();
        } else if (streamGraph.getStaticSubGraphs().length != 1) {
            // temporary hack: work estimate can not be generated with dynamic rates
            GenerateWorkEst.clearWorkEst();
        } else {
            // otherwise generate file
            GenerateWorkEst.generateWorkEst();        // work-estimate.txt
        }

        System.err.println("Done generating cluster code.");

        /*
        // attempt to find constrained schedule!
        Greedy g = new Greedy(d_sched);

        for (int w = 0; w < 20; w++) {
        int res = g.nextPhase(); // find a constrained phase
        if (res > 0) break;
        }

        for (int w = 0; w < 20; w++) { 
        g.combineInit();
        }
        */
        System.exit(0);
    }
    
    private static void mapToPartitionZero(SIRStream str, final Map<SIROperator,Integer> partitionMap) {
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void preVisitStream(SIRStream self,
                                           SIRIterator iter) {
                    partitionMap.put(self, new Integer(0));
                    if (self instanceof SIRSplitJoin) {
                        partitionMap.put(((SIRSplitJoin)self).getSplitter(), new Integer(0));
                        partitionMap.put(((SIRSplitJoin)self).getJoiner(), new Integer(0));
                    } else if (self instanceof SIRFeedbackLoop) {
                        partitionMap.put(((SIRFeedbackLoop)self).getSplitter(), new Integer(0));
                        partitionMap.put(((SIRFeedbackLoop)self).getJoiner(), new Integer(0));
                    }
                }
            });
    }

//    /**
//     * Just some debugging output.
//     */
//    private static void debugOutput(SIRStream str) {
//        streamit.scheduler2.constrained.Scheduler cscheduler =
//            streamit.scheduler2.constrained.Scheduler.createForSDEP(topStreamIter);
//
//        //cscheduler.computeSchedule(); //"Not Implemented"
//
//        if (!(str instanceof SIRPipeline)) return;
//    
//        int pipe_size = ((SIRPipeline)str).size();
//    
//        SIRFilter first = (SIRFilter)((SIRPipeline)str).get(0);
//        SIRFilter last = (SIRFilter)((SIRPipeline)str).get(pipe_size-1);
//
//        streamit.scheduler2.iriter.Iterator firstIter = 
//            IterFactory.createFactory().createIter(first);
//        streamit.scheduler2.iriter.Iterator lastIter = 
//            IterFactory.createFactory().createIter(last);   
//
//        streamit.scheduler2.SDEPData sdep;
//
//        try {
//            sdep = cscheduler.computeSDEP(firstIter, lastIter);
//
//            if (ClusterBackend.debugPrint) {
//                System.out.println("\n");
//                System.out.println("Source --> Sink Dependency:\n");
//            
//                System.out.println("  Source Init Phases: "+sdep.getNumSrcInitPhases());
//                System.out.println("  Destn. Init Phases: "+sdep.getNumDstInitPhases());
//                System.out.println("  Source Steady Phases: "+sdep.getNumSrcSteadyPhases());
//                System.out.println("  Destn. Steady Phases: "+sdep.getNumDstSteadyPhases());
//            }
//        
//            /*
//              for (int t = 0; t < 20; t++) {
//              int phase = sdep.getSrcPhase4DstPhase(t);
//              int phaserev = sdep.getDstPhase4SrcPhase(t);
//              System.out.println("sdep ["+t+"] = "+phase+
//              " reverse_sdep["+t+"] = "+phaserev);
//              }
//            */
//
//        } catch (streamit.scheduler2.constrained.NoPathException ex) {
//
//        }
//        DoSchedules.findSchedules(topStreamIter, firstIter, str);
//    }
   
}

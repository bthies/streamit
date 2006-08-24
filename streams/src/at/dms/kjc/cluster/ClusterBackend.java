// $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/src/at/dms/kjc/cluster/ClusterBackend.java,v 1.99 2006-08-24 03:31:00 thies Exp $
package at.dms.kjc.cluster;

import at.dms.kjc.common.StructureIncludeFile;
import at.dms.kjc.flatgraph.FlatNode;
//import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.flatgraph.GraphFlattener;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
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

public class ClusterBackend {

    //public static Simulator simulator;
    // get the execution counts from the scheduler

    /**
     * Print out some debugging info if true.
     */
    public static boolean debugPrint = false;

    /**
     * Given a flatnode, map to the init execution count.
     */
    public static HashMap<FlatNode,Integer> initExecutionCounts;
    /**
     * Given a flatnode, map to steady-state execution count.
     *
     * <br/> Also read in several other modules.
     */
    public static HashMap<FlatNode,Integer> steadyExecutionCounts;

//    /**
//     * Map filters (also presumably splitters and joiners) to FlatNodes.
//     * 
//     * <br/> Set up here but used by {@link ClusterExecutionCode}
//     */
//    public static HashMap<SIROperator,FlatNode> filter2Node;

    /**
     * Result of call to {@link SIRScheduler#getExecutionCounts(SIRStream)}.
     * <br/>
     * (not generic since mixing generics and arrays not allowed)
     */
    private static HashMap/*<FlatNode,int[]>*/[] executionCounts;
    
    /**
     * Holds passed structures until they can be handeed off to {@link StructureIncludeFile}.
     */
    private static SIRStructure[] structures;

    
    /**
     * If true have each filter print out each value it is pushing
     *onto its output tape.
     */
    public static boolean FILTER_DEBUG_MODE = false;

    /**
     * Used to iterate over str structure ignoring flattening.
     * <br/> Also used in {@link ClusterCodeGenerator} and {@link FlatIrToCluster2}
     */
    public static streamit.scheduler2.iriter.Iterator topStreamIter; 
    
    /**
     * This provides a way for cache partitioner to access filter execution counts.
     */

    private static HashMap<FlatNode,int[]> filter_steady_counts;

    /**
     * This provides a way for cache partitioner to access filter execution counts.
     */

    public static int getExecCounts(SIROperator oper) {
        int c[] = (int[])filter_steady_counts.get(oper);
        if (c == null) { assert(1 == 0); }
        return c[0];
    }

    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[] structs,
                           SIRHelper[] helpers,
                           SIRGlobal global) {

        HashMap[] exec_counts1;
        HashMap[] exec_counts2;

        boolean doCacheOptimization = KjcOptions.cacheopt;
        int code_cache = 16000;
        int data_cache = 16000;

        // if (debugPrint)
        //    System.out.println("Cluster Backend SIRGlobal: "+global);

        System.out.println("Entry to Cluster Backend"
                           + ((KjcOptions.standalone && KjcOptions.cluster == 1) ? " (uniprocessor)": ""));
        // System.out.println("  --cluster parameter is: "+KjcOptions.cluster);
        // if (debugPrint)
        //     System.out.println("  peekratio is: "+KjcOptions.peekratio);
        // System.out.println("  rename1 is: "+KjcOptions.rename1);
        // System.out.println("  rename2 is: "+KjcOptions.rename2);

        if (debugPrint) {
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
    
        // Introduce Multiple Pops where programmer
        // didn't take advantage of them
        IntroduceMultiPops.doit(str);
        
        // Perform propagation on fields from 'static' sections.
        Set<SIRGlobal> statics = new HashSet<SIRGlobal>();
        if (global != null)
            statics.add(global);
        StaticsProp.propagate(str, statics);

        if (debugPrint) {
            System.err.println("// str after RenameAll and StaticsProp");
            SIRGlobal[] globals;
            if (global != null) {
                globals = new SIRGlobal[1];
                globals[0] = global;
            } else {
                globals = new SIRGlobal[0];
            }
            SIRToStreamIt.run(str, interfaces, interfaceTables, structs,
                              globals);
            System.err.println("// END str after RenameAll and StaticsProp");
        }

        // propagate constants and unroll loop
        System.err.print("Running Constant Prop and Unroll...");

        // Constant propagate and unroll.
        // Set unrolling factor to <= 4 for loops that don't involve
        //  any tape operations.
        Unroller.setLimitNoTapeLoops(true, 4);
    
        ConstantProp.propagateAndUnroll(str);
        System.err.println(" done.");

        // do constant propagation on fields
        System.err.print("Running Constant Field Propagation...");
        ConstantProp.propagateAndUnroll(str, true);
        if (debugPrint) {
            System.err.println("// str after ConstantProp");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs);
            System.err.println("// END str after ConstantProp");
        }

        // convert round(x) to floor(0.5+x) to avoid obscure errors
        RoundToFloor.doit(str);

        // add initPath functions
        EnqueueToInitPath.doInitPath(str);

        // construct stream hierarchy from SIRInitStatements
        ConstructSIRTree.doit(str);

        //this must be run now, FlatIRToC relies on it!!!
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

        // Unroll and propagate maximally within each (not phased) filter.
        // Initially justified as necessary for IncreaseFilterMult which is
        // now obsolete.
        Optimizer.optimize(str); 
        if (debugPrint) {
            System.err.println("// str after Optimizer");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs);
            System.err.println("// END str after Optimizer");
        }
 
        // estimate code and local variable size for each filter (and store where???)
        Estimator.estimate(str);

        // canonicalize stream graph, reorganizing some splits and joins
        Lifter.liftAggressiveSync(str);
        //StreamItDot.printGraph(str, "before-partition.dot");

        // gather application-characterization statistics
        if (KjcOptions.stats) {
            StatisticsGathering.doit(str);
        }

        // Flattener is a misnomer here.
        // rewrite str for linearreplacement, frequencyreplacement
        // redundantreplacement
        str = Flattener.doLinearAnalysis(str);
        // statespace.
        str = Flattener.doStateSpaceAnalysis(str);

        // for scheduler, interpret dynamic rates as a constant.
        // TODO:
        // eventually make a policy that changes dynamic rates <r> to
        // to <r.max> or 1000 if <r.max> is dynamic.
        // for now: 
        // to push MPEG through cluster need size of
        // apps/benchmarks/mpeg2/input/momessage.m2v in bits rounded
        // up to a multiple of 32.
        SIRDynamicRateManager.pushConstantPolicy(1000000);
        //SIRDynamicRateManager.pushIdentityPolicy();
        
        // Calculate SIRSchedule before increasing multiplicity
        StreamItDot.printGraph(str, "before-peekmult.dot");
        exec_counts1 = SIRScheduler.getExecutionCounts(str);

        // sets filter steady counts, which are needed by cache partitioner
        filter_steady_counts = exec_counts1[1]; 

        // Code relating to IncreaseFilterMult removed here.
        
        Optimizer.optimize(str);
        Estimator.estimate(str);

        // should really be hosts -- how many systems
        // will be running this code.
        int hosts = KjcOptions.cluster;

        HashMap<SIROperator,Integer> partitionMap = new HashMap<SIROperator,Integer>();

        if ( doCacheOptimization ) {
            str = new CachePartitioner(str, WorkEstimate.getWorkEstimate(str), 0, code_cache, data_cache).calcPartitions(partitionMap);
        }

        // Calculate SIRSchedule after increasing multiplicity
        StreamItDot.printGraph(str, "after-peekmult.dot");
        exec_counts2 = SIRScheduler.getExecutionCounts(str);

        // sets filter steady counts, which are needed by cache partitioner
        filter_steady_counts = exec_counts2[1]; 

        // code relating to IncreaseFilterMult removed here.
        
        // something to do with profiling?
        MarkFilterBoundaries.doit(str);

        if (KjcOptions.optfile != null) {
            System.err.println("Running User-Defined Transformations...");
            str = ManualPartition.doit(str);
            System.err.println("User-Defined Transformations End.");
            // cluster backend should handle multipops 
            //RemoveMultiPops.doit(str);
        }

        System.err.println("Running Partitioning... target number of threads: "+hosts);

        StreamItDot.printGraph(str, "before-partition.dot");

        if ( doCacheOptimization ) {
            // this performs the Cache Aware Fusion (CAF) pass from
            // LCTES'05.  This fuses filters for targetting a uniprocessor.
            str = CachePartitioner.doit(str, code_cache, data_cache);
        } else if (KjcOptions.partition_dp || 
                   KjcOptions.partition_greedy || 
                   KjcOptions.partition_greedier) {
            // if these are turned on, then fuse filters as if
            // targetting a multiprocessor
            str = Partitioner.doit(str, 0, hosts, false, false);
            // from now on, target however many threads were
            // produced by the partitioner
            KjcOptions.cluster = Partitioner.countFilters(str);
        }

        //HashMap partitionMap = new HashMap();
        partitionMap.clear();

        if ( doCacheOptimization ) {
            str = new CachePartitioner(str, WorkEstimate.getWorkEstimate(str), 0, code_cache, data_cache).calcPartitions(partitionMap);

            str.setParent(null); 
            str = new DynamicProgPartitioner(str, WorkEstimate.getWorkEstimate(str), hosts, false, false).calcPartitions(partitionMap);   

        } else {
            // if mapping to 1 machine, then just map everyone to
            // partition 0 as an optimization (the partitioner would
            // do the same thing, but would take longer)
            if (hosts==1) {
                mapToPartitionZero(str, partitionMap);
            } else {
                // Fix up a bug that might be caused by previous 
                // pass of partitioner
                str.setParent(null); 
                str = new DynamicProgPartitioner(str, WorkEstimate.getWorkEstimate(str), hosts, false, false).calcPartitions(partitionMap);   
            }
        }
    
        System.err.println("Done Partitioning...");

        Unroller.setLimitNoTapeLoops(false, 0);

        if (KjcOptions.sjtopipe) {
            SJToPipe.doit(str);
        }

        StreamItDot.printGraph(str, "after-partition.dot");

        //VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(str);

    
        //VarDecl Raise to move peek index up so
        //constant prop propagates the peek buffer index
        new VarDeclRaiser().raiseVars(str);

        // optionally print a version of the source code that we're
        // sending to the scheduler
        if (KjcOptions.print_partitioned_source) {
            new streamit.scheduler2.print.PrintProgram().printProgram(IterFactory.createFactory().createIter(str)); 
        }

        //run constrained scheduler

        System.err.print("Constrained Scheduler Begin...");

        topStreamIter = IterFactory.createFactory().createIter(str);
        //topStreamIter = IterFactory.createFineGrainedFactory().createIter(str);
        //new streamit.scheduler2.print.PrintGraph().printProgram(topStreamIter);
        //new streamit.scheduler2.print.PrintProgram().printProgram(topStreamIter);

//        if (KjcOptions.debug) {
//            debugOutput(str);
//        }

        System.err.println(" done.");

        // end constrained scheduler

        System.err.println("Flattener Begin...");
        executionCounts = SIRScheduler.getExecutionCounts(str);
        PartitionDot.printScheduleGraph(str, "schedule.dot", executionCounts);
        GraphFlattener graphFlattener = new GraphFlattener(str);
        //graphFlattener.dumpGraph("flatgraph.dot");
        System.err.println("Flattener End.");

        //create the execution counts for other passes
        createExecutionCounts(str, graphFlattener);

        
        if (debugPrint) {
            SIRGlobal[] globals;
            if (global != null) {
                globals = new SIRGlobal[1];
                globals[0] = global;
            } else globals = new SIRGlobal[0];
            System.err.println("// str before Cluster-specific code");
            SIRToStreamIt.run(str,interfaces,interfaceTables,structs,globals);
            System.err.println("// END str before Cluster-specific code");
        }

        
        // if going to standalone without fusion, expand the main
        // method names to include the filter name, so we can identify them
        if (KjcOptions.standalone && !KjcOptions.fusion) {
            RenameAll.expandFunctionNames(str);
        }

        ////////////////////////////////////////////////
        // the cluster specific code begins here

        NodeEnumerator.reset();
        NodeEnumerator.init(graphFlattener);    // xlate between node numbers and SIROperators / FlatNodes 

        RegisterStreams.reset();
        RegisterStreams.init(graphFlattener);   // set up NetStreams and associate vectors of NetStreams with node numbers
        
        /*
        // Remove globals pass is broken in cluster !!!
        if (KjcOptions.removeglobals) { RemoveGlobals.doit(graphFlattener.top); }
        */

        StructureIncludeFile.doit(structures, graphFlattener.top);

        /// start output portals

        SIRPortal portals[] = SIRPortal.getPortals();

        LatencyConstraints.detectConstraints(topStreamIter, portals);
    
        /// end output portals

    
        //VarDecl Raise to move array assignments down?
        new VarDeclRaiser().raiseVars(str);

//        // creating filter2Node
//        filter2Node = new HashMap<SIROperator,FlatNode>();
//        graphFlattener.top.accept(
//                new FlatVisitor() {
//                  public void visitNode(FlatNode node) {
//                      filter2Node.put(node.contents,node);
//                  }
//                },
//                null,
//                true);

        DiscoverSchedule d_sched = new DiscoverSchedule();
        d_sched.findPhases(graphFlattener.top.contents);

        //generating code for partitioned nodes
        //ClusterExecutionCode.doit(graphFlattener.top);

        System.err.println("Generating cluster code...");

        ClusterFusion.setPartitionMap(partitionMap);
        if (KjcOptions.fusion) {
            graphFlattener.top.accept(new ClusterFusion(), new HashSet(), true);
        }

        //ClusterCode.setPartitionMap(partitionMap);    // legacy code now no-op

        ClusterCode.generateCode(graphFlattener.top);   // thread*.cpp

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
        GenerateConfigFile.generateConfigFile();        // cluster-config.txt
        GenerateSetupFile.generateSetupFile();          // cluster-setup.txt

        if (at.dms.kjc.sir.linear.frequency.LEETFrequencyReplacer.didTransform) {
            // if we are referencing FFTW functions, don't bother
            // generating a work estimate because we haven't profiled
            // those functions for an accurate count.  Delete the file
            // to avoid confusion on part of user.
            GenerateWorkEst.clearWorkEst();
        } else {
            // otherwise generate file
            GenerateWorkEst.generateWorkEst();        // work-estimate.txt
        }

        System.err.println(" done.");    

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
    
        System.out.println("Exiting");
        System.exit(0);
    }

    private static void mapToPartitionZero(SIRStream str, final HashMap<SIROperator,Integer> partitionMap) {
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
   
    private static void createExecutionCounts(SIRStream str,
                                              GraphFlattener graphFlattener) {
        // make fresh hashmaps for results
        HashMap[] result = { initExecutionCounts = new HashMap<FlatNode,Integer>(), 
                             steadyExecutionCounts = new HashMap<FlatNode,Integer>()} ;

        // Set up initExecutionCounts, steadyExecutionCounts to map FlatNodes
        // to execution counts stored in executionCounts[0], [1]
        for (int i=0; i<2; i++) {
            for (Iterator it = executionCounts[i].keySet().iterator();
                 it.hasNext(); ){
                SIROperator obj = (SIROperator)it.next();
                int val = ((int[])executionCounts[i].get(obj))[0];
                //System.err.println("execution count for " + obj + ": " + val);
                if (graphFlattener.getFlatNode(obj) != null)
                    result[i].put(graphFlattener.getFlatNode(obj), 
                                  new Integer(val));
            }
        }
    
        //Schedule the new Identities and Splitters introduced by GraphFlattener
        //
        // Go over the needstoBeSched list:
        // If a node's predecessor has an execution count (init / steady) from
        // {init,steady}Executioncounts use that count.  Otherwise use the 
        // nodes' predecessor's execution count from executioncounts[0] / [1]
        //
        // if SIRIdentity node, put this execution count in {init,steady}Executioncounts
        //
        // if SIRSplitter, put in this execution count and push it, (weighted
        // appropriately) to the successors of the splitter.
        //
        // if  SIRJoiner and oldcontents has an execution count in executioncounts[0] / [1]
        // use that execution count.
       
        for(int i=0;i<GraphFlattener.needsToBeSched.size();i++) {
            FlatNode node=(FlatNode)GraphFlattener.needsToBeSched.get(i);
            int initCount=-1;
            if(node.incoming.length>0) {
                if(initExecutionCounts.get(node.incoming[0])!=null)
                    initCount=((Integer)initExecutionCounts.get(node.incoming[0])).intValue();
                if((initCount==-1)&&(executionCounts[0].get(node.incoming[0].contents)!=null))
                    initCount=((int[])executionCounts[0].get(node.incoming[0].contents))[0];
            }
            int steadyCount=-1;
            if(node.incoming.length>0) {
                if(steadyExecutionCounts.get(node.incoming[0])!=null)
                    steadyCount=((Integer)steadyExecutionCounts.get(node.incoming[0])).intValue();
                if((steadyCount==-1)&&(executionCounts[1].get(node.incoming[0].contents)!=null))
                    steadyCount=((int[])executionCounts[1].get(node.incoming[0].contents))[0];
            }
            if(node.contents instanceof SIRIdentity) {
                if(initCount>=0)
                    initExecutionCounts.put(node,new Integer(initCount));
                if(steadyCount>=0)
                    steadyExecutionCounts.put(node,new Integer(steadyCount));
            } else if(node.contents instanceof SIRSplitter) {
                //System.out.println("Splitter:"+node);
                int[] weights=node.weights;
                FlatNode[] edges=node.edges;
                int sum=0;
                for(int j=0;j<weights.length;j++)
                    sum+=weights[j];
                for(int j=0;j<edges.length;j++) {
                    if(initCount>=0)
                        initExecutionCounts.put(edges[j],new Integer((initCount*weights[j])/sum));
                    if(steadyCount>=0)
                        steadyExecutionCounts.put(edges[j],new Integer((steadyCount*weights[j])/sum));
                }
                if(initCount>=0)
                    initExecutionCounts.put(node,new Integer(initCount));
                if(steadyCount>=0)
                    steadyExecutionCounts.put(node,new Integer(steadyCount));
            } else if(node.contents instanceof SIRJoiner) {
                //FlatNode oldNode=graphFlattener.getFlatNode(node.contents);
                if(executionCounts[0].get(node.oldContents)!=null)
                    initExecutionCounts.put(node,new Integer(((int[])executionCounts[0].get(node.oldContents))[0]));
                if(executionCounts[1].get(node.oldContents)!=null)
                    steadyExecutionCounts.put(node,new Integer(((int[])executionCounts[1].get(node.oldContents))[0]));
            }
        }
    }
}

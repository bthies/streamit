package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
//import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
//import at.dms.kjc.sir.lowering.fission.*;
//import at.dms.kjc.lir.*;
import java.io.Serializable;
import java.util.*;
//import java.io.*;
//import at.dms.util.Utils;

/**
 * The main driver class for the StreamIt to RStream 
 * backend.  Used to house the run method that does all the 
 * work of translating the SIR into C code to be inputted to 
 * the RStream compiler.
 * 
 * @author Michael Gordon
 * @author Bill Thies
 * @author Jasper Lin
 * 
 */
public class StrToRStream {
    /** look for do loops in the filter's code **/
    public static final boolean CONVERT_FOR_TO_DO_LOOPS = true;
    /** generate MIV buffer index expressions if possible **/
    public static final boolean GENERATE_MIVS = true;
    /** generate code for superfluous identites and splitter **/
    public static final boolean GENERATE_UNNECESSARY = false;
        
    /** if true, generate a separate peek buffer for peeking filters
     * before execution, restore the peek buffer to the pop buffer,
     * after execution, backup the non-pop'ed items to the peek buffer
     * If false, just move the un-pop'ed items from the end of the pop buffer
     * to the beginning after the filter executes

     * Please note that setting this to true is untested and 
     * produces unnecessary code, in fact I don't know why it is an option
     * I guess I just want to keep around all the code I wrote.
     **/
    public static final boolean HEADER_FOOTER_PEEK_RESTORE = false;

    /** The execution counts from the scheduler: [0] init, [1] steady **/
    public static HashMap[] executionCounts;
    
    /** The structure defined in the application, see SIRStructure **/
    public static SIRStructure[] structures;
    
    //given a flatnode map to the execution count
    public static HashMap<FlatNode, Integer> initExecutionCounts;
    public static HashMap<FlatNode, Integer> steadyExecutionCounts;
    // get the execution counts from the scheduler
    
    /**
     * The entry point of the RStream "backend" for the StreamIt
     * Compiler. Given the SIR representation of the application, 
     * this call will create the c code that will be accepted by the 
     * RStream compiler.  The code will be placed in the current
     * working directory.
     *
     * @param str The stream graph
     * @param interfaces Not used 
     * @param interfaceTables Not used
     * @param structs The structures used in this StreamIt application
     * 
     *
     * 
     */
    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[] structs,
                           SIRHelper[] helpers,
                           SIRGlobal global) {

        System.out.println("Entry to RStream Conversion");

        structures = structs;
    
        // propagate constants and unroll loop
        System.out.println("Running Constant Prop and Unroll...");
        Set<SIRGlobal> theStatics = new HashSet<SIRGlobal>();
        if (global != null) theStatics.add(global);
        Map associatedGlobals = StaticsProp.propagate(str,theStatics);
        ConstantProp.propagateAndUnroll(str,true);
        System.out.println("Done Constant Prop and Unroll.");

        // convert round(x) to floor(0.5+x) to avoid obscure errors
        RoundToFloor.doit(str);

        // add initPath functions
        EnqueueToInitPath.doInitPath(str);

        // construct stream hierarchy from SIRInitStatements
        ConstructSIRTree.doit(str);

        //rename all variables/functions in each filter to be
        //exclusive over all filters...
        //this must be run now, FlatIRToRS relies on it!!!
        RenameAll.renameAllFilters(str);
    
        if (Flattener.hasDynamicRates(str)) {
            Utils.fail("Dynamic rates are not yet supported in the RStream backend.");
        }

        if (SIRPortal.findMessageStatements(str)) {
            Utils.fail("Teleport messaging is not yet supported in the Raw backend.");
        }

        //VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(str);

        // do constant propagation on fields
            System.out.println("Running Constant Field Propagation...");
            FieldProp.doPropagate(str);
            System.out.println("Done Constant Field Propagation...");
            //System.out.println("Analyzing Branches..");
            //new BlockFlattener().flattenBlocks(str);
            //new BranchAnalyzer().analyzeBranches(str);

        // expand array initializers loaded from a file
        ArrayInitExpander.doit(str);

        
        // Raise all pushes, pops, peeks to statement level
        // Several phases above introduce new peeks, pops, pushes
        //  including but not limited to doLinearAnalysis.
        // However, ConvertFileFilters introduces bogus method
        // calls without types that would cause SimplifyPopPeekPush
        // to fail (from JVariableDefinition with a null type).
        SimplifyPopPeekPush.simplify(str);


        //convert all file readers/writers to normal 
        //sirfilters, not predefined filters
        ConvertFileFilters.doit(str);

        Lifter.liftAggressiveSync(str);
        StreamItDot.printGraph(str, "canonical-graph.dot");

        //mgordon: I don't know, I could forsee the linear analysis 
        //and the statespace analysis being useful to Reservoir in the 
        //future...but don't run it now, there is no point.
        //  str = Flattener.doLinearAnalysis(str);
        //      str = Flattener.doStateSpaceAnalysis(str);

        // run user-defined transformations if enabled
        if (KjcOptions.optfile != null) {
            System.err.println("Running User-Defined Transformations...");
            str = ManualPartition.doit(str);
            System.err.println("Done User-Defined Transformations...");
            RemoveMultiPops.doit(str);
        }

        //if Splitjoin to pipe is enabled, run it...
        if (KjcOptions.sjtopipe) {
            SJToPipe.doit(str);
        }

        if (KjcOptions.debug) {
            SIRPrinter printer1 = new SIRPrinter("entry.sir");
            IterFactory.createFactory().createIter(str).accept(printer1);
            printer1.close();
        }

        //VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(str);
    
        //VarDecl Raise to move peek index up so
        //constant prop propagates the peek buffer index
        new VarDeclRaiser().raiseVars(str);

        // vectorize if not generating Reservoir C
        if (KjcOptions.vectorize > 0 && ! KjcOptions.doloops && ! KjcOptions.absarray) {
            VectorizeEnable.vectorizeEnable(str,new HashMap<SIROperator,Integer>());  
            RemoveMultiPops.doit(str);
        }
        
        if (KjcOptions.localstoglobals) {
            ConvertLocalsToFields.doit(str);
        }

        // optionally print a version of the source code that we're
        // sending to the scheduler
        if (KjcOptions.print_partitioned_source) {
            new streamit.scheduler2.print.PrintProgram().printProgram(IterFactory.createFactory().createIter(str));
        }
    
        System.out.println("Flattener Begin...");
        executionCounts = SIRScheduler.getExecutionCounts(str);
    
        /* RMR { scale multiplicity of all streams */
        if (KjcOptions.steadymult > 1) {
            for (Iterator it = executionCounts[1].keySet().iterator(); it.hasNext(); ){
                SIROperator obj = (SIROperator)it.next();
                int val = KjcOptions.steadymult * ((int[])executionCounts[1].get(obj))[0];
                int[] wrapper = { val };
                executionCounts[1].put(obj, wrapper);
            }
        }
        /* } RMR */

        //flatten the "graph"
        GraphFlattener graphFlattener = new GraphFlattener(str);
        System.out.println("Flattener End.");
    
        //create the execution counts for other passes
        createExecutionCounts(str, graphFlattener);
    
        //VarDecl Raise to move array assignments down?
        new VarDeclRaiser().raiseVars(str);
    
        //printer1 = new SIRPrinter("beforecodegen.sir");
        //IterFactory.createFactory().createIter(str).accept(printer1);
        //printer1.close();

        //generate the include file that has the structure definitions
        //and headers for any vector types that appear in the program.
        StructureIncludeFile.doit(structures,
                getVectorHeaderText(graphFlattener.top));
        //generate the c code for the application
        
        GenerateCCode.generate(graphFlattener.top);
        //exit
        System.exit(0);
    }

    /**
     *  Helper function to add everything in a collection to the set
     *
     * @param set The Hashset we want to add *c* to
     * @param c   The collection to add
     *
     */
    public static void addAll(HashSet<Object> set, Collection<Serializable> c) 
    {
        Iterator<Serializable> it = c.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj == null)
                System.out.println("trying to add null obj");
            set.add(obj);
        }
    }

    
    private static void createExecutionCounts(SIRStream str,
                                              GraphFlattener graphFlattener) {
        // make fresh hashmaps for results
        HashMap[] result = { initExecutionCounts = new HashMap<FlatNode, Integer>(), 
                             steadyExecutionCounts = new HashMap<FlatNode, Integer>()} ;
    
        // then filter the results to wrap every filter in a flatnode,
        // and ignore splitters
        for (int i=0; i<2; i++) {
            for (Iterator it = executionCounts[i].keySet().iterator();
                 it.hasNext(); ){
                SIROperator obj = (SIROperator)it.next();
                int val = ((int[])executionCounts[i].get(obj))[0];
                //System.err.println("execution count for " + obj + ": " + val);
                /* This bug doesn't show up in the new version of
                 * FM Radio - but leaving the comment here in case
                 * we need to special case any other scheduler bugsx.
         
                 if (val==25) { 
                 System.err.println("Warning: catching scheduler bug with special-value "
                 + "overwrite in RawBackend");
                 val=26;
                 }
                 if ((i == 0) &&
                 (obj.getName().startsWith("Fused__StepSource") ||
                 obj.getName().startsWith("Fused_FilterBank")))
                 val++;
                */
                if (graphFlattener.getFlatNode(obj) != null) {
                    //System.out.println("Mult (" + i + ") for " + graphFlattener.getFlatNode(obj) + 
                    //             " = " + val);
                    result[i].put(graphFlattener.getFlatNode(obj), 
                                  new Integer(val));
                }
            }
        }
    
        //Schedule the new Identities and Splitters introduced by GraphFlattener
        for(int i=0;i<GraphFlattener.needsToBeSched.size();i++) {
            FlatNode node=GraphFlattener.needsToBeSched.get(i);
            int initCount=-1;
            if(node.incoming.length>0) {
                if(initExecutionCounts.get(node.incoming[0])!=null)
                    initCount=initExecutionCounts.get(node.incoming[0]).intValue();
                if((initCount==-1)&&(executionCounts[0].get(node.incoming[0].contents)!=null))
                    initCount=((int[])executionCounts[0].get(node.incoming[0].contents))[0];
            }
            int steadyCount=-1;
            if(node.incoming.length>0) {
                if(steadyExecutionCounts.get(node.incoming[0])!=null)
                    steadyCount=steadyExecutionCounts.get(node.incoming[0]).intValue();
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
                FlatNode[] edges=node.getEdges();
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
                    result[0].put(node,new Integer(initCount));
                if(steadyCount>=0)
                    result[1].put(node,new Integer(steadyCount));
            } else if(node.contents instanceof SIRJoiner) {
                FlatNode oldNode=graphFlattener.getFlatNode(node.contents);
                if(executionCounts[0].get(node.oldContents)!=null)
                    result[0].put(node,new Integer(((int[])executionCounts[0].get(node.oldContents))[0]));
                if(executionCounts[1].get(node.oldContents)!=null)
                    result[1].put(node,new Integer(((int[])executionCounts[1].get(node.oldContents))[0]));
            }
        }
    
        //  dumpExecutionCounts();
    }
    
    
    public static int getMult(FlatNode node, boolean init) 
    {
        Integer mult;
        if (init) 
            mult = initExecutionCounts.get(node);
        else 
            mult = steadyExecutionCounts.get(node);

        if (mult == null) {
            //System.out.println("** Mult HashMap (" + init + ") does not contain " + node);
            return 0;
        }
    
        return mult.intValue();
    }


    public static void dumpExecutionCounts() 
    {
        for (int i = 0; i < 2; i++) {
            HashMap exeCounts = i == 0 ? 
                initExecutionCounts : steadyExecutionCounts;
            System.out.println("** Execution Counts for " + i);
        
            Iterator nodes = exeCounts.keySet().iterator();
            while (nodes.hasNext()) {
                FlatNode node = (FlatNode)nodes.next();
                System.out.println(node + " = " +  exeCounts.get(node));
            }
        }
    }

    private static String getVectorHeaderText(FlatNode top) {
        // horrible mess for getting typedefs for all vector types in program into vectorTypeDefs
        final Set<String> vectorTypeDefs = new HashSet<String>();
        top.accept(new at.dms.kjc.flatgraph.FlatVisitor() {
            /**
             * Find any CVector type in a JVariableDefinition and
             * update vectorTypeDefs.
             */
            public void visitNode(FlatNode node) {
                if (node.isFilter()) {
                    SIRFilter filter = (SIRFilter)node.contents;
                    for (JFieldDeclaration decl : filter.getFields()) {
                        decl.accept(new SLIREmptyVisitor(){
                            @Override
                            public void visitVariableDefinition(JVariableDefinition self,
                                    int modifiers, CType type, String ident, JExpression expr) {
                                if (type instanceof CVectorType) {
                                    vectorTypeDefs.add(((CVectorType)type).typedefString());
                                }
                            }
                        });
                    }
                    for (JMethodDeclaration m : filter.getMethods()) {
                        m.accept(new SLIREmptyVisitor(){
                            @Override
                            public void visitVariableDefinition(JVariableDefinition self,
                                    int modifiers, CType type, String ident, JExpression expr) {
                                if (type instanceof CVectorType) {
                                    vectorTypeDefs.add(((CVectorType)type).typedefString());
                                }
                            }
                        });
                    }
                }
                        
            } } , new HashSet<FlatNode>(), true);

        StringBuffer sb = new StringBuffer();
        for (String typedef : vectorTypeDefs) {
            sb.append(typedef);
            sb.append('\n');
        }
        sb.append(CVectorType.miscStrings());
        return sb.toString();
    }
}

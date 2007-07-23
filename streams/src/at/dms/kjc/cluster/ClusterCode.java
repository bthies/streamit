
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.Vector;
import at.dms.kjc.common.CodegenPrintWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.io.*;
import at.dms.kjc.common.CommonUtils;

/**
 * This class generates and emits code for each filter, splitter, and joiner
 * in the flat graph.
 */
public class ClusterCode {
    /**
     * If sum of weights  >= this value, do not unroll work fully.
     */
    private static final int MAX_SUM_WEIGHTS_TO_UNROLL = 128;
    /**
     * If not unrolling work completely, by how much do we unroll it?
     */
    private static final int UNROLL_BY = 8;
    
    /**
     * getter? joiners for feedbackloops with >0 enqueues need initialization to
     * perform initial pushes of enqueued values into their input tapes.
     */
    public static final HashSet<SIRJoiner> feedbackJoinersNeedingPrep = new HashSet<SIRJoiner>();
    
    /**
     * Walk the FlatGraph generating code for each filter, splitter, joiner.
     * <br/>
     * All of the code generation work is static (at this level). 
     */
    private static class DoIt implements FlatVisitor {
        /**
         * visitor doing all the work for {@link generateCode(FlatNode)}.
         */
        public void visitNode(FlatNode node) 
        {

            if (node.contents instanceof SIRFilter) {
                SIRFilter filter = (SIRFilter) node.contents;
                if (KjcOptions.compressed && 
                        (Arrays.equals(ClusterBackend.originalRates.get(filter), new Integer[]{1,1,1}) ||
                         Arrays.equals(ClusterBackend.originalRates.get(filter), new Integer[]{4,4,4})) &&
                        !at.dms.kjc.sir.lowering.fission.StatelessDuplicate.hasMutableState(filter)) {
                    JMethodDeclaration oldWork = filter.getWork();
                    String originalName = oldWork.getName();
                    oldWork.setName(originalName + "_ph");
                    
                    JFormalParameter[] formalParams = oldWork.getParameters();
                    int length = formalParams.length;
                    JExpression[] oldWorkParams = new JExpression[length];
                    for (int i = 0; i < length; i++) {
                        oldWorkParams[i] = new JLocalVariableExpression(formalParams[i]); 
                    }
                    
                    JBlock methodCalls;
                    
                    if (ClusterBackend.originalRates.get(filter)[0] == 1) {
                        methodCalls = new JBlock(new JStatement[]{
                                new JExpressionStatement(
                                        new JMethodCallExpression(oldWork.getName(), oldWorkParams)),
                                new JExpressionStatement(
                                        new JMethodCallExpression(oldWork.getName(), oldWorkParams)),
                                new JExpressionStatement(
                                        new JMethodCallExpression(oldWork.getName(), oldWorkParams)),
                                new JExpressionStatement(
                                        new JMethodCallExpression(oldWork.getName(), oldWorkParams))
                        }); 
                    } else {
                        methodCalls = new JBlock(new JStatement[]{
                                new JExpressionStatement(
                                        new JMethodCallExpression(oldWork.getName(), oldWorkParams)),
                        });
                    }
                    
                    JBlock block = new JBlock();
                   
                    JVariableDefinition chunk_size = new JVariableDefinition(CStdType.Integer, "chunk_size");
                    JVariableDefinition header = new JVariableDefinition(CStdType.Integer, "header");
                    JVariableDefinition temp = new JVariableDefinition(CStdType.Integer, "temp");
                    
                    block.addStatement(new JVariableDeclarationStatement(chunk_size));
                    block.addStatement(new JVariableDeclarationStatement(header));
                    block.addStatement(new JVariableDeclarationStatement(temp));
                    
                    block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(temp), 
                            new SIRPopExpression(CStdType.Integer))));
                    
                    block.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(temp), CStdType.Integer)));
                    
                    if (!KjcOptions.blender) {
                        block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                                new JLocalVariableExpression(chunk_size), 
                                new JShiftExpression(null, Constants.OPE_SL, 
                                        new JLocalVariableExpression(temp), new JIntLiteral(24)))));
                    } else {
                        block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                                new JLocalVariableExpression(chunk_size), 
                                new JLocalVariableExpression(temp))));
                    }
                    
                    block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(temp), 
                            new SIRPopExpression(CStdType.Integer))));
                    
                    block.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(temp), CStdType.Integer)));
                       
                    if (!KjcOptions.blender) {
                        block.addStatement(new JExpressionStatement(new JCompoundAssignmentExpression(null, Constants.OPE_PLUS,
                                new JLocalVariableExpression(chunk_size), 
                                new JShiftExpression(null, Constants.OPE_SL,
                                        new JLocalVariableExpression(temp), new JIntLiteral(16)))));
                    } else {
                        block.addStatement(new JExpressionStatement(new JCompoundAssignmentExpression(null, Constants.OPE_PLUS,
                                new JLocalVariableExpression(chunk_size), 
                                new JShiftExpression(null, Constants.OPE_SL,
                                        new JLocalVariableExpression(temp), new JIntLiteral(8)))));
                    }
                    
                    block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(temp), 
                            new SIRPopExpression(CStdType.Integer))));
                    
                    block.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(temp), CStdType.Integer)));
                    
                    if (!KjcOptions.blender) {
                        block.addStatement(new JExpressionStatement(new JCompoundAssignmentExpression(null, Constants.OPE_PLUS,
                                new JLocalVariableExpression(chunk_size), 
                                new JShiftExpression(null, Constants.OPE_SL,
                                        new JLocalVariableExpression(temp), new JIntLiteral(8)))));
                    } else {
                        block.addStatement(new JExpressionStatement(new JCompoundAssignmentExpression(null, Constants.OPE_PLUS,
                                new JLocalVariableExpression(chunk_size), 
                                new JShiftExpression(null, Constants.OPE_SL,
                                        new JLocalVariableExpression(temp), new JIntLiteral(16)))));
                    }
                    
                    block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(temp), 
                            new SIRPopExpression(CStdType.Integer))));
                    
                    block.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(temp), CStdType.Integer)));
                    
                    if (!KjcOptions.blender) {
                        block.addStatement(new JExpressionStatement(new JCompoundAssignmentExpression(null, Constants.OPE_PLUS,
                                new JLocalVariableExpression(chunk_size), 
                                new JLocalVariableExpression(temp))));    
                    } else {
                        block.addStatement(new JExpressionStatement(new JCompoundAssignmentExpression(null, Constants.OPE_PLUS,
                                new JLocalVariableExpression(chunk_size), 
                                new JShiftExpression(null, Constants.OPE_SL,
                                        new JLocalVariableExpression(temp), new JIntLiteral(24)))));
                    }
                    
                    block.addStatement(new JIfStatement(null, new JRelationalExpression(null, Constants.OPE_LT,
                            new JLocalVariableExpression(chunk_size), 
                            new JIntLiteral(8)), KjcOptions.mencoder ? 
                                                        // mencoder only executes one iteration, so make this a return statement
                                                        new JReturnStatement(null, null, null) :
                                                        // otherwise use a continue statement to exit from the loop
                                                        new JContinueStatement(null, null, null), null, null));
                    
                    block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(temp), 
                            new SIRPopExpression(CStdType.Integer))));
                    
                    block.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(temp), CStdType.Integer)));
                    
                    block.addStatement(new JExpressionStatement(new JAssignmentExpression(null,
                            new JLocalVariableExpression(header), 
                            new JShiftExpression(null, Constants.OPE_SL,
                                    new JLocalVariableExpression(temp), new JIntLiteral(8)))));
                    
                    block.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(temp), 
                            new SIRPopExpression(CStdType.Integer))));
                    
                    block.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(temp), CStdType.Integer)));
                    
                    block.addStatement(new JExpressionStatement(new JCompoundAssignmentExpression(null, Constants.OPE_PLUS,
                            new JLocalVariableExpression(header), 
                            new JLocalVariableExpression(temp))));
                    
                    block.addStatement(new JIfStatement(null, new JEqualityExpression(null, true,
                            new JLocalVariableExpression(header), 
                            new JIntLiteral(8)), 
                            new JBlock(new JStatement[]{
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer)),
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer)),
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer)),
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer)),
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer)),
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer)),
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer)),
                                    new JExpressionStatement(new SIRPushExpression(
                                            new SIRPopExpression(CStdType.Integer), CStdType.Integer))                         
                            }), null, null));
                    
                    JBlock outer = new JBlock();
                    
                    block.addStatement(new JForStatement(new JEmptyStatement(), new JEmittedTextExpression(""), new JEmptyStatement(), outer));
                    
                    JVariableDefinition rle_code = new JVariableDefinition(CStdType.Integer, "rle_code");
                    JVariableDefinition skip_code = new JVariableDefinition(CStdType.Integer, "skip_code");
                    
                    outer.addStatement(new JVariableDeclarationStatement(rle_code));
                    outer.addStatement(new JVariableDeclarationStatement(skip_code));
                    
                    outer.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(skip_code), 
                            new SIRPopExpression(CStdType.Integer))));
                    
                    outer.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(skip_code), CStdType.Integer)));
                    
                    outer.addStatement(new JIfStatement(null, new JEqualityExpression(null, true,
                            new JLocalVariableExpression(skip_code), 
                            new JIntLiteral(0)), new JReturnStatement(null, null, null), null, null));
                    
                    outer.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(rle_code),
                            new SIRPopExpression(CStdType.Integer))));
                    
                    outer.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(rle_code), CStdType.Integer)));
                    
                    JBlock inner = new JBlock();
                    
                    outer.addStatement(new JWhileStatement(null, new JEqualityExpression(null, false,
                            new JLocalVariableExpression(rle_code), new JIntLiteral(255)), 
                            inner, null));
                    
                    inner.addStatement(new JIfStatement(null, new JEqualityExpression(null, true,
                            new JLocalVariableExpression(rle_code), new JIntLiteral(0)), 
                            new JExpressionStatement(new SIRPushExpression(
                                new SIRPopExpression(CStdType.Integer), CStdType.Integer)), 
                            new JIfStatement(null, new JRelationalExpression(null, Constants.OPE_GE,
                                new JLocalVariableExpression(rle_code), new JIntLiteral(128)), 
                                methodCalls,
                                new JWhileStatement(null, new JRelationalExpression(null, Constants.OPE_GT,
                                    new JPostfixExpression(Constants.OPE_POSTDEC, new JLocalVariableExpression(rle_code)), 
                                    new JIntLiteral(0)),
                                    methodCalls, null), 
                                null), 
                            null));
                    
                    inner.addStatement(new JExpressionStatement(new JAssignmentExpression(null, 
                            new JLocalVariableExpression(rle_code),
                            new SIRPopExpression(CStdType.Integer))));
    
                    inner.addStatement(new JExpressionStatement(new SIRPushExpression( 
                            new JLocalVariableExpression(rle_code), CStdType.Integer)));
                    
                    JMethodDeclaration newWork = new JMethodDeclaration(CStdType.Void, originalName, 
                            oldWork.getParameters(), block);

                    filter.setWork(newWork);
                    filter.addMethod(oldWork);
                } /* else if (KjcOptions.compressed && 
                            !at.dms.kjc.sir.lowering.fission.StatelessDuplicate.hasMutableState(filter)) {
                    int push = ClusterBackend.originalRates.get(filter)[0];
                    int pop = ClusterBackend.originalRates.get(filter)[1];
                    int peek = ClusterBackend.originalRates.get(filter)[2];
                    
                    if (push == pop && peek % pop == 0) {
                        // find fields tht need to be included in checkpoint
                        DetectConst.detect(filter);
                        
                        generateSlidingWindow(node, push, peek / pop);
                        return;
                    }
                } */
                // find fields tht need to be included in checkpoint
                DetectConst.detect(filter);
                // generate code for a filter.
                if (KjcOptions.mencoder && ClusterBackend.steadyExecutionCounts.get(node) == 1) {
                    FlatIRToCluster.generateArrayCode(filter);
                } else if (KjcOptions.blender || KjcOptions.mencoder) {
                    FlatIRToCluster.generateDirectArrayCode(filter);
                } else {
                    FlatIRToCluster.generateCode(filter);
                }
                
                // attempt to clean up program as generating code was commented out,
                // left it commented out.
                //((SIRFilter)node.contents).setMethods(JMethodDeclaration.EMPTY());
            } else 
            if (node.contents instanceof SIRSplitter) {
                // generate code for a splitter
                generateSplitter(node);
            } else 
            if (node.contents instanceof SIRJoiner) {
                // generate code for a joiner
                generateJoiner(node);
            }
        }
        
    }  // end class DoIt

    /**
     *  Generate and emit code for all nodes in FlatGraph
     *  @param topLevel the entry point to the FlatGraph of the program
     */
    public static void generateCode(FlatNode topLevel) 
    {
        topLevel.accept(new DoIt(), new HashSet<FlatNode>(), true);
    }

    /**
     * Generate code to copy an element from one tape to another, advancing both tapes.
     * @param p     : a CodegenPrintWriter to collect / emit the generated code.
     * @param from  : tape being popped from
     * @param to    : tape being pushed to
     */
    private static void printCopyTapeElement(CodegenPrintWriter p, Tape from, Tape to) {
        if (to instanceof TapeDynrate) {
            p.print(to.getPushName() + "(");
            p.print("reinterpret_cast<" + ((TapeDynrate)(to)).getTypeCastName() + ">(");
            p.println(from.getPopName() + "()));");
        } else {
            p.print(to.getPushName() + "(");
            p.println(from.getPopName() + "());");
        }
    }

    /**
     *  Generate and emit code for a splitter.
     * @param node FlatNode for the splitter to generate code for.
     */

    public static void generateSplitter(FlatNode node) {
        
        SIRSplitter splitter = (SIRSplitter)node.contents;

        if (splitter.getSumOfWeights() == 0) {
            // The splitter is not doing any work
        
            return;
        }

        int init_counts, steady_counts;

        Integer init_int = ClusterBackend.initExecutionCounts.get(node);
        if (init_int==null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        steady_counts = ClusterBackend.steadyExecutionCounts.get(node).intValue();

        CType baseType = CommonUtils.getBaseType(CommonUtils.getOutputType(node));
        int thread_id = NodeEnumerator.getSIROperatorId(node.contents);

        List<Tape> in_v = RegisterStreams.getNodeInStreams(node.contents);
        Tape in = in_v.get(0);
        List<Tape> out = RegisterStreams.getNodeOutStreams(node.contents);
    
        CodegenPrintWriter p = null;
        try {
            p = new CodegenPrintWriter(new FileWriter("thread"+thread_id+".cpp"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ClusterCodeGenerator gen = new ClusterCodeGenerator(splitter, new JFieldDeclaration[0]);

//        // debugging only
//        System.err.print("(Splitter");
//        if (node.incoming != null && node.incoming.length > 0 && node.incoming[0].contents instanceof SIRFilter) {
//            System.err.print (" splitter_" + ((SIRFilter)(node.incoming[0].contents)).getName());
//        } else {
//            System.err.print(" splitter_" + node.contents.toString());
//        }
//        System.err.print(" " + init_counts);
//        System.err.print(" " + steady_counts);
//        System.err.println(")");
//        
        // comment in output file
        p.println("// init counts: "+init_counts+" steady counts: "+steady_counts); 
        p.newLine();

        //  +=============================+
        //  | Preamble                    |
        //  +=============================+     
        FlatIRToCluster f2c = new FlatIRToCluster(p);
        gen.generatePreamble(f2c,p);

//        p.println("void save_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}");
//        p.println("void load_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}");
//        p.newLine();
//
//	p.println("void save_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
//	p.println("void load_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
//        p.newLine();
//

        //  +=============================+
        //  | Splitter Push               |
        //  +=============================+

        int sum_of_weights = splitter.getSumOfWeights();
        // declare any external data structures needed
        // for edges
        p.print(in.downstreamDeclarationExtern());
        for (Tape outTape : out) {
            if (outTape != null) {
                p.print (outTape.upstreamDeclarationExtern());
            }
        }

        // defines push routine for cluster tapes
        // pushing an item (DUPLICATE) or sum_of_weight items
        // (ROUNDROBIN) causes a datum to be pushed all the
        // way throuh the splitter onto an output tape or tapes.
        p.print(in.downstreamDeclaration());
        // For cluster edges this defined a "pop" routine that I hope very much
        // is not used!
        for (Tape outTape : out) {
            if (outTape != null) {
                p.print (outTape.upstreamDeclaration());
            }
        }

        //  +=============================+
        //  | Splitter Work               |
        //  +=============================+

        p.newLine();

        p.print("void __splitter_"+thread_id+"_work(int ____n) {\n");
        p.print("  for (;____n > 0; ____n--) {\n");
    
        // work function for DUPLICATE:  pop into tmp, push to all outputs
        if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {
            p.print("  " + CommonUtils.declToString(baseType, "tmp", true) + ";\n");
            p.print("  " + in.assignPopToVar("tmp") + ";\n");

            for (Tape s : out) {
                if (s != null) {
                    p.print("  " + s.getPushName() + "(tmp);\n");
                }
            }
        
        } else if (splitter.getType().equals(SIRSplitType.ROUND_ROBIN) ||
                   splitter.getType().equals(SIRSplitType.WEIGHTED_RR)) {

            int sum = splitter.getSumOfWeights();
            // work function for ROUND_ROBIN: copy input elements to correct outputs

            // Depending on total weight unroll fully or by some constant factor.
            if (sum > MAX_SUM_WEIGHTS_TO_UNROLL) {
                // big weight do not unroll everything!!
                for (int i = 0; i < out.size(); i++) {
                    int num = splitter.getWeight(i);
                    Tape s = out.get(i);      
                    if (s == null) continue;

                    if (num/UNROLL_BY > 0) {
                        p.print("  for (int __k = 0; __k < "+ (num/UNROLL_BY) +"; __k++) {\n");
            
                        for (int y = 0; y < UNROLL_BY; y++) {
                            printCopyTapeElement(p,in,s);
                        }
                        p.print("  }\n");
                    }
                    
                    int rem = num % UNROLL_BY;
                    for (int y = 0; y < rem; y++) {
                        printCopyTapeElement(p,in,s);
                    }
                }
            } else {
                // weight small enough to unroll everything.
                for (int i = 0; i < out.size(); i++) {
                    int num = splitter.getWeight(i);
                    Tape s = out.get(i);      
                    if (s == null) continue;
                    
                    for (int y = 0; y < num; y++) {
                        printCopyTapeElement(p,in,s);
                    }
                }
            }
        }

        p.print("  }\n");
        p.print("}\n");
        p.newLine();

        //  +=============================+
        //  | Run Function                |
        //  +=============================+

        Vector<String> run = gen.generateRunFunction(null, "__splitter_"
                + thread_id + "_main", new Vector<String>());

        for (int i = 0; i < run.size(); i++) {
            p.print(run.elementAt(i));
        }

        //  +=============================+
        //  | Write Splitter to File      |
        //  +=============================+
    
        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write splitter code to file thread"+thread_id+".cpp");
        }
    
        if (ClusterBackend.debugging) {
            System.out.println("Code for " + node.contents.getName() +
                               " written to thread"+thread_id+".cpp");
        }

    }


    /**
     *  Generate and emit code for a joiner
     *  @param node is a FlatNode where node.contents instanceof SIRJoiner
     */

    public static void generateJoiner(FlatNode node) {
        
        SIRJoiner joiner = (SIRJoiner)node.contents;

        if (joiner.getSumOfWeights() == 0) {
            // The joiner is not doing any work
        
            return;
        }

        int init_counts, steady_counts;

        Integer init_int = ClusterBackend.initExecutionCounts.get(node);
        if (init_int==null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        steady_counts = ClusterBackend.steadyExecutionCounts.get(node).intValue();
        CType baseType = CommonUtils.getBaseType(CommonUtils.getJoinerType(node));
        int thread_id = NodeEnumerator.getSIROperatorId(node.contents);

        List<Tape> in = RegisterStreams.getNodeInStreams(node.contents);
        List<Tape> out_v = RegisterStreams.getNodeOutStreams(node.contents);
        Tape out = out_v.get(0);

        CodegenPrintWriter p = null;
        try {
            p = new CodegenPrintWriter(new FileWriter("thread"+thread_id+".cpp"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        ClusterCodeGenerator gen = new ClusterCodeGenerator(joiner, new JFieldDeclaration[0]);

//        // debugging only
//        System.err.print("(Joiner");
//        if (node.getEdges() != null && node.getEdges().length > 0 && node.getEdges()[0].contents instanceof SIRFilter) {
//            System.err.print (" joiner_" + ((SIRFilter)(node.getEdges()[0].contents)).getName());
//        } else {
//            System.err.print(" joiner_" + node.contents.toString());
//        }
//        System.err.print(" " + init_counts);
//        System.err.print(" " + steady_counts);
//        System.err.println(")");
        
 
        p.print("// init counts: "+init_counts+" steady counts: "+steady_counts+"\n"); 
        p.newLine();

        //  +=============================+
        //  | Preamble                    |
        //  +=============================+     

        FlatIRToCluster f2c = new FlatIRToCluster(p);
        gen.generatePreamble(f2c,p);

//        p.print("void save_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}\n");
//        p.print("void load_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}\n");
//        p.newLine();
//
//        p.println("void save_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
//        p.println("void load_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
//        p.newLine();


//        // declare any external data needed for input and output tapes.
//        p.print(out.upstreamDeclarationExtern());
//        for (Tape ns : in) {
//          if (ns != null) {
//              p.print(ns.downstreamDeclarationExtern());
//          }
//        }

        // declare pop routine for cluster tapes.
        p.print(out.upstreamDeclaration());
        for (Tape ns : in) {
           if (ns != null) {
               p.print(ns.downstreamDeclaration());
           }
         }
       
        //  +=============================+
        //  | Init Path                   |
        //  +=============================+


        if (joiner.getParent() instanceof SIRFeedbackLoop) {
        
            SIRFeedbackLoop floop = (SIRFeedbackLoop)joiner.getParent();

            p.print("//delay = "+((JIntLiteral)floop.getDelay()).intValue());
            p.newLine();

            JMethodDeclaration ipath = floop.getInitPath();

            if (ipath != null) {
                ipath.setName("__Init_Path_" + thread_id);
                FlatIRToCluster fir = new FlatIRToCluster();
                fir.setDeclOnly(false);
                ipath.accept(fir);
                p.print(fir.getPrinter().getString());

                p.newLine();
                p.newLine();
            }

        }

        // +==============================+
        // | Joiner Prep Feedbackloop     |
        // +==============================+
        
        if (joiner.getParent() instanceof SIRFeedbackLoop
                && ((SIRFeedbackLoop) joiner.getParent()).getDelayInt() > 0) {

            //
            // Joiner is a part of feedbackloop with non-zero delay:
            // we need initialization code to put enqueues onto tape from loop
            // portion
            // back to joiner.
            //

            feedbackJoinersNeedingPrep.add(joiner);
            Tape loopTape = RegisterStreams.getNodeInStreams(joiner).get(1);
            int enqueue_count = ((SIRFeedbackLoop) joiner.getParent())
                    .getDelayInt();
            p.println("void __feedbackjoiner_" + thread_id + "_prep() {");
            p.indent();
            p.print(loopTape.pushbackInit(enqueue_count));
            p.print("for (int __i = 0; __i < " + enqueue_count
                    + "; __i++) {\n");
            p.indent();
            p.print(loopTape.pushbackPrefix());
            p.print("__Init_Path_" + thread_id + "(__i)");
            p.print(loopTape.pushbackSuffix());
            p.print(";\n");
            p.outdent();
            p.println("}");
            p.print(loopTape.pushbackCleanup());
            p.outdent();
            p.println("}");

            p.print("  " + baseType.toString() + " tmp;\n");
        }
        
        // +=============+
        // | Joiner Work |
        //  +============+

 
        p.newLine();

        if (joiner.getType().equals(SIRJoinType.ROUND_ROBIN)
                || joiner.getType().equals(SIRJoinType.WEIGHTED_RR)) {

            String joinerWork = null;
            int workCount = 4;
            
            if (KjcOptions.compressed && in.size() == 2) {
                p.println("static unsigned char *__frame_" + in.get(0).getSource() + "_" + in.get(0).getDest() + " = NULL;");
                p.println("static unsigned char *__frame_" + in.get(1).getSource() + "_" + in.get(1).getDest() + " = NULL;");
                
                if (ClusterBackend.joinerWork.containsKey(joiner)) {
                    SIRFilter filter = ClusterBackend.joinerWork.get(joiner);
                    JMethodDeclaration work = filter.getWork();
                    workCount = 4 / ClusterBackend.originalRates.get(filter)[0];
                    
                    final JFormalParameter[] params = new JFormalParameter[]{
                            new JFormalParameter(new CArrayType(CStdType.Char, 1, new JExpression[]{new JIntLiteral(4)}), "__tape_0"), 
                            new JFormalParameter(new CArrayType(CStdType.Char, 1, new JExpression[]{new JIntLiteral(4)}), "__tape_1")};
                    
                    work.setName("__joiner_work_" + joiner.getNumber());
                    
                    work.setParameters(params);
                    
                    final String push = out.getPushName();
                    
                    work.getBody().accept(new SLIRReplacingVisitor(){
                        private int popCount = -1;
                        
                        public Object visitPopExpression(SIRPopExpression self,
                                                         CType tapeType) {
                            ++popCount;
                            return new JArrayAccessExpression(
                                    new JLocalVariableExpression(params[popCount % 2]),
                                    new JIntLiteral(popCount / 2));
                        }

                        public Object visitPushExpression(SIRPushExpression self, CType tapeType, JExpression arg) {
                            return new JMethodCallExpression(push, new JExpression[]{arg});
                        }
                    });
                    
                    p.println("inline void " + work.getName() + "(const unsigned char* __tape_0, const unsigned char* __tape_1)");
                    f2c.visitBlockStatement(work.getBody(), null);
                    p.println();
                    
                    joinerWork = work.getName();
                }
            }
            
            p.println("void __joiner_" + thread_id + "_work(int ____n) {");
            p.indent();
            p.println("for (;____n > 0; ____n--) {");
            p.indent();

            int sum = joiner.getSumOfWeights();
            
            if (KjcOptions.compressed && in.size() == 2) {
                Tape tape0 = in.get(0);
                Tape tape1 = in.get(1);
                String buffer0 = "BUFFER_" + tape0.getSource() + "_" + tape0.getDest();
                String tail0 = "TAIL_" + tape0.getSource() + "_" + tape0.getDest();
                String frame0 = "__frame_" + tape0.getSource() + "_" + tape0.getDest();
                String buffer1 = "BUFFER_" + tape1.getSource() + "_" + tape1.getDest();
                String tail1 = "TAIL_" + tape1.getSource() + "_" + tape1.getDest();
                String frame1 = "__frame_" + tape1.getSource() + "_" + tape1.getDest();
                String pop0 = tape0.getPopName() + "()";
                String pop1 = tape1.getPopName() + "()";
                
                String headOut = "HEAD_" + out.getSource() + "_" + out.getDest();
                String push = out.getPushName();
                
                p.println("const unsigned int frameheight = " + KjcOptions.frameheight + ";");
                p.println("const unsigned int framewidth = " + KjcOptions.framewidth + ";");
                p.println("if (" + frame0 + " == NULL) {");
                p.indent();
                p.println(frame0 + " = (unsigned char*)calloc(4 * frameheight * framewidth, 1);");
                p.println(frame1 + " = (unsigned char*)calloc(4 * frameheight * framewidth, 1);");
                p.outdent();
                p.println("}");
                p.println("unsigned int size0 = " + pop0 + ";");
                if (!KjcOptions.blender) {
                    p.println("size0 <<= 8;");
                    p.println("size0 += " + pop0 + ";");
                    p.println("size0 <<= 8;");
                    p.println("size0 += " + pop0 + ";");
                    p.println("size0 <<= 8;");
                    p.println("size0 += " + pop0 + ";");
                } else {
                    p.println("size0 += (" + pop0 + " << 8);");
                    p.println("size0 += (" + pop0 + " << 16);");
                    p.println("size0 += (" + pop0 + " << 24);");
                }
                p.println("unsigned int size1 = " + pop1 + ";");
                if (!KjcOptions.blender) {
                    p.println("size1 <<= 8;");
                    p.println("size1 += " + pop1 + ";");
                    p.println("size1 <<= 8;");
                    p.println("size1 += " + pop1 + ";");
                    p.println("size1 <<= 8;");
                    p.println("size1 += " + pop1 + ";");
                } else {
                    p.println("size1 += (" + pop1 + " << 8);");
                    p.println("size1 += (" + pop1 + " << 16);");
                    p.println("size1 += (" + pop1 + " << 24);");
                }
                p.println("unsigned int header0 = " + pop0 + ";");
                p.println("header0 <<= 8;");
                p.println("header0 += " + pop0 + ";");
                p.println("unsigned int header1 = " + pop1 + ";");
                p.println("header1 <<= 8;");
                p.println("header1 += " + pop1 + ";");
                p.println("unsigned int start_line0, start_line1;");
                p.println("unsigned int lines_to_change0, lines_to_change1;");
                p.println("if (header0 & 0x0008) {");
                p.indent();
                p.println("start_line0 = " + pop0 + ";");
                p.println("start_line0 <<= 8;");
                p.println("start_line0 += " + pop0 + ";");
                p.println(pop0 + "; " + pop0 + ";");
                p.println("lines_to_change0 = " + pop0 + ";");
                p.println("lines_to_change0 <<= 8;");
                p.println("lines_to_change0 += " + pop0 + ";");
                p.println(pop0 + "; " + pop0 + ";");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("start_line0 = 0;");
                p.println("lines_to_change0 = frameheight;");
                p.outdent();
                p.println("}");
                p.println("if (header1 & 0x0008) {");
                p.indent();
                p.println("start_line1 = " + pop1 + ";");
                p.println("start_line1 <<= 8;");
                p.println("start_line1 += " + pop1 + ";");
                p.println(pop1 + "; " + pop1 + ";");
                p.println("lines_to_change1 = " + pop1 + ";");
                p.println("lines_to_change1 <<= 8;");
                p.println("lines_to_change1 += " + pop1 + ";");
                p.println(pop1 + "; " + pop1 + ";");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("start_line1 = 0;");
                p.println("lines_to_change1 = frameheight;");
                p.outdent();
                p.println("}");
                p.println();
                p.println("unsigned int end_line0 = start_line0 + lines_to_change0;");
                p.println("unsigned int end_line1 = start_line1 + lines_to_change1;");
                p.println("unsigned int start_line_out = (start_line0 < start_line1 ? start_line0 : start_line1);");
                p.println("unsigned int end_line_out = (end_line0 > end_line1 ? end_line0 : end_line1);");
                p.println("unsigned int lines_to_change_out = end_line_out - start_line_out;");
                p.println();
                p.println("unsigned int out_ptr = " + headOut + ";");
                p.println(headOut + " = " + headOut + " + 4;");
                p.println();
                p.println("if (start_line_out == 0 && lines_to_change_out == frameheight) {");
                p.indent();
                p.println(push + "(0);");
                p.println(push + "(0);");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println(push + "(0);");
                p.println(push + "(8);");
                p.println(push + "((start_line_out & 0xFF00) >> 8);");
                p.println(push + "(start_line_out & 0xFF);");
                p.println(push + "(0);");
                p.println(push + "(0);");
                p.println(push + "((lines_to_change_out & 0xFF00) >> 8);");
                p.println(push + "(lines_to_change_out & 0xFF);");
                p.println(push + "(0);");
                p.println(push + "(0);");
                p.outdent();
                p.println("}");
                p.println();
                p.println("const unsigned char SKIP = 0;");
                p.println("const unsigned char RLE = 1;");
                p.println("const unsigned char NEW = 2;");
                p.println("const unsigned char SKIP_LINE[2] = {1, (unsigned char) -1};");
                p.println("const unsigned char *in0_loc, *in1_loc;");
                p.println("in0_loc = " + buffer0 + " + " + tail0 + ";");
                p.println("in1_loc = " + buffer1 + " + " + tail1 + ";");
                p.println("int i = start_line_out;");
                p.println("for (; i < end_line_out; i++) {");
                p.indent();
                p.println("const unsigned char* in[2] = {");
                p.indent();
                p.indent();
                p.println("(i<start_line0 || i>=end_line0) ? SKIP_LINE : in0_loc, ");
                p.println("(i<start_line1 || i>=end_line1) ? SKIP_LINE : in1_loc};");
                p.outdent();
                p.outdent();
                p.println();
                p.println("int pos[2];");
                p.println("unsigned char state[2];");
                p.println("int front, back, rle_code;");
                p.println("unsigned char* framepos0 = " + frame0 + " + 4*framewidth*i;");
                p.println("unsigned char* framepos1 = " + frame1 + " + 4*framewidth*i;");
                p.println("pos[0] = (*in[0] - 1) * 4;");
                p.println("pos[1] = (*in[1] - 1) * 4;");
                p.println("in[0]++; in[1]++;");
                p.println("state[0] = SKIP; state[1] = SKIP;");
                p.println("front = pos[0] > pos[1] ? 0 : 1;");
                p.println("back = 1 - front;");
                p.println(push + "(pos[back]/4 + 1);");
                p.println("int commonpos = pos[back];");
                p.println("framepos0 += pos[back];");
                p.println("framepos1 += pos[back];");
                p.println("while (commonpos < 4 * framewidth) {");
                p.indent();
                p.println("rle_code = (signed char)*in[back];");
                p.println("in[back]++;");
                p.println();
                p.println("if (rle_code == -1) {");
                p.indent();
                p.println("pos[back] = framewidth * 4;");
                p.println("state[back] = SKIP;");
                p.println("in[back]--;");
                p.outdent();
                p.println("} else if (rle_code == 0) {");
                p.indent();
                p.println("pos[back] += (*in[back] - 1) * 4;");
                p.println("in[back]++;");
                p.println("state[back] = SKIP;");
                p.outdent();
                p.println("} else if (rle_code < 0) {");
                p.indent();
                p.println("pos[back] += -rle_code * 4;");
                p.println("state[back] = RLE;");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("pos[back] += rle_code * 4;");
                p.println("state[back] = NEW;");
                p.outdent();
                p.println("}");
                p.println();
                p.println("front = pos[0] > pos[1] ? 0 : 1;");
                p.println("back = 1 - front;");
                p.println();
                p.println("if (commonpos == pos[back]) {");
                p.indent();
                p.println("continue;");
                p.outdent();
                p.println("} else if (state[0] == SKIP && state[1] == SKIP) {");
                p.indent();
                p.println("int skip = (pos[back] - commonpos) / 4;");
                p.println("if (pos[0] == 4 * framewidth && pos[1] == 4 * framewidth) {");
                p.indent();
                p.println("break;");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("while (skip > 254) {");
                p.indent();
                p.println(push + "(0);");
                p.println(push + "(255);");
                p.println("framepos0 += 4 * 254;");
                p.println("framepos1 += 4 * 254;");
                p.println("skip -= 254;");
                p.outdent();
                p.println("}");
                p.println(push + "(0);");
                p.println(push + "(skip + 1);");
                p.println("framepos0 += 4*skip;");
                p.println("framepos1 += 4*skip;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else if (state[0] == RLE && state[1] == RLE && (pos[back] - commonpos > 4)) {");
                p.indent();
                p.println("int rle_amt = (pos[back] - commonpos) / 4;");
                p.println("int j = rle_amt;");
                p.println("for (; j > 0; j--) {");
                p.indent();
                p.println("*framepos0++ = in[0][0];");
                p.println("*framepos0++ = in[0][1];");
                p.println("*framepos0++ = in[0][2];");
                p.println("*framepos0++ = in[0][3];");
                p.println("*framepos1++ = in[1][0];");
                p.println("*framepos1++ = in[1][1];");
                p.println("*framepos1++ = in[1][2];");
                p.println("*framepos1++ = in[1][3];");
                p.outdent();
                p.println("}");
                p.println();
                p.println(push + "(-rle_amt);");
                if (joinerWork != null) {
                    if (workCount == 1) {
                        p.println(joinerWork + "((unsigned char*)in[0], (unsigned char*)in[1]);");
                    } else {
                        p.println("for (int index = 0; index < 4; index++) {");
                        p.indent();
                        p.println(joinerWork + "(((unsigned char*)in[0]) + index, ((unsigned char*)in[1]) + index);");
                        p.outdent();
                        p.println("}");
                    }
                }
                p.println("in[back] += 4;");
                p.println("if (pos[back] == pos[front]) {");
                p.indent();
                p.println("in[front] += 4;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("int total_stretch = (pos[back] - commonpos) / 4;");
                p.println("do {");
                p.indent();
                p.println("int stretch = (total_stretch == 128) ? 127 : total_stretch;");
                p.println(push + "(stretch);");
                p.println();
                p.println("unsigned char* __framepos0 = framepos0;");
                p.println("if (state[0] == NEW) {");
                p.indent();
                p.println("int j = stretch * 4;");
                p.println("for (; j > 0; j--) {");
                p.indent();
                p.println("*__framepos0++ = *(in[0])++;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else if (state[0] == RLE) {");
                p.indent();
                p.println("int j = 0;");
                p.println("for (; j < stretch; j++) {");
                p.indent();
                p.println("*__framepos0++ = in[0][0];");
                p.println("*__framepos0++ = in[0][1];");
                p.println("*__framepos0++ = in[0][2];");
                p.println("*__framepos0++ = in[0][3];");
                p.outdent();
                p.println("}");
                p.println("if (commonpos + 4*stretch == pos[0]) {");
                p.indent();
                p.println("in[0] += 4;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("}");
                p.println();
                p.println("unsigned char* __framepos1 = framepos1;");
                p.println("if (state[1] == NEW) {");
                p.indent();
                p.println("int j = stretch * 4;");
                p.println("for (; j > 0; j--) {");
                p.indent();
                p.println("*__framepos1++ = *(in[1])++;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else if (state[1] == RLE) {");
                p.indent();
                p.println("int j = 0;");
                p.println("for (; j < stretch; j++) {");
                p.indent();
                p.println("*__framepos1++ = in[1][0];");
                p.println("*__framepos1++ = in[1][1];");
                p.println("*__framepos1++ = in[1][2];");
                p.println("*__framepos1++ = in[1][3];");
                p.outdent();
                p.println("}");
                p.println("if (commonpos + 4*stretch == pos[1]) {");
                p.indent();
                p.println("in[1] += 4;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("}");
                p.println();
                p.println("int j = 0;");
                p.println("for (; j < stretch; j++) {");
                p.indent();
                if (joinerWork != null) {
                    if (workCount == 1) {
                        p.println(joinerWork + "(framepos0, framepos1);");
                        p.println("framepos0 += 4;");
                        p.println("framepos1 += 4;");
                    } else {
                        p.println("int index = 0;");
                        p.println("for (; index < 4; index++) {");
                        p.indent();
                        p.println(joinerWork + "(framepos0++, framepos1++);");
                        p.outdent();
                        p.println("}");
                    }
                }
                p.outdent();
                p.println("}");
                p.println("total_stretch -= 127;");
                p.println("commonpos += 4 * 127;");
                p.outdent();
                p.println("} while (total_stretch > 0);");
                p.outdent();
                p.println("}");
                p.println("commonpos = pos[back];");
                p.outdent();
                p.println("}");
                p.println("in[0]++; in[1]++;");
                p.println("if (start_line0 <= i && i < end_line0) {");
                p.indent();
                p.println("in0_loc = in[0];");
                p.outdent();
                p.println("}");
                p.println("if (start_line1 <= i && i < end_line1) {");
                p.indent();
                p.println("in1_loc = in[1];");
                p.outdent();
                p.println("}");
                p.println(push + "((unsigned char) -1);");
                p.outdent();
                p.println("}");
                p.println();
                p.println(push + "(0);");
                p.println("unsigned int framesize = " + headOut + " - out_ptr;");            
                p.println(headOut + " = out_ptr;");
                if (!KjcOptions.blender) {
                    p.println(push + "((framesize & 0xFF000000) >> 24);");
                    p.println(push + "((framesize & 0xFF0000) >> 16);");
                    p.println(push + "((framesize & 0xFF00) >> 8);");
                    p.println(push + "(framesize & 0xFF);");
                } else {
                    p.println(push + "(framesize & 0xFF);");
                    p.println(push + "((framesize & 0xFF00) >> 8);");
                    p.println(push + "((framesize & 0xFF0000) >> 16);");
                    p.println(push + "((framesize & 0xFF000000) >> 24);");
                }
                p.println(headOut + " = out_ptr + framesize;");
                
            }
            else
            // depending on the weight unroll fully or by some constant factor
            if (sum < MAX_SUM_WEIGHTS_TO_UNROLL) {
                // unroll fully!
                for (int i = 0; i < in.size(); i++) {
                    int num = joiner.getWeight(i);
                    if (num != 0) {
                        Tape s = in.get(i);

                        for (int y = 0; y < num; y++) {
                            printCopyTapeElement(p, s, out);
                        }
                    }
                }
            } else {
                // do not unroll fully weight >= MAX_WEIGHTS_TO_UNROLL
                for (int i = 0; i < in.size(); i++) {
                    int num = joiner.getWeight(i);
                    if (num != 0) {
                        Tape s = in.get(i);

                        if (num / UNROLL_BY > 0) {
                            p.println("for (int __k = 0; __k < " + num
                                    / UNROLL_BY + "; __k++) {");
                            p.indent();
                            for (int y = 0; y < UNROLL_BY; y++) {
                                printCopyTapeElement(p, s, out);
                            }
                            p.outdent();
                            p.println("}");
                        }

                        int rem = num % UNROLL_BY;
                        for (int y = 0; y < rem; y++) {
                            printCopyTapeElement(p, s, out);
                        }
                    }
                }
            }
            p.outdent();
            p.println("}");
            p.outdent();
            p.println("}");

            p.newLine();

        } // if (joiner.getType().equals(SIRJoinType.ROUND_ROBIN) ...) 
        // else: no work function for null joiner.
        

        //  +=============================+
        //  | Run Function                |
        //  +=============================+

        Vector<String> run = gen.generateRunFunction(null, "__joiner_"
                + thread_id + "_main", new Vector<String>());

        for (int i = 0; i < run.size(); i++) {
            p.print(run.elementAt(i));
        }

        //  +=============================+
        //  | Write Joiner to File        |
        //  +=============================+

        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write joiner code to file thread"
                    + thread_id + ".cpp");
        }

        if (ClusterBackend.debugging)
            System.out.println("Code for " + node.contents.getName() +
                               " written to thread"+thread_id+".cpp");


    }
    
    public static void generateSlidingWindow(FlatNode node, int n, int m) {
    /*    SIRFilter filter = (SIRFilter)node.contents;
        
        int init_counts, steady_counts;

        Integer init_int = ClusterBackend.initExecutionCounts.get(node);
        if (init_int==null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        steady_counts = ClusterBackend.steadyExecutionCounts.get(node).intValue();
        int thread_id = NodeEnumerator.getSIROperatorId(filter);

        Tape in = RegisterStreams.getFilterInStream(filter);
        Tape out = RegisterStreams.getFilterOutStream(filter);
        
        CodegenPrintWriter p = null;
        try {
            p = new CodegenPrintWriter(new FileWriter("thread"+thread_id+".cpp"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        ClusterCodeGenerator gen = new ClusterCodeGenerator(filter, new JFieldDeclaration[0]);
         
        p.print("// init counts: "+init_counts+" steady counts: "+steady_counts+"\n"); 
        p.newLine();

        //  +=============================+
        //  | Preamble                    |
        //  +=============================+     

        FlatIRToCluster f2c = new FlatIRToCluster(p);
        gen.generatePreamble(f2c,p);

        // declare pop routine for cluster tapes.
        p.print(out.upstreamDeclaration());
        if (in != null) {
            p.print(in.downstreamDeclaration());
        }

        
        // +=============+
        // | Joiner Work |
        //  +============+

 
        p.newLine();

                p.println("static unsigned char *__frame_" + in.getSource() + "_" + in.getDest() + " = NULL;");
                
                    JMethodDeclaration work = filter.getWork();
                    String oldWork = work.getName();
                    
                    final JFormalParameter[] params = new JFormalParameter[]{
                            new JFormalParameter(new CArrayType(CStdType.Char, 1, new JExpression[]{new JIntLiteral(4)}), "__tape_0"), 
                            new JFormalParameter(new CArrayType(CStdType.Char, 1, new JExpression[]{new JIntLiteral(4)}), "__tape_1")};
                    
                    work.setName(oldWork + "_ph");
                    
                    work.setParameters(params);
                    
                    final String push = out.getPushName();
                    
                    work.getBody().accept(new SLIRReplacingVisitor(){
                        private int popCount = -1;
                        
                        public Object visitPopExpression(SIRPopExpression self,
                                                         CType tapeType) {
                            ++popCount;
                            return new JArrayAccessExpression(
                                    new JLocalVariableExpression(params[popCount % 2]),
                                    new JIntLiteral(popCount / 2));
                        }

                        public Object visitPushExpression(SIRPushExpression self, CType tapeType, JExpression arg) {
                            return new JMethodCallExpression(push, new JExpression[]{arg});
                        }
                    });
                    
                    p.println("inline void " + work.getName() + "(const unsigned char* __tape_0, const unsigned char* __tape_1)");
                    f2c.visitBlockStatement(work.getBody(), null);
                    p.println();
                    
                    String newWork = work.getName();
            
            p.println("void __filter_" + thread_id + "_work(int ____n) {");
            p.indent();
            p.println("for (;____n > 0; ____n--) {");
            p.indent();

                String buffer = "BUFFER_" + in.getSource() + "_" + in.getDest();
                String tail = "TAIL_" + in.getSource() + "_" + in.getDest();
                String frame = "__frame_" + in.getSource() + "_" + in.getDest();
                String pop = in.getPopName() + "()";
                
                String headOut = "HEAD_" + out.getSource() + "_" + out.getDest();
                
                p.println("const unsigned int frameheight = " + KjcOptions.frameheight + ";");
                p.println("const unsigned int framewidth = " + KjcOptions.framewidth + ";");
                p.println("const unsigned int windowheight = " + n + ";");
                p.println("const unsigned int windowwidth = " + m + ";");
                p.println("if (" + frame + " == NULL) {");
                p.indent();
                p.println(frame + " = (unsigned char*)calloc(4 * (frameheight + 2 * windowheight) * (framewidth + 2 * windowwidth), 1);");
                p.outdent();
                p.println("}");
                p.println("unsigned int size = " + pop + ";");
                if (!KjcOptions.blender) {
                    p.println("size <<= 8;");
                    p.println("size += " + pop + ";");
                    p.println("size <<= 8;");
                    p.println("size += " + pop + ";");
                    p.println("size <<= 8;");
                    p.println("size += " + pop + ";");
                } else {
                    p.println("size += (" + pop + " << 8);");
                    p.println("size += (" + pop + " << 16);");
                    p.println("size += (" + pop + " << 24);");
                }
                p.println("unsigned int header = " + pop + ";");
                p.println("header <<= 8;");
                p.println("header += " + pop + ";");
                p.println("unsigned int start_line;");
                p.println("unsigned int lines_to_change;");
                p.println("if (header & 0x0008) {");
                p.indent();
                p.println("start_line = " + pop + ";");
                p.println("start_line <<= 8;");
                p.println("start_line += " + pop + ";");
                p.println(pop + "; " + pop + ";");
                p.println("lines_to_change = " + pop + ";");
                p.println("lines_to_change <<= 8;");
                p.println("lines_to_change += " + pop + ";");
                p.println(pop + "; " + pop + ";");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("start_line = 0;");
                p.println("lines_to_change = frameheight;");
                p.outdent();
                p.println("}");
                p.println();
                p.println("unsigned int end_line = start_line + lines_to_change;");
                p.println("unsigned int start_line_out = ((start_line < windowheight) ? 0 : (start_line - windowheight));");
                p.println("unsigned int end_line_out = (((end_line + windowheight) > frameheight) ? frameheight : (end_line + windowheight));");
                p.println("unsigned int lines_to_change_out = end_line_out - start_line_out;");
                p.println();
                p.println("unsigned int out_ptr = " + headOut + ";");
                p.println(headOut + " = " + headOut + " + 4;");
                p.println();
                p.println("if (start_line_out == 0 && lines_to_change_out == frameheight) {");
                p.indent();
                p.println(push + "(0);");
                p.println(push + "(0);");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println(push + "(0);");
                p.println(push + "(8);");
                p.println(push + "((start_line_out & 0xFF00) >> 8);");
                p.println(push + "(start_line_out & 0xFF);");
                p.println(push + "(0);");
                p.println(push + "(0);");
                p.println(push + "((lines_to_change_out & 0xFF00) >> 8);");
                p.println(push + "(lines_to_change_out & 0xFF);");
                p.println(push + "(0);");
                p.println(push + "(0);");
                p.outdent();
                p.println("}");
                p.println();
                p.println("const unsigned char SKIP = 0;");
                p.println("const unsigned char RLE = 1;");
                p.println("const unsigned char NEW = 2;");
                p.println("const unsigned char SKIP_LINE[2] = {1, (unsigned char) -1};");
                p.println("unsigned char *framepos;");
                p.println("const unsigned char *in_loc;");
                p.println("in_loc = " + buffer + " + " + tail + ";");
                p.println("int i = start_line_out;");
                p.println("int lag = 0;");
                p.println("for (; lag < windowheight; lag++) {");
                p.indent();
                p.println("framepos = " + frame + " + (4 * (i + lag) * (framewidth + 2 * windowwidth)) + windowwidth;");
                p.println("int skip_code = *in_loc++;");
                p.println("if (skip_code == 0) break;");
                p.println("framepos += 4 * (skip_code - 1);");
                p.println();
                p.println("int rle_code = *in_loc++;");
                p.println("while (rle_code != 255) {");
                p.indent();
                p.println("if (rle_code == 0) {");
                p.indent();
                p.println("framepos += 4 * (skip_code - 1);");
                p.outdent();
                p.println("} else if (rle_code < 0) {");
                p.indent();
                p.println("*framepos++ = *in_loc++;");
                p.println("*framepos++ = *in_loc++;");
                p.println("*framepos++ = *in_loc++;");
                p.println("*framepos++ = *in_loc++;");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("while (rle_code--) {");
                p.indent();
                p.println("*framepos++ = in_loc[0];");
                p.println("*framepos++ = in_loc[1];");
                p.println("*framepos++ = in_loc[2];");
                p.println("*framepos++ = in_loc[3];");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("}");
                p.println();
                p.println("for (; i < end_line_out; i++) {");
                p.indent();
                p.println("const unsigned char* in[2] = {");
                p.indent();
                p.indent();
                p.println("(i<start_line0 || i>=end_line0) ? SKIP_LINE : in0_loc, ");
                p.println("(i<start_line1 || i>=end_line1) ? SKIP_LINE : in1_loc};");
                p.outdent();
                p.outdent();
                p.println();
                p.println("int pos[2];");
                p.println("unsigned char state[2];");
                p.println("int front, back, rle_code;");
                p.println("unsigned char* framepos0 = " + frame0 + " + 4*framewidth*i;");
                p.println("unsigned char* framepos1 = " + frame1 + " + 4*framewidth*i;");
                p.println("pos[0] = (*in[0] - 1) * 4;");
                p.println("pos[1] = (*in[1] - 1) * 4;");
                p.println("in[0]++; in[1]++;");
                p.println("state[0] = SKIP; state[1] = SKIP;");
                p.println("front = pos[0] > pos[1] ? 0 : 1;");
                p.println("back = 1 - front;");
                p.println(push + "(pos[back]/4 + 1);");
                p.println("int commonpos = pos[back];");
                p.println("framepos0 += pos[back];");
                p.println("framepos1 += pos[back];");
                p.println("while (commonpos < 4 * framewidth) {");
                p.indent();
                p.println("rle_code = (signed char)*in[back];");
                p.println("in[back]++;");
                p.println();
                p.println("if (rle_code == -1) {");
                p.indent();
                p.println("pos[back] = framewidth * 4;");
                p.println("state[back] = SKIP;");
                p.println("in[back]--;");
                p.outdent();
                p.println("} else if (rle_code == 0) {");
                p.indent();
                p.println("pos[back] += (*in[back] - 1) * 4;");
                p.println("in[back]++;");
                p.println("state[back] = SKIP;");
                p.outdent();
                p.println("} else if (rle_code < 0) {");
                p.indent();
                p.println("pos[back] += -rle_code * 4;");
                p.println("state[back] = RLE;");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("pos[back] += rle_code * 4;");
                p.println("state[back] = NEW;");
                p.outdent();
                p.println("}");
                p.println();
                p.println("front = pos[0] > pos[1] ? 0 : 1;");
                p.println("back = 1 - front;");
                p.println();
                p.println("if (commonpos == pos[back]) {");
                p.indent();
                p.println("continue;");
                p.outdent();
                p.println("} else if (state[0] == SKIP && state[1] == SKIP) {");
                p.indent();
                p.println("int skip = (pos[back] - commonpos) / 4;");
                p.println("if (pos[0] == 4 * framewidth && pos[1] == 4 * framewidth) {");
                p.indent();
                p.println("break;");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("while (skip > 254) {");
                p.indent();
                p.println(push + "(0);");
                p.println(push + "(255);");
                p.println("framepos0 += 4 * 254;");
                p.println("framepos1 += 4 * 254;");
                p.println("skip -= 254;");
                p.outdent();
                p.println("}");
                p.println(push + "(0);");
                p.println(push + "(skip + 1);");
                p.println("framepos0 += 4*skip;");
                p.println("framepos1 += 4*skip;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else if (state[0] == RLE && state[1] == RLE && (pos[back] - commonpos > 4)) {");
                p.indent();
                p.println("int rle_amt = (pos[back] - commonpos) / 4;");
                p.println("int j = rle_amt;");
                p.println("for (; j > 0; j--) {");
                p.indent();
                p.println("*framepos0++ = in[0][0];");
                p.println("*framepos0++ = in[0][1];");
                p.println("*framepos0++ = in[0][2];");
                p.println("*framepos0++ = in[0][3];");
                p.println("*framepos1++ = in[1][0];");
                p.println("*framepos1++ = in[1][1];");
                p.println("*framepos1++ = in[1][2];");
                p.println("*framepos1++ = in[1][3];");
                p.outdent();
                p.println("}");
                p.println();
                p.println(push + "(-rle_amt);");
                if (joinerWork != null) {
                    if (workCount == 1) {
                        p.println(joinerWork + "((unsigned char*)in[0], (unsigned char*)in[1]);");
                    } else {
                        p.println("for (int index = 0; index < 4; index++) {");
                        p.indent();
                        p.println(joinerWork + "(((unsigned char*)in[0]) + index, ((unsigned char*)in[1]) + index);");
                        p.outdent();
                        p.println("}");
                    }
                }
                p.println("in[back] += 4;");
                p.println("if (pos[back] == pos[front]) {");
                p.indent();
                p.println("in[front] += 4;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else {");
                p.indent();
                p.println("int total_stretch = (pos[back] - commonpos) / 4;");
                p.println("do {");
                p.indent();
                p.println("int stretch = (total_stretch == 128) ? 127 : total_stretch;");
                p.println(push + "(stretch);");
                p.println();
                p.println("unsigned char* __framepos0 = framepos0;");
                p.println("if (state[0] == NEW) {");
                p.indent();
                p.println("int j = stretch * 4;");
                p.println("for (; j > 0; j--) {");
                p.indent();
                p.println("*__framepos0++ = *(in[0])++;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else if (state[0] == RLE) {");
                p.indent();
                p.println("int j = 0;");
                p.println("for (; j < stretch; j++) {");
                p.indent();
                p.println("*__framepos0++ = in[0][0];");
                p.println("*__framepos0++ = in[0][1];");
                p.println("*__framepos0++ = in[0][2];");
                p.println("*__framepos0++ = in[0][3];");
                p.outdent();
                p.println("}");
                p.println("if (commonpos + 4*stretch == pos[0]) {");
                p.indent();
                p.println("in[0] += 4;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("}");
                p.println();
                p.println("unsigned char* __framepos1 = framepos1;");
                p.println("if (state[1] == NEW) {");
                p.indent();
                p.println("int j = stretch * 4;");
                p.println("for (; j > 0; j--) {");
                p.indent();
                p.println("*__framepos1++ = *(in[1])++;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("} else if (state[1] == RLE) {");
                p.indent();
                p.println("int j = 0;");
                p.println("for (; j < stretch; j++) {");
                p.indent();
                p.println("*__framepos1++ = in[1][0];");
                p.println("*__framepos1++ = in[1][1];");
                p.println("*__framepos1++ = in[1][2];");
                p.println("*__framepos1++ = in[1][3];");
                p.outdent();
                p.println("}");
                p.println("if (commonpos + 4*stretch == pos[1]) {");
                p.indent();
                p.println("in[1] += 4;");
                p.outdent();
                p.println("}");
                p.outdent();
                p.println("}");
                p.println();
                p.println("int j = 0;");
                p.println("for (; j < stretch; j++) {");
                p.indent();
                if (joinerWork != null) {
                    if (workCount == 1) {
                        p.println(joinerWork + "(framepos0, framepos1);");
                        p.println("framepos0 += 4;");
                        p.println("framepos1 += 4;");
                    } else {
                        p.println("int index = 0;");
                        p.println("for (; index < 4; index++) {");
                        p.indent();
                        p.println(joinerWork + "(framepos0++, framepos1++);");
                        p.outdent();
                        p.println("}");
                    }
                }
                p.outdent();
                p.println("}");
                p.println("total_stretch -= 127;");
                p.println("commonpos += 4 * 127;");
                p.outdent();
                p.println("} while (total_stretch > 0);");
                p.outdent();
                p.println("}");
                p.println("commonpos = pos[back];");
                p.outdent();
                p.println("}");
                p.println("in[0]++; in[1]++;");
                p.println("if (start_line0 <= i && i < end_line0) {");
                p.indent();
                p.println("in0_loc = in[0];");
                p.outdent();
                p.println("}");
                p.println("if (start_line1 <= i && i < end_line1) {");
                p.indent();
                p.println("in1_loc = in[1];");
                p.outdent();
                p.println("}");
                p.println(push + "((unsigned char) -1);");
                p.outdent();
                p.println("}");
                p.println();
                p.println(push + "(0);");
                p.println("unsigned int framesize = " + headOut + " - out_ptr;");            
                p.println(headOut + " = out_ptr;");
                if (!KjcOptions.blender) {
                    p.println(push + "((framesize & 0xFF000000) >> 24);");
                    p.println(push + "((framesize & 0xFF0000) >> 16);");
                    p.println(push + "((framesize & 0xFF00) >> 8);");
                    p.println(push + "(framesize & 0xFF);");
                } else {
                    p.println(push + "(framesize & 0xFF);");
                    p.println(push + "((framesize & 0xFF00) >> 8);");
                    p.println(push + "((framesize & 0xFF0000) >> 16);");
                    p.println(push + "((framesize & 0xFF000000) >> 24);");
                }
                p.println(headOut + " = out_ptr + framesize;");
                
            p.outdent();
            p.println("}");
            p.outdent();
            p.println("}");

            p.newLine();

        //  +=============================+
        //  | Write Filter to File        |
        //  +=============================+

        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write filter code to file thread"
                    + thread_id + ".cpp");
        }

        if (ClusterBackend.debugging)
            System.out.println("Code for " + filter.getName() +
                               " written to thread"+thread_id+".cpp");*/
    }
}

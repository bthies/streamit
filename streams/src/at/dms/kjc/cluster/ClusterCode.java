
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.Vector;
import at.dms.kjc.common.CodegenPrintWriter;
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
                // find fields tht need to be included in checkpoint
                DetectConst.detect(node);
                // generate code for a filter.
                FlatIRToCluster.generateCode(node);

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
     * @param from  : tape veing popped from
     * @param to    : tape being pushed to
     */
    private static void printCopyTapeElement(CodegenPrintWriter p, Tape from, Tape to) {
        p.print(to.pushPrefix());
        p.print(from.popExprNoCleanup());
        p.print(to.pushSuffix());
        p.print(from.popExprCleanup());
        p.print(";\n");
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
            p.print("  " + baseType.toString() + " tmp;\n");
            p.print("  tmp = " + in.popExpr() + ";\n");

            for (Tape s : out) {
                if (s != null) {
                p.print("  " + s.pushPrefix() + "tmp" + s.pushSuffix() + ";\n");
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


        // declare any external data needed for input and output tapes.
        out.upstreamDeclarationExtern();
        for (Tape ns : in) {
          if (ns != null) {
              ns.downstreamDeclarationExtern();
          }
        }

        // declare pop routine for cluster tapes.
        out.upstreamDeclaration();
        for (Tape ns : in) {
           if (ns != null) {
               ns.downstreamDeclaration();
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

            ipath.setName("__Init_Path_"+thread_id);
            FlatIRToCluster fir = new FlatIRToCluster();
            fir.setDeclOnly(false);
            ipath.accept(fir);
            p.print(fir.getPrinter().getString());

            p.newLine();
            p.newLine();

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

            p.print("void __joiner_" + thread_id + "_work(int ____n) {\n");
            p.print("  for (;____n > 0; ____n--) {\n");

            int sum = joiner.getSumOfWeights();

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
                            p.println("  for (int __k = 0; __k < " + num
                                    / UNROLL_BY + "; __k++) {\n");
                            for (int y = 0; y < UNROLL_BY; y++) {
                                printCopyTapeElement(p, s, out);
                            }
                            p.println("  }\n");
                        }

                        int rem = num % UNROLL_BY;
                        for (int y = 0; y < rem; y++) {
                            printCopyTapeElement(p, s, out);
                        }
                    }
                }
            }
            p.print("  }\n");
            p.print("}\n");

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
}

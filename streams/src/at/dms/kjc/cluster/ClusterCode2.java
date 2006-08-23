package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.CodegenPrintWriter;
import java.util.HashSet;
import java.util.List;
import java.io.*;
import at.dms.kjc.common.CommonUtils;

/**
 * Repository for Janis' generateSplitter2, generateJoiner2
 * 
 * This attempt to create a new library-oriented verions of
 * the cluster code generator was never full featured enough
 * to be of much use.
 *  
 * @author dimock
 *
 */
public class ClusterCode2 {
    private static class DoIt implements FlatVisitor {
        /**
         * Don't you dare: only public because whole class was made a visitor 
         * rather than using an inner class.  Call {@link generateCode(FlatNode)}.
         */
        public void visitNode(FlatNode node) 
        {

            if (node.contents instanceof SIRFilter) {
                // find fields tht need to be included in checkpoint
                DetectConst.detect(node);
                // generate code for a filter.
//                FlatIRToCluster.generateCode(node);
                // alternate (incomplete) code generation.
                 FlatIRToCluster2.generateCode(node);
            }

            if (node.contents instanceof SIRSplitter) {
                // generate code foe a plitter
//                generateSplitter(node);
                generateSplitter2(node); // new codegen

            }

            if (node.contents instanceof SIRJoiner) {
                // generate code for a joiner
//                generateJoiner(node);
                generateJoiner2(node); // new codegen

            }
        }
        
    }  // end class DoIt

    /**
     * Generate and emit code for all nodes in FlatGraph
     * 
     * @param topLevel
     *            the entry point to the FlatGraph of the program
     */
    @Deprecated
    public static void generateCode(FlatNode topLevel) {
        topLevel.accept(new DoIt(), new HashSet(), true);
     }

    /**
     *  Generate and emit code for a splitter using new codegen allowing speculative execution.
     *  <br/>
     *  Unfortunately, this experimental version is not full-featured.
     * @param node FlatNode for the splitter to generate code for.
     * @deprecated
     */
    
     @Deprecated 
     public static void generateSplitter2(FlatNode node) {
        
        SIRSplitter splitter = (SIRSplitter)node.contents;

        // The splitter is not doing any work
        if (splitter.getSumOfWeights() == 0) return; 

        int init_counts, steady_counts;

        Integer init_int = (Integer)ClusterBackend.initExecutionCounts.get(node);
        if (init_int==null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();

        CType baseType = CommonUtils.getBaseType(CommonUtils.getOutputType(node));
        int id = NodeEnumerator.getSIROperatorId(node.contents);

        List<NetStream> in_v = RegisterStreams.getNodeInStreams(node.contents);
        NetStream in = in_v.get(0);
        List<NetStream> out = RegisterStreams.getNodeOutStreams(node.contents);

        int sum_of_weights = splitter.getSumOfWeights();
    
        CodegenPrintWriter p = null;
        try {
            p = new CodegenPrintWriter(new FileWriter("thread_"+id+".cpp"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        p.println("#include <stream_node.h>\n");
        p.println("class thread"+id+" : public stream_node<"+baseType+","+baseType+"> {");
        p.println("public:");
        p.println("  thread"+id+"() : stream_node<"+baseType+","+baseType+">("+id+","+NodeEnumerator.getNumberOfNodes()+","+sum_of_weights+","+sum_of_weights+","+sum_of_weights+") {");
        p.println("    add_input("+in.getSource()+");");

        for (int i = 0; i < out.size(); i++) {
          NetStream s = out.get(i);
          if (s != null) {
            int num;
            if (splitter.getType().equals(SIRSplitType.DUPLICATE))
                num = sum_of_weights;
            else 
                num = splitter.getWeight(i);    
            p.println("    add_output_rate("+s.getDest()+","+num+");");
          }
        }
    
        p.println("  }");
        p.newLine();

        p.println("  "+baseType+" buf["+sum_of_weights+"];");
        p.newLine();

        p.println("  int state_size() { return 0; }");
        p.println("  void save_state(object_write_buffer *buf) {}");
        p.println("  void load_state(object_write_buffer *buf) {}");
        p.println("  void init_state() {}");
        p.println("  void send_credits() {}");
        p.println("  void exec_message(message *msg) {}");
        p.newLine();
    
        p.println("  void work() {");
        p.println("    consumer_array["+in.getSource()+"]->pop_items(buf, "+sum_of_weights+");");
        int offs = 0;
        for (int i = 0; i < out.size(); i++) {
          NetStream s = out.get(i);      
          if (s != null) {
            int num;
            if (splitter.getType().equals(SIRSplitType.DUPLICATE))
                num = sum_of_weights;
            else
                num = splitter.getWeight(i);    
        
            p.println("    producer_array["+s.getDest()+"]->push_items(buf+"+offs+", "+num+");");

            if (!splitter.getType().equals(SIRSplitType.DUPLICATE)) offs += num;
          }
        }
        p.println("  }");
        p.newLine();

        p.println("  void work_n(int __n) {");
        p.println("    for (int y = 0; y < __n; y++) {");
        p.println("      consumer_array["+in.getSource()+"]->pop_items(buf, "+sum_of_weights+");");
        offs = 0;
        for (int i = 0; i < out.size(); i++) {
          NetStream s = out.get(i);      
          if (s != null) {
            int num;
            if (splitter.getType().equals(SIRSplitType.DUPLICATE))
                num = sum_of_weights;
            else
                num = splitter.getWeight(i);    

            p.println("      producer_array["+s.getDest()+"]->push_items(buf+"+offs+", "+num+");");

            if (!splitter.getType().equals(SIRSplitType.DUPLICATE)) offs += num;
          }
        }
        p.println("    }");
        p.println("  }");
        p.newLine();

        p.println("};");
        p.newLine();

        p.println("thread"+id+" *instance_"+id+" = NULL;");
        p.println("thread"+id+" *get_instance_"+id+"() {");
        p.println("  if (instance_"+id+" == NULL) { instance_"+id+" = new thread"+id+"();");
        p.println("    instance_"+id+"->init_stream(); }");
        p.println("  return instance_"+id+";");
        p.println("}");
        p.println("void __get_thread_info_"+id+"() { get_instance_"+id+"()->get_thread_info(); }");
        p.println("void __declare_sockets_"+id+"() { get_instance_"+id+"()->declare_sockets(); }");
        p.println("extern int __max_iteration;");
        p.println("void run_"+id+"() { get_instance_"+id+"()->run_simple("+init_counts+"+("+steady_counts+"*__max_iteration)); }");

        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write code to file thread_"+id+".cpp");
        }
    
        if (ClusterBackend.debugPrint)
            System.out.println("Code for " + node.contents.getName() +
                               " written to thread_"+id+".cpp");

    }


    /**
     *  Generate and emit code for a joiner using new codegen allowing speculative execution.
     *  <br/>
     *  Unfortunately, this experimental version is not full-featured.
     * @param node is a FlatNode where node.contents instanceof SIRJoiner
     * @deprecated
     */

    @Deprecated 
    public static void generateJoiner2(FlatNode node) {
        
        SIRJoiner joiner = (SIRJoiner)node.contents;

        // The joiner is not doing any work
        if (joiner.getSumOfWeights() == 0) return; 

        int init_counts, steady_counts;

        Integer init_int = (Integer)ClusterBackend.initExecutionCounts.get(node);
        if (init_int==null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();

        CType baseType = CommonUtils.getBaseType(CommonUtils.getJoinerType(node));
        int id = NodeEnumerator.getSIROperatorId(node.contents);

        List<NetStream> in = RegisterStreams.getNodeInStreams(node.contents);
        List<NetStream> out_v = RegisterStreams.getNodeOutStreams(node.contents);
        NetStream out = out_v.get(0);

        int sum_of_weights = joiner.getSumOfWeights();
    
        CodegenPrintWriter p = null;
        try {
            p = new CodegenPrintWriter(new FileWriter("thread_"+id+".cpp"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        p.println("#include <stream_node.h>\n");
        p.println("class thread"+id+" : public stream_node<"+baseType+","+baseType+"> {");
        p.println("public:");
        p.println("  thread"+id+"() : stream_node<"+baseType+","+baseType+">("+id+","+NodeEnumerator.getNumberOfNodes()+","+sum_of_weights+","+sum_of_weights+","+sum_of_weights+") {");

        for (int i = 0; i < in.size(); i++) {
          NetStream s = in.get(i);
          if (s != null) {
            int num = joiner.getWeight(i);  
            p.println("    add_input_rate("+s.getSource()+","+num+");");
          }
        }

        p.println("    add_output("+out.getDest()+");");
    
        p.println("  }");
        p.newLine();

        p.println("  "+baseType+" buf["+sum_of_weights+"];");
        p.newLine();

        p.println("  int state_size() { return 0; }");
        p.println("  void save_state(object_write_buffer *buf) {}");
        p.println("  void load_state(object_write_buffer *buf) {}");
        p.println("  void init_state() {}");
        p.println("  void send_credits() {}");
        p.println("  void exec_message(message *msg) {}");
        p.newLine();
    
        p.println("  void work() {");
        int offs = 0;
        for (int i = 0; i < in.size(); i++) {
            NetStream s = in.get(i); 
            if (s == null) continue;
            int num = joiner.getWeight(i);  
            p.println("    consumer_array["+s.getSource()+"]->pop_items(buf+"+offs+", "+num+");");
            offs += num;
        }
        p.println("    producer_array["+out.getDest()+"]->push_items(buf, "+sum_of_weights+");");
        p.println("  }");
        p.newLine();

        p.println("  void work_n(int __n) {");
        p.println("    for (int y = 0; y < __n; y++) {");
        offs = 0;
        for (int i = 0; i < in.size(); i++) {
            NetStream s = in.get(i); 
            if (s == null) continue;
            int num = joiner.getWeight(i);  
            p.println("      consumer_array["+s.getSource()+"]->pop_items(buf+"+offs+", "+num+");");
            offs += num;
        }
        p.println("      producer_array["+out.getDest()+"]->push_items(buf, "+sum_of_weights+");");
        p.println("    }");
        p.println("  }");
        p.newLine();

        p.println("};");
        p.newLine();

        p.println("thread"+id+" *instance_"+id+" = NULL;");
        p.println("thread"+id+" *get_instance_"+id+"() {");
        p.println("  if (instance_"+id+" == NULL) { instance_"+id+" = new thread"+id+"();");
        p.println("    instance_"+id+"->init_stream(); }");
        p.println("  return instance_"+id+";");
        p.println("}");
        p.println("void __get_thread_info_"+id+"() { get_instance_"+id+"()->get_thread_info(); }");
        p.println("void __declare_sockets_"+id+"() { get_instance_"+id+"()->declare_sockets(); }");
        p.println("extern int __max_iteration;");
        p.println("void run_"+id+"() { get_instance_"+id+"()->run_simple("+init_counts+"+("+steady_counts+"*__max_iteration)); }");

        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write code to file thread_"+id+".cpp");
        }
    
        if (ClusterBackend.debugPrint)
            System.out.println("Code for " + node.contents.getName() +
                               " written to thread_"+id+".cpp");

    }
  
}


package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.Vector;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.raw.Util;
import java.util.HashSet;
import java.io.*;

/**
 * This class generates and emits code for each filter, splitter, and joiner
 * in the flat graph.
 */
public class ClusterCode extends at.dms.util.Utils implements FlatVisitor {
    
    /**
     *  Generate and emit code for all nodes in FlatGraph
     */
    public static void generateCode(FlatNode topLevel) 
    {
        topLevel.accept(new ClusterCode(), new HashSet(), true);
    }

   
    /**
     * Don't you dare: only public because whole class was made a visitor 
     * rather than using an inner class.
     */
    public void visitNode(FlatNode node) 
    {

        if (node.contents instanceof SIRFilter) {

            DetectConst.detect(node);
            FlatIRToCluster.generateCode(node);
            // FlatIRToCluster2.generateCode(node);

            //((SIRFilter)node.contents).setMethods(JMethodDeclaration.EMPTY());

        }

        if (node.contents instanceof SIRSplitter) {

            generateSplitter(node);
            generateSplitter2(node); // new codegen

        }

        if (node.contents instanceof SIRJoiner) {

            generateJoiner(node);
            generateJoiner2(node); // new codegen

        }
    }


    /**
     *  Generate and emit code for a splitter using new codegen
     */
    
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

        CType baseType = Util.getBaseType(Util.getOutputType(node));
        int id = NodeEnumerator.getSIROperatorId(node.contents);

        Vector in_v = (Vector)RegisterStreams.getNodeInStreams(node.contents);
        NetStream in = (NetStream)in_v.elementAt(0);
        Vector out = (Vector)RegisterStreams.getNodeOutStreams(node.contents);

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
            NetStream s = (NetStream)out.elementAt(i);      
            int num;
            if (splitter.getType().equals(SIRSplitType.DUPLICATE))
                num = sum_of_weights;
            else 
                num = splitter.getWeight(i);    
            p.println("    add_output_rate("+s.getDest()+","+num+");");
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
            NetStream s = (NetStream)out.elementAt(i);      
        
            int num;
            if (splitter.getType().equals(SIRSplitType.DUPLICATE))
                num = sum_of_weights;
            else
                num = splitter.getWeight(i);    
        
            p.println("    producer_array["+s.getDest()+"]->push_items(buf+"+offs+", "+num+");");

            if (!splitter.getType().equals(SIRSplitType.DUPLICATE)) offs += num;
        }
        p.println("  }");
        p.newLine();

        p.println("  void work_n(int __n) {");
        p.println("    for (int y = 0; y < __n; y++) {");
        p.println("      consumer_array["+in.getSource()+"]->pop_items(buf, "+sum_of_weights+");");
        offs = 0;
        for (int i = 0; i < out.size(); i++) {
            NetStream s = (NetStream)out.elementAt(i);      

            int num;
            if (splitter.getType().equals(SIRSplitType.DUPLICATE))
                num = sum_of_weights;
            else
                num = splitter.getWeight(i);    

            p.println("      producer_array["+s.getDest()+"]->push_items(buf+"+offs+", "+num+");");

            if (!splitter.getType().equals(SIRSplitType.DUPLICATE)) offs += num;
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
    
        System.out.println("Code for " + node.contents.getName() +
                           " written to thread_"+id+".cpp");

    }




    /**
     *  Generate and emit code for a joiner using new codegen
     */

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

        CType baseType = Util.getBaseType(Util.getJoinerType(node));
        int id = NodeEnumerator.getSIROperatorId(node.contents);

        Vector in = (Vector)RegisterStreams.getNodeInStreams(node.contents);
        Vector out_v = (Vector)RegisterStreams.getNodeOutStreams(node.contents);
        NetStream out = (NetStream)out_v.elementAt(0);

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
            NetStream s = (NetStream)in.elementAt(i);       
            int num = joiner.getWeight(i);  
            p.println("    add_input_rate("+s.getSource()+","+num+");");
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
            NetStream s = (NetStream)in.elementAt(i);       
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
            NetStream s = (NetStream)in.elementAt(i);       
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
    
        System.out.println("Code for " + node.contents.getName() +
                           " written to thread_"+id+".cpp");

    }

    /**
     *  Generate and emit code for a splitter
     */

    public static void generateSplitter(FlatNode node) {
        
        SIRSplitter splitter = (SIRSplitter)node.contents;

        if (splitter.getSumOfWeights() == 0) {
            // The splitter is not doing any work
        
            return;
        }

        int init_counts, steady_counts;

        Integer init_int = (Integer)ClusterBackend.initExecutionCounts.get(node);
        if (init_int==null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();

        CType baseType = Util.getBaseType(Util.getOutputType(node));
        int thread_id = NodeEnumerator.getSIROperatorId(node.contents);

        Vector in_v = (Vector)RegisterStreams.getNodeInStreams(node.contents);
        NetStream in = (NetStream)in_v.elementAt(0);
        Vector out = (Vector)RegisterStreams.getNodeOutStreams(node.contents);
    
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

        p.println("void save_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}");
        p.println("void load_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}");
        p.newLine();

	p.println("void save_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
	p.println("void load_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
        p.newLine();


        //  +=============================+
        //  | Splitter Push               |
        //  +=============================+

        int sum_of_weights = splitter.getSumOfWeights();

	if (! KjcOptions.standalone) { 

	p.print("#ifndef __CLUSTER_STANDALONE\n");

        p.print(baseType.toString()+" "+in.push_buffer()+"["+sum_of_weights+"];\n");
        p.print("int "+in.push_index()+" = 0;\n");
        p.newLine();

        p.print("void "+in.push_name()+"("+baseType.toString()+" data) {\n");

        if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {
            for (int i = 0; i < out.size(); i++) {
                NetStream s = (NetStream)out.elementAt(i);      
                FlatNode dest = NodeEnumerator.getFlatNode(s.getDest());

                if (ClusterFusion.fusedWith(node).contains(dest)) {
                    p.print("  "+s.push_name()+"(data);\n");
                } else {
                    p.print("  "+s.producer_name()+".push(data);\n");
                }
            }       
        } else {
            p.print("  "+in.push_buffer()+"["+in.push_index()+"++] = data;\n");
            p.print("  if ("+in.push_index()+" == "+sum_of_weights+") {\n");
            int offs = 0;
        
            for (int i = 0; i < out.size(); i++) {
                int num = splitter.getWeight(i);
                NetStream s = (NetStream)out.elementAt(i);
                FlatNode dest = NodeEnumerator.getFlatNode(s.getDest());

                if (ClusterFusion.fusedWith(node).contains(dest)) {
                    for (int y = 0; y < num; y++) {
                        p.print("  "+s.push_name()+"("+in.push_buffer()+"["+(offs+y)+"]);\n");
                    }
                } else {
                    p.print("    "+s.producer_name()+".push_items(&"+in.push_buffer()+"["+offs+"], "+num+");\n");
                }
                offs += num;
            }

            p.print("    "+in.push_index()+" = 0;\n");
            p.print("  }\n");
        }
        p.print("}\n");
        p.newLine();

        //  +=============================+
        //  | Splitter Pop                |
        //  +=============================+

        int split_ways = splitter.getWays();
        for (int ch = 0; ch < split_ways; ch++) {

            int weight = splitter.getWeight(ch);
            if (weight == 0) { // this deals with the odd case of a 0 weight 
                ch--;          // which will have no coresponding output edge
                split_ways--;  // in the graph.
                continue;
            }
            NetStream _out = (NetStream)out.elementAt(ch);

            p.print(baseType.toString()+" "+_out.pop_buffer()+"["+weight+"];\n");
            p.print("int "+_out.pop_index()+" = "+weight+";\n");
            p.newLine();

            p.print(baseType.toString()+" "+_out.pop_name()+"() {\n");

            if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {

                p.print("    "+baseType.toString()+" tmp = "+in.consumer_name()+".pop();\n");
                for (int y = 0; y < splitter.getWays(); y++) {
                    if (y != ch) {
                        p.print("    "+((NetStream)out.elementAt(y)).producer_name()+".push(tmp);\n");
                    }
                }
                p.print("    return tmp;\n");
        
            } else {

                int sum = splitter.getSumOfWeights();
                int offs = 0;
        
                p.print("  "+baseType.toString()+" tmp["+sum+"];\n");
                p.print("  if ("+_out.pop_index()+" == "+splitter.getWeight(ch)+") {\n");
        
                p.print("    "+in.consumer_name()+".pop_items(tmp, "+sum+");\n");
        
                for (int y = 0; y < out.size(); y++) {
                    int num = splitter.getWeight(y);
                    NetStream s = (NetStream)out.elementAt(y);
                    if (y == ch) {
                        p.print("    memcpy(&tmp["+offs+"], "+_out.pop_buffer()+", "+num+" * sizeof("+baseType.toString()+"));\n");
                    } else {
                        p.print("    "+s.producer_name()+".push_items(&tmp["+offs+"], "+num+");\n");
                    }
                    offs += num;
                }

                p.print("    "+_out.pop_index()+" = 0;\n");
                p.print("  }\n");
                p.print("  return "+_out.pop_buffer()+"["+_out.pop_index()+"++];\n");
            }

            p.print("}\n");
            p.newLine();
        }
    
	p.print("#endif // __CLUSTER_STANDALONE\n");
	}

        //  +=============================+
        //  | Splitter Work               |
        //  +=============================+

        int _s = in.getSource();
        int _d = in.getDest();
        p.print("#ifdef __FUSED_"+_s+"_"+_d+"\n");
        p.print("extern "+baseType.toString()+" BUFFER_"+_s+"_"+_d+"[];\n");
        p.print("extern int HEAD_"+_s+"_"+_d+";\n");
        p.print("extern int TAIL_"+_s+"_"+_d+";\n");
        p.print("#endif\n");

        for (int o = 0; o < out.size(); o++) {
            _s = ((NetStream)out.elementAt(o)).getSource();
            _d = ((NetStream)out.elementAt(o)).getDest();
            p.print("#ifdef __FUSED_"+_s+"_"+_d+"\n");
            p.print("extern "+baseType.toString()+" BUFFER_"+_s+"_"+_d+"[];\n");
            p.print("extern int HEAD_"+_s+"_"+_d+";\n");
            p.print("extern int TAIL_"+_s+"_"+_d+";\n");
            p.print("#endif\n");
        }

        p.newLine();

        p.print("void __splitter_"+thread_id+"_work(int ____n) {\n");
        //p.println("// ClusterCode_1");
        p.print("  for (;____n > 0; ____n--) {\n");
    
        FlatNode source = NodeEnumerator.getFlatNode(in.getSource());

        if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {

            p.print("  "+baseType.toString()+" tmp;\n");    

            _s = in.getSource();
            _d = in.getDest();      

            p.newLine();

            p.print("  #ifdef __FUSED_"+_s+"_"+_d+"\n");
            p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
            p.print("    tmp = BUFFER_"+_s+"_"+_d+"[TAIL_"+_s+"_"+_d+"];TAIL_"+_s+"_"+_d+"++;\n");
            p.print("    #else\n");
            p.print("    tmp = BUFFER_"+_s+"_"+_d+"[TAIL_"+_s+"_"+_d+"];TAIL_"+_s+"_"+_d+"++;TAIL_"+_s+"_"+_d+"&=__BUF_SIZE_MASK_"+_s+"_"+_d+";\n");
            p.print("    #endif\n");
            p.print("  #else\n");

            if (ClusterFusion.fusedWith(node).contains(source)) {
                p.print("  tmp = "+in.pop_name()+"();\n");
            } else {        
                p.print("  tmp = "+in.consumer_name()+".pop();\n");     
            }

            p.print("  #endif\n");

            p.newLine();

            for (int i = 0; i < out.size(); i++) {
                NetStream s = (NetStream)out.elementAt(i);      

                _s = s.getSource();
                _d = s.getDest();

                p.print("  #ifdef __FUSED_"+_s+"_"+_d+"\n");

                p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
                p.print("    BUFFER_"+_s+"_"+_d+"[HEAD_"+_s+"_"+_d+"]=tmp;HEAD_"+_s+"_"+_d+"++;\n");
                p.print("    #else\n");
                p.print("    BUFFER_"+_s+"_"+_d+"[HEAD_"+_s+"_"+_d+"]=tmp;HEAD_"+_s+"_"+_d+"++;HEAD_"+_s+"_"+_d+"&=__BUF_SIZE_MASK_"+_s+"_"+_d+";\n");
                p.print("    #endif\n");

                p.print("  #else\n");
                p.print("  "+s.producer_name()+".push(tmp);\n");
                p.print("  #endif\n");
            }
        
        } else if (splitter.getType().equals(SIRSplitType.ROUND_ROBIN) ||
                   splitter.getType().equals(SIRSplitType.WEIGHTED_RR)) {

        
            int sum = splitter.getSumOfWeights();
            int offs = 0;

            int _s1 = in.getSource();
            int _d1 = in.getDest();

	    // depending on the weight unroll fully or by some constant factor

            if (sum > 128) {

                // big weight do not unroll everything!!

                for (int i = 0; i < out.size(); i++) {
                    int num = splitter.getWeight(i);
                    NetStream s = (NetStream)out.elementAt(i);      
            
                    int _s2 = s.getSource();
                    int _d2 = s.getDest();
        
                    //p.println("// ClusterCode_2");
		    
		    int step = 8;

                    p.print("  for (int k = 0; k < "+num/step+"; k++) {\n");
            
                    for (int y = 0; y < step; y++) {
            
                        // Destination
            
                        p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                        p.print("    #ifdef __NOMOD_"+_s2+"_"+_d2+"\n");
                        p.print("      BUFFER_"+_s2+"_"+_d2+"[HEAD_"+_s2+"_"+_d2+" + "+y+"] = \n");
                        p.print("    #else\n");
                        p.print("      BUFFER_"+_s2+"_"+_d2+"[(HEAD_"+_s2+"_"+_d2+" + "+y+") & __BUF_SIZE_MASK_"+_s2+"_"+_d2+"] = \n");
                        p.print("    #endif\n");
                        p.print("  #else\n");
                        p.print("    "+s.producer_name()+".push(\n");
                        p.print("  #endif\n");
            
                        // Source
            
                        p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
                        p.print("    #ifdef __NOMOD_"+_s1+"_"+_d1+"\n");
                        p.print("      BUFFER_"+_s1+"_"+_d1+"[TAIL_"+_s1+"_"+_d1+" + "+y+"]\n");
                        p.print("    #else\n");
                        p.print("      BUFFER_"+_s1+"_"+_d1+"[(TAIL_"+_s1+"_"+_d1+" + "+y+") & __BUF_SIZE_MASK_"+_s1+"_"+_d1+"]\n");
                        p.print("    #endif\n");
                        p.print("  #else\n");
            
                        if (ClusterFusion.fusedWith(node).contains(source)) {
                            p.print("    "+in.pop_name()+"()\n");
                        } else {        
                            p.print("    "+in.consumer_name()+".pop()\n");
                        }
            
                        p.print("  #endif\n");
            
                        // Close Assignement to Dest or Push Operator
            
                        p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                        p.print("    ; // assignement\n");
                        p.print("  #else\n");           
                        p.print("    ); // push()\n");
                        p.print("  #endif\n");
            
                    }
            
                    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                    p.print("    HEAD_"+_s2+"_"+_d2+" += "+step+";\n");
                    p.print("    #ifndef __NOMOD_"+_s2+"_"+_d2+"\n");
                    p.print("    HEAD_"+_s2+"_"+_d2+" &= __BUF_SIZE_MASK_"+_s2+"_"+_d2+";\n");
                    p.print("    #endif\n");
                    p.print("  #endif\n");
            
                    p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
                    p.print("    TAIL_"+_s1+"_"+_d1+" += "+step+";\n");
                    p.print("    #ifndef __NOMOD_"+_s1+"_"+_d1+"\n");
                    p.print("    TAIL_"+_s1+"_"+_d1+" &= __BUF_SIZE_MASK_"+_s1+"_"+_d1+";\n");
                    p.print("    #endif\n");
                    p.print("  #endif\n");

                    p.print("  }\n");

                    // remainder

                    int rem = num % step;

                    for (int y = 0; y < rem; y++) {
            
                        // Destination
            
                        p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                        p.print("    #ifdef __NOMOD_"+_s2+"_"+_d2+"\n");
                        p.print("      BUFFER_"+_s2+"_"+_d2+"[HEAD_"+_s2+"_"+_d2+" + "+y+"] = \n");
                        p.print("    #else\n");
                        p.print("      BUFFER_"+_s2+"_"+_d2+"[(HEAD_"+_s2+"_"+_d2+" + "+y+") & __BUF_SIZE_MASK_"+_s2+"_"+_d2+"] = \n");
                        p.print("    #endif\n");
                        p.print("  #else\n");
                        p.print("    "+s.producer_name()+".push(\n");
                        p.print("  #endif\n");
            
                        // Source
            
                        p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
                        p.print("    #ifdef __NOMOD_"+_s1+"_"+_d1+"\n");
                        p.print("      BUFFER_"+_s1+"_"+_d1+"[TAIL_"+_s1+"_"+_d1+" + "+y+"]\n");
                        p.print("    #else\n");
                        p.print("      BUFFER_"+_s1+"_"+_d1+"[(TAIL_"+_s1+"_"+_d1+" + "+y+") & __BUF_SIZE_MASK_"+_s1+"_"+_d1+"]\n");
                        p.print("    #endif\n");
                        p.print("  #else\n");
            
                        if (ClusterFusion.fusedWith(node).contains(source)) {
                            p.print("    "+in.pop_name()+"()\n");
                        } else {        
                            p.print("    "+in.consumer_name()+".pop()\n");
                        }
            
                        p.print("  #endif\n");
            
                        // Close Assignement to Dest or Push Operator
            
                        p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                        p.print("    ; // assignement\n");
                        p.print("  #else\n");           
                        p.print("    ); // push()\n");
                        p.print("  #endif\n");
            
                    }
            
                    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                    p.print("    HEAD_"+_s2+"_"+_d2+" += "+rem+";\n");
                    p.print("    #ifndef __NOMOD_"+_s2+"_"+_d2+"\n");
                    p.print("    HEAD_"+_s2+"_"+_d2+" &= __BUF_SIZE_MASK_"+_s2+"_"+_d2+";\n");
                    p.print("    #endif\n");
                    p.print("  #endif\n");
            
                    p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
                    p.print("    TAIL_"+_s1+"_"+_d1+" += "+rem+";\n");
                    p.print("    #ifndef __NOMOD_"+_s1+"_"+_d1+"\n");
                    p.print("    TAIL_"+_s1+"_"+_d1+" &= __BUF_SIZE_MASK_"+_s1+"_"+_d1+";\n");
                    p.print("    #endif\n");
                    p.print("  #endif\n");
                }
        
            } else {

        

                for (int i = 0; i < out.size(); i++) {
                    int num = splitter.getWeight(i);
                    NetStream s = (NetStream)out.elementAt(i);      
            
                    int _s2 = s.getSource();
                    int _d2 = s.getDest();
            
                    for (int y = 0; y < num; y++) {
            
                        // Destination
            
                        p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                        p.print("    #ifdef __NOMOD_"+_s2+"_"+_d2+"\n");
                        p.print("      BUFFER_"+_s2+"_"+_d2+"[HEAD_"+_s2+"_"+_d2+" + "+y+"] = \n");
                        p.print("    #else\n");
                        p.print("      BUFFER_"+_s2+"_"+_d2+"[(HEAD_"+_s2+"_"+_d2+" + "+y+") & __BUF_SIZE_MASK_"+_s2+"_"+_d2+"] = \n");
                        p.print("    #endif\n");
                        p.print("  #else\n");
                        p.print("    "+s.producer_name()+".push(\n");
                        p.print("  #endif\n");
            
                        // Source
            
                        p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
                        p.print("    #ifdef __NOMOD_"+_s1+"_"+_d1+"\n");
                        p.print("      BUFFER_"+_s1+"_"+_d1+"[TAIL_"+_s1+"_"+_d1+" + "+offs+"]\n");
                        p.print("    #else\n");
                        p.print("      BUFFER_"+_s1+"_"+_d1+"[(TAIL_"+_s1+"_"+_d1+" + "+offs+") & __BUF_SIZE_MASK_"+_s1+"_"+_d1+"]\n");
                        p.print("    #endif\n");
                        p.print("  #else\n");
            
                        if (ClusterFusion.fusedWith(node).contains(source)) {
                            p.print("    "+in.pop_name()+"()\n");
                        } else {        
                            p.print("    "+in.consumer_name()+".pop()\n");
                        }
            
                        p.print("  #endif\n");
            
                        // Close Assignement to Dest or Push Operator
            
                        p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                        p.print("    ; // assignement\n");
                        p.print("  #else\n");           
                        p.print("    ); // push()\n");
                        p.print("  #endif\n");
            
                        offs++;
                    }
            
                    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
                    p.print("    HEAD_"+_s2+"_"+_d2+" += "+num+";\n");
                    p.print("    #ifndef __NOMOD_"+_s2+"_"+_d2+"\n");
                    p.print("    HEAD_"+_s2+"_"+_d2+" &= __BUF_SIZE_MASK_"+_s2+"_"+_d2+";\n");
                    p.print("    #endif\n");
                    p.print("  #endif\n");
            
                }
        
                p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
                p.print("    TAIL_"+_s1+"_"+_d1+" += "+sum+";\n");
                p.print("    #ifndef __NOMOD_"+_s1+"_"+_d1+"\n");
                p.print("    TAIL_"+_s1+"_"+_d1+" &= __BUF_SIZE_MASK_"+_s1+"_"+_d1+";\n");
                p.print("    #endif\n");
                p.print("  #endif\n");
        
                /*
                  int sum = splitter.getSumOfWeights();
                  int offs = 0;
        
                  p.print("  "+baseType.toString()+" tmp["+sum+"];\n");
                  p.newLine();

                  _s = in.getSource();
                  _d = in.getDest();
        
                  p.print("  #ifdef __FUSED_"+_s+"_"+_d+"\n");

                  for (int y = 0; y < sum; y++) {

                  p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
                  p.print("    tmp["+y+"] = BUFFER_"+_s+"_"+_d+"[TAIL_"+_s+"_"+_d+"];TAIL_"+_s+"_"+_d+"++;\n");
                  p.print("    #else\n");
                  p.print("    tmp["+y+"] = BUFFER_"+_s+"_"+_d+"[TAIL_"+_s+"_"+_d+"];TAIL_"+_s+"_"+_d+"++;TAIL_"+_s+"_"+_d+"&=__BUF_SIZE_MASK_"+_s+"_"+_d+";\n");
                  p.print("    #endif\n");

                  }

                  p.print("  #else\n");

                  if (ClusterFusion.fusedWith(node).contains(source)) {
                  for (int y = 0; y < sum; y++) {
                  p.print("  tmp["+y+"] = "+in.pop_name()+"();\n");
                  }
                  } else {      
                  p.print("  "+in.consumer_name()+".pop_items(tmp, "+sum+");\n");
                  }

                  p.print("  #endif\n");
                  p.newLine();
        
                  for (int i = 0; i < out.size(); i++) {
                  int num = splitter.getWeight(i);
                  NetStream s = (NetStream)out.elementAt(i);        

                  _s = s.getSource();
                  _d = s.getDest();

                  p.print("  #ifdef __FUSED_"+_s+"_"+_d+"\n");

                  for (int y = 0; y < num; y++) {

                  p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
                  p.print("    BUFFER_"+_s+"_"+_d+"[HEAD_"+_s+"_"+_d+"]=tmp["+(offs+y)+"];HEAD_"+_s+"_"+_d+"++;\n");
                  p.print("    #else\n");
                  p.print("    BUFFER_"+_s+"_"+_d+"[HEAD_"+_s+"_"+_d+"]=tmp["+(offs+y)+"];HEAD_"+_s+"_"+_d+"++;HEAD_"+_s+"_"+_d+"&=__BUF_SIZE_MASK_"+_s+"_"+_d+";\n");
                  p.print("    #endif\n");

                  }

                  p.print("  #else\n");

                  p.print("  "+s.producer_name()+".push_items(&tmp["+offs+"], "+num+");\n");

                  p.print("  #endif\n");

                  offs += num;
                  }
                */
            }
        }

        p.print("  }\n");
        p.print("}\n");
        p.newLine();


        //  +=============================+
        //  | Splitter Main               |
        //  +=============================+

        /*
          p.print("void __splitter_"+thread_id+"_main() {\n");
          p.print("  int i, ii;\n");
    
          p.print("  if (__steady_"+thread_id+" == 0) {\n");
          p.print("    for (i = 0; i < "+init_counts+"; i++) {\n");
          p.print("      check_thread_status(__state_flag_"+thread_id+",__thread_"+thread_id+");\n");
          p.print("      __splitter_"+thread_id+"_work(1);\n");
          p.print("    }\n");
          p.print("  }\n");
          p.print("  __steady_"+thread_id+"++;\n");

          p.print("  while (__number_of_iterations_"+thread_id+" > 1000) {\n");
          p.print("    check_thread_status(__state_flag_"+thread_id+",__thread_"+thread_id+");\n");
          p.print("    __splitter_"+thread_id+"_work_1k();\n");
          p.print("    __number_of_iterations_"+thread_id+" -= 1000;\n");
          p.print("    __steady_"+thread_id+" += 1000;\n");
          p.print("    if (__frequency_of_chkpts != 0 && __steady_"+thread_id+" % __frequency_of_chkpts == 0) save_state::save_to_file(__thread_"+thread_id+", __steady_"+thread_id+", __write_thread__"+thread_id+");\n");
          p.print("  }\n");
    
          p.print("  for (i = 1; i <= __number_of_iterations_"+thread_id+"; i++, __steady_"+thread_id+"++) {\n");   
          p.print("    for (ii = 0; ii < "+steady_counts+"; ii++) {\n");
          p.print("      check_thread_status(__state_flag_"+thread_id+",__thread_"+thread_id+");\n");
          p.print("      __splitter_"+thread_id+"_work(1);\n");
          p.print("    }\n");

          p.print("    if (__frequency_of_chkpts != 0 && __steady_"+thread_id+" % __frequency_of_chkpts == 0) save_state::save_to_file(__thread_"+thread_id+", __steady_"+thread_id+", __write_thread__"+thread_id+");\n");

          p.print("  }\n");

          p.print("}\n");

        */

        //  +=============================+
        //  | Run Function                |
        //  +=============================+

        Vector run = gen.generateRunFunction(null, "__splitter_"+thread_id+"_main", new Vector());

        for (int i = 0; i < run.size(); i++) {
            p.print(run.elementAt(i).toString());
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
    
        System.out.println("Code for " + node.contents.getName() +
                           " written to thread"+thread_id+".cpp");

    }


    /**
     *  Generate and emit code for a joiner
     */

    public static void generateJoiner(FlatNode node) {
        
        SIRJoiner joiner = (SIRJoiner)node.contents;

        if (joiner.getSumOfWeights() == 0) {
            // The joiner is not doing any work
        
            return;
        }

        int init_counts, steady_counts;

        Integer init_int = (Integer)ClusterBackend.initExecutionCounts.get(node);
        if (init_int==null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();
        CType baseType = Util.getBaseType(Util.getJoinerType(node));
        int thread_id = NodeEnumerator.getSIROperatorId(node.contents);

        Vector in = (Vector)RegisterStreams.getNodeInStreams(node.contents);
        Vector out_v = (Vector)RegisterStreams.getNodeOutStreams(node.contents);
        NetStream out = (NetStream)out_v.elementAt(0);

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

        p.print("void save_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}\n");
        p.print("void load_peek_buffer__"+thread_id+"(object_write_buffer *buf) {}\n");
        p.newLine();

	p.println("void save_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
	p.println("void load_file_pointer__"+thread_id+"(object_write_buffer *buf) {}");
        p.newLine();


        //  +=============================+
        //  | Joiner Pop                  |
        //  +=============================+


        int sum_of_weights = joiner.getSumOfWeights();

        p.print(baseType.toString()+" "+out.pop_buffer()+"["+sum_of_weights+"];\n");
        p.print("int "+out.pop_index()+" = "+sum_of_weights+";\n");
        p.newLine();

        p.print(baseType.toString()+" "+out.pop_name()+"() {\n");

        p.print("  if ("+out.pop_index()+" == "+sum_of_weights+") {\n");
        int _offs = 0;
        
        //int ways = joiner.getWays();
        for (int i = 0; i < in.size(); i++) {
            int num = joiner.getWeight(i);
            
            NetStream s = (NetStream)in.elementAt(i);
            FlatNode source = NodeEnumerator.getFlatNode(s.getSource());

            if (ClusterFusion.fusedWith(node).contains(source)) {
                for (int y = 0; y < num; y++) {
                    p.print("    "+out.pop_buffer()+"["+(_offs + y)+"] = "+s.pop_name()+"();\n");
                }
            } else {
                p.print("    "+s.consumer_name()+".pop_items(&"+out.pop_buffer()+"["+_offs+"], "+num+");\n");
            }

            _offs += num;
        }
        p.print("    "+out.pop_index()+" = 0;\n");
        p.print("  }\n");
    
        p.print("  return "+out.pop_buffer()+"["+out.pop_index()+"++];\n");
        p.print("}\n");
        p.newLine();

        //  +=============================+
        //  | Init Path                   |
        //  +=============================+


        if (joiner.getParent() instanceof SIRFeedbackLoop) {
        
            p.print("int __init_counter_"+thread_id+" = 0;\n"); // INVALID
            p.newLine();

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

        //  +=============================+
        //  | Joiner Work                 |
        //  +=============================+

        int _s = out.getSource();
        int _d = out.getDest();
        p.print("#ifdef __FUSED_"+_s+"_"+_d+"\n");
        p.print("extern "+baseType.toString()+" BUFFER_"+_s+"_"+_d+"[];\n");
        p.print("extern int HEAD_"+_s+"_"+_d+";\n");
        p.print("extern int TAIL_"+_s+"_"+_d+";\n");
        p.print("#endif\n");

        for (int o = 0; o < in.size(); o++) {
            _s = ((NetStream)in.elementAt(o)).getSource();
            _d = ((NetStream)in.elementAt(o)).getDest();
            p.print("#ifdef __FUSED_"+_s+"_"+_d+"\n");
            p.print("extern "+baseType.toString()+" BUFFER_"+_s+"_"+_d+"[];\n");
            p.print("extern int HEAD_"+_s+"_"+_d+";\n");
            p.print("extern int TAIL_"+_s+"_"+_d+";\n");
            p.print("#endif\n");
        }

        p.newLine();

        p.print("void __joiner_"+thread_id+"_work(int ____n) {\n");
        //p.println("// ClusterCode_3");
        p.print("  for (;____n > 0; ____n--) {\n");

        FlatNode dest_flat = NodeEnumerator.getFlatNode(out.getSource());

        if (joiner.getType().equals(SIRJoinType.ROUND_ROBIN) || 
            joiner.getType().equals(SIRJoinType.WEIGHTED_RR)) {

            if (joiner.getParent() instanceof SIRFeedbackLoop) {
        
                //
                // Joiner is a part of feedback Loop
                //

                p.print("  "+baseType.toString()+" tmp;\n");
        
                for (int i = 0; i < in.size(); i++) {
            
                    NetStream s = (NetStream)in.elementAt(i);       
                    int num = joiner.getWeight(i);
            
                    for (int ii = 0; ii < num; ii++) {
            
                        if (i == 1 && joiner.getParent() instanceof SIRFeedbackLoop) {
                            int delay =  ((SIRFeedbackLoop)joiner.getParent()).getDelayInt();
                            p.print("  if (__init_counter_"+thread_id+" < "+delay+") {\n");
                            p.print("    tmp = __Init_Path_"+thread_id+"(__init_counter_"+thread_id+");\n");
                            p.print("    __init_counter_"+thread_id+"++;\n");
                            p.print("  } else\n");
                            p.print("    tmp = "+s.consumer_name()+".pop();\n");
                
                        } else {
                
                            p.print("  tmp = "+s.consumer_name()+".pop();\n");
                        }
            
                        p.print("  "+out.producer_name()+".push(tmp);\n");
                    }
            
                }

            } else {


                //
                // Joiner is NOT a part of feedback Loop
                //


                int sum = joiner.getSumOfWeights();
                int offs = 0;

                int _s2 = out.getSource();
                int _d2 = out.getDest();

		// depending on the weight unroll fully or by some constant factor

		if (sum < 128) {

		    // unroll fully!

		    for (int i = 0; i < in.size(); i++) {
			int num = joiner.getWeight(i);
			NetStream s = (NetStream)in.elementAt(i);       
			
			int _s1 = s.getSource();
			int _d1 = s.getDest();
			
			for (int y = 0; y < num; y++) {
			    
			    // Destination
			    
			    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			    p.print("    #ifdef __NOMOD_"+_s2+"_"+_d2+"\n");
			    p.print("      BUFFER_"+_s2+"_"+_d2+"[HEAD_"+_s2+"_"+_d2+" + "+offs+"] = \n");
			    p.print("    #else\n");
			    p.print("      BUFFER_"+_s2+"_"+_d2+"[(HEAD_"+_s2+"_"+_d2+" + "+offs+") & __BUF_SIZE_MASK_"+_s2+"_"+_d2+"] = \n");
			    p.print("    #endif\n");
			    p.print("  #else\n");
			    
			    if (ClusterFusion.fusedWith(node).contains(dest_flat)) {
				p.print("    "+out.push_name()+"(\n");
			    } else {
				p.print("    "+out.producer_name()+".push(\n");
			    }
			    
			    p.print("  #endif\n");
			    
			    // Source
			    
			    p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
			    p.print("    #ifdef __NOMOD_"+_s1+"_"+_d1+"\n");
			    p.print("      BUFFER_"+_s1+"_"+_d1+"[TAIL_"+_s1+"_"+_d1+" + "+y+"]\n");
			    p.print("    #else\n");
			    p.print("      BUFFER_"+_s1+"_"+_d1+"[(TAIL_"+_s1+"_"+_d1+" + "+y+") & __BUF_SIZE_MASK_"+_s1+"_"+_d1+"]\n");
			    p.print("    #endif\n");
			    p.print("  #else\n");
			    p.print("    "+s.consumer_name()+".pop()\n");
			    p.print("  #endif\n");
			    
			    // Close Assignement to Dest or Push Operator
			    
			    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			    p.print("    ; // assignement\n");
			    p.print("  #else\n");           
			    p.print("    ); // push()\n");
			    p.print("  #endif\n");
			    
			    offs++;
			}
			
			p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
			p.print("    TAIL_"+_s1+"_"+_d1+" += "+num+";\n");
			p.print("    #ifndef __NOMOD_"+_s1+"_"+_d1+"\n");
			p.print("    TAIL_"+_s1+"_"+_d1+" &= __BUF_SIZE_MASK_"+_s1+"_"+_d1+";\n");
			p.print("    #endif\n");
			p.print("  #endif\n");
			
		    }
        
		    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
		    p.print("    HEAD_"+_s2+"_"+_d2+" += "+sum+";\n");
		    p.print("    #ifndef __NOMOD_"+_s2+"_"+_d2+"\n");
		    p.print("    HEAD_"+_s2+"_"+_d2+" &= __BUF_SIZE_MASK_"+_s2+"_"+_d2+";\n");
		    p.print("    #endif\n");
		    p.print("  #endif\n");

		} else {

		    // do not unroll fully weight >= 128

		    for (int i = 0; i < in.size(); i++) {
			int num = joiner.getWeight(i);
			NetStream s = (NetStream)in.elementAt(i);       
			
			int _s1 = s.getSource();
			int _d1 = s.getDest();
			
			int step = 8;

			p.println("  for (int k = 0; k < "+num/step+"; k++) {\n");

			for (int y = 0; y < step; y++) {
			    
			    // Destination
			    
			    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			    p.print("    #ifdef __NOMOD_"+_s2+"_"+_d2+"\n");
			    p.print("      BUFFER_"+_s2+"_"+_d2+"[HEAD_"+_s2+"_"+_d2+" + "+y+"] = \n");
			    p.print("    #else\n");
			    p.print("      BUFFER_"+_s2+"_"+_d2+"[(HEAD_"+_s2+"_"+_d2+" + "+y+") & __BUF_SIZE_MASK_"+_s2+"_"+_d2+"] = \n");
			    p.print("    #endif\n");
			    p.print("  #else\n");
			    
			    if (ClusterFusion.fusedWith(node).contains(dest_flat)) {
				p.print("    "+out.push_name()+"(\n");
			    } else {
				p.print("    "+out.producer_name()+".push(\n");
			    }
			    
			    p.print("  #endif\n");
			    
			    // Source
			    
			    p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
			    p.print("    #ifdef __NOMOD_"+_s1+"_"+_d1+"\n");
			    p.print("      BUFFER_"+_s1+"_"+_d1+"[TAIL_"+_s1+"_"+_d1+" + "+y+"]\n");
			    p.print("    #else\n");
			    p.print("      BUFFER_"+_s1+"_"+_d1+"[(TAIL_"+_s1+"_"+_d1+" + "+y+") & __BUF_SIZE_MASK_"+_s1+"_"+_d1+"]\n");
			    p.print("    #endif\n");
			    p.print("  #else\n");
			    p.print("    "+s.consumer_name()+".pop()\n");
			    p.print("  #endif\n");
			    
			    // Close Assignement to Dest or Push Operator
			    
			    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			    p.print("    ; // assignement\n");
			    p.print("  #else\n");           
			    p.print("    ); // push()\n");
			    p.print("  #endif\n");
			    
			    offs++;
			}

			p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
			p.print("    TAIL_"+_s1+"_"+_d1+" += "+step+";\n");
			p.print("    #ifndef __NOMOD_"+_s1+"_"+_d1+"\n");
			p.print("    TAIL_"+_s1+"_"+_d1+" &= __BUF_SIZE_MASK_"+_s1+"_"+_d1+";\n");
			p.print("    #endif\n");
			p.print("  #endif\n");
			
        
			p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			p.print("    HEAD_"+_s2+"_"+_d2+" += "+step+";\n");
			p.print("    #ifndef __NOMOD_"+_s2+"_"+_d2+"\n");
			p.print("    HEAD_"+_s2+"_"+_d2+" &= __BUF_SIZE_MASK_"+_s2+"_"+_d2+";\n");
			p.print("    #endif\n");
			p.print("  #endif\n");

			p.println("  }\n");			

			int rem = num % step;

			for (int y = 0; y < rem; y++) {
			    
			    // Destination
			    
			    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			    p.print("    #ifdef __NOMOD_"+_s2+"_"+_d2+"\n");
			    p.print("      BUFFER_"+_s2+"_"+_d2+"[HEAD_"+_s2+"_"+_d2+" + "+y+"] = \n");
			    p.print("    #else\n");
			    p.print("      BUFFER_"+_s2+"_"+_d2+"[(HEAD_"+_s2+"_"+_d2+" + "+y+") & __BUF_SIZE_MASK_"+_s2+"_"+_d2+"] = \n");
			    p.print("    #endif\n");
			    p.print("  #else\n");
			    
			    if (ClusterFusion.fusedWith(node).contains(dest_flat)) {
				p.print("    "+out.push_name()+"(\n");
			    } else {
				p.print("    "+out.producer_name()+".push(\n");
			    }
			    
			    p.print("  #endif\n");
			    
			    // Source
			    
			    p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
			    p.print("    #ifdef __NOMOD_"+_s1+"_"+_d1+"\n");
			    p.print("      BUFFER_"+_s1+"_"+_d1+"[TAIL_"+_s1+"_"+_d1+" + "+y+"]\n");
			    p.print("    #else\n");
			    p.print("      BUFFER_"+_s1+"_"+_d1+"[(TAIL_"+_s1+"_"+_d1+" + "+y+") & __BUF_SIZE_MASK_"+_s1+"_"+_d1+"]\n");
			    p.print("    #endif\n");
			    p.print("  #else\n");
			    p.print("    "+s.consumer_name()+".pop()\n");
			    p.print("  #endif\n");
			    
			    // Close Assignement to Dest or Push Operator
			    
			    p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			    p.print("    ; // assignement\n");
			    p.print("  #else\n");           
			    p.print("    ); // push()\n");
			    p.print("  #endif\n");
			    
			    offs++;
			}

			p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
			p.print("    TAIL_"+_s1+"_"+_d1+" += "+rem+";\n");
			p.print("    #ifndef __NOMOD_"+_s1+"_"+_d1+"\n");
			p.print("    TAIL_"+_s1+"_"+_d1+" &= __BUF_SIZE_MASK_"+_s1+"_"+_d1+";\n");
			p.print("    #endif\n");
			p.print("  #endif\n");
			
        
			p.print("  #ifdef __FUSED_"+_s2+"_"+_d2+"\n");
			p.print("    HEAD_"+_s2+"_"+_d2+" += "+rem+";\n");
			p.print("    #ifndef __NOMOD_"+_s2+"_"+_d2+"\n");
			p.print("    HEAD_"+_s2+"_"+_d2+" &= __BUF_SIZE_MASK_"+_s2+"_"+_d2+";\n");
			p.print("    #endif\n");
			p.print("  #endif\n");
			

		    }
		}

                /*


                int sum = joiner.getSumOfWeights();
                int offs = 0;
        
                p.print("  "+baseType.toString()+" tmp["+sum+"];\n");
                p.newLine();

                for (int i = 0; i < in.size(); i++) {

                int num = joiner.getWeight(i);
                NetStream s = (NetStream)in.elementAt(i);       
                _s = s.getSource();
                _d = s.getDest();

                p.print("  #ifdef __FUSED_"+_s+"_"+_d+"\n");

                for (int y = 0; y < num; y++) {

                p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
                p.print("    tmp["+(offs+y)+"] = BUFFER_"+_s+"_"+_d+"[TAIL_"+_s+"_"+_d+"];TAIL_"+_s+"_"+_d+"++;\n");
                p.print("    #else\n");
                p.print("    tmp["+(offs+y)+"] = BUFFER_"+_s+"_"+_d+"[TAIL_"+_s+"_"+_d+"];TAIL_"+_s+"_"+_d+"++;TAIL_"+_s+"_"+_d+"&=__BUF_SIZE_MASK_"+_s+"_"+_d+";\n");
                p.print("    #endif\n");
            
                }

                p.print("  #else\n");

                p.print("  "+s.consumer_name()+".pop_items(&tmp["+offs+"], "+num+");\n");

                p.print("  #endif\n");
            
                offs += num;
            
                }

                p.newLine();

                _s = out.getSource();
                _d = out.getDest();

                p.print("  #ifdef __FUSED_"+_s+"_"+_d+"\n");

                for (int y = 0; y < sum; y++) {

                p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
                p.print("    BUFFER_"+_s+"_"+_d+"[HEAD_"+_s+"_"+_d+"]=tmp["+y+"];HEAD_"+_s+"_"+_d+"++;\n");
                p.print("    #else\n");
                p.print("    BUFFER_"+_s+"_"+_d+"[HEAD_"+_s+"_"+_d+"]=tmp["+y+"];HEAD_"+_s+"_"+_d+"++;HEAD_"+_s+"_"+_d+"&=__BUF_SIZE_MASK_"+_s+"_"+_d+";\n");
                p.print("    #endif\n");

                }

                p.print("  #else\n");
        
                p.print("  "+out.producer_name()+".push_items(tmp, "+sum+");\n");
                p.print("  #endif\n");


                */
            }
        }

        p.print("  }\n");
        p.print("}\n");

        p.newLine();

        //  +=============================+
        //  | Joiner Main                 |
        //  +=============================+

	/*
        p.print("void __joiner_"+thread_id+"_main() {\n");
        p.print("  int i, ii;\n");
    
        if (init_counts > 0) {
            p.print("  if (__steady_" + thread_id + " == 0) {\n");
            if (init_counts > 1) {
                p.print("    for (i = 0; i < " + init_counts + "; i++) {\n");
            }
            p.print("      check_thread_status(__state_flag_" + thread_id
                    + ",__thread_" + thread_id + ");\n");
            p.print("      __joiner_" + thread_id + "_work(1);\n");
            if (init_counts > 1) {
                p.print("    }\n");
            }
            p.print("  }\n");
        }
        p.print("  __steady_"+thread_id+"++;\n");

        if (steady_counts > 0) {
            //p.println("// ClusterCode_4");
            p.print("  for (i = 1; i <= __number_of_iterations_" + thread_id
                    + "; i++, __steady_" + thread_id + "++) {\n");
            if (steady_counts > 1) {
                p.print("    for (ii = 0; ii < " + steady_counts
                        + "; ii++) {\n");
            }
            p.print("      check_thread_status(__state_flag_" + thread_id
                    + ",__thread_" + thread_id + ");\n");
            p.print("      __joiner_" + thread_id + "_work(1);\n");
            if (steady_counts > 1) {
                p.print("    }\n");
            }
        }
    
        p.print("    if (__frequency_of_chkpts != 0 && i % __frequency_of_chkpts == 0) save_state::save_to_file(__thread_"+thread_id+", __steady_"+thread_id+", __write_thread__"+thread_id+");\n");

        p.print("  }\n");

        p.print("}\n");
	*/

        //  +=============================+
        //  | Run Function                |
        //  +=============================+

        Vector run = gen.generateRunFunction(null, "__joiner_"+thread_id+"_main", new Vector());

        for (int i = 0; i < run.size(); i++) {
            p.print(run.elementAt(i).toString());
        }

        //  +=============================+
        //  | Write Joiner to File        |
        //  +=============================+

        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write joiner code to file thread"+thread_id+".cpp");
        }

        System.out.println("Code for " + node.contents.getName() +
                           " written to thread"+thread_id+".cpp");


    }
}

    

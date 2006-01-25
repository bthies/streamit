
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
//import at.dms.kjc.cluster.*;
import at.dms.kjc.sir.*;
//import at.dms.util.Utils;
import java.util.Vector;
//import java.util.List;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.raw.Util;
//import at.dms.kjc.sir.lowering.*;
//import java.util.ListIterator;
//import java.util.Iterator;
//import java.util.LinkedList;
import java.util.HashMap;
//import java.util.TreeSet;
import java.util.HashSet;
import java.io.*;
//import java.lang.*;
import at.dms.kjc.cluster.ClusterUtils;

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class ClusterCode extends at.dms.util.Utils implements FlatVisitor {
    // the max-ahead is the maximum number of lines that this will
    // recognize as a pattern for folding into a loop
    //    private static final int MAX_LOOKAHEAD = 20;

    //Hash set of tiles mapped to filters or joiners
    //all other tiles are routing tiles
    public static HashSet realTiles;
    public static HashSet tiles;

    public static final String ARRAY_INDEX = "__ARRAY_INDEX__";

    //private static HashMap partitionMap;

    /**
     * Set private variable partitionMap, which is never read.
     * 
     * Is this for Debugging or just a code maintenance glitch?
     * 
     * @param pmap
     */ 
    public static void setPartitionMap(HashMap pmap) {
        //partitionMap = pmap;
    }

    public static void generateCode(FlatNode topLevel) 
    {
        //generate the code for all tiles 
    
        realTiles = new HashSet();
        topLevel.accept(new ClusterCode(), new HashSet(), true);
        tiles = new HashSet();
    
    }

   
    //generate the code for the tiles containing filters and joiners
    //remember which tiles we have generated code for
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
    
        CodegenPrintWriter p = new CodegenPrintWriter();

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
            FileWriter fw = new FileWriter("thread_"+id+".cpp");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write code to file thread_"+id+".cpp");
        }
    
        System.out.println("Code for " + node.contents.getName() +
                           " written to thread_"+id+".cpp");

    }




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
    
        CodegenPrintWriter p = new CodegenPrintWriter();

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
            FileWriter fw = new FileWriter("thread_"+id+".cpp");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write code to file thread_"+id+".cpp");
        }
    
        System.out.println("Code for " + node.contents.getName() +
                           " written to thread_"+id+".cpp");

    }


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
    
        CodegenPrintWriter p = new CodegenPrintWriter();

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

        //  +=============================+
        //  | Splitter Push               |
        //  +=============================+

        int sum_of_weights = splitter.getSumOfWeights();

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

        for (int ch = 0; ch < splitter.getWays(); ch++) {

            NetStream _out = (NetStream)out.elementAt(ch);
            int weight = splitter.getWeight(ch);

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

            if (sum > 256) {

                // big weight do not unroll everything!!

                for (int i = 0; i < out.size(); i++) {
                    int num = splitter.getWeight(i);
                    NetStream s = (NetStream)out.elementAt(i);      
            
                    int _s2 = s.getSource();
                    int _d2 = s.getDest();
        
                    //p.println("// ClusterCode_2");
                    p.print("  for (int k = 0; k < "+num/32+"; k++) {\n");
            
                    for (int y = 0; y < 32; y++) {
            
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
                    p.print("    HEAD_"+_s2+"_"+_d2+" += 32;\n");
                    p.print("    #ifndef __NOMOD_"+_s2+"_"+_d2+"\n");
                    p.print("    HEAD_"+_s2+"_"+_d2+" &= __BUF_SIZE_MASK_"+_s2+"_"+_d2+";\n");
                    p.print("    #endif\n");
                    p.print("  #endif\n");
            
                    p.print("  #ifdef __FUSED_"+_s1+"_"+_d1+"\n");
                    p.print("    TAIL_"+_s1+"_"+_d1+" += 32;\n");
                    p.print("    #ifndef __NOMOD_"+_s1+"_"+_d1+"\n");
                    p.print("    TAIL_"+_s1+"_"+_d1+" &= __BUF_SIZE_MASK_"+_s1+"_"+_d1+";\n");
                    p.print("    #endif\n");
                    p.print("  #endif\n");

                    p.print("  }\n");

                    // remainder

                    int rem = num % 32;

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
            FileWriter fw = new FileWriter("thread"+thread_id+".cpp");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write splitter code to file thread"+thread_id+".cpp");
        }
    
        System.out.println("Code for " + node.contents.getName() +
                           " written to thread"+thread_id+".cpp");

    }


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

        CodegenPrintWriter p = new CodegenPrintWriter();
        
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
            FileWriter fw = new FileWriter("thread"+thread_id+".cpp");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write joiner code to file thread"+thread_id+".cpp");
        }

        System.out.println("Code for " + node.contents.getName() +
                           " written to thread"+thread_id+".cpp");


    }

    public static void generateGlobal(SIRGlobal global, SIRHelper[] helpers) {

        String str = new String();
        JFieldDeclaration fields[];

        if (global == null) {
            fields = new JFieldDeclaration[0];
        } else {
            fields = global.getFields();
        }

        // ================================
        // Writing global.h
        // ================================

        str += "#include <math.h>\n";
        str += "#include \"structs.h\"\n";
        str += "#include <StreamItVectorLib.h>\n";
        str += "\n";

        str += "#define max(A,B) (((A)>(B))?(A):(B))\n";
        str += "#define min(A,B) (((A)<(B))?(A):(B))\n";

        str += "\n";
    
        for (int i = 0; i < helpers.length; i++) {
            JMethodDeclaration[] m = helpers[i].getMethods();
            for (int j = 0; j < m.length; j++) {
                FlatIRToCluster f2c = new FlatIRToCluster();
                f2c.helper_package = helpers[i].getIdent();
                f2c.setDeclOnly(true);
                m[j].accept(f2c);
                str += "extern "+f2c.getPrinter().getString()+"\n";
            }
        }

        str += "extern void __global__init();\n";
        str += "\n";

        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();

            if (type.toString().endsWith("Portal")) continue;

            String ident = fields[f].getVariable().getIdent();
            str += "extern " + ClusterUtils.declToString(type, "__global__" + ident) + ";\n";
        }

        try {
            FileWriter fw = new FileWriter("global.h");
            fw.write(str.toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write <global.h>");
        }



        // ================================
        // Writing global.cpp
        // ================================

        str = ""; // reset string
        str += "#include <stdlib.h>\n";
        str += "#include <unistd.h>\n";
        str += "#include <math.h>\n";
        str += "#include \"global.h\"\n";
        str += "\n";

        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();

            if (type.toString().endsWith("Portal")) continue;

            JExpression init_val = fields[f].getVariable().getValue();
            String ident = fields[f].getVariable().getIdent();

            str += ClusterUtils.declToString(type, " __global__"+ident);

            if (init_val == null) {
                if (type.isOrdinal()) str += (" = 0");
                if (type.isFloatingPoint()) str += (" = 0.0f");
            }

            if (init_val != null && init_val instanceof JIntLiteral) {
                str += (" = "+((JIntLiteral)init_val).intValue());
            }

            if (init_val != null && init_val instanceof JFloatLiteral) {
                str += (" = "+((JFloatLiteral)init_val).floatValue());
            }

            str += (";\n");
        }

        for (int i = 0; i < helpers.length; i++) {
            if (!helpers[i].isNative()) {
                JMethodDeclaration[] m = helpers[i].getMethods();
                for (int j = 0; j < m.length; j++) {
                    FlatIRToCluster f2c = new FlatIRToCluster();
                    f2c.helper_package = helpers[i].getIdent();
                    f2c.setDeclOnly(false);
                    m[j].accept(f2c);
                    str += f2c.getPrinter().getString()+"\n";
                }
            }
        }

        if (global == null) {
            str += "void __global__init() { }";
        } else {
            FlatIRToCluster f2c = new FlatIRToCluster();
            f2c.setGlobal(true);
            f2c.setDeclOnly(false);
            global.getInit().accept(f2c);
            str += f2c.getPrinter().getString();
        }
        str += "\n";
    
        try {
            FileWriter fw = new FileWriter("global.cpp");
            fw.write(str.toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write <global.cpp>");
        }
    }


    public static void generateMasterFile() {

        int threadNumber = NodeEnumerator.getNumberOfNodes();

        CodegenPrintWriter p = new CodegenPrintWriter();
    
        p.print("#include <pthread.h>\n");
        p.print("#include <unistd.h>\n");
        p.print("#include <signal.h>\n");
        p.print("#include <string.h>\n");
        p.print("#include <stdlib.h>\n");
        p.print("#include <stdio.h>\n");
        p.newLine();
        p.print("#include <netsocket.h>\n");
        p.print("#include <node_server.h>\n");
        p.print("#include <init_instance.h>\n");
        p.print("#include <master_server.h>\n");
        p.print("#include <save_state.h>\n");
        p.print("#include <save_manager.h>\n");
        p.print("#include <delete_chkpts.h>\n");
        p.print("#include <object_write_buffer.h>\n");
        p.print("#include <read_setup.h>\n");
        p.print("#include <ccp.h>\n");
        p.print("#include \"global.h\"\n");
        if (KjcOptions.countops) {
            p.println("#include \"profiler.h\"");
        }
        p.newLine();

        p.print("int __max_iteration;\n");
        p.print("int __timer_enabled = 0;\n");
        p.print("int __frequency_of_chkpts;\n");
        p.print("int __out_data_buffer;\n");

        p.print("vector <thread_info*> thread_list;\n");
        p.print("netsocket *server = NULL;\n");
        p.print("unsigned __ccp_ip = 0;\n");
        p.print("int __init_iter = 0;\n");
        p.newLine();

        p.print("unsigned myip;\n");
        p.newLine();

        for (int i = 0; i < threadNumber; i++) {

            FlatNode tmp = NodeEnumerator.getFlatNode(i);
            if (!ClusterFusion.isEliminated(tmp)) {     
                p.print("extern void __declare_sockets_"+i+"();\n");
                p.print("extern thread_info *__get_thread_info_"+i+"();\n");
                p.print("extern void run_"+i+"();\n");
                p.print("pthread_attr_t __pthread_attr_"+i+";\n");
                p.print("pthread_t __pthread_"+i+";\n");
                p.print("static void *run_thread_"+i+"(void *param) {\n");
                p.print("  run_"+i+"();\n");
                p.print("}\n");
            }

        }

        p.newLine();

        p.print("static void *run_join(void *param) {\n");

        for (int i = 0; i < threadNumber; i++) {

            FlatNode tmp = NodeEnumerator.getFlatNode(i);
            if (!ClusterFusion.isEliminated(tmp)) {     
                p.print("  if (myip == init_instance::get_thread_ip("+i+")) {\n");
                p.print("    pthread_join(__pthread_"+i+", NULL);\n");
                p.print("  }\n");
            }
        }
        p.print("  sleep(1);\n");
        p.print("  exit(0);\n");

        p.print("}\n");

        p.newLine();

        p.print("int master_pid;\n");

        p.newLine();

        /*

        p.print("void sig_recv(int sig_nr) {\n");
        p.print("  if (master_pid == getpid()) {\n");
        p.print("    fprintf(stderr,\"\n data sent     : %d\\n\", mysocket::get_total_data_sent());\n");
        p.print("    fprintf(stderr,\" data received : %d\\n\", mysocket::get_total_data_received());\n");
        p.print("  }\n");
        p.print("}\n");

        p.newLine();

        */

        ///////////////////////////////////////
        // create sockets and threads

        p.print("void init() {\n");
    
        for (int i = 0; i < threadNumber; i++) {
            FlatNode tmp = NodeEnumerator.getFlatNode(i);
            if (!ClusterFusion.isEliminated(tmp)) {     
                p.print("  if (myip == init_instance::get_thread_ip("+i+")) {\n");
                p.print("    __declare_sockets_"+i+"();\n");
                p.print("  }\n");
            }
        }

        p.print("  init_instance::initialize_sockets();\n");

        //p.print("  pthread_t id;\n");

        for (int i = 0; i < threadNumber; i++) {

            FlatNode tmp = NodeEnumerator.getFlatNode(i);
            if (!ClusterFusion.isEliminated(tmp)) {
                // estimate stack size needed by this thread
                int stackSize = NodeEnumerator.getStackSize(i);
                p.print("  if (myip == init_instance::get_thread_ip("+i+")) {\n");
                p.print("    thread_info *info = __get_thread_info_"+i+"();\n"); 
                p.print("    int *state = info->get_state_flag();\n");
                p.print("    *state = RUN_STATE;\n");
                p.print("    pthread_attr_setstacksize(&__pthread_attr_"+i+", PTHREAD_STACK_MIN+"+stackSize+");\n");
                p.print("    pthread_create(&__pthread_"+i+", &__pthread_attr_"+i+", run_thread_"+i+", (void*)\"thread"+i+"\");\n");
                p.print("    info->set_pthread(__pthread_"+i+");\n");
                p.print("    info->set_active(true);\n");
                p.print("  }\n");
            }
        }

        p.print("  pthread_t id;\n");
        p.print("  pthread_create(&id, NULL, run_join, NULL);\n");

        p.print("}\n");

        p.newLine();

        p.print("int main(int argc, char **argv) {\n");

        p.print("  myip = get_myip();\n");

        // tell the profiler how many ID's there are
        if (KjcOptions.countops) {
            p.print("  profiler::set_num_ids(" + InsertCounters.getNumIds() + ");\n");
        }

        p.print("  read_setup::read_setup_file();\n");
        p.print("  __frequency_of_chkpts = read_setup::freq_of_chkpts;\n");
        p.print("  __out_data_buffer = read_setup::out_data_buffer;\n");
        p.print("  __max_iteration = read_setup::max_iteration;\n");

        p.print("  master_pid = getpid();\n");

        p.print("  for (int a = 1; a < argc; a++) {\n");

        p.print("    if (argc > a + 1 && strcmp(argv[a], \"-init\") == 0) {\n"); 
        p.print("       int tmp;\n");
        p.print("       sscanf(argv[a + 1], \"%d\", &tmp);\n");
        p.print("       fprintf(stderr,\"Initial Iteration: %d\\n\", tmp);\n"); 
        p.print("       __init_iter = tmp;"); 
        p.print("       init_instance::set_start_iter(__init_iter);"); 
        p.print("    }\n");

        p.print("    if (argc > a + 1 && strcmp(argv[a], \"-i\") == 0) {\n"); 
        p.print("       int tmp;\n");
        p.print("       sscanf(argv[a + 1], \"%d\", &tmp);\n");
        p.print("       fprintf(stderr,\"Number of Iterations: %d\\n\", tmp);\n"); 
        p.print("       __max_iteration = tmp;"); 
        p.print("    }\n");

        p.print("    if (strcmp(argv[a], \"-t\") == 0) {\n"); 
        p.print("       fprintf(stderr,\"Timer enabled.\\n\");\n"); 
        p.print("       __timer_enabled = 1;"); 
        p.print("    }\n");

        p.print("    if (argc > a + 1 && strcmp(argv[a], \"-ccp\") == 0) {\n");
        p.print("       fprintf(stderr,\"CCP address: %s\\n\", argv[a + 1]);\n"); 
        p.print("       __ccp_ip = lookup_ip(argv[a + 1]);\n");
        p.print("    }\n");

        p.print("    if (strcmp(argv[a], \"-runccp\") == 0) {\n");
        p.print("      ccp c;\n");
        p.print("      if (__init_iter > 0) c.set_init_iter(__init_iter);\n");
        p.print("      (new delete_chkpts())->start();\n");
        p.print("      c.run_ccp();\n");
        p.print("    }\n"); 

        p.print("    if (strcmp(argv[a], \"-console\") == 0) {\n");
        p.print("      char line[256], tmp;\n");
        p.print("      master_server *m = new master_server();\n");
        p.print("      m->print_commands();\n");
        p.print("      for (;;) {\n");
        p.print("        fprintf(stderr,\"master> \");fflush(stdout);\n");
        p.print("        line[0] = 0;\n");
        p.print("        scanf(\"%[^\\n]\", line);scanf(\"%c\", &tmp);\n");
        p.print("        m->process_command(line);\n");
        p.print("      }\n");
        p.print("    }\n");

        p.print("  }\n");

        p.newLine();

        p.print("  __global__init();\n");
        p.newLine();

        p.print("  thread_info *t_info;\n");

        for (int i = 0; i < threadNumber; i++) {
            FlatNode tmp = NodeEnumerator.getFlatNode(i);
            if (!ClusterFusion.isEliminated(tmp)) {     
                p.print("  t_info = __get_thread_info_"+i+"();\n"); 
                p.print("  thread_list.push_back(t_info);\n");  
            }    
        }

        p.print("  (new save_manager())->start();\n");
        p.print("  node_server *node = new node_server(thread_list, init);\n");

        p.newLine();

        //p.print("  signal(3, sig_recv);\n\n");
        p.print("  node->run(__ccp_ip);\n");

        // print profiling summary
        if (KjcOptions.countops) {
            p.print("  profiler::summarize();\n");
        }

        //p.print("  for (;;) {}\n");   
        //p.print("  init_instance::close_sockets();\n");
        //p.print("  return 0;\n");

        p.print("}\n");

        try {
            FileWriter fw = new FileWriter("master.cpp");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write master code file");
        }

    }



    public static void generateClusterHeader() {

        CodegenPrintWriter p = new CodegenPrintWriter();

        p.newLine();
        p.newLine();

        p.print("//#define __CHECKPOINT_FREQ 10000");
        p.newLine();
        p.newLine();

        try {
            FileWriter fw = new FileWriter("cluster.h");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write cluster.h");
        }   
    }

    public static void generateMakeFile(SIRHelper[] helpers) {

        int threadNumber = NodeEnumerator.getNumberOfNodes();

        CodegenPrintWriter p = new CodegenPrintWriter();
    
        p.newLine();
        p.print("LIB_CLUSTER = $(STREAMIT_HOME)/library/cluster\n");

        p.newLine();    
        p.print("CC = gcc34 #gcc34\n"); // gcc34
        p.print("CC_IA64 = ecc\n");
        p.print("CC_ARM = /u/janiss/bin/arm343 #arm-linux-gcc\n");

        p.newLine();
        p.print("CCFLAGS = -O3 #-O3\n");
        p.print("CCFLAGS_IA64 = -O3\n");
        p.print("CCFLAGS_ARM = -O3\n");

        p.newLine();
        p.print("NAMES = ");
    
        {
            int i;
            for (i = 0; i < threadNumber - 1; i++) {
                p.print("\tthread"+i+" \\");
                p.newLine();
            }
            p.print("\tthread"+i);
            p.newLine();
        }

        p.newLine();

        p.print("SOURCES = \t$(NAMES:%=%.cpp)\n");
        p.print("OBJS = \t$(NAMES:%=%.o)\n");
        p.print("OBJS_IA64 = \t$(NAMES:%=%_ia64.o)\n");
        p.print("OBJS_ARM = \t$(NAMES:%=%_arm.o)\n");

        p.newLine();

        if (KjcOptions.standalone) {
            p.print("all: fusion\n");
        } else {
            p.print("all: run_cluster\n");
        }

        p.newLine();

        if (KjcOptions.standalone) {
            p.print("ia64: fusion_ia64\n");
        } else {
            p.print("ia64: run_cluster_ia64\n");
        }

        p.newLine();
    
        p.print("arm: fusion_arm\n");
         
        p.newLine();

        p.print("clean:\n");
        p.print("\trm -f run_cluster fusion master*.o fusion*.o thread*.o\n");
        p.newLine();

        p.newLine();

        // =============== run_cluster

        p.print("run_cluster: master.o global.o ");
        for (int y = 0; y < helpers.length; y++) {
            if (helpers[y].isNative()) {
                p.print(helpers[y].getIdent()+".o ");
            }
        }
        p.print("$(OBJS)\n");
        p.print("\t$(CC) $(CCFLAGS) -o $@ $^ -L$(LIB_CLUSTER) -lpthread -lcluster -lstdc++\n");
        p.newLine();

        // =============== fusion

        p.print("fusion: fusion.o $(OBJS)\n");
        p.print("\tar r objects.a $^\n");
        p.print("\tranlib objects.a\n");
        p.print("\t$(CC) $(CCFLAGS) -o $@ objects.a -L$(LIB_CLUSTER) -lpthread -lcluster -lstdc++\n");
        p.newLine();
    
        // =============== %.o : %.cpp
    
        p.print("%.o: %.cpp fusion.h cluster.h global.h\n");
        p.print("\t$(CC) $(CCFLAGS) -I$(LIB_CLUSTER) -c -o $@ $<\n");
        p.newLine();

        p.newLine();

        // =============== run_cluster_ia64

        p.print("run_cluster_ia64: master_ia64.o $(OBJS_IA64)\n");
        p.print("\t$(CC_IA64) $(CCFLAGS_IA64) -o $@ $^ -L$(LIB_CLUSTER) -lpthread -lcluster_ia64\n");
        p.newLine();

        // =============== fusion_ia64

        p.print("fusion_ia64: fusion_ia64.o $(OBJS_IA64)\n");
        p.print("\t$(CC_IA64) $(CCFLAGS_IA64) -o $@ $^ -L$(LIB_CLUSTER) -lpthread -lcluster_ia64\n");

        p.newLine();

        // =============== %_ia64.o : %.cpp

        p.print("%_ia64.o: %.cpp fusion.h cluster.h\n");
        p.print("\t$(CC_IA64) $(CCFLAGS_IA64) -I$(LIB_CLUSTER) -c -o $@ $<\n");
        p.newLine();




        // =============== fusion_arm

        p.print("fusion_arm: fusion_arm.o $(OBJS_ARM)\n");
        p.print("\tar r objects_arm.a $^\n");
        p.print("\tranlib objects_arm.a\n");
        p.print("\t$(CC_ARM) $(CCFLAGS_ARM) -o $@ objects_arm.a -L$(LIB_CLUSTER) -lstdc++ -lm -lcluster_arm #-lpthread\n");

        p.newLine();

        // =============== %_arm.o : %.cpp

        p.print("%_arm.o: %.cpp fusion.h cluster.h\n");
        p.print("\t$(CC_ARM) $(CCFLAGS_ARM) -I$(LIB_CLUSTER) -c -o $@ $<\n");
        p.newLine();



        try {
            FileWriter fw = new FileWriter("Makefile.cluster");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write Makefile");
        }   
    }


    /**
     * partitionMap is SIROperator->Integer denoting partition #
     */
    public static void generateConfigFile() {

        int threadNumber = NodeEnumerator.getNumberOfNodes();

        CodegenPrintWriter p = new CodegenPrintWriter();

        String currentHostName = getCurrentHostName();

        /*
          String me = new String();

          try {

          Runtime run = Runtime.getRuntime();
          Process proc = run.exec("uname -n");
          proc.waitFor();
        
          InputStream in = proc.getInputStream();
        
          int len = in.available() - 1;
        
          for (int i = 0; i < len; i++) {
          me += (char)in.read();
          }
          } catch (Exception ex) {

          ex.printStackTrace();
          }
        */

        for (int i = 0; i < threadNumber; i++) {
            p.print(i+" "+currentHostName+"\n");
        }

        try {
            FileWriter fw = new FileWriter("cluster-config.txt");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write cluster configuration file");
        }   
    }

    /**
     * Returns the name of the machine this is currently running on.
     */
    private static String getCurrentHostName() {
        String result = "unknown";
        try {
            Process proc = Runtime.getRuntime().exec("hostname");
            proc.waitFor();
            BufferedReader output = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            result = output.readLine();
        } catch (IOException e) {
            System.err.println("Warning: could not determine current host name for cluster-config file (IOException).");
        } catch (InterruptedException e) {
            System.err.println("Warning: could not determine current host name for cluster-config file (InterruptedException).");
        }
        return result;
    }

    public static void generateSetupFile() {

        CodegenPrintWriter p = new CodegenPrintWriter();


        p.print("frequency_of_checkpoints 0    // Must be a multiple of 1000 or 0 for disabled.\n");
        p.print("outbound_data_buffer 1000     // Number of bytes to buffer before writing to socket. Must be <= 1400 or 0 for disabled\n");
        p.print("number_of_iterations 10000    // Number of steady state iterations can be overriden by parameter -i <number>\n");

        try {
            FileWriter fw = new FileWriter("cluster-setup.txt");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write cluster setup file");
        }   
    }
}

    

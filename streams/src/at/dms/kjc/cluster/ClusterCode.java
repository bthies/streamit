package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.cluster.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.Vector;
import java.util.List;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.kjc.raw.Util;
import at.dms.kjc.sir.lowering.*;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.io.*;
import java.lang.*;

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class ClusterCode extends at.dms.util.Utils implements FlatVisitor {
    // the max-ahead is the maximum number of lines that this will
    // recognize as a pattern for folding into a loop
    private static final int MAX_LOOKAHEAD = 20;

    //Hash set of tiles mapped to filters or joiners
    //all other tiles are routing tiles
    public static HashSet realTiles;
    public static HashSet tiles;

    public static final String ARRAY_INDEX = "__ARRAY_INDEX__";

    private static HashMap partitionMap;

    public static void setPartitionMap(HashMap pmap) {
	partitionMap = pmap;
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
	    ((SIRFilter)node.contents).setMethods(JMethodDeclaration.EMPTY());

	}

	if (node.contents instanceof SIRSplitter) {

	    generateSplitter(node);

	}

	if (node.contents instanceof SIRJoiner) {

	    generateJoiner(node);

	}
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
	
	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);
		
	ClusterCodeGenerator gen = new ClusterCodeGenerator(splitter, new JFieldDeclaration[0]);

	p.print("// init counts: "+init_counts+" steady counts: "+steady_counts+"\n"); 
	p.print("\n");

	//  +=============================+
	//  | Preamble                    |
	//  +=============================+     

	Vector pre = gen.generatePreamble();

	for (int i = 0; i < pre.size(); i++) {
	    p.print(pre.elementAt(i).toString());
	}

	//  +=============================+
	//  | Splitter Push               |
	//  +=============================+

	int sum_of_weights = splitter.getSumOfWeights();

	p.print(baseType.toString()+" "+in.push_buffer()+"["+sum_of_weights+"];\n");
	p.print("int "+in.push_index()+" = 0;\n");
	p.print("\n");

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
	p.print("\n");

	//  +=============================+
	//  | Splitter Pop                |
	//  +=============================+

	for (int ch = 0; ch < splitter.getWays(); ch++) {

	    NetStream _out = (NetStream)out.elementAt(ch);
	    int weight = splitter.getWeight(ch);

	    p.print(baseType.toString()+" "+_out.pop_buffer()+"["+weight+"];\n");
	    p.print("int "+_out.pop_index()+" = "+weight+";\n");
	    p.print("\n");

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
	    p.print("\n");
	}
	

	//  +=============================+
	//  | Splitter Work               |
	//  +=============================+

	p.print("void __splitter_"+thread_id+"_work() {\n");
	FlatNode source = NodeEnumerator.getFlatNode(in.getSource());

	if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {

	    p.print("  "+baseType.toString()+" tmp;\n");	
	    if (ClusterFusion.fusedWith(node).contains(source)) {
		p.print("  tmp = "+in.pop_name()+"();\n");
	    } else {	    
		p.print("  tmp = "+in.consumer_name()+".pop();\n");	    
	    }
	    for (int i = 0; i < out.size(); i++) {
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.producer_name()+".push(tmp);\n");
	    }
	    
	} else if (splitter.getType().equals(SIRSplitType.ROUND_ROBIN)) {
	    
	    p.print("  "+baseType.toString()+" tmp;\n");
	    for (int i = 0; i < out.size(); i++) {
		if (ClusterFusion.fusedWith(node).contains(source)) {
		    p.print("  tmp = "+in.pop_name()+"();\n");
		} else {	    
		    p.print("  tmp = "+in.consumer_name()+".pop();\n");	    
		}
		p.print("  tmp = "+in.consumer_name()+".pop();\n");
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.producer_name()+".push(tmp);\n");
	    }

	} else if (splitter.getType().equals(SIRSplitType.WEIGHTED_RR)) {

	    /*

	    for (int i = 0; i < out.size(); i++) {

		int num = splitter.getWeight(i);
		NetStream s = (NetStream)out.elementAt(i);		
		
		for (int ii = 0; ii < num; ii++) {

		    p.print("  tmp = "+in.consumer_name()+".read_"+baseType.toString()+"();\n");
		    p.print("  "+s.producer_name()+".write_"+baseType.toString()+"(tmp);\n");
		}
	    }

	    */
	    
	    int sum = splitter.getSumOfWeights();
	    int offs = 0;
		
	    p.print("  "+baseType.toString()+" tmp["+sum+"];\n");

	    if (ClusterFusion.fusedWith(node).contains(source)) {
		for (int y = 0; y < sum; y++) {
		    p.print("  tmp["+y+"] = "+in.pop_name()+"();\n");
		}
	    } else {	    
		p.print("  "+in.consumer_name()+".pop_items(tmp, "+sum+");\n");
	    }

	    for (int i = 0; i < out.size(); i++) {
		int num = splitter.getWeight(i);
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.producer_name()+".push_items(&tmp["+offs+"], "+num+");\n");
		offs += num;
	    }
	}

	p.print("}\n");
	p.print("\n");


	//  +=============================+
	//  | Splitter Work 1k            |
	//  +=============================+

	p.print("void __splitter_"+thread_id+"_work_1k() {\n");

	if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {
	    
	    p.print("  "+baseType.toString()+" tmp[1000 * "+steady_counts+"];\n");
	    p.print("  "+in.consumer_name()+".pop_items(tmp, 1000 * "+steady_counts+");\n");		
	    for (int i = 0; i < out.size(); i++) {
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.producer_name()+".push_items(tmp, 1000 * "+steady_counts+");\n");
	    }

	} else {	
	    p.print("  for (int i = 0; i < 1000 * "+steady_counts+"; i++) __splitter_"+thread_id+"_work();\n");
	}	

	p.print("}\n");
	p.print("\n");


	//  +=============================+
	//  | Splitter Main               |
	//  +=============================+
	
	p.print("void __splitter_"+thread_id+"_main() {\n");
	p.print("  int i, ii;\n");
	
	p.print("  if (__steady_"+thread_id+" == 0) {\n");
	p.print("    for (i = 0; i < "+init_counts+"; i++) {\n");
	p.print("      check_thread_status(__state_flag_"+thread_id+",__thread_"+thread_id+");\n");
	p.print("      __splitter_"+thread_id+"_work();\n");
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
	p.print("      __splitter_"+thread_id+"_work();\n");
	p.print("    }\n");

	p.print("    if (__frequency_of_chkpts != 0 && __steady_"+thread_id+" % __frequency_of_chkpts == 0) save_state::save_to_file(__thread_"+thread_id+", __steady_"+thread_id+", __write_thread__"+thread_id+");\n");

	p.print("  }\n");

	p.print("}\n");

	//  +=============================+
	//  | Run Function                |
	//  +=============================+

	Vector run = gen.generateRunFunction(null, "__splitter_"+thread_id+"_main");

	for (int i = 0; i < run.size(); i++) {
	    p.print(run.elementAt(i).toString());
	}

	//  +=============================+
	//  | Write Splitter to File      |
	//  +=============================+
	
	try {
	    FileWriter fw = new FileWriter("thread"+thread_id+".cpp");
	    fw.write(str.toString());
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

	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);
		
	ClusterCodeGenerator gen = new ClusterCodeGenerator(joiner, new JFieldDeclaration[0]);

	p.print("// init counts: "+init_counts+" steady counts: "+steady_counts+"\n"); 
	p.print("\n");

	//  +=============================+
	//  | Preamble                    |
	//  +=============================+     

	Vector pre = gen.generatePreamble();

	for (int i = 0; i < pre.size(); i++) {
	    p.print(pre.elementAt(i).toString());
	}

	//  +=============================+
	//  | Joiner Pop                  |
	//  +=============================+


	int sum_of_weights = joiner.getSumOfWeights();

	p.print(baseType.toString()+" "+out.pop_buffer()+"["+sum_of_weights+"];\n");
	p.print("int "+out.pop_index()+" = "+sum_of_weights+";\n");
	p.print("\n");

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
	p.print("\n");

	//  +=============================+
	//  | Joiner Work                 |
	//  +=============================+

	p.print("void __joiner_"+thread_id+"_work() {\n");

	if (joiner.getType().equals(SIRJoinType.ROUND_ROBIN)) {

	    p.print("  "+baseType.toString()+" tmp;\n");

	    for (int i = 0; i < in.size(); i++) {
		NetStream s = (NetStream)in.elementAt(i);		
		p.print("  tmp = "+s.consumer_name()+".pop();\n");
		
		p.print("  "+out.producer_name()+".push(tmp);\n");
		
	    }

	} else if (joiner.getType().equals(SIRJoinType.WEIGHTED_RR)) {

	    if (joiner.getParent() instanceof SIRFeedbackLoop) {

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

		int sum = joiner.getSumOfWeights();
		int offs = 0;
		
		p.print("  "+baseType.toString()+" tmp["+sum+"];\n");
		
		for (int i = 0; i < in.size(); i++) {
		    
		    NetStream s = (NetStream)in.elementAt(i);		
		    int num = joiner.getWeight(i);
		    
		    p.print("  "+s.consumer_name()+".pop_items(&tmp["+offs+"], "+num+");\n");
		    
		    offs += num;
		    
		}
		
		p.print("  "+out.producer_name()+".push_items(tmp, "+sum+");\n");
	    
	    }
	}

	p.print("}\n");

	p.print("\n");

	//  +=============================+
	//  | Joiner Main                 |
	//  +=============================+

	p.print("void __joiner_"+thread_id+"_main() {\n");
	p.print("  int i, ii;\n");
	
	p.print("  if (__steady_"+thread_id+" == 0) {\n");
	p.print("    for (i = 0; i < "+init_counts+"; i++) {\n");
	p.print("      check_thread_status(__state_flag_"+thread_id+",__thread_"+thread_id+");\n");
	p.print("      __joiner_"+thread_id+"_work();\n");
	p.print("    }\n");
	p.print("  }\n");
	p.print("  __steady_"+thread_id+"++;\n");

	p.print("  for (i = 1; i <= __number_of_iterations_"+thread_id+"; i++, __steady_"+thread_id+"++) {\n");	
	p.print("    for (ii = 0; ii < "+steady_counts+"; ii++) {\n");
	p.print("      check_thread_status(__state_flag_"+thread_id+",__thread_"+thread_id+");\n");
	p.print("      __joiner_"+thread_id+"_work();\n");
	p.print("    }\n");

	p.print("    if (__frequency_of_chkpts != 0 && i % __frequency_of_chkpts == 0) save_state::save_to_file(__thread_"+thread_id+", __steady_"+thread_id+", __write_thread__"+thread_id+");\n");

	p.print("  }\n");

	p.print("}\n");
	
	//  +=============================+
	//  | Run Function                |
	//  +=============================+

	Vector run = gen.generateRunFunction(null, "__joiner_"+thread_id+"_main");

	for (int i = 0; i < run.size(); i++) {
	    p.print(run.elementAt(i).toString());
	}

	//  +=============================+
	//  | Write Joiner to File        |
	//  +=============================+

	try {
	    FileWriter fw = new FileWriter("thread"+thread_id+".cpp");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write joiner code to file thread"+thread_id+".cpp");
	}

	System.out.println("Code for " + node.contents.getName() +
			   " written to thread"+thread_id+".cpp");


    }


    public static void generateMasterFile() {

	int threadNumber = NodeEnumerator.getNumberOfNodes();

	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);
	
	p.print("#include <pthread.h>\n");
	p.print("#include <unistd.h>\n");
	p.print("#include <signal.h>\n");
	p.print("#include <string.h>\n");
	p.print("#include <stdlib.h>\n");
	p.print("#include <stdio.h>\n");
	p.println();
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
	p.println();

	p.print("int __max_iteration;\n");
	p.print("int __timer_enabled = 0;\n");
	p.print("int __frequency_of_chkpts;\n");
	p.print("int __out_data_buffer;\n");

	p.print("vector <thread_info*> thread_list;\n");
	p.print("netsocket *server = NULL;\n");
	p.print("unsigned __ccp_ip = 0;\n");
	p.print("int __init_iter = 0;\n");
	p.println();

	p.print("unsigned myip;\n");
	p.println();

	for (int i = 0; i < threadNumber; i++) {

	    FlatNode tmp = NodeEnumerator.getFlatNode(i);
	    if (!ClusterFusion.isEliminated(tmp)) {	    
		p.print("extern void __declare_sockets_"+i+"();\n");
		p.print("extern thread_info *__get_thread_info_"+i+"();\n");
		p.print("extern void run_"+i+"();\n");
		p.print("pthread_t __pthread_"+i+";\n");
		p.print("static void *run_thread_"+i+"(void *param) {\n");
		p.print("  run_"+i+"();\n");
		p.print("}\n");
	    }

	}

	p.println();

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

	p.println();

	p.print("int master_pid;\n");

	p.println();

	/*

	p.print("void sig_recv(int sig_nr) {\n");
	p.print("  if (master_pid == getpid()) {\n");
	p.print("    printf(\"\n data sent     : %d\\n\", mysocket::get_total_data_sent());\n");
        p.print("    printf(\" data received : %d\\n\", mysocket::get_total_data_received());\n");
	p.print("  }\n");
	p.print("}\n");

	p.println();

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
		p.print("  if (myip == init_instance::get_thread_ip("+i+")) {\n");
		p.print("    thread_info *info = __get_thread_info_"+i+"();\n"); 
		p.print("    int *state = info->get_state_flag();\n");
		p.print("    *state = RUN_STATE;\n");
		p.print("    pthread_create(&__pthread_"+i+", NULL, run_thread_"+i+", (void*)\"thread"+i+"\");\n");
		p.print("    info->set_pthread(__pthread_"+i+");\n");
		p.print("    info->set_active(true);\n");
		p.print("  }\n");
	    }
	}

	p.print("  pthread_t id;\n");
	p.print("  pthread_create(&id, NULL, run_join, NULL);\n");

	p.print("}\n");

	p.println();

	p.print("int main(int argc, char **argv) {\n");

	p.print("  myip = get_myip();\n");

	p.print("  read_setup::read_setup_file();\n");
	p.print("  __frequency_of_chkpts = read_setup::freq_of_chkpts;\n");
	p.print("  __out_data_buffer = read_setup::out_data_buffer;\n");
	p.print("  __max_iteration = read_setup::max_iteration;\n");

	p.print("  master_pid = getpid();\n");

	p.print("  for (int a = 1; a < argc; a++) {\n");

	p.print("    if (argc > a + 1 && strcmp(argv[a], \"-init\") == 0) {\n"); 
	p.print("       int tmp;\n");
	p.print("       sscanf(argv[a + 1], \"%d\", &tmp);\n");
	p.print("       printf(\"Initial Iteration: %d\\n\", tmp);\n"); 
	p.print("       __init_iter = tmp;"); 
	p.print("    }\n");

	p.print("    if (argc > a + 1 && strcmp(argv[a], \"-i\") == 0) {\n"); 
	p.print("       int tmp;\n");
	p.print("       sscanf(argv[a + 1], \"%d\", &tmp);\n");
	p.print("       printf(\"Number of Iterations: %d\\n\", tmp);\n"); 
	p.print("       __max_iteration = tmp;"); 
	p.print("    }\n");

	p.print("    if (strcmp(argv[a], \"-t\") == 0) {\n"); 
	p.print("       printf(\"Timer enabled.\\n\");\n"); 
	p.print("       __timer_enabled = 1;"); 
	p.print("    }\n");

	p.print("    if (argc > a + 1 && strcmp(argv[a], \"-ccp\") == 0) {\n");
	p.print("       printf(\"CCP address: %s\\n\", argv[a + 1]);\n"); 
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
	p.print("        printf(\"master> \");fflush(stdout);\n");
	p.print("        line[0] = 0;\n");
	p.print("        scanf(\"%[^\\n]\", line);scanf(\"%c\", &tmp);\n");
	p.print("        m->process_command(line);\n");
	p.print("      }\n");
	p.print("    }\n");

	p.print("  }\n");

	p.print("\n");

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

	p.print("\n");

	//p.print("  signal(3, sig_recv);\n\n");
	p.print("  node->run(__ccp_ip);\n");

	//p.print("  for (;;) {}\n");	
	//p.print("  init_instance::close_sockets();\n");
	//p.print("  return 0;\n");

	p.print("}\n");

	try {
	    FileWriter fw = new FileWriter("master.cpp");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write master code file");
	}

    }


    public static void generateMakeFile() {

	int threadNumber = NodeEnumerator.getNumberOfNodes();

	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);
	
	p.println();
	p.print("\nall: run_cluster");
	p.println();
	p.println();

	p.print("run_cluster: master.o");

	for (int i = 0; i < threadNumber; i++) {
	    p.print(" thread"+i+".o");
	}

	p.println();

	p.print("\tgcc -O2 -o run_cluster master.o");
	
	for (int i = 0; i < threadNumber; i++) {
	    p.print(" thread"+i+".o");
	}

	p.print(" -L$(STREAMIT_HOME)/library/cluster -lpthread -lstdc++ -lcluster");

	p.println();
	p.println();

	p.print("master.o: master.cpp");
	p.println();
	p.print("\tgcc -O2 -I$(STREAMIT_HOME)/library/cluster -c master.cpp");
	p.println();
	p.println();


	for (int i = 0; i < threadNumber; i++) {

	    p.print("thread"+i+".o: thread"+i+".cpp");
	    p.println();
	    p.print("\tgcc -O2 -I$(STREAMIT_HOME)/library/cluster -c thread"+i+".cpp");
	    p.println();
	    p.println();

	}

	
	try {
	    FileWriter fw = new FileWriter("Makefile.cluster");
	    fw.write(str.toString());
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

	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);

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
	    p.print(i+" "+"machine-"+ClusterFusion.getPartition(NodeEnumerator.getFlatNode(i))+"\n");
	}

	try {
	    FileWriter fw = new FileWriter("cluster-config.txt");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write cluster configuration file");
	}	
    }


    public static void generateSetupFile() {

	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);

	p.print("frequency_of_checkpoints 0    // Must be a multiple of 1000 or 0 for disabled.\n");
	p.print("outbound_data_buffer 1000     // Number of bytes to buffer before writing to socket. Must be <= 1400 or 0 for disabled\n");
	p.print("number_of_iterations 10000    // Number of steady state iterations can be overriden by parameter -i <number>\n");

	try {
	    FileWriter fw = new FileWriter("cluster-setup.txt");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write cluster setup file");
	}	
    }
}

    

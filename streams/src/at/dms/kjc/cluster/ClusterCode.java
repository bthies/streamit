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
	//  | Splitter Work               |
	//  +=============================+

	p.print("void __splitter_"+thread_id+"_work() {\n");

	p.print("  "+baseType.toString()+" tmp;\n");

	if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {
	    
	    p.print("  tmp = "+in.consumer_name()+".read_"+baseType.toString()+"();\n");
	    
	    for (int i = 0; i < out.size(); i++) {
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.producer_name()+".write_"+baseType.toString()+"(tmp);\n");
	    }

	} else if (splitter.getType().equals(SIRSplitType.ROUND_ROBIN)) {
	    	    
	    for (int i = 0; i < out.size(); i++) {

		p.print("  tmp = "+in.consumer_name()+".read_"+baseType.toString()+"();\n");
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.producer_name()+".write_"+baseType.toString()+"(tmp);\n");
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

	    p.print("  "+in.consumer_name()+".read_chunk(tmp, "+sum+" * sizeof("+baseType.toString()+"), "+sum+");\n");
		
	    for (int i = 0; i < out.size(); i++) {
		    
		int num = splitter.getWeight(i);
		NetStream s = (NetStream)out.elementAt(i);		
		
		p.print("  "+s.producer_name()+".write_chunk(&tmp["+offs+"], "+num+" * sizeof("+baseType.toString()+"), "+num+");\n");
		    
		offs += num;
		    
	    }
		
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

	p.print("  for (i = 1; i <= __number_of_iterations_"+thread_id+"; i++, __steady_"+thread_id+"++) {\n");	
	p.print("    for (ii = 0; ii < "+steady_counts+"; ii++) {\n");
	p.print("      check_thread_status(__state_flag_"+thread_id+",__thread_"+thread_id+");\n");
	p.print("      __splitter_"+thread_id+"_work();\n");
	p.print("    }\n");

	p.print("    if (__frequency_of_chkpts != 0 && i % __frequency_of_chkpts == 0) save_state::save_to_file(__thread_"+thread_id+", __steady_"+thread_id+", __write_thread__"+thread_id+");\n");

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
	//  | Joiner Work                 |
	//  +=============================+

	p.print("void __joiner_"+thread_id+"_work() {\n");

	if (joiner.getType().equals(SIRJoinType.ROUND_ROBIN)) {

	    p.print("  "+baseType.toString()+" tmp;\n");

	    for (int i = 0; i < in.size(); i++) {
		NetStream s = (NetStream)in.elementAt(i);		
		p.print("  tmp = "+s.consumer_name()+".read_"+baseType.toString()+"();\n");
		
		p.print("  "+out.producer_name()+".write_"+baseType.toString()+"(tmp);\n");
		
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
			    p.print("    tmp = "+s.consumer_name()+".read_"+baseType.toString()+"();\n");
			    
			} else {
			    
			    p.print("  tmp = "+s.consumer_name()+".read_"+baseType.toString()+"();\n");
			}
			
			p.print("  "+out.producer_name()+".write_"+baseType.toString()+"(tmp);\n");
		    }
		    
		}

	    } else {

		int sum = joiner.getSumOfWeights();
		int offs = 0;
		
		p.print("  "+baseType.toString()+" tmp["+sum+"];\n");
		
		for (int i = 0; i < in.size(); i++) {
		    
		    NetStream s = (NetStream)in.elementAt(i);		
		    int num = joiner.getWeight(i);
		    
		    p.print("  "+s.consumer_name()+".read_chunk(&tmp["+offs+"], "+num+" * sizeof("+baseType.toString()+"), "+num+");\n");
		    
		    offs += num;
		    
		}
		
		p.print("  "+out.producer_name()+".write_chunk(tmp, "+sum+" * sizeof("+baseType.toString()+"), "+sum+");\n");
	    
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
	p.print("int __frequency_of_chkpts;\n");
	p.print("int __out_data_buffer;\n");

	p.print("vector <thread_info*> thread_list;\n");
	p.print("netsocket *server = NULL;\n");
	p.print("unsigned __ccp_ip = 0;\n");
	p.print("int __init_iter = 0;\n");
	p.println();

	for (int i = 0; i < threadNumber; i++) {
	    
	    p.print("extern void __declare_sockets_"+i+"();\n");
	    p.print("extern thread_info *__get_thread_info_"+i+"();\n");
	    p.print("extern void run_"+i+"();\n");
	    p.print("static void *run_thread_"+i+"(void *param) {\n");
	    p.print("  run_"+i+"();\n");
	    p.print("}\n");

	}

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
	    p.print("  if (get_myip() == init_instance::get_thread_ip("+i+")) {\n");
	    p.print("    __declare_sockets_"+i+"();\n");
	    p.print("  }\n");
	}

	p.print("  init_instance::initialize_sockets();\n");

	p.print("  pthread_t id;\n");

	for (int i = 0; i < threadNumber; i++) {

	    p.print("  if (get_myip() == init_instance::get_thread_ip("+i+")) {\n");

	    p.print("    thread_info *info = __get_thread_info_"+i+"();\n"); 
	    p.print("    int *state = info->get_state_flag();\n");
	    p.print("    *state = RUN_STATE;\n");
	    p.print("    pthread_create(&id, NULL, run_thread_"+i+", (void*)\"thread"+i+"\");\n");
	    p.print("    info->set_pthread(id);\n");
	    p.print("    info->set_active(true);\n");

	    p.print("  }\n");
	}
	p.print("}\n");


	p.println();

	p.print("int main(int argc, char **argv) {\n");

	p.print("  read_setup::read_setup_file();\n");
	p.print("  __frequency_of_chkpts = read_setup::freq_of_chkpts;\n");
	p.print("  __out_data_buffer = read_setup::out_data_buffer;\n");
	p.print("  __max_iteration = read_setup::max_iteration;\n");

	p.print("  master_pid = getpid();\n");

	p.print("  for (int a = 1; a < argc; a++) {");

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
	    p.print("  t_info = __get_thread_info_"+i+"();\n"); 
	    p.print("  thread_list.push_back(t_info);\n");	    
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
    public static void generateConfigFile(HashMap partitionMap) {

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
	    p.print(i+" "+"machine-"+getPartition(NodeEnumerator.getFlatNode(i), partitionMap)+"\n");
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

	p.print("frequency_of_checkpoints 1000 // must be a multiple of 1000 or 0 for disabled.\n");
	p.print("outbound_data_buffer 400      // <= 1400 or 0 for disabled.\n");
	p.print("number_of_iterations 10000    // number of steady state iterations\n");

	try {
	    FileWriter fw = new FileWriter("cluster-setup.txt");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write cluster setup file");
	}	
    }

    /**
     * Returns partition that <thread> should execute on.
     */
    private static String getPartition(FlatNode node, HashMap partitionMap) {
	SIROperator op = node.contents;
	Integer partition = (Integer)partitionMap.get(op);

	if (partition!=null) {
	    // if we assigned it to a partition, then return it.  Add
	    // 1 for sake of clusters that start with machine "1"
	    // instead of "0".
	    int val = (((Integer)partition).intValue()+1);
	    return ""+val;
	} else if (op instanceof SIRSplitter) {
	    // note that splitters/joiners collapsed in a fused node
	    // should have been assigned a partition by the
	    // partitioner; the ones remaining are "border cases".
	    if (node.incoming[0]==null) {
		// if we hit the top (a null splitter), assign to partition 0
		return "1";
	    } else {
		// integrate forwards to partition that is communicating
		// most with this one.
		SIRSplitter split = (SIRSplitter)op;
		HashMap map = new HashMap(); // String partition->Integer sum
		int[] weights = split.getWeights();
		for (int i=0; i<weights.length; i++) {
		    String part = getPartition(node.edges[i], partitionMap);
		    Integer _oldSum = (Integer)map.get(part);
		    int oldSum = 0;
		    if (_oldSum!=null) {
			oldSum = _oldSum.intValue();
		    }
		    map.put(part.intern(), new Integer(oldSum+weights[i]));
		}
		
		int max = -1;
		String result = null;
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
		    String part = (String)it.next();
		    int sum = ((Integer)map.get(part)).intValue();
		    if (sum>max) {
			max = sum;
			result = part;
		    }
		}
		assert result!=null;
		return result;
	    }
	} else if (op instanceof SIRJoiner) {
	    // integrate backwards to partition that is communicating
	    // most with this one.
	    SIRJoiner join = (SIRJoiner)op;
	    HashMap map = new HashMap(); // String partition->Integer sum
	    int[] weights = join.getWeights();
	    for (int i=0; i<weights.length; i++) {
		String part = getPartition(node.incoming[i], partitionMap);
		Integer _oldSum = (Integer)map.get(part);
		int oldSum = 0;
		if (_oldSum!=null) {
		    oldSum = _oldSum.intValue();
		}
		map.put(part.intern(), new Integer(oldSum+weights[i]));
	    }
	    
	    int max = -1;
	    String result = null;
	    Iterator it = map.keySet().iterator();
	    while (it.hasNext()) {
		String part = (String)it.next();
		int sum = ((Integer)map.get(part)).intValue();
		if (sum>max) {
		    max = sum;
		    result = part;
		}
	    }
	    assert result!=null;
	    return result;
	} else if (op instanceof SIRIdentity) {
	    // if we find identity that wasn't assigned, integrate it
	    // into its destination (arbitrarily -- could just as well
	    // be the source)
	    return getPartition(node.edges[0], partitionMap);
	} else {
	    Utils.fail("No partition was assigned to " + op + " of type " + op.getClass());
	    return null;
	}
    }
}

    

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

	CType baseType = Util.getBaseType(Util.getOutputType(node));
	
	int thread_id = NodeEnumerator.getSIROperatorId(node.contents);
	
	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);
	
	p.print("#include <stdlib.h>\n");
	p.print("#include <math.h>\n");	
	p.print("#include <init_instance.h>\n");
	p.print("#include <mysocket.h>\n");
	p.print("#include <peek_stream.h>\n");

	p.print("\n");

	p.print("extern int __number_of_iterations;\n");
	
	//Visit fields declared in the filter class
	//JFieldDeclaration[] fields = self.getFields();
	//for (int i = 0; i < fields.length; i++)
	//   fields[i].accept(this);

	p.print("\n");

	//declare input/output socket variables
	NetStream in = (NetStream)RegisterStreams.getNodeInStreams(node.contents);
	Vector out = (Vector)RegisterStreams.getNodeOutStreams(node.contents);

	p.print("mysocket *"+in.name()+"in;\n");

	for (int i = 0; i < out.size(); i++) {
	    p.print("mysocket *"+((NetStream)out.elementAt(i)).name()+"out;\n");
	}
	
	p.print("\n");
	p.print("void __declare_sockets_"+thread_id+"() {\n");

	p.print("  init_instance::add_incoming("+in.getSource()+","+in.getDest()+",DATA_SOCKET);\n");

	for (int i = 0; i < out.size(); i++) {
	    NetStream s = (NetStream)out.elementAt(i);
		
	    p.print("  init_instance::add_outgoing("+s.getSource()+","+s.getDest()+",DATA_SOCKET);\n");
	    
	}
	
	p.print("}\n");
	
	p.print("\n");

	p.print("void __splitter_"+thread_id+"_work() {\n");

	p.print("  "+baseType.toString()+" tmp;\n");

	if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {
	    
	    p.print("  tmp = "+in.name()+"in->read_"+baseType.toString()+"();\n");
	    
	    for (int i = 0; i < out.size(); i++) {
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.name()+"out->write_"+baseType.toString()+"(tmp);\n");
	    }

	} else if (splitter.getType().equals(SIRSplitType.ROUND_ROBIN)) {
	    	    
	    for (int i = 0; i < out.size(); i++) {

		p.print("  tmp = "+in.name()+"in->read_"+baseType.toString()+"();\n");
		NetStream s = (NetStream)out.elementAt(i);		
		p.print("  "+s.name()+"out->write_"+baseType.toString()+"(tmp);\n");
	    }

	} else if (splitter.getType().equals(SIRSplitType.WEIGHTED_RR)) {

	    for (int i = 0; i < out.size(); i++) {

		int num = splitter.getWeight(i);
		NetStream s = (NetStream)out.elementAt(i);		
		
		for (int ii = 0; ii < num; ii++) {

		    p.print("  tmp = "+in.name()+"in->read_"+baseType.toString()+"();\n");
		    p.print("  "+s.name()+"out->write_"+baseType.toString()+"(tmp);\n");
		}
	    }
	}

	p.print("}\n");
	
	p.print("\n");
	
	p.print("void run_"+thread_id+"() {\n");
	p.print("  int i;\n");
	
	p.print("  "+in.name()+"in = new mysocket(init_instance::get_incoming_socket("+in.getSource()+","+in.getDest()+",DATA_SOCKET));\n");
	
	for (int i = 0; i < out.size(); i++) {
	    NetStream s = (NetStream)out.elementAt(i);
	    
	    p.print("  "+s.name()+"out = new mysocket(init_instance::get_outgoing_socket("+s.getSource()+","+s.getDest()+",DATA_SOCKET));\n");
	}

	// get int init count
	Integer initCounts = (Integer)ClusterBackend.initExecutionCounts.get(node);
	int init;
	if (initCounts==null) {
	    init = 0;
	} else {
	    init = initCounts.intValue();
	}
	p.print("  {int __split_iter = " + init + " + " + ClusterBackend.steadyExecutionCounts.get(node) + " * __number_of_iterations;\n");
	p.print("  for (i = 0; i < __split_iter; i++) {\n");
	p.print("    __splitter_"+thread_id+"_work();\n");
	p.print("  }}\n");
	
	p.print("}\n");
	
	System.out.println("Code for " + node.contents.getName() +
			   " written to thread"+thread_id+".cpp");
	
	try {
	    FileWriter fw = new FileWriter("thread"+thread_id+".cpp");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write splitter code to file thread"+thread_id+".cpp");
	}
    }


    public static void generateJoiner(FlatNode node) {
    	
	SIRJoiner joiner = (SIRJoiner)node.contents;

	CType baseType = Util.getBaseType(Util.getJoinerType(node));

	int thread_id = NodeEnumerator.getSIROperatorId(node.contents);

	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);

	p.print("#include <stdlib.h>\n");
	p.print("#include <math.h>\n");	
	p.print("#include <init_instance.h>\n");
	p.print("#include <mysocket.h>\n");
	p.print("#include <peek_stream.h>\n");

	p.print("\n");

	p.print("extern int __number_of_iterations;\n");

	//Visit fields declared in the filter class
	//JFieldDeclaration[] fields = self.getFields();
	//for (int i = 0; i < fields.length; i++)
	//   fields[i].accept(this);

	p.print("\n");

	//declare input/output socket variables
	Vector in = (Vector)RegisterStreams.getNodeInStreams(node.contents);
	NetStream out = (NetStream)RegisterStreams.getNodeOutStreams(node.contents);

	for (int i = 0; i < in.size(); i++) {
	    p.print("mysocket *"+((NetStream)in.elementAt(i)).name()+"in;\n");
	}

	p.print("mysocket *"+out.name()+"out;\n");

	
	p.print("\n");
	p.print("void __declare_sockets_"+thread_id+"() {\n");

	for (int i = 0; i < in.size(); i++) {
	    NetStream s = (NetStream)in.elementAt(i);	    
	    p.print("  init_instance::add_incoming("+s.getSource()+","+s.getDest()+",DATA_SOCKET);\n");    
	}

	p.print("  init_instance::add_outgoing("+out.getSource()+","+out.getDest()+",DATA_SOCKET);\n");
	
	p.print("}\n");
	
	p.print("\n");



	if (joiner.getParent() instanceof SIRFeedbackLoop) {

	    p.print("//Feedback Loop Joiner\n");
	    
	    p.print("\nint __init_counter_"+thread_id+" = 0;\n");

	    JMethodDeclaration initPath = ((SIRFeedbackLoop)joiner.getParent()).getInitPath();

	    initPath.setName("__Init_Path_"+thread_id);

	    FlatIRToCluster toC = new FlatIRToCluster();
	    toC.declOnly = false;
	    initPath.accept(toC);
	    p.print(toC.getString());

	    p.print("\n");
	    
	    //fw.write(createInitPath(joiner) + "\n");	    
	}
	

	p.print("void __joiner_"+thread_id+"_work() {\n");

	p.print("  "+baseType.toString()+" tmp;\n");


	if (joiner.getType().equals(SIRJoinType.ROUND_ROBIN)) {

	    for (int i = 0; i < in.size(); i++) {
		NetStream s = (NetStream)in.elementAt(i);		
		p.print("  tmp = "+s.name()+"in->read_"+baseType.toString()+"();\n");
		
		p.print("  "+out.name()+"out->write_"+baseType.toString()+"(tmp);\n");
		
	    }

	} else if (joiner.getType().equals(SIRJoinType.WEIGHTED_RR)) {

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
			p.print("    tmp = "+s.name()+"in->read_"+baseType.toString()+"();\n");
			
		    } else {

			p.print("  tmp = "+s.name()+"in->read_"+baseType.toString()+"();\n");
		    }

		    p.print("  "+out.name()+"out->write_"+baseType.toString()+"(tmp);\n");
		}

	    }

	}

	p.print("}\n");

	p.print("\n");

	p.print("void run_"+thread_id+"() {\n");
	p.print("  int i;\n");
	
	for (int i = 0; i < in.size(); i++) {
	    NetStream s = (NetStream)in.elementAt(i);
		
	    p.print("  "+s.name()+"in = new mysocket(init_instance::get_incoming_socket("+s.getSource()+","+s.getDest()+",DATA_SOCKET));\n");
	}

	p.print("  "+out.name()+"out = new mysocket(init_instance::get_outgoing_socket("+out.getSource()+","+out.getDest()+",DATA_SOCKET));\n");


	// get int init count
	Integer initCounts = (Integer)ClusterBackend.initExecutionCounts.get(node);
	int init;
	if (initCounts==null) {
	    init = 0;
	} else {
	    init = initCounts.intValue();
	}
	p.print("  {int __join_iter = " + init + " + " + ClusterBackend.steadyExecutionCounts.get(node) + " * __number_of_iterations;\n");
	p.print("  for (i = 0; i < __join_iter; i++) {\n");
	p.print("    __joiner_"+thread_id+"_work();\n");
	p.print("  }}\n");

	p.print("}\n");
	
	System.out.println("Code for " + node.contents.getName() +
			   " written to thread"+thread_id+".cpp");

	try {
	    FileWriter fw = new FileWriter("thread"+thread_id+".cpp");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write joiner code to file thread"+thread_id+".cpp");
	}
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
	p.print("#include <stdio.h>\n");
	p.println();
	p.print("#include <mysocket.h>\n");
	p.print("#include <open_socket.h>\n");
	p.print("#include <init_instance.h>\n");
	p.print("#include <thread_list_element.h>\n");
	p.println();

	p.print("int __number_of_iterations = 20;\n");
	p.print("thread_list_element *thread_list_top = NULL;\n");
	p.println();

	for (int i = 0; i < threadNumber; i++) {
	    
	    p.print("extern void __declare_sockets_"+i+"();\n");
	    p.print("extern void run_"+i+"();\n");
	    p.print("static void *run_thread_"+i+"(void *param) {\n");
	    p.print("  run_"+i+"();\n");
	    p.print("}\n");

	}


	p.println();

	p.print("int master_pid;\n");

	p.println();

	p.print("void sig_recv(int sig_nr) {\n");
	p.print("  if (master_pid == getpid()) {\n");
	p.print("    printf(\"\n data sent     : %d\\n\", mysocket::get_total_data_sent());\n");
        p.print("    printf(\" data received : %d\\n\", mysocket::get_total_data_received());\n");
	p.print("  }\n");
	p.print("}\n");

	p.println();

	p.print("void listern_for_server() {\n");
	p.print("  mysocket *sock;\n");
	p.print("  sock = open_socket::listen(22223);\n");
        p.print("  if (sock == NULL) return;\n");

	p.print("}\n");
	p.println();

	p.print("void handle_server_command(char *command) {\n");
	p.print("}\n");
	p.println();

	p.print("int main(int argc, char **argv) {\n");

	p.print("  master_pid = getpid();\n");

	p.print("  if (argc > 2 && strcmp(argv[1], \"-i\") == 0) {\n"); 
	p.print("     int tmp;\n");
	p.print("     sscanf(argv[2], \"%d\", &tmp);\n");
	p.print("     printf(\"Argument is: %d\\n\", tmp);\n"); 
	p.print("     __number_of_iterations = tmp;"); 
	p.print("  }\n");

	p.print("  pthread_t id;\n");

	p.print("  init_instance::read_config_file();\n");

	for (int i = 0; i < threadNumber; i++) {
	    p.print("  if (get_myip() == lookup_ip(init_instance::get_node_name("+i+"))) {\n");
	    p.print("    __declare_sockets_"+i+"();\n");
	    p.print("  }\n");
	}


	p.print("  init_instance::initialize_sockets();\n");

	for (int i = 0; i < threadNumber; i++) {

	    p.print("  if (get_myip() == lookup_ip(init_instance::get_node_name("+i+"))) {\n");

	    p.print("    pthread_create(&id, NULL, run_thread_"+i+", (void*)\"thread"+i+"\");\n");

	    p.print("    thread_list_top = new thread_list_element("+i+", id, thread_list_top);\n");
	
	    p.print("  }\n");
	}

	p.print("\n  signal(3, sig_recv);\n\n");

	p.print("  for (;;) {}\n");
	
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

    

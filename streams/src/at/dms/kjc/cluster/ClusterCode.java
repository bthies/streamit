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
	p.print("#include <math.h>\n\n");	
	p.print("#include <mysocket.h>\n");
	p.print("#include <init_instance.h>\n");

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

	p.print("  init_instance::add_incoming("+in.getSource()+","+in.getDest()+");\n");

	for (int i = 0; i < out.size(); i++) {
	    NetStream s = (NetStream)out.elementAt(i);
		
	    p.print("  init_instance::add_outgoing("+s.getSource()+","+s.getDest()+",lookup_ip(init_instance::get_node_name("+s.getDest()+")));\n");
	    
	}
	
	p.print("}\n");
	
	p.print("\n");

	p.print("void __splitter_"+thread_id+"_work() {\n");

	p.print("  "+baseType.toString()+" tmp;\n");

	p.print("  tmp = "+in.name()+"in->read_"+baseType.toString()+"();\n");
	
	for (int i = 0; i < out.size(); i++) {
	    NetStream s = (NetStream)out.elementAt(i);		
	    p.print("  "+s.name()+"out->write_"+baseType.toString()+"(tmp);\n");
	}
	
	p.print("}\n");
	
	p.print("\n");
	
	p.print("void run_"+thread_id+"() {\n");
	p.print("  int i;\n");
	
	p.print("  "+in.name()+"in = new mysocket(init_instance::get_incoming_socket("+in.getSource()+","+in.getDest()+"));\n");
	
	for (int i = 0; i < out.size(); i++) {
	    NetStream s = (NetStream)out.elementAt(i);
	    
	    p.print("  "+s.name()+"out = new mysocket(init_instance::get_outgoing_socket("+s.getSource()+","+s.getDest()+"));\n");
	}
	
	p.print("  for (i = 0; i < __number_of_iterations; i++) {\n");
	p.print("    __splitter_"+thread_id+"_work();\n");
	p.print("  }\n");
	
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
	p.print("#include <math.h>\n\n");	
	p.print("#include <mysocket.h>\n");
	p.print("#include <init_instance.h>\n");

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
	    p.print("  init_instance::add_incoming("+s.getSource()+","+s.getDest()+");\n");    
	}

	p.print("  init_instance::add_outgoing("+out.getSource()+","+out.getDest()+",lookup_ip(init_instance::get_node_name("+out.getDest()+")));\n");
	
	p.print("}\n");
	
	p.print("\n");

	p.print("void __joiner_"+thread_id+"_work() {\n");

	p.print("  "+baseType.toString()+" tmp;\n");

	for (int i = 0; i < in.size(); i++) {
	    NetStream s = (NetStream)in.elementAt(i);		
	    p.print("  tmp = "+s.name()+"in->read_"+baseType.toString()+"();\n");
	    
	    p.print("  "+out.name()+"out->write_"+baseType.toString()+"(tmp);\n");
	
	}

	p.print("}\n");

	p.print("\n");

	p.print("void run_"+thread_id+"() {\n");
	p.print("  int i;\n");
	
	for (int i = 0; i < in.size(); i++) {
		NetStream s = (NetStream)in.elementAt(i);
		
		p.print("  "+s.name()+"in = new mysocket(init_instance::get_incoming_socket("+s.getSource()+","+s.getDest()+"));\n");
	}

	p.print("  "+out.name()+"out = new mysocket(init_instance::get_outgoing_socket("+out.getSource()+","+out.getDest()+"));\n");


	p.print("  for (i = 0; i < __number_of_iterations; i++) {\n");
	p.print("    __joiner_"+thread_id+"_work();\n");
	p.print("  }\n");

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
	p.print("#include <stdio.h>\n");
	p.println();
	p.print("#include <mysocket.h>\n");
	p.print("#include <init_instance.h>\n");
	p.println();

	p.print("int __number_of_iterations = 20;\n");
	p.println();

	for (int i = 0; i < threadNumber; i++) {
	    
	    p.print("extern void __declare_sockets_"+i+"();\n");
	    p.print("extern void run_"+i+"();\n");
	    p.print("static void *run_thread_"+i+"(void *param) {\n");
	    p.print("  run_"+i+"();\n");
	    p.print("}\n");

	}

	p.println();

	p.print("int main(void) {\n");

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
	
	    p.print("  }\n");
	}

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

	p.print("run_cluster: master.cpp");

	for (int i = 0; i < threadNumber; i++) {
	    p.print(" thread"+i+".cpp");
	}

	p.println();

	p.print("\tgcc -O2 -I$(STREAMIT_HOME)/library/cluster -o run_cluster master.cpp");
	
	for (int i = 0; i < threadNumber; i++) {
	    p.print(" thread"+i+".cpp");
	}

	p.print(" -L$(STREAMIT_HOME)/library/cluster -lpthread -lstdc++ -lcluster");

	p.println();
	p.println();
	
	try {
	    FileWriter fw = new FileWriter("Makefile.cluster");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write Makefile");
	}	
    }


    public static void generateConfigFile() {

	int threadNumber = NodeEnumerator.getNumberOfNodes();

	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);

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

	for (int i = 0; i < threadNumber; i++) {
	    p.print(i+" "+me+"\n");
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

}


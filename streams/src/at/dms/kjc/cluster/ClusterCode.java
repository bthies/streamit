package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.cluster.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.List;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.kjc.sir.lowering.*;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.io.*;

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

	p.print("  sleep(3);\n");
	p.print("  return 0;\n");

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
}



package at.dms.kjc.cluster;

import java.io.*;
import java.lang.*;
import java.util.*;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.*;

class FusionCode {

    public static void generateFusionHeader() {

	int threadCount = NodeEnumerator.getNumberOfNodes();
	
	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);

	p.print("#ifndef __FUSION_H\n");
	p.print("#define __FUSION_H\n");

	p.println();
	p.print("#define __ITERS 10000\n");
	p.print("#define __MULT 100\n");
	p.println();

	p.print("/*\n");

	for (int t = 0; t < threadCount; t++) {
	    
	    SIROperator oper = NodeEnumerator.getOperator(t);
	    Vector out = RegisterStreams.getNodeOutStreams(oper);

	    for (int i = 0; i < out.size(); i++) {
		NetStream stream = (NetStream)out.elementAt(i);
		int src = stream.getSource();
		int dst = stream.getDest();

		p.print("#define __FUSED_"+src+"_"+dst+"\n");
		p.print("#define __BUF_SIZE_MASK_"+src+"_"+dst+" 8191\n");
	    }
	}

	p.print(" */\n");
	p.print("#endif\n");

	try {
	    FileWriter fw = new FileWriter("fusion.h");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write fusion.h");
	}
    }

    public static void generateFusionFile(DiscoverSchedule d_sched) {
	
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
	p.print("#include \"fusion.h\"\n");
	p.println();
	
	p.print("int __max_iteration;\n");
	p.print("int __timer_enabled = 0;\n");
	p.print("int __frequency_of_chkpts;\n");
	p.println();

	for (int i = 0; i < threadNumber; i++) {
	    FlatNode node = NodeEnumerator.getFlatNode(i);
	    SIROperator oper = (SIROperator)node.contents;
	    Vector out = RegisterStreams.getNodeOutStreams(oper);
	    
	    for (int s = 0; s < out.size(); s++) {
		NetStream stream = (NetStream)out.elementAt(s);
		int src = stream.getSource();
		int dst = stream.getDest();
		String type = stream.getTypeToC();

		p.print(type+" BUFFER_"+src+"_"+dst+"[__BUF_SIZE_MASK_"+src+"_"+dst+" + 1];\n");
		p.print("int HEAD_"+src+"_"+dst+" = 0;\n");
		p.print("int TAIL_"+src+"_"+dst+" = 0;\n");
	    }
	}
	
	p.println();

	for (int i = 0; i < threadNumber; i++) {
	    FlatNode node = NodeEnumerator.getFlatNode(i);
	    int id = NodeEnumerator.getSIROperatorId(node.contents);

	    //if (!ClusterFusion.isEliminated(tmp)) {	    
	    //p.print("extern void __declare_sockets_"+i+"();\n");
	    //p.print("extern void __init_sockets_"+i+"(void (*)());\n");
	    if (node.contents instanceof SIRFilter) {
		//p.print("extern void __init_pop_buf__"+i+"();\n");
		//p.print("extern void __update_pop_buf__"+i+"();\n");
		p.print("extern void "+((SIRFilter)node.contents).getInit().getName()+"__"+id+"();\n");
	    }
	    p.print("extern void "+get_work_function(node.contents)+"();\n");
	}

	p.println();

	p.print("void main() {\n");
	p.print("  ");
	
	/*
	for (int i = 0; i < threadNumber; i++) {
	    //FlatNode tmp = NodeEnumerator.getFlatNode(i);
	    //if (!ClusterFusion.isEliminated(tmp)) {	    
	    p.print("__declare_sockets_"+i+"();");
	}

	p.println();
	p.println();

	p.print("  init_instance::initialize_sockets();");

	p.println();
	p.println();

	p.print("  ");

	for (int i = 0; i < threadNumber; i++) {
	    //FlatNode tmp = NodeEnumerator.getFlatNode(i);
	    //if (!ClusterFusion.isEliminated(tmp)) {	    
	    p.print("__init_sockets_"+i+"(NULL);");
	}

	p.println();
	p.println();
	*/

	int n_phases = d_sched.getNumberOfPhases();

	p.print("// number of phases: "+n_phases+"\n");

	p.println();
	p.println();
	p.print("  // ============= Initialization =============\n");
	p.println();

	for (int ph = 0; ph < n_phases; ph++) {
	
	    //p.print("  // ============= Phase: "+ph+" =============\n");

	    HashSet phase = d_sched.getAllOperatorsInPhase(ph);
	    Iterator iter = phase.iterator();

	    while (iter.hasNext()) {
		SIROperator oper = (SIROperator)iter.next();
		int id = NodeEnumerator.getSIROperatorId(oper);
		FlatNode node = NodeEnumerator.getFlatNode(id);

		Integer init = (Integer)ClusterBackend.initExecutionCounts.get(node);
		int init_int = 0;
		if (init != null) init_int = (init).intValue();

		Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(node);
		int steady_int = 0;
		if (steady != null) steady_int = (steady).intValue();

		if (steady_int > 0) {

		    if (init_int > 0) {
			
			if (oper instanceof SIRFilter) {
			    
			    //if (ph > 0) p.print("  __init_pop_buf__"+id+"(); "); else p.print("  ");
			    p.print("  ");
			    p.print(((SIRFilter)oper).getInit().getName()+"__"+id+"(); ");
			    
			} else {
			    p.print("  ");
			}
			
			p.print(get_loop(init_int, get_work_function(oper)+"();"));
			p.println();
			
		    } else {
			
			if (oper instanceof SIRFilter) {		
			    
			    //if (ph > 0) p.print("  __init_pop_buf__"+id+"(); ");
			    p.print("  ");
			    p.print(((SIRFilter)oper).getInit().getName()+"__"+id+"();");
			    p.println();
			}
		    }
		}
	    }

	    //p.println();
	}

	p.println();
	p.print("  // ============= Steady State =============\n");
	p.println();

	p.print("  for (int n = 0; n < (__ITERS / __MULT); n++) {\n");

	for (int ph = 0; ph < n_phases; ph++) {
	
	    //p.print("  // ============= Phase: "+ph+" =============\n");

	    HashSet phase = d_sched.getAllOperatorsInPhase(ph);
	    Iterator iter = phase.iterator();

	    while (iter.hasNext()) {
		SIROperator oper = (SIROperator)iter.next();
		int id = NodeEnumerator.getSIROperatorId(oper);
		FlatNode node = NodeEnumerator.getFlatNode(id);

		Integer init = (Integer)ClusterBackend.initExecutionCounts.get(node);
		int init_int = 0;
		if (init != null) init_int = (init).intValue();

		Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(node);
		int steady_int = 0;
		if (steady != null) steady_int = (steady).intValue();

		if (steady_int > 0) {

		    p.print("    for (int i=0; i<("+steady_int+"*__MULT); i++) { "+get_work_function(oper)+"(); }");

		}

		//p.print("    "+get_loop(steady_int * 100, get_work_function(oper)+"();"));
		/*
		if (oper instanceof SIRFilter && ph > 0) {		

		    p.print("    "+get_loop(steady_int, "__update_pop_buf__"+id+"(); "+get_work_function(oper)+"();"));
		} else {

		    p.print("    "+get_loop(steady_int, get_work_function(oper)+"();"));
		}
		*/

		p.println();

	    }

	    //p.println();
	    //p.print("  }\n");
	}
	
	p.print("  }\n");
	p.print("}");
	p.println();

	try {
	    FileWriter fw = new FileWriter("fusion.cpp");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write fusion.cpp");
	}
    }


    private static String get_loop(int times, String code) {    
	String res = "";
	if (times <= 4) {
	    for (int i = 0; i < times; i++) res += code;
	    return res;
	} else {
	    return "for (int i=0; i<"+times+"; i++) { "+code+" }"; 
	}
    }

    private static String get_work_function(SIROperator oper) {

	int id = NodeEnumerator.getSIROperatorId(oper);	

	if (oper instanceof SIRFilter) {   
	    return ((SIRFilter)oper).getWork().getName()+"__"+id;
	}
		    
	if (oper instanceof SIRSplitter) {
	    return "__splitter_"+id+"_work";
	}
	
	if (oper instanceof SIRJoiner) {
	    return "__joiner_"+id+"_work";
	}

	assert (1 == 0);
	return null;
    }



}

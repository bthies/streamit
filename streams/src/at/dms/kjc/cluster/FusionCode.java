
package at.dms.kjc.cluster;

import java.io.*;
import java.lang.*;
import java.util.*;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.*;

class FusionCode {


    public static int bestMult(int data_cache1, int data_cache2) {

	int threadCount = NodeEnumerator.getNumberOfNodes();
	int min_mult;// = cache_size;

	int histogram[] = new int[threadCount];

	for (int t = 0; t < threadCount; t++) {
	    
	    SIROperator oper = NodeEnumerator.getOperator(t);
	    int dws = DataEstimate.estimateDWS(oper);
	    int io = DataEstimate.estimateIOSize(oper);
	    int avail = 0;

	    // if dws < .8 * data_cahce1

	    if (dws / 8 * 10 < data_cache1) {
		avail = data_cache1 - dws;
	    } else {
		avail = data_cache2 - dws;
	    }

	    if (io == 0) io = 1;
	    int mult = avail / io;

	    //System.out.println("DWS: "+dws+" Avail: "+avail+" IO: "+io+" Mult: "+mult);

	    histogram[t] = mult;

	    //if (mult < min_mult) { min_mult = mult; }
	}	    
	
	Arrays.sort(histogram);

	System.out.println("[bestMult] min: "+histogram[0]+" max: "+histogram[threadCount-1]+" 10th-precentile: "+histogram[threadCount/10]);

	min_mult = histogram[threadCount/10];

	if (min_mult > 100) min_mult = 100;	
	if (min_mult < 0) min_mult = 1;	
	System.out.println("[bestMult] returning multiplicity : "+min_mult);

	return min_mult;
    }

    public static void generateFusionHeader() {

	int threadCount = NodeEnumerator.getNumberOfNodes();
	
	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);

	p.print("#ifndef __FUSION_H\n");
	p.print("#define __FUSION_H\n");
	p.println();
	
	p.print("#define pow2ceil(A) ((A<=256)?(256):(((A<=1024)?(1024):(((A<=4096)?(4096):(((A<=16384)?(16384):(((A<=65536)?(65536):(((A<=131072)?(131072):(((A<=262144)?(262144):(524288))))))))))))))\n");
	p.println();

	//p.print("#define __ITERS 10000\n");

	int mult = bestMult(16000,65000); // estimating best multiplicity 

	p.print("#define __MULT "+mult+"\n");
	p.println();


	//p.print("/*\n");

	for (int t = 0; t < threadCount; t++) {
	    
	    SIROperator oper = NodeEnumerator.getOperator(t);
	    Vector out = RegisterStreams.getNodeOutStreams(oper);

	    for (int i = 0; i < out.size(); i++) {
		NetStream stream = (NetStream)out.elementAt(i);
		int src = stream.getSource();
		int dst = stream.getDest();

		p.print("#define __FUSED_"+src+"_"+dst+"\n");

		SIROperator src_oper = NodeEnumerator.getOperator(src);
		SIROperator dst_oper = NodeEnumerator.getOperator(dst);

		boolean no_peek = false;

		if (dst_oper instanceof SIRJoiner ||
		    dst_oper instanceof SIRSplitter) { no_peek = true; }
		
		if (dst_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)dst_oper;
		    if (f.getPeekInt() == f.getPopInt()) { no_peek = true; }
		}

		if (no_peek) p.print("#define __NOPEEK_"+src+"_"+dst+"\n");

		int source_size = 0, dest_size = 0, max_size;
		
		if (src_oper instanceof SIRJoiner) {
		    SIRJoiner j = (SIRJoiner)src_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(src));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }
		    int push_n = j.getSumOfWeights();
		    int total = (steady_int * push_n);
		    p.print("//source pushes: "+steady_int+" * "+push_n+" = ("+total+" items)\n");

		    source_size = total;
		}

		if (src_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)src_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(src));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }
		    int push_n = f.getPushInt();
		    int total = (steady_int * push_n);
		    p.print("//source pushes: "+steady_int+" * "+push_n+" = ("+total+" items)\n");

		    source_size = total;
		}

		if (dst_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)dst_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }

		    int pop_n = f.getPopInt();
		    int peek_n = f.getPeekInt();

		    p.print("//destination pops: "+steady_int+" * "+pop_n+" ");		    
		    if (peek_n > pop_n) {
			p.print("peeks: "+(peek_n-pop_n)+" ");		    
		    }

		    int total = (steady_int * pop_n) + (peek_n - pop_n);

		    p.print(" = ("+total+" items)\n");

		    dest_size = total;
		}

		if (dst_oper instanceof SIRSplitter) {
		    SIRSplitter s = (SIRSplitter)dst_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }

		    int pop_n = s.getSumOfWeights();
		    if (s.getType().isDuplicate()) pop_n = 1;
		    p.print("//destination pops: "+steady_int+" * "+pop_n+" ");		    

		    int total = (steady_int * pop_n);
		    p.print(" = ("+total+" items)\n");

		    dest_size = total;
		}

		if (source_size > dest_size) max_size = source_size; else max_size = dest_size;

		p.print("#define __BUF_SIZE_MASK_"+src+"_"+dst+" (pow2ceil("+max_size+"*__MULT)-1)\n");

		p.print("\n");

	    }
	}

	//p.print(" */\n");
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
	p.print("#include <read_setup.h>\n");
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
	    p.print("extern void "+get_work_function(node.contents)+"(int);\n");

	    /*
	    String work_n = get_work_n_function(node.contents);
	    if (work_n != null) {
		p.print("extern void "+work_n+"(int n);\n");
	    }
	    */
	}

	p.println();

	p.print("void main(int argc, char **argv) {\n");
	p.print("  ");

	p.print("  read_setup::read_setup_file();\n");
	p.print("  __max_iteration = read_setup::max_iteration;\n");

	p.print("  for (int a = 1; a < argc; a++) {\n");
	p.print("    if (argc > a + 1 && strcmp(argv[a], \"-i\") == 0) {\n");
	p.print("      int tmp;\n");
	p.print("      sscanf(argv[a + 1], \"%d\", &tmp);\n");
	p.print("      printf(\"Number of Iterations: %d\n\", tmp);\n");
	p.print("      __max_iteration = tmp;\n");
	p.print("    }\n");
	p.print("  }\n");

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
			

			p.print("  "+get_work_function(oper)+"("+init_int+");");

			//p.print(get_loop(init_int, get_work_function(oper)+"();"));
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

	p.print("  for (int n = 0; n < (__max_iteration / __MULT); n++) {\n");

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

		    Vector out = RegisterStreams.getNodeOutStreams(oper);
		    for (int i = 0; i < out.size(); i++) {
			NetStream s = (NetStream)out.elementAt(i);
			int _s = s.getSource();
			int _d = s.getDest();
			p.print("    #ifdef __NOPEEK_"+_s+"_"+_d+"\n");
			p.print("    HEAD_"+_s+"_"+_d+" = 0; TAIL_"+_s+"_"+_d+" = 0;\n");
			p.print("    #endif\n");
		    }
		    
		    p.print("    "+get_work_function(oper)+"("+steady_int+"*__MULT);");
		}

		p.println();
	    }
	}
	
	p.print("  }\n");



	p.print("  for (int n = 0; n < (__max_iteration % __MULT); n++) {\n");

	for (int ph = 0; ph < n_phases; ph++) {
	
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

		    Vector out = RegisterStreams.getNodeOutStreams(oper);
		    for (int i = 0; i < out.size(); i++) {
			NetStream s = (NetStream)out.elementAt(i);
			int _s = s.getSource();
			int _d = s.getDest();
			p.print("    #ifdef __NOPEEK_"+_s+"_"+_d+"\n");
			p.print("    HEAD_"+_s+"_"+_d+" = 0; TAIL_"+_s+"_"+_d+" = 0;\n");
			p.print("    #endif\n");
		    }
		    
		    p.print("    "+get_work_function(oper)+"("+steady_int+");");
		}

		p.println();
	    }
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


    private static String get_work_n_function(SIROperator oper) {

	int id = NodeEnumerator.getSIROperatorId(oper);	

	/*
	if (oper instanceof SIRFilter) {   
	   return ((SIRFilter)oper).getWork().getName()+"__n__"+id;
	}
	*/

	return null;
    }



}

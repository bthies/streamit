package at.dms.kjc.cluster;

import java.io.*;
//import java.lang.*;
import java.util.*;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.*;
import at.dms.kjc.KjcOptions;
//import at.dms.kjc.sir.lowering.partition.WorkEstimate;

class FusionCode {

    public static int bestMult(int data_cache1, 
			       int data_cache2) {
			       
	/* WorkEstimate work_est) { */

	int threadCount = NodeEnumerator.getNumberOfNodes();
	int min_mult;// = cache_size;

	int histogram[] = new int[threadCount];

	for (int t = 0; t < threadCount; t++) {
	    
	    SIROperator oper = NodeEnumerator.getOperator(t);
	    int dws = DataEstimate.estimateDWS(oper);
	    int io = DataEstimate.estimateIOSize(oper);
	    int avail = 0;

	    // if dws < .8 * data_cahce1

	    if ((dws + io) / 8 * 10 < data_cache1) {
		avail = (data_cache1 - dws);// * 2 / 3;
	    } else {
		avail = (data_cache2 - dws) * 2 / 3;
	    }

	    if (io == 0) io = 1;
	    int mult = avail / io;

	    if (mult <= 0) mult = 1;


	    int globals = 0;
	    if (oper instanceof SIRFilter) {
		globals = DataEstimate.filterGlobalsSize((SIRFilter)oper);
	    }

	    System.out.println("DWS: "+dws+" (g="+globals+") Avail: "+avail+" IO: "+io+" Mult: "+mult);

	    /*
	    if (oper instanceof SIRFilter) {
		SIRFilter filter = (SIRFilter)oper;
		System.out.println("DWS Filter work: "+work_est.getWork(filter)+" reps: "+work_est.getReps(filter));

	    }
	    */

	    histogram[t] = mult;

	    //if (mult < min_mult) { min_mult = mult; }
	}	    
	
	Arrays.sort(histogram);

	System.out.println("[bestMult] [DWS] min: "+histogram[0]+" max: "+histogram[threadCount-1]);

	System.out.println("[bestMult] [DWS] 0th-precentile: "+histogram[threadCount-1]);

	System.out.println("[bestMult] [DWS] 10th-precentile: "+histogram[(threadCount-1)*9/10]);
	System.out.println("[bestMult] [DWS] 20th-precentile: "+histogram[(threadCount-1)*8/10]);
	System.out.println("[bestMult] [DWS] 30th-precentile: "+histogram[(threadCount-1)*7/10]);
	System.out.println("[bestMult] [DWS] 40th-precentile: "+histogram[(threadCount-1)*6/10]);
	System.out.println("[bestMult] [DWS] 50th-precentile: "+histogram[(threadCount-1)*5/10]);
	System.out.println("[bestMult] [DWS] 60th-precentile: "+histogram[(threadCount-1)*4/10]);
	System.out.println("[bestMult] [DWS] 70th-precentile: "+histogram[(threadCount-1)*3/10]);
	System.out.println("[bestMult] [DWS] 80th-precentile: "+histogram[(threadCount-1)*2/10]);
	System.out.println("[bestMult] [DWS] 90th-precentile: "+histogram[(threadCount-1)*1/10]);

	System.out.println("[bestMult] [DWS] 100th-precentile: "+histogram[0]);

	min_mult = histogram[(threadCount-1)/10];

	//if (min_mult > 100) min_mult = 100;	
	if (min_mult <= 0) min_mult = 1;	
	System.out.println("[bestMult] [DWS] Returning Multiplicity : "+min_mult);

	return min_mult;
    }

    public static void generateFusionHeader(SIRStream top_stream) {

	//WorkEstimate work_est = WorkEstimate.getWorkEstimate(top_stream);

	int threadCount = NodeEnumerator.getNumberOfNodes();
	
	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);

	p.print("#ifndef __FUSION_H\n");
	p.print("#define __FUSION_H\n");
	p.println();

	p.print("#define max(A,B) (((A)>(B))?(A):(B))\n");
	p.print("#define pow2ceil(A) ((A<=256)?(256):(((A<=1024)?(1024):(((A<=4096)?(4096):(((A<=16384)?(16384):(((A<=65536)?(65536):(((A<=131072)?(131072):(((A<=262144)?(262144):(524288))))))))))))))\n");
	p.println();

	//p.print("#define __ITERS 10000\n");

	//int mult = bestMult(16000,65000,work_est); // estimating best multiplicity 
	int mult = bestMult(8500,65000); // estimating best multiplicity 

	if (KjcOptions.nomult) mult = 1;

	p.print("#define __MULT "+mult+"\n");
	p.println();

	if (!KjcOptions.standalone) {

	    // if not standalone then comment out the fusion variables
	    p.print("/*\n");

	}

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

		int extra = 0;

		if (dst_oper instanceof SIRJoiner ||
		    dst_oper instanceof SIRSplitter) { no_peek = true; }
		
		if (dst_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)dst_oper;
		    extra = f.getPeekInt() - f.getPopInt();
		    if (f.getPeekInt() == f.getPopInt()) { no_peek = true; }
		}

		if (extra > 0) {
		    p.print("#define __PEEK_BUF_SIZE_"+src+"_"+dst+" "+extra+"\n");
		}


		//if (KjcOptions.peekratio == 1024 || no_peek) {
		
		if (no_peek || KjcOptions.modfusion == false) {
		    p.print("#define __NOMOD_"+src+"_"+dst+"\n");
		}
		
		//if (no_peek) p.print("#define __NOMOD_"+src+"_"+dst+"\n");
		//p.print("#define __NOMOD_"+src+"_"+dst+"\n");

		int source_init_items = 0;
		int source_steady_items = 0;
		int dest_init_items = 0;
		int dest_steady_items = 0;
		int dest_peek = 0;

		// steady state, source
		
		if (src_oper instanceof SIRJoiner) {
		    SIRJoiner j = (SIRJoiner)src_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(src));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }
		    int push_n = j.getSumOfWeights();
		    int total = (steady_int * push_n);
		    p.print("//source pushes: "+total+" items during steady state\n");

		    source_steady_items = total;
		}

		if (src_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)src_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(src));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }
		    int push_n = f.getPushInt();
		    int total = (steady_int * push_n);
		    p.print("//source pushes: "+total+" items during steady state\n");

		    source_steady_items = total;
		}

		// init sched, source

		if (src_oper instanceof SIRJoiner) {
		    SIRJoiner j = (SIRJoiner)src_oper;
		    Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(src));
		    int init_int = 0;
		    if (init != null) { init_int = (init).intValue(); }
		    int push_n = j.getSumOfWeights();
		    int total = (init_int * push_n);
		    p.print("//source pushes: "+total+" items during init schedule\n");

		    source_init_items = total;
		}

		if (src_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)src_oper;
		    Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(src));
		    int init_int = 0;
		    if (init != null) { init_int = (init).intValue(); }
		    int push_n = f.getPushInt();
		    int total = (init_int * push_n);
		    p.print("//source pushes: "+total+" items during init schedule\n");

		    source_init_items = total;
		}


		// destination peek

		if (dst_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)dst_oper;
		    int pop_n = f.getPopInt();
		    int peek_n = f.getPeekInt();
		    if (peek_n > pop_n) {
			dest_peek = peek_n - pop_n;
			p.print("//destination peeks: "+(peek_n - pop_n)+" extra items\n");
		    }
		}

		// steady state, dest

		if (dst_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)dst_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }
		    int pop_n = f.getPopInt();
		    int total = (steady_int * pop_n);
		    p.print("//destination pops: "+total+" items during steady state\n");    
		    dest_steady_items = total;
		}

		if (dst_oper instanceof SIRSplitter) {
		    SIRSplitter s = (SIRSplitter)dst_oper;
		    Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
		    int steady_int = 0;
		    if (steady != null) { steady_int = (steady).intValue(); }
		    int pop_n = s.getSumOfWeights();
		    if (s.getType().isDuplicate()) pop_n = 1;
		    int total = (steady_int * pop_n);
		    p.print("//destination pops: "+total+" items during steady state\n");
		    dest_steady_items = total;
		}

		// init sched, dest

		if (dst_oper instanceof SIRFilter) {
		    SIRFilter f = (SIRFilter)dst_oper;
		    Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
		    int init_int = 0;
		    if (init != null) { init_int = (init).intValue(); }
		    int pop_n = f.getPopInt();
		    int total = (init_int * pop_n);
		    p.print("//destination pops: "+total+" items during init schedule\n");    
		    dest_init_items = total;
		}

		if (dst_oper instanceof SIRSplitter) {
		    SIRSplitter s = (SIRSplitter)dst_oper;
		    Integer init = (Integer)ClusterBackend.initExecutionCounts.get(NodeEnumerator.getFlatNode(dst));
		    int init_int = 0;
		    if (init != null) { init_int = (init).intValue(); }
		    int pop_n = s.getSumOfWeights();
		    if (s.getType().isDuplicate()) pop_n = 1;
		    int total = (init_int * pop_n);
		    p.print("//destination pops: "+total+" items during init_schedule\n");
		    dest_init_items = total;
		}

		int steady_items = 0;
		int init_items = 0;

		if (source_steady_items > dest_steady_items) {
		    steady_items = source_steady_items;
		} else {
		    steady_items = dest_steady_items;
		}

		if (source_init_items > dest_init_items) {
		    init_items = source_init_items;
		} else {
		    init_items = dest_init_items;
		}
		
		p.print("#define __BUF_SIZE_MASK_"+src+"_"+dst+" (pow2ceil(max("+init_items+","+steady_items+"*__MULT)+"+dest_peek+")-1)\n");

		p.print("\n");

	    }
	}

	if (!KjcOptions.standalone) {

	    // if not standalone then comment out the fusion variables
	    p.print(" */\n");

	}

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

    // implicit_mult - how much schedule has been scaled up due to
    // peek optimization

    public static void generateFusionFile(DiscoverSchedule d_sched, int implicit_mult) {
	
	int threadNumber = NodeEnumerator.getNumberOfNodes();
	
	TabbedPrintWriter p;
	StringWriter str; 
	
	str = new StringWriter();
        p = new TabbedPrintWriter(str);
	
	p.print("#include <pthread.h>\n");
	p.print("#include <unistd.h>\n");
	//p.print("#include <signal.h>\n");
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
	p.print("#include <timer.h>\n");
	p.print("#include \"fusion.h\"\n");
	p.println();
	
	p.print("int __max_iteration;\n");
	p.print("int __timer_enabled = 0;\n");
	p.print("int __frequency_of_chkpts;\n");
	p.print("volatile int __vol;\n");
	p.print("proc_timer tt;\n");
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
		//p.print(type+" PEEK_BUFFER_"+src+"_"+dst+"[__PEEK_BUF_SIZE_"+src+"_"+dst+"];\n");
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
	    	// Init function name should be OK even for SIRPredefinedFilter
	    	p.print("extern void "+getWorkName((SIRFilter)node.contents,id)+"();\n");
	    }

	    if (node.contents instanceof SIRTwoStageFilter) {
		String initWork = ((SIRTwoStageFilter)node.contents).getInitWork().getName()+"__"+id;
		p.print("extern void "+initWork+"();\n");
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

	p.print("int main(int argc, char **argv) {\n");
	p.print("  ");

	p.print("  read_setup::read_setup_file();\n");
	p.print("  __max_iteration = read_setup::max_iteration;\n");

	p.print("  for (int a = 1; a < argc; a++) {\n");
	p.print("    if (argc > a + 1 && strcmp(argv[a], \"-i\") == 0) {\n");
	p.print("      int tmp;\n");
	p.print("      sscanf(argv[a + 1], \"%d\", &tmp);\n");
	p.print("      fprintf(stderr,\"Number of Iterations: %d\\n\", tmp);\n");
	p.print("      __max_iteration = tmp;\n");
	p.print("    }\n");

	p.print("    if (strcmp(argv[a], \"-t\") == 0) {\n"); 
	p.print("       fprintf(stderr,\"Timer enabled.\\n\");\n"); 
	p.print("       __timer_enabled = 1;"); 
	p.print("    }\n");

	p.print("  }\n");


	p.print("  if ("+implicit_mult+" > 1) {\n");
	p.print("    fprintf(stderr,\"Implicit multiplicity: "+implicit_mult+"\\n\");\n");
	p.print("    int tmp;\n");
	p.print("    tmp = __max_iteration / "+implicit_mult+";\n");
	p.print("    if (__max_iteration % "+implicit_mult+" > 0) tmp++;\n");
	p.print("    __max_iteration = tmp;\n");
	p.print("    fprintf(stderr,\"Number of Iterations: %d (%d)\\n\", __max_iteration, __max_iteration * "+implicit_mult+");\n");
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
			    p.print(getWorkName((SIRFilter)oper,id) +"(); ");
			    
			} else {
			    p.print("  ");
			}
			

			if (oper instanceof SIRTwoStageFilter) {

			    String initWork = ((SIRTwoStageFilter)node.contents).getInitWork().getName()+"__"+id;
			    p.print("  "+initWork+"();");

			    if (init_int > 1) {
				p.print("  "+get_work_function(oper)+"("+
					(init_int-1)+");");
			    }
			    
			} else {
			    p.print("  "+get_work_function(oper)+"("+init_int+");");
			}

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
	p.print("  tt.start();\n");

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
			p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
			p.print("    #ifdef __PEEK_BUF_SIZE_"+_s+"_"+_d+"\n");

			p.print("      for (int __y = 0; __y < __PEEK_BUF_SIZE_"+_s+"_"+_d+"; __y++) {\n");
			p.print("        BUFFER_"+_s+"_"+_d+"[__y] = BUFFER_"+_s+"_"+_d+"[__y + TAIL_"+_s+"_"+_d+"];\n");
			p.print("      }\n");
			p.print("      HEAD_"+_s+"_"+_d+" -= TAIL_"+_s+"_"+_d+";\n");
			p.print("      TAIL_"+_s+"_"+_d+" = 0;\n");
			
			p.print("    #else\n");

			p.print("      HEAD_"+_s+"_"+_d+" = 0;\n");
			p.print("      TAIL_"+_s+"_"+_d+" = 0;\n");

			p.print("    #endif\n");
			p.print("    #endif\n");


			
			/*

			p.print("    if (HEAD_"+_s+"_"+_d+" - TAIL_"+_s+"_"+_d+" != __PEEK_BUF_SIZE_"+_s+"_"+_d+") {\n");
			p.print("      fprintf(stderr,\"head: %d\\n\", HEAD_"+_s+"_"+_d+");\n");
			p.print("      fprintf(stderr,\"tail: %d\\n\", TAIL_"+_s+"_"+_d+");\n");
			p.print("      assert(1 == 0);\n");
			p.print("    }\n");
			*/
		    }
		    
		    p.print("    "+get_work_function(oper)+"("+steady_int+"*__MULT);");
		}

		p.println();
	    }
	}
	
	p.print("  }\n");



	//p.print("  for (int n = 0; n < (__max_iteration % __MULT); n++) {\n");
	p.print("  int rem = (__max_iteration % __MULT);\n\n");

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
			p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
			p.print("    #ifdef __PEEK_BUF_SIZE_"+_s+"_"+_d+"\n");

			p.print("      for (int __y = 0; __y < __PEEK_BUF_SIZE_"+_s+"_"+_d+"; __y++) {\n");
			p.print("        BUFFER_"+_s+"_"+_d+"[__y] = BUFFER_"+_s+"_"+_d+"[__y + TAIL_"+_s+"_"+_d+"];\n");
			p.print("      }\n");
			p.print("      HEAD_"+_s+"_"+_d+" -= TAIL_"+_s+"_"+_d+";\n");
			p.print("      TAIL_"+_s+"_"+_d+" = 0;\n");
			
			p.print("    #else\n");

			p.print("      HEAD_"+_s+"_"+_d+" = 0;\n");
			p.print("      TAIL_"+_s+"_"+_d+" = 0;\n");

			p.print("    #endif\n");
			p.print("    #endif\n");
		      
			/*

			p.print("    if (HEAD_"+_s+"_"+_d+" - TAIL_"+_s+"_"+_d+" != __PEEK_BUF_SIZE_"+_s+"_"+_d+") {\n");
			p.print("      fprintf(stderr,\"head: %d\\n\", HEAD_"+_s+"_"+_d+");\n");
			p.print("      fprintf(stderr,\"tail: %d\\n\", TAIL_"+_s+"_"+_d+");\n");
			p.print("      assert(1 == 0);\n");
			p.print("    }\n");
			*/
		    }
		    
		    p.print("    "+get_work_function(oper)+"("+steady_int+"*rem);");
		}

		/*

		if (steady_int > 0) {

		    Vector out = RegisterStreams.getNodeOutStreams(oper);
		    for (int i = 0; i < out.size(); i++) {
			NetStream s = (NetStream)out.elementAt(i);
			int _s = s.getSource();
			int _d = s.getDest();



			p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
			p.print("    HEAD_"+_s+"_"+_d+" = 0; TAIL_"+_s+"_"+_d+" = 0;\n");
			p.print("    #endif\n");
		    }
		    
		    p.print("    "+get_work_function(oper)+"("+steady_int+");");
		}
		*/

		p.println();
	    }
	}
	
	//p.print("  }\n");

	p.print("  tt.stop();\n");
	p.print("  tt.output(stderr);\n");

	p.print("  return 0;\n");
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
	    return getWorkName((SIRFilter)oper,id);
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

    // work function name  should change to include them)
    // If not a predefined filter then the work method should have a useful
    // unique name.  If a predefined filter, the work method may be called
    // "UNINITIALIZED DUMMY METHOD" (A Kopi2Sir bug?) so give it a reasonable name.
    private static String getWorkName(SIRFilter f, int id) {
	if (f instanceof SIRPredefinedFilter) {
		//System.err.println("FlatIRToCluster Predef work name = "+f.getName()+"__work__"+id);		
	    return f.getName()+"__work__"+id;
	} else {
		//System.err.println("FlatIRToCluster Filter work name = "+f.getWork().getName()+"__"+id);
	    return f.getWork().getName()+"__"+id;
	}
    }


}

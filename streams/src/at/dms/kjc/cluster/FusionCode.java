package at.dms.kjc.cluster;

import java.io.*;
import java.util.*;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.*;
//import at.dms.kjc.CType;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.cluster.ClusterUtils;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.common.CommonUtils;

/**
 * A class that generates code for the --cluster --standalone, where
 * there is just a single thread that invokes work functions of the
 * stream operators. The class generates files: fusion.h and fusion.cpp
 */

class FusionCode {
    public static int mult = 1;

    /**
     * estimates a multiplicity for execution scaling using cache sizes
     * 
     * @param data_cache1 L1 data cache size
     * @param data_cache2 L2 data cache size
     * @return the multiplicity
     */

    public static int bestMult(int data_cache1, 
                               int data_cache2) {
                   
        /* WorkEstimate work_est) { */

        int threadCount = NodeEnumerator.getNumberOfNodes();
        int min_mult;// = cache_size;

        int histogram[] = new int[threadCount];

    // create a histogram of multipicities for individual operators

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

            if (ClusterBackend.debugPrint) {
                System.out.println("DWS: "+dws+" (g="+globals+") Avail: "+avail+" IO: "+io+" Mult: "+mult);
            }

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

        if (ClusterBackend.debugPrint) {
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
        }

        min_mult = histogram[(threadCount-1)/10];

        //if (min_mult > 100) min_mult = 100;   
        if (min_mult <= 0) min_mult = 1;    

        if (ClusterBackend.debugPrint) {
            System.out.println("[bestMult] [DWS] Returning Multiplicity : "+min_mult);
        }

        return min_mult;
    }

    /**
     * creates the fusion.h file
     * 
     * @param top_stream the top stream (not used)
     * @param inc_mult specifies whether to use execution scaling
     */

    public static void generateFusionHeader(SIRStream top_stream, boolean inc_mult) {

        //WorkEstimate work_est = WorkEstimate.getWorkEstimate(top_stream);

        int threadCount = NodeEnumerator.getNumberOfNodes();
    
        CodegenPrintWriter p = null;
    
        try {
            p = new CodegenPrintWriter(new FileWriter("fusion.h"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        p.print("#ifndef __FUSION_H\n");
        p.print("#define __FUSION_H\n");
        p.newline();

        p.print("#define max(A,B) (((A)>(B))?(A):(B))\n");
        p.print("#define pow2ceil(A) ((A<=256)?(256):(((A<=1024)?(1024):(((A<=4096)?(4096):(((A<=16384)?(16384):(((A<=65536)?(65536):(((A<=131072)?(131072):(((A<=262144)?(262144):(((A<=524288)?(524288):(((A<=1048576)?(1048576):(((A<=2097152)?(2097152):(((A<=4194304)?(4194304):(((A<=8388608)?(8388608):(((A<=16777216)?(16777216):(((A<=33554432)?(33554432):(((A<=67108864)?(67108864):(((A<=134217728)?(134217728):(((A<=268435456)?(268435456):(((A<=536870912)?(536870912):(1073741824))))))))))))))))))))))))))))))))))))");
        p.newLine();

        //p.print("#define __ITERS 10000\n");

        //int mult = bestMult(16000,65000,work_est); // estimating best multiplicity 
        mult = bestMult(8500,65000); // estimating best multiplicity 

        if (KjcOptions.nomult || !inc_mult) mult = 1;

        if (mult != 1) {
            p.print("#define __MULT " + mult + "\n");
            p.newLine();
        }
        
        if (KjcOptions.standalone) {
        //p.print("#define __CLUSTER_STANDALONE\n");
        // threadcount is the number of operators after fusion/cacheopts
        for (int t = 0; t < threadCount; t++) {
        
            SIROperator oper = NodeEnumerator.getOperator(t);
            for (Tape stream : RegisterStreams.getNodeOutStreams(oper)) {
                if (stream != null) {
//                int src = stream.getSource();
//                int dst = stream.getDest();
//              
//                if (FixedBufferTape.isFixedBuffer(src,dst)) {
//                    p.print("#define __FUSED_"+src+"_"+dst+"\n");
//                }
//                
//                int extraPeeks = FixedBufferTape.getRemaining(src,dst);
//                if (extraPeeks > 0) {
//                    p.print("//destination peeks: "+(extraPeeks)+" extra items\n");
//                    p.print("#define __PEEK_BUF_SIZE_" + src + "_" + dst
//                                + " " + extraPeeks + "\n");
//                }
//                //if (KjcOptions.peekratio == 1024 || no_peek) {
//
//                if (! FixedBufferTape.needsModularBuffer(src,dst)) {
//                    p.print("#define __NOMOD_"+src+"_"+dst+"\n");
//                }
//
//                int buffersize = FixedBufferTape.bufferSize(src, dst, p, true);
//        
//                p.print("#define __BUF_SIZE_MASK_" + src + "_" + dst
//                            + " (pow2ceil(" /*max(" + FixedBufferTape.getInitItems()
//                            + "," + FixedBufferTape.getSteadyItems()*/ + buffersize + (mult == 1? "" : "*__MULT") /*+ ")"*/ + "+"
//                            +  extraPeeks + ")-1)\n");

                    p.print(stream.dataDeclarationH());
                    p.print("\n");
                }
            }
        }

        }
        p.print("#endif\n"); 
        
        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write fusion.h");
        }
    }

    // implicit_mult - how much schedule has been scaled up due to
    // peek optimization

    /**
     * creates the fusion.cpp file
     * 
     * @param d_sched reference to {@link DiscoverSchedule} with a schedule
     */

    public static void generateFusionFile(DiscoverSchedule d_sched) {
    
        int threadNumber = NodeEnumerator.getNumberOfNodes();
    
        CodegenPrintWriter p = null;
        try {
            p = new CodegenPrintWriter(new FileWriter("fusion.cpp"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    
        // math.h is not needed in fusion.cpp, but 
        // when concatenating threads, having math.h included later 
        // leads to some syntax errors -- presumably one of our header files
        // clobbers some name (that does not affect the correctness of 
        // execution of our compiled programs) that math.h uses.
        p.println("#include <math.h>");
        p.print("#include <pthread.h>\n");
        p.print("#include <unistd.h>\n");
        //p.print("#include <signal.h>\n");
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
        //p.print("#include <read_setup.h>\n");
        p.print("#include <timer.h>\n");
        p.print("#include \"fusion.h\"\n");
        p.println("#include \"structs.h\"");
        if (KjcOptions.countops) {
            p.println("#include \"profiler.h\"");
        }
        p.newLine();
    
        p.print("int __max_iteration;\n");
        p.print("int __timer_enabled = 0;\n");
        p.print("int __frequency_of_chkpts;\n");
        p.print("volatile int __vol;\n");
        p.print("proc_timer tt(\"total runtime\");\n");
        p.newLine();

        // declare profiling timers
        if (KjcOptions.profile) {
            String ident = InsertTimers.getIdentifier();
            p.println("proc_timer " + ident + "[" + InsertTimers.getNumTimers() + "] = {");
            p.indent();
            for (int i=0; i<InsertTimers.getNumTimers(); i++) { 
                String name = InsertTimers.getTimerName(i);
                p.print("proc_timer(\"" + name + "\")");
                if (i!=InsertTimers.getNumTimers()-1) { 
                    p.print(", ");
                }
                p.println("// " + ident + "[" + i + "]");
            }
            p.outdent();
            p.println("};");
        }
        p.newLine();

        for (int i = 0; i < threadNumber; i++) {
            FlatNode node = NodeEnumerator.getFlatNode(i);
            SIROperator oper = (SIROperator)node.contents;

            for (Tape stream : RegisterStreams.getNodeOutStreams(oper)) {
                if (stream != null) {
                    p.print(stream.dataDeclaration());
//                int src = stream.getSource();
//                int dst = stream.getDest();
//                if (FixedBufferTape.isFixedBuffer(src, dst)) {
//                    String type = ClusterUtils.CTypeToString(stream.getType());
//
//                    p.print(type + " BUFFER_" + src + "_" + dst
//                            + "[__BUF_SIZE_MASK_" + src + "_" + dst
//                            + " + 1];\n");
//                    p.print("int HEAD_" + src + "_" + dst + " = 0;\n");
//                    p.print("int TAIL_" + src + "_" + dst + " = 0;\n");
//
//                    p.newLine();
//                }
                }
            }
        }

        for (SIRJoiner j : ClusterCode.feedbackJoineersNeedingPrep) {
            p.println("extern void __feedbackjoiner_"+ NodeEnumerator.getSIROperatorId(j) +"_prep();");
        }

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

            if (node.contents instanceof SIRTwoStageFilter) {
                String initWork = ((SIRTwoStageFilter)node.contents).getInitWork().getName()+"__"+id;
                p.print("extern void "+initWork+"();\n");
            }

            if (node.contents instanceof SIRStream && SIRPortal.getPortalsWithSender((SIRStream)node.contents).length > 0) {
                p.print("extern void __init_sdep_"+id+"();\n");
            }
            
            if (node.contents instanceof SIRStream && SIRPortal.getPortalsWithReceiver((SIRStream)node.contents).length > 0) {
                p.print("extern void check_messages__"+id+"();\n");
            }
            p.print("extern void "+get_work_function(node.contents)+"(int);\n");

	    if ((node.contents instanceof SIRFileReader)
	            || (node.contents instanceof SIRFileWriter)) {
		p.print("extern void "+get_work_function(node.contents)+"__close();\n");
	    }

	    if ((node.contents instanceof SIRFilter) &&
		!(node.contents instanceof SIRFileReader) &&
		!(node.contents instanceof SIRFileWriter)) {
		
		String input_type = CommonUtils.CTypeToString(((SIRFilter)node.contents).getInputType(),true);
		String output_type = CommonUtils.CTypeToString(((SIRFilter)node.contents).getOutputType(), true);

                p.println("#ifdef BUFFER_MERGE");
		p.println("extern void "+get_work_function(node.contents)+"__mod(int ____n, "+input_type+" *____in, "+output_type+" *____out);");
		p.println("extern void "+get_work_function(node.contents)+"__mod2(int ____n, "+input_type+" *____in, "+output_type+" *____out, int s1, int s2);");
		p.println("#endif");
	    }

            /*
              String work_n = get_work_n_function(node.contents);
              if (work_n != null) {
              p.print("extern void "+work_n+"(int n);\n");
              }
            */
        }

        p.newLine();

        p.print("int main(int argc, char **argv) {\n");

        // tell the profiler how many ID's there are
        if (KjcOptions.countops) {
            p.println("  profiler::set_num_ids(" + InsertCounters.getNumIds() + ");");
        }

        p.print("  read_setup::read_setup_file();\n");
        p.print("  __max_iteration = read_setup::max_iteration;\n");

        p.print("  for (int a = 1; a < argc; a++) {\n");
        p.print("    if (argc > a + 1 && strcmp(argv[a], \"-i\") == 0) {\n");
        p.print("      int tmp;\n");
        p.print("      sscanf(argv[a + 1], \"%d\", &tmp);\n");
        p.println("#ifdef VERBOSE");
        p.print("      fprintf(stderr,\"Number of Iterations: %d\\n\", tmp);\n");
        p.println("#endif");
        p.print("      __max_iteration = tmp;\n");
        p.print("    }\n");

        p.print("    if (strcmp(argv[a], \"-t\") == 0) {\n"); 
        p.println("#ifdef VERBOSE");
        p.print("       fprintf(stderr,\"Timer enabled.\\n\");\n"); 
        p.println("#endif");
        p.print("       __timer_enabled = 1;"); 
        p.print("    }\n");

        p.print("  }\n");

// implicit_mult used to be a parameter, but entire peek-scaling
// feature has been turned off since much more likely to be a pessimization
// than an optimization
//        p.print("  if ("+implicit_mult+" > 1) {\n");
//        p.println("#ifdef VERBOSE");
//        p.print("    fprintf(stderr,\"Implicit multiplicity: "+implicit_mult+"\\n\");\n");
//        p.println("#endif");
//        p.print("    int tmp;\n");
//        p.print("    tmp = __max_iteration / "+implicit_mult+";\n");
//        p.print("    if (__max_iteration % "+implicit_mult+" > 0) tmp++;\n");
//        p.print("    __max_iteration = tmp;\n");
//        p.println("#ifdef VERBOSE");
//        p.print("    fprintf(stderr,\"Number of Iterations: %d (%d)\\n\", __max_iteration, __max_iteration * "+implicit_mult+");\n");
//        p.println("#endif");
//        p.print("  }\n");

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

        p.newLine();
        p.newLine();
        p.print("  // ============= Initialization =============\n");

        for (SIRJoiner j : ClusterCode.feedbackJoineersNeedingPrep) {
            p.println("__feedbackjoiner_"+ NodeEnumerator.getSIROperatorId(j) +"_prep();");
        }

        p.newLine();
        
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
                            if (SIRPortal.getPortalsWithSender((SIRFilter)oper).length > 0) {
                                //p.print(" /* " + SIRPortal.getPortalsWithSender((SIRFilter)oper).length + "*/ ");
                                p.print("  __init_sdep_"+id+"();");
                            }                
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
                        p.newLine();
            
                    } else {
            
                        if (oper instanceof SIRFilter) {        
                
                            //if (ph > 0) p.print("  __init_pop_buf__"+id+"(); ");
                            p.print(((SIRFilter)oper).getInit().getName()+"__"+id+"();");
                            if (SIRPortal.getPortalsWithSender((SIRFilter)oper).length > 0) {
                                p.print("  __init_sdep_"+id+"();");
                            }
                            p.newLine();
                        }
                    }
                }
            }

            //p.println();
        }

        p.newLine();
        p.print("  // ============= Steady State =============\n");
        p.newLine();
        p.println("  if (__timer_enabled) {");
        p.println("    tt.start();");
        p.println("  }");
        p.print("  for (int n = 0; n < (__max_iteration " + (mult == 1? "" : " / __MULT") +  " ); n++) {\n");

        for (int ph = 0; ph < n_phases; ph++) {
    
            //p.print("  // ============= Phase: "+ph+" =============\n");

            HashSet phase = d_sched.getAllOperatorsInPhase(ph);
            Iterator iter = phase.iterator();

            while (iter.hasNext()) {
                SIROperator oper = (SIROperator)iter.next();
                int id = NodeEnumerator.getSIROperatorId(oper);
                FlatNode node = NodeEnumerator.getFlatNode(id);

                boolean rcv_msg = false;
                if (oper instanceof SIRFilter) {
                    if (SIRPortal.getPortalsWithReceiver((SIRFilter)oper).length > 0) {
                        rcv_msg = true;
                    }
                }

//                Integer init = (Integer)ClusterBackend.initExecutionCounts.get(node);
//                int init_int = 0;
//                if (init != null) init_int = (init).intValue();

                Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(node);
                int steady_int = 0;
                if (steady != null) steady_int = (steady).intValue();

                if (steady_int > 0) {

                    for (Tape stream : RegisterStreams.getNodeOutStreams(oper)) {
                        if (stream != null) {
                            // do an tape / buffer managment needed at beginning
                            // of iteration
                            // e.g. copying down read-ahead in a non-circular
                            // buffer.
                            p.print(stream.topOfWorkIteration());

                        }
                    }
                    if (rcv_msg)
                        p.print("    check_messages__" + id + "();\n");
                    p.print("    " + get_work_function(oper) + "(" + steady_int
                            + (mult == 1 ? "" : "*__MULT") + " );");
                }

                p.newLine();
            }
        }
        
        p.print("  }\n");


        if (mult != 1) {
        
        //p.print("  for (int n = 0; n < (__max_iteration % __MULT); n++) {\n");
        p.print("  int rem = (__max_iteration % __MULT);\n\n");

        for (int ph = 0; ph < n_phases; ph++) {
    
            HashSet phase = d_sched.getAllOperatorsInPhase(ph);
            Iterator iter = phase.iterator();

            while (iter.hasNext()) {
                SIROperator oper = (SIROperator)iter.next();
                int id = NodeEnumerator.getSIROperatorId(oper);
                FlatNode node = NodeEnumerator.getFlatNode(id);

                boolean rcv_msg = false;
                if (oper instanceof SIRFilter) {
                    if (SIRPortal.getPortalsWithReceiver((SIRFilter)oper).length > 0) {
                        rcv_msg = true;
                    }
                }
                
//                Integer init = (Integer)ClusterBackend.initExecutionCounts.get(node);
//                int init_int = 0;
//                if (init != null) init_int = (init).intValue();

                Integer steady = (Integer)ClusterBackend.steadyExecutionCounts.get(node);
                int steady_int = 0;
                if (steady != null) steady_int = (steady).intValue();


                if (steady_int > 0) {

                    for (Tape stream : RegisterStreams.getNodeOutStreams(oper)) {
                      if (stream == null) continue;
                        // do an tape / buffer managment needed at beginning of iteration
                        // e.g. copying down read-ahead in a non-circular buffer.
                        p.print(stream.topOfWorkIteration());
                        }
                    }

                    if (rcv_msg) p.print("    check_messages__"+id+"();\n");
                    p.print("    "+get_work_function(oper)+"("+steady_int+"*rem);");
                    
                    if ((node.contents instanceof SIRFileReader) ||
                        (node.contents instanceof SIRFileWriter)) {
                        p.print("    "+get_work_function(oper)+"__close();\n");
                    }
                    
                }

                /*

                if (steady_int > 0) {

                Vector out = RegisterStreams.getNodeOutStreams(oper);
                for (int i = 0; i < out.size(); i++) {
                Tape s = (Tape)out.elementAt(i);
                int _s = s.getSource();
                int _d = s.getDest();



                p.print("    #ifdef __NOMOD_"+_s+"_"+_d+"\n");
                p.print("    HEAD_"+_s+"_"+_d+" = 0; TAIL_"+_s+"_"+_d+" = 0;\n");
                p.print("    #endif\n");
                }
            
                p.print("    "+get_work_function(oper)+"("+steady_int+");");
                }
                */

                p.newLine();
            }
        }
        
        //p.print("  }\n");

        p.indent();

        // print -t timer summary.
        p.println("if (__timer_enabled) {");
        p.println("  tt.stop();");
        p.println("  tt.output(stderr);");
        p.println("}");
        
        // print profiling timer summary
        p.println();
        if (KjcOptions.profile) {
            String ident = InsertTimers.getIdentifier();
            p.println("FILE* timer_output = fopen(\"profile.c.log\", \"w\");");
            p.println("for (int i = 0; i<" + InsertTimers.getNumTimers() + "; i++) {"); 
            p.println("  " + ident + "[i].output(timer_output);");
            p.println("}");
            p.println("fclose(timer_output);");
            p.println("printf(\"Profiling information written to profile.c.log.\\n\");");
        }
        p.println();

        // print profiling summary
        if (KjcOptions.countops) {
            p.println("  profiler::summarize();");
        }

        p.println("return 0;");
        p.outdent();

        p.println("}");

        try {
            p.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write fusion.cpp");
        }
    }


//    private static String get_loop(int times, String code) {
//        String res = "";
//        if (times == 0) {
//            return "";
//        }
//        if (times == 1) {
//            return code;
//        }
//        //res += "// FusionCode_2 " + times + "\n";
//        if (times <= 4) {
//            for (int i = 0; i < times; i++)
//                res += code;
//            return res;
//        } else {
//            return "for (int i=0; i<" + times + "; i++) { " + code + " }";
//        }
//    }

    private static String get_work_function(SIROperator oper) {

        int id = NodeEnumerator.getSIROperatorId(oper);

        if (oper instanceof SIRFilter) {
            return ClusterUtils.getWorkName((SIRFilter) oper, id);
        }

        if (oper instanceof SIRSplitter) {
            return "__splitter_" + id + "_work";
        }

        if (oper instanceof SIRJoiner) {
            return "__joiner_" + id + "_work";
        }

        assert (false);
        return null;
    }


//    private static String get_work_n_function(SIROperator oper) {
//
//        int id = NodeEnumerator.getSIROperatorId(oper); 
//
//        /*
//          if (oper instanceof SIRFilter) {   
//          return ((SIRFilter)oper).getWork().getName()+"__n__"+id;
//          }
//        */
//
//        return null;
//    }



}

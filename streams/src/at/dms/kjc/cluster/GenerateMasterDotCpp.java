package at.dms.kjc.cluster; 

import java.io.FileWriter;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;

/**
 * Generate "master.cpp"
 * 
 * 
 * @author Janis
 */
public class GenerateMasterDotCpp {

    /**
     * is code (threadNNN.cpp) generated for this NNN?
     * <br/> assumes input is in range of generated numbers from {@link NodeEnumerator} 
     * @param i : a node (thread) number
     * @return true if code was not generated for the thread, false otherwise.
     */
    public static boolean isEliminated(int i) {
        FlatNode f = NodeEnumerator.getFlatNode(i);
        return ClusterFusion.isEliminated(f)
            || (f.isJoiner() && f.getTotalIncomingWeights() == 0)
            || (f.isSplitter() && f.getTotalOutgoingWeights() == 0);
    }
    
    /**
     * Generate "master.cpp"
     * 
     * <p>This file is concerned with setting up and tearing down 
     * sockets, threads, profiler.</p>
     *
     */
    public static void generateMasterDotCpp() {

        int threadNumber = NodeEnumerator.getNumberOfNodes();

        CodegenPrintWriter p = new CodegenPrintWriter();
    
        p.print("#include <pthread.h>\n");
        p.print("#include <unistd.h>\n");
        p.print("#include <signal.h>\n");
        p.print("#include <string.h>\n");
        p.print("#include <stdlib.h>\n");
        p.print("#include <stdio.h>\n");
        p.newLine();
        p.print("#include <message.h>\n");
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
        p.print("#include <streamit_random.h>\n");
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
            if (isEliminated(i)) {continue;}
            p.print("extern void __declare_sockets_" + i + "();\n");
            p.print("extern thread_info *__get_thread_info_" + i + "();\n");
            p.print("extern void run_" + i + "();\n");
            p.print("pthread_attr_t __pthread_attr_" + i + ";\n");
            p.print("pthread_t __pthread_" + i + ";\n");
            p.print("static void *run_thread_" + i + "(void *param) {\n");
            p.print("  run_" + i + "();\n");
            p.print("}\n");

        }

        p.newLine();

        p.print("static void *run_join(void *param) {\n");

        for (int i = 0; i < threadNumber; i++) {
            if (isEliminated(i)) {continue;}
            p.print("  if (myip == init_instance::get_thread_ip("+i+")) {\n");
            p.print("    pthread_join(__pthread_"+i+", NULL);\n");
            p.print("    __get_thread_info_"+i+"()->set_active(false);\n");
            p.print("  }\n");
        }
        p.print("  sleep(1);\n");
        p.print("  if (__ccp_ip != 0) for(;;) sleep(1);\n");
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
            if (isEliminated(i)) {continue;}
            p.print("  if (myip == init_instance::get_thread_ip("+i+")) {\n");
            p.print("    __declare_sockets_"+i+"();\n");
            p.print("  }\n");
        }

        p.print("  init_instance::initialize_sockets();\n");

        //p.print("  pthread_t id;\n");

        for (int i = 0; i < threadNumber; i++) {
            if (isEliminated(i)) {continue;}
            // estimate stack size needed by this thread
            int stackSize = NodeEnumerator.getStackSize(i);
            p.print("  if (myip == init_instance::get_thread_ip(" + i + ")) {\n");
            p.print("    thread_info *info = __get_thread_info_" + i + "();\n");
            p.print("    int *state = info->get_state_flag();\n");
            p.print("    *state = RUN_STATE;\n");
            p.print("    pthread_attr_setstacksize(&__pthread_attr_" + i 
                    + ", PTHREAD_STACK_MIN+" + stackSize + ");\n");
            p.print("    pthread_create(&__pthread_" + i + ", &__pthread_attr_" 
                    + i + ", run_thread_" + i + ", (void*)\"thread" + i
                    + "\");\n");
            p.print("    info->set_pthread(__pthread_" + i + ");\n");
            p.print("    info->set_active(true);\n");
            p.print("  }\n");
        }

        p.print("  pthread_t id;\n");
        p.print("  pthread_create(&id, NULL, run_join, NULL);\n");

        p.print("}\n");

        p.newLine();

        p.print("int main(int argc, char **argv) {\n");

        p.print("  thread_info *t_info;\n");

        for (int i = 0; i < threadNumber; i++) {
            if (isEliminated(i)) {continue;}
            p.print("  t_info = __get_thread_info_"+i+"();\n"); 
            p.print("  thread_list.push_back(t_info);\n");  
        }

        p.print("\n  myip = get_myip();\n");

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
        p.println("#ifdef VERBOSE");
        p.print("       fprintf(stderr,\"Initial Iteration: %d\\n\", tmp);\n"); 
        p.println("#endif");
        p.print("       __init_iter = tmp;"); 
        p.print("       init_instance::set_start_iter(__init_iter);"); 
        p.print("    }\n");

        p.print("    if (argc > a + 1 && strcmp(argv[a], \"-i\") == 0) {\n"); 
        p.print("       int tmp;\n");
        p.print("       sscanf(argv[a + 1], \"%d\", &tmp);\n");
        p.println("#ifdef VERBOSE");
        p.print("       fprintf(stderr,\"Number of Iterations: %d\\n\", tmp);\n"); 
        p.println("#endif");
        p.print("       __max_iteration = tmp;"); 
        p.print("    }\n");

        p.print("    if (strcmp(argv[a], \"-t\") == 0) {\n"); 
        p.println("#ifdef VERBOSE");
        p.print("       fprintf(stderr,\"Timer enabled.\\n\");\n"); 
        p.println("#endif");
        p.print("       __timer_enabled = 1;"); 
        p.print("    }\n");

        p.print("    if (argc > a + 1 && strcmp(argv[a], \"-ccp\") == 0) {\n");
        p.println("#ifdef VERBOSE");
        p.print("       fprintf(stderr,\"CCP address: %s\\n\", argv[a + 1]);\n"); 
        p.println("#endif");
        p.print("       __ccp_ip = lookup_ip(argv[a + 1]);\n");
        p.print("    }\n");

        p.print("    if (strcmp(argv[a], \"-runccp\") == 0) {\n");
        p.print("      int tmp = 1;\n");
        p.print("      if (argc > a + 1 && argv[a+1][0] >= '1' &&  argv[a+1][0] <= '9' ) {\n");
        p.print("        sscanf(argv[a + 1], \"%d\", &tmp);\n");
        p.print("      }\n");
        p.println("#ifdef VERBOSE");
        p.print("      fprintf(stderr,\"RUNCCP number of init nodes is: %d\\n\", tmp);\n");
        p.println("#endif");
        p.print("      ccp c(thread_list, tmp);\n");
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

}

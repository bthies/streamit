package at.dms.kjc.cluster;

import java.io.FileWriter;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.sir.SIRHelper;




/**
 * Generate Makefile.cluster for cluster back end.
 * 
 * <p>Output program is "a.out" unless --output switch provides name.</p>
 *
 * @author Janis
 *
 */
public class GenerateMakefile {
   /**
    *  Generate Makefile.cluster for cluster back end.
    *  
    *  <p>Gets number of threads for a NodeEnumerator.
    *  Gets helper functions from parameter: if a helper function is
    *  native, assumes that there is already a .o file for it.
    *  </p>
    * @param helpers Helper functions
    */
    public static void generateMakefile(SIRHelper[] helpers) {

        int threadNumber = NodeEnumerator.getNumberOfNodes();

        CodegenPrintWriter p = new CodegenPrintWriter();
        String executablename = KjcOptions.output == null? "a.out": KjcOptions.output;
        
        p.newLine();
        p.print("LIB_CLUSTER = $(STREAMIT_HOME)/library/cluster\n");

        p.newLine();    
        p.print("CC = gcc34 #gcc34\n"); // gcc34
//        p.print("CC_IA64 = ecc\n");
//        p.print("CC_ARM = /u/janiss/bin/arm343 #arm-linux-gcc\n");

        p.newLine();
        p.print("CCFLAGS = -O3\n");
//        p.print("CCFLAGS_IA64 = -O3\n");
//        p.print("CCFLAGS_ARM = -O3\n");

        p.newLine();
        p.print("NAMES = ");
    
        {
            int i;
            for (i = 0; i < threadNumber - 1; i++) {
                p.print("\tthread"+i+" \\");
                p.newLine();
            }
            p.print("\tthread"+i);
            p.newLine();
        }

        p.newLine();

        p.print("SOURCES = \t$(NAMES:%=%.cpp)\n");
        p.print("OBJS = \t$(NAMES:%=%.o)\n");
//        p.print("OBJS_IA64 = \t$(NAMES:%=%_ia64.o)\n");
//        p.print("OBJS_ARM = \t$(NAMES:%=%_arm.o)\n");

        p.newLine();

        
        p.println("all: " + executablename);
        p.newLine();

//        if (KjcOptions.standalone) {
//            p.print("ia64: fusion_ia64\n");
//        } else {
//            p.print("ia64: run_cluster_ia64\n");
//        }
//
//        p.newLine();
//    
//        p.print("arm: fusion_arm\n");
//         
//        p.newLine();
//
        p.print("clean:\n");
        p.println("\trm -f master*.o fusion*.o thread*.o " + executablename);
        p.newLine();
        p.newLine();

        // =============== run_cluster

        if (KjcOptions.standalone) {
            // =============== fusion

            p.print(executablename + ": fusion.o ");
        } else {
            p.print(executablename + ": master.o global.o ");
        }
            for (int y = 0; y < helpers.length; y++) {
            if (helpers[y].isNative()) {
                p.print(helpers[y].getIdent() + ".o ");
            }
        }
        p.print("$(OBJS)\n");
        p.print("\t$(CC) $(CCFLAGS) -o $@ $^ -L$(LIB_CLUSTER) -lpthread -lcluster -lstdc++\n");
        p.newLine();
        
        // =============== %.o : %.cpp
    
        p.print("%.o: %.cpp fusion.h cluster.h global.h\n");
        p.print("\t$(CC) $(CCFLAGS) -I$(LIB_CLUSTER) -c -o $@ $<\n");
        p.newLine();

        p.newLine();

//        // =============== run_cluster_ia64
//
//        p.print("run_cluster_ia64: master_ia64.o $(OBJS_IA64)\n");
//        p.print("\t$(CC_IA64) $(CCFLAGS_IA64) -o $@ $^ -L$(LIB_CLUSTER) -lpthread -lcluster_ia64\n");
//        p.newLine();
//
//        // =============== fusion_ia64
//
//        p.print("fusion_ia64: fusion_ia64.o $(OBJS_IA64)\n");
//        p.print("\t$(CC_IA64) $(CCFLAGS_IA64) -o $@ $^ -L$(LIB_CLUSTER) -lpthread -lcluster_ia64\n");
//
//        p.newLine();
//
//        // =============== %_ia64.o : %.cpp
//
//        p.print("%_ia64.o: %.cpp fusion.h cluster.h\n");
//        p.print("\t$(CC_IA64) $(CCFLAGS_IA64) -I$(LIB_CLUSTER) -c -o $@ $<\n");
//        p.newLine();
//
//
//
//
//        // =============== fusion_arm
//
//        p.print("fusion_arm: fusion_arm.o $(OBJS_ARM)\n");
//        p.print("\tar r objects_arm.a $^\n");
//        p.print("\tranlib objects_arm.a\n");
//        p.print("\t$(CC_ARM) $(CCFLAGS_ARM) -o $@ objects_arm.a -L$(LIB_CLUSTER) -lstdc++ -lm -lcluster_arm #-lpthread\n");
//
//        p.newLine();
//
//        // =============== %_arm.o : %.cpp
//
//        p.print("%_arm.o: %.cpp fusion.h cluster.h\n");
//        p.print("\t$(CC_ARM) $(CCFLAGS_ARM) -I$(LIB_CLUSTER) -c -o $@ $<\n");
//        p.newLine();
//


        try {
            FileWriter fw = new FileWriter("Makefile.cluster");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write Makefile");
        }   
    }
}

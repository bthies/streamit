// $Id
package at.dms.kjc.vanillaSlice;

//import java.util.*;
//import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.backendSupport.*;
//import at.dms.kjc.slicegraph.*;
import at.dms.kjc.common.*;

/**
 * Takes a ComputeNode collection, a collection of Channel's, 
 * and a mapping from Channel x end -> ComputeNode and emits code for the ComputeNode. 
 * Most work in superclass.
 * 
 * @author dimock
 *
 */
public class EmitStandaloneCode extends EmitCode {

    
    /**
     * Constructor.
     * @param backendbits indicates BackEndFactory containing all useful info.
     */
    public EmitStandaloneCode(
            BackEndFactory<UniProcessors, UniProcessor, UniComputeCodeStore, Integer> backendbits) {
        super(backendbits);
    }
    
    /**
     * Create typedefs and other general header info.
     * @param structs       Structures detected by front end.
     * @param backendbits   BackEndFactory to get access to rest of code.
     * @param p             a CodeGenPrintWriter on which to emit the C code.
     */
    static public void emitTypedefs(SIRStructure[] structs, BackEndFactory backendbits, CodegenPrintWriter p) {
        p.println("#ifndef __STRUCTS_H\n");
        p.println("#define __STRUCTS_H\n");
        EmitTypedefs.emitTypedefs(structs,backendbits,p);
        p.println("typedef int bit;\n");
        p.println("#ifndef round");
        p.println("#define round(x) (floor((x)+0.5))");
        p.println("#endif\n");

        p.println("#endif // __STRUCTS_H\n");
    }
    
    /**
     * Standard code for front of a C file here.
     * 
     */
    public void generateCHeader(CodegenPrintWriter p) {
        p.println("#include <math.h>");     // in case math functions
        p.println("#include <stdio.h>");    // in case of FileReader / FileWriter
        p.println("#include \"structs.h\"");
        p.newLine();
        p.println("int " + UniBackEndFactory.iterationBound + ";");
        p.newLine();
    }

    /**
     * Generate a "main" function.
     * Override!
     */
    public void generateMain(CodegenPrintWriter p) {
        p.println();
        p.println();
        p.println("// main() Function Here");
        p.println(
"/* helper routines to parse command line arguments */\n"+
"#include <unistd.h>\n" +
"\n"+
"/* retrieve iteration count for top level driver */\n"+
"static int __getIterationCounter(int argc, char** argv) {\n"+
"    int flag;\n"+
"    while ((flag = getopt(argc, argv, \"i:\")) != -1)\n"+
"       if (flag == 'i') return atoi(optarg);\n"+
"    return -1; /* default iteration count (run indefinitely) */\n"+
"}"+
"\n");
        p.println("int main(int argc, char** argv) {");
        p.indent();
        p.println(UniBackEndFactory.iterationBound + "   = __getIterationCounter(argc, argv);\n");
        p.println(
                backendbits.getComputeNodes().getNthComputeNode(0).getComputeCode().getMainFunction().getName()
                + "();");
        p.println("return 0;");
        p.outdent();
        p.println("}");
    }
}

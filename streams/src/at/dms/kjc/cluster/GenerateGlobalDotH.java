// $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/src/at/dms/kjc/cluster/GenerateGlobalDotH.java,v 1.3 2009-05-22 19:15:44 ctan Exp $
package at.dms.kjc.cluster;

import java.io.FileWriter;

import at.dms.kjc.CType;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFloatLiteral;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.sir.SIRGlobal;
import at.dms.kjc.sir.SIRHelper;



/**
 * Generate "global.h" for cluster back end.
 * 
 * @author Janis
 *
 */
public class GenerateGlobalDotH {

    /**
     * Generate "global.h" and "global.cpp" for cluster back end.
     * 
     * <p>Forward references for globals, and code for helper functions</p>
     * 
     * @param global  A static section if any
     * @param helpers Helper functions
     */
    public static void generateGlobalDotH(SIRGlobal global, SIRHelper[] helpers) {

        String str = new String();
        JFieldDeclaration fields[];

        if (global == null) {
            fields = new JFieldDeclaration[0];
        } else {
            fields = global.getFields();
        }

        // ================================
        // Writing global.h
        // ================================

        str += "#ifndef __GLOBAL_H\n";
        str += "#define __GLOBAL_H\n\n";
        str += "#include <math.h>\n";
        str += "#include \"structs.h\"\n";
        str += "#include <StreamItVectorLib.h>\n";
	if(KjcOptions.numbers > 0)
	    str += "#include <stdint.h>\n";
        str += "\n";

        str += "#define max(A,B) (((A)>(B))?(A):(B))\n";
        str += "#define min(A,B) (((A)<(B))?(A):(B))\n";

        str += "\n";
    
        for (int i = 0; i < helpers.length; i++) {
            JMethodDeclaration[] m = helpers[i].getMethods();
            for (int j = 0; j < m.length; j++) {
                FlatIRToCluster f2c = new FlatIRToCluster();
                f2c.helper_package = helpers[i].getIdent();
                f2c.setDeclOnly(true);
                m[j].accept(f2c);
                str += "extern "+f2c.getPrinter().getString()+"\n";
            }
        }

        str += "extern void __global__init();\n";
        str += "\n";

        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();

            if (type.toString().endsWith("Portal")) continue;

            String ident = fields[f].getVariable().getIdent();
            str += "extern " + ClusterUtils.declToString(type, "__global__" + ident) + ";\n";
        }

	if(KjcOptions.numbers > 0) {
	    str += "static __inline__ uint64_t rdtsc(void) {\n";
	    str += "  uint32_t hi, lo;\n";
	    str += "  asm volatile (\"rdtsc\" : \"=a\"(lo), \"=d\"(hi));\n";
	    str += "  return ((uint64_t ) lo) | (((uint64_t) hi) << 32);\n";
	    str += "}\n";
	    str += "\n";
	}

        str += "#endif // __GLOBAL_H\n";
        
        try {
            FileWriter fw = new FileWriter("global.h");
            fw.write(str.toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write <global.h>");
            System.exit(1);
        }
    }
}

// $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/src/at/dms/kjc/cluster/GenerateGlobalDotCpp.java,v 1.1 2006-07-07 17:40:01 dimock Exp $
package at.dms.kjc.cluster;

import java.io.FileWriter;

import at.dms.kjc.CType;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFloatLiteral;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.sir.SIRGlobal;
import at.dms.kjc.sir.SIRHelper;



/**
 * Generate "global.cpp" for cluster back end.
 * 
 * @author Janis
 *
 */
public class GenerateGlobalDotCpp {

    /**
     * Generate "global.h" and "global.cpp" for cluster back end.
     * 
     * <p>Forward references for globals, and code for helper functions</p>
     * 
     * @param global  A static section if any
     * @param helpers Helper functions
     */
    public static void generateGlobalDotCpp(SIRGlobal global, SIRHelper[] helpers) {

        String str = new String();
        JFieldDeclaration fields[];

        if (global == null) {
            fields = new JFieldDeclaration[0];
        } else {
            fields = global.getFields();
        }

        // ================================
        // Writing global.cpp
        // ================================

        str += "#include <stdlib.h>\n";
        str += "#include <unistd.h>\n";
        str += "#include <math.h>\n";
        str += "#include \"global.h\"\n";
        str += "\n";

        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();

            if (type.toString().endsWith("Portal")) continue;

            JExpression init_val = fields[f].getVariable().getValue();
            String ident = fields[f].getVariable().getIdent();

            str += ClusterUtils.declToString(type, " __global__"+ident);

            if (init_val == null) {
                if (type.isOrdinal()) str += (" = 0");
                if (type.isFloatingPoint()) str += (" = 0.0f");
            }

            if (init_val != null && init_val instanceof JIntLiteral) {
                str += (" = "+((JIntLiteral)init_val).intValue());
            }

            if (init_val != null && init_val instanceof JFloatLiteral) {
                str += (" = "+((JFloatLiteral)init_val).floatValue());
            }

            str += (";\n");
        }

        for (int i = 0; i < helpers.length; i++) {
            if (!helpers[i].isNative()) {
                JMethodDeclaration[] m = helpers[i].getMethods();
                for (int j = 0; j < m.length; j++) {
                    FlatIRToCluster f2c = new FlatIRToCluster();
                    f2c.helper_package = helpers[i].getIdent();
                    f2c.setDeclOnly(false);
                    m[j].accept(f2c);
                    str += f2c.getPrinter().getString()+"\n";
                }
            }
        }

        if (global == null) {
            str += "void __global__init() { }";
        } else {
            FlatIRToCluster f2c = new FlatIRToCluster();
            f2c.setGlobal(true);
            f2c.setDeclOnly(false);
            global.getInit().accept(f2c);
            str += f2c.getPrinter().getString();
        }
        str += "\n";
    
        try {
            FileWriter fw = new FileWriter("global.cpp");
            fw.write(str.toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write <global.cpp>");
        }
    }

}

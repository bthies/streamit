// $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/src/at/dms/kjc/cluster/GenerateClusterDotH.java,v 1.2 2006-07-07 20:31:24 dimock Exp $
package at.dms.kjc.cluster;

import java.io.FileWriter;

import at.dms.kjc.common.CodegenPrintWriter;

/**
 * Generate file cluster.h
 * 
 * <p>Probably legacy code since the generated file
 * contains a single line of code and that is commented out.</p>
 * 
 * @author Janis
 *
 */

public class GenerateClusterDotH {

    /**
     * Generate file cluster.h
     *
     */
    public static void generateClusterDotH() {

        CodegenPrintWriter p = new CodegenPrintWriter();

        p.newLine();
        p.newLine();

        p.print("//#define __CHECKPOINT_FREQ 10000");
        p.newLine();
        p.newLine();

        try {
            FileWriter fw = new FileWriter("cluster.h");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write cluster.h");
        }   
    }


}

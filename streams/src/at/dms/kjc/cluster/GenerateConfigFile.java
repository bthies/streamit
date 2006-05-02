package at.dms.kjc.cluster;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import at.dms.kjc.common.CodegenPrintWriter;


/**
 * Generate cluster-config.txt for cluster back end.
 * 
 * <p>This file should contain a mapping of threads to machines:
 * If more than one machine then "machine-1" to "machine-n".
 * 
 * It appears to currently be disabled to only handle a single machine, 
 * no matter what number of cluster nodes was passed to the back end.
 * </p>
 * 
 * @author Janis
 *
 */
public class GenerateConfigFile {

    /**
     * Generate cluster-config.txt
     */
    public static void generateConfigFile() {

        int threadNumber = NodeEnumerator.getNumberOfNodes();

        CodegenPrintWriter p = new CodegenPrintWriter();

        String currentHostName = getCurrentHostName();

        /*
          String me = new String();

          try {

          Runtime run = Runtime.getRuntime();
          Process proc = run.exec("uname -n");
          proc.waitFor();
        
          InputStream in = proc.getInputStream();
        
          int len = in.available() - 1;
        
          for (int i = 0; i < len; i++) {
          me += (char)in.read();
          }
          } catch (Exception ex) {

          ex.printStackTrace();
          }
        */

        for (int i = 0; i < threadNumber; i++) {

	    //p.print(i+" "+"machine-"+ClusterFusion.getPartition(NodeEnumerator.getFlatNode(i))+"\n");            
	    
	    p.print(i+" "+currentHostName+"\n");
        }

        try {
            FileWriter fw = new FileWriter("cluster-config.txt");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write cluster configuration file");
        }   
    }

    /**
     * Returns the name of the machine this is currently running on.
     */
    private static String getCurrentHostName() {
        String result = "unknown";
        try {
            Process proc = Runtime.getRuntime().exec("hostname");
            proc.waitFor();
            BufferedReader output = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            result = output.readLine();
        } catch (IOException e) {
            System.err.println("Warning: could not determine current host name for cluster-config file (IOException).");
        } catch (InterruptedException e) {
            System.err.println("Warning: could not determine current host name for cluster-config file (InterruptedException).");
        }
        return result;
    }
}

package at.dms.kjc.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.util.Utils;

/**
 * Generate cluster-config.txt for cluster back end.
 * 
 * <p>This file contains a mapping of threads to machines.  It checks
 * for a file ($STREAMIT_HOME/cluster-machines.txt) listing the
 * available cluster machines; if it finds one, it maps threads to
 * those machines in the order found in the file.  If not, it maps
 * everyone to the current host (useful for testing or shared-memory
 * implementation.)
 * 
 * @author Janis
 *
 */
public class GenerateConfigFile {
    /**
     * Name of file where machines are listed, one per line.
     */
    private final static String CLUSTER_MACHINES_FILENAME = "cluster-machines.txt";
    /**
     * Name of file where thread/machine mapping is output.
     */
    private final static String CLUSTER_CONFIG_FILENAME = "cluster-config.txt";

    /**
     * Generate cluster-config.txt
     */
    public static void generateConfigFile() {
        int threadNumber = NodeEnumerator.getNumberOfNodes();
        CodegenPrintWriter p = new CodegenPrintWriter();

        String[] clusterMachines = readClusterMachines();
        if (clusterMachines.length == 0) {
            mapToCurrentHost(clusterMachines, p, threadNumber);
        } else if (clusterMachines.length < threadNumber) {
            mapMultipleThreadsPerMachine(clusterMachines, p, threadNumber);
        } else {
            mapOneThreadPerMachine(clusterMachines, p, threadNumber);
        }

        try {
            FileWriter fw = new FileWriter(CLUSTER_CONFIG_FILENAME);
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write cluster configuration file");
        }   
    }

    /**
     * No machines specified, map all threads to current host.
     */
    private static void mapToCurrentHost(String[] clusterMachines, CodegenPrintWriter p, int threadNumber) {
        String currentHostName = getCurrentHostName();
        for (int i = 0; i < threadNumber; i++) {
            p.print(i+" "+currentHostName+"\n");
        }
        // print warning    
        System.err.println("NOTE: Missing or empty $STREAMIT_HOME/cluster-machines.txt file,");
        System.err.println(" so all threads assigned to " + currentHostName + " in cluster-config.txt.");
    }

    /**
     * We have more threads than machines, so distribute "evenly" across machines.
     * For example, if 7 threads and 3 machines, then machines get (3 2 2) threads.
     */
    private static void mapMultipleThreadsPerMachine(String[] clusterMachines, CodegenPrintWriter p, int threadNumber) {
        int curThread = 0;
        // for each machine...
        for (int i=0; i<clusterMachines.length; i++) {
            int threadsRemaining = threadNumber - curThread;
            int machinesRemaining = clusterMachines.length - i;
            int threadsPerMachine = (int)Math.ceil(((float)threadsRemaining) / ((float)machinesRemaining));
            // assign current machine
            for (int j=0; j<threadsPerMachine; j++, curThread++) {
                p.print(curThread+" "+clusterMachines[i]+"\n");
            }
        }
        // print warning
        int maxPerMachine = (int)Math.ceil(((float)threadNumber) / ((float)clusterMachines.length));
        System.err.println("Reading machine names from $STREAMIT_HOME/cluster-machines.txt.");
        System.err.println("  WARNING: More threads than machines, assigning up to " + maxPerMachine + " threads");
        System.err.println("  per machine.  To adjust mapping, edit ./cluster-config.txt.");
    }

    private static void mapOneThreadPerMachine(String[] clusterMachines, CodegenPrintWriter p, int threadNumber) {
        // we have enough machines for the threads
        for (int i=0; i<threadNumber; i++) {
            p.print(i+" "+clusterMachines[i]+"\n");
        }
        System.err.println("Reading machine names from $STREAMIT_HOME/cluster-machines.txt.");
        System.err.println("  Storing thread/machine mapping in ./cluster-config.txt.");
    }

    /**
     * Returns an array of machine names that are defined in
     * $STREAMIT_HOME/cluster-machines.txt.  If this file is not found
     * or if the file is empty, returns 0-length array.
     */
    private static String[] readClusterMachines() {
        File file = new File(Utils.getEnvironmentVariable("STREAMIT_HOME") + File.separatorChar + CLUSTER_MACHINES_FILENAME);
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                ArrayList list = new ArrayList();
                String line;
                while ((line = reader.readLine()) != null) {
                    list.add(line);
                }
                return (String[])list.toArray(new String[0]);
            } catch (IOException e) {
                return new String[0];
            }
        } else {
            return new String[0];
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

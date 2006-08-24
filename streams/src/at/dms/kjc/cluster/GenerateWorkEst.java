package at.dms.kjc.cluster;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * Generates work-estimate.txt file that contains work estimates for
 * all threads during a single steady state cycle
 *
 */

public class GenerateWorkEst {

    /**
     * Generate the work-estimate.txt file
     */
    public static void generateWorkEst() {

        int threadNumber = NodeEnumerator.getNumberOfNodes();
        CodegenPrintWriter p = new CodegenPrintWriter();

        for (int i = 0; i < threadNumber; i++) {

	    SIROperator oper = NodeEnumerator.getOperator(i);	   
	    FlatNode node = NodeEnumerator.getFlatNode(i);	    

        // case found where joiner with only 0 weights caused NullPointerException
        int steady_counts = 0;
        try {
            steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();
        } catch (NullPointerException e) {}
        
	    int w = 0;

	    if (oper instanceof SIRFilter) {
		WorkEstimate w_est = WorkEstimate.getWorkEstimate((SIRFilter)oper);
		WorkList w_list = w_est.getSortedFilterWork();
		w = w_list.getWork(0);
	    }

	    // For splitters and joiners the estimate is 8 times the sum of weights
	   
 	    if (oper instanceof SIRJoiner) {
		SIRJoiner j = (SIRJoiner)oper;
		w = j.getSumOfWeights() * 8;
	    }

	    if (oper instanceof SIRSplitter) {
		SIRSplitter s = (SIRSplitter)oper;
		w = s.getSumOfWeights() * 8;
	    }

	    // If work estimate is 0 (for Identities) then set it to the smallest
	    // positive amount 1

	    if (w == 0) w = 1;

	    p.print(i+" " + (w * steady_counts) + "\n");

        }

        try {
            FileWriter fw = new FileWriter("work-estimate.txt");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write work estimation file");
        }   
    }

    /**
     * Delete the work-estimate.txt file
     */
    public static void clearWorkEst() {
        try {
            File f = new File("work-estimate.txt");
            if (f.exists()) {
                f.delete();
            }
        }
        catch (Exception e) {
            System.err.println("Unable to delete work-estimate.txt");
        }   
    }

}

package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.linprog.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.raw.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;

public class PartitionUtil {

    /**
     * Prints work summaries to the screen.
     */
    public static void printTileWork(LinkedList partitions, int numTiles) {
	int maxWork = getMaxWork(partitions);
	double totalUtil = getTotalUtilization(partitions, numTiles);
	for (int i=0; i<partitions.size(); i++) {
	    PartitionRecord pr = (PartitionRecord)partitions.get(i);
	    double util = ((double)pr.getWork()) / ((double)maxWork);
	    System.out.println("partition " + i + " has work:\t" + pr.getWork() 
			       + "\t Estimated utilization:\t" + Utils.asPercent(util));
	}
	System.out.println("Estimated total utilization: " + Utils.asPercent(totalUtil));
    }


    /**
     * Gets max work out of <partitions>.
     */
    public static int getMaxWork(LinkedList partitions) {
	int maxWork = -1;
	for (int tile=0; tile<partitions.size(); tile++) {
	    PartitionRecord pr = (PartitionRecord)partitions.get(tile);
	    if (pr.getWork()>maxWork) {
		maxWork = pr.getWork();
	    }
	}
	return maxWork;
    }

    /**
     * Estimates total utilization (as fraction, e.g. 0.5023) for
     * <partitions> running on <numTiles>.  (Not just running on the
     * number of occupied tiles!  That is, empty tiles hurt the
     * utilization.)
     */
    private static double getTotalUtilization(LinkedList partitions, int numTiles) {
	double totalUtil = 0;
	int maxWork = getMaxWork(partitions);
	for (int i=0; i<partitions.size(); i++) {
	    PartitionRecord pr = (PartitionRecord)partitions.get(i);
	    double util = ((double)pr.getWork()) / ((double)maxWork);
	    totalUtil += util / ((double)numTiles);
	}
	return totalUtil;
    }

    /**
     * The following functions are for saving data to disk.
     */
    static private PrintStream out;
    public static void setupScalingStatistics() {
	try {
	    out = new PrintStream(new FileOutputStream("dp_scaling.txt"));	
	    out.println("Number of tiles" + "\t" + 
			"Number of tiles used" + "\t" + 
			"maxWork" + "\t" + 
			"Utilization");
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
    }

    public static void doScalingStatistics(LinkedList partitions, int numTiles) {
	out.println(numTiles + "\t" + 
		    partitions.size() + "\t" + 
		    getMaxWork(partitions) + "\t" + 
		    getTotalUtilization(partitions, numTiles));
    }

    public static void stopScalingStatistics() {
	out.close();
    }

}

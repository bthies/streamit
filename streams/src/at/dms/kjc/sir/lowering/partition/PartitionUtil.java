package at.dms.kjc.sir.lowering.partition;

import java.util.*;
import java.io.*;
import lpsolve.*;

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
    /** output stream to send data to. **/
    static PrintStream outputStream = System.out;
    /** gets the current output stream. **/
    public static PrintStream getOutputStream() {return outputStream;}
    /** sets the current output stream. **/
    public static void setOutputStream(PrintStream newStream) {outputStream=newStream;}
    

    /**
     * Prints a listing of the work for each tile, given that
     * <partitions> is a mapping from every stream structure in <str>
     * to an integer partition number, from -1...(numTiles-1).  If a
     * stream structure is entirely contained on a given tile, then it
     * has that tile number.  If it is split across multiple tiles,
     * then it has a target of -1.
     */
    public static void printTileWork(HashMap partitions, WorkEstimate work, int numTiles) {

	int[] tileWork = new int[numTiles];

	String[] tileContents = new String[numTiles];
	for (int i=0; i<numTiles; i++) {
	    tileContents[i] = "";
	}

	int maxWork = -1;
	for (Iterator it = partitions.keySet().iterator(); it.hasNext(); ) {
	    Object node = it.next();
	    int tile = ((Integer)partitions.get(node)).intValue();
	    if (tile == -1) { continue; }
	    tileContents[tile] += "  " + ((SIROperator)node).getName() + "\n";
	    if (node instanceof SIRFilter) {
		// for filter, add work estimate of filter
		tileWork[tile] += work.getWork((SIRFilter)node);
	    } else if (node instanceof SIRJoiner) {
		// for joiners, add JOINER_WORK_ESTIMATE
		tileWork[tile] += ILPPartitioner.JOINER_WORK_ESTIMATE;
	    }
	    // keep track of max work
	    if (tileWork[tile]>maxWork) {
		maxWork = tileWork[tile];
	    }
	}

	// print each tile's work
	double totalUtil = 0;
	for (int i=0; i<tileWork.length; i++) {
	    double util = ((double)tileWork[i]) / ((double)maxWork);
	    totalUtil += util / ((double)tileWork.length);
	    outputStream.println("tile " + i + " has work:\t" + tileWork[i] 
			       + "\t Estimated utilization:\t" + Utils.asPercent(util));
	    //System.out.print(tileContents[i]);
	}

	outputStream.println("Estimated total utilization: " + Utils.asPercent(totalUtil));
    }
}

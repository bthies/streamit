package at.dms.kjc.sir.lowering.partition;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;
import java.io.*;

/**
 * A wrapper for a linked list to save ourself a lot of casting with
 * work entries.
 */
class WorkList extends java.util.LinkedList {

    public WorkList(Collection c) {
	super(c);
    }

    /**
     * Gets total work at position <i>.
     */
    public int getWork(int i) {
	return ((WorkInfo)((Map.Entry)super.get(i)).getValue()).getTotalWork();
    }

    /**
     * Gets filter at position <i>.
     */
    public SIRFilter getFilter(int i) {
	return (SIRFilter)((Map.Entry)super.get(i)).getKey();
    }

    /**
     * Gets container at position <i>.
     */
    public SIRContainer getContainer(int i) {
	return (SIRContainer)((Map.Entry)super.get(i)).getKey();
    }

    /**
     * Write the contents of this to filename <filename>.
     */
    public void writeToFile(String filename) {
	PrintStream out = null;
	try {
	    out = new PrintStream(new FileOutputStream(filename));
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
	// first get max-length string so we justify the work column
	int max = 0;
	for (int i=size()-1; i>=0; i--) {
	    SIRStream str = (SIRStream)((Map.Entry)super.get(i)).getKey();
	    if (max<str.getIdent().length()) {
		max = str.getIdent().length();
	    }
	}
	// then print, justified
	String title1 = "Filter";
	out.print(title1);
	for (int i=title1.length(); i<max; i++) {
	    out.print(" ");
	}
	out.print("\t" + "Reps" + "\t" + "Estimated Work" + "\t" + "Total Estimated Work");
	if (KjcOptions.simulatework) {
	    out.println("\t" + "Measured Work" + "\t" + "(Measured-Estimated)/Measured" + "\t" + "Total Measured Work");
	} else {
	    out.println();
	}
	for (int i=size()-1; i>=0; i--) {
	    SIRStream str = (SIRStream)((Map.Entry)super.get(i)).getKey();
	    WorkInfo workInfo = (WorkInfo)((Map.Entry)super.get(i)).getValue();
	    out.print(str.getIdent());
	    for (int j=str.getIdent().length(); j<max; j++) {
		out.print(" ");
	    }
	    out.print("\t" + workInfo.getReps() + "\t" + workInfo.getInexactUnitWork() + "\t" + (workInfo.getReps()*workInfo.getInexactUnitWork()));
	    if (KjcOptions.simulatework) {
		out.println("\t" + workInfo.getUnitWork() + "\t" + (((float)workInfo.getUnitWork()-(float)workInfo.getInexactUnitWork())/(float)workInfo.getUnitWork()) + 
			    "\t" + workInfo.getTotalWork());
	    } else {
		out.println();
	    }
	}
	out.close();
    }
}

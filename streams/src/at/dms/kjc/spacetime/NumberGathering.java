package at.dms.kjc.spacetime;

import java.util.Vector;

public class NumberGathering 
{
    public static void doit(RawChip chip, Trace[] files) 
    {
	Trace[] fileWriters;
	//holds the number of items each fw writes in 
	//each stage
	int[] steady;
	int[] init;
	int[] primepump;
	//get all the file writers
	Vector fw = new Vector();
	for (int i = 0; i < files.length; i++) 
	    if (files[i].getHead().isFileWriter())
		fw.add(files[i]);

	fileWriters = (Trace[])fw.toArray(new Trace[0]);
	assert fileWriters.length > 0 : "Error in number gathering: no file writer";
	steady = new int[fileWriters.length];
	init = new int[fileWriters.length];
	primepump = new int[fileWriters.length];
	for (int i = 0; i < fileWriters.length; i++) {
	    FilterTraceNode node = (FilterTraceNode)fileWriters[i].getHead().getNext();
	    FilterInfo fi = FilterInfo.getFilterInfo(node);
	    assert node.getFilter().getInputType().isNumeric() :
		"non-numeric type for input to filewriter";
	    
	    steady[i] = fi.totalItemsReceived(false, false);
	    primepump[i] = fi.totalItemsReceived(false, true);
	    init[i] = fi.totalItemsReceived(true, false);
	}
    }    
}

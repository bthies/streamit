package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.kjc.flatgraph2.*;


public class NumberGathering 
{
    public Trace[] fileWriters;
    //holds the number of items each fw writes in 
    //each stage
    public int[] steady;
    public int[] skip;
    
    public static NumberGathering doit(RawChip chip, Trace[] files) 
    {
	return (new NumberGathering(chip, files));
    }
    
    private NumberGathering(RawChip chip, Trace[] files)
    {
	//get all the file writers
	Vector fw = new Vector();
	for (int i = 0; i < files.length; i++) 
	    if (files[i].getHead().isFileWriter())
		fw.add(files[i]);

	fileWriters = (Trace[])fw.toArray(new Trace[0]);
	assert fileWriters.length > 0 : "Error in number gathering: no file writer";
	steady = new int[fileWriters.length];
	skip = new int[fileWriters.length];
	//assign all the arrays 
	for (int i = 0; i < fileWriters.length; i++) {
	    FilterTraceNode node = (FilterTraceNode)fileWriters[i].getHead().getNext();
	    FilterInfo fi = FilterInfo.getFilterInfo(node);
	    assert node.getFilter().getInputType().isNumeric() :
		"non-numeric type for input to filewriter";
	    
	    steady[i] = fi.totalItemsReceived(false, false);
	    skip[i] = fi.totalItemsReceived(true, false) + 
		fi.totalItemsReceived(false, true);
	}
    }    

    public int getID(FileOutputContent foc) 
    {
	for (int i = 0; i < fileWriters.length; i++) {
	    if (foc == ((FilterTraceNode)fileWriters[i].getHead().getNext()).getFilter())
		return i;
	}
	assert false : "FileOutputContent not found";
	return -1;
    }
    
}

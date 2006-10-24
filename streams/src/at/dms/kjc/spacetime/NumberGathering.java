package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.slicegraph.FilterTraceNode;


/**
 * This class stores statistics needed to generate automatic performance 
 * statistics on the raw simulator.  For each file writer, this class calculates
 * how many items will be written for the init and for each steady-state.
 * 
 * @author mgordon
 *
 */
public class NumberGathering 
{
    /** the file writers of the application */
    public Trace[] fileWriters;
    /** holds the number of items each fw writes in steady state */ 
    public int[] steady;
    /** holds the number of items each fw writes in init */ 
    public int[] skip;
    /** total number of items written in init stage */
    public int totalSteadyItems;

    /**
     * Collect statistics need for performance code generation on the 
     * raw simulator.
     * 
     * @param chip The raw chip.
     * @param files The file readers and writer traces.
     * 
     * @return The class with the stats.
     */
    public static NumberGathering doit(RawChip chip, Trace[] files) 
    {
        return (new NumberGathering(chip, files));
    }
    
    /**
     * Do the collection of stats.
     * 
     * @param chip
     * @param files
     */
    private NumberGathering(RawChip chip, Trace[] files)
    {
        //get all the file writers
        Vector<Trace> fw = new Vector<Trace>();
        for (int i = 0; i < files.length; i++) 
            if (files[i].getHead().isFileOutput())
                fw.add(files[i]);

        fileWriters = fw.toArray(new Trace[0]);
        assert fileWriters.length > 0 : "Error in number gathering: no file writer";
        steady = new int[fileWriters.length];
        skip = new int[fileWriters.length];
        totalSteadyItems = 0;
        //assign all the arrays 
        for (int i = 0; i < fileWriters.length; i++) {
            FilterTraceNode node = (FilterTraceNode)fileWriters[i].getHead().getNext();
            FilterInfo fi = FilterInfo.getFilterInfo(node);
            assert node.getFilter().getInputType().isNumeric() :
                "non-numeric type for input to filewriter";
        
            steady[i] = fi.totalItemsReceived(false, false);
            totalSteadyItems += steady[i];
            skip[i] = fi.totalItemsReceived(true, false) + 
                fi.totalItemsReceived(false, true);
        }
    }    

    /**
     * Given the file writer, return the unique id that identifies it.
     * 
     * @param foc The file writer.
     * @return The unique id.
     */
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
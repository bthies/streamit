package at.dms.kjc.spacetime;

import java.util.Vector;

import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.*;


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
    public Slice[] fileWriters;
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
    public static NumberGathering doit(RawProcElements chip, Slice[] files) 
    {
        return (new NumberGathering(chip, files));
    }
    
    /**
     * Do the collection of stats.
     * 
     * @param chip
     * @param files
     */
    private NumberGathering(RawProcElements chip, Slice[] files)
    {
        //get all the file writers
        Vector<Slice> fw = new Vector<Slice>();
        for (int i = 0; i < files.length; i++) 
            if (files[i].getHead().isFileOutput())
                fw.add(files[i]);

        fileWriters = fw.toArray(new Slice[0]);
        assert fileWriters.length > 0 : "Error in number gathering: no file writer";
        steady = new int[fileWriters.length];
        skip = new int[fileWriters.length];
        totalSteadyItems = 0;
        //assign all the arrays 
        for (int i = 0; i < fileWriters.length; i++) {
            FilterSliceNode node = (FilterSliceNode)fileWriters[i].getHead().getNext();
            FilterInfo fi = FilterInfo.getFilterInfo(node);
            assert node.getFilter().getInputType().isNumeric() :
                "non-numeric type for input to filewriter";
        
            steady[i] = fi.totalItemsReceived(SchedulingPhase.STEADY);
            totalSteadyItems += steady[i];
            skip[i] = fi.totalItemsReceived(SchedulingPhase.INIT) + 
                fi.totalItemsReceived(SchedulingPhase.PRIMEPUMP);
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
            if (foc == ((FilterSliceNode)fileWriters[i].getHead().getNext()).getFilter())
                return i;
        }
        assert false : "FileOutputContent not found";
        return -1;
    }    
}
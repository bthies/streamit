//$Id: SpaceTimeScheduleAndPartitioner.java,v 1.2 2007-03-14 14:22:06 dimock Exp $
/**
 * Extracts the "schedule" part of Mike's SpaceTimeSchedule.
 * 
 */
package at.dms.kjc.backendSupport;

import java.util.Vector;

import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;

/**
 * Extend BasicSpaceTimeSchedule by storing a partitioner.
 * 
 * BasicSpaceTimeSchedule collects initialization schedule, prime-pump schedule
 * and steady-state schedule in one place.
 * This is purely a data structure: it is operated on by 
 * other classes to generate these schedules.
 * 
 * It is convenient for classes performing layout to keep the partitioner with
 * the schedule.  The partitioner supplies the initial slice graph and includes
 * a map from filters to the amount of work that they perform, which is needed
 * to partition.
 * 
 * @author mgordon (refactored dimock)
 */
public class SpaceTimeScheduleAndPartitioner extends BasicSpaceTimeSchedule {
    /** Partitioner stored with schedule. */
    private Partitioner partitioner;
    
    public SpaceTimeScheduleAndPartitioner(Partitioner partitioner) {
        super();
        this.partitioner = partitioner;
    }
    
    
    /**
     * Return the number of outputs that are written to file writers during the 
     * steady-state.
     *  
     * @return the number of outputs that are written to file writers during the 
     * steady-state.
     */
    public int outputsPerSteady() {
        int outputs = 0;
        
        //get all the file writers
        Vector<Slice> fileWriters = new Vector<Slice>();
        for (int i = 0; i < getPartitioner().io.length; i++) 
            if (getPartitioner().io[i].getHead().isFileOutput())
                fileWriters.add(getPartitioner().io[i]);
        
        for (int i = 0; i < fileWriters.size(); i++) {
            FilterSliceNode node = (FilterSliceNode)fileWriters.get(i).getHead().getNext();
            FilterInfo fi = FilterInfo.getFilterInfo(node);
            assert node.getFilter().getInputType().isNumeric() :
                "non-numeric type for input to filewriter";
        
            outputs += fi.totalItemsReceived(SchedulingPhase.STEADY);
        }
        return outputs;
    }
    

    
    /** 
     * @param partitioner
     */
    public void setPartitioner(Partitioner partitioner) {
        this.partitioner = partitioner;
    }

    /**
     * @return the partitioner associated with this schedule.
     */
    public Partitioner getPartitioner() {
        return partitioner;
    }
}

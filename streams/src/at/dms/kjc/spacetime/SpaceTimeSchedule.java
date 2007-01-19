package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.FilterInfo;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.Slice;

/**
 * This class represents the space/time schedule for the application, 
 * including both the steady state and the initialization stage.
 * It is operated on by other classes to generate these schedules.
 * 
 * See {@link BasicSpaceTimeSchedule} for schedule itself.
 * This class combines the schedule with a rawChip and a Partitioner
 * for easy scheduling on RAW.
 * 
 * @author mgordon
 *
 */
public class SpaceTimeSchedule extends BasicSpaceTimeSchedule {
    //the slice partitioner used
    private Partitioner partitioner;
    //the raw chip that we are compiling to
    private RawChip rawChip;
    
    /**
     * Constructor
     * @param p Partitioner is carried around with schedule.
     * @param r rawChip is carried around with schedule. 
     */
    public SpaceTimeSchedule(Partitioner p, RawChip r) {
        super();
        rawChip = r;
        setPartitioner(p);
    }
     
    /**
     * @return Returns the rawChip.
     */
    public RawChip getRawChip() {
        return rawChip;
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
        
            outputs += fi.totalItemsReceived(false, false);
        }
        return outputs;
    }
    
    /** @return total nodes that we are scheduling for */
    
    public int getTotalNodes() {
        return rawChip.getTotalTiles();
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
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndSlicer;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.SIRSlicer;
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
public class SpaceTimeSchedule extends SpaceTimeScheduleAndSlicer {
    //the raw chip that we are compiling to
    private RawChip rawChip;
    
    protected SIRSlicer sirSlicer;
    
    /**
     * Constructor
     * @param p Partitioner is carried around with schedule.
     * @param r rawChip is carried around with schedule. 
     */
    public SpaceTimeSchedule(SIRSlicer p, RawChip r) {
        super(p);
        sirSlicer = p;
        rawChip = r;
    }
     
    /**
     * @return Returns the rawChip.
     */
    public RawChip getRawChip() {
        return rawChip;
    }
    /** @return total nodes that we are scheduling for */
    
    public int getTotalNodes() {
        return rawChip.getTotalTiles();
    }
    
    public SIRSlicer getSIRSlicer() {
        return sirSlicer;
    }
}
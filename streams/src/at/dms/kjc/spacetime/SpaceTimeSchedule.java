package at.dms.kjc.spacetime;

import java.util.*;

/**
 * This class represents the space/time schedule for the application, 
 * including both the steady state and the initialization stage.
 * It is operated on by other classes the generate these schedules.
 * 
 * @author mgordon
 *
 */
public class SpaceTimeSchedule {
    //the slice partitioner used
    public Partitioner partitioner;
    //a list of the execution order of slices    
    private Trace[] schedule;
    //the raw chip that we are compiling to
    private RawChip rawChip;
    //true if the tile reads from a file
    private boolean[] readsFile;
    //true if the tile writes a file
    private boolean[] writesFile;
    //the initialization schedule
    private Trace[] initSchedule;
    //the preloop schedule!
    private Trace[][] primePumpSchedule; 
    
    public SpaceTimeSchedule(Partitioner p, RawChip r) {
        rawChip = r;
        partitioner = p;
        readsFile = new boolean[rawChip.getTotalTiles()];
        writesFile = new boolean[rawChip.getTotalTiles()];
        for (int i = 0; i < rawChip.getTotalTiles(); i++) {
            readsFile[i] = false;
            writesFile[i] = false;
        }
    }
     
    /**
     * @return Returns the rawChip.
     */
    public RawChip getRawChip() {
        return rawChip;
    }
    
    /**
     * @param initSchedule The initSchedule to set.
     */
    public void setInitSchedule(LinkedList is) {
        this.initSchedule = (Trace[])is.toArray(new Trace[0]);
    }

    /**
     * @return Returns the initSchedule.
     */
    public Trace[] getInitSchedule() {
        return initSchedule;
    }

    /**
     * @return Returns the steady state schedule.
     */
    public Trace[] getSchedule() {
        return schedule;
    }

    /**
     * @param schedule The steady-state schedule to set.
     */
    public void setSchedule(LinkedList schedule) {
        this.schedule = (Trace[])schedule.toArray(new Trace[0]);
    }
    
    
    /**
     * @return Returns the primePumpSchedule.
     */
    public Trace[][] getPrimePumpSchedule() {
        return primePumpSchedule;
    }

    /**
     * @param primePumpSchedule The primePumpSchedule to set.
     */
    public void setPrimePumpSchedule(LinkedList preLoopSchedule) {
        //      convert into an array for easier access...
        primePumpSchedule = new Trace[preLoopSchedule.size()][];
        for (int i = 0; i < preLoopSchedule.size(); i++ ) {
            LinkedList schStep = (LinkedList)preLoopSchedule.get(i);
            primePumpSchedule[i] = new Trace[schStep.size()];
            for (int j = 0; j < schStep.size(); j++) 
                primePumpSchedule[i][j] = (Trace)schStep.get(i);
        }
    }

    public void setWritesFile(int tileNum) {
        assert rawChip.isValidTileNumber(tileNum);
        writesFile[tileNum] = true;
    }
    
    public void setReadsFile(int tileNum) {
        assert rawChip.isValidTileNumber(tileNum);
        readsFile[tileNum] = true;
    }
}

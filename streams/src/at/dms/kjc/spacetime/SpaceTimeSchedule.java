package at.dms.kjc.spacetime;

import java.util.*;

/**
 * This class represents the space/time schedule for the application, 
 * including both the steady state and the initialization stage.
 * It is operated on by other classes to generate these schedules.
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
    //the multiplicities of the traces in the primepump
    //Trace->Integer
    private HashMap primePumpMult;
    
    public SpaceTimeSchedule(Partitioner p, RawChip r) {
        rawChip = r;
        partitioner = p;
        readsFile = new boolean[rawChip.getTotalTiles()];
        writesFile = new boolean[rawChip.getTotalTiles()];
        primePumpMult = new HashMap();
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
     * @param is The initSchedule to set.
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
     * Return a linked list representation of the
     * steady state schedule;
     * 
     * @return a linked list representation of the
     * steady state schedule;
     */
    public LinkedList<Trace> getScheduleList() {
        LinkedList<Trace> list = new LinkedList<Trace>();
        for (int i = 0; i < schedule.length; i++)
            list.add(schedule[i]);
        
        return list;
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
     * @return A flat (one-dimensional) array of the primepump schedule.
     */
    public Trace[] getPrimePumpScheduleFlat() {
        LinkedList pp = new LinkedList();
        
        for (int i = 0; i < primePumpSchedule.length; i++) 
            for (int j = 0; j < primePumpSchedule[i].length; j++) 
                pp.add(primePumpSchedule[i][j]);
        
        
        return (Trace[])pp.toArray(new Trace[0]);
    }
    
    /**
     * @param preLoopSchedule The primePumpSchedule to set.
     */
    public void setPrimePumpSchedule(LinkedList preLoopSchedule) {
        //      convert into an array for easier access...
        System.out.println("Setting primepump schedule:");   
        primePumpSchedule = new Trace[preLoopSchedule.size()][];
        for (int i = 0; i < preLoopSchedule.size(); i++ ) {
            LinkedList schStep = (LinkedList)preLoopSchedule.get(i);
            primePumpSchedule[i] = new Trace[schStep.size()];
            for (int j = 0; j < schStep.size(); j++) {
                Trace current = (Trace)schStep.get(j);
                System.out.println(current);
                primePumpSchedule[i][j] = current;
                //generate the prime pump multiplicity map
                if (!primePumpMult.containsKey(current))
                    primePumpMult.put(current, new Integer(1));
                else 
                    primePumpMult.put(current, 
                            new Integer(((Integer)primePumpMult.get(current)).intValue() + 1));
            }
        }
    }

    /**
     * @param f
     * @return The total number of times this filter fires in the prime pump stage
     * so this accounts for the number number of times that a trace if called in the
     * prime pump stage to fill the rotating buffers.
     */
    public int getPrimePumpTotalMult(FilterInfo f) {
        return getPrimePumpMult(f.traceNode.getParent()) * f.steadyMult;
    }
    
    /** 
     * @param trace
     * @return Return the number of times this trace fires in the prime pump schedule.
     */
    public int getPrimePumpMult(Trace trace) {
        if (!primePumpMult.containsKey(trace))
            return 0;
        return ((Integer)primePumpMult.get(trace)).intValue();
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

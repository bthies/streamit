//$Id$
/**
 * Extracts the "schedule" part of Mike's SpaceTimeSchedule.
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.common.CommonUtils;

/**
 * Collects initialization schedule, prime-pump schedule
 * and steady-state schedule in one place.
 * This is purely a data structure: it is operated on by 
 * other classes to generate these schedules.
 * @author mgordon (refactored dimock)
 */
public class BasicSpaceTimeSchedule {
    //the initialization schedule
    private Slice[] initSchedule;
    //the preloop schedule!
    private Slice[][] primePumpSchedule; 
    //a list of the execution order of slices    
    private Slice[] schedule;
    //the multiplicities of the slices in the primepump
    //Slice->Integer
    private HashMap<Slice, Integer> primePumpMult;
 
    /**
     * Constructor
     */
    public BasicSpaceTimeSchedule() {
        primePumpMult = new HashMap<Slice, Integer>();
    }
    /**
     * @param is The initSchedule to set.
     */
    final public void setInitSchedule(LinkedList<Slice> is) {
        this.initSchedule = (Slice[])is.toArray(new Slice[0]);
    }

    /**
     * @return Returns the initSchedule.
     */
    final public Slice[] getInitSchedule() {
        return initSchedule;
    }

    /**
     * Return a linked list representation of the
     * steady state schedule;
     * 
     * @return a linked list representation of the
     * steady state schedule;
     */
    final public LinkedList<Slice> getScheduleList() {
        LinkedList<Slice> list = new LinkedList<Slice>();
        for (int i = 0; i < schedule.length; i++)
            list.add(schedule[i]);
        
        return list;
    }
    
    /**
     * @return Returns the steady state schedule.
     */
    final public Slice[] getSchedule() {
        return schedule;
    }

    /**
     * @param schedule The steady-state schedule to set.
     */
    final public void setSchedule(LinkedList<Slice> schedule) {
        this.schedule = schedule.toArray(new Slice[0]);
    }
    
    
    /**
     * @return Returns the primePumpSchedule.
     */
    final public Slice[][] getPrimePumpSchedule() {
        return primePumpSchedule;
    }

    /** 
     * @return A flat (one-dimensional) array of the primepump schedule.
     */
    final public Slice[] getPrimePumpScheduleFlat() {
        LinkedList<Slice> pp = new LinkedList<Slice>();
        
        for (int i = 0; i < primePumpSchedule.length; i++) 
            for (int j = 0; j < primePumpSchedule[i].length; j++) 
                pp.add(primePumpSchedule[i][j]);
        
        
        return pp.toArray(new Slice[0]);
    }
    
    /**
     * @param preLoopSchedule The primePumpSchedule to set.
     */
    final public void setPrimePumpSchedule(LinkedList<LinkedList<Slice>> preLoopSchedule) {
        //      convert into an array for easier access...
        CommonUtils.println_debugging("Setting primepump schedule:");   
        primePumpSchedule = new Slice[preLoopSchedule.size()][];
        for (int i = 0; i < preLoopSchedule.size(); i++ ) {
            LinkedList schStep = preLoopSchedule.get(i);
            primePumpSchedule[i] = new Slice[schStep.size()];
            for (int j = 0; j < schStep.size(); j++) {
                Slice current = (Slice)schStep.get(j);
                CommonUtils.println_debugging(current.toString());
                primePumpSchedule[i][j] = current;
                //generate the prime pump multiplicity map
                if (!primePumpMult.containsKey(current))
                    primePumpMult.put(current, new Integer(1));
                else 
                    primePumpMult.put(current, 
                            new Integer(primePumpMult.get(current).intValue() + 1));
            }
        }
    }
 
    /** 
     * @param slice
     * @return Return the number of times this slice fires in the prime pump schedule.
     */
    final public int getPrimePumpMult(Slice slice) {
        if (!primePumpMult.containsKey(slice))
            return 0;
        return primePumpMult.get(slice).intValue();
    }

    /**
     * @param f
     * @return The total number of times this filter fires in the prime pump stage
     * so this accounts for the number number of times that a slice if called in the
     * prime pump stage to fill the rotating buffers.
     */
    final public int getPrimePumpTotalMult(FilterInfo f) {
        return getPrimePumpMult(f.sliceNode.getParent()) * f.steadyMult;
    }

}

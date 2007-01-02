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
 * @author mgordon
 *
 */
public class SpaceTimeSchedule {
    //the slice partitioner used
    public Partitioner partitioner;
    //a list of the execution order of slices    
    private Slice[] schedule;
    //the raw chip that we are compiling to
    private RawChip rawChip;
    //the initialization schedule
    private Slice[] initSchedule;
    //the preloop schedule!
    private Slice[][] primePumpSchedule; 
    //the multiplicities of the slices in the primepump
    //Slice->Integer
    private HashMap<Slice, Integer> primePumpMult;
    
    public SpaceTimeSchedule(Partitioner p, RawChip r) {
        rawChip = r;
        partitioner = p;
    
        primePumpMult = new HashMap<Slice, Integer>();
      
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
        this.initSchedule = (Slice[])is.toArray(new Slice[0]);
    }

    /**
     * @return Returns the initSchedule.
     */
    public Slice[] getInitSchedule() {
        return initSchedule;
    }

    /**
     * Return a linked list representation of the
     * steady state schedule;
     * 
     * @return a linked list representation of the
     * steady state schedule;
     */
    public LinkedList<Slice> getScheduleList() {
        LinkedList<Slice> list = new LinkedList<Slice>();
        for (int i = 0; i < schedule.length; i++)
            list.add(schedule[i]);
        
        return list;
    }
    
    /**
     * @return Returns the steady state schedule.
     */
    public Slice[] getSchedule() {
        return schedule;
    }

    /**
     * @param schedule The steady-state schedule to set.
     */
    public void setSchedule(LinkedList<Slice> schedule) {
        this.schedule = schedule.toArray(new Slice[0]);
    }
    
    
    /**
     * @return Returns the primePumpSchedule.
     */
    public Slice[][] getPrimePumpSchedule() {
        return primePumpSchedule;
    }

    /** 
     * @return A flat (one-dimensional) array of the primepump schedule.
     */
    public Slice[] getPrimePumpScheduleFlat() {
        LinkedList<Slice> pp = new LinkedList<Slice>();
        
        for (int i = 0; i < primePumpSchedule.length; i++) 
            for (int j = 0; j < primePumpSchedule[i].length; j++) 
                pp.add(primePumpSchedule[i][j]);
        
        
        return pp.toArray(new Slice[0]);
    }
    
    /**
     * @param preLoopSchedule The primePumpSchedule to set.
     */
    public void setPrimePumpSchedule(LinkedList<LinkedList<Slice>> preLoopSchedule) {
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
        for (int i = 0; i < partitioner.io.length; i++) 
            if (partitioner.io[i].getHead().isFileOutput())
                fileWriters.add(partitioner.io[i]);
        
        for (int i = 0; i < fileWriters.size(); i++) {
            FilterSliceNode node = (FilterSliceNode)fileWriters.get(i).getHead().getNext();
            FilterInfo fi = FilterInfo.getFilterInfo(node);
            assert node.getFilter().getInputType().isNumeric() :
                "non-numeric type for input to filewriter";
        
            outputs += fi.totalItemsReceived(false, false);
        }
        return outputs;
    }
    
    /**
     * @param f
     * @return The total number of times this filter fires in the prime pump stage
     * so this accounts for the number number of times that a slice if called in the
     * prime pump stage to fill the rotating buffers.
     */
    public int getPrimePumpTotalMult(FilterInfo f) {
        return getPrimePumpMult(f.sliceNode.getParent()) * f.steadyMult;
    }
    
    /** 
     * @param slice
     * @return Return the number of times this slice fires in the prime pump schedule.
     */
    public int getPrimePumpMult(Slice slice) {
        if (!primePumpMult.containsKey(slice))
            return 0;
        return primePumpMult.get(slice).intValue();
    }
    
  
}

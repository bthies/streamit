package at.dms.kjc.raw;

import java.util.Vector;

/**
 * This class represents a node in the switch schedule.
 * Each node corresponds with one data item.  So 
 * for duplicate splitters there will be multiple targets
 * for a data item (hence the vector destinations).
 */
public class SwitchScheduleNode 
{
    /* Next node in the schedule */
    public SwitchScheduleNode next;
    /* destination filters for this data item */
    private Vector destinations;

    
    public SwitchScheduleNode() {
	destinations = new Vector();
    }

    public void addDest(int tile) {
	destinations.add(new Integer(tile));
    }
    
    public int getDest(int i) 
    {
	return ((Integer)(destinations.get(i))).intValue();
    }
    
}


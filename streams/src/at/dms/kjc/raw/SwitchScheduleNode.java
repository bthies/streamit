package at.dms.kjc.raw;

import java.util.Vector;
import at.dms.kjc.sir.lowering.Namer;

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
    private int size;
        
    public SwitchScheduleNode() {
	destinations = new Vector();
	size = 0;
    }

    public void addDest(FlatNode to) {
	destinations.add(to);
	size++;
    }
    
    public int getSize() 
    {
	return size;
    }
    
    public FlatNode getDest(int i) 
    {
	return (FlatNode)destinations.get(i);
    }
    
    public void printMe() {
	for (int i = 0; i < destinations.size(); i++) {
	    FlatNode node = (FlatNode)destinations.get(i);
	    System.out.println(" " + Namer.getName(node.contents) + " Tile:" +
			       Layout.getTile(node.contents));
	}
	System.out.println("=====");
    }
    
}


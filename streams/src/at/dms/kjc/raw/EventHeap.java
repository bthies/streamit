package at.dms.kjc.raw;

import java.util.LinkedList;
import java.util.List;
import at.dms.kjc.flatgraph.FlatNode;
import java.util.Iterator;
import java.util.HashMap;

/**
 * The class represents the heap of scheduled events for the work
 * based simulator.  It is self-explanatory.
 **/
public class EventHeap 
{
    private LinkedList eventHeap;
    private HashMap itemIDs;

    public EventHeap() 
    {
	eventHeap = new LinkedList();
	itemIDs = new HashMap();
    }

    public boolean isEmpty() 
    {
	return eventHeap.isEmpty();
    }
    
    //add to the event queue in the correct order...
    public void addEvent(SimulatorEvent event) 
    {
	int pos = -1;
	int i;
	
	for (i = 0; i < eventHeap.size(); i++) {
	    SimulatorEvent current = (SimulatorEvent)eventHeap.get(i);
	    if (current.time > event.time) {
		pos = i;
		break;
	    }
	}
	
	for (int j = i; i < eventHeap.size(); i++) {
	    SimulatorEvent current = (SimulatorEvent)eventHeap.get(i);
	    if (current.node == event.node && 
		current.itemID < event.itemID) {
		pos = i + 1;
	    } 
	}
	
	if (pos != -1)
	    eventHeap.add(pos, event);
	else	//if we get here, add it to the end
	    eventHeap.addLast(event);
    }
    
    
    public SimulatorEvent getNextEvent() 
    {
	if (!eventHeap.isEmpty())
	    return (SimulatorEvent)eventHeap.removeFirst();
	else 
	    return null;
    }
    
    public SimulatorEvent peekNextEvent() 
    {
	return (SimulatorEvent)eventHeap.getFirst();
    }

    public Iterator iterator() 
    {
	return eventHeap.iterator();
    }

    public int getItemId(FlatNode node) 
    {
	if (!itemIDs.containsKey(node)) {
	    itemIDs.put(node, new Integer(0));
	    return 0;
	}
	else {
	    int old = ((Integer)itemIDs.get(node)).intValue();
	    itemIDs.put(node, new Integer(old + 1));
	    return old;
	}
    }    
}

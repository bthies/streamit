package at.dms.kjc.raw;

import java.util.LinkedList;
import java.util.List;
import at.dms.kjc.flatgraph.FlatNode;
import java.util.Iterator;

/**
 * The class represents the heap of scheduled events for the work
 * based simulator.  It is self-explanatory.
 **/
public class EventHeap 
{
    private LinkedList eventHeap;
    
    public EventHeap() 
    {
	eventHeap = new LinkedList();
    }

    public boolean isEmpty() 
    {
	return eventHeap.isEmpty();
    }
    
    //add to the event queue in the correct order...
    public void addEvent(SimulatorEvent event) 
    {
	for (int i = 0; i < eventHeap.size(); i++) {
	    if (((SimulatorEvent)eventHeap.get(i)).time > event.time) {
		eventHeap.add(i, event);
		return;
	    }
	}
	//if we get here, add it to the end
	eventHeap.addLast(event);
    }
    
    public void addEvent(String type, int time, FlatNode node, List dests, boolean isLast)
    {
	SimulatorEvent event = new SimulatorEvent(type, time, node, dests, isLast);
	addEvent(event);
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
    
}

package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;

public class SimpleScheduler 
{
    public Partitioner partitioner;
    private int currentTime;
    //the time when a tile is available
    private int[] tileAvail;
    //true if the tile reads from a file
    private boolean[] readsFile;
    //true if the tile writes a file
    private boolean[] writesFile;
    private RawChip rawChip;
    private LinkedList schedule;

    public SimpleScheduler(Partitioner partitioner, RawChip rawChip) 
    {
	this.partitioner = partitioner;
	this.rawChip = rawChip;
	schedule = new LinkedList();
	readsFile = new boolean[rawChip.getTotalTiles()];
	writesFile = new boolean[rawChip.getTotalTiles()];
	for (int i = 0; i < rawChip.getTotalTiles(); i++) {
	    readsFile[i] = false;
	    writesFile[i] = false;
	}
	
    }
    
    public void schedule() 
    {
	//sort traces...
	Trace[] tempArray = (Trace[])partitioner.getTraceGraph().clone();
	Arrays.sort(tempArray, 
		    new CompareTraceBNWork(partitioner));
	List sortedTraces = Arrays.asList(tempArray);

	
	//reverse the list
	Collections.reverse(sortedTraces);

	//set all the available times to zero
	tileAvail = new int[rawChip.getTotalTiles()];
	for (int i = 0; i < rawChip.getTotalTiles(); i++)
	    tileAvail[i] = 0;

	//start to schedule the traces
	currentTime = 0;

	while (!sortedTraces.isEmpty()) {
	    Trace trace = (Trace)sortedTraces.get(0);
	    System.out.println("Scheduling " + trace);
	    while (true) {
		HashMap layout = canScheduleTrace(trace);
		//if we cannot schedule this trace...
		if (layout == null) {
		    //try to schedule other traces that will fit before the 
		    //room becomes available for this trace
		    while (scheduleSmallerTrace(sortedTraces, trace)) {
		    }
		    //increment current time to next smallest avail time
		    incrementCurrentTime();
		}
		else {
		    scheduleTrace(layout, trace, sortedTraces);
		    break;
		}
	    }
	}
    }
    
    //reset the current time to next min tile avail time
    private void incrementCurrentTime() 
    {
	int newMin = Integer.MAX_VALUE;

	for (int i = 0; i < tileAvail.length; i++) {
	    if (currentTime < tileAvail[i]) {
		if (tileAvail[i] < newMin)
		    newMin = tileAvail[i];
	    }
	}
	currentTime = newMin;
    }
    

    //layout maps FilterTraceNodes -> RawTiles...
    private void scheduleTrace(HashMap layout, Trace trace, List sortedList) 
    {
	assert layout != null && trace != null;
	//remove this trace from the list of traces to schedule
	sortedList.remove(trace);
	//add the trace to the schedule
	schedule.add(trace);
	//now set the layout for the filterTraceNodes
	//and set the available time for each tile
	TraceNode node = trace.getHead().getNext();
	while (node instanceof FilterTraceNode) {
	    assert layout.containsKey(node) && 
		layout.get(node) != null;
	    RawTile tile = (RawTile)layout.get(node);
	    ((FilterTraceNode)node).setXY(tile.getX(), 
					  tile.getY());
	    //add to the avail time for the tile
	    //add the bottleneck work
	    tileAvail[tile.getTileNumber()] = currentTime 
		+ partitioner.getTraceBNWork(trace);

	    //if this is a file node, record that we have 
	    //used this tile to either read or write a file...
	    if (((FilterTraceNode)node).isFileInput()) {
		assert tile.hasIODevice() && !readsFile[tile.getTileNumber()];
		readsFile[tile.getTileNumber()] = true;
		
	    } else if (((FilterTraceNode)node).isFileOutput()) {
		assert tile.hasIODevice() && !writesFile[tile.getTileNumber()];
		writesFile[tile.getTileNumber()] = true;
	    }
	}
    }
    
    //remove it from the sortedTraces list...
    private boolean scheduleSmallerTrace(List sortedTraces, Trace bigTrace) 
    {
	return false;
    }

    //see if we can schedule the trace at current time given tile avail
    //return the layout mapping filtertracenode -> rawtile
    private HashMap canScheduleTrace(Trace trace) 
    {
	if (getAvailTiles() < trace.getNumFilters())
	    return null;
	//try all starting tiles
	for (int i = 0; i < rawChip.getTotalTiles(); i++) {
	    HashMap layout = new HashMap();
	    //if successful, return layout
	    if (getLayout(trace.getHead().getNextFilter(), rawChip.getTile(0), 
			  layout))
		return layout;
	}
	//if we got here, then we could not find a layout
	return null;
    }

    private boolean getLayout(FilterTraceNode filter, RawTile tile, HashMap layout) 
    {
	//check if this tile is available, if not return false
	if (!isTileAvail(tile))
	    return false;
	
	//if this is an endpoint, it must be on a border tile
	if (filter.getNext() instanceof OutputTraceNode &&
	    !tile.hasIODevice())
	    return false;
	
	//check file readers/writers, they must be 
	//on border and each tile can have one of each
	if (filter.isFileInput() && !(tile.hasIODevice() && 
				      readsFile[tile.getTileNumber()]))
	    return false;
	if (filter.isFileOutput() && !(tile.hasIODevice() &&
				       writesFile[tile.getTileNumber()]))
	    return false;

	//see if the downstream filters fit
	if (filter.getNext() instanceof FilterTraceNode) {
	    
	}
	
	//add this to the layout, because everything worked
	layout.put(filter, tile);
	return true;
    }
    
    private boolean isTileAvail(RawTile tile) 
    {
	if (tileAvail[tile.getTileNumber()] <= currentTime)
	    return true;
	return false;
    }

    //return the number of available tiles at the current time
    private int getAvailTiles() 
    {
	int ret = 0;
	for (int i = 0; i < tileAvail.length; i++) 
	    if (tileAvail[i] <= currentTime)
		ret ++;
	return ret;
    }
    

}


public class CompareTraceBNWork implements Comparator
{
    private Partitioner partitioner;
    
    public CompareTraceBNWork(Partitioner partitioner) 
    {
	this.partitioner = partitioner;
    }
    
    public int compare (Object o1, Object o2) 
    {
	assert o1 instanceof Trace && o2 instanceof Trace;
	
	if (partitioner.getTraceBNWork((Trace)o1) < 
	    partitioner.getTraceBNWork((Trace)o2))
	    return -1;
	else if (partitioner.getTraceBNWork((Trace)o1) ==
		 partitioner.getTraceBNWork((Trace)o2))
	    return 0;
	else
	    return 1;
    }
}

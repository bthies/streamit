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
    private SpaceTimeSchedule spSched;

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
	//	spSched = new SpaceTimeSchedule(rawChip.getYSize(), rawChip.getXSize());
    }
    
    public LinkedList schedule() 
    {

	//sort traces...
	Trace[] tempArray = (Trace[])partitioner.getTraceGraph().clone();
	Arrays.sort(tempArray, 
		    new CompareTraceBNWork(partitioner));
	LinkedList sortedTraces = new LinkedList(Arrays.asList(tempArray));

	
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
	    System.out.println("Trying to schedule " + trace);
	    while (true) {
		HashMap layout = canScheduleTrace(trace);
		//if we cannot schedule this trace...
		if (layout == null) {
		    //try to schedule other traces that will fit before the 
		    //room becomes available for this trace
		    while (scheduleSmallerTrace(sortedTraces, trace)) {}
		    //increment current time to next smallest avail time
		    currentTime = nextTime(currentTime);
		}
		else {
		    scheduleTrace(layout, trace, sortedTraces);
		    break;
		}
	    }
	}
	
	//set up dependencies
	setupDepends();
	return schedule;
    }

    private void setupDepends() 
    {
	Iterator sch = schedule.iterator();
	Trace prev = null;
	System.out.println("Schedule: ");
	while (sch.hasNext()) {
	    Trace trace = (Trace)sch.next();
	    System.out.println(" * " + trace);
	    if (prev != null)
		trace.addDependency(prev);
	    prev = trace;
	}
	
	//now call done dependencies...cannot be done up there...
	sch = schedule.iterator();
	while (sch.hasNext()) {
	    ((Trace)sch.next()).doneDependencies();
	}
	
    }
    

    //see if we can schedule any smaller traces to run and finish before the 
    //earliest time the current trace can start execution
    private boolean scheduleSmallerTrace(LinkedList sortedTraces, Trace bigTrace) 
    {
	
	//find the earliest time that bigTrace can execute based on the 
	//number of idle tiles...
	int finishBefore = currentTime;
	while (true) {
	    int idleTiles = 0; 
	    for (int i = 0; i < tileAvail.length; i++) {
		if (finishBefore >= tileAvail[i])
		    idleTiles++;
	    }
	    if (idleTiles >= bigTrace.getNumFilters())
		break;
	    
	    finishBefore = nextTime(finishBefore);
	}
	
	//see if we can schedule any trace to finish before finishBefore
	Iterator it = sortedTraces.iterator();
	assert it.next() == bigTrace;
	while (it.hasNext()) {
	    Trace trace = (Trace)it.next();
	    System.out.println("   (Trying to schedule smaller trace " + trace + ")");
	    //see when it will finish, if smaller than finishBefore
	    if ((currentTime + partitioner.getTraceBNWork(trace)) <= 
		    finishBefore) {
		//see if we can schedule the trace now
		HashMap layout = canScheduleTrace(trace);
		if (layout != null) {
		    //schedule the trace and return true
		    scheduleTrace(layout, trace, sortedTraces);
		    return true;
		}
	    }
	}
	return false;
    }

    //reset the current time to next min tile avail time
    private int nextTime(int time) 
    {
	int newMin = 0;
	//set newMin to max of tileavail times
	for (int i = 0; i < tileAvail.length; i++) 
	    if (newMin < tileAvail[i])
		newMin = tileAvail[i];

	for (int i = 0; i < tileAvail.length; i++) {
	    if (time < tileAvail[i] && tileAvail[i] < newMin) {
		newMin = tileAvail[i];
	    }
	}

	return newMin;
    }
    

    //layout maps FilterTraceNodes -> RawTiles...
    private void scheduleTrace(HashMap layout, Trace trace, LinkedList sortedList) 
    {
	assert layout != null && trace != null;
	System.out.println("Scheduling Trace: " + trace);
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
	    
	    System.out.println("  *(" + currentTime + ") Assigning " + node + " to " + tile + 
			       "(new avail: " + tileAvail[tile.getTileNumber()] + ")");

	    //if this is a file node, record that we have 
	    //used this tile to either read or write a file...
	    if (((FilterTraceNode)node).isFileInput()) {
		assert tile.hasIODevice() && !readsFile[tile.getTileNumber()];
		readsFile[tile.getTileNumber()] = true;
		
	    } else if (((FilterTraceNode)node).isFileOutput()) {
		assert tile.hasIODevice() && !writesFile[tile.getTileNumber()];
		writesFile[tile.getTileNumber()] = true;
	    }

	    //set the space time schedule?? Jasper's stuff
	    //don't do it for the first node
	    /*
	    if (node != trace.getHead().getNext()) 
		spSched.add(trace, ((FilterTraceNode)node).getY(),
			  ((FilterTraceNode)node).getX());
	    else //do this instead
		spSched.addHead(trace, ((FilterTraceNode)node).getY(),
				((FilterTraceNode)node).getX());
	    */

	    node = node.getNext();
	}
    }
    
    //see if we can schedule the trace at current time given tile avail
    //return the layout mapping filtertracenode -> rawtile
    private HashMap canScheduleTrace(Trace trace) 
    {
	if (getAvailTiles() < trace.getNumFilters())
	    return null;
	
	LinkedList tiles = new LinkedList();
	for (int i = 0; i < rawChip.getTotalTiles(); i++) {
	    tiles.add(rawChip.getTile(i));
	}
	

	//first try starting tiles that are either endpoints of upstream
	//or starts of downstream
	Iterator sources = trace.getHead().getSourceSet().iterator();
	while (sources.hasNext()) {
	    Trace current = ((Edge)sources.next()).getSrc().getParent();
	    if (schedule.contains(current)) {
		//get the endpoint of the trace
		RawTile tile = rawChip.getTile(current.getTail().getPrevFilter().getX(),
					       current.getTail().getPrevFilter().getY());
		//we already tried this tile previously
		if (!tiles.contains(tile))
		    continue;
		HashMap layout = new HashMap();
		System.out.println("     (trying " + tile + " at " + currentTime + ")");
		//if successful, return layout
		if (getLayout(trace.getHead().getNextFilter(), tile,
			      layout))
		    return layout;
		else //we tried this tile, remove it...
		    tiles.remove(tile);
	    }
	    
	}
	
	Iterator dests = trace.getTail().getDestSet().iterator();
	while (dests.hasNext()) {
	    Trace current = ((Edge)dests.next()).getDest().getParent();
	    if (schedule.contains(current)) {
		//get the endpoint of the trace
		RawTile tile = rawChip.getTile(current.getHead().getNextFilter().getX(),
					       current.getHead().getNextFilter().getY());
		//we already tried this tile previously
		if (!tiles.contains(tile))
		    continue;
		HashMap layout = new HashMap();
		System.out.println("     (trying " + tile + " at " + currentTime + ")");
		//if successful, return layout
		if (getLayout(trace.getHead().getNextFilter(), tile, 
			      layout))
		    return layout;
		else //we tried this tile, remove it...
		    tiles.remove(tile);
	    }
	}
	
	Iterator tilesIt = tiles.iterator();
	while (tilesIt.hasNext()) {
	    RawTile tile = (RawTile)tilesIt.next();
	    HashMap layout = new HashMap();
	    System.out.println("     (trying " + tile + " at " + currentTime + ")");
	    //if successful, return layout
	    if (getLayout(trace.getHead().getNextFilter(), tile,
			  layout))
		return layout;
	}
	//if we got here, then we could not find a layout
	return null;
    }

    private boolean getLayout(FilterTraceNode filter, RawTile tile, HashMap layout)
    {
	
	//check if this tile is available, if not return false
	if (!isTileAvail(tile)) {
	    System.out.println("       (Tile not currently available)");
	    return false;
	}
	
	//cannot assign a tile twice...
	if (layout.containsValue(tile)) {
	    System.out.println("       (Tile Already Assigned)");
	    return false;
	}

	//if this is an endpoint, it must be on a border tile
	if ((filter.getNext().isOutputTrace() || filter.getPrevious().isInputTrace()) &&
	    !tile.hasIODevice()) {
	    System.out.println("       (Endpoint not at border tile)");
	    return false;
	}
	
	//check file readers/writers, they must be 
	//on border and each tile can have one of each
	if (filter.isFileInput() && !(tile.hasIODevice() && 
				      !readsFile[tile.getTileNumber()])) {
	    System.out.println("       (Failed file reader)");
	    return false;
	}
	
	if (filter.isFileOutput() && !(tile.hasIODevice() &&
				       !writesFile[tile.getTileNumber()])) {
	    System.out.println("       (Failed file writer)");
	    return false;
	}
	
	//add this to the layout, because everything worked
	layout.put(filter, tile);
	
	//see if the downstream filters fit
	if (filter.getNext().isFilterTrace()) {

	    Vector neighbors = tile.getNeighborTiles();
	    //try all the possible neighboring tiles for the
	    //next filter, if any work return true
	    boolean found = false;
	    //try the middle tiles first
	    for (int i = 0; i < neighbors.size(); i++) 
		if (!((RawTile)neighbors.get(i)).hasIODevice() &&
		    getLayout((FilterTraceNode)filter.getNext(), 
			      (RawTile)neighbors.get(i), layout)) {
		    found = true;
		    break;
		}
	    //try border tiles
	    for (int i = 0; !found && i < neighbors.size(); i++) 
		if (((RawTile)neighbors.get(i)).hasIODevice() &&
		    getLayout((FilterTraceNode)filter.getNext(), 
			      (RawTile)neighbors.get(i), layout)) {
		    found = true;
		    break;
		}
	    //nothing found return false
	    if (!found) {
		//remove the current tile from the layout
		layout.remove(filter);
		System.out.println("       (Cannot find anything downstream)");
		return false;
	    }
	    
	}
	
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

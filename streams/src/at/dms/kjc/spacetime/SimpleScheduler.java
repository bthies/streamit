package at.dms.kjc.spacetime;

import java.util.*;
import java.io.FileWriter;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import at.dms.kjc.sir.linear.LinearAnalyzer;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.*;

public class SimpleScheduler 
{
    public Partitioner partitioner;
    private int currentTime;
    //the time when a tile is available
    private int[] tileAvail;
    
    //for right now just place one file reader and writer on
    //a tile, I know the corner tiles can support 2

    //true if the tile reads from a file
    private boolean[] readsFile;
    //true if the tile writes a file
    private boolean[] writesFile;
    private RawChip rawChip;
    private LinkedList schedule;
    private SpaceTimeSchedule spSched;
    private LinkedList initSchedule;
    //the io that has been laid out so far (traces)
    private HashSet laidOutIO;

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
	laidOutIO = new HashSet();
	//	spSched = new SpaceTimeSchedule(rawChip.getYSize(), rawChip.getXSize());
    }
    
    public LinkedList getSchedule() 
    {
	return schedule;
    }

    public LinkedList getInitSchedule() 
    {
	return initSchedule;
    }
    
    
    //return true if t1 is scheduled before t2
    public boolean scheduledBefore(Trace t1, Trace t2) 
    {

	if (partitioner.isIO(t1)) {
	    //assume that file readers come before everything
	    if (t1.getTail().isFileReader())
		return true;
	    if (t2.getTail().isFileReader()) 
		return false;
	    //assume file writes come after everything	    
	    if (t1.getHead().isFileWriter())
		return false;
	    if (t2.getHead().isFileWriter())
		return true;
	}
	

	assert schedule.contains(t1) && schedule.contains(t2) &&
	    t1 != t2;
	if (schedule.indexOf(t1) < schedule.indexOf(t2))
	    return true;
	return false;
    }

    public void schedule() 
    {
	initSchedule = InitSchedule.getInitSchedule(partitioner.topTraces);
	
	//set current time
	currentTime = 0;
	
	//set all the available times to zero
	tileAvail = new int[rawChip.getTotalTiles()];
	for (int i = 0; i < rawChip.getTotalTiles(); i++)
	    tileAvail[i] = 0;


	//schedule the traces either based on work or dependencies
	if (KjcOptions.scheduler.equals("work")) 
	    scheduleWork();
	else if (KjcOptions.scheduler.equals("comm"))
	    scheduleCommunication();
	else if (KjcOptions.scheduler.equals("dep"))
	    scheduleDep();
	//set up dependencies
	setupDepends();

    }
    
    private void scheduleCommunication() 
    {
	//sort traces...
	Trace[] tempArray = (Trace[])partitioner.getTraceGraph().clone();
	Arrays.sort(tempArray, 
		    new CompareTraceCommunication());
	LinkedList sortedTraces = new LinkedList(Arrays.asList(tempArray));

	//schedule predefined filters first, but don't put them in the 
	//schedule just assign them tiles...
	removePredefined(sortedTraces);

	//reverse the list
	//Collections.reverse(sortedTraces);

	System.out.println("Sorted Traces: ");
	Iterator it = sortedTraces.iterator();
	while (it.hasNext()) {
	    Trace trace = (Trace)it.next();
	    System.out.println(" * " + trace);
	}
	

	//start to schedule the traces
	while (!sortedTraces.isEmpty()) {
	    Trace trace = null;
	    //find the first trace we can schedule
	    Iterator traces = sortedTraces.iterator();
	    while (traces.hasNext()) {
	    	trace = (Trace)traces.next();
	    	if (canScheduleTrace(trace))
		    break;
	    }

	    assert trace != null;
	    //System.out.println("Trying to schedule " + trace);
	    while (true) {
		HashMap layout = canLayoutTrace(trace);
		//if we cannot schedule this trace...
		if (layout == null) {
		    //try to schedule other traces that will fit before the 
		    //room becomes available for this trace
		    //while (scheduleSmallerTrace(sortedTraces, trace)) {}
		    //increment current time to next smallest avail time
		    currentTime = nextTime(currentTime);
		}
		else {
		    scheduleTrace(layout, trace, sortedTraces);
		    break;
		}
	    }
	}	
    }
    

    //schedule according to data-flow dependencies
    private void scheduleDep() 
    {
	LinkedList sortedTraces = (LinkedList)initSchedule.clone();

	//schedule predefined filters first, but don't put them in the 
	//schedule just assign them tiles...
	removePredefined(sortedTraces);

	//start to schedule the traces
	while (!sortedTraces.isEmpty()) {
	    Trace trace = (Trace)sortedTraces.get(0);
	    //System.out.println("Trying to schedule " + trace);
	    while (true) {
		HashMap layout = canLayoutTrace(trace);
		//if we cannot schedule this trace...
		if (layout == null) {
		    //try to schedule other traces that will fit before the 
		    //room becomes available for this trace
		    //while (scheduleSmallerTrace(sortedTraces, trace)) {}
		    //increment current time to next smallest avail time
		    currentTime = nextTime(currentTime);
		}
		else {
		    scheduleTrace(layout, trace, sortedTraces);
		    break;
		}
	    }
	}
	
    }
    
    //schedule according to work load
    private void scheduleWork() 
    {

	//sort traces...
	Trace[] tempArray = (Trace[])partitioner.getTraceGraph().clone();
	Arrays.sort(tempArray, 
		    new CompareTraceBNWork(partitioner));
	LinkedList sortedTraces = new LinkedList(Arrays.asList(tempArray));

	//schedule predefined filters first, but don't put them in the 
	//schedule just assign them tiles...
	removePredefined(sortedTraces);
	


	//reverse the list
	Collections.reverse(sortedTraces);
	
	System.out.println("Sorted Traces: ");
	Iterator it = sortedTraces.iterator();
	while (it.hasNext()) {
	    Trace trace = (Trace)it.next();
	    System.out.println(" * " + trace + " (work: " +
			       partitioner.getTraceBNWork(trace) + ")");
	}
	

	//start to schedule the traces
	while (!sortedTraces.isEmpty()) {
	    Trace trace = null;
	    //find the first trace we can schedule
	    Iterator traces = sortedTraces.iterator();
	    while (traces.hasNext()) {
	    	trace = (Trace)traces.next();
	    	if (canScheduleTrace(trace))
		    break;
	    }

	    assert trace != null;
	    //System.out.println("Trying to schedule " + trace);
	    while (true) {
		HashMap layout = canLayoutTrace(trace);
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
    }

    private boolean canScheduleTrace(Trace trace) 
    {
	//check to make sure all srcs are in the same "state" either scheduled or not
	Iterator sources = trace.getHead().getSourceSet().iterator();
	if (sources.hasNext()) {
	    Trace src = ((Edge)sources.next()).getSrc().getParent();	
	    boolean isScheduled = schedule.contains(src);
	    while (sources.hasNext()) {
		src = ((Edge)sources.next()).getSrc().getParent();
		if (isScheduled != schedule.contains(src))
		    return false;
	    }
	}
	
	//check to make sure all dests are in the same "state" either scheduled or not
	 Iterator dests = trace.getTail().getDestSet().iterator();
	 if (dests.hasNext()) {
	     Trace dst = ((Edge)dests.next()).getDest().getParent();    
	     boolean isScheduled = schedule.contains(dst);
	     while (dests.hasNext()) {
		 dst =  ((Edge)dests.next()).getDest().getParent();    
		 if (isScheduled != schedule.contains(dst))
		     return false;
	     }
	 }
	 //everything passed
	 return true;
    }
    
    
    private void removePredefined(LinkedList sortedTraces) 
    {
	for (int i = 0; i < partitioner.io.length; i++) {
	    /*  Some old code, ignore
	    HashMap layout = canLayoutTrace(partitioner.io[i]);
	    assert layout != null : "Cannot find tile for predefined filter";
	    //"assign" the predefined to a tile...
	    FilterTraceNode node = partitioner.io[i].getHead().getNextFilter();
	    assert node.isPredefined() && layout.containsKey(node) &&
		layout.get(node) != null;
	    //assign
	     RawTile tile = (RawTile)layout.get(node);
	    ((FilterTraceNode)node).setXY(tile.getX(), 
					  tile.getY());
	    System.out.println("Assigning " + node + " to " + tile);
	    //record that we used this port for a file reader or writer...
	    if (((FilterTraceNode)node).isFileInput()) {
		assert tile.hasIODevice() && !readsFile[tile.getTileNumber()];
		readsFile[tile.getTileNumber()] = true;
		
	    } else if (((FilterTraceNode)node).isFileOutput()) {
		assert tile.hasIODevice() && !writesFile[tile.getTileNumber()];
		writesFile[tile.getTileNumber()] = true;
	    }
	    */
	    sortedTraces.remove(partitioner.io[i]);
	}	
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
	//assert it.next() == bigTrace;
	while (it.hasNext()) {
	    Trace trace = (Trace)it.next();
	    //see if we can legally schedule the trace
	    if (!canScheduleTrace(trace))
	    	continue;
	    //	    System.out.println("   (Trying to schedule smaller trace " + trace + ")");
	    //see when it will finish, if smaller than finishBefore
	    if ((currentTime + partitioner.getTraceBNWork(trace)) <= 
		    finishBefore) {
		//see if we can schedule the trace now
		HashMap layout = canLayoutTrace(trace);
		if (layout != null) {
		    //schedule the trace and return true
		    System.out.println("  Scheduling smaller trace " + trace);
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
	System.out.println("Scheduling Trace: " + trace + " at time " + currentTime);
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

	    //add to the avail time for the tile, use either the current time or the tile's avail
	    //whichever is greater
	    //add the bottleneck work
	    tileAvail[tile.getTileNumber()] = 
		((currentTime > tileAvail[tile.getTileNumber()]) ? currentTime : tileAvail[tile.getTileNumber()])
		+ partitioner.getTraceBNWork(trace);
	    System.out.println("   * new avail for " + tile + " = " + tileAvail[tile.getTileNumber()]);
	    //	    System.out.println("  *(" + currentTime + ") Assigning " + node + " to " + tile + 
	    //		       "(new avail: " + tileAvail[tile.getTileNumber()] + ")");

	    assert !(((FilterTraceNode)node).isFileInput() || 
		     ((FilterTraceNode)node).isFileInput());
	    
	    //if this trace has file input take care of it,
	    //assign the file reader and record that the tile reads a file...
	    if (node.getPrevious().isInputTrace()) {
		InputTraceNode in = (InputTraceNode)node.getPrevious();
		//if this trace reads a file, make sure that we record that
		//the tile is being used to read a file
		if (in.hasFileInput()) {
		    readsFile[tile.getTileNumber()] = true;
		    assert in.getSingleEdge().getSrc().getPrevFilter().isPredefined();
		    //set the tile for the file reader to tbe this tile
		    in.getSingleEdge().getSrc().getPrevFilter().setXY(tile.getX(), 
								      tile.getY());
		}
	    }

	    //writes to a file, set tile for writer and record that the tile 
	    //writes a file
	    if (node.getNext().isOutputTrace()) {
		OutputTraceNode out = (OutputTraceNode)node.getNext();
		if (out.hasFileOutput()) {
		    writesFile[tile.getTileNumber()] = true;
		    assert out.getSingleEdge().getDest().getNextFilter().isPredefined();
		    //set the tile
		    out.getSingleEdge().getDest().getNextFilter().setXY(tile.getX(),
									tile.getY());
		}
	    }	

	    //set file reading and writing
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
    private HashMap canLayoutTrace(Trace trace) 
    {
	if (getAvailTiles() < trace.getNumFilters())
	    return null;
	
	LinkedList tiles = new LinkedList();
	for (int i = 0; i < rawChip.getTotalTiles(); i++) {
	    tiles.add(rawChip.getTile(i));
	}
	
	//if we want to, try to force a single input trace to the source's tile as long as the
	//source has one output
	if (KjcOptions.forceplacement && 
	    trace.getHead().getSourceSet().size() == 1) {
	    Trace upstream = ((Edge)trace.getHead().getSourceSet().iterator().next()).getSrc().getParent();
	    if (schedule.contains(upstream) && upstream.getTail().getDestSet().size() == 1) { 
		RawTile tile = rawChip.getTile(upstream.getTail().getPrevFilter().getX(),
					       upstream.getTail().getPrevFilter().getY());
		if (tiles.contains(tile)) {
		    HashMap layout = new HashMap();
		    //try to place on the tile ignoring the tile avail
		    if (getLayout(trace.getHead().getNextFilter(), tile, layout, true))
			return layout;
		    else 
			tiles.remove(tile);
		}
	    }
	}
	
	//if we want to, try to force a single output trace to the dest's tile as long as the
	//downstream tile has one input
	if (KjcOptions.forceplacement && 
	    trace.getTail().getDestSet().size() == 1) {
	    Trace downstream = ((Edge)trace.getTail().getDestSet().iterator().next()).getDest().getParent();
	    if (schedule.contains(downstream)) {
		RawTile tile = rawChip.getTile(downstream.getTail().getPrevFilter().getX(),
					       downstream.getTail().getPrevFilter().getY());
		if (tiles.contains(tile) && downstream.getHead().getSourceSet().size() == 1) {
		    HashMap layout = new HashMap();
		    //try to place on the tile ignoring the tile avail
		    if (getLayout(trace.getHead().getNextFilter(), tile, layout, true))
			return layout;
		    else 
			tiles.remove(tile);
		}
	    }
	}
	
	
	//first try starting tiles that are either endpoints of upstream
	//or starts of downstream
	Iterator sources = trace.getHead().getSourceSet().iterator();
	while (sources.hasNext()) {
	    Trace current = ((Edge)sources.next()).getSrc().getParent();
	    //now if the source is already scheduled or if it is io try its tile
	    if (schedule.contains(current)) {
		//get the endpoint of the trace
		RawTile tile = rawChip.getTile(current.getTail().getPrevFilter().getX(),
					       current.getTail().getPrevFilter().getY());
		//we already tried this tile previously
		if (!tiles.contains(tile))
		    continue;
		HashMap layout = new HashMap();
		//System.out.println("     (for " + trace + " trying source " + tile + " at " + currentTime + ")");
		//if successful, return layout
		if (getLayout(trace.getHead().getNextFilter(), tile,
			      layout, false))
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
		//System.out.println("     (for " + trace + " trying dest " + tile + " at " + currentTime + ")");
		//if successful, return layout
		if (getLayout(trace.getHead().getNextFilter(), tile, 
			      layout, false))
		    return layout;
		else //we tried this tile, remove it...
		    tiles.remove(tile);
	    }
	}
	
	Iterator tilesIt = tiles.iterator();
	while (tilesIt.hasNext()) {
	    RawTile tile = (RawTile)tilesIt.next();
	    HashMap layout = new HashMap();
	    //System.out.println("     (for " + trace + " trying " + tile + " at " + currentTime + ")");
	    //if successful, return layout
	    if (getLayout(trace.getHead().getNextFilter(), tile,
			  layout, false))
		return layout;
	}
	//if we got here, then we could not find a layout
	return null;
    }

    
    private boolean getLayout(FilterTraceNode filter, RawTile tile, HashMap layout, boolean ignoreAvail)
    {
	//System.out.println("For " + filter + " trying " + tile);
	//check if this tile is available, if not return false
	if (!ignoreAvail && !isTileAvail(tile)) {
	    //System.out.println("       (Tile not currently available)");
	    return false;
	}
	
	//cannot assign a tile twice...
	if (layout.containsValue(tile)) {
	    //System.out.println("       (Tile Already Assigned)");
	    return false;
	}

	//if this is an endpoint, it must be on a border tile
	if ((filter.getNext().isOutputTrace() || filter.getPrevious().isInputTrace()) &&
	    !tile.hasIODevice()) {
	    //System.out.println("       (Endpoint not at border tile)");
	    return false;
	}
	

	//right now we are assuming that all traces reading and writes files have
	//one input/ output, so make sure this is the case 
	//the above makes sure it is a border tile
	if (filter.getNext().isOutputTrace()) {
	    OutputTraceNode out = (OutputTraceNode)filter.getNext();
	    if (out.hasFileOutput()) {
		assert out.oneOutput();
		//make sure no one else uses this tile to write
		if (writesFile[tile.getTileNumber()]) {
		    //System.out.println("       (Tile already used for file writer)");
		    return false; 
		}
		
	    }
	}
	
	if (filter.getPrevious().isInputTrace()) {
	    InputTraceNode in = (InputTraceNode)filter.getPrevious();
	    if (in.hasFileInput()) {
		assert in.oneInput();
		//make sure no one else uses this tile to write
		if (readsFile[tile.getTileNumber()]) {
		    //System.out.println("       (Tile already used for file reader)");
		    return false;
		}
		
	    }
	}
	
	/*	
	//check file readers/writers, they must be 
	//on border and each tile can have one of each
	if (filter.isFileInput() && !(tile.hasIODevice() && 
				      !readsFile[tile.getTileNumber()])) {
	    //System.out.println("       (Failed file reader)");
	    return false;
	}
	if (filter.isFileOutput() && !(tile.hasIODevice() &&
				       !writesFile[tile.getTileNumber()])) {
	    //System.out.println("       (Failed file writer)");
	    return false;
	}
	*/
	//add this to the layout, because everything worked
	layout.put(filter, tile);
	
	//see if the downstream filters fit
	if (filter.getNext().isFilterTrace()) {

	    Vector neighbors = tile.getNeighborTiles();
	    //try all the possible neighboring tiles for the
	    //next filter, if any work return true
	    boolean found = false;
	    //try the middle tiles first
	    for (int i = 0; i < neighbors.size(); i++) {
		if (!((RawTile)neighbors.get(i)).hasIODevice() &&
		    getLayout((FilterTraceNode)filter.getNext(), 
			      (RawTile)neighbors.get(i), layout, ignoreAvail)) {
		    found = true;
		    break;
		}
	    }
	    //try border tiles
	    for (int i = 0; !found && i < neighbors.size(); i++) {
		if (((RawTile)neighbors.get(i)).hasIODevice() &&
		    getLayout((FilterTraceNode)filter.getNext(), 
			      (RawTile)neighbors.get(i), layout, ignoreAvail)) {
		    found = true;
		    break;
		}
	    }
	    //nothing found return false
	    if (!found) {
		//remove the current tile from the layout
		layout.remove(filter);
		//System.out.println("       (Cannot find anything downstream)");
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


public class CompareTraceCommunication implements Comparator 
{
    public int compare (Object o1, Object o2) 
    {
	assert o1 instanceof Trace && o2 instanceof Trace;
	Trace trace1 = (Trace)o1;
	Trace trace2 = (Trace)o2;
	int comm1 = trace1.getHead().getSourceSet().size() + 
	    trace1.getTail().getDestSet().size();
	int comm2 = trace2.getHead().getSourceSet().size() + 
	    trace2.getTail().getDestSet().size();

	if (comm1 < comm2)
	    return -1;
	else if (comm1 == comm2)
	    return 0;
	else
	    return 1;
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

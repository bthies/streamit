package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import streamit.scheduler.*;
import streamit.scheduler.simple.*;

/**
 * This class generates a schedule for the switch code by simulating the 
 * init schedule and one
 * steady state execution of the schedule
 */
public class Simulator extends at.dms.util.Utils implements FlatVisitor
{
    public static HashMap initSchedules;
    public static HashMap steadySchedules;
    
    public static HashMap initJoinerCode;
    public static HashMap steadyJoinerCode;
    
    private HashMap switchSchedules;
    
    //the current joiner code we are working on (steady or init)
    private static HashMap joinerCode;
        
    //the curent node in the joiner schedule we are working on
    private HashMap currentJoinerCode;
    
    private FlatNode toplevel;
    private FlatNode bottom;
    
    public static void simulate(FlatNode top) 
    {
	Schedule schedule = SIRScheduler.getSchedule(getTopMostParent(top));

	HashMap initExecutionCounts = new HashMap();
	HashMap steadyExecutionCounts = new HashMap();
	
	HashMap initJoinerCode = new HashMap();
	HashMap steadyJoinerCode = new HashMap();
	
	//generate the joiner schedule
	JoinerSimulator.createJoinerSchedules(top);
	
	//get the schedule for the graph
	//first find the top most pipeline	
	createExecutionCounts(schedule.getInitSchedule(),
			      initExecutionCounts);
	createExecutionCounts(schedule.getSteadySchedule(), 
			      steadyExecutionCounts);

	SimulationCounter counters = 
	    new SimulationCounter(JoinerSimulator.schedules);

	joinerCode = initJoinerCode;
	initSchedules = (new Simulator(top)).go(initExecutionCounts, counters);
	joinerCode = steadyJoinerCode;
	steadySchedules = (new Simulator(top)).go(steadyExecutionCounts, counters);
    }
    
   
    

    private Simulator(FlatNode top) 
    {
	switchSchedules = new HashMap();
	currentJoinerCode = new HashMap();
	toplevel = top;
	//find the bottom (last) filter, used later to decide
	//execution order
	bottom = null;
	toplevel.accept(this, new HashSet(), false);
    }


    //creates execution counts of filters in graph (flatnode maps count)
    private static void createExecutionCounts(Object schedObject, HashMap counts) 
    {
	if (schedObject instanceof List) {
	    //visit all of the elements
	    for (ListIterator it = ((List)schedObject).listIterator();
		 it.hasNext(); ) {
		createExecutionCounts(it.next(), counts);
	    }
	} else if (schedObject instanceof SchedRepSchedule) {
    	    // get the schedRep
	    SchedRepSchedule rep = (SchedRepSchedule)schedObject;
	    ///===========================================BIG INT?????
	    for(int i = 0; i < rep.getTotalExecutions().intValue(); i++)
		createExecutionCounts(rep.getOriginalSchedule(), counts);
	} else {
	    //do not count splitter and joiners
	    if (schedObject instanceof SIRSplitter || 
		schedObject instanceof SIRJoiner)
		return;
	    
	    // hit a filter
	    if (!(schedObject instanceof SIRFilter)) {
		System.out.println(schedObject);
		Utils.fail("non-filter encountered in scheduler");
	    }
	    //add one to the count for this node
	    FlatNode fnode = FlatNode.getFlatNode((SIROperator)schedObject);
	    if (!counts.containsKey(fnode))
		counts.put(fnode, new Integer(1));
	    else {
		//add one to counter
		int old = ((Integer)counts.get(fnode)).intValue();
		counts.put(fnode, new Integer(old + 1));
	    }
	    
	}
    }
	

    //simple helper function to find the topmost pipeline
    private static SIRStream getTopMostParent(FlatNode node) 
    {
	SIRContainer[] parents = node.contents.getParents();
	return parents[parents.length -1];
    }
    
    /* the main simulation method */
    private HashMap go(HashMap counts, SimulationCounter counters) 
    {
	
	FlatNode fire, dest;
		
	while(true) {
	    //find out who should fire
	    fire = whoShouldFire(counts, counters);
	    //if no one left to fire, stop
	    if (fire == null)
		break;
	    //keep track of everything needed when a node fires
	    fireMe(fire, counters, counts);
	    //simulate the firings
	    //1 item for a joiner, push items for a filter
	    int items = 1;
	    if (fire.contents instanceof SIRFilter)
		items = ((SIRFilter)fire.contents).getPushInt();
	    
	    for (int i = 0; i < items; i++) {
		
		//get the destinations of this item
		//could be multiple dests with duplicate splitters
		//a filter always has one outgoing arc, so sent to way 0
		generateSwitchCode(fire, getDestination(fire.edges[0], 
							counters, "", 
							fire));
	    }
	    
	}
	return switchSchedules;
    }


    //generate the switch code for 1 data item given the list of destinations
    //we do not want to duplicate items until necesary, so we have to keep track 
    //of all the routes and then generate the switch code
    //this way we can route multiple dests per route instruction
    private void generateSwitchCode(FlatNode fire, List dests) 
    {
	//keeps the next hop for the sends
	HashSet sends = new HashSet();
	//keeps routes for all of the intermediate hops
	//this hashset points to a hashset 
	//the secode hash map is indexed by the sources of 
	//the route instructions for the tile
	//the second hashmap points to a hashset
	//of all the dest for a given source

	HashMap routes = new HashMap();
       
	//maps receivers to their previous hop
	HashMap receives = new HashMap();
		
	//fill the maps
	ListIterator destsIt = dests.listIterator();
	while (destsIt.hasNext()) {
	    FlatNode dest = (FlatNode)destsIt.next();
	    Coordinate[] hops = 
		(Coordinate[])Router.getRoute(fire, dest).toArray(new Coordinate[0]);
	    //add each route to the maps
	    sends.add(hops[1]);
	    //add the intermediate routes
	    for (int i = 1; i < hops.length -1; i++) {
		if (!routes.containsKey(hops[i]))
		    routes.put(hops[i], new HashMap());
		HashMap prevs = (HashMap)routes.get(hops[i]);
		if (!prevs.containsKey(hops[i-1]))
		    prevs.put(hops[i-1], new HashSet());
		HashSet nexts = (HashSet)prevs.get(hops[i-1]);
		nexts.add(hops[i+1]);
	    }
	    //add the receive
	    receives.put(hops[hops.length-1], hops[hops.length - 2]);
	}
	//now generate the send, routes, and receives
	addSends(fire, sends);
	addRoutes(routes);
	addReceives(receives);
    }
        
    private void addRoutes(HashMap routes) 
    {
	//get each router node
	Iterator routerIt = routes.keySet().iterator();
	while (routerIt.hasNext()) {
	    //for each router get all the sources
	    Coordinate router = (Coordinate)routerIt.next();
	    //get the router's switch code buffer
	    if (!switchSchedules.containsKey(router))
		switchSchedules.put(router, new StringBuffer());
	
	    StringBuffer buf = (StringBuffer)switchSchedules.get(router);
	    //for each source get the dests
	    Iterator sourcesIt = ((HashMap)routes.get(router)).keySet().iterator();
	    while(sourcesIt.hasNext()) {
		//generate the switch code, for each source send to all the dests
		//in one route instruction
		Coordinate source = (Coordinate)sourcesIt.next();
		Iterator destsIt = 
		    ((HashSet)((HashMap)routes.get(router)).get(source)).iterator();
		buf.append("\tnop\troute ");
		while(destsIt.hasNext()) {
		    Coordinate dest = (Coordinate)destsIt.next();
		    buf.append("$c" + Layout.getDirection(router, source) + "i->$c" +
			       Layout.getDirection(router, dest) + "o,");
		}
		buf.setCharAt(buf.length() - 1, '\n');
	    }
	}
    }
        
    private void addSends(FlatNode fire, HashSet sends) 
    {
	if (!switchSchedules.containsKey(Layout.getTile(fire)))
	    switchSchedules.put(Layout.getTile(fire), new StringBuffer());
	
	StringBuffer buf = (StringBuffer)switchSchedules.get(Layout.getTile(fire));
	
	Iterator it = sends.iterator();
	
	buf.append("\tnop\troute ");
	while (it.hasNext()) {
	    Coordinate dest = (Coordinate)it.next();
	    buf.append("$csto->" + "$c" + 
		       Layout.getDirection(Layout.getTile(fire), dest) + 
		       "o,");
	}
	//erase the trailing ,
	buf.setCharAt(buf.length() - 1, '\n');
    }
    
    private void addReceives(HashMap receives) 
	{
	    Iterator it = receives.keySet().iterator();
	    
	    while (it.hasNext()) {
		Coordinate rec = (Coordinate)it.next();
		Coordinate send = (Coordinate)receives.get(rec);
		if (!switchSchedules.containsKey(rec))
		    switchSchedules.put(rec, new StringBuffer());
		StringBuffer buf = (StringBuffer)switchSchedules.get(rec);
		buf.append("\tnop\troute ");
		buf.append("$c" + Layout.getDirection(rec, send) + "i");
		buf.append("->$csti\n");
	    }
	}
    
    private void fireMe(FlatNode fire, SimulationCounter counters, HashMap executionCounts) 
    {
	//System.out.println("Firing " + Namer.getName(fire.contents));
	

	if (fire.contents instanceof SIRFilter) {
	    //decrement the schedule execution counter
	    int oldVal = ((Integer)executionCounts.get(fire)).intValue();
	    if (oldVal - 1 < 0)
		Utils.fail("Executed too much");
	    executionCounts.put(fire, new Integer(oldVal - 1));
	    
	    //take the values off the buffer
	    //take off peek values on the first invocation
	    if (!counters.hasFired(fire))
		counters.decrementBufferCount(fire, 
					      ((SIRFilter)fire.contents).
					      getPeekInt());
	    else 
		counters.decrementBufferCount(fire, 
					      ((SIRFilter)fire.contents).
					      getPopInt());
	    
	    //now this node has fired
	    counters.setFired(fire);
	}
	else if (fire.contents instanceof SIRJoiner) {
	    //System.out.println("Firing a joiner");
	    
	    JoinerScheduleNode previous = 
		(JoinerScheduleNode)currentJoinerCode.get(fire);
	    JoinerScheduleNode current = new JoinerScheduleNode();
	    current.buffer = counters.getJoinerBuffer(fire);
	    current.type = JoinerScheduleNode.FIRE;
	    previous.next = current;
	    //set current
	    currentJoinerCode.put(fire, current);
	    //decrement the buffer
	    counters.decrementJoinerBufferCount(fire, counters.getJoinerBuffer(fire));
	    //step the schedule
	    counters.incrementJoinerSchedule(fire);
	}
    }
    
    
	 
	    

    //get the destination of the data item
    private  List getDestination(FlatNode node, SimulationCounter counters, String
				 joinerBuffer, FlatNode previous) 
    {
	//if we reached a node then this is a destination
	//add to its buffer and
	//create a list and add it
	if (node.contents instanceof SIRFilter) {
	    counters.incrementBufferCount(node);
	    LinkedList list = new LinkedList();
	    list.add(node);
	    return list;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //just pass thru joiners except the joiners that are the 
	    //last joiner in a joiner group
	    //this list is kept in the layout class
	    if (Layout.joiners.contains(node)) {
		joinerBuffer = joinerBuffer + getJoinerBuffer(node, previous);
		
		//System.out.println("Destination is a buffer " + joinerBuffer);
		
		//add the item to the appropiate buffer
		//as determined by the simulation
		counters.incrementJoinerBufferCount(node, joinerBuffer);
		LinkedList list = new LinkedList();
		list.add(node);
		//add to the joiner code for this node
		JoinerScheduleNode prev = 
		    (JoinerScheduleNode)currentJoinerCode.get(node);
		JoinerScheduleNode current = new JoinerScheduleNode();
		current.buffer = joinerBuffer;
		current.type = JoinerScheduleNode.RECEIVE;
		if (prev == null) {
		    //first node in joiner code
		    joinerCode.put(node, current);
		}
		else {
		    //connect 
		    prev.next = current;
		}
		//set current
		currentJoinerCode.put(node, current);
		return list;
	    }
	    else {
		return getDestination(node.edges[0], counters, 
				      joinerBuffer + getJoinerBuffer(node, 
								     previous),
				      node);
	    }
	}
	else if (node.contents instanceof SIRSplitter) {
	    //here is the meat
	    SIRSplitter splitter = (SIRSplitter)node.contents;
	    //if splitter send the item out to all arcs
	    //build a list of all the dests
	    if (splitter.getType() == SIRSplitType.DUPLICATE) {
		LinkedList list = new LinkedList();
                for (int i = 0; i < node.ways;i++) {
		    //decrement counter on arc
		    if (counters.getArcCountOutgoing(node, i) == 0)
			counters.resetArcCountOutgoing(node, i);
		    counters.decrementArcCountOutgoing(node, i);
		    list.add(getDestination(node.edges[i], 
					    counters, joinerBuffer,
					    previous));
		}
		return list;
	    }
	    else {
		//weighted round robin
		for (int i = 0; i < node.ways; i++) {
		    if (counters.getArcCountOutgoing(node, i) > 0) {
			counters.decrementArcCountOutgoing(node, i);
			return getDestination(node.edges[i], 
					      counters, joinerBuffer,
					      previous);
		    }
		}
		//none were greater than zero, reset all counters
		//and send to the first
		for (int i = 0; i < node.ways; i++) {
		    counters.resetArcCountOutgoing(node, i);
		}
		counters.decrementArcCountOutgoing(node, 0);
		return getDestination(node.edges[0], counters,
				      joinerBuffer, previous);
	    }
	    
	}
	else {
	    throw new RuntimeException("SimulateDataItem");
	}
	//return null;
    }

    //for now, find the most-downstream filter to fire
    private  FlatNode whoShouldFire(HashMap executionCounts, 
				    SimulationCounter counters) 
    {
	//breadth first search from bottom
	HashSet visited = new HashSet();
	Vector queue = new Vector();
	FlatNode node;
		
	queue.add(bottom);
	while (!queue.isEmpty()) {
	    node = (FlatNode)queue.get(0);
	    visited.add(node);
	    if (canFire(node, executionCounts, counters)) 
		return node;
	    queue.remove(0);
	    for (int i = 0; i < node.inputs; i++) {
		if (!visited.contains(node.incoming[i]))
		    queue.add(node.incoming[i]);
	    }
	}
	//no node can fire
	return null;
    }
    
    private boolean canFire(FlatNode node, HashMap executionCounts, 
			    SimulationCounter counters) 
    {
	//the only thing that can fire is a filter
	if (node.contents instanceof SIRFilter) {
	    //check if this node has fired the number of times given by
	    //the schedule
	    
	    //System.out.println("Checking execution count: " + 
	    //Namer.getName(node.contents));
	    
	    Integer count = (Integer)executionCounts.get(node);
	    if (count == null)
		return false;
	    if (count.intValue() == 0)
		return false;
	    
	    //on the first execution we must consume peek items
	    //counters.fired tells us if we have fired already
	    int requirement;
	    if (!counters.hasFired(node))
		requirement = ((SIRFilter)node.contents).getPeekInt();
	    else
		requirement = ((SIRFilter)node.contents).getPopInt();
	    
	    //System.out.println("canFire: " + Namer.getName(node.contents) +
	    //		       " req: " + requirement + " buf: " + 
	    //		       counters.getBufferCount(node));
	    

	    if (counters.getBufferCount(node) >= requirement) {
		return true;
	    }
	    else
		return false;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //determine if a joiner can fire
	    //if the buffer associated with its current 
	    //input has an item in it
	    if (counters.getJoinerBufferCount(node, counters.
					      getJoinerBuffer(node)) > 0)
		return true;
	    else
		return false;
	}
	else 
	    return false;
    }
    
    
	
    //look for bottom node
    public void visitNode(FlatNode node) 
    {
	if (node.ways == 0) {
	    if (bottom != null)
		Utils.fail("Simulator found > 1 bottom nodes...");
	    else 
		bottom = node;
	}
    }
    
    private String getJoinerBuffer(FlatNode node, FlatNode previous) 
    {
	for (int i = 0; i < node.inputs; i++) {
	    if (node.incoming[i] == previous)
		return String.valueOf(i);
	}
	
	Utils.fail("cannot find previous node in joiner list");
	return null;
    }
 
    private static void printExecutionCounts(HashMap map) 
    {
	System.out.println();

	Iterator it = map.keySet().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode)it.next();
	    System.out.println(Namer.getName(node.contents) + " " + 
			       ((Integer)map.get(node)).toString());
	}
	System.out.println();
	
    }
       
}

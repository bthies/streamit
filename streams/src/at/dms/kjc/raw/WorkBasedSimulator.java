package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
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
import at.dms.kjc.sir.lowering.partition.WorkEstimate;

/**
 * This class generates a schedule for the switch code by simulating the 
 * init schedule and one
 * steady state execution of the schedule
 */
public class WorkBasedSimulator extends Simulator  implements FlatVisitor
{
    private HashMap switchSchedules;
    
    //the current joiner code we are working on (steady or init)
    private static HashMap joinerCode;
        
    //the curent node in the joiner schedule we are working on
    private HashMap currentJoinerCode;
    
    private FlatNode bottom;

    //true if we are simulating the init schedule
    private boolean initSimulation;


    //variable for the event-driven simulation
    private int currentTime;
    //the heap of waiting to execute events
    private EventHeap eventHeap;
    //all the nodes that are currently executing...
    //well this is really all the nodes that have received enough data
    //to fire and cannot receive any more
    private HashSet firingNodes;
    //the queue of items ready to be injected onto the network...
    //the pendingQueue needs to be a linked list so that we pop off of it in the same order 
    //we added to it...FIFO
    private LinkedList pendingQueue;
    
    private WorkEstimatesMap workEstimatesMap;

    public void simulate(FlatNode top) 
    {
	System.out.println("WorkBasedSimulator Running...");
	
	initJoinerCode = new HashMap();
        steadyJoinerCode = new HashMap();

	//generate the joiner schedule
	JoinerSimulator.createJoinerSchedules(top);
	
	SimulationCounter counters = 
	    new SimulationCounter(JoinerSimulator.schedules);
	
	
	//create copies of the executionCounts
	HashMap initExecutionCounts = (HashMap)RawBackend.initExecutionCounts.clone();
	HashMap steadyExecutionCounts = (HashMap)RawBackend.steadyExecutionCounts.clone();

	//get the workestimates and put them in the hashmap
	WorkEstimatesMap estimates = new WorkEstimatesMap(top);

	joinerCode = initJoinerCode;
	
	//	System.out.println("\n\nInit Execution Counts");
	//RawBackend.printCounts(RawBackend.initExecutionCounts);
	
	//the init simulation stays the same, based on a furthest downstream will fire
	//one will notice all of the if (initSimulation) cases in the functions
	initSchedules = 
	    (new WorkBasedSimulator(top, estimates, true)).
	    goInit(initExecutionCounts, counters, null);
	testExecutionCounts(initExecutionCounts);
	System.out.println("End of init simulation");
	
	//	System.out.println("\n\nSteady Execution Counts");
	//RawBackend.printCounts(RawBackend.steadyExecutionCounts);

	//reset the necessary counters in the simulation, such as the 
	//execution counts
	counters.resetBuffers();

	//now run the simulator based on the work estimation
	joinerCode = steadyJoinerCode;
	steadySchedules = 
	    (new WorkBasedSimulator(top, estimates, false)).
	    go(steadyExecutionCounts, counters, null);
	testExecutionCounts(steadyExecutionCounts);
	System.out.println("End of steady-state simulation");
    }
    
    
    public WorkBasedSimulator() {
    }
    

    private WorkBasedSimulator(FlatNode top, WorkEstimatesMap estimates, boolean init) 
    {
	switchSchedules = new HashMap();
	currentJoinerCode = new HashMap();
	toplevel = top;
	workEstimatesMap = estimates;
	//find the bottom (last) filter, used later to decide
	//execution order
	bottom = null;
	initSimulation = init;
    }
    
    /*
      This function tests to see if a simulation of a schedule has executed
      to its completion.  It checks if all the execution counts for 
      mapped streams are 0
    */
    private static void testExecutionCounts(HashMap exeCounts) 
    {
	boolean bad = false;
	
	Iterator it = exeCounts.keySet().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode)it.next();
	    if (Layout.isAssigned(node)) {
		if (((Integer)exeCounts.get(node)).intValue() != 0) {
		    System.out.println(node.contents.getName() + " has " + 
				       exeCounts.get(node) + " executions remaining!!!!");
		    bad = true;
		}
	    }
	}
	if (bad)
	    Utils.fail("Error in simulator.  Some nodes did not execute.  See above...");
	
    }
     /*
      This function tests to see if a simulation of a schedule has executed
      to its completion.  It checks if all the execution counts for 
      mapped streams are 0
    */
    private boolean nonZeroExeCounts(HashMap exeCounts) 
    {
	boolean bad = false;
	
	Iterator it = exeCounts.keySet().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode)it.next();
	    if (Layout.getTile(node) != null) {
		if (((Integer)exeCounts.get(node)).intValue() != 0) {
		    bad = true;
		}
	    }
	}
	
	return bad;
    }

    /*
      This function is called before the init simulation is run.  It creates the code
      in the joiner code to call the initpath function and place the results in the
      correct buffer of the joiner
    */
    private void callInitPaths(SimulationCounter counters) 
    {
	//find all the joiners that are immediately contained in a FeedbackLoop
	Iterator joiners = Layout.getJoiners().iterator();
	//clone the joiner schedules
	
	FlatNode joiner;
	//iterate over all of the joiners of a feedbackloop
	while (joiners.hasNext()) {
	    joiner = (FlatNode)joiners.next();
	    if ((((SIRJoiner)joiner.contents).getParent() instanceof SIRFeedbackLoop)) {
		//create the initPath calls 
		SIRFeedbackLoop loop = (SIRFeedbackLoop)((SIRJoiner)joiner.contents).getParent();
		int delay = loop.getDelayInt();
		JoinerScheduleNode current = ((JoinerScheduleNode)JoinerSimulator.schedules.get(joiner));
		
		for (int i = 0; i < delay; i++) {
		    //for each init path call find the correct buffer to place it in.
		    while(true) {
			if (current.buffer.endsWith("1")) {
			    //create the joinerCode Node and put it in the init schedule
			    JoinerScheduleNode prev = 
				(JoinerScheduleNode)currentJoinerCode.get(joiner);
			    JoinerScheduleNode code = new JoinerScheduleNode(i, current.buffer);
			    if (prev == null) {
				//first node in joiner code
				//this will add it to the joiner code hashmap
				//this hashmap store the first instruction of each code sequence
				joinerCode.put(joiner, code);
			    }
			    else {
				//connect to the prev
				prev.next = code;
			    }
			    //set current
			    currentJoinerCode.put(joiner, code);
			    //record that a data item was placed in this buffer
			    counters.incrementJoinerBufferCount(joiner, current.buffer);
			    //we found a buffer so break and place the next initPath call
			    current = current.next;
			    break;
			}
			current = current.next;
		    }
		}
	    }
	}
    }
    

    //The start of the simulation for the initialization schedule
    private HashMap goInit(HashMap counts, SimulationCounter counters, FlatNode lastToFire) 
    {
	//create the initpath calls
	callInitPaths(counters);
	return go(counts, counters, lastToFire);
    }

    /* the main simulation method */
    private HashMap go(HashMap counts, SimulationCounter counters, FlatNode lastToFire) 
    {
	currentTime = 0;
	eventHeap = new EventHeap();
	firingNodes = new HashSet();
	pendingQueue = new LinkedList();

	//this will add the event for the source nodes
	addEvents(counts, counters, this);
	
	//the simulation loop
	do {
	    /*	    
		   System.out.println("-----------------------");
		   System.out.println("Pending:");
		   Iterator pit = pendingQueue.iterator();
		   while (pit.hasNext()) {
		   SimulatorEvent e = (SimulatorEvent)pit.next();
		   System.out.println(e.node + " " + e.time + " " + e.isLast);
		   }
		   System.out.println("--------");
		   System.out.println("Scheduled");
		   Iterator sit = eventHeap.iterator();
		   while (sit.hasNext()) {
		   SimulatorEvent e = (SimulatorEvent)sit.next();
		   System.out.println(e.node + " " + e.time + " " + e.isLast);
		   }
		   System.out.println("--------");
	    */
	    //get next node to fire and fire it
	    SimulatorEvent event = eventHeap.getNextEvent();
	    if (event != null) {
		//increment the simulation time
		currentTime = event.time;
		FlatNode fire = event.node;
		//System.out.println("sending from " + event.node.contents.getName() + " " + currentTime);
	
		//update the simulation state to reflect the sent item...
		sendItemUpdate(fire, event.dests, counts, counters);

		//this node is finished firing, remove it from the list of firing nodes...
		if (event.isLast)
		    firingNodes.remove(fire);
	    }	    
	    //add events for nodes that can fire...
	    addEvents(counts, counters, this);
	    /*
	      if (!pendingQueue.isEmpty() && eventHeap.isEmpty()) {
	      Iterator it = pendingQueue.iterator();
	      while (it.hasNext()) {
	      SimulatorEvent e = (SimulatorEvent)it.next();
	      System.out.println(e.node + " " + e.time + " " + e.isLast);
	      }
	    }
	    */	    
	} while (!eventHeap.isEmpty() || !pendingQueue.isEmpty());
	
	//	System.out.println(firingNodes);

	return switchSchedules;
    }

    private void sendItemUpdate(FlatNode src, List dests, HashMap counts, SimulationCounter counters) 
    {
	//create the joiner schedule node that will place this item on the switch processor
	//extracting it from the proper buffer...
	if (src.isJoiner()) {
	    //this joiner fired because it has data that can be sent downstream
	    JoinerScheduleNode current = new JoinerScheduleNode();
	    current.buffer = counters.getJoinerBuffer(src);
	    current.type = JoinerScheduleNode.FIRE;
	    addJoinerCode(src, current);
	    //decrement the buffer
	    counters.decrementJoinerBufferCount(src, counters.getJoinerBuffer(src));
	    //step the schedule
	    counters.incrementJoinerSchedule(src);

	}
	/*
	Iterator destsIt = dests.iterator();
	LinkedList realDests = new LinkedList();
	//now, update the necessary state at the destination
	while (destsIt.hasNext()) {
	    FlatNode dest = (FlatNode)destsIt.next();
	    if (dest.contents instanceof SIRIdentity) {
		realDests.addAll(updateIdentityDestinations(dest,
							    counters));
	    }
	    else {
		realDests.add(dest);
	    }   
	}

	*/
	
	List realDests = updateDestinations(src, dests.iterator(), counters);

	if (KjcOptions.magic_net) {
	    //generating code for the raw magic network
	    appendMagicNetNodes(src, realDests);
	}
	else {
	    //not generating code for the magic network
	    //generate switch code for all intermediate
	    //hops
	    generateSwitchCode(src, realDests);
	} 
    }
    
    private List getIdentityDestinations(FlatNode identity, SimulationCounter sCounters) 
    {
	if (!(identity.contents instanceof SIRIdentity)) 
	    Utils.fail("Calling getIdentityDestinations() on non-SIRIdentity");

	Iterator dests = getDestination(identity, sCounters).iterator();
	LinkedList realDests = new LinkedList();
	while(dests.hasNext()) {
	    FlatNode dest = (FlatNode)dests.next();
	    if (Layout.isAssigned(dest)) {
		realDests.add(dest);
	    }
	    else {
		realDests.addAll(getIdentityDestinations(dest, sCounters));
	    }
	}
	return realDests;
    }

    private List updateDestinations(FlatNode src, Iterator dests, SimulationCounter counters) 
    {
	//remember what we visited for duplicate identity removal
	//updated in updateJoinerDestination
	HashSet visited = new HashSet();
	LinkedList realDests = new LinkedList();

	while (dests.hasNext()) {
	    FlatNode dest = (FlatNode)dests.next();
	    if (dest.contents instanceof SIRIdentity) {
		Iterator idDests = getIdentityDestinations(dest, counters).iterator();
		while (idDests.hasNext()) {
		    FlatNode idDest = (FlatNode)idDests.next();
		    counters.incrementBufferCount(idDest);
		    realDests.add(idDest);
		    if (idDest.isJoiner())
			updateJoinerDestination(visited, dest, idDest, counters);    
		}
	    }
	    else {
		realDests.add(dest);
		counters.incrementBufferCount(dest);
		if (dest.isJoiner()) {
		    updateJoinerDestination(visited, src, dest, counters);
		}
	    }
	}
	return realDests;
    }

    private void updateJoinerDestination(HashSet visited, FlatNode src, FlatNode dest,
					 SimulationCounter counters) 
    {
	counters.decrementBufferCount(dest, 1);
	//get the joiner buffer as determined by getDestination and stored in a list 
	//		String joinerBuffer = counters.getJoinerReceiveBuffer(dest);
	//System.out.println(src.contents.getName() + " to joiner Buffer (Receive): " + joinerBuffer);//
	
	//try this bitches!!!!
	//get the buffer this item is being sent to..
	String joinerBuffer = buildJoinerBufferString(src, src.edges[0], dest);
	//record that the data was placed in this buffer...
	counters.incrementJoinerBufferCount(dest, joinerBuffer);
	//add to the joiner code for this dest
	JoinerScheduleNode current = new JoinerScheduleNode();
	current.buffer = joinerBuffer;
	//if we have seen this dest already, then we are passing
	//thru a duplicate splitter with identity filters...
	//create the code to duplicate the item inside the joiner
	if (visited.contains(dest))
	    current.type = JoinerScheduleNode.DUPLICATE;
	else //otherwise, normal receive
	    current.type = JoinerScheduleNode.RECEIVE;
	addJoinerCode(dest, current);
	
	visited.add(dest);
    }
    
    
    

    private void addEvents(HashMap exeCounts, SimulationCounter sCounters, Simulator sim)
    {
	buildandQueueEvent(exeCounts, sCounters, sim);
	moveFromPendingtoScheduled(exeCounts, sCounters);
    }
    
    //put them in the queue of pending sends.  These
    //sends are waiting to be scheduled 
    private void buildandQueueEvent(HashMap exeCounts, SimulationCounter sCounters, Simulator sim) 
    {
	int items = 0;
	
	//find all nodes that can fire, without 
	//regard to if they are already scheduled to fire
	do {
	    FlatNode node = nextToFire(exeCounts, sCounters);
	    
	    //	    System.out.println("firing " + node);
	    
	    if (node == null)
		return;
	    
	    
	    //perform the firing
	    //keep track of everything needed when a node fires
	    items = fireMe(node, sCounters, exeCounts);
	    
	    for (int i = 0; i < items; i++) {
		//if this is the last item, set isLast to true
		boolean isLast = (i == items - 1);
		
		SimulatorEvent event = new SimulatorEvent("send", i, node, 
							  getDestination(node, sCounters), 
							  eventHeap.getItemId(node),
							  isLast);
		
		//		System.out.println("Creating Event " + node.contents.getName() + " " + i);
		pendingQueue.add(event);
		
	    }
	    
	    //add this node to the list of firing nodes...
	    if (items > 0)
		firingNodes.add(node);
	    //if items was less then 1, nothing was added to the 
	    //pending queue, so find another filter to fire, if possible
	} while (items < 1);
	
    }
    
    private boolean earlierEventsWaiting(SimulatorEvent tryToSched) 
    {
	Iterator it = pendingQueue.iterator();
	while (it.hasNext()) {
	    SimulatorEvent current = (SimulatorEvent)it.next();
	    if (current == tryToSched) 
		break;
	    if (current.node == tryToSched.node &&
		current.itemID < tryToSched.itemID) 
		return true;
	}
	return false;
    }
    

    //move earliest event from the pending queue and schedule them to be fired...
    private void moveFromPendingtoScheduled(HashMap exeCounts, SimulationCounter sCounters) 
    {
	Iterator it = pendingQueue.iterator();
	//This set will store all the events we scheduled on this call,
	//so we can remove them from the pending queue
	HashSet scheduled = new HashSet();	

	//iterate over the pending queue
	//and schedule one event
	while (it.hasNext()) {
	    SimulatorEvent event = (SimulatorEvent)it.next();
	    
	    //do not schedule if earlier items from the same src have not 
	    //been scheduled...
	    if (earlierEventsWaiting(event))
		continue;

	    //check that its downstream dest(s) is not firing...
	    //event.dests = getAssignedDests(event.dests, sCounters);
	    Iterator dests = event.dests.iterator();
	    boolean waitForDest = false;

	    //do not schedule the item if its dests are firing...
	    while (dests.hasNext()) {
		FlatNode dest = (FlatNode)dests.next();
		if (dest.contents instanceof SIRIdentity) {
		    waitForDest = waitForIdentityDests(dest, exeCounts, sCounters);
		}
		if (firingNodes.contains(dest) || canFire(dest, exeCounts, sCounters)) {
		    waitForDest = true;
		    //System.out.println("Cannot Schedule " + event.node.contents.getName() +
		    //		       "because of dest " + dest.contents.getName());
		    break;
		}
	    }

	    
	    //need to wait for the dest(s) to be ready to receive
	    //so do not schedule this event
	    //look for others to add to the event queue
	    if (waitForDest)
		continue;
	    
	    //add to the scheduled set
	    scheduled.add(event);
	    //we can add this event now...
	    addEvent(event);
	    //	    System.out.println("Scheduling " + event.node + " " + event.time);
	    break;
	}
	//iterate over the newly scheduled events and remove them from the 
	//pending queue
	Iterator remove = scheduled.iterator();
	while (remove.hasNext())
	    pendingQueue.remove(remove.next());
    }
    
    private boolean waitForIdentityDests(FlatNode identity, 
					 HashMap exeCounts, SimulationCounter sCounters)
    {
	Iterator upstream = Util.getAssignedEdges(identity).iterator();

	while (upstream.hasNext()) {
	    FlatNode current = (FlatNode)upstream.next();
	    if (firingNodes.contains(current) || canFire(current, exeCounts, sCounters))
		return true;
	}
	
	//no one firing upstream or 
	return false;
    }
    

    /*
    private LinkedList getAssignedDests(List dests, SimulationCounter counters) 
    {
	Iterator destIt = dests.iterator();
	LinkedList realDests = new LinkedList();
	
	while (destIt.hasNext()) {
	    FlatNode dest = (FlatNode)destIt.next();
	    if (dest.contents instanceof SIRIdentity) {
		Iterator idDests = getIdentityDestinations(dest, counters).iterator();
		while (idDests.hasNext()) {
		    FlatNode idDest = (FlatNode)idDests.next();
		    if (idDest.contents instanceof SIRJoiner) {
			String joinerBuffer = buildJoinerBufferString(dest, dest.edges[0], idDest);
			
		    }
		    realDests.add(idDest);
		}
	    }
	    else 
		realDests.add(dest);
	}
	
	return realDests;
    }
    */
    
    //add the event to the event heap
    private void addEvent(SimulatorEvent event) 
    {
	//add to the time of the event, the current time and the work estimation
	event.time += (currentTime + getWorkEstimate(event.node));
	eventHeap.addEvent(event);
    }
    
    private FlatNode nextToFire(HashMap exeCounts, SimulationCounter sCounters) 
    {
	Iterator trav = BreadthFirstTraversal.getTraversal(toplevel).iterator();
	
	while (trav.hasNext()) {
	    FlatNode node = (FlatNode)trav.next();
	
	    /*	    
	    if (canFire(node, exeCounts, sCounters)) {
		System.out.println(node + " " + !firingNodes.contains(node) + " " + 
				   checkDownStream(node, exeCounts, sCounters, new HashSet()));
	    }
	    */
		
	    //check if it can fire
	    if (Layout.isAssigned(node) &&
		canFire(node, exeCounts, sCounters) &&
		!firingNodes.contains(node)) 
		if (node.ways == 0 || 
		    (node.ways > 0 && checkDownStream(node, exeCounts, sCounters, new HashSet())))
		    return node;
	}
	//found nothing
	return null;
    }
    
    private boolean checkDownStream(FlatNode node, HashMap exeCounts, SimulationCounter sCounters, 
				    HashSet visited) 
    {
	if (visited.contains(node))
	    return true;
	//	System.out.println("Checking down stream:" + node);
	visited.add(node);
	
	Iterator downstream = Util.getAssignedEdges(node).iterator();
	
	if (!downstream.hasNext())
	    return true;

	while (downstream.hasNext()) {
	    FlatNode current = (FlatNode)downstream.next();
	    
	    //if we visited this node before and did not return false
	    //then return true now...
	    if (visited.contains(current))
		return true;
	    /*
	    System.out.println("   Checking current: " + !canFire(current, exeCounts, sCounters)
			       + " " + !firingNodes.contains(current));
	    */
	    if (!canFire(current, exeCounts, sCounters) && !firingNodes.contains(current) &&
		checkDownStream(current, exeCounts, sCounters, visited))
		return true;
	}
	
	return false;
    }
    
	
    //get the work estimation for a filter or joiner, a joiner defaults to 1 work cycle
    private int getWorkEstimate(FlatNode node)
    {
	if (node.isFilter() || node.isJoiner())
	    return workEstimatesMap.getEstimate(node);

	Utils.fail("Trying to get work estimation for non-filter/joiner");
	return -1;
    }

    private void appendMagicNetNodes(FlatNode fire, List dests) {
	Coordinate source = Layout.getTile(fire);
	
	HashMap receiveSchedules = MagicNetworkSchedule.steadyReceiveSchedules;
	HashMap sendSchedules = MagicNetworkSchedule.steadySendSchedules;
	
	//append the current information to the correct schedule
	//depending of if it is the steady state or init
	if (initSimulation) {
	    receiveSchedules = MagicNetworkSchedule.initReceiveSchedules;
	    sendSchedules = MagicNetworkSchedule.initSendSchedules;
	}
	
	//if the source schedule does not exist create it
	if (!sendSchedules.containsKey(source))
	    sendSchedules.put(source, new LinkedList());
	
	LinkedList sourceSendSchedule = (LinkedList)sendSchedules.get(source);

	//generate a list of coordinates to add to the send schedule for the source
	LinkedList destsCoordinate = new LinkedList();


	//iterate thru the dests adding to the receive schedules for the dests
	Iterator it = dests.iterator();
	while(it.hasNext()) {
	    Coordinate currentDest = Layout.getTile((FlatNode)it.next());
	
	    
	    if (!receiveSchedules.containsKey(currentDest))
		receiveSchedules.put(currentDest, new LinkedList());
	    LinkedList destReceiveSchedule = (LinkedList)receiveSchedules.get(currentDest);
	    
	    destReceiveSchedule.add(source);

	    //add to the list of coordinate dests
	    destsCoordinate.add(currentDest);
	}

	//add the list of coordinates to the source send schedule
	sourceSendSchedule.add(destsCoordinate);
    }


    //generate the switch code for 1 data item given the list of destinations
    //we do not want to duplicate items until necesary, so we have to keep track 
    //of all the routes and then generate the switch code
    //this way we can route multiple dests per route instruction
    private void generateSwitchCode(FlatNode fire, List dests) 
    {
	//should only have one previous
	HashMap prev = new HashMap();
	HashMap next = new HashMap();

	//	System.out.print("  Send from " + fire);

	ListIterator destsIt = dests.listIterator();
	while (destsIt.hasNext()) {
 	    FlatNode dest = (FlatNode)destsIt.next();
	    //	    System.out.print(" to " + dest);
	    //	    System.out.println("Dest: " + dest.getName());
	    if (fire == null)
		System.out.println("Yup Fire is null");
	    if (dest == null) 
		System.out.println("Yup dest is null");
 	    Coordinate[] hops = 
 		(Coordinate[])Router.getRoute(fire, dest).toArray(new Coordinate[0]);
	    //add to fire's next
	    if (!next.containsKey(Layout.getTile(fire))) 
		next.put(Layout.getTile(fire), new HashSet());
	    ((HashSet)next.get(Layout.getTile(fire))).add(hops[1]);
	    //add to all other previous, next
	    for (int i = 1; i < hops.length -1; i++) {
		if (prev.containsKey(hops[i]))
		    if (prev.get(hops[i]) != hops[i-1])
			Utils.fail("More than one previous tile for a single data item");
		prev.put(hops[i], hops[i-1]);
		if (!next.containsKey(hops[i]))
		    next.put(hops[i], new HashSet());
		((HashSet)next.get(hops[i])).add(hops[i+1]);
	    }
	    //add the last step, plus the dest to the dest map
	    if (prev.containsKey(hops[hops.length - 1]))
		if (prev.get(hops[hops.length - 1]) != hops[hops.length - 2])
		    Utils.fail("More than one previous tile for a single data item (2)");
	    prev.put(hops[hops.length-1], hops[hops.length - 2]);
	    if (!next.containsKey(hops[hops.length-1]))
		next.put(hops[hops.length - 1], new HashSet());
	    ((HashSet)next.get(hops[hops.length - 1])).add(hops[hops.length -1]);
	}
	
	//create the appropriate amount of routing instructions
	int elements = Util.getTypeSize(Util.getOutputType(fire));
	for (int i = 0; i < elements; i++)
	    asm(Layout.getTile(fire), prev, next);
	
    }
    
    private void asm(Coordinate fire, HashMap previous, HashMap next) 
    {
	//generate the sends
	if (!switchSchedules.containsKey(fire))
	    switchSchedules.put(fire, new StringBuffer());
	StringBuffer buf = (StringBuffer)switchSchedules.get(fire);
	Iterator it = ((HashSet)next.get(fire)).iterator();
	buf.append("route ");
	while (it.hasNext()) {
	    Coordinate dest = (Coordinate)it.next();
	    buf.append("$csto->" + "$c" + 
		       Layout.getDirection(fire, dest) + 
		       "o,");
	}
	//erase the trailing ,
	buf.setCharAt(buf.length() - 1, '\n');
	
	//generate all the other 
	Iterator tiles = next.keySet().iterator();
	while (tiles.hasNext()) {
	    Coordinate tile = (Coordinate)tiles.next();
	    if (tile == fire) 
		continue;
	    if (!switchSchedules.containsKey(tile))
		switchSchedules.put(tile, new StringBuffer());
	    buf = (StringBuffer)switchSchedules.get(tile);
	    Coordinate prevTile = (Coordinate)previous.get(tile);
	    buf.append("route ");	    Iterator nexts = ((HashSet)next.get(tile)).iterator();
	    while(nexts.hasNext()) {
		Coordinate nextTile = (Coordinate)nexts.next();
		if (!nextTile.equals(tile))
		    buf.append("$c" + Layout.getDirection(tile, prevTile) + "i->$c" +
			       Layout.getDirection(tile, nextTile) + "o,");
		else 
		    buf.append("$c" + Layout.getDirection(tile, prevTile) + "i->$c" +
			       Layout.getDirection(tile, nextTile) + "i,");
	    }
	    buf.setCharAt(buf.length() - 1, '\n');
	}
    }

    private int itemsNeededToFire(FlatNode fire, SimulationCounter counters,
				  HashMap executionCounts) 
    {
	//if this is the first time a two stage initpeek is needed to execute
	if (initSimulation &&
	    !counters.hasFired(fire) &&
	    fire.contents instanceof SIRTwoStageFilter) {
	    return ((SIRTwoStageFilter)fire.contents).getInitPeek();
	}
	else if (!initSimulation && KjcOptions.ratematch && 
		 fire.contents instanceof SIRFilter) {
	    //we are ratematching filters
	    return (((SIRFilter)fire.contents).getPopInt() * 
		((Integer)RawBackend.steadyExecutionCounts.get(fire)).intValue() +
		(((SIRFilter)fire.contents).getPeekInt() -
		 ((SIRFilter)fire.contents).getPopInt()));
	}
	//otherwise peek items are needed
	return ((SIRFilter)fire.contents).getPeekInt();
    }
   
    private void decrementExecutionCounts(FlatNode fire, HashMap executionCounts, SimulationCounter counters) 
    {
	//decrement one from the execution count
	int oldVal = ((Integer)executionCounts.get(fire)).intValue();
	if (oldVal - 1 < 0)
	    Utils.fail("Executed too much");
	
	//if we are ratematching the node only fires once but only do this
	//for filters
	if (!initSimulation && KjcOptions.ratematch && 
	    fire.contents instanceof SIRFilter) { 
	    executionCounts.put(fire, new Integer(0));
	} 
	else 
	    executionCounts.put(fire, new Integer(oldVal - 1));
    }
    
    private int consumedItems(FlatNode fire, SimulationCounter counters,
			      HashMap executionCounts) {
	//if this is the first time a two stage fires consume initpop
	if (initSimulation &&
	    !counters.hasFired(fire) &&
	    fire.contents instanceof SIRTwoStageFilter)
	    return ((SIRTwoStageFilter)fire.contents).getInitPop();
	else if (!initSimulation && KjcOptions.ratematch &&
		 fire.contents instanceof SIRFilter) {
	    //we are ratematching on the filter
	    //it consumes for the entire steady state
	    return ((SIRFilter)fire.contents).getPopInt() *
		((Integer)RawBackend.steadyExecutionCounts.get(fire)).intValue();
	}
	//otherwise just consume pop
	return ((SIRFilter)fire.contents).getPopInt();
    }
    
    //consume the data and return the number of items produced
    private int fireMe(FlatNode fire, SimulationCounter counters, HashMap executionCounts) 
    {
	if (fire.contents instanceof SIRFilter) {
	    //decrement the schedule execution counter
	    decrementExecutionCounts(fire, executionCounts, counters);
	    
	    //consume the date from the buffer
	    counters.decrementBufferCount(fire, 
					  consumedItems(fire, counters, executionCounts));	 

	    //for a steady state execution return the normal push
	    int ret = ((SIRFilter)fire.contents).getPushInt();
	    
	    //if the filter is a two stage, and it has not fired
	    //return the initPush() unless the initWork does nothing
	    if (initSimulation &&
		!counters.hasFired(fire) &&
		fire.contents instanceof SIRTwoStageFilter)
		ret = ((SIRTwoStageFilter)fire.contents).getInitPush();
	    else if (!initSimulation && KjcOptions.ratematch) {
		//we are ratematching so produce all the data on the one firing.
		ret *= ((Integer)RawBackend.steadyExecutionCounts.get(fire)).intValue();
	    }
	    //now this node has fired
	    counters.setFired(fire);
	    return ret;
	}
	else if (fire.contents instanceof SIRJoiner) {
	    return fireJoiner(fire, counters, executionCounts);
	}
    
	Utils.fail("Trying to fire a non-filter or joiner");
	return -1;
    }
    
    /*
      add the joiner code to the code schedule for the given joiner
    */
    private void addJoinerCode(FlatNode fire, JoinerScheduleNode code) 
    {
	//add to the joiner code for this fire
	    JoinerScheduleNode prev = 
		(JoinerScheduleNode)currentJoinerCode.get(fire);
	    if (prev == null) {
		//first node in joiner code
		joinerCode.put(fire, code);
	    }
	    else {
		//connect 
		prev.next = code;
	    }
	    //set code
	    currentJoinerCode.put(fire, code);
    }
    
    
    
    private int fireJoiner(FlatNode fire, SimulationCounter counters, HashMap executionCounts)
    {
	//	System.out.println("Firing " + fire.contents.getName());
	//The joiner is passing a data item, record this as an execution
	decrementExecutionCounts(fire, executionCounts, counters);
	
	if (false) {
	    //this joiner fired because it has data that can be sent downstream
	    JoinerScheduleNode current = new JoinerScheduleNode();
	    current.buffer = counters.getJoinerBuffer(fire);
	    current.type = JoinerScheduleNode.FIRE;
	    addJoinerCode(fire, current);
	    //decrement the buffer
	    counters.decrementJoinerBufferCount(fire, counters.getJoinerBuffer(fire));
	    //step the schedule
	    counters.incrementJoinerSchedule(fire);
	}
	
	return 1;
    }


    //return the destinations of the data item and generate the code
    //to receive data into the joiner
    private List getDestination(FlatNode node, SimulationCounter counters)
    {
	return getDestinationHelper(node.edges[0], counters, "", node);
    }
    
    //call with src, src.edges[0], dest
    private String buildJoinerBufferString(FlatNode previous, FlatNode current, FlatNode dest) 
    {
	if (dest == current)
	    return getJoinerBuffer(current, previous);
	
	return getJoinerBuffer(current, previous) + buildJoinerBufferString(current,
									    current.edges[0], 
									    dest);
    }
	    
    //get the destination of the data item
    private List getDestinationHelper(FlatNode node, SimulationCounter counters, String
				 joinerBuffer, FlatNode previous) 
    {

	if (node.contents instanceof SIRIdentity &&
	    node.edges[0].contents instanceof SIRIdentity) {
	    //pass thru multiple conntected identities
	    getDestinationHelper(node.edges[0], counters, joinerBuffer, node);
	}
	else if (node.contents instanceof SIRFilter) {
	    //if we reached a node then this is a destination
	    //add to its buffer and
	    //create a list and add it
	    
	    //update the receiver's incoming buffer if init, for steady
	    //this is done in receiveUpdate
	    //	    if (initSimulation) 
	    //	counters.incrementBufferCount(node);
	    LinkedList list = new LinkedList();
	    list.add(node);
	    return list;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //just pass thru joiners except the joiners that are the 
	    //last joiner in a joiner group
	    //this list is kept in the Layout class
	    if (Layout.getJoiners().contains(node)) {
		LinkedList list = new LinkedList();
		list.add(node);
		return list;
	    }
	    else {
		return getDestinationHelper(node.edges[0], counters, 
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
		    list.addAll(getDestinationHelper(node.edges[i], 
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
			return getDestinationHelper(node.edges[i], 
					      counters, joinerBuffer,
					      previous);
		    }
		}
		//none were greater than zero, reset all counters
		//and send to the first non-zero weight
		for (int i = 0; i < node.ways; i++) {
		    counters.resetArcCountOutgoing(node, i);
		}
		for (int i = 0; i < node.ways; i++) {
		    if (counters.getArcCountOutgoing(node, i) > 0) {
			counters.decrementArcCountOutgoing(node, i);
			return getDestinationHelper(node.edges[i], 
					      counters, joinerBuffer,
					      previous);
		    }
		}
	    }
	    
	}
	else {
	    throw new RuntimeException("SimulateDataItem");
	}
	return null;
    }

    //for now, find the most-downstream filter to fire
    //from the starting node
    private FlatNode whoShouldFireDownStream(FlatNode current, HashMap executionCounts, 
				    SimulationCounter counters) 
    {
	FlatNode start = current;
	//breadth first search from bottom
	if (start == null)
	    start = toplevel;
	HashSet visited = new HashSet();
	Vector queue = new Vector();
	FlatNode node;
	FlatNode mostDownStream = null;
	
	queue.add(start);
	while (!queue.isEmpty()) {
	    node = (FlatNode)queue.get(0);
	    queue.remove(0);
	    
	    if (node == null)
		continue;
	    
	    if (canFire(node, executionCounts, counters)) {
		mostDownStream = node;
	    }

	    //to keep the order of the nodes of a splitjoin in the correct order
	    //(the order defined by the joiner) add to the queue in the reverse order
	    for (int i = node.ways - 1; i >= 0; i--) {
		if (!visited.contains(node.edges[i])) {
		    queue.add(node.edges[i]); 
		    visited.add(node.edges[i]);
		}
	    }
	}
	//no node can fire
	if (mostDownStream == current)
	    return null;
	return mostDownStream;
    }
   

    public boolean canFire(FlatNode node, HashMap executionCounts, 
			    SimulationCounter counters) 
    {
	if (node == null)
	    return false;
	if (Layout.getIdentities().contains(node))
	    return false;

	if (node.contents instanceof SIRFilter) {
	    //	    if (node.contents instanceof SIRTwoStageFilter) {
	    //System.out.println(node.contents.getName() + ": " +
	    //		   counters.getBufferCount(node) + " >= " +
	    //		   itemsNeededToFire(node, counters) );
	    //}
	    

	    //check if this node has fired the number of times given by
	    //the schedule
	    Integer count = (Integer)executionCounts.get(node);
	    //if a node is not executed at all in a schedule it will not have an
	    //entry
	    if (count == null)
		return false;
	    if (count.intValue() == 0) {
		return false;
	    }
	    if (counters.getBufferCount(node) >= itemsNeededToFire(node, counters, executionCounts)) {
		return true;
	    }
	    else
		return false;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //first of all, a joiner can only fire it is the most downstream
	    //joiner in a joiner group
	    if (!Layout.getJoiners().contains(node))
		return false;
	    //determine if the joiner can receive and buffer data
	    //this does not count as an execution of the joiner
	    //only sending data counts as an execution, that is why we check that 
	    //next
	    //if (counters.getBufferCount(node) > 0) 
	    //	return true;
	    
	    //check if this node has fired the number of times given by
	    //the schedule
	    Integer count = (Integer)executionCounts.get(node);
	    //if a node is not executed at all in a schedule it will not have an
	    //entry
	    if (count == null)
		return false;
	    if (count.intValue() == 0) {
		return false;
	    }
	    
	    /*
	      System.out.println("Joiner Buffer: " + counters.
			       getJoinerBuffer(node) + " has " +
			       counters.getJoinerBufferCount(node, counters.
							     getJoinerBuffer(node)));
	    */
	    //determine if the joiner can send data downstream from a buffer
	    if (counters.getJoinerBufferCount(node, counters.
	    				      getJoinerBuffer(node)) > 0)
		return true;
	    else
		return false;
	}
	else 
	    return false;
    }
   	
    //Just a debugging function, not used
    public void visitNode(FlatNode node) 
    {
	System.out.println(node.contents.getName());
	if (node.contents instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter two = (SIRTwoStageFilter)node.contents;
	    System.out.println("init peek: " + two.getInitPeek() + " init pop:" + two.getInitPop() +
			       " init push: " + two.getInitPush());
	}
	if (node.contents instanceof SIRFilter) {
	    SIRFilter two = (SIRFilter)node.contents;
	    System.out.println("init peek: " + two.getPeekInt() + " init pop:" + two.getPopInt() +
			       " init push: " + two.getPushInt());
	}
	
	if (RawBackend.initExecutionCounts.containsKey(node))
	    System.out.println("   executes in init " + 
			       ((Integer)RawBackend.initExecutionCounts.get(node)).intValue());
	if (RawBackend.steadyExecutionCounts.containsKey(node))
	    System.out.println("   executes in steady " + 
			       ((Integer)RawBackend.steadyExecutionCounts.get(node)).intValue());
    }
    
    private String getJoinerBuffer(FlatNode node, FlatNode previous) 
    {
	//	System.out.println(node.contents.getName());
	//System.out.println(previous.contents.getName());
	for (int i = 0; i < node.inputs; i++) {
	    if (node.incoming[i] == null)
		continue;
	    
	    if (node.incoming[i] == previous)
		return String.valueOf(i);
	    if (node.incoming[i].contents instanceof SIRSplitter) {
		FlatNode temp = node.incoming[i];
		while (true) {
		    if (temp == previous)
			return String.valueOf(i);
		    if (!(temp.contents instanceof SIRSplitter))
			break;
		    temp = temp.incoming[0];
		}
	    }
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
	    System.out.println(node.contents.getName() + " " + 
			       ((Integer)map.get(node)).toString());
	}
	System.out.println();
	
    }

}

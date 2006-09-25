package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
//import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import at.dms.kjc.common.CommonUtils;
import java.util.HashSet;
//import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
//import java.util.ListIterator;
import java.util.Iterator;

/**
 * This class generates a schedule for the switch code by simulating the init
 * schedule and one steady state execution of the schedule
 */
public class FineGrainSimulator extends Simulator {

    public void simulate() {
        System.out.println("FineGrainSimulator Running...");

        initJoinerCode = new HashMap<FlatNode, JoinerScheduleNode>();
        steadyJoinerCode = new HashMap<FlatNode, JoinerScheduleNode>();

        SimulationCounter counters = new SimulationCounter(
                                                           JoinerSimulator.schedules);

        // create copies of the executionCounts
        HashMap<FlatNode, Integer> initExecutionCounts = (HashMap<FlatNode, Integer>) ssg.getExecutionCounts(true)
            .clone();
        HashMap<FlatNode, Integer> steadyExecutionCounts = (HashMap<FlatNode, Integer>) ssg.getExecutionCounts(false)
            .clone();

        joinerCode = initJoinerCode;

        // System.out.println("\n\nInit Execution Counts");
        // SpaceDynamicBackend.printCounts(SpaceDynamicBackend.initExecutionCounts);

        this.initialize(true);
        initSchedules = this.goInit(initExecutionCounts, counters, null);
        testExecutionCounts(initExecutionCounts);
        //System.out.println("End of init simulation");

        // System.out.println("\n\nSteady Execution Counts");
        // SpaceDynamicBackend.printCounts(SpaceDynamicBackend.steadyExecutionCounts);

        // re-initialized the state of the simulator
        this.initialize(false);
        // reset the state of the buffers
        counters.resetBuffers();
    
        System.gc();

        joinerCode = steadyJoinerCode;
        steadySchedules = this.go(steadyExecutionCounts, counters, null);
        testExecutionCounts(steadyExecutionCounts);
        //System.out.println("End of steady-state simulation");
    }

    public FineGrainSimulator(SpdStaticStreamGraph ssg, JoinerSimulator js) {
        super(ssg, js);
    }

    /** Initialize the state of the communication simulator * */
    private void initialize(boolean init) {
        switchSchedules = new HashMap<Object, StringBuffer>();
        currentJoinerCode = new HashMap<FlatNode, JoinerScheduleNode>();
        toplevel = ssg.getTopLevel();
        initSimulation = init;
    }

    /*
     * This function tests to see if a simulation of a schedule has executed to
     * its completion. It checks if all the execution counts for mapped streams
     * are 0
     */
    private void testExecutionCounts(HashMap<FlatNode, Integer> exeCounts) {
        boolean bad = false;

        Iterator<FlatNode> it = exeCounts.keySet().iterator();
        while (it.hasNext()) {
            FlatNode node = it.next();
            if (layout.isAssigned(node)) {
                if (exeCounts.get(node).intValue() != 0) {
                    System.out
                        .println(node.contents.getName() + " has "
                                 + exeCounts.get(node)
                                 + " executions remaining!!!!");
                    bad = true;
                }
            }
        }
        if (bad)
            Utils
                .fail("Error in simulator.  Some nodes did not execute.  See above...");

    }

    /*
     * This function is called before the init simulation is run. It creates the
     * code in the joiner code to call the initpath function and place the
     * results in the correct buffer of the joiner
     */
    private void callInitPaths(SimulationCounter counters) {
        // find all the joiners that are immediately contained in a FeedbackLoop
        Iterator<FlatNode> joiners = layout.getJoiners().iterator();
        // clone the joiner schedules

        FlatNode joiner;
        // iterate over all of the joiners of a feedbackloop
        while (joiners.hasNext()) {
            joiner = joiners.next();
            if ((((SIRJoiner) joiner.contents).getParent() instanceof SIRFeedbackLoop)) {
                // create the initPath calls
                SIRFeedbackLoop loop = (SIRFeedbackLoop) ((SIRJoiner) joiner.contents)
                    .getParent();
                int delay = loop.getDelayInt();
                JoinerScheduleNode current = JoinerSimulator.schedules
                                              .get(joiner);
                CType joinerType = CommonUtils.getJoinerType(joiner);
                for (int i = 0; i < delay; i++) {
                    // for each init path call find the correct buffer to place
                    // it in.
                    while (true) {
                        if (current.buffer.endsWith("1")) {
                            // create the joinerCode Node and put it in the init
                            // schedule
                            JoinerScheduleNode prev = currentJoinerCode
                                .get(joiner);
                            JoinerScheduleNode code = 
                                new JoinerScheduleNode(i,current.buffer,joinerType);
                            if (prev == null) {
                                // first node in joiner code
                                // this will add it to the joiner code hashmap
                                // this hashmap store the first instruction of
                                // each code sequence
                                joinerCode.put(joiner, code);
                            } else {
                                // connect to the prev
                                prev.next = code;
                            }
                            // set current
                            currentJoinerCode.put(joiner, code);
                            // record that a data item was placed in this buffer
                            counters.incrementJoinerBufferCount(joiner,
                                                                current.buffer);
                            // we found a buffer so break and place the next
                            // initPath call
                            current = current.next;
                            break;
                        }
                        current = current.next;
                    }
                }
            }
        }
    }

    // The start of the simulation for the initialization schedule
    private HashMap<Object, StringBuffer> goInit(HashMap<FlatNode, Integer> counts, SimulationCounter counters,
                           FlatNode lastToFire) {
        // create the initpath calls
        callInitPaths(counters);
        // simulate
        return go(counts, counters, lastToFire);
    }

    /* the main simulation method */
    private HashMap<Object, StringBuffer> go(HashMap<FlatNode, Integer> counts, SimulationCounter counters,
                       FlatNode lastToFire) {
        FlatNode fire, dest;

        while (true) {
            // find out who should fire
            fire = whoShouldFire(lastToFire, counts, counters);
            // if no one left to fire, stop
            if (fire == null)
                break;
            // keep track of everything needed when a node fires
            int items = fireMe(fire, counters, counts);
            // simulate the firings
            // 1 item for a joiner, push items for a filter
            for (int i = 0; i < items; i++) {
                // get the destinations of this item
                // could be multiple dests with duplicate splitters
                // a filter always has one outgoing arc, so sent to way 0
                if (KjcOptions.magic_net) {
                    assert false;
                    // generating code for the raw magic network
                    // appendMagicNetNodes(fire, getDestination(fire,
                    // counters));
                } else {
                    // not generating code for the magic network
                    // generate switch code for all intermediate
                    // hops
                    generateSwitchCode(fire, getDestination(fire, counters));
                }
                // see if anyone downstream can fire
                // if (fire != lastToFire)
                go(counts, counters, fire);
            }
        }
        return switchSchedules;
    }

    /*
     * private void appendMagicNetNodes(FlatNode fire, List dests) { RawTile
     * source = layout.getTile(fire);
     * 
     * HashMap receiveSchedules = MagicNetworkSchedule.steadyReceiveSchedules;
     * HashMap sendSchedules = MagicNetworkSchedule.steadySendSchedules;
     * 
     * //append the current information to the correct schedule //depending of
     * if it is the steady state or init if (initSimulation) { receiveSchedules =
     * MagicNetworkSchedule.initReceiveSchedules; sendSchedules =
     * MagicNetworkSchedule.initSendSchedules; }
     * 
     * //if the source schedule does not exist create it if
     * (!sendSchedules.containsKey(source)) sendSchedules.put(source, new
     * LinkedList());
     * 
     * LinkedList sourceSendSchedule = (LinkedList)sendSchedules.get(source);
     * 
     * //generate a list of coordinates to add to the send schedule for the
     * source LinkedList destsRawTile = new LinkedList();
     * 
     * 
     * //iterate thru the dests adding to the receive schedules for the dests
     * Iterator it = dests.iterator(); while(it.hasNext()) { RawTile currentDest =
     * layout.getTile((FlatNode)it.next());
     * 
     * 
     * if (!receiveSchedules.containsKey(currentDest))
     * receiveSchedules.put(currentDest, new LinkedList()); LinkedList
     * destReceiveSchedule = (LinkedList)receiveSchedules.get(currentDest);
     * 
     * destReceiveSchedule.add(source);
     * 
     * //add to the list of coordinate dests destsRawTile.add(currentDest); }
     * 
     * //add the list of coordinates to the source send schedule
     * sourceSendSchedule.add(destsRawTile); }
     */

    protected int fireJoiner(FlatNode fire, SimulationCounter counters,
                             HashMap<FlatNode, Integer> executionCounts) {
        // System.out.println("Firing " + fire.contents.getName());
        // The joiner is passing a data item, record this as an execution
        decrementExecutionCounts(fire, executionCounts, counters);

        // else, this joiner fired because it has data that can be sent
        // downstream
        JoinerScheduleNode current = new JoinerScheduleNode(CommonUtils
                                                            .getJoinerType(fire));
        current.buffer = counters.getJoinerBuffer(fire);
        current.type = JoinerScheduleNode.FIRE;
        addJoinerCode(fire, current);

        // decrement the buffer
        counters.decrementJoinerBufferCount(fire, counters
                                            .getJoinerBuffer(fire));
        // step the schedule
        counters.incrementJoinerSchedule(fire);
        return 1;
    }

    // return the destinations of the data item and generate the code
    // to receive data into the joiner
    private List<FlatNode> getDestination(FlatNode node, SimulationCounter counters) {
        List<FlatNode> list = getDestinationHelper(node.edges[0], counters, "", node);
        HashSet<FlatNode> visited = new HashSet<FlatNode>();
        // generate joiner receive code
        // iterate over the list
        Iterator<FlatNode> it = list.iterator();
        while (it.hasNext()) {
            FlatNode dest = it.next();
            if (!(dest.contents instanceof SIRJoiner))
                continue;
            // decrement the buffer from the joiner
            counters.decrementBufferCount(dest, 1);
            // get the joiner buffer as determined by getDestination and stored
            // in a list
            String joinerBuffer = counters.getJoinerReceiveBuffer(dest);
            // as determined by the simulation
            counters.incrementJoinerBufferCount(dest, joinerBuffer);
            // add to the joiner code for this dest
            JoinerScheduleNode current = new JoinerScheduleNode(CommonUtils.getJoinerType(dest));
            current.buffer = joinerBuffer;
            if (visited.contains(dest))
                current.type = JoinerScheduleNode.DUPLICATE;
            else
                current.type = JoinerScheduleNode.RECEIVE;
            addJoinerCode(dest, current);

            visited.add(dest);
        }
        return list;
    }

    // get the destination of the data item
    private List<FlatNode> getDestinationHelper(FlatNode node,
                                      SimulationCounter counters, String joinerBuffer, FlatNode previous) {
        // if we reached a node then this is a destination
        // add to its buffer and
        // create a list and add it
        if (node.contents instanceof SIRFilter) {
            if (layout.getIdentities().contains(node)) {
                return getDestinationHelper(node.edges[0], counters,
                                            joinerBuffer, node);
            }
            counters.incrementBufferCount(node);
            LinkedList<FlatNode> list = new LinkedList<FlatNode>();
            list.add(node);
            return list;
        } else if (node.contents instanceof SIRJoiner) {
            // just pass thru joiners except the joiners that are the
            // last joiner in a joiner group
            // this list is kept in the layout class
            if (layout.getJoiners().contains(node)) {
                joinerBuffer = joinerBuffer + getJoinerBuffer(node, previous);
                counters.addJoinerReceiveBuffer(node, joinerBuffer);
                counters.incrementBufferCount(node);
                LinkedList<FlatNode> list = new LinkedList<FlatNode>();
                list.add(node);
                // if previous == identity
                return list;
            } else {
                return getDestinationHelper(node.edges[0], counters,
                                            joinerBuffer + getJoinerBuffer(node, previous), node);
            }
        } else if (node.contents instanceof SIRSplitter) {
            // here is the meat
            SIRSplitter splitter = (SIRSplitter) node.contents;
            // if splitter send the item out to all arcs
            // build a list of all the dests
            if (splitter.getType() == SIRSplitType.DUPLICATE) {
                LinkedList<FlatNode> list = new LinkedList<FlatNode>();
                for (int i = 0; i < node.ways; i++) {
                    // decrement counter on arc
                    if (counters.getArcCountOutgoing(node, i) == 0)
                        counters.resetArcCountOutgoing(node, i);
                    counters.decrementArcCountOutgoing(node, i);
                    list.addAll(getDestinationHelper(node.edges[i], counters,
                                                     joinerBuffer, previous));
                }
                return list;
            } else {
                // weighted round robin
                for (int i = 0; i < node.ways; i++) {
                    if (counters.getArcCountOutgoing(node, i) > 0) {
                        counters.decrementArcCountOutgoing(node, i);
                        return getDestinationHelper(node.edges[i], counters,
                                                    joinerBuffer, previous);
                    }
                }
                // none were greater than zero, reset all counters
                // and send to the first non-zero weight
                for (int i = 0; i < node.ways; i++) {
                    counters.resetArcCountOutgoing(node, i);
                }
                for (int i = 0; i < node.ways; i++) {
                    if (counters.getArcCountOutgoing(node, i) > 0) {
                        counters.decrementArcCountOutgoing(node, i);
                        return getDestinationHelper(node.edges[i], counters,
                                                    joinerBuffer, previous);
                    }
                }
            }

        } else {
            throw new RuntimeException("SimulateDataItem");
        }
        return null;
    }

    // for now, find the most-downstream filter to fire
    // from the starting node
    private FlatNode whoShouldFire(FlatNode current, HashMap<FlatNode, Integer> executionCounts,
                                   SimulationCounter counters) {
        FlatNode start = current;

        if (start == null)
            start = toplevel;
        HashSet<FlatNode> visited = new HashSet<FlatNode>();
        Vector<FlatNode> queue = new Vector<FlatNode>();
        FlatNode node;
        FlatNode mostDownStream = null;

        queue.add(start);
        while (!queue.isEmpty()) {
            node = queue.get(0);
            queue.remove(0);

            if (node == null)
                continue;

            if (canFire(node, executionCounts, counters)) {
                mostDownStream = node;
            }

            // to keep the order of the nodes of a splitjoin in the correct
            // order
            // (the order defined by the joiner) add to the queue in the reverse
            // order
            for (int i = node.ways - 1; i >= 0; i--) {
                if (!visited.contains(node.edges[i])) {
                    queue.add(node.edges[i]);
                    visited.add(node.edges[i]);
                }
            }
        }
        // no node can fire
        if (mostDownStream == current)
            return null;
        return mostDownStream;
    }

    public boolean canFire(FlatNode node, HashMap<FlatNode, Integer> executionCounts,
                           SimulationCounter counters) {
        if (node == null)
            return false;
        if (layout.getIdentities().contains(node))
            return false;

        if (node.contents instanceof SIRFilter) {
            // if (node.contents instanceof SIRTwoStageFilter) {
            // System.out.println(node.contents.getName() + ": " +
            // counters.getBufferCount(node) + " >= " +
            // itemsNeededToFire(node, counters) );
            // }

            // check if this node has fired the number of times given by
            // the schedule
            Integer count = executionCounts.get(node);
            // if a node is not executed at all in a schedule it will not have
            // an
            // entry
            if (count == null)
                return false;
            if (count.intValue() == 0) {
                return false;
            }
            if (counters.getBufferCount(node) >= itemsNeededToFire(node,
                                                                   counters, executionCounts)) {
                return true;
            } else
                return false;
        } else if (node.contents instanceof SIRJoiner) {
            // first of all, a joiner can only fire it is the most downstream
            // joiner in a joiner group
            if (!layout.getJoiners().contains(node))
                return false;
            // determine if the joiner can receive and buffer data
            // this does not count as an execution of the joiner
            // only sending data counts as an execution, that is why we check
            // that
            // next
            // if (counters.getBufferCount(node) > 0)
            // return true;

            // check if this node has fired the number of times given by
            // the schedule
            Integer count = executionCounts.get(node);
            // if a node is not executed at all in a schedule it will not have
            // an
            // entry
            if (count == null)
                return false;
            if (count.intValue() == 0) {
                return false;
            }

            // determine if the joiner can send data downstream from a buffer
            if (counters.getJoinerBufferCount(node, counters
                                              .getJoinerBuffer(node)) > 0)
                return true;
            else
                return false;
        } else
            return false;
    }

    private String getJoinerBuffer(FlatNode node, FlatNode previous) {
        // System.out.println(node.contents.getName());
        // System.out.println(previous.contents.getName());
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

    private static void printExecutionCounts(HashMap map) {
        System.out.println();

        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            FlatNode node = (FlatNode) it.next();
            System.out.println(node.contents.getName() + " "
                               + ((Integer) map.get(node)).toString());
        }
        System.out.println();

    }

}

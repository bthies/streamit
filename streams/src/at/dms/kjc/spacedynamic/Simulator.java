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
//import java.util.Vector;
import java.util.List;
//import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
//import at.dms.kjc.sir.lowering.partition.WorkEstimate;



public abstract class Simulator {
    /** ComputeNode->StringBuffer, the init switch schedule for this tile **/
    public HashMap<Object, StringBuffer> initSchedules;
    /** ComputeNode->StringBuffer, the steady switch schedule for this tile **/
    public HashMap<Object, StringBuffer> steadySchedules;
    /** FlatNode->JoinerScheduleNode, the receiving/sending schedule for the joiner 
        for init **/
    public HashMap<FlatNode, JoinerScheduleNode> initJoinerCode;
    /** FlatNode->JoinerScheduleNode, the receiving/sending schedule for the joiner
        for steady **/
    public HashMap<FlatNode, JoinerScheduleNode> steadyJoinerCode;
    
    protected SpdStaticStreamGraph ssg;

    protected FlatNode toplevel;

    /** joinerSimulator gives the receiving schedule for each joiner **/
    protected JoinerSimulator joinerSimulator;

    protected Layout layout;
    protected RawChip rawChip;


    //These next fields don't need to be used, they are implementation dependent 
    
    /** true if we are simulating the init schedule */
    protected boolean initSimulation;

    /** The current switch schedule we are working on **/
    protected HashMap<Object, StringBuffer> switchSchedules;

    /** the current joiner code we are working on (steady or init) **/
    protected HashMap<FlatNode, JoinerScheduleNode> joinerCode;
        
    /** the curent node in the joiner schedule we are working on **/
    protected HashMap<FlatNode, JoinerScheduleNode> currentJoinerCode;    

    
    public Simulator(SpdStaticStreamGraph ssg, JoinerSimulator joinerSimulator) 
    {
        this.ssg = ssg;
        this.joinerSimulator = joinerSimulator;
        this.toplevel = ssg.getTopLevel();
        this.layout = ((SpdStreamGraph)ssg.getStreamGraph()).getLayout();
        this.rawChip = ((SpdStreamGraph)ssg.getStreamGraph()).getRawChip();
    }

    public abstract void simulate();
    public abstract boolean canFire(FlatNode node, HashMap<FlatNode, Integer> executionCounts, 
                                    SimulationCounter counters);

    /**
     * 
     * @return true if we need switch code for this graph, otherwise we
     * can just use the dynamic network because there are no overlapping routes
     * and no splitters or joiners 
     */
    public static boolean needSimulator(SpdStaticStreamGraph ssg) {
        //check if there are any overlapping routes...
        if (((SpdStreamGraph)ssg.getStreamGraph()).getLayout().getIntermediateTiles().size() > 0)
            return true;
        
        //check if there are any splitters...
        Iterator it = ssg.getFlatNodes().iterator();
        while (it.hasNext()) {
            FlatNode node = (FlatNode) it.next();
            if (node.isJoiner() || node.isSplitter())
                return true;
        }
        //all's good!
        return false;
    }
    

    /** Create switch assembly code for to route one item from <pre>fire</pre> to
        the dests.  <pre>previous</pre> is a hashmap from ComputeNode -> ComputeNode that 
        maps a node to its previous hop, <pre>next</pre> is similiar...

    **/
    protected void asm(ComputeNode fire, HashMap<ComputeNode, ComputeNode> previous, HashMap<ComputeNode, HashSet> next) 
    {
        assert fire != null;
        //System.out.println("asm: " + fire);
        //generate the sends
        if (!switchSchedules.containsKey(fire))
            switchSchedules.put(fire, new StringBuffer());
        StringBuffer buf = switchSchedules.get(fire);
        Iterator it = next.get(fire).iterator();
        buf.append("route ");
        while (it.hasNext()) {
            ComputeNode dest = (ComputeNode)it.next();
            buf.append("$csto->" + "$c" + 
                       rawChip.getDirection(fire, dest) + 
                       "o,");
        }
        //erase the trailing ,
        buf.setCharAt(buf.length() - 1, '\n');
    
        //generate all the other 
        Iterator<ComputeNode> tiles = next.keySet().iterator();
        while (tiles.hasNext()) {
            ComputeNode tile = tiles.next();
            assert tile != null;
            if (tile == fire) 
                continue;
            if (!switchSchedules.containsKey(tile))
                switchSchedules.put(tile, new StringBuffer());
            buf = switchSchedules.get(tile);
            ComputeNode prevTile = previous.get(tile);
            buf.append("route ");       Iterator nexts = next.get(tile).iterator();
            while(nexts.hasNext()) {
                ComputeNode nextTile = (ComputeNode)nexts.next();
                if (!nextTile.equals(tile))
                    buf.append("$c" + rawChip.getDirection(tile, prevTile) + "i->$c" +
                               rawChip.getDirection(tile, nextTile) + "o,");
                else 
                    buf.append("$c" + rawChip.getDirection(tile, prevTile) + "i->$c" +
                               rawChip.getDirection(tile, nextTile) + "i,");
            }
            buf.setCharAt(buf.length() - 1, '\n');
        }
    }
    
    /** generate the switch code for 1 data item given the list of destinations
     * we do not want to duplicate items until neccessary, so we have to keep track 
     * of all the routes and then generate the switch code
     * this way we can route multiple dests per route instruction
     */
    protected void generateSwitchCode(FlatNode fire, List<FlatNode> dests) 
    {
        assert !(layout.getIdentities().contains(fire));
        
        //should only have one previous
        HashMap<ComputeNode, ComputeNode> prev = new HashMap<ComputeNode, ComputeNode>();
        HashMap<ComputeNode, HashSet> next = new HashMap<ComputeNode, HashSet>();

        ListIterator<FlatNode> destsIt = dests.listIterator();
        while (destsIt.hasNext()) {
            FlatNode dest = destsIt.next();
            assert dest != null;
            assert !(layout.getIdentities().contains(dest));
            //  System.out.println("  Dest: " + dest + " " + layout.getTile(dest));
            ComputeNode[] hops = 
                layout.router.
            getRoute(ssg, layout.getComputeNode(fire), layout.getComputeNode(dest)).toArray(new ComputeNode[0]);

            
            assert hops.length > 1 : "Error: Bad Layout (could not find route from " + fire.toString() + " -> " +
                dest.toString();

            //for (int i = 0; i < hops.length; i++)
            //      System.out.println("     " + hops[i]);
        
            //add to fire's next
            if (!next.containsKey(layout.getComputeNode(fire))) 
                next.put(layout.getComputeNode(fire), new HashSet());
            next.get(layout.getComputeNode(fire)).add(hops[1]);
            //add to all other previous, next
            for (int i = 1; i < hops.length -1; i++) {
                if (prev.containsKey(hops[i]))
                    if (prev.get(hops[i]) != hops[i-1])
                        Utils.fail("More than one previous tile for a single data item");
                prev.put(hops[i], hops[i-1]);
                if (!next.containsKey(hops[i]))
                    next.put(hops[i], new HashSet());
                next.get(hops[i]).add(hops[i+1]);
            }
            //add the last step, plus the dest to the dest map
            if (prev.containsKey(hops[hops.length - 1]))
                if (prev.get(hops[hops.length - 1]) != hops[hops.length - 2])
                    Utils.fail("More than one previous tile for a single data item (2)");
            prev.put(hops[hops.length-1], hops[hops.length - 2]);
            if (!next.containsKey(hops[hops.length-1]))
                next.put(hops[hops.length - 1], new HashSet());
            next.get(hops[hops.length - 1]).add(hops[hops.length -1]);
        }
    
        //create the appropriate amount of routing instructions
        int elements = Util.getTypeSize(CommonUtils.getOutputType(fire));
        for (int i = 0; i < elements; i++)
            asm(layout.getComputeNode(fire), prev, next);
    }

    /** 
        given a node <pre>fire</pre> and the current state of the simulation, 
        return how many items are necessary for the node to fire.
    **/
    protected int itemsNeededToFire(FlatNode fire, SimulationCounter counters,
                                    HashMap<FlatNode, Integer> executionCounts) 
    {
        //if this is the first time a two stage initpeek is needed to execute
        if (initSimulation &&
            !counters.hasFired(fire) &&
            fire.contents instanceof SIRTwoStageFilter) {
            return ((SIRTwoStageFilter)fire.contents).getInitPeekInt();
        }
        else if (!initSimulation && KjcOptions.ratematch && 
                 fire.contents instanceof SIRFilter) {
            //we are ratematching filters
            return (((SIRFilter)fire.contents).getPopInt() *
                    ssg.getMult(fire, false) +
                    (((SIRFilter)fire.contents).getPeekInt() -
                     ((SIRFilter)fire.contents).getPopInt()));
        }
        //otherwise peek items are needed
        return ((SIRFilter)fire.contents).getPeekInt();
    }

    /** Give that a node has just fired, update the simulation state **/    
    protected void decrementExecutionCounts(FlatNode fire, HashMap<FlatNode, Integer> executionCounts, 
                                            SimulationCounter counters) 
    {
        //decrement one from the execution count
        int oldVal = executionCounts.get(fire).intValue();
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
    
    /** return how many items a node consumes (pop's) for one firing 
        given the current state of the simulation **/
    protected int consumedItems(FlatNode fire, SimulationCounter counters,
                                HashMap<FlatNode, Integer> executionCounts) {
        //if this is the first time a two stage fires consume initpop
        if (initSimulation &&
            !counters.hasFired(fire) &&
            fire.contents instanceof SIRTwoStageFilter)
            return ((SIRTwoStageFilter)fire.contents).getInitPopInt();
        else if (!initSimulation && KjcOptions.ratematch &&
                 fire.contents instanceof SIRFilter) {
            //we are ratematching on the filter
            //it consumes for the entire steady state
            return ((SIRFilter)fire.contents).getPopInt() *
                ssg.getMult(fire, false);
        }
        //otherwise just consume pop
        return ((SIRFilter)fire.contents).getPopInt();
    }

        
    /** consume the data and return the number of items produced **/
    protected int fireMe(FlatNode fire, SimulationCounter counters, HashMap<FlatNode, Integer> executionCounts) 
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
                ret = ((SIRTwoStageFilter)fire.contents).getInitPushInt();
            else if (!initSimulation && KjcOptions.ratematch) {
                //we are ratematching so produce all the data on the one firing.
                ret *= ssg.getMult(fire, false);
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
    protected void addJoinerCode(FlatNode fire, JoinerScheduleNode code) 
    {
        //add to the joiner code for this fire
        JoinerScheduleNode prev = 
            currentJoinerCode.get(fire);
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
    
    /** this method should fire a joiner, assuming the joiner can fire, and update the 
        simulation state **/
    protected abstract int fireJoiner (FlatNode fire, SimulationCounter counters, HashMap<FlatNode, Integer> executionCounts);
}


package at.dms.kjc.spacetime;

import at.dms.kjc.common.SimulatedAnnealing;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class calculates the assignment of filters of slices to tiles of 
 * the Raw chip by using simulated annealing.  At each annealing step, it
 * selects a random slice and finds a new random tile assignment for 
 * the filters of the slice.  The cost function is still in development and
 * is changing frequently.  
 * 
 * See the asplos submission for more details.
 * 
 * @author mgordon
 *
 */
public class AnnealedLayout extends SimulatedAnnealing implements Layout {
    /** if a tile does at least <pre>BIG_WORKER</pre> percentage of the bottle neck tile,
     * communication to and from it are included in the communication cost component
     * of the cost function.
     */
    private static final double BIG_WORKER = 0.9;
    
    /** multiply the communication estimate of the layout by this */
    private static double COMM_MULTIPLIER = 1.0;
    
    /** the added weight of the latency of a intertracebuffer that 
     * is connected to a big worker tile.
     */
    private static final int BIG_WORKER_COMM_LATENCY_WEIGHT = 100;
    
    private static final int ILLEGAL_COST = 1000000;
  
    /** the rawchip we are compiling to */
    private RawChip rawChip;
    /** the space time schedule object, with different schedules */
    private SpaceTimeSchedule spaceTime;
    /** A list of filters that need to be assigned to tiles */
    private LinkedList filterList;
    /** The partitioner we used to partitioner the SIR graph into slices */
    private Partitioner partitioner;
    /** the number of tiles in the raw chip */
    private int numTiles;
    /** the raw tiles, so we don't have to call rawChip */
    private RawTile tiles[];
    /** used to get a random sequence of directions to try from 
     * a tile when calc'ing a new random layout for a trace
     */
    private static String[] dirStrings;
    /** the tile that has the max work at the current step */ 
    private int maxWorkTile;
    /** Set of FilterTraceNodes of the Filters that write to slice that are file writers
     * This is not the file writers themselves, but the last filter of the slice upstream 
     * of the file writer slice. 
     * */
    private HashSet fileWriters;
    /** Set of FilterTraceNodes of the Filters that read from slice that are file readers
     * This is not the file readers themselves, but the first filter of the slice downstream 
     * of the file reader slice. 
     * */
    private HashSet fileReaders;
    /** The order the traces will be scheduled, so sorted by bottleneck work */
    private LinkedList scheduleOrder;
    /** The class that performs the buffer to DRAM assignment */
    private BufferDRAMAssignment assignBuffers;
    /** the total work all the tiles for the current layout */
    private int totalWork;
    
    /**
     * Create a new Annealed layout object that will assign filters of 
     * the slices of the slice graph of spaceTime to tiles of the Raw Chip.
     * 
     * @param spaceTime The spaceTime compilation up till now.
     */
    public AnnealedLayout(SpaceTimeSchedule spaceTime) {
        super();
        this.spaceTime = spaceTime;
        rawChip = spaceTime.getRawChip();
        this.partitioner = spaceTime.partitioner;
        numTiles = rawChip.getTotalTiles();
        tiles = new RawTile[numTiles];
        for (int i = 0; i < numTiles; i++) 
            tiles[i] = rawChip.getTile(i);
        //get the schedule order of the graph!
        Trace[] tempArray = (Trace[]) spaceTime.partitioner.getTraceGraph().clone();
        Arrays.sort(tempArray, new CompareTraceBNWork(spaceTime.partitioner));
        scheduleOrder = new LinkedList(Arrays.asList(tempArray));

        // reverse the list, we want the list in descending order!
        Collections.reverse(scheduleOrder);
        
        //init the buffer to dram assignment pass
        assignBuffers = new BufferDRAMAssignment();
        //COMM_MULTIPLIER = COMP_COMM_RATIO / 
    }

    /**
     * Calculate the assignment of filters of slices to tiles. 
     */
    public void run() {
        //run the simulated annealing with 2 iterations, 
        //100 attempts per iteration
        simAnnealAssign(2, 100);
        printLayoutStats();
        for (int i = 0; i < filterList.size(); i++) 
            System.out.println(filterList.get(i) + " is assigned to " + 
                    assignment.get(filterList.get(i)));
    }
    
    /**
     * Get the raw tile that was assigned to filter in the layout.
     * 
     * Must call {@link AnnealedLayout#run} first.
     */
    public RawTile getTile(FilterTraceNode filter) {
        assert assignment.containsKey(filter) : 
            "AnnealedLayout does have a mapping for " + filter;
        return (RawTile)assignment.get(filter);
    }
        
    /**
     * Set the underlying assigment of FilterTraceNode's -> RawTiles
     * to the newAssignment HashMap.
     * 
     * @param newAssignment The assignment to use.
     */
    public void setAssignment(HashMap newAssignment) {
        assignment = newAssignment;
//      reassign buffers.
        assignBuffers.run(spaceTime, this);
    }
    
    /**
     * Print some statistics to the screen for the layout.
     */
    public void printLayoutStats() {
        int[] tileCosts = getTileWorks(false);
        
        System.out.println("Placement cost: " + placementCost(false));
        
        System.out.println("Max Work Tile of layout is " + maxWorkTile + 
                ", it has work: " + maxTileWork(tileCosts));
        
       
        for (int i = 0; i < tileCosts.length; i++) 
            System.out.println("Tile " + i + " has work " + tileCosts[i]);
        
        System.out.println("Std Dev of work: " + standardDeviation(tileCosts));
        System.out.println("Avg work: " + Util.mean(tileCosts));
        System.out.println("Median work: " + Util.median(tileCosts));
        System.out.println("Total Work: " + totalWork);
        System.out.println("Sum of tile work: " + Util.sum(tileCosts));
        System.out.println("Wasted work: " + (Util.sum(tileCosts) - totalWork));
        
        assert isLegalLayout() : 
            "Simulated annealing arrived at an illegal layout.";
    }
    
    /**
     * This function will perturb the configuration by moving
     * something around.
     */
    public void swapAssignment() {
        //swapAssignmentIllegal();
        //swap filters...
        swapAssignmentLegal();
        //reassign buffers.
        assignBuffers.run(spaceTime, this);
    }
    
    /** 
     * perturb the configuration by finding a new layout for a single trace.
     *
     */
    public void swapAssignmentLegal() {
        Trace[] traces = partitioner.getTraceGraph();
        Trace reassignMe = traces[getRandom(traces.length)];
        
        assert newTraceAssignment(reassignMe.getHead().getNextFilter()) :
            "Error: could not find a legal layout for " + reassignMe + " during layout";
    }
   
    
    /**
     * Create the initial assignment of filters of slices to tiles. 
     */
    public void initialPlacement() {
       //try a random layout! nope
        //randomInitialPlacement();
        legalInitialPlacement();
        //assign buffers...
        assignBuffers.run(spaceTime, this);
    }
    
    /**
     * Set the assignment of node to tile in the layout.
     * 
     * @param node The node that will be assigned to tile.
     * @param tile The tile to assign node to.
     */
    public void setTile(FilterTraceNode node, RawTile tile) {
        assignment.put(node, tile);
    }
    
    /**
     * A recursive function that will find a new layout for a slice. 
     * For the TraceNode node, try to layout it and the remaining
     * TraceNodes of its slice.  We start by selecting a random starting
     * tile for the first node of the slice and then snake the slice along
     * the chip randomly.  If at anytime we cannot continue, return false, 
     * and the caller will try a new direction for the next node.  If nothing 
     * is legal, return false.  The false will propagate to the root.  
     * 
     * @param newAssign
     * @param node  call with first filterTraceNode of trace
     * @return true if we successfully laid out the trace, 
     * it should alway return true when finished recursing. 
     */
    private boolean newTraceAssignment(TraceNode node) {
        //we are finished
        if (node.isOutputTrace()) {
	    //check the assignment of the previous filter 
	    //if it is a file writer, to make sure the tile
	    //is legal
  
	    //if this filter write directly to a file, then we have
	    //to make sure that no other file writer filters are mapped to 
	    //<tile>
	    if (fileWriters.contains(node.getPrevious()) &&
		!legalFileWriterTile
		(node.getPrevious().getAsFilter(), 
		 (RawTile)assignment.get(node.getPrevious())))
		return false;;       
            return true;
	}
	

        //if we get here we are a filterTraceNode 
        FilterTraceNode filter = node.getAsFilter();
        
        if (node.getPrevious().isInputTrace()) {
            //Choose a random tile for the initial placement
            RawTile tile = null;
	    //set of tiles we have tried, no dups...
	    HashSet tilesTried = new HashSet();
	    //keep picking random tiles until we have tried all of them
            while (tilesTried.size() < numTiles) {
                tile = tiles[getRandom(numTiles)];
		tilesTried.add(tile);
                //if this is a file reader, we have to make sure that 
                //no other file readers are mapped to this tile... 
                if (fileReaders.contains(filter) && 
                        !legalFileReaderTile(filter, tile))
                    continue;
		
		setTile(filter, tile);
		if (newTraceAssignment(node.getNext()))
		    return true;
	    }
	    //if we get here, we have failed to find a layout.
	    return false;
        }
        else {
            //we are not the first tile so choose some
            RawTile prevTile = (RawTile)assignment.get(node.getPrevious());
            int dirVec = getRandom(24);
            for (int i = 0; i < 4; i++) {
                //get the next direction to try...
                RawTile tile = rawChip.getTile(prevTile, dirStrings[dirVec].charAt(i));
                //if null, then there was no tile in this direction, try another direction
                if (tile == null)
                    continue;
                
                //see if any of the previous filters of the trace were mapped to this
                //tile if so continue
                boolean prevMap = false;
                TraceNode prevNode = filter.getPrevious();
                while (assignment.containsKey(prevNode)) {
                    if (tile == assignment.get(prevNode)){
                        prevMap = true;
                        break;
                    }
                    prevNode = prevNode.getPrevious();
                }

                //try another tile
                if (prevMap)
                    continue;
                                       
                //assign this filter to the tile in this direction
                setTile(filter, tile);
                //try the assignment to the tile in this direction
                if (newTraceAssignment(node.getNext()))
                    return true; //if it worked return!
                else
                    continue; //if not, try another direction if there are more
            }
            //if we get here, we have failed
            return false;
        }
    }
 
    /**
     * We want to try to place <pre>filter</pre> on <pre>tile</pre> 
     * but it is a file reader, so we have to make sure that no 
     * other file reading filters are mapped to this tile.  
     * If so return false.
     * 
     * Note that when we use file reader, we mean a FilterTraceNode 
     * that is directly connected to a FileReader slice.
     * 
     * @see AnnealedLayout#fileReaders
     * 
     * @param filter A FilterTraceNode that reads from a file
     * @param tile The tile it is currently mapped to.
     * @return false if there are other file readers mapped to this tile.
     */
    private boolean legalFileReaderTile(FilterTraceNode filter,
            RawTile tile) {
	Iterator frs = fileReaders.iterator();
        while (frs.hasNext()) {
            FilterTraceNode current = (FilterTraceNode)frs.next();
            if (current == filter) 
                continue;
            RawTile occupied = (RawTile)assignment.get(current);
            //see if we have a mapping for this file reader filter yet
            if (occupied == null)
                continue;
            //if so, see if it mapped to the tile we want, if so return false
            if (occupied == tile)
                return false;
        }
        //no file readers mapped to tile, return true
        return true;
    }
    
    /**
     * We want to try to place <pre>filter</pre> on <pre>tile</pre> but 
     * <pre>filter</pre> writes to a file, so we have to make sure 
     * that no other filters that writes to a file is mapped to this tile.  
     * If there is one, return false.
     * 
     * Note that when we use the term file writer, we mean a FilterTraceNode
     * that outputs to a file, not the FileWriter slice itself.
     * 
     * @see AnnealedLayout#fileWriters
     *  
     * @param filter A FilterTraceNode that writes to a file.
     * @param tile The RawTile it is currently mapped to.
     * @return false if another FilterTraceNode that writes to a file is 
     * mapped to tile.
     */
    private boolean legalFileWriterTile(FilterTraceNode filter, 
            RawTile tile) {
	Iterator fws = fileWriters.iterator();
	while (fws.hasNext()) {
	    FilterTraceNode current = (FilterTraceNode)fws.next();
	    if (current == filter)
		continue;
	    RawTile occupied = (RawTile)assignment.get(current);
	    //see if we have a mapping for this file reader filter yet
	    if (occupied == null)
		continue;
	    //if so, see if it mapped to the tile we want, if so return false
	    if (occupied == tile) {
                //System.out.print(tile +  " has two file writers!");
		return false;
            }
	}
	//no file writer mapped to tile, return true
	return true;
    }
    
    /**
     * For each trace, generate a random legal tile assignment 
     * as the initial layout, but each slice assignment is legal and thus
     * the entire assignment is legal.
     */
    private void legalInitialPlacement() {
        Trace[] traces = partitioner.getTraceGraph();
        for (int i = 0; i < traces.length; i++) {
            assert newTraceAssignment(traces[i].getHead().getNextFilter()) :
                "Error: could not find a legal layout for " + traces[i] + " during initial layout";
        }
    }
    
    /** 
     * Return the cost function evaluated on the current assignment.
     * 
     * @return the cost function evaluated on the current assignment.
     *  
     */
    public double placementCost(boolean debug) {
   
        int[] tileCosts = getTileWorks(true);
        
        double workOfBottleNeckTile = (double)maxTileWork(tileCosts);
        
        double cost = workOfBottleNeckTile;//standardDeviation(tileCosts);  
                   //+ (COMM_MULTIPLIER * (double)commEstimate(tileCosts));
        
        
        //int wastedWork = Util.sum(tileCosts) - totalWork;
       
        //cost += (double)wastedWork * 0.15; 
            
        /*
        if (!isLegalLayout())
            cost += ILLEGAL_COST;
        */
        return cost;
    }

    /** 
     * Check that communicating filters of slice are placed on neighboring tiles.
     * If all are, then return true.
     * 
     * Note: This does not check for file reader/write collision.
     * 
     * @return Return true if all slices are properly snaked across the chip.
     */
    private boolean isLegalLayout() {
        Iterator filters = filterList.iterator();
        
        while (filters.hasNext()) {
            FilterTraceNode filter = (FilterTraceNode)filters.next();
            //last filter of a trace, no need to check it          
            if (filter.getNext().isOutputTrace())
                continue;
            
            RawTile myTile = (RawTile)assignment.get(filter);
            RawTile upTile = 
                (RawTile)assignment.get(filter.getNext().getAsFilter());
            if (!(rawChip.areNeighbors(myTile, upTile)))
                return false;
        }
        
        return true;
    }
    
    /** 
     * Given Edge <pre>edge</pre>, if edge is mapped to a source DRAM or 
     * a destination DRAM whose home tile is in the bigWorker set, then
     * return the distance that the edge occupies + 1 * a multiplier, 
     * if neither home tile is a bigWorker, then just return manhattan distance 
     * + 1.  
     * 
     * @param bigWorkers A set of RawTiles for tiles that do a bunch of work.
     * @param edge The Edge in question.
     * 
     * @return A metric of the latency of the Edge edge.
     */
    private int distanceLatency(HashSet bigWorkers, Edge edge) {
        
        //get the port that source is writing to
        RawTile srcTile = (RawTile)assignment.get(edge.getSrc().getPrevFilter());
        RawTile dstTile = (RawTile)assignment.get(edge.getDest().getNextFilter());
        int multipler = 1;
        //we only care about the tiles that do a bunch of work!
        if (bigWorkers.contains(srcTile) || bigWorkers.contains(dstTile)) {
            multipler = BIG_WORKER_COMM_LATENCY_WEIGHT;
        }
        //System.out.println("Accouting for comm over big worker.");
        IODevice srcPort = 
            LogicalDramTileMapping.getHomeDram
            (srcTile);
        //get the por that the dest is reading from
        IODevice dstPort = 
            LogicalDramTileMapping.getHomeDram
            (dstTile);
        
        //now get the neighboring tiles of the dram ram's and find the
        //distance between them, because we send everthing over the 
        //static network...
        RawTile srcNeighbor = srcPort.getNeighboringTile();
        RawTile dstNeighbor = dstPort.getNeighboringTile();
        
        return (rawChip.manhattanDistance(srcNeighbor, dstNeighbor) + 1) * multipler; 
    }    
    
    /**
     * Calculate a measure of the communication cost of the current 
     * assignment.  This method changes frequently, see the method itself 
     * for implementation.
     * 
     * @return An estimate of the cost of communication for this layout.
     */
    private int commEstimate(int tileCosts[]) {
        int estimate = 0;
        Trace[] traces = partitioner.getTraceGraph();
        HashSet bigWorkers = new HashSet();
        
        //create a hashset of tiles that do a bunch of work...
        for (int i = 0; i < tileCosts.length; i++) {
            if ((double)tileCosts[i] / (double)tileCosts[maxWorkTile] >= 
                BIG_WORKER) {
                bigWorkers.add(rawChip.getTile(i));
            }
        }
        
        //buffer edges are assigned drams by the buffer dram assignment,
        //so we can get a fairly accurate picture of the communication
        //of the graph...
        for (int i = 0; i < traces.length; i++) {
            Trace trace = traces[i];
            Iterator edges = trace.getTail().getDestSet().iterator();
            while (edges.hasNext()) {
                Edge edge = (Edge)edges.next();
                InterTraceBuffer buf = InterTraceBuffer.getBuffer(edge);
                OutputTraceNode output = edge.getSrc();
                InputTraceNode input = edge.getDest();
                //account for the distance of the connected to/from a
                //big worker
                estimate += distanceLatency(bigWorkers, edge);
                
//              if the off chip buffer does something then count its 
                //communicate if it goes through a bigWorker
                if (!OffChipBuffer.unnecessary(output)) {
                    StreamingDram srcDRAM = 
                        IntraTraceBuffer.getBuffer(output.getPrevFilter(), 
                                output).getDRAM();
                    StreamingDram dstDRAM = 
                        buf.getDRAM();
                    estimate += itemsThroughBigWorkers(bigWorkers, edge,
                            srcDRAM, dstDRAM);
                    
                }
//              if the off chip buffer does something then count its 
                //communicate if it goes through a bigWorker
                if (!OffChipBuffer.unnecessary(input)) {
                    StreamingDram srcDRAM = buf.getDRAM();
                    StreamingDram dstDRAM = 
                        IntraTraceBuffer.getBuffer(input, input.getNextFilter()).getDRAM();
                    estimate += itemsThroughBigWorkers(bigWorkers, edge,
                            srcDRAM, dstDRAM);
                    
                }
            }
        }
        return estimate;
    }
    
    /**
     * Given a set of tiles that perform a bunch of work, bigWorkers,
     * and a Edge with source dram src and dest dram dst, determine how many 
     * items travel over the bigWorkers tiles in the route from 
     * src to dest.  
     * 
     * @param bigWorkers A set of RawTiles that perform a bunch of work.
     * @param edge The Edge in question.
     * @param src The src DRAM for edge.
     * @param dst The dest DRAM for edge.
     * 
     * @return The number of items that travel over inter-trace-buffer
     * edge with src dram and dst dram 
     * and the pass through a tile in the <bigWorkers> tile set.
     * 
     */
    private int itemsThroughBigWorkers(HashSet bigWorker, Edge edge, 
            StreamingDram src, StreamingDram dst) {
        int estimate = 0;
        int items = edge.steadyItems();
        
        //the estimate will be based on the src and dst tiles for this edge
        //int work = 
        //    Math.max(spaceTime.partitioner.getTraceBNWork(edge.getSrc().getParent()),
        //            spaceTime.partitioner.getTraceBNWork(edge.getDest().getParent()));
        //get the route
        Iterator route = Router.getRoute(src, dst).iterator();
        //set of big workers we have already accounted for 
        HashSet<ComputeNode> accountedFor = new HashSet<ComputeNode>();
        //cycle through the route and see if any of the intermediate
        //hops are big workers...
        while (route.hasNext()) {
            ComputeNode hop = (ComputeNode)route.next();
            if (hop instanceof RawTile && 
                    bigWorker.contains(hop) &&  
                    !LogicalDramTileMapping.mustUseGdn((RawTile)hop)) {
                estimate +=  items; //work;
                accountedFor.add(hop);
            }
        }
        //now we if the src or dst owner tiles is a big worker and 
        //it is gdn, we should account for the total time of the edge also
        //because the dram command will have to wait until it is completed
        RawTile srcIssuer = LogicalDramTileMapping.getOwnerTile(src);
        if (bigWorker.contains(srcIssuer) &&
                LogicalDramTileMapping.mustUseGdn(srcIssuer)
                && !accountedFor.contains(srcIssuer)) {
            accountedFor.add(srcIssuer);
            estimate += items; //work;
        }
        
        RawTile dstIssuer = LogicalDramTileMapping.getOwnerTile(dst);
        if (bigWorker.contains(dstIssuer) &&
                LogicalDramTileMapping.mustUseGdn(dstIssuer) &&
                !accountedFor.contains(dstIssuer))
            estimate += items;//work;
        
        
        return estimate;
    }
    
    /**
     * Return the max element of tileCosts.
     * 
     * @param tileCosts The array that holds the cost for each tile 
     * number. 
     * 
     * @return The maximum amount of work that is performed on a tile.
     *
     */
    private int maxTileWork(int[] tileCosts) {
        
        //find the max
        int max = 0;
        
        for (int i = 0; i < tileCosts.length; i++) {
            if (tileCosts[i] > max) {
                max = tileCosts[i];
                maxWorkTile = i;
            }
        }
        //return the work that the max tile is estimated to do
        return max;
    }
    
    /**
     * Calculate the amount of work that is performed by each
     * tile in the steady-state.  This calculation will use the work
     * estimation for each filter and will account for pipeline lag inside 
     * of a slice.
     * 
     * If bias, we attempt to bias the tiles with little work by calling
     * {@link AnnealedLayout#biasCosts}.
     * 
     * @param bias Should we bias the costs calculated.
     *
     * @return An array that contains the amount of work indexed by tile number.
     */
    private int[] getTileWorks(boolean bias) {
        ScheduleModel model = new ScheduleModel(spaceTime, this, 
                scheduleOrder);
        
        model.createModel();
        
        int[] tileCosts = model.getTileCosts();
        
        if (bias)
            return biasCosts(tileCosts);
        else 
            return tileCosts;
    }
    
    /**
     * Given an array of tileCosts indexed by tile number, 
     * if an elements is less than the max, then multiply 
     * its value by 1.20.
     * 
     * @param tileCosts The array of tile costs.
     * 
     * @return A new biased array.
     */
    private int[] biasCosts(int[] tileCosts) {
        int maxWork = maxTileWork(tileCosts);
        for (int i = 0; i < tileCosts.length; i++) {
            if (tileCosts[i] < maxWork) 
                tileCosts[i] = (int)((double)tileCosts[i] * 1.20);
        }
            
        return tileCosts;
    }
    
    /**
     * Not used anymore.
     * 
     * @return Got me?
     */
    private int[] getTileWorksOld() {
        int[] tileCosts = new int[rawChip.getTotalTiles()];
        Iterator traces = scheduleOrder.iterator();
        
        while (traces.hasNext()) {
            Trace trace = (Trace)traces.next();
            FilterTraceNode node = trace.getFilterNodes()[0];
            RawTile tile = (RawTile)assignment.get(node);
            //the cost of the previous node, not used for first filter 
            int prevStart = tileCosts[tile.getTileNumber()];
                    
            //the first filter does not have to account for startup lag
            tileCosts[tile.getTileNumber()] += 
                partitioner.getFilterWorkSteadyMult(node);

            //now cycle thru the rest of the nodes...
            for (int f = 1; f < trace.getFilterNodes().length; f++) {
                node = trace.getFilterNodes()[f];
                //get this node's assignment;
                tile = (RawTile)assignment.get(node);
                //accounting for pipeling lag, when is the earliest I can start?
                
                //offset is the offset between the last filter's start time
                //and this tile's current work load, if it is positive,
                //this tile is free after the last time, so we should,
                //account for this time in the pipeline lag
                //if negative, this tile is free before the last tile was free
                //so it does not help, so set to zero
                int offSet = tileCosts[tile.getTileNumber()] - 
                   prevStart;
                
                offSet = Math.max(0, offSet);
                
                //now when is the earliest I can start in relation to the last tile
                //and the work of this tile
                int pipeLag = 
                    Math.max((partitioner.getFilterStartupCost(node) - offSet),
                            0);
                
                //set the prev start to when I started, for the next iteration
                prevStart = tileCosts[tile.getTileNumber()];
                //the new work for this tile, is my work + my startup cost...
                tileCosts[tile.getTileNumber()] += (pipeLag + 
                    partitioner.getFilterWorkSteadyMult(node));

               
            }
            //account for the cost of issuing its load dram commands 
            RawTile inputTile = 
                (RawTile)assignment.get(trace.getHead().getNextFilter());
            tileCosts[inputTile.getTileNumber()]+= ScheduleModel.DRAM_ISSUE_COST;
                        
            //account for the
            //cost of sending an item over the gdn if it uses it...
            RawTile outputTile = 
                (RawTile)assignment.get(trace.getTail().getPrevFilter());
            if (LogicalDramTileMapping.mustUseGdn(outputTile)) {
                tileCosts[outputTile.getTileNumber()] += (node.getFilter().getPushInt() * 
                        node.getFilter().getSteadyMult() * 
                        ScheduleModel.GDN_PUSH_COST);
            }
            
            //account for the cost of issuing its store dram command
            tileCosts[outputTile.getTileNumber()]+= ScheduleModel.DRAM_ISSUE_COST;
        }
         
        return tileCosts;
    }
    
    /**    
     * This method initalizes the simulated annealing algorithm.  It will
     * setup the file reading and writing structures and determine which 
     * filters need to be assigned to tiles.
     */
    public void initialize() {
        //generate the startup cost of each filter...         
        partitioner.calculateWorkStats();
        totalWork = 0;
        
        //create the filter list
        filterList = new LinkedList();
        Iterator traceNodes = 
            Util.traceNodeTraversal(partitioner.getTraceGraph());
        while (traceNodes.hasNext()) {
            TraceNode node = (TraceNode)traceNodes.next();
            //add filters to the list of things to assign to tiles,
            //but don't add file readers/writers... they will
            //"occupy" the tile of their neighbor stream...
            if (node.isFilterTrace() && 
                    !(node.getAsFilter().isPredefined()))  {
                totalWork += partitioner.getFilterWorkSteadyMult(node.getAsFilter());
                filterList.add(node);
            }
        }
        
        fileReaders = new HashSet();
        fileWriters = new HashSet();
        
        //set up the file readers and file writers list
        //these are filters the read directly from a file
        //or write directly to a file (not the file readers and writers themselves).
        for (int i = 0; i < partitioner.io.length; i++) {
            Trace trace = partitioner.io[i];
            if (trace.getHead().isFileOutput()) {
                assert trace.getHead().getSourceSet().size() == 1 :
                    "We don't support joined file writers yet.";
                fileWriters.add(trace.getHead().getSingleEdge().getSrc().getPrevFilter());
            }
            else if (trace.getTail().isFileInput()) {
                assert trace.getTail().getDestSet().size() == 1 :
                    "We don't support split file readers right now.";
                fileReaders.add(trace.getTail().getSingleEdge().getDest().getNextFilter());
            }
            else 
                assert false : "Trace in io[] is not a file trace.";
         }

    }

    
    
    /**
     * Calculate the standard deviation of the values in vals.
     * 
     * @param vals An array of ints.
     * 
     * @return The standard deviation of the values in <pre>vals</pre>.
     */
    public static double standardDeviation(int[] vals) {
        double squaredSum = 0.0;
        double sum = 0.0;
        
        for (int i = 0; i < vals.length; i++) { 
            squaredSum += ((double)vals[i] * (double)vals[i]);
            sum += (double)vals[i];
        }
        
        double mean  = sum / (double) vals.length;
        
        
        double stdDev = Math.sqrt((squaredSum / (double)vals.length) - (mean * mean));
        assert stdDev >= 0.0 : squaredSum + " " + mean + " " + vals.length + " " + 
        stdDev;
        return stdDev;
    }
    
    static {
        //initialize the direction strings
        //when attempting to find a new assignment for a slice,
        //for each filter we pick a random dirString and try to 
        //place the next filter at a tiles in the directions 
        //given by the string...
        dirStrings = new String[24];
        dirStrings[0] = "NESW";
        dirStrings[1] = "NEWS";
        dirStrings[2] = "NSEW";
        dirStrings[3] = "NSWE";
        dirStrings[4] = "NWSE";
        dirStrings[5] = "NWES";
        dirStrings[6] = "ENSW";
        dirStrings[7] = "ENWS";
        dirStrings[8] = "ESNW";
        dirStrings[9] = "ESWN";
        dirStrings[10] = "EWNS";
        dirStrings[11] = "EWSN";
        dirStrings[12] = "SNEW";
        dirStrings[13] = "SNWE";
        dirStrings[14] = "SENW";
        dirStrings[15] = "SEWN";
        dirStrings[16] = "SWEN";
        dirStrings[17] = "SWNE";
        dirStrings[18] = "WNES";
        dirStrings[19] = "WNSE";
        dirStrings[20] = "WENS";
        dirStrings[21] = "WESN";
        dirStrings[22] = "WSEN";
        dirStrings[23] = "WSNE";
    }
}

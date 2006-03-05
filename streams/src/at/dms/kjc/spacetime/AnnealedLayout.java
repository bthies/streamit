/**
 * 
 */
package at.dms.kjc.spacetime;

import at.dms.kjc.common.SimulatedAnnealing;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author mgordon
 *
 */
public class AnnealedLayout extends SimulatedAnnealing {
    /** if a tile does at least <BIG_WORKER> percentage of the bottle neck tile,
     * communication to and from it are included in the communication cost component
     * of the cost function.
     */
    private static final double BIG_WORKER = 0.9;
    
    private static final int ILLEGAL_COST = 1000000;
    /** the cost of outputing one item using the gdn versus using the static net
     for the final filter of a Trace */
    private static final int GDN_PUSH_COST = 2;
    /** the cost of issue a read or a write dram command on the tiles assigned 
     * to trace enpoints for each trace.
     */
    private static final int DRAM_ISSUE_COST = 5;
    
    private RawChip rawChip;
    private SpaceTimeSchedule spaceTime;
    private LinkedList filterList;
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
    
    private HashSet fileWriters;
    private HashSet fileReaders;
    
    public AnnealedLayout(SpaceTimeSchedule spaceTime) {
        super();
        this.spaceTime = spaceTime;
        rawChip = spaceTime.getRawChip();
        this.partitioner = spaceTime.partitioner;
        numTiles = rawChip.getTotalTiles();
        tiles = new RawTile[numTiles];
        for (int i = 0; i < numTiles; i++) 
            tiles[i] = rawChip.getTile(i);
    }

    public void run() {
        //run the simulated annealing with 2 iterations, 
        //100 attempts per iteration
        simAnnealAssign(2, 1000);
        printLayoutStats();
    }
    
    public void printLayoutStats() {
        int[] tileCosts = getTileWorks();
        
        System.out.println("Max Work Tile of layout is " + maxWorkTile + 
                ", it has work: " + maxTileWork(tileCosts));
        
       
        for (int i = 0; i < tileCosts.length; i++) 
            System.out.println("Tile " + i + " has work " + tileCosts[i]);
        
        System.out.println("Std Dev of work: " + standardDeviation(tileCosts));
        
        assert isLegalLayout() : 
            "Simulated annealing arrived at an illegal layout.";
    }
    
    /**
     * This function will perturb the configuration by moving
     * something around
     */
    public void swapAssignment() {
        //swapAssignmentIllegal();
        swapAssignmentLegal();
    }
    
    /** 
     * perturb the configuration by finding a new layout for a single trace
     *
     */
    public void swapAssignmentLegal() {
        Trace[] traces = partitioner.getTraceGraph();
        Trace reassignMe = traces[getRandom(traces.length)];
        
        assert newTraceAssignment(assignment, 
				  reassignMe.getHead().getNextFilter()) :
            "Error: could not find a legal layout for " + reassignMe + " during layout";
    }
   
    
    /**
     * Perturb the configuration by choosing a new assignment for a
     * single filter.  This could lead to an illegal layout because
     * we use only near-neighbor communication...
     */
    public void swapAssignmentIllegal() {
        //randomly find a filter
        FilterTraceNode filter = 
            (FilterTraceNode)filterList.get(getRandom(filterList.size()));
        
        //swap it to a random tile
        RawTile oldTile = (RawTile)assignment.get(filter);
        RawTile newTile = null;
        
        //find a new tile to swap it to...
        while (true) {
            newTile = 
                tiles[getRandom(rawChip.getTotalTiles())];
            if (newTile == oldTile)
                continue;
            else
                break;
        }
        //commit the new assignment
        assignment.remove(filter);
        assignment.put(filter, newTile);
    }

    /**
     *  
     */
    public void initialPlacement() {
       //try a random layout! nope
        //randomInitialPlacement();
        legalInitialPlacement();
    }
    
    /**
     * 
     * @param newAssign
     * @param node  call with first filterTraceNode of trace
     * @return true if we successfully laid out the trace, 
     * it should alway return true when finished recursing. 
     */
    private boolean newTraceAssignment(HashMap newAssign, 
            TraceNode node) {
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
		 (RawTile)newAssign.get(node.getPrevious())))
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
		
		newAssign.put(node, tile);
		if (newTraceAssignment(newAssign, node.getNext()))
		    return true;
	    }
	    //if we get here, we have failed to find a layout.
	    return false;
        }
        else {
            //we are not the first tile so choose some
            RawTile prevTile = (RawTile)newAssign.get(node.getPrevious());
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
                while (newAssign.containsKey(prevNode)) {
                    if (tile == newAssign.get(prevNode)){
                        prevMap = true;
                        break;
                    }
                    prevNode = prevNode.getPrevious();
                }

                //try another tile
                if (prevMap)
                    continue;
                                       
                //assign this filter to the tile in this direction
                newAssign.put(filter, tile);
                //try the assignment to the tile in this direction
                if (newTraceAssignment(newAssign, node.getNext()))
                    return true; //if it worked return!
                else
                    continue; //if not, try another direction if there are more
            }
            //if we get here, we have failed
            return false;
        }
    }
 
    /**
     * We want to try to place <filter> on <tile> but it is a file reader,
     * so we have to make sure that no other file reading filters are 
     * mapped to this tile.  If so return false.
     * 
     * @param filter
     * @param tile
     * @return
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
     * We want to try to place <filter> on <tile> but <filter> writes 
     * to a file, so we have to make sure that no other filters that writes 
     * to a file is mapped to this tile.  If there is one, return false. 
     * @param filter
     * @param tile
     * @return
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
	    if (occupied == tile)
		return false;
	}
	//no file writer mapped to tile, return true
	return true;
    }
    
    /**
     * For each trace, generate a random legal tile assignment 
     * as the initial layout.
     */
    private void legalInitialPlacement() {
        Trace[] traces = partitioner.getTraceGraph();
        for (int i = 0; i < traces.length; i++) {
            assert newTraceAssignment(assignment, traces[i].getHead().getNextFilter()) :
                "Error: could not find a legal layout for " + traces[i] + " during initial layout";
        }
    }
    
    /** 
     * The initial layout is random and probably illegal! 
     */
    private void randomInitialPlacement() {
        Iterator filters = filterList.iterator();
        
        while (filters.hasNext()) {
            FilterTraceNode filter = (FilterTraceNode)filters.next();
            
            assignment.put(filter, 
                    rawChip.getTile(getRandom(rawChip.getTotalTiles())));
        }
    }
    
    /** 
     * @return The maximum work for a tile (accounting for 
     * pipeline bubbles) plus the cost of intertrace communication.
     *  
     */
    public double placementCost(boolean debug) {
        int[] tileCosts = getTileWorks();
        
        double workOfBottleNeckTile = (double)maxTileWork(tileCosts);
        
        double cost = workOfBottleNeckTile //+ standardDeviation(tileCosts)  
                   + (double)bigWorkersCommEstimate(tileCosts);
        
        /*
        if (!isLegalLayout())
            cost += ILLEGAL_COST;
        */
        return cost;
    }

    /** check if the layout is legal, meaning that communicating filters of a
     * trace are placed on neighboring tiles.
     * 
     * @return
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
     * @return An estimate of the cost of communication for this layout.
     */
    private int bigWorkersCommEstimate(int tileCosts[]) {
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
        
        //guess at the initial communication latency for 
        //edges between slices...
        for (int i = 0; i < traces.length; i++) {
            Trace trace = traces[i];
            Iterator edges = trace.getTail().getDestSet().iterator();
            while (edges.hasNext()) {
                Edge edge = (Edge)edges.next();
                //get the port that source is writing to
                RawTile srcTile = (RawTile)assignment.get(edge.getSrc().getPrevFilter());
                RawTile dstTile = (RawTile)assignment.get(edge.getDest().getNextFilter());
                //we only care about the tiles that do a bunch of work!
                if (bigWorkers.contains(srcTile) || bigWorkers.contains(dstTile)) {
                   
                    IODevice srcPort = 
                        LogicalDramTileMapping.getHomeDram
                        (srcTile);
                    //get the por that the dest is reading from
                    IODevice dstPort = 
                        LogicalDramTileMapping.getHomeDram
                        (dstTile);
                    
                    estimate += (rawChip.manhattanDistance(srcPort, dstPort));
                }
            }
        }
        return estimate;
    }
    
    /**
     * @return The maximum amount of work that is performed on a tile
     * using a formula that accounts for startup costs of filter (pipeline lag).
     * Also, if the filter outputs using the gdn account 
     * for the added cost of sending an item.
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
     * @return The work estimation of each tile.
     */
    private int[] getTileWorks() {
        int[] tileCosts = new int[rawChip.getTotalTiles()];
        
        Iterator filterNodes = assignment.keySet().iterator();
        while (filterNodes.hasNext()) {
            FilterTraceNode filterNode = (FilterTraceNode)filterNodes.next();
            //get the cost of a filter as its total work plus the pipeline lag
            //(startupcost)
            int filterCost = partitioner.getFilterWorkSteadyMult(filterNode) +
            partitioner.getFilterStartupCost(filterNode);
            
            //if this filter is an trace endpoint, account for the cost
            //of issuing its dram command... 
            if (filterNode.getNext().isOutputTrace())
                filterCost += DRAM_ISSUE_COST;
            if (filterNode.getPrevious().isInputTrace())
                filterCost += DRAM_ISSUE_COST;

            RawTile tile = (RawTile)assignment.get(filterNode);
            //if the filter outputs to the gdn, add some fixed cost per item pushed
            //in the steady
            if (filterNode.getNext().isOutputTrace() && 
                    LogicalDramTileMapping.mustUseGdn(tile)) {
                filterCost += (filterNode.getFilter().getPushInt() * 
                        filterNode.getFilter().getSteadyMult() * 
                        partitioner.steadyMult * GDN_PUSH_COST);
            }
                
            
            //add it to the tile costs...
            tileCosts[tile.getTileNumber()] += 
                filterCost;
        }
        return tileCosts;
    }
    
    /**    
     * initalize the simulated annealing.
     */
    public void initialize() {
        //generate the startup cost of each filter...         
        partitioner.calculateStartupCosts();
        
        //create the filter list
        filterList = new LinkedList();
        Iterator traceNodes = 
            Util.traceNodeTraversal(partitioner.getTraceGraph());
        while (traceNodes.hasNext()) {
            TraceNode node = (TraceNode)traceNodes.next();
                        
            if (node.isFilterTrace()) {
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
     * @param vals
     * @return The standard deviation of the values in <vals>.
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

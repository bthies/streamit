package at.dms.kjc.tilera;

import java.util.HashMap;

import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import java.util.LinkedList;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.iterator.IterFactory;
import at.dms.kjc.iterator.SIRFilterIter;
import at.dms.kjc.slicegraph.fission.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.List;

/**
 * 
 * 
 * @author mgordon
 *
 */
public class TMD extends Scheduler {

    private double DUP_THRESHOLD;
    public LevelizeSliceGraph lsg;
    private HashMap<Slice, Integer> fizzAmount;
    public static final int FISS_COMP_COMM_THRESHOLD = 10;
    /** if true, then we have slices with fanout greater than 2 and we do not 
     * hava a layout where communicating slices are neighbors, boo
     */
    public boolean fallBackLayout;
    
    public TMD() {
        super();
        fizzAmount = new HashMap<Slice, Integer>();
        DUP_THRESHOLD = ((double)KjcOptions.dupthresh) / 100.0;
    }
    
    /** Get the tile for a Slice 
     * @param node the {@link at.dms.kjc.slicegraph.SliceNode} to look up. 
     * @return the tile that should execute the {@link at.dms.kjc.slicegraph.SliceNode}. 
     */
    public Tile getComputeNode(SliceNode node) {
        assert layoutMap.keySet().contains(node);
        return layoutMap.get(node);
    }
    
    
    /** Set the Tile for a Slice 
     * @param node         the {@link at.dms.kjc.slicegraph.SliceNode} to associate with ...
     * @param tile   The tile to assign the node
     */
    public void setComputeNode(SliceNode node, Tile tile) {
        assert node != null && tile != null;
        layoutMap.put(node, tile);
        //remember what filters each tile has mapped to it
        //System.out.println("Settting " + node + " to tile " + tile.getTileNumber());
        if (tile.isComputeTile())
            tile.getComputeCode().addFilter(node.getAsFilter());
    }
    
    /**
     * Assign the filternodes of the slice graph to tiles on the chip based on the levels
     * of the graph. 
     */
    public void runLayout() {
        assert graphSchedule != null : 
            "Must set the graph schedule (multiplicities) before running layout";
        
        lsg = new LevelizeSliceGraph(graphSchedule.getSlicer().getTopSlices());
        Slice[][] levels = lsg.getLevels();
        
        
        //if the fan out of any non-predefined filter is > 2 then we have to use the fallback
        //layout algorithm and global barriers...
        int maxFanout = 0;
        fallBackLayout = false;
        for (int l = 0; l < levels.length; l++) {
            for (int f = 0; f < levels[l].length; f++) {
                if (levels[l][f].getFirstFilter().isPredefined())
                    continue;
                if (levels[l][f].getTail().getDestSet(SchedulingPhase.STEADY).size() > maxFanout) {
                    maxFanout = levels[l][f].getTail().getDestSet(SchedulingPhase.STEADY).size();
                    if (maxFanout > 2) {
                        fallBackLayout = true;
                        System.out.println(levels[l][f] + " has fanout > 2!");
                    }
                }
            }
        }
        
        System.out.println("Max slice fanout: " + maxFanout);

        if (fallBackLayout)
            fallBackLayout();
        else 
            neighborsLayout(levels);
    }
    
    /**
     * In this optimized layout algorithm communicating filters that are not placed on the
     * same tile are placed on neighboring tiles.
     */
    private void neighborsLayout(Slice[][] levels) {
        //we know fanout <= 2 and each level has number_of_tiles slices
        
        //assert that the first level only has a file reader and that we always have a
        //file reader
        //assert levels[0].length == 1 && levels[0][0].getFirstFilter().isFileInput();
        
        //place each slice in a set that will be mapped to the same tile
        System.out.println("Partitioning into same tile sets...");
        Set<Set<Slice>> sameTile = createSameTileSets(levels);
        assert sameTile.size() <= TileraBackend.chip.abstractSize() : 
            sameTile.size() + " " + TileraBackend.chip.abstractSize();
        Tile nextToAssign = TileraBackend.chip.getComputeNode(0, 0);
        Set<Slice> current = sameTile.iterator().next();
        Set<Set<Slice>> done = new HashSet<Set<Slice>>();
        System.out.println("Beginning Neighbors Layout...");
        
        while (true) {
            //system.out.println("Assigning " + current + " to " + nextToAssign.getTileNumber());
            assignSlicesToTile(current, nextToAssign);
            done.add(current);
            assert done.contains(current);
                        
            //now find the next slice set to assign to the snake
            //first find a slice that has a nonlocal output, so we can make the set it is in
            //neighbors with the slice we just assigned...
            Slice nonLocalOutput = null;
            for (Slice slice : current) {
                if (slice.getTail().getDestSet(SchedulingPhase.STEADY).size() > 1) 
                    nonLocalOutput = slice;
                else if (slice.getTail().getDestSet(SchedulingPhase.STEADY).size() == 1) {
                    Slice destSlice = slice.getTail().getDestSlices(SchedulingPhase.STEADY).iterator().next();
                    if (current != getSetWithSlice(sameTile, destSlice) && 
                            getSetWithSlice(sameTile, destSlice) != null) {
                        nonLocalOutput = slice;
                    }
                }
            }
            //nothing else to assign
            if (done.size() == sameTile.size()) {
                break;
            }
            
            current = null;
            //set the next set of slice to assign to the next tile in the snake
            //fis
            if (nonLocalOutput != null) {
                //one of the slices does communicate with a slice not of its own set
                for (Slice slice : nonLocalOutput.getTail().getDestSlices(SchedulingPhase.STEADY)) {
                    Set<Slice> set = getSetWithSlice(sameTile, slice);
                    if (set != current && 
                            !done.contains(set)) {
                        current = getSetWithSlice(sameTile, slice);
                        break;
                    }
                }
            }
            //if we didn't find a communicating set to make a neighbor, then just pick any old set of slices 
            if (current == null) {
                for (Set<Slice> next : sameTile) {
                    if (!done.contains(next))
                        current = next;
                }
            }

            assert current != null;
            nextToAssign = nextToAssign.getNextSnakeTile();
        }
        
        System.out.println("End Neighbors Layout...");
        
    }
    
    private void assignSlicesToTile(Set<Slice> slices, Tile tile) {
        for (Slice slice : slices) {
            //System.out.println("Assign " + slice.getFirstFilter() + " to tile " + tile.getTileNumber());
            setComputeNode(slice.getFirstFilter(), tile);
        }
    }
    
    /**
     * 
     */
    private Set<Set<Slice>> createSameTileSets(Slice[][] levels) {
        HashSet<Set<Slice>> sameTile = new HashSet<Set<Slice>>();
        for (int l = 0; l < levels.length; l++) {
            //System.out.println("Level " + l + " has size " + levels[l].length);
            LinkedList<Slice> alreadyAssigned = new LinkedList<Slice>();
            for (int s = 0; s < levels[l].length; s++) {
                Slice slice = levels[l][s];
                //assign predefined to offchip memory and don't add them to any set
                if (slice.getFirstFilter().isPredefined()) {
                    setComputeNode(slice.getFirstFilter(), TileraBackend.chip.getOffChipMemory());
                } else {
                    //find the input with the largest amount of data coming in
                    //and put this slice in the set that the max input is in
                    int bestInputs = -1;
                    Set<Slice> theBest = null;;
                    
                    for (Edge edge : slice.getHead().getSourceSet(SchedulingPhase.STEADY)) {
                        if (slice.getHead().getWeight(edge, SchedulingPhase.STEADY) > bestInputs) {
                            //the set we want to see if this slice should be added to
                            Set<Slice> testSet = getSetWithSlice(sameTile, edge.getSrc().getParent());
                            
                            //if the test set is null, then we have not put the upstream slice on the chip 
                            if (testSet == null) {
                                continue;
                            }
                            
                            //check if the best contains a slice from this level already, if so, we cannot
                            //assign another slice so continue
                            boolean canUse = true;
                            for (Slice seen : alreadyAssigned) {
                                if (testSet.contains(seen))
                                    canUse = false;
                            }
                            if (!canUse)
                                continue;
                            
                            //otherwise, we have not added a slice from this level to this set, so 
                            //we can use it
                            theBest = testSet;
                            bestInputs = slice.getHead().getWeight(edge, SchedulingPhase.STEADY);
                            
                        }
                    }
                    //remember that we have assigned this slice in the level
                    alreadyAssigned.add(slice);
                    //no upstream slice is in a set
                    if (theBest == null) {
                        //System.out.println("no best: " + slice.getFirstFilter());
                        //create a new set and add it to the set of sets
                        HashSet<Slice> newSet = new HashSet<Slice>();
                        newSet.add(slice);
                        sameTile.add(newSet);
                    } else {
                        //we should put slice in the set that is the best
                        theBest.add(slice);
                    }
                }
            }
        }
        return sameTile;
    }
    
    /**
     * Give a set of set of slices and slice return the set of slices that contains
     * slice.
     */
    private Set<Slice> getSetWithSlice(Set<Set<Slice>> sameTile, Slice slice) {
        Set<Slice> set = null;
        for (Set<Slice> current : sameTile) {
            if (current.contains(slice)) {
                assert set == null;
                set = current;
            }
        }
        return set;
    }
    
    /**
     * This layout algorithm does not try to place communicating slices as neighbors,
     * it is used when the fanout of any slice is greater than 2.
     */
    private void fallBackLayout() {
        System.out.println("Using fall back layout algorithm because a slice has fanout > 2.  Forced to use global barrier.");
        Slice[][] levels = lsg.getLevels();
        
        System.out.println("Levels: " + levels.length);
        
        for (int l = 0; l < levels.length; l++) {
            assert levels[l].length  <= TileraBackend.chip.abstractSize() : 
                "Too many filters in level for TMD layout!";
            HashSet<Tile> allocatedTiles = new HashSet<Tile>(); 
            
            if (levels[l].length == 1 && levels[l][0].getFirstFilter().isPredefined()) {
                //we only support full levels for right now other than predefined filters 
                //that are not fizzed
               setComputeNode(levels[l][0].getFirstFilter(), TileraBackend.chip.getOffChipMemory());
            } else {
                for (int f = 0; f < levels[l].length; f++) {
                    Slice slice = levels[l][f];
                    Tile theTile = tileToAssign(slice, TileraBackend.chip, allocatedTiles);
                    setComputeNode(slice.getFirstFilter(), theTile);
                    allocatedTiles.add(theTile);
                }
            }
        }
    }
 
    private Tile tileToAssign(Slice slice, TileraChip chip, Set<Tile> allocatedTiles) {
        Tile theBest = null;
        int bestInputs = -1;
           
        //add the tiles to the list that are allocated to upstream inputs
        for (Edge edge : slice.getHead().getSourceSet(SchedulingPhase.STEADY)) {
            Tile upstreamTile = getComputeNode(edge.getSrc().getPrevious());
            if (upstreamTile == TileraBackend.chip.getOffChipMemory())
                continue;
            if (allocatedTiles.contains(upstreamTile))
                continue;
            
            if (slice.getHead().getWeight(edge, SchedulingPhase.STEADY) > bestInputs) {
                theBest = upstreamTile;
                bestInputs = slice.getHead().getWeight(edge, SchedulingPhase.STEADY);
            }
        }
        
                
        if (theBest == null) {
            //could not find a tile that was allocated to an upstream input
            //just pick a tile
            for (Tile tile : chip.getAbstractTiles()) {
                if (allocatedTiles.contains(tile))
                    continue;
                theBest = tile;
                break;
            }
        }

        assert theBest != null;
        return theBest;
    }
    
    /**
     * Run the Time-Multiplexing Data-parallel scheduler.  Right now, it assumes 
     * a pipeline of stateless filters
     */
    public void run(int tiles) {
        //if we are using the SIR data parallelism pass, then don't run TMD
        if (KjcOptions.dup == 1)
            return;
        
        calculateFizzAmounts(tiles);
        
        int factor = multiplicityFactor(tiles);
        System.out.println("Using fission steady multiplicty factor: " + factor);
        
        LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());
        
        //go through and multiply the steady multiplicity of each filter by factor
        for (Slice slice : slices) {
            FilterContent filter = slice.getFirstFilter().getFilter();
            filter.multSteadyMult(factor);
         }
        //must reset the filter info's because we have changed the schedule
        FilterInfo.reset();
        
        TileraBackend.scheduler.graphSchedule.getSlicer().dumpGraph("before_fission.dot", 
                null);
        
        int maxFission = 0;
        int i = 0;
        //go throught and perform the fission
        for (Slice slice : slices) {
            if (fizzAmount.containsKey(slice) && fizzAmount.get(slice) > 1) {
                if (Fissioner.doit(slice, graphSchedule.getSlicer(), fizzAmount.get(slice))) {
                    System.out.println("Fissed " + slice.getFirstFilter() + " by " + fizzAmount.get(slice));
                    if (fizzAmount.get(slice) > maxFission)
                        maxFission = fizzAmount.get(slice);
                }
                TileraBackend.scheduler.graphSchedule.getSlicer().dumpGraph("fission_pass_" + i + ".dot", 
                        null);
                i++;
            }
        }
        
        System.out.println("Max fission amount: " + maxFission);
        
        //because we have changed the multiplicities of the FilterContents
        //we have to reset the filter info's because they cache the date of the 
        //FilterContents
        FilterInfo.reset();
        
        
    }
    
    /**
     * Return the level that this slice occupies.
     */
    public int getLevel(Slice slice) {
        return lsg.getLevel(slice);
    }
    
    public int numLevels() {
        return lsg.getLevels().length;
    }
    
    public int getLevelSize(int l) {
        return lsg.getLevels()[l].length;
    }
    
    /**
     * 
     */
    public void calculateFizzAmounts(int totalTiles) {
        Slice[][] origLevels = new LevelizeSliceGraph(graphSchedule.getSlicer().getTopSlices()).getLevels();
        
        //assume that level 0 has a file reader and the last level has a file writer
        for (int l = 0; l < origLevels.length; l++) {
            //for the level, calculate the total work and create a hashmap of fsn to work
            HashMap <FilterSliceNode, Integer> workEsts = new HashMap<FilterSliceNode, Integer>();
            LinkedList<FilterSliceNode> sortedWork = new LinkedList<FilterSliceNode>();
            //this is the total amount of work
            int levelTotal = 0;
            //the total amount of stateless work
            int slTotal = 0;
            int cannotFizz = 0;            
            for (int s = 0; s < origLevels[l].length; s++) {
               FilterSliceNode fsn = origLevels[l][s].getFirstFilter();
               FilterContent fc = fsn.getFilter();
               if (fsn.isPredefined())
                   workEsts.put(fsn, 0);
               //the work estimation is the estimate for the work function  
               int workEst = SliceWorkEstimate.getWork(origLevels[l][s]);
               workEsts.put(fsn, workEst);
               //insert into the sorted list of works
               int index = 0;
               for (index = 0; index <= sortedWork.size(); index++) {
                       if (index == sortedWork.size() ||
                               workEst > workEsts.get(sortedWork.get(index))) {
                           break;
                       }
               }   
               
               levelTotal += workEst;
               int commRate = (fc.getPushInt()  + fc.getPopInt()) * fc.getMult(SchedulingPhase.STEADY);
               if (Fissioner.canFizz(origLevels[l][s], true)) {
                   if (workEst / commRate <= FISS_COMP_COMM_THRESHOLD) {
                       System.out.println("Dont' fiss " + fsn + ", too much communication!");
                       cannotFizz++;
                   } else {
                       slTotal += workEst;
                       sortedWork.add(index, fsn);
                   }
               } 
               else {
                   System.out.println("Cannot fiss " + fsn);
                   cannotFizz++;
               }
            }
            
            //tiles available, don't count stateful filters
            int availTiles = totalTiles - cannotFizz;
            //can't do anything, no avail tiles
            if (availTiles == 0)
                continue;
            //now go through the level and parallelize each filter according to the work it does in the
            //level
            int tilesUsed = cannotFizz;
            int maxLevelWork = 0;
            int perfectPar = slTotal / availTiles; 
                
            for (int f = 0; f < sortedWork.size(); f++) {
                FilterSliceNode fsn = sortedWork.get(f);
                FilterContent fc = fsn.getFilter();
                //don't parallelize file readers/writers
                if (fsn.isPredefined())
                    continue;
                int commRate = (fc.getPushInt() + fc.getPopInt()) * fc.getMult(SchedulingPhase.STEADY);
                //if we cannot fizz this filter, do nothing
                if (!Fissioner.canFizz(fsn.getParent(), false) || 
                        workEsts.get(fsn) / commRate <= FISS_COMP_COMM_THRESHOLD) {
                    assert false;
                } 
                //System.out.println("Calculating fizz amount for: " + fsn + "(" + availTiles + " avail tiles)");
                
                double faFloat = 
                    (((double)workEsts.get(fsn)) / ((double)slTotal)) * ((double)availTiles);
                int fa = 0;
                if (faFloat < 1.0) 
                    fa = 1;
                else {
                    fa = (int)Math.floor(faFloat);
                  /*  if (f < sortedWork.size() / 2) 
                        fa = (int)Math.ceil(faFloat);
                    else 
                        fa = (int)Math.floor(faFloat); */
                }
                /*
                double faFloat = (((double)workEsts.get(fsn)) / ((double)slTotal)) * ((double)availTiles);
                int fa = (int)Math.ceil(faFloat);
                 */
                //System.out.println(l + ": " + workEsts.get(fsn) + " / " + slTotal + " * " + availTiles + " = " + fa);
             
                //availTiles -= fa;
                int thisWork = workEsts.get(fsn) / fa;
                if (thisWork > maxLevelWork)
                    maxLevelWork = thisWork;
             
                fizzAmount.put(fsn.getParent(), (int)fa);
                assert fa > 0 : fsn;
                tilesUsed += fa;
            }
            
            System.out.println("Level " + l + ": max work: " + maxLevelWork + ", perfect: " + perfectPar);
            
            assert tilesUsed <= totalTiles : "Level " + l + " has too many slices: " + tilesUsed;
            
            //assert that we use all the tiles for each level
            if (tilesUsed < totalTiles) 
                System.out.println("Level " + l + " does not use all the tiles available for TMD " + tilesUsed);
        }
        
        
    }
    
    /**
     * Determine the factor that we are going to multiple each slice by so that 
     * fission on the slice graph is legal.  Keep trying multiples of the number 
     * tiles until each slice passes the tests for legality.
     *  
     * @param tiles The number of tiles we are targeting 
     * @return the factor to multiply the steady multiplicities by
     */
    private int multiplicityFactor(int tiles) {
        int maxFactor = 1;
        LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());
        
        for (Slice slice : slices) {
         
            if (slice.getFirstFilter().isPredefined())
                continue;
            
            FilterInfo fi = FilterInfo.getFilterInfo(slice.getFirstFilter());
            //nothing to do for filters with no input
            if (fi.pop == 0)
                continue;

            if (fizzAmount.containsKey(slice) && fizzAmount.get(slice) > 1) {
              //this works only for pipelines right now, so just that we have at most
                //one input and at most one output for the slice
                assert slice.getHead().getSourceSet(SchedulingPhase.STEADY).size() <= 1;
                
                //check that we have reached the threshold for duplicated items
                int threshFactor = (int)Math.ceil((((double)(fi.peek - fi.pop)) * fizzAmount.get(slice)) / 
                        ((double)(DUP_THRESHOLD * (((double)fi.pop) * ((double)fi.steadyMult)))));

                //this factor makes sure that copydown is less than pop*mult*factor
                int cdFactor = (int)Math.ceil(((double)fi.copyDown) / ((double)(fi.pop * fi.steadyMult)));

                int myFactor = Math.max(cdFactor, threshFactor);

                if (maxFactor < myFactor)
                    maxFactor = myFactor;
            }
        }
        
        //now find the smallest integer less than or equal to maxfactor that is 
        //divisible by all the fissAmounts
        boolean dividesAll = false;
        while (!dividesAll) {
            dividesAll = true;
            for (Integer fa : fizzAmount.values()) {
                if (maxFactor % fa.intValue() != 0) {
                    dividesAll = false;
                    break;
                }
            }
            if (!dividesAll)
                maxFactor++;
        }
        
        return maxFactor;
    }
    
    /**
     * Returns the number of peeking filters in the graph.
     */
    public static int countPeekingFilters(SIRStream str) {
        //Don't count identity filters
        final int[] count = { 0 };
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    if (self.getPeekInt() > self.getPopInt())
                        count[0]++;
                }});
        return count[0];
    }
    
    /**
     * Returns the number of filters in the graph.
     */
    public static int countFilters(SIRStream str) {
        //Don't count identity filters
        final int[] count = { 0 };
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self,
                                        SIRFilterIter iter) {
                    if (!(self instanceof SIRIdentity))
                        count[0]++;
                }});
        return count[0];
    }
    
    public static SIRStream SIRFusion(SIRStream str, int tiles) {
        KjcOptions.partition_greedier = true;
        KjcOptions.partition_dp = false;
        while (!allLevelsFit(str, tiles)) {
            int tilesNeeded = countFilters(str);
            str = at.dms.kjc.sir.lowering.partition.Partitioner.doit(str,
                    tilesNeeded - 1, false, false, true);
            StreamItDot.printGraph(str, "tmd_sir_fusion.dot");
        }
        KjcOptions.partition_greedier = false;
        return str;
    }
    
    /**
     * Check to see that all filters in the SIR graph have fewer cousins than there 
     * are tiles of the chip.
     * 
     * @param str The stream graph
     * @param tiles The number of cores of the target machine
     */
    public static SIRStream fuseCousins(SIRStream str, int tiles) {
        KjcOptions.partition_greedier = true;       
        KjcOptions.partition_dp = false;
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();
               
        for (int i = workList.size() - 1; i >= 0; i--) {
            SIRFilter filter = workList.getFilter(i);
                    
            int cousins = 1;
            
            SIRContainer container = filter.getParent();
            
            while (container != null) {
                if (container instanceof SIRSplitJoin) {
                    cousins *= (((SIRSplitJoin)container).getParallelStreams().size());
                    if (cousins > tiles) {
                        at.dms.kjc.sir.lowering.partition.Partitioner.doit(container,
                                tiles, false, false, true);
                    }
                }
                container = container.getParent();
            }
        }
        
        KjcOptions.partition_greedier = false;
        return str;
    }
   
    
    /**
     * Check to see that all filters in the SIR graph have fewer cousins than there 
     * are tiles of the chip.
     * 
     * @param str The stream graph
     * @param tiles The number of cores of the target machine
     */
    public static boolean allLevelsFit(SIRStream str, int tiles) {
                
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();
               
        for (int i = workList.size() - 1; i >= 0; i--) {
            SIRFilter filter = workList.getFilter(i);
                    
            int cousins = getNumCousins(filter); 
            if (cousins > tiles) {
                System.out.println(filter + " cousins: " + cousins);
                return false;                
            }
        }
        return true;
    }
   
    
    /**
    * Returns number of parallel streams that are at same nesting
    * depth as <filter> relative to the top-level splitjoin.  Assumes
    * that the splitjoin widths on the path from <filter> to the
    * top-level splitjoin are symmetric across other siblings.
    */
   private static int getNumCousins(SIRFilter filter) {
       int cousins = 1;
       SIRContainer container = filter.getParent();
       while (container != null) {
           if (container instanceof SIRSplitJoin) {
               cousins *= (((SIRSplitJoin)container).getParallelStreams().size());
           }
           container = container.getParent();
       }
       return cousins;
   }
   
   
}

package at.dms.kjc.tilera;

import java.util.HashMap;

import at.dms.kjc.sir.SIRContainer;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRSplitJoin;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.lowering.fission.StatelessDuplicate;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import java.util.LinkedList;
import at.dms.kjc.backendSupport.*;
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

    private double DUP_THRESHOLD = .1;
    private LevelizeSliceGraph lsg;
    private HashMap<Slice, Integer> fizzAmount;
    public static final int FISS_COMP_COMM_THRESHOLD = 10;
    
    public TMD() {
        super();
        fizzAmount = new HashMap<Slice, Integer>();
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
        boolean fallBack = false;
        for (int l = 0; l < levels.length; l++) {
            if (fallBack)
                break;
            if (levels[l].length != TileraBackend.chip.abstractSize()  && 
                    !(levels[l].length == 1 && levels[l][0].getFirstFilter().isPredefined())) { 
                fallBack = true;
                break;
            }
            for (int f = 0; f < levels[l].length; f++) {
                if (levels[l][f].getFirstFilter().isPredefined())
                    continue;
                if (levels[l][f].getTail().getDestSet(SchedulingPhase.STEADY).size() > 2) {
                    fallBack = true;
                    break;
                }
            }
        }
        
        if (fallBack)
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
        assert levels[0].length == 1 && levels[0][0].getFirstFilter().isFileInput();
        
        //place each slice in a set that will be mapped to the same tile
        Set<Set<Slice>> sameTile = createSameTileSets(levels);
        assert sameTile.size() == TileraBackend.chip.abstractSize();
        Tile nextToAssign = TileraBackend.chip.getComputeNode(0, 0);
        Set<Slice> current = sameTile.iterator().next();
        Set<Set<Slice>> done = new HashSet<Set<Slice>>();
        
        while (true) {
            assignSlicesToTile(current, nextToAssign);
            done.add(current);
            assert done.contains(current);
                        
            //now find the next slice set to assign to the snake
            //first find a slice that has a nonlocal output
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
            if (done.size() == sameTile.size())
                break;
            
            //set the next set of slice to assign to the next tile in the snake
            if (nonLocalOutput == null) {
                //does not communicate with anyone, so pick any slice set to layout next
                current = null;
                for (Set<Slice> next : sameTile) {
                    if (!done.contains(next))
                        current = next;
                }
                assert current != null;
            } else {
                //one of the slices does communicate with a slice not of its own set
                Set<Slice> nonLocal = null;
                for (Slice slice : nonLocalOutput.getTail().getDestSlices(SchedulingPhase.STEADY)) {
                    if (getSetWithSlice(sameTile, slice) != current) {
                        nonLocal = getSetWithSlice(sameTile, slice);
                        break;
                    }
                }
                
                assert nonLocal != null;
                //check that all non-local slices that are communicated to from current 
                //are in the same set
                for (Slice slice: current) {
                    for (Slice output : slice.getTail().getDestSlices(SchedulingPhase.STEADY)) {
                        assert current == getSetWithSlice(sameTile, output) ||
                            nonLocal == getSetWithSlice(sameTile, output) ||
                            output.getFirstFilter().isFileOutput();
                    }
                }
                
                current = nonLocal;
            }
            
            nextToAssign = nextToAssign.getNextSnakeTile();
        }
        
        
        
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
                            theBest = getSetWithSlice(sameTile, edge.getSrc().getParent());
                            bestInputs = slice.getHead().getWeight(edge, SchedulingPhase.STEADY);
                        }
                    }
                    //no upstream slice is in a set
                    if (theBest == null) {
                        //for now assume that there is always one file reader that feeds only level 1
                        assert l == 1;
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
            
            if (levels[l].length < TileraBackend.chip.abstractSize()) {
                //we only support full levels for right now other than predefined filters 
                //that are not fizzed
                assert levels[l].length == 1 && levels[l][0].getFirstFilter().isPredefined() :
                    "Level " + l + " of slice graph has fewer filters than tiles.";
                
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
        
        //make sure that all filters that execute in the init stage are mapped to the same tile!
        if (slice.getFirstFilter().getFilter().getInitMult() > 0) {
            assert slice.getHead().getSources(SchedulingPhase.INIT).length <= 1;
            if (slice.getHead().getSources(SchedulingPhase.INIT).length  == 1 && 
                    !slice.getHead().getSources(SchedulingPhase.INIT)[0].getSrc().getPrevFilter().isPredefined()) {
                assert theBest == getComputeNode(
                           slice.getHead().getSources(SchedulingPhase.INIT)[0].getSrc().getPrevFilter()) :
                               slice + " " + theBest.getTileNumber() + " ? " + slice.getHead().getSources(SchedulingPhase.INIT)[0].getSrc().getParent() + 
                               " " + getComputeNode(
                                       slice.getHead().getSources(SchedulingPhase.INIT)[0].getSrc().getPrevFilter()).getTileNumber();
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
        
        //go throught and perform the fission
        for (Slice slice : slices) {
            if (fizzAmount.containsKey(slice) && fizzAmount.get(slice) > 1) {
                if (Fissioner.doit(slice, fizzAmount.get(slice))) {
                    System.out.println("Fissing " + slice.getFirstFilter() + " by " + fizzAmount.get(slice));
                }
            }
        }
        
        //because we have changed the multiplicities of the FilterContents
        //we have to reset the filter info's because they cache the date of the 
        //FilterContents
        FilterInfo.reset();
        
        
    }
    
    /**
     * 
     */
    public void calculateFizzAmounts(int totalTiles) {
        Slice[][] origLevels = new LevelizeSliceGraph(graphSchedule.getSlicer().getTopSlices()).getLevels();
        
        //assume that level 0 has a file reader and the last level has a file writer
        for (int l = 1; l < origLevels.length - 1; l++) {
            //for the level, calculate the total work and create a hashmap of fsn to work
            HashMap <FilterSliceNode, Integer> workEsts = new HashMap<FilterSliceNode, Integer>();
            //this is the total amount of work
            int levelTotal = 0;
            //the total amount of stateless work
            int slTotal = 0;
            //tiles available, don't count stateful filters
            int availTiles = totalTiles;
            for (int s = 0; s < origLevels[l].length; s++) {
               FilterSliceNode fsn = origLevels[l][s].getFirstFilter();
               FilterContent fc = fsn.getFilter();
               if (fsn.isPredefined())
                   workEsts.put(fsn, 0);
               //the work estimation is the estimate for the work function  
               int workEst = SliceWorkEstimate.getWork(origLevels[l][s]);
               workEsts.put(fsn, workEst);
               levelTotal += workEst;
               int commRate = fc.getPushInt() * fc.getPopInt() * fc.getMult(SchedulingPhase.STEADY);
               if (Fissioner.canFizz(origLevels[l][s], true)) {
                   if (workEst / commRate <= FISS_COMP_COMM_THRESHOLD) {
                       System.out.println("Dont' fiss " + fsn + ", too much communication!");
                   } else
                       slTotal += workEst;
               } 
               else {
                   System.out.println("Cannot fiss " + fsn);
                   availTiles--;
               }
            }
               
            //now go through the level and parallelize each filter according to the work it does in the
            //level
            int tilesUsed = 0;
             
            for (int s = 0; s < origLevels[l].length; s++) {
                FilterSliceNode fsn = origLevels[l][s].getFirstFilter();
                FilterContent fc = fsn.getFilter();
                //don't parallelize file readers/writers
                if (fsn.isPredefined())
                    continue;
                int commRate = fc.getPushInt() * fc.getPopInt() * fc.getMult(SchedulingPhase.STEADY);
                //if we cannot fizz this filter, do nothing
                if (!Fissioner.canFizz(origLevels[l][s], false) || 
                        workEsts.get(fsn) / commRate <= FISS_COMP_COMM_THRESHOLD) 
                    continue;
                
                long fa = 
                    Math.round((((double)workEsts.get(fsn)) / ((double)slTotal)) * ((double)availTiles));
                
                
                //System.out.println(workEsts.get(fsn) + " / " + slTotal + " * " + availTiles + " = " + fa);
                
                fizzAmount.put(fsn.getParent(), (int)fa);
                tilesUsed += fa;
            }
            
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
                assert slice.getHead().getSourceSet(SchedulingPhase.STEADY).size() <= 1 &&
                    slice.getTail().getDestSet(SchedulingPhase.STEADY).size() <= 1;
                
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
     * This is no longer used, it ran on the SIR graph and partitioned the graph based
     * on the ASPLOS 06 TMD scheduling policy.
     *  
     * Use fission to create a time-multiplexed, data-parallel version of the 
     * application. Use the cousins algorithm specifying the number of tiles, so that
     * we duplicate each data parallel (stateless) filter a number of times taking 
     * the task parallel work into account.  See ASPLOS 06 paper for an explanation. 
     *  
     * @param str The stream graph
     * @param tiles The number of cores of the target machine
     */
    public void oldRun(SIRStream str, int tiles) {
        System.out.println("Running TMD Partitioner...");
        
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        WorkList workList = work.getSortedFilterWork();
        HashMap<SIRStream, Double> averageWork = 
            new HashMap<SIRStream, Double>(); 
        findAverageWork(str, work, averageWork);
        
        
        int totalWork = 0;
        for (int i = 0; i < workList.size(); i++) {
            SIRFilter filter = workList.getFilter(i);
            int filterWork = work.getWork(filter); 
            System.out.println("Sorted Work " + i + ": " + filter + " work " 
                    + filterWork + ", is fissable: " + StatelessDuplicate.isFissable(filter));
            totalWork += filterWork;
        }
        
        for (int i = workList.size() - 1; i >= 0; i--) {
            SIRFilter filter = workList.getFilter(i);
            System.out.println(filter + " work: " + work.getWork(filter));
            if (!StatelessDuplicate.isFissable(filter)) {
                System.out.println("  not fissible");
                continue;
            }
            int commRate = ((int[])work.getExecutionCounts().get(filter))[0] * 
                 (filter.getPushInt() + filter.getPopInt());
            int filterWork = work.getWork(filter);
            if (filterWork / commRate <= 10) {
                System.out.println("   Comp/Comp rate too low!");
                continue;
            }
          
            int cousins = getNumCousins(filter); 
            if (cousins == 1) {
                System.out.println("   No cousins: dup " + tiles);    
                StatelessDuplicate.doit(filter, tiles);
            }
            else {
                // esimate work fraction of this filter vs. cousins
                double workFraction = estimateWorkFraction(filter, averageWork);
                System.out.println("   Filter " + filter + " has " + cousins + " cousins and does " + workFraction + " of the work.");
                if (cousins < tiles) {
                    int reps = (int)Math.ceil(workFraction * ((double)tiles));
                    reps = Math.min(tiles - cousins + 1, reps);
                   
                    System.out.println("   Calling dup with: " + reps);
                    if (reps > 1)
                        StatelessDuplicate.doit(filter, reps);
           
                }
                
            }
        }
    }
    
    /**
     * Estimates the percentage of work performed by 'filter' in
     * relation to all of its cousins.  Works by ascending through
     * stream graph hierarchy.  Whenever a splitjoin is encountered,
     * the average work of filters on each branch is used to guide the
     * division of work between children.  Such fractions are
     * accumulated multiplicatively until reaching the top-level
     * splitjoin.
     */
    private double estimateWorkFraction(SIRFilter filter, HashMap<SIRStream, Double> averageWork) {
        double fraction = 1.0;
        SIRContainer parent = filter.getParent();
        SIRStream child = filter;
        while (parent != null) {
            if (parent instanceof SIRSplitJoin) {
                // work done by other streams
                double otherWork = 0.0;
                // work done by stream containing <filter>
                double myWork = 0.0;    
                System.out.println(parent);
                for (int i=0; i<parent.size(); i++) {
                    System.out.println(averageWork);
                    SIRStream temp = parent.get(i);
                    System.out.println(temp);
                    Double workD = averageWork.get(temp);//parent.get(i));
                    System.out.println(workD);
                    double work = workD.doubleValue();
                    if (parent.get(i)==child) {
                        myWork = work;
                    } else {
                        otherWork += work;
                    }
                }
                // accumulate the fraction of work that we do as part of this splitjoin
                fraction *= myWork / (myWork + otherWork);
            }
            child = parent;
            parent = parent.getParent();
        }
        return fraction;
    }
    
    /**
    * Returns number of parallel streams that are at same nesting
    * depth as <filter> relative to the top-level splitjoin.  Assumes
    * that the splitjoin widths on the path from <filter> to the
    * top-level splitjoin are symmetric across other siblings.
    */
   private int getNumCousins(SIRFilter filter) {
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
   
    /**
     * Mutates 'averageWork' into a mapping from filters to the
     * average amount of work for all filters deeply nested within
     * 'str'.
     */
    private void findAverageWork(SIRStream str, WorkEstimate work, 
            HashMap<SIRStream, Double> averageWork) {
        // the total amount of fissable work per container
        HashMap<SIRStream, Integer> sum = new HashMap<SIRStream, Integer>();
        // the number of fissable filters per container
        HashMap<SIRStream, Integer> count = new HashMap<SIRStream, Integer>();

        findWork(str, work, count, sum, averageWork);
    }

    /**
     * Counts the number of filters in each container and stores
     * result in 'count'.  Also accumulates 'sum' of work in
     * containers, as well as 'average' of work across all filters in
     * container.
     */
    private void findWork(SIRStream str, WorkEstimate work, 
                          HashMap<SIRStream, Integer> count,
                          HashMap<SIRStream, Integer> sum,
                          HashMap<SIRStream, Double> average) {
        if (str instanceof SIRFilter) {
            SIRFilter filter = (SIRFilter)str;
            count.put(filter, 1);
            sum.put(filter, work.getWork(filter));
            average.put(filter, (double)work.getWork(filter));
        } else {
            SIRContainer cont = (SIRContainer)str;
            int mysum = 0;
            int mycount = 0;
            // visit children to accumulate sum, count
            for (int i=0; i<cont.size(); i++) {
                SIRStream child = (SIRStream) cont.get(i);
                findWork(child, work, count, sum, average);
                mysum += sum.get(child);
                mycount += count.get(child);
            }
            // calculate average
            double myaverage= ((double)mysum) / ((double)mycount);
            // store results
            sum.put(cont, mysum);
            count.put(cont, mycount);
            average.put(cont, myaverage);
        }
    }
}

package at.dms.kjc.smp;

import java.util.HashMap;

import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.slicegraph.fission.FissionGroup;
import at.dms.kjc.spacetime.*;
import at.dms.kjc.*;
import java.util.LinkedList;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.iterator.IterFactory;
import at.dms.kjc.iterator.SIRFilterIter;

import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.List;

public class TMDBinPackFissAll extends Scheduler {

    public final static double FUSE_WORK_CHANGE_THRESHOLD = .90;

    private double DUP_THRESHOLD;
    public static final int FISS_COMP_COMM_THRESHOLD = 10;
    private HashMap<Slice, Integer> fizzAmount;

    public TMDBinPackFissAll() {
        super();
        DUP_THRESHOLD = ((double)KjcOptions.dupthresh) / 100.0;
        fizzAmount = new HashMap<Slice, Integer>();
    }
    
    /** Get the Core for a Slice 
     * @param node the {@link at.dms.kjc.slicegraph.SliceNode} to look up. 
     * @return the Core that should execute the {@link at.dms.kjc.slicegraph.SliceNode}. 
     */
    public Core getComputeNode(SliceNode node) {
        assert layoutMap.keySet().contains(node);
        return layoutMap.get(node);
    }
    
    /** Set the Core for a Slice 
     * @param node         the {@link at.dms.kjc.slicegraph.SliceNode} to associate with ...
     * @param core   The tile to assign the node
     */
    public void setComputeNode(SliceNode node, Core core) {
        assert node != null && core != null;
        //remember what filters each tile has mapped to it
        layoutMap.put(node, core);
        if (core.isComputeNode())
            core.getComputeCode().addFilter(node.getAsFilter());
    }
    
    /**
     * Assign the filternodes of the slice graph to tiles on the chip based on the levels
     * of the graph. 
     */
    public void runLayout() {
        assert graphSchedule != null : 
            "Must set the graph schedule (multiplicities) before running layout";
        
        LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());
        HashSet<Slice> fizzedSlices = new HashSet<Slice>();
        HashSet<Slice> unfizzedSlices = new HashSet<Slice>();
        
        // Get work estimates for all slices
        HashMap<FilterSliceNode, Long> workEsts = new HashMap<FilterSliceNode, Long>();
        for(Slice slice : slices) {
        	long workEst = SliceWorkEstimate.getWork(slice);
        	workEsts.put(slice.getFirstFilter(), workEst);
        }
        
        // Categorize slices into predefined, fizzed and unfizzed slices
        // Predefined filters are automatically added to off-chip memory
        for(Slice slice : slices) {
        	if(slice.getFirstFilter().isPredefined())
        		setComputeNode(slice.getFirstFilter(), SMPBackend.chip.getOffChipMemory());
        	else if(FissionGroupStore.isFizzed(slice))
        		fizzedSlices.add(slice);
        	else
        		unfizzedSlices.add(slice);
        }
        
        System.out.println("Number of fizzed slices: " + fizzedSlices.size());
        System.out.println("Number of unfizzed slices: " + unfizzedSlices.size());
        
        // Sort unfizzed slices by estimated work, most work first
        LinkedList<Slice> sortedUnfizzedSlices = new LinkedList<Slice>();
        for(Slice slice : unfizzedSlices) {
        	boolean inserted = false;
        	for(int x = 0 ; x < sortedUnfizzedSlices.size() ; x++) {
        		if(workEsts.get(slice.getFirstFilter()) >
        			workEsts.get(sortedUnfizzedSlices.get(x).getFirstFilter())) {
        			sortedUnfizzedSlices.add(x, slice);
        			inserted = true;
        			break;
        		}
        	}
        	
        	if(!inserted)
        		sortedUnfizzedSlices.add(slice);
        }
        
        // Attempt to load balance unfizzed slices across cores by using
        // LPT algorithm, which greedily assigns the slice with the most work
        // to the core with the least amount of estimate work
        long[] workAmounts = new long[SMPBackend.chip.size()];
        for(int x = 0 ; x < workAmounts.length ; x++)
        	workAmounts[x] = 0;
        
        for(Slice slice : sortedUnfizzedSlices) {
        	// Find core with minimum amount of work
        	long minWork = Long.MAX_VALUE;
        	int minCore = -1;
        	
        	for(int core = 0 ; core < workAmounts.length ; core++) {
        		if(workAmounts[core] < minWork) {
        			minWork = workAmounts[core];
        			minCore = core;
        		}
        	}
        	
        	// Add slice to core
        	setComputeNode(slice.getFirstFilter(), SMPBackend.chip.getNthComputeNode(minCore));
        	workAmounts[minCore] += workEsts.get(slice.getFirstFilter());
        }
        
        for(int x = 0 ; x < workAmounts.length ; x++)
        	System.out.println("Core " + x + " has work: " + workAmounts[x]);
        
        // Schedule fizzed slices by assigning fizzed copies sequentially
        // across cores
        HashSet<Slice> alreadyAssigned = new HashSet<Slice>();
        for(Slice slice : fizzedSlices) {
            // If slice already assigned, skip it
            if(alreadyAssigned.contains(slice))
                continue;
        	
        	// Get fizzed copies of slice
        	Slice[] fizzedCopies = FissionGroupStore.getFizzedSlices(slice);
        	
        	// Assign fizzed set sequentially across cores
        	for(int x = 0 ; x < fizzedCopies.length ; x++) {
        		setComputeNode(fizzedCopies[x].getFirstFilter(), SMPBackend.chip.getNthComputeNode(x));
        		System.out.println("Assigning " + fizzedCopies[x].getFirstFilter() + " to core " + SMPBackend.chip.getNthComputeNode(x).getCoreID());
        	}

            // Mark fizzed set as assigned
            for(Slice fizzedSlice : fizzedCopies)
                alreadyAssigned.add(fizzedSlice);

            // If using shared buffers, then fission does not replace the original
            // unfizzed slice with fizzed slices.  The current 'slice' is the original
            // unfizzed slice.  Set the compute node for 'slice' to the offChipMemory.
            // This is so that when we dump a dot-graph, we have a core to return when
            // we display the 'slice' in the graph.  Returning offChipMemory as the core
            // is sub-optimal, though there's not much else we can do right now
            if(KjcOptions.sharedbufs) {
                assert FissionGroupStore.isUnfizzedSlice(slice);
                setComputeNode(slice.getFirstFilter(), SMPBackend.chip.getOffChipMemory());
                alreadyAssigned.add(slice);
            }
        }
    }
    
    /**
     * Run the Time-Multiplexing Data-parallel scheduler.  Right now, it assumes 
     * a pipeline of stateless filters
     */
    public void run(int tiles) {
    	
        //if we are using the SIR data parallelism pass, then don't run TMD
        if (KjcOptions.dup == 1 || KjcOptions.optfile != null) {
        	System.out.println("***** Not using TMD scheduler since an SIR partitioner was used *****!");
        	//multiply steady multiplicity of each filter by KjcOptions.steadyMult
		    LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());

		    for (Slice slice : slices) {
		    	FilterContent filter = slice.getFirstFilter().getFilter();
		    	filter.multSteadyMult(KjcOptions.steadymult);
		    }

		    //must reset the filter info's because we have changed the schedule
	        FilterInfo.reset();
		    return;
        }
        
        //calculate how much to fizz each filter 
        calculateFizzAmounts(tiles);
        
        //calculate multiplicity factor necessary to fizz filters
        int factor = multiplicityFactor(tiles);
        System.out.println("Using fission steady multiplicity factor: " + factor);
                
        //go through and multiply the steady multiplicity of each filter by factor        
        LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());

        for (Slice slice : slices) {
            FilterContent filter = slice.getFirstFilter().getFilter();
            filter.multSteadyMult(factor * KjcOptions.steadymult);
         }
        
        //must reset the filter info's because we have changed the schedule
        FilterInfo.reset();
        
        SMPBackend.scheduler.graphSchedule.getSlicer().dumpGraph("before_fission.dot", 
                null, false);
        
        int maxFission = 0;
        int i = 0;
        //go through and perform the fission
        for (Slice slice : slices) {
            if (fizzAmount.containsKey(slice) && fizzAmount.get(slice) > 1) {
                FissionGroup fissionGroup = 
                    StatelessFissioner.doit(slice, graphSchedule.getSlicer(), fizzAmount.get(slice));

                if(fissionGroup != null) {
                    System.out.println("Fissing " + slice.getFirstFilter() + " by " + fizzAmount.get(slice));
                    if (fizzAmount.get(slice) > maxFission)
                        maxFission = fizzAmount.get(slice);

                    FissionGroupStore.addFissionGroup(fissionGroup);
                }

                SMPBackend.scheduler.graphSchedule.getSlicer().dumpGraph("fission_pass_" + i + ".dot", 
									 null, false);
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
     * Calculate the amount by which filters can be fizzed.  For now, stateless
     * filters that meet specific criteria are all fizzed by <totalTiles>
     */
    public void calculateFizzAmounts(int totalTiles) {
    	LinkedList<Slice> slices = DataFlowOrder.getTraversal(graphSchedule.getSlicer().getTopSlices());
    	
    	// Look for fizzable filters
    	for(Slice slice : slices) {
    		FilterSliceNode fsn = slice.getFirstFilter();
    		FilterContent fc = fsn.getFilter();
    		
    		long workEst = SliceWorkEstimate.getWork(slice);
    		int commRate = (fc.getPushInt() + fc.getPopInt() * fc.getMult(SchedulingPhase.STEADY));
    		
    		// Predefined filters can't be fizzed, automatically skip
    		if(fsn.isPredefined())
    			continue;
    	
    		// Check if fizzable.  If so, fizz by totalTiles
    		if(StatelessFissioner.canFizz(slice, false)) {
    			if(commRate > 0 && workEst / commRate <= FISS_COMP_COMM_THRESHOLD) {
    				System.out.println("Can fizz, but too much communication: " + fsn);
    			}
    			else {
    				System.out.println("Can fizz: " + fsn + ", fizzing by: " + totalTiles);
    				fizzAmount.put(slice, totalTiles);
    			}
    		}
    		else {
    			System.out.println("Cannot fiss: " + fsn);
    		}
    	}
    }
    
    /**
     * Calculates least common multiple between <a> and <b>
     */
    private int LCM(int a, int b) {
    	int product = a * b;
    	
    	do {
    		if(a < b) {
    			int tmp = a;
    			a = b;
    			b = tmp;
    		}
    		
    		a = a % b;
    	} while(a != 0);
    	
    	return product / b;
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
        
        //find minimum steady-state multiplicity factor necessary to meet
        //copyDown and duplication constraints of fission
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
                //assert slice.getHead().getSourceSet(SchedulingPhase.STEADY).size() <= 1;
                
                //check that we have reached the threshold for duplicated items
                int threshFactor = 
                    (int)Math.ceil((((double)(fi.peek - fi.pop)) * fizzAmount.get(slice)) / 
                        ((double)(DUP_THRESHOLD * (((double)fi.pop) * ((double)fi.steadyMult)))));

                //this factor makes sure that copydown is less than pop*mult*factor
                int cdFactor = 
                    (int)Math.ceil(((double)fi.copyDown) / ((double)(fi.pop * fi.steadyMult) 
                                                            / (double)(fizzAmount.get(slice))));

                int myFactor = Math.max(cdFactor, threshFactor);

                if (maxFactor < myFactor)
                    maxFactor = myFactor;
            }
        }
        
        //find LCM of all fizz amounts for filters
        int lcm = 1;
        for(Integer fa : fizzAmount.values())
        	lcm = LCM(lcm, fa.intValue());

        //scale up LCM so that it is at least as large as the minimum
        //steady-state multiplicity factor
        lcm = (int)Math.ceil((double)maxFactor / (double)lcm) * lcm;
        
        return lcm;
    }

    @Override
    public SIRStream SIRFusion(SIRStream str, int tiles) {
        if(false)
            return str;

        System.out.println("Performing fusion of stateful filters");

        SIRStream oldStr;
        //get the first work estimate
        WorkEstimate work = WorkEstimate.getWorkEstimate(str);
        //bin pack the shits
        StatefulGreedyBinPacking binPacker = new StatefulGreedyBinPacking(str, tiles, work);
        binPacker.pack();
        //get the max bin weight for the packing
        long oldWork = binPacker.maxBinWeight();
        //the work of the new partitioning
        long newWork = 0;
        //the percentage change
        double workChange;

        if(StatefulFusion.countStatefulFilters(str) < KjcOptions.smp) {
            return str;
        }
        
        do {
            oldStr = (SIRStream)ObjectDeepCloner.deepCopy(str);

            int numStatefulFilters = StatefulFusion.countStatefulFilters(str);
            int minTiles = at.dms.kjc.sir.lowering.partition.Partitioner.estimateFuseAllResult(str);

            if(numStatefulFilters - 1 <= minTiles + 1) {
                System.out.println("  Fusing all filters");
                str = FuseAll.fuse(str, false);
            }
            else {
                System.out.println("  Attempting to fuse down to " + (numStatefulFilters - 1) + 
                                   " stateful filters");
                str = new StatefulFusion(str, work, numStatefulFilters - 1, false).doFusion();
            }

            work = WorkEstimate.getWorkEstimate(str);

            //greedy bin pack the stateful filters
            binPacker = new StatefulGreedyBinPacking(str, tiles, work);
            binPacker.pack();
            newWork = binPacker.maxBinWeight();

            //find the percentage change in work between the two 
            workChange = ((double)oldWork) / ((double)newWork);

            //remember this as the old work for the next (possible) iteration
            System.out.println(oldWork + " / " + newWork + " = " + workChange);
            oldWork = newWork;

            //if number of stateful filters didn't change, then quit since can't fuse anymore
            int newNumStatefulFilters = StatefulFusion.countStatefulFilters(str);
            if(newNumStatefulFilters == numStatefulFilters || 
               newNumStatefulFilters <= KjcOptions.smp) {
                break;
            }
        } while (workChange >= FUSE_WORK_CHANGE_THRESHOLD);
        
        if(workChange < FUSE_WORK_CHANGE_THRESHOLD) {
            str = oldStr;
        }

        return str;
    }
}

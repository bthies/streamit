package at.dms.kjc.sir.lowering.partition.cache;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

class CConfigSplitJoin extends CConfigContainer {

    protected SIRSplitJoin split_join;
    private FusionInfo fusion_info;
    private int num_tiles;
    
    public CConfigSplitJoin(SIRSplitJoin sj, CachePartitioner partitioner) {
	super(sj, partitioner);
	split_join = sj;
	fusion_info = null;
	num_tiles = 0;
    }

    public static int gcd(int x, int y) {
	while (y != 0) {
	    int r = x % y;
	    x = y; y = r;
	}
	return x;
    }

    public int numberOfTiles() {
	if (num_tiles > 0) return num_tiles; // check if we have precomputed
 
	FusionInfo fi = getFusionInfo();

	if (fi.getCost().getCost() == fi.getWorkEstimateNoPenalty()) {

	    // no fusion penalty 
	    num_tiles = 1;
	} else {    

	    // positive fusion penalty
	    num_tiles = 0;
	    for (int i = 0; i < cont.size(); i++) {
		num_tiles += childConfig(i).numberOfTiles();
	    }
	}
	
	return num_tiles;
    }


    public FusionInfo getFusionInfo() {

	if (fusion_info != null) return fusion_info; // check if we have precomputed

	int work = 0;
	int work_no_penalty = 0;
	
	int code = 0;
	int data = 0;
	
	// how many times the split join will be executed 
	// after fusion in a steady state

	/*

	HashMap sched[] = SIRScheduler.getExecutionCounts(split_join);
	HashMap steady = sched[1];
	
	int splitter_mult = 0;
	int joiner_mult = 0;

	int channel_mult[] = new int[cont.size()];

	if (split_join.getSplitter().getSumOfWeights() > 0) {
	    int a[] = (int[])steady.get(split_join.getSplitter());
	    splitter_mult = a[0];

	    for (int i = 0; i < cont.size(); i++) {
		int pop = childConfig(i).getFusionInfo().getPopInt();
		int split_weight = split_join.getSplitter().getWeight(i);
		channel_mult[i] = splitter_mult * split_weight / pop;
	    }
	    
	    // add impossible unroll penalty
	    if (splitter_mult > KjcOptions.unroll) {
		work += 1000;
	    }
	}
		
	if (split_join.getJoiner().getSumOfWeights() > 0) {
	    int a[] = (int[])steady.get(split_join.getJoiner());
	    joiner_mult = a[0];

	    for (int i = 0; i < cont.size(); i++) {
		int push = childConfig(i).getFusionInfo().getPushInt();
		int joiner_weight = split_join.getJoiner().getWeight(i);
		int cmult = joiner_mult * joiner_weight / push;
		if (splitter_mult > 0) assert (cmult == channel_mult[i]);
		channel_mult[i] = cmult;
	    }
	    
	    // add impossible unroll penalty
	    if (joiner_mult > KjcOptions.unroll) {
		work += 1000;
	    }
	}
		
	int_obj

	// add impossible unroll penalty
	int_obj = (Integer)steady.get(split_join.getJoiner());
	if (int_obj != null && int_obj.intValue() > KjcOptions.unroll) {
	    work += 1000;
	}
	*/

	int mult = 1;
	int fmult[] = new int[cont.size()];

	int j_sum = split_join.getJoiner().getSumOfWeights();
	int s_sum = split_join.getSplitter().getSumOfWeights();

	int s_mult = 0;
	int j_mult = 0;

	//System.out.println("[Fusion info for SplitJoin]");

	//for (int i = 0; i < cont.size(); i++) {
	//    System.out.println("[split: "+split_join.getSplitter().getWeight(i)+" join: "+split_join.getJoiner().getWeight(i)+" pop: "+childConfig(i).getFusionInfo().getPopInt()+" push: "+childConfig(i).getFusionInfo().getPushInt()+"]");
	//}
	
	if (split_join.getSplitter().getSumOfWeights() == 0) {

	    //System.out.println("CASE1");

	    for (int i = 0; i < cont.size(); i++) {
		int push = childConfig(i).getFusionInfo().getPushInt();
		int join_weight = split_join.getJoiner().getWeight(i);
		int m = push / gcd(join_weight, push); // multiple for joiner
		mult = mult * m / gcd(mult, m);
	    }

	    for (int i = 0; i < cont.size(); i++) {
		int push = childConfig(i).getFusionInfo().getPushInt();
		int join_weight = split_join.getJoiner().getWeight(i);
		fmult[i] = mult * join_weight / push;
	    }
	    
	    j_mult = mult;

	} else {

	    //System.out.println("CASE2");

	    for (int i = 0; i < cont.size(); i++) {
		int pop = childConfig(i).getFusionInfo().getPopInt();
		int split_weight = split_join.getSplitter().getWeight(i);
		int m = pop / gcd(split_weight, pop); // multiple for splitter
		mult = mult * m / gcd(mult, m);
	    }

	    for (int i = 0; i < cont.size(); i++) {
		int pop = childConfig(i).getFusionInfo().getPopInt();
		int split_weight = split_join.getSplitter().getWeight(i);
		fmult[i] = mult * split_weight / pop;

		//System.out.print("[fmult["+i+"] = "+fmult[i]+"] ");
	    }

	    s_mult = mult;
	    
	    if (j_sum > 0) {

		int push = childConfig(0).getFusionInfo().getPushInt();
		int joiner_weight = split_join.getJoiner().getWeight(0);
		
		// items is number of items joiner has to consume
		// from branch 1
		int items = push * fmult[0];

		int lcd = items * joiner_weight / gcd(items, joiner_weight);
		
		j_mult = lcd / joiner_weight; 

		int xtra = lcd / items;
		s_mult *= xtra;

		for (int i = 0; i < cont.size(); i++) {
		    fmult[i] *= xtra;
		}
	    }
	}


	for (int i = 0; i < cont.size(); i++) {
	    //System.out.print("[fmult["+i+"] = "+fmult[i]+"] ");
	}

	if (s_mult > KjcOptions.unroll) work += 100;
	if (j_mult > KjcOptions.unroll) work += 100;

	// duplicate splitter fusion penalty
	//if (split_join.getSplitter().getType().isDuplicate()) {
	//    work += 100;
	//}

	//System.out.println("\n[s-mult: "+s_mult+" j-mult: "+j_mult+"]");

	code += s_sum * s_mult * 20;
	code += j_sum * j_mult * 20;

	FusionInfo child0 = childConfig(0).getFusionInfo();

	data += s_sum * s_mult * child0.getInputSize() * 2;
	data += j_sum * j_mult * child0.getOutputSize() * 2;

	for (int i = 0; i < cont.size(); i++) {
	    CConfig child = childConfig(i);
	    FusionInfo fi = child.getFusionInfo();
	    
	    work += fi.getWorkEstimate();
	    work_no_penalty += fi.getWorkEstimateNoPenalty();

	    code += fi.getCodeSize() * fmult[i];
	    data += fi.getDataSize() * fmult[i];
	    //data += fi.getInputSize() * fi.getPopInt() * fmult[i] + 
	    //fi.getOutputSize() * fi.getPushInt() * fmult[i];
	    
	    // add impossible unroll penalty
	    if (KjcOptions.unroll < fmult[i]) {
		work += fi.getWorkEstimate()/2;
	    }

	    // add fusion peek overhead
	    /*
	    if (child instanceof CConfigFilter) {
		CConfigFilter fc = (CConfigFilter)child;
		SIRFilter filter = (SIRFilter)fc.getStream();
		if (filter.getPeekInt() > filter.getPopInt()) {
		    work += fi.getWorkEstimate() / 2;
		}
	    }

	    if (child instanceof CConfigPipeline) {
		CConfigPipeline pipe = (CConfigPipeline)child;
		if (pipe.childConfig(1) instanceof CConfigFilter) {
		    CConfigFilter cfilter = (CConfigFilter)pipe.childConfig(1);
		    SIRFilter filter = (SIRFilter)cfilter.getStream();
		    if (filter.getPeekInt() > filter.getPopInt()) {
			work += cfilter.getFusionInfo().getWorkEstimate() / 2;
		    }
		}
	    }
	    */
	}

	int pop = split_join.getSplitter().getSumOfWeights()*s_mult;

	if (split_join.getSplitter().getType().isDuplicate()) {
	    pop = s_mult;
	}

	int push = split_join.getJoiner().getSumOfWeights()*j_mult;

	//System.out.println("Fused SplitJoin Pop: "+pop+" Push: "+push); 

	fusion_info = 
	    new FusionInfo(work, work_no_penalty, code, data,
			   pop,
			   pop,
			   push,
			   child0.getInputSize(),
			   child0.getOutputSize());	

	return fusion_info;
    }
    
    protected SIRStream doCut(LinkedList partitions, PartitionRecord curPartition,
			      int x1, int x2, int xPivot, int tileLimit, int tPivot, SIRStream str) {
	// there's a division at this <xPivot>.  We'll
	// return result of a vertical cut
	int[] arr = { 1 + (xPivot-x1), x2-xPivot };
	PartitionGroup pg = PartitionGroup.createFromArray(arr);
	// do the vertical cut
	SIRSplitJoin sj = RefactorSplitJoin.addHierarchicalChildren((SIRSplitJoin)str, pg);
	
	// recurse left and right.
	SIRStream left = traceback(partitions, curPartition, x1, xPivot, tPivot, sj.get(0));
	// mark that we have a partition here
	curPartition = new PartitionRecord();
	partitions.add(curPartition);
	SIRStream right = traceback(partitions, curPartition, xPivot+1, x2, tileLimit-tPivot, sj.get(1));
	
	// mutate ourselves if we haven't been mutated already
	sj.set(0, left);
	sj.set(1, right);
	
	return sj;
    }

    /**
     * Mark that horizontal fusion in splitjoin costs something.
     */
    private static final CCost fusionOverhead = new CCost(1);
    protected CCost fusionOverhead() {
	return fusionOverhead;
    }
}


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


class CConfigPipeline extends CConfigContainer {
    
    FusionInfo fusion_info[][];
    int num_tiles;

    // if greedyCuts[i] == true then cut was made after element i
    boolean greedyCuts[];

    public int findGreedyCut(int from, int to) {
        for (int i = from; i < to; i++) {
            if (greedyCuts[i]) return i;
        } 

        return -1;
    }

    public CConfigPipeline(SIRContainer cont, CachePartitioner partitioner) {
        super(cont, partitioner);

        fusion_info = new FusionInfo[cont.size()][cont.size()];
        for (int i = 0; i < cont.size(); i++)
            for (int j = 0; j < cont.size(); j++)
                fusion_info[i][j] = null;
    
        num_tiles = 0;

        greedyCuts = new boolean[cont.size() - 1];
        for (int i = 0; i < cont.size()-1; i++) {
            greedyCuts[i] = false;
        }

    }

    public boolean getPeek() {
        return childConfig(0).getPeek();
    }

    public int numberOfTiles() {
        if (num_tiles > 0) return num_tiles; // check if we have precomputed

        num_tiles = numberOfTiles(0, cont.size()-1);
    
        //System.out.println("pipeline num tiles: "+num_tiles);

        //System.out.println("Pipeline (0,"+(cont.size()-1)+") Greedy Cuts:");

        //for (int i = 0; i < cont.size()-1; i++) {
        //    if (greedyCuts[i]) System.out.println("cut after: "+i);
        //}

        int p_start = 0;

        for (int i = 0; i < cont.size()-2; i++) {
            if (greedyCuts[i]) {
                num_tiles += refactor(p_start, i);
                p_start = i + 1;
            }
        }

        num_tiles += refactor(p_start, cont.size()-1);

        num_tiles += recombine_partitions();

        //System.out.println("pipeline FINAL num tiles: "+num_tiles);
        return num_tiles;
    }


    // returns true if item at index peeks

    private boolean does_peek(int index) {

        CConfig child = childConfig(index);
    
        if (child instanceof CConfigFilter) {
            CConfigFilter fc = (CConfigFilter)child;
            SIRFilter filter = (SIRFilter)fc.getStream();

            int f_pop = filter.getPopInt();
            int f_peek = filter.getPeekInt();

            if (f_peek > f_pop) return true;
        }


        if (child instanceof CConfigSplitJoin) {
            CConfigSplitJoin sj = (CConfigSplitJoin)child;
            if (sj.getPeek()) {

                return true;
            }
        }
    
        return false;
    }

    
    private int recombine_partitions() {

        int result = 0;

        int last_cut = -1;
        int curr_cut = 0;

        for (curr_cut = 0; curr_cut < cont.size() - 2; curr_cut++) {
            if (greedyCuts[curr_cut]) {
                break;
            }
        }   

        for (int i = curr_cut + 1; i < cont.size() - 2; i++) {

            if (greedyCuts[i]) {

                // last_cut + 1 ... curr_cut
                // curr_cut + 1 ... i

                FusionInfo f_all = getFusionInfo(last_cut + 1, i);

                if (f_all.getCost().getCost() <= f_all.getWorkEstimateNoPenalty() &&
                    !does_peek(curr_cut+1)) {
            
                    greedyCuts[curr_cut] = false;
                    //System.out.println("recombining adjacent partitions!");
                    //System.out.println("recomb: ["+(last_cut+1)+
                    //                   ","+curr_cut+","+i+"]");

                    curr_cut = i;
                    result--;

                } else {

                    last_cut = curr_cut;
                    curr_cut = i;
                }
            }
        }

        FusionInfo f_all = getFusionInfo(last_cut + 1, cont.size() - 1);
    
        if (f_all.getCost().getCost() <= f_all.getWorkEstimateNoPenalty() &&
            !does_peek(curr_cut+1)) {
            greedyCuts[curr_cut] = false;

            //System.out.println("recombining adjacent partitions!");
            //System.out.println("recomb: ["+(last_cut+1)+
            //                   ","+curr_cut+","+(cont.size()-1)+"]");

            result--;
        }

        return result;
    }

    private int refactor(int from, int to) {

        //System.out.println("refactor from:"+from+" to:"+to);

        int new_cuts = 0;

        // find multiplicity of filters

        int mult[] = new int[cont.size()];
        mult[from] = 1; // init mult. of first item to 1

        for (int i = from; i < to; i++) {
            int push = childConfig(i).getFusionInfo().getPushInt() * mult[i];
            int pop = childConfig(i+1).getFusionInfo().getPopInt();

            int common = gcd(push,pop);
            int xtra = pop / common;

            if (xtra > 1) {
                for (int a = from; a <= i; a++) {
                    mult[a] = mult[a] * xtra; 
                }
            }

            mult[i+1] = (push * xtra) / pop;
        }

        for (int i = from+1; i <= to; i++) {
            CConfig child = childConfig(i);

            //If peek ratio is 1024 -- try to refactor partition

            if (KjcOptions.peekratio >= 1024 && child instanceof CConfigFilter) {
                CConfigFilter fc = (CConfigFilter)child;
                SIRFilter filter = (SIRFilter)fc.getStream();
        
                int f_pop = filter.getPopInt();
                int f_peek = filter.getPeekInt();
                int f_extra = f_peek - f_pop;

                if (f_extra > 0) { // peek > pop
            
                    //double amortize_break = f_extra / ClusterBackend.getExecCounts(filter);
                    //double amortize_fuse = f_extra / mult[i];
        
                    //System.out.print("[refactor extra:"+f_extra+"("+(f_pop * mult[i] * 4)+") pop:"+f_pop+" m:"+mult[i]+" ]");
            

                    // break if peek rate < 25% of (peek-pop)
                    //if (f_extra > f_pop * mult[i] * 4) {
            
                    //System.out.print("Cutting partition!!");
                    greedyCuts[i-1] = true;
                    new_cuts++;
                    //} 

                    /*
                      else if (amortize_break * 2 < amortize_fuse) {

                      // break if amortize_break * 2 < amortize_fuse

                      //System.out.print("Cutting partition!!");
                      greedyCuts[i-1] = true;
                      new_cuts++;
                      }
                    */
            
                    //System.out.println();
            
                }
            }

            if (i > from && child instanceof CConfigSplitJoin) {
                CConfigSplitJoin sj = (CConfigSplitJoin)child;
                if (sj.getPeek()) {

                    //System.out.print("Cutting partition!!");
                    greedyCuts[i-1] = true;
                    new_cuts++;

                    //work += fi.getWorkEstimate()/2;
                    //work += 1500;
                }
            }
        }

        return new_cuts;
    }

    private int numberOfTiles(int from, int to) {

        //System.out.println("Pipeline.numberOfTiles("+from+","+to+")");

        if (from > to) return 0;
        if (from == to) return childConfig(from).numberOfTiles();

        for (int i = from; i <= to; i++) {
            int child_tiles = childConfig(i).numberOfTiles();
            if (child_tiles > 1) {
                int left = numberOfTiles(from, i - 1);
                int right = numberOfTiles(i + 1, to);

                if (i > 0) greedyCuts[i-1] = true;
                if (i < cont.size()-1) greedyCuts[i] = true;

                return left + child_tiles + right;
            }
        }

        // all children fit into one tile each

        // first try fusing everything

        FusionInfo f_all = getFusionInfo(from, to);

        //System.out.println("try one tile ["+f_all.getCost().getCost()+","+f_all.getWorkEstimateNoPenalty()+"]");

        if (f_all.getCost().getCost() <= f_all.getWorkEstimateNoPenalty()) {

            //System.out.println("pipeline segnemt fits into one tile!");
            return 1;
        }

        // find multiplicity of filters

        int mult[] = new int[cont.size()];
        mult[from] = 1; // init mult. of first item to 1

        for (int i = from; i < to; i++) {
            int push = childConfig(i).getFusionInfo().getPushInt() * mult[i];
            int pop = childConfig(i+1).getFusionInfo().getPopInt();

            int common = gcd(push,pop);
            int xtra = pop / common;

            if (xtra > 1) {
                for (int a = from; a <= i; a++) {
                    mult[a] = mult[a] * xtra; 
                }
            }

            mult[i+1] = (push * xtra) / pop;
        }

    
        // find max bandwidth connection

        int max_size = 0;
        int max_loc = 0;

        for (int i = from; i < to; i++) {
    
            int size = childConfig(i).getFusionInfo().getPushInt() * mult[i];
            if (size > max_size) {
                max_size = size;
                max_loc = i;
            }
        }

        //System.out.println("max_size: "+max_size+" max_loc: "+max_loc);

        int f_start = max_loc;
        int f_end = max_loc + 1;

        FusionInfo _fi = getFusionInfo(f_start, f_end);
    
        if (_fi.getCost().getCost() > _fi.getWorkEstimateNoPenalty()) {
    
            // can not fuse f_start and f_end
            // make cut between f_start and f_end

            int left = numberOfTiles(from, f_start);
            int right = numberOfTiles(f_end, to);
        
            greedyCuts[f_start] = true;

            return left + right;
        }


        // fuse up and down starting with (max_loc, max_loc + 1) while cant fuse    

        boolean can_fuse_up = true, can_fuse_down = true;

        for (;;) {
        
            // check if still can fuse up
            if (can_fuse_up) {
                if (f_start == from) {
                    can_fuse_up = false;
                } else {
                    FusionInfo fi = getFusionInfo(f_start - 1, f_end);
                    if (fi.getCost().getCost() > fi.getWorkEstimateNoPenalty()) {
                        can_fuse_up = false;
                    }
                }
            }
        
            // check if still can fuse down
            if (can_fuse_down) {
                if (f_end == to) {
                    can_fuse_down = false;
                } else {
                    FusionInfo fi = getFusionInfo(f_start, f_end + 1);
                    if (fi.getCost().getCost() > fi.getWorkEstimateNoPenalty()) {
                        can_fuse_down = false;
                    }
                }
            }
        
            if (can_fuse_up && can_fuse_down) { f_start -= 1; }
            if (can_fuse_up && !can_fuse_down) { f_start -= 1; }
            if (!can_fuse_up && can_fuse_down) { f_end += 1; }

            if (!can_fuse_up && !can_fuse_down) { 

                //System.out.println("done fusing ["+f_start+","+f_end+"]");

                int left = numberOfTiles(from, f_start - 1);
                int right = numberOfTiles(f_end + 1, to);

                if (f_start > 0) greedyCuts[f_start - 1] = true;
                if (f_end < cont.size()-1) greedyCuts[f_end] = true;

                return left + right + 1;
            }
        }
    }

    public FusionInfo getFusionInfo() {
        return getFusionInfo(0, cont.size() - 1);
    }
    
    public static int gcd(int x, int y) {
        while (y != 0) {
            int r = x % y;
            x = y; y = r;
        }
        return x;
    }

    public FusionInfo getFusionInfo(int from, int to) {

        // check if we have precomputed
        if (fusion_info[from][to] != null) return fusion_info[from][to];

        int work = 0;
        int work_no_penalty = 0;
        int code = 0;
        int data = 0;

        int mult[] = new int[cont.size()];
        mult[from] = 1; // init mult. of first item to 1

        for (int i = from; i < to; i++) {
            int push = childConfig(i).getFusionInfo().getPushInt() * mult[i];
            int pop = childConfig(i+1).getFusionInfo().getPopInt();

            int common = gcd(push,pop);
            int xtra = pop / common;

            if (xtra > 1) {
                for (int a = from; a <= i; a++) {
                    mult[a] = mult[a] * xtra; 
                }
            }

            mult[i+1] = (push * xtra) / pop;
        }

        for (int i = from; i <= to; i++) {
            CConfig child = childConfig(i);

            FusionInfo fi = child.getFusionInfo();
            work += fi.getWorkEstimate();
            work_no_penalty += fi.getWorkEstimateNoPenalty();

            code += fi.getCodeSize() * mult[i];
            data += fi.getDataSize();// * mult[i]; // removed Nov-15-2004

            // removed Nov-15-2004
            //if (i < to) data += fi.getOutputSize() * fi.getPushInt() * mult[i] * 2;

            // add impossible unroll penalty
            if (KjcOptions.unroll < mult[i]) work += fi.getWorkEstimate()/2;

            //If peek ratio is 1024 add fusion peek overhead!
            if (KjcOptions.peekratio >= 1024) { 
                if (i > from && child instanceof CConfigFilter) {
                    CConfigFilter fc = (CConfigFilter)child;
                    SIRFilter filter = (SIRFilter)fc.getStream();

                    int f_pop = filter.getPopInt();
                    int f_peek = filter.getPeekInt();
                    int f_extra = f_peek - f_pop;

                    if (f_extra > 0) { // peek > pop

                        // only penalize if peek rate < 25% of (peek-pop)

                        //System.out.print("[ extra:"+f_extra+"("+(f_pop * mult[i] * 4)+") pop:"+f_pop+" m:"+mult[i]+" ]");

                        if (f_extra > f_pop * mult[i] * 4) {
                            //work += fi.getWorkEstimate()/2;
                            //work += 1500;
                            //System.out.print("penalty!!");
                        }

                        //System.out.println();

                    }
                }

                if (i > from && child instanceof CConfigSplitJoin) {
                    CConfigSplitJoin sj = (CConfigSplitJoin)child;
                    if (sj.getPeek()) {
                        //work += fi.getWorkEstimate()/2;
                        //work += 1500;
                    }
                }
            }
        }

        int to_push = childConfig(to).getFusionInfo().getPushInt();
        int from_pop = childConfig(from).getFusionInfo().getPopInt();
        int from_peek = childConfig(from).getFusionInfo().getPeekInt();
    
        fusion_info[from][to] = 
            new FusionInfo(work, work_no_penalty, code, data, 
                           from_pop * mult[from],
                           from_pop * mult[from] + (from_peek - from_pop),
                           to_push * mult[to],
                           childConfig(from).getFusionInfo().getInputSize(),
                           childConfig(to).getFusionInfo().getOutputSize());

        return fusion_info[from][to];
    }

    protected SIRStream doCut(LinkedList<PartitionRecord> partitions, PartitionRecord curPartition,
                              int x1, int x2, int xPivot, int tileLimit, int tPivot, SIRStream str) {
        // there's a division at this <yPivot>.  We'll
        // return result of a horizontal cut.
        int[] arr = { 1 + (xPivot-x1), x2-xPivot };
        PartitionGroup pg = PartitionGroup.createFromArray(arr);
    
        SIRContainer result;
        // might have either pipeline or feedback loop at this point...
        if (str instanceof SIRPipeline) {
            result = RefactorPipeline.addHierarchicalChildren((SIRPipeline)str, pg);
        } else if (str instanceof SIRFeedbackLoop) {
            // if we have a feedbackloop, then factored is
            // just the original, since it will have only
            // two children
            result = (SIRContainer)str;
        } else {
            result = null;
            Utils.fail("Unrecognized stream type: " + str);
        }
    
        // recurse up and down
        SIRStream top = traceback(partitions, curPartition, x1, xPivot, tPivot, result.get(0));
        // mark that we have a partition here
        curPartition = new PartitionRecord();
        partitions.add(curPartition);
        SIRStream bot = traceback(partitions, curPartition, xPivot+1, x2, tileLimit-tPivot, result.get(1));
    
        // mutate ourselves if we haven't been mutated yet
        result.set(0, top);
        result.set(1, bot);
    
        // all done
        return result;
    }

    /**
     * For now, fusion overhead of a pipeline is 0.
     */
    private static final CCost fusionOverhead = new CCost(0);
    protected CCost fusionOverhead() {
        return fusionOverhead;
    }
    
}

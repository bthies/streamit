package at.dms.kjc.smp;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.kjc.flatgraph.WorkSorted;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.fission.*;
import java.util.*;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * @author mgordon
 *
 */
public class StatefulGreedyBinPacking {
    private int numBins;
    private SIRStream str;
    private WorkEstimate workEstimates;
    private LinkedList<SIRFilter>[] bins;
    private long[] binWeight;
    private long maxBinWeight;
    
    public StatefulGreedyBinPacking(SIRStream str, int numBins, WorkEstimate workEstimates) {
        this.str = str;
        this.numBins = numBins;
        this.workEstimates = workEstimates;
        bins = new LinkedList[numBins];
        binWeight = new long[numBins];
        for (int i = 0; i < numBins; i++) {
            bins[i] = new LinkedList<SIRFilter>();
            binWeight[i] = 0;
        }
    }

    public void pack() {
        GraphFlattener gf = new GraphFlattener(str);
        FlatNode topLevel = gf.top;
                
        //now sort the filters by work
        Iterator<FlatNode> sorted = 
            WorkSorted.getSortedList(topLevel, workEstimates).iterator();
        int filters = 0;

        //perform the packing
        while (sorted.hasNext()) {
            FlatNode node = sorted.next();
            assert node.isFilter();

            if(!StatelessDuplicate.hasMutableState((SIRFilter)node.contents))
                continue;

            filters++;
            
            int bin = findMinBin();
            bins[bin].add((SIRFilter)node.contents);
            binWeight[bin] += workEstimates.getWork((SIRFilter)node.contents);
        }
        System.out.println("Packed " + filters + " stateful filters.");

        //find max bin
        maxBinWeight = -1;
        for (int i = 0; i < numBins; i++)
            if (binWeight[i] > maxBinWeight) {
                maxBinWeight = binWeight[i];
            }
        
        /*
        for (int i = 0; i < numBins; i++) {
            System.out.println("Bin " + i + " (weight = " + binWeight[i] + "):");
            Iterator<SIRFilter> binIt = bins[i].iterator();
            while (binIt.hasNext()) {
                SIRFilter temp = binIt.next();
                System.out.println("  " + temp + " (" + workEstimates.getReps(temp) + 
                                   ", " + workEstimates.getWork(temp) + ")");
                //System.out.println("  " + binIt.next());
            }
        }
        */
    }
    
    public HashSet<Integer> getCriticalPathTiles(double threshold) {
        HashSet<Integer> cps = new HashSet<Integer>();
        assert threshold > 0.0 && threshold < 1.0;
     
        double workThreshold = maxBinWeight * threshold;
        
        for (int i = 0; i < numBins; i++) {
            if (binWeight[i] >= workThreshold) {
                cps.add(new Integer(i));
            }
        }

        return cps;
    }
    
    public HashSet<SIRFilter> getCriticalpath(double threshold) {
        HashSet<SIRFilter> cps = new HashSet<SIRFilter>();
        assert threshold > 0.0 && threshold < 1.0;
     
        double workThreshold = maxBinWeight * threshold;
        
        for (int i = 0; i < numBins; i++) {
            if (binWeight[i] >= workThreshold) {
                Iterator<SIRFilter> filters = bins[i].iterator();
                while (filters.hasNext()) {
                    cps.add(filters.next());
                }
            }
        }

        return cps;
    }
    
    private int findMinBin() {
        long minWeight = Long.MAX_VALUE;
        int minBin = -1;
        for (int i = 0; i < numBins; i++) 
            if (binWeight[i] < minWeight) {
                minBin = i;
                minWeight = binWeight[i];
            }
        return minBin;
    }
    
    public long maxBinWeight() {
        long maxBinWeight = 0;
        for (int i = 0; i < numBins; i++)
            if (binWeight[i] > maxBinWeight)
                maxBinWeight = binWeight[i];
        return maxBinWeight;
    }
}

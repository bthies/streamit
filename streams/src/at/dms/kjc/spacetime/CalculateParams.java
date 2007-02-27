/**
 * 
 */
package at.dms.kjc.spacetime;
import at.dms.kjc.*;
import at.dms.kjc.flatgraph.*;
import java.util.*;
import at.dms.kjc.sir.*;

/**
 * @author mgordon
 *
 */
public class CalculateParams {

    private SIRStream str;
    private double avgMaxSliceSize;
    private int traces;
    private int filters;
    private double CCRatio;
    private HashMap<SIRFilter, int[]> exeCounts;
    
    public CalculateParams(SIRStream str, double CCRatio, HashMap<SIRFilter, int[]> exeCounts) {
        this.str = str;
        this.CCRatio = CCRatio;
        this.exeCounts = exeCounts;
    }
    
    public void doit() {
        calcAvgMaxSliceSize();
        System.out.println("Average Max Slice Size based on SIR graph: " + 
                avgMaxSliceSize);
        
        if (KjcOptions.slicethresh == 101 && KjcOptions.steadymult == 1) {
            setSteadyMultTime();
        }
        
        /*
        if (KjcOptions.autoparams) {
            calcSteadyMult();
            calcSliceThresh();
        }
        
        System.out.println("Using SteadyMult: " + KjcOptions.steadymult);
        System.out.println("Using SliceThresh: " + KjcOptions.slicethresh);
        */
    }
    
    /**
     * Set the steady-state multiplier when we have time only and 
     * the user didn't specify a steady mult (or specified 1).  
     * Bascially, this will only affect filters with a low CC ratio
     * that don't communicate much data.
     *
     */
    private void setSteadyMultTime() {
        //if we are in thread mode, then set mult to 1
        if (KjcOptions.noswpipe) {
            KjcOptions.steadymult = 1;
            return;
        }
        //set the steady mult for apps with a large comp to comm to 1
        if (CCRatio > 30.0) {
            KjcOptions.steadymult = 1;
            return;
        }
        //if we get here, we have an app that has a low comp to comm
        //now for apps with little of both, force the mode of the rates
        //to be divisible by 8
        int modeOfRates = findRateMode();
        if (modeOfRates < 8) {
            KjcOptions.steadymult = SpaceTimeBackend.getRawChip().cacheLineWords;//8 / modeOfRates; 
            System.out.println("** Setting steadymult to " + KjcOptions.steadymult);
        }
    }
    
    /**
     * Find the most commonly occuring rate (pop or push * steady mult) in the
     * str graph. 
     * 
     * @return The rate mode.
     */
    private int findRateMode() {
        GraphFlattener gf = new GraphFlattener(str);
        FlatNode topLevel = gf.top;
        Iterator<FlatNode> flatNodes = DataFlowTraversal.getTraversal(topLevel).iterator();

        HashMap<Integer, Integer> ratesToOccurance = new HashMap<Integer, Integer>();
        
        while(flatNodes.hasNext()) {
            FlatNode node = flatNodes.next();
            if (node.isFilter() && !(node.contents instanceof SIRFileReader) &&
                    !(node.contents instanceof SIRFileWriter)) {
                int pushRate = node.getFilter().getPushInt() * 
                  exeCounts.get(node.getFilter())[0];
                int popRate = node.getFilter().getPopInt() *
                  exeCounts.get(node.getFilter())[0];
                
                if (!ratesToOccurance.containsKey(pushRate))
                    ratesToOccurance.put(pushRate, 0);
                if (!ratesToOccurance.containsKey(popRate))
                    ratesToOccurance.put(popRate, 0);
                
                ratesToOccurance.put(pushRate, ratesToOccurance.get(pushRate) + 1);
                ratesToOccurance.put(popRate, ratesToOccurance.get(popRate) + 1);
            }
        }
        
        //now find the mode
        int maxOcc = 0;
        int rate = -1;
        Iterator<Integer> rates = ratesToOccurance.keySet().iterator();
        while (rates.hasNext()) {
            Integer i = rates.next();
            
            if (ratesToOccurance.get(i) > maxOcc) {
                maxOcc = ratesToOccurance.get(i);
                rate = i;
            }
        }
        return rate;
    }
    
    private void calcSliceThresh() {
        if (traces == 1) {
            KjcOptions.slicethresh = 0;
        }
        else if (KjcOptions.st_cyc_per_wd == 1) {
            KjcOptions.slicethresh = 66;
        }
        else {
            KjcOptions.slicethresh = 33;
        }
    }
    
    private void calcSteadyMult() {
        if (traces == 1) {
            if (avgMaxSliceSize <= 16)
                KjcOptions.steadymult = 16;
            else 
                KjcOptions.steadymult = 512;
        }
        else {
            KjcOptions.steadymult = 16;
        }
    }
    
    private void calcAvgMaxSliceSize() {
        GraphFlattener gf = new GraphFlattener(str);
        FlatNode topLevel = gf.top;
        
        Iterator<FlatNode> flatNodes = DataFlowTraversal.getTraversal(topLevel).iterator();
        
        if ((topLevel.contents instanceof SIRFileReader) && 
                topLevel.getEdges()[0].isFilter()) 
            traces = 1;
                
        while(flatNodes.hasNext()) {
            FlatNode node = flatNodes.next();
            if (node.isSplitter()) {
                for (int i = 0; i < node.ways; i++) {
                    if (!node.getEdges()[i].isSplitter())
                        traces++;
                }
            }
            else if (node.isJoiner() && node.ways > 0 && 
                    !(node.getEdges()[0].isJoiner()) && 
                    !(node.getEdges()[0].isSplitter())) {
                traces++;
            }
            else if (node.isFilter() && !(node.contents instanceof SIRFileReader) &&
                    !(node.contents instanceof SIRFileWriter)) {
                //System.out.println("Counting " + node);
                filters++;
            }
        }
        
        if (traces == 0)
            traces = 1;
             
        avgMaxSliceSize = ((double)filters) / ((double)traces);
    }
}

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
    
    public CalculateParams(SIRStream str, double CCRatio) {
        this.str = str;
        this.CCRatio = CCRatio;
    }
    
    public void doit() {
        calcAvgMaxSliceSize();
        System.out.println("Average Max Slice Size based on SIR graph: " + 
                avgMaxSliceSize);
        /*
        if (KjcOptions.autoparams) {
            calcSteadyMult();
            calcSliceThresh();
        }
        
        System.out.println("Using SteadyMult: " + KjcOptions.steadymult);
        System.out.println("Using SliceThresh: " + KjcOptions.slicethresh);
        */
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
                topLevel.edges[0].isFilter()) 
            traces = 1;
                
        while(flatNodes.hasNext()) {
            FlatNode node = flatNodes.next();
            if (node.isSplitter()) {
                for (int i = 0; i < node.ways; i++) {
                    if (!node.edges[i].isSplitter())
                        traces++;
                }
            }
            else if (node.isJoiner() && node.ways > 0 && 
                    !(node.edges[0].isJoiner()) && 
                    !(node.edges[0].isSplitter())) {
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

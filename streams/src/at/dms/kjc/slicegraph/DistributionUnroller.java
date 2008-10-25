package at.dms.kjc.slicegraph;

import at.dms.kjc.backendSupport.*;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * This class will unroll the splitting and joining distribution of the slice (or slices)
 * so that all of the weight's array entries are 1 and the number of entries in the
 * distribution is equal to the number of items pushed (for splitter) or pop'ed (for
 * a joiner).
 * 
 * @author mgordon
 *
 */
public class DistributionUnroller {
   
    public static void test(Slice[] slices) {
        for (Slice slice : slices) {
            InputSliceNode input = slice.getHead();
            OutputSliceNode output = slice.getTail();
            
            System.out.println(slice);
            String inBefore = "nothing";
            inBefore = input.debugString(false, SchedulingPhase.STEADY);
            String outBefore = "nothing";
            outBefore = output.debugString(false, SchedulingPhase.STEADY);
            unroll(slice);
            //System.out.println(input.debugString(false));
            //System.out.println(output.debugString(false));
            roll(slice);
           
            System.out.println(inBefore);
            System.out.println(input.debugString(false, SchedulingPhase.STEADY));
            System.out.println(outBefore);
            System.out.println(output.debugString(false, SchedulingPhase.STEADY));
            System.out.println("=============");
            
        }
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of all the slices in <slices>.
     */
    public static void roll(Slice[] slices) {
        for (Slice slice : slices) {
            roll(slice);
        }
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of <slice>.   
     */
    public static void roll(Slice slice) {
        InputSliceNode input = slice.getHead();
        OutputSliceNode output = slice.getTail();
        
        input.canonicalize(SchedulingPhase.STEADY);
        output.canonicalize(SchedulingPhase.STEADY);
        
        if (input.hasInitPattern())
            input.canonicalize(SchedulingPhase.INIT);
        
        if (output.hasInitPattern()) 
            output.canonicalize(SchedulingPhase.INIT);
        
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of all the slices in <slices>.
     */
    public static void unroll(Slice[] slices) {
        for (Slice slice : slices) {
            unroll(slice);
        }
    }
    
    /**
     * Unroll the distribution pattern for the input and output slice nodes
     * of <slice>.   
     */
    public static void unroll(Slice slice) {
        InputSliceNode input = slice.getHead();
        OutputSliceNode output = slice.getTail();
        unroll(input);
        unroll(output);
    }
    
    public static void unroll(InputSliceNode input) {
        FilterInfo fi = FilterInfo.getFilterInfo(input.getNextFilter());
       
        //unroll the init joining schedule
        InterSliceEdge[] initSrcs = 
            new InterSliceEdge[fi.totalItemsReceived(SchedulingPhase.INIT)];
        unrollHelperInput(input.getSources(SchedulingPhase.INIT), 
                input.getWeights(SchedulingPhase.INIT), 
                initSrcs);
        input.setInitSources(initSrcs);
        input.setInitWeights(makeOnesArray(initSrcs.length));

        
        //unroll the steady joining schedule
        if (input.getSources(SchedulingPhase.STEADY).length > 0) {
            InterSliceEdge[] srcs = 
                new InterSliceEdge[fi.totalItemsReceived(SchedulingPhase.STEADY)];
            unrollHelperInput(input.getSources(SchedulingPhase.STEADY), 
                    input.getWeights(SchedulingPhase.STEADY), 
                    srcs);
            input.setSources(srcs);
            input.setWeights(makeOnesArray(srcs.length));
        }
    }
    
    public static void unroll(OutputSliceNode output) {
        //unroll the steady splitting schedule
        FilterInfo fi = FilterInfo.getFilterInfo(output.getPrevFilter());
        

        InterSliceEdge[][] initDests = 
            new InterSliceEdge[fi.totalItemsSent(SchedulingPhase.INIT)][];
        unrollHelperOutput(output.getDests(SchedulingPhase.INIT),
                output.getWeights(SchedulingPhase.INIT),
                initDests);
        output.setInitDests(initDests);
        output.setInitWeights(makeOnesArray(initDests.length));


       
        if (output.getDests(SchedulingPhase.STEADY).length > 0) {
            InterSliceEdge[][] dests = 
                new InterSliceEdge[fi.totalItemsSent(SchedulingPhase.STEADY)][];
            unrollHelperOutput(output.getDests(SchedulingPhase.STEADY),
                    output.getWeights(SchedulingPhase.STEADY),
                    dests);
            output.setDests(dests);
            output.setWeights(makeOnesArray(dests.length));
        }
    }
    
    private static int[] makeOnesArray(int size) {
        int[] ret = new int[size];
        Arrays.fill(ret, 1);
        return ret;
    }
    
    /**
     * Fill in <unrolled> with the unrolled <src> and <weights>.
     */
    private static void unrollHelperInput(InterSliceEdge[] src, int weights[], 
            InterSliceEdge[] unrolled) {
        assert src.length == weights.length;
        
        int weightsSum = 0;
        for (int weight : weights) 
            weightsSum += weight;
        
        if (weightsSum == 0)
            return;
        
        assert unrolled.length % weightsSum == 0;
        int index = 0;
        
        for (int rep = 0; rep < unrolled.length / weightsSum; rep++) {
            for (int w = 0; w < weights.length; w++) {
                InterSliceEdge obj = src[w];
                for (int i = 0; i < weights[w]; i++) {
                    unrolled[index++] = obj; 
                }                 
            }
        }
        
        assert index == unrolled.length;
    }
    
    /**
     * Fill in <unrolled> with the unrolled <src> and <weights>.
     */
    private static void unrollHelperOutput(InterSliceEdge[][] src, int weights[], 
            InterSliceEdge[][] unrolled) {
        assert src.length == weights.length;
        
        int weightsSum = 0;
        for (int weight : weights) 
            weightsSum += weight;

        if (weightsSum == 0)
            return;

        assert unrolled.length % weightsSum == 0;
                
        int index = 0;
        
        for (int rep = 0; rep < unrolled.length / weightsSum; rep++) {
            for (int w = 0; w < weights.length; w++) {
                InterSliceEdge[] obj = src[w].clone();
                for (int i = 0; i < weights[w]; i++) {
                    unrolled[index++] = obj; 
                }                 
            }
        }
        
        assert index == unrolled.length;
    }
}

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
            if (input.hasInitPattern())
                    inBefore = input.debugString(false, SchedulingPhase.INIT);
            String outBefore = "nothing";
            if (output.hasInitPattern())
                outBefore = output.debugString(false, SchedulingPhase.INIT);
            unroll(slice);
            //System.out.println(input.debugString(false));
            //System.out.println(output.debugString(false));
            roll(slice);
            /*
            System.out.println(inBefore);
            if (input.hasInitPattern())
                System.out.println(input.debugString(false, SchedulingPhase.INIT));
            System.out.println(outBefore);
            if (output.hasInitPattern())
                System.out.println(output.debugString(false, SchedulingPhase.INIT));
            System.out.println("=============");
            */
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
        FilterSliceNode filter = slice.getFirstFilter();
        FilterInfo fi = FilterInfo.getFilterInfo(filter);

        //unroll the steady joining schedule
        if (input.getSources(SchedulingPhase.STEADY).length > 0) {
            InterSliceEdge[] srcs = 
                new InterSliceEdge[fi.totalItemsReceived(SchedulingPhase.STEADY)];
            unrollHelper(input.getSources(SchedulingPhase.STEADY), 
                    input.getWeights(SchedulingPhase.STEADY), 
                    srcs);
            input.setSources(srcs);
            input.setWeights(makeOnesArray(srcs.length));
        }

        //unroll the steady splitting schedule
        
        if (output.getDests(SchedulingPhase.STEADY).length > 0) {
            InterSliceEdge[][] dests = 
                new InterSliceEdge[fi.totalItemsSent(SchedulingPhase.STEADY)][];
            unrollHelper(output.getDests(SchedulingPhase.STEADY),
                    output.getWeights(SchedulingPhase.STEADY),
                    dests);
            output.setDests(dests);
            output.setWeights(makeOnesArray(dests.length));
        }
        
        if (input.hasInitPattern()) {
            //unroll the init joining schedule
            InterSliceEdge[] initSrcs = 
                new InterSliceEdge[fi.totalItemsReceived(SchedulingPhase.INIT)];
            unrollHelper(input.getSources(SchedulingPhase.INIT), 
                    input.getWeights(SchedulingPhase.INIT), 
                    initSrcs);
            input.setInitSources(initSrcs);
            input.setInitWeights(makeOnesArray(initSrcs.length));
        }
      
        if (output.hasInitPattern()) {
            //unroll the steady splitting schedule
            if (fi.totalItemsSent(SchedulingPhase.INIT) == 0) {
                output.setInitDests(null);
                output.setInitWeights(null);
            } else {

                InterSliceEdge[][] initDests = 
                    new InterSliceEdge[fi.totalItemsSent(SchedulingPhase.INIT)][];
                unrollHelper(output.getDests(SchedulingPhase.INIT),
                        output.getWeights(SchedulingPhase.INIT),
                        initDests);
                output.setInitDests(initDests);
                output.setInitWeights(makeOnesArray(initDests.length));
            }
        }
      
    }
    
    private static int[] makeOnesArray(int size) {
        int[] ret = new int[size];
        Arrays.fill(ret, 1);
        return ret;
    }
    /**
     * Fill in <unrolled> with the unrolled <target> and <weights>.
     */
    private static <T> void unrollHelper(T[] target, int weights[], T[] unrolled) {
        assert target.length == weights.length;
        
        int weightsSum = 0;
        for (int weight : weights) 
            weightsSum += weight;
                
        assert unrolled.length % weightsSum == 0;
        int index = 0;
        
        for (int rep = 0; rep < unrolled.length / weightsSum; rep++) {
            for (int w = 0; w < weights.length; w++) {
                T obj = target[w];
                for (int i = 0; i < weights[w]; i++) {
                    unrolled[index++] = obj; 
                }                 
            }
        }
        
        assert index == unrolled.length;
    }
}

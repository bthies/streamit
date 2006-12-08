package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SliceNode;
import at.dms.util.Utils;
import java.util.HashMap;

public class TraceBufferSchedule 
{
    private int currentWeight;
    private int currentBuffer;
    private InputSliceNode input;
    private OutputSliceNode output;
    private static HashMap<SliceNode, TraceBufferSchedule> nodes;
    
    static 
    {
        nodes = new HashMap<SliceNode, TraceBufferSchedule>();
    }
    
    private TraceBufferSchedule(InputSliceNode in) 
    {
        input = in;
        output = null;
    
        currentWeight = input.getWeights()[0];
        currentBuffer = 0;
    }
    
    private TraceBufferSchedule(OutputSliceNode out) 
    {
        output = out;
        input = null;

        currentWeight = output.getWeights()[0];
        currentBuffer = 0;
    }

    public static Edge getOutputBuffer(InputSliceNode in) 
    {
        if (!nodes.containsKey(in))
            nodes.put(in, new TraceBufferSchedule(in));

        return nodes.get(in).updateInput();
    }
    
    public static Edge[] getInputBuffers(OutputSliceNode out)
    {
        if (!nodes.containsKey(out))
            nodes.put(out, new TraceBufferSchedule(out));
        return nodes.get(out).updateOutput();
    }

    private Edge updateInput() {
        if (input == null)
            Utils.fail("Calling get outputbuffer illegally");

        if (currentWeight <= 0) {
            currentBuffer = (currentBuffer + 1) % (input.getSources().length);
            //reset the round-robin weight
            currentWeight = input.getWeights()[currentBuffer];
        }
        //decrement the weight on this arc
        currentWeight --;
        //return the appropriate output buffer to receive from
        return input.getSources()[currentBuffer];
    }

    private Edge[] updateOutput() 
    {
        if (output == null)
            Utils.fail("Calling getInputBuffer illegally");
    
        if (currentWeight <= 0) {
            currentBuffer = (currentBuffer + 1) % (output.getDests().length);
            //reset the round-robin weight
            currentWeight = output.getWeights()[currentBuffer];
        }
        currentWeight--;
        return output.getDests()[currentBuffer];
    }
    
    
}
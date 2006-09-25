package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.HashMap;

public class TraceBufferSchedule 
{
    private int currentWeight;
    private int currentBuffer;
    private InputTraceNode input;
    private OutputTraceNode output;
    private static HashMap<TraceNode, TraceBufferSchedule> nodes;
    
    static 
    {
        nodes = new HashMap<TraceNode, TraceBufferSchedule>();
    }
    
    private TraceBufferSchedule(InputTraceNode in) 
    {
        input = in;
        output = null;
    
        currentWeight = input.getWeights()[0];
        currentBuffer = 0;
    }
    
    private TraceBufferSchedule(OutputTraceNode out) 
    {
        output = out;
        input = null;

        currentWeight = output.getWeights()[0];
        currentBuffer = 0;
    }

    public static Edge getOutputBuffer(InputTraceNode in) 
    {
        if (!nodes.containsKey(in))
            nodes.put(in, new TraceBufferSchedule(in));

        return nodes.get(in).updateInput();
    }
    
    public static Edge[] getInputBuffers(OutputTraceNode out)
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

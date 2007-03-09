package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.ComputeNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import java.util.*;
import at.dms.kjc.slicegraph.Buffer;
import at.dms.kjc.spacetime.ComputeCodeStore;
import at.dms.kjc.spacetime.ComputeNodes;
/**
 * Stub for uniprocessor backend.
 * @author dimock
 *
 */
public class UniBackEndFactory extends BackEndFactory<
    ComputeNodes,
    ComputeNode<?>,
    ComputeCodeStore<?>,
    Object> { 

    @Override
    public  BackEndScaffold<
        ComputeNodes,ComputeNode<?>,ComputeCodeStore<?>,Object> getBackEndMain() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public ComputeNodes getComputeNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public  ComputeCodeStore<?> getComputeCodeStore(ComputeNode<?> parent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public  ComputeNode<?> getComputeNode(ComputeNodes allNodes,
            Object specifier) {
        // TODO Auto-generated method stub
        return null;
    }
    /**
     * Process an input slice node: find the correct ProcElement(s) and add joiner code, and buffers.
     * please delegate work to some other object.
     * @param input           the InputSliceNode 
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param rawChip         the available compute nodes.
     * 
     */
    @Override
    public void processInputSliceNode(InputSliceNode input,
            SchedulingPhase whichPhase, ComputeNodes computeNodes) {
        
    }
    
    /**
     * Process all filter slice nodes in a Slice (just one in a SimpleSlice): find the correct ProcElement(s) and add filter code.
     * please delegate work to some other object.
     * @param slice           Slice containing filters
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */
    @Override
    public void processFilterSlices(Slice slice, 
            SchedulingPhase whichPhase, ComputeNodes computeNodes) {
        
    }

    /**
     * Process a filter slice node: find the correct ProcElement(s) and add code and buffers.
     * please delegate work to some other object.
     * @param filter          the FilterSliceNode.
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */

    public void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, ComputeNodes computeNodes) {
        
    }

    /**
     * Process an output slice node: find the correct ProcElement(s) and add splitter code, and buffers.
     * please delegate work to some other object.
     * @param output          the OutputSliceNode.
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */
    @Override
    public void processOutputSliceNode(OutputSliceNode output,
            SchedulingPhase whichPhase, ComputeNodes computeNodes) {
        
    }
    
    @Override
    public Collection<Buffer> getChannels() {
        return null;
    }

}

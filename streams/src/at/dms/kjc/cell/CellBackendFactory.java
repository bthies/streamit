package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.ComputeNodesI;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

public class CellBackendFactory extends BackEndFactory {

    @Override
    public BackEndScaffold getBackEndMain() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Channel getChannel(Edge e) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Channel getChannel(SliceNode src, SliceNode dst) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CodeStoreHelper getCodeStoreHelper(SliceNode node) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComputeCodeStore getComputeCodeStore(ComputeNode parent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComputeNode getComputeNode(Object specifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ComputeNodesI getComputeNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processFilterSlices(Slice slice, SchedulingPhase whichPhase,
            ComputeNodesI computeNodes) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processInputSliceNode(InputSliceNode input,
            SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processOutputSliceNode(OutputSliceNode output,
            SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
        // TODO Auto-generated method stub

    }

}

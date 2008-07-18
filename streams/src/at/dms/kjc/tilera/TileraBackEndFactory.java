/**
 * 
 */
package at.dms.kjc.tilera;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

/**
 * @author mgordon
 *
 */
public class TileraBackEndFactory extends BackEndFactory<TileraChip, Tile, TileCodeStore, Integer> {

    private TileraChip chip;
    private TileraBackEndScaffold scaffold;
    
    public TileraBackEndFactory(TileraChip tChip) {
        chip = tChip;
        scaffold = new TileraBackEndScaffold();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getBackEndMain()
     */
    @Override
    public TileraBackEndScaffold getBackEndMain() {
        return scaffold;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getChannel(at.dms.kjc.slicegraph.Edge)
     */
    @Override
    public Channel getChannel(Edge e) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getChannel(at.dms.kjc.slicegraph.SliceNode, at.dms.kjc.slicegraph.SliceNode)
     */
    @Override
    public Channel getChannel(SliceNode src, SliceNode dst) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getCodeStoreHelper(at.dms.kjc.slicegraph.SliceNode)
     */
    @Override
    public CodeStoreHelper getCodeStoreHelper(SliceNode node) {
        // TODO Auto-generated method stub
        if (node instanceof FilterSliceNode) {
            // simply do appropriate wrapping of calls...
            return new FilterCodeGeneration((FilterSliceNode)node,this);
        } else {
            assert false;
            return null;
        }
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getComputeCodeStore(at.dms.kjc.backendSupport.ComputeNode)
     */
    @Override
    public TileCodeStore getComputeCodeStore(Tile parent) {
        return parent.getComputeCode();
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getComputeNode(java.lang.Object)
     */
    @Override
    public Tile getComputeNode(Integer tileNum) {
        return chip.getNthComputeNode(tileNum.intValue());
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#getComputeNodes()
     */
    @Override
    public TileraChip getComputeNodes() {
        return chip;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processFilterSliceNode(at.dms.kjc.slicegraph.FilterSliceNode, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, TileraChip chip) {
        System.out.println("Processing: " + filter + " on tile " + layout.getComputeNode(filter).getTileNumber() + "(" + whichPhase + ")");
        (new ProcessFilterSliceNode(filter, whichPhase, this)).processFilterSliceNode();
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processFilterSlices(at.dms.kjc.slicegraph.Slice, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processFilterSlices(Slice slice, SchedulingPhase whichPhase,
            TileraChip chip) {
        assert false : "The Tilera backend does not support slices with multiple filters (processFilterSlices()).";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processInputSliceNode(at.dms.kjc.slicegraph.InputSliceNode, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processInputSliceNode(InputSliceNode input,
            SchedulingPhase whichPhase, TileraChip chip) {
        // TODO Auto-generated method stub
        System.out.println("Processing: " + input);
        // Convert the channel accesses to reads from and writes to the input and output buffers
       
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.backendSupport.BackEndFactory#processOutputSliceNode(at.dms.kjc.slicegraph.OutputSliceNode, at.dms.kjc.backendSupport.SchedulingPhase, at.dms.kjc.backendSupport.ComputeNodesI)
     */
    @Override
    public void processOutputSliceNode(OutputSliceNode output,
            SchedulingPhase whichPhase, TileraChip chip) {
        // TODO Auto-generated method stub
        System.out.println("Processing: " + output);
    }
}

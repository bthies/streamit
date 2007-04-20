package at.dms.kjc.spacetime;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SliceNode;

/**
 * Factor out parts of RAW (at.dms.kjc.spacetime) back end that need matching types.
 *
 * @author dimock
 * @see BackEndFactory
 * @see BackEndAbsFactory
 *
 */
public class RawBackEndFactory extends BackEndFactory<RawChip, RawTile, RawComputeCodeStore, int[]> 
{

    public RawBackEndFactory() {
        this(null,null);
    }
    
    public RawBackEndFactory(RawChip rawChip, Layout<RawTile> layout) {
        if (rawChip == null) {
            rawChip = new RawChip(KjcOptions.raw,
                    KjcOptions.rawcol > 0 ? KjcOptions.rawcol : KjcOptions.raw);
            }
        this.rawChip = rawChip;
        setLayout(layout);
    }
    
    // Singleton...
    private BackEndScaffold scaffolding = null;
    /**
     * The converter from a Schedule to ComputeNode's with completed ComputeCodeStore's
     * and Buffer's.
     *  
     * @see Rawify 
     * */
    @Override
    public BackEndScaffold getBackEndMain() {
        if (scaffolding == null) {
            scaffolding  = new BackEndScaffold();
        }
        return scaffolding;
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
            SchedulingPhase whichPhase, RawChip rawChip) {
        Rawify.processInputSliceNode(input,whichPhase,rawChip);
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
            SchedulingPhase whichPhase, RawChip rawChip) {
        Rawify.processFilterSlices(slice, whichPhase, rawChip);
    }

    /**
     * Process a filter slice node: find the correct ProcElement(s) and add code and buffers.
     * please delegate work to some other object.
     * @param filter          the FilterSliceNode.
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */
    @Override
    public void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, RawChip rawChip) {
        throw new AssertionError("Expect this method to be unused");
    }
  
    
    /**
     * Process an output slice node: find the correct ProcElement(s) and add splitter code and buffers.
     * please delegate work to some other object.
     * @param output          the OutputSliceNode.
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */
    @Override
    public void processOutputSliceNode(OutputSliceNode output,
            SchedulingPhase whichPhase, RawChip rawChip) {
        Rawify.processOutputSliceNode(output, whichPhase, rawChip);
    }


    
    // Singleton...
    private RawChip rawChip = null;
    /** @see RawChip */
    @Override
    public RawChip getComputeNodes() {
        return rawChip;
    }

    /** @see RawTile */
    @Override
    public RawTile getComputeNode(int[] xy) {
        assert xy.length == 2;
        return rawChip.getTile(xy[0], xy[1]);
    }

    /** @see RawComputeCodeStore */
    @Override
    public RawComputeCodeStore getComputeCodeStore(RawTile parent) {
        return parent.getComputeCode();
    }

    // place to keep Layout.
    private Layout<RawTile> layout;
    /**
     * Keep a copy of the {@link Layout}: the mapping from {@link at.dms.kjc.slicegraph.SliceNode SliceNode} to 
     * {@link ComputeNode}.
     * @param layout
     */
    public void setLayout(Layout<RawTile> layout) {
        this.layout = layout;
    }
    /**
     * Get saved copy of {@link Layout}.
     * @return
     */
    public Layout<RawTile> getLayout() {
        return layout;
    }

    @Override
    public Channel getChannel(Edge e) {
        if (e instanceof InterSliceEdge) {
            return InterSliceBuffer.getBuffer((InterSliceEdge)e);
        } else {
            // insist on types
            if (e.getSrc() instanceof FilterSliceNode) {
                return IntraSliceBuffer.getBuffer((FilterSliceNode)(e.getSrc()), (OutputSliceNode)(e.getDest()));
            } else {
                return IntraSliceBuffer.getBuffer((InputSliceNode)(e.getSrc()), (FilterSliceNode)(e.getDest()));
            }
        }
    }

    @Override
    public Channel getChannel(SliceNode src, SliceNode dst) {
        if (src instanceof OutputSliceNode && dst instanceof InputSliceNode) {
            return InterSliceBuffer.getBuffer(new InterSliceEdge((OutputSliceNode)src, (InputSliceNode)dst));
        } else {
            if (src instanceof FilterSliceNode) {
                return IntraSliceBuffer.getBuffer((FilterSliceNode)(src), (OutputSliceNode)(dst));
            } else {
                return IntraSliceBuffer.getBuffer((InputSliceNode)(src), (FilterSliceNode)(dst));
            }
        }
    }

    /**
     * Eventually make RawExecutionCode a subclass of CodeStoreHelper, but unused for now.
     */
    @Override
    public CodeStoreHelper getCodeStoreHelper(SliceNode node) {
        return null;
    }
}

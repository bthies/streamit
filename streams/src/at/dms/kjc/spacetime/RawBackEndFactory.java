package at.dms.kjc.spacetime;

import java.util.HashMap;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.BackEndAbsFactory;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Buffer;
import java.util.*;

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

    // Singleton...
    private BackEndScaffold<RawChip, RawTile, RawComputeCodeStore, int[]> scaffolding = null;
    /**
     * The converter from a Schedule to ComputeNode's with completed ComputeCodeStore's
     * and Buffer's.
     *  
     * @see Rawify 
     * */
    @Override
    public BackEndScaffold<RawChip, RawTile, RawComputeCodeStore, int[]> getBackEndMain() {
        if (scaffolding == null) {
            scaffolding  = new BackEndScaffold<RawChip, RawTile, RawComputeCodeStore, int[]>();
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
        if (rawChip == null) {
        rawChip = new RawChip(KjcOptions.raw,
                KjcOptions.rawcol > 0 ? KjcOptions.rawcol : KjcOptions.raw);
        }
        return rawChip;
    }


    // Singleton tile per position (on the singleton chip).
    private HashMap<int[], RawTile> tileMap = new HashMap<int[], RawTile>();
    /** @see RawTile */
    @Override
    public RawTile getComputeNode(RawChip rawChip, int[] xy) {
        assert xy.length == 2;
        RawTile tile = tileMap.get(xy);
        if (tile == null) {
            tile = new RawTile(xy[0], xy[1], rawChip);
            tileMap.put(xy, tile);
        }
        return tile;
    }

    // Single code store per tile.
    private HashMap<RawTile, RawComputeCodeStore> storeMap = new HashMap<RawTile, RawComputeCodeStore>();
    /** @see RawComputeCodeStore */
    @Override
    public RawComputeCodeStore getComputeCodeStore(RawTile parent) {
        RawComputeCodeStore store = storeMap.get(parent);
        if (store == null) {
            store = new RawComputeCodeStore(parent);
            storeMap.put(parent, store);
        }
        return store;
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
    
    public HashSet<Buffer> channels;

    public Collection<Buffer> getChannels() {
        return channels;
    }
}

package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.CodeStoreHelperJoiner;
import at.dms.kjc.backendSupport.CodeStoreHelperSimple;
import at.dms.kjc.backendSupport.CodeStoreHelperSplitter;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

public class CellBackendFactory 
    extends BackEndFactory<CellChip, CellPU, CellComputeCodeStore, Integer> {

    private CellChip cellChip;
    private GetOrMakeCellChannel channelMaker = new GetOrMakeCellChannel(this);
    
    /**
     * Constructor if creating UniBackEndFactory before layout
     * Creates <b>numProcessors</b> processors.
     * @param numProcessors  number of processors to create.
     */
    public CellBackendFactory(Integer numProcessors) {
       this(new CellChip(numProcessors));
    }
    
    /**
     * Constructor if creating UniBackEndFactory after layout.
     * Call it with same collection of processors used in Layout.
     * @param processors  the existing collection of processors.
     */
    public CellBackendFactory(CellChip processors) {
        if (processors == null) {
            processors = new CellChip(1);
        }
        this.cellChip = processors;
    }
    
    private BackEndScaffold scaffolding = null;
    
    @Override
    public BackEndScaffold getBackEndMain() {
        if (scaffolding == null) {
            scaffolding = new BackEndScaffold();
        }
        return scaffolding;
    }

    @Override
    public Channel getChannel(Edge e) {
        return channelMaker.getOrMakeChannel(e);
    }

    @Override
    public Channel getChannel(SliceNode src, SliceNode dst) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CodeStoreHelper getCodeStoreHelper(SliceNode node) {
        if (node instanceof FilterSliceNode) {
            // simply do appropriate wrapping of calls...
            return new CodeStoreHelperSimple((FilterSliceNode)node,this);
        } else if (node instanceof InputSliceNode) {
            // CodeStoreHelper that does not expect a work function.
            // Can we combine with above?
            return new CodeStoreHelperJoiner((InputSliceNode)node, this);
        } else {
            return new CodeStoreHelperSplitter((OutputSliceNode)node,this);
        }
    }

    @Override
    public CellComputeCodeStore getComputeCodeStore(CellPU parent) {
        return parent.getComputeCode();
    }

    @Override
    public CellPU getComputeNode(Integer specifier) {
        return cellChip.getNthComputeNode(specifier);
    }

    @Override
    public CellChip getComputeNodes() {
        return cellChip;
    }

    @Override
    public void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, CellChip computeNodes) {
        System.out.println("processfilterslidenode");
        //ProcessCellFilterSliceNode.processFilterSliceNode(filter, whichPhase, this);
    }

    @Override
    public void processFilterSlices(Slice slice, SchedulingPhase whichPhase,
            CellChip computeNodes) {
        // TODO Auto-generated method stub

    }

    @Override
    public void processInputSliceNode(InputSliceNode input,
            SchedulingPhase whichPhase, CellChip computeNodes) {
        // TODO Auto-generated method stub
        System.out.println("processinputslidenode");

    }

    @Override
    public void processOutputSliceNode(OutputSliceNode output,
            SchedulingPhase whichPhase, CellChip computeNodes) {
        // TODO Auto-generated method stub
        System.out.println("processoutputslidenode");
    }

}

package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.CodeStoreHelper;
import at.dms.kjc.backendSupport.CodeStoreHelperJoiner;
import at.dms.kjc.backendSupport.CodeStoreHelperSimple;
import at.dms.kjc.backendSupport.CodeStoreHelperSplitter;
import at.dms.kjc.backendSupport.GetOrMakeChannel;
import at.dms.kjc.backendSupport.ProcessFilterSliceNode;
import at.dms.kjc.backendSupport.ProcessInputSliceNode;
import at.dms.kjc.backendSupport.ProcessOutputSliceNode;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SliceNode;

/**
 * Specialization of {@link at.dms.kjc.backendSupport.BackEndFactory} for uniprocessor backend.
 * Provides some specializations for the uniprocssor backend directly, and links to others in separate classes.
 * @author dimock
 *
 */
public class UniBackEndFactory extends BackEndFactory<
    UniProcessors,
    UniProcessor,
    UniComputeCodeStore,
    Integer> { 

    private UniProcessors processors ;

    /**
     * Constructor if creating UniBackEndFactory before layout
     * Creates <b>numProcessors</b> processors.
     * @param numProcessors  number of processors to create.
     */
    public UniBackEndFactory(Integer numProcessors) {
       this(new UniProcessors(numProcessors));
    }
    
    /**
     * Constructor if creating UniBackEndFactory after layout.
     * Call it with same collection of processors used in Layout.
     * @param processors  the existing collection of processors.
     */
    public UniBackEndFactory(UniProcessors processors) {
        if (processors == null) {
            processors = new UniProcessors(1);
        }
        this.processors = processors;
    }
    
    private BackEndScaffold scaffolding = null;

    @Override
    public  BackEndScaffold getBackEndMain() {
        if (scaffolding == null) {
            scaffolding  = new BackEndScaffold();
        }
        return scaffolding;
    }
    
    
    @Override
    public UniProcessors getComputeNodes() {
        return processors;
    }

    
    @Override
    public UniComputeCodeStore getComputeCodeStore(UniProcessor parent) {
        return parent.getComputeCode();
    }

    @Override
    public UniProcessor getComputeNode(Integer specifier) {
        return processors.getNthComputeNode(specifier);
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
            SchedulingPhase whichPhase, UniProcessors computeNodes) {
        new ProcessInputSliceNode(input,whichPhase,this).processInputSliceNode();

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
            SchedulingPhase whichPhase, UniProcessors computeNodes) {
        throw new AssertionError("processFilterSlices called, back end should be calling processFilterSlice(singular)");
        
    }

    /**
     * Process a filter slice node: find the correct ProcElement(s) and add code and buffers.
     * please delegate work to some other object.
     * @param filter          the FilterSliceNode.
     * @param whichPhase      INIT / PRIMEPUMP / STEADY
     * @param computeNodes    the available compute nodes.
     */

    public void processFilterSliceNode(FilterSliceNode filter,
            SchedulingPhase whichPhase, UniProcessors computeNodes) {
        new ProcessFilterSliceNode(filter,whichPhase,this).processFilterSliceNode();
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
            SchedulingPhase whichPhase, UniProcessors computeNodes) {
        new ProcessOutputSliceNode(output,whichPhase,this).processOutputSliceNode();

    }

    private GetOrMakeChannel channelTypeSelector = new GetOrMakeChannel(this);
    
    @Override
    public Channel getChannel(Edge e) {
        return channelTypeSelector.getOrMakeChannel(e);
    }

    @Override
    public Channel getChannel(SliceNode src, SliceNode dst) {
        throw new AssertionError("Getting channel by src, dst not supported.");
    }
    

    /** name of variable used as bound for number of iterations. */
    public static final String iterationBound = "_iteration_bound";

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

}

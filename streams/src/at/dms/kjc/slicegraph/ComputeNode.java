package at.dms.kjc.slicegraph;

import java.util.LinkedList;
import at.dms.kjc.spacetime.ComputeCodeStore;

/**
 * This abstract class represents a device that can perform computation.
 * For such a devide, it makes sense to associate {@link SliceNode SliceNode}s
 * with the device, and to create ComputeCode for the SliceNodes.
 * @author gordon / dimock
 * @param <StoreType> A recursive type: ComputeCodeStore's and ComputeNode's refer to each other and may be extended...
 *
 */
public class ComputeNode<StoreType extends ComputeCodeStore<?>> extends ProcElement {

    protected StoreType computeCode;
    
    private LinkedList<SliceNode> initFilters;
    private LinkedList<SliceNode> primepumpFilters;
    private LinkedList<SliceNode> steadyFilters;

    /**
     * Constructor.
     * Because of mutually recursive types of ComputeNode and ComputeCodeStore
     * you should call as:
     * <pre>
       MyComputeNodeType computenode = new MyComputeNodeType<MyCodeStoreType>();
       computenode.setComputeNode(new MyCodeStoreType<MyComputeNodeTyp>();
     * </pre>
     *
     */
    public ComputeNode() {
        initFilters = new LinkedList<SliceNode>();
        primepumpFilters = new LinkedList<SliceNode>();
        steadyFilters = new LinkedList<SliceNode>();
        computeCode = (StoreType)new ComputeCodeStore<ComputeNode<StoreType>>(this);
    }
    
    /**
     * Access the ComputeCodeStore for this compute node.
     * (So as to add or inspect code).
     * @return
     */
    public StoreType getComputeCode() {
        return computeCode;
    }

    /**
     * Set theComputeCodeStore for this compute node.
     * @param a ComputeCodeStore subclass (actual type as per paramaterization of ComputeNode)
     */
    public void setComputeCode(StoreType computeCode) {
        this.computeCode = computeCode;
    }

    /**
     * Add a FilterSliceNode to the the filters associated with this node. 
     * @param init        true if init stage, false if primepump or steady stage
     * @param primepump   true if primepump stage, false if init or steady stage
     * @param node      FilterSliceNode to add
     */
    public void addSliceNode(boolean init, boolean primepump, SliceNode node) {
        if (init)
            initFilters.add(node);
        else if (primepump)
            primepumpFilters.add(node);
        else
            steadyFilters.add(node);
    }

    /**
     * Get the filters associated with this node.
     * @param init       true if init stage, false if primepump or steady stage
     * @param primepump  true if primepump stage, false if init or steady stage
     * @return the FilterSliceNodes added by addFilterSlice for the passed values of init and primepump
     */
    public Iterable<SliceNode> getSliceNodes(boolean init, boolean primepump) {
        if (init)
            return initFilters;
        else if (primepump)
            return primepumpFilters;
        else
            return steadyFilters;
    }

    
}

package at.dms.kjc.backendSupport;

import at.dms.kjc.slicegraph.ProcElement;
import at.dms.kjc.slicegraph.SliceNode;

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
    
    private int uniqueId = 0;
    private boolean uniqueIdIsSet = false;
    
    /**
     * Constructor.
     * Because of mutually recursive types of ComputeNode and ComputeCodeStore
     * you should call as:
     * <pre>
       MyComputeNodeType computenode = new MyComputeNodeType<MyCodeStoreType>();
       computenode.setComputeNode(new MyCodeStoreType<MyComputeNodeTyp>(computenode);
     * </pre>
     */
    public ComputeNode() {
        computeCode = (StoreType)null;
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
     * set unique integer representing this compute node.
     */
    
    public void setUniqueId(int unique_id) {
        uniqueId = unique_id;
        uniqueIdIsSet = true;
    }
    
    /**
     * return unique integer representing this compute node.
     */
    public int getUniqueId() {
        assert uniqueIdIsSet == true;
        return uniqueId;
    }
}

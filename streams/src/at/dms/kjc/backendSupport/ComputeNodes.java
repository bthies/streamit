package at.dms.kjc.backendSupport;

import java.util.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.spacetime.ComputeCodeStore;
/**
 * A ComputeNodes is a collection of {@link ComputeNode}s.
 *  
 * @author dimock
 */
public class ComputeNodes<CodeStoreType extends ComputeCodeStore<?>>  implements ComputeNodesI<CodeStoreType> {

    /** our collection of nodes... */
    private Vector<ComputeNode<CodeStoreType>> nodes;
    
    /**
     * Construct a new collection and fill it with {@link ComputeNode}s.
     * @param numberOfNodes
     */
    public ComputeNodes(int numberOfNodes) {
        nodes = new Vector<ComputeNode<CodeStoreType>>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            nodes.add(new ComputeNode<CodeStoreType>());
        }
    }
    
    /**
     * Assume that it is easy to add more nodes...
     */
    public boolean canAllocateNewComputeNode() {
        return true;
    }

    public ComputeNode<CodeStoreType> getNthComputeNode(int n) {
        return nodes.elementAt(n);
    }

    public boolean isValidComputeNodeNumber(int nodeNumber) {
        return 0 <= nodeNumber && nodeNumber < nodes.size();
    }

    public int newComputeNode() {
        nodes.add(new ComputeNode<CodeStoreType>());
        return nodes.size() - 1;
    }

    public int size() {
        return nodes.size();
    }
    
    
    
//    // Stuff below is more layout...
//    
//    private Map<SliceNode,ComputeNode> sliceToComputeNode;
//    private Set<ComputeNode> computeNodes;
//    
//    public ComputeNodes() {
//        sliceToComputeNode = new HashMap<SliceNode,ComputeNode>();
//        computeNodes = new HashSet<ComputeNode>();
//    }
//    
//    /**
//     * Return number of compute nodes.
//     * @return
//     */
//    public int howMany() {
//        return computeNodes.size();
//    }
//    
//    /**
//     * Add a compute node to this collection of compute nodes. 
//     * @param node
//     */
//    public void addComputeNode(ComputeNode node) {
//        computeNodes.add(node);
//    }
//    
//    /**
//     * Find compute node for slice node.
//     * @param sliceNode
//     * @return
//     */
//    public ComputeNode getComputeNodeForSliceNode(SliceNode sliceNode) {
//        return sliceToComputeNode.get(sliceNode);
//    }
//    
//    /**
//     * Associate a slice with a compute node.
//     * For the moment, if you want to associate a SliceNode with multiple
//     * ComputeNode's you have to clone the SliceNode...
//     * @param computeNode
//     * @param sliceNode
//     */
//    public void setComputeNodeForSliceNode(ComputeNode computeNode, SliceNode sliceNode) {
//        assert computeNodes.contains(computeNode);
//        sliceToComputeNode.put(sliceNode, computeNode);
//    }
}

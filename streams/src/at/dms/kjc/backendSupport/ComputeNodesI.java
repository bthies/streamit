package at.dms.kjc.backendSupport;

import at.dms.kjc.slicegraph.ComputeNode;
import at.dms.kjc.spacetime.ComputeCodeStore;
/**
 * Define what a collection of ComputeNodes must implement.
 * A Collection of ComputeNodes inherits from a collection of ProcElements
 * Here, we want to mix in the fact that ComputeNodes are accessible by
 * some index, possibly a subrange of the indices used to access ProcElements
 * And we need to be able to get the size of a collection of ComputeNodes
 * and an element from the collection. 
 * 
 * Would like to enforce that ComputeNodes have a constructor that takes
 * an integer number of nodes as an argument, but this does not seem possible
 * to specify with a Java interface.
 */
public interface ComputeNodesI<CodeStoreType extends ComputeCodeStore<?>> {

//    /**
//     * Must have a constructor that takes a number of nodes
//     * @param number of ComputeNode's in the initial collection.
//     * @param <T> the class / constructor name.
//     */
//    // not doable in Java, following requests a method with manifest name "T"
//    // not a constructor name parameterized by T.    
//    public <T extends ComputeNodesI<CodeStoreType>> T T(int numNodes);
    
    /**
     * Does this implementation have dynamic creation of new nodes
     * or are all nodes statically allocated.
     * @return if false, {@link #newComputeNode()} may throw any throwable.
     */
    public boolean canAllocateNewComputeNode();
    
    /**
     * Create a new compute node if compute nodes are dynamically creatable.
     * @return unique number for the new compute node, usable with {@link #getComputeNode(int)}.
     */
    public int newComputeNode();
    
    /**
     * Does a number correspond to a compute node?
     * @param nodeNumber  number to check
     * @return true if a valid compute node number, false otherwise.
     */
    public boolean isValidComputeNodeNumber(int nodeNumber);
    
    /**
     * Return the (current) number of compute nodes.
     * @return size of collection of compute nodes.
     */
    public int size();
    
    /**
     * Get the nth element of the collection of compute nodes. 
     * @param <T> the actual class returned.
     */
    public <T extends ComputeNode<CodeStoreType>> T getNthComputeNode(int n);
}

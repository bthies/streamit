package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.ComputeNodesI; 
import java.util.*;
import at.dms.kjc.backendSupport.ComputeNodes;

/**
 * Implementation of {@link at.backendSupport.ComputeNodesI} to provide a collection of {@link UniProcessor}s.
 * Provides mapping of int -> {@link UniProcessor}.
 * @author dimock
 *
 */

public class UniProcessors  implements ComputeNodesI<UniComputeCodeStore>{

    /** our collection of nodes... */
    private Vector<UniProcessor> nodes; 

    /**
     * Construct a new collection and fill it with {@link ComputeNode}s.
     * 
     * @param numberOfNodes
     */
    public UniProcessors(Integer numberOfNodes) {
        nodes = new Vector<UniProcessor>(numberOfNodes);
        for (int i = 0; i < numberOfNodes; i++) {
            UniProcessor node = new UniProcessor(i);
            nodes.add(node);
        }
    }

    /**
     * Assume that it is easy to add more nodes...
     */
    public boolean canAllocateNewComputeNode() {
        return true;
    }

    public UniProcessor getNthComputeNode(int n) {
        return nodes.elementAt(n);
    }

    public boolean isValidComputeNodeNumber(int nodeNumber) {
        return 0 <= nodeNumber && nodeNumber < nodes.size();
    }

    public int newComputeNode() {
        nodes.add(new UniProcessor(nodes.size()));
        return nodes.size() - 1;
    }

    public int size() {
        return nodes.size();
    }

    public UniProcessor[] toArray() {
        return nodes.toArray(new UniProcessor[nodes.size()]);
    }
}


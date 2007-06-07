package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.ComputeNode;

/**
 * Completely vanilla extension to {@link at.dms.kjc.backendSupport.ComputeNode} for a processor (computation node)
 * with no quirks.
 * @author dimock
 *
 */
public class UniProcessor extends ComputeNode<UniComputeCodeStore> {
    public UniProcessor(int uniqueId) {
        super();
        setUniqueId(uniqueId);
        computeCode = new UniComputeCodeStore(this);
    }
}

package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.ComputeNode;

public class UniProcessor extends ComputeNode<UniComputeCodeStore> {
    public UniProcessor(int uniqueId) {
        super();
        setUniqueId(uniqueId);
        computeCode = new UniComputeCodeStore(this);
    }
}

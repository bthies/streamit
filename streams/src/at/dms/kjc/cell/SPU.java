package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.ComputeNode;

public class SPU extends ComputeNode<CellComputeCodeStore> {

    public SPU(int uniqueId) {
        super();
        setUniqueId(uniqueId);
        computeCode = new CellComputeCodeStore(this);
    }
    
}

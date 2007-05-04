package at.dms.kjc.cell;

import at.dms.kjc.backendSupport.ComputeNode;

public class CellPU extends ComputeNode<CellComputeCodeStore> {

    protected CellPU(int uniqueId) {
        super();
        setUniqueId(uniqueId);
        computeCode = new CellComputeCodeStore(this);
    }
}

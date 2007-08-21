package at.dms.kjc.cell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.slicegraph.SliceNode;

public abstract class CellPU extends ComputeNode<CellComputeCodeStore> {

    private HashMap<SliceNode,CellComputeCodeStore> computeCodeStores;
    
    protected CellPU(int uniqueId) {
        super();
        setUniqueId(uniqueId);
        computeCode = new CellComputeCodeStore(this);
        computeCodeStores = new HashMap<SliceNode,CellComputeCodeStore>();
    }
    
    public CellComputeCodeStore getComputeCodeStore(SliceNode s) {
        CellComputeCodeStore cs = computeCodeStores.get(s);
        if (cs == null) {
            cs = new CellComputeCodeStore(this, s);
            computeCodeStores.put(s, cs);
        }
        return cs;
    }
    
    public ArrayList<CellComputeCodeStore> getComputeCodeStores() {
        ArrayList<CellComputeCodeStore> codestores = new ArrayList<CellComputeCodeStore>();
        for (CellComputeCodeStore cs : computeCodeStores.values()) {
            codestores.add(cs);
        }
        return codestores;
    }
    
    public int getNumComputeCodeStores() {
        return computeCodeStores.size();
    }
    
    abstract public boolean isPPU();
    abstract public boolean isSPU();
}

package at.dms.kjc.cell;

import java.util.Vector;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.ComputeNodesI;

public class CellChip implements ComputeNodesI<CellComputeCodeStore> {
    
    private Vector<SPU> spus;
    private PPU ppu;
    
    /**
     * Construct a new collection and fill it with {@link ComputeNode}s.
     * 
     * @param numberOfNodes
     */
    public CellChip(Integer numberOfSpus) {
        spus = new Vector<SPU>(numberOfSpus);
        for (int i = 0; i < numberOfSpus; i++) {
            SPU spu = new SPU(i);
            spus.add(spu);
        }
    }

    public boolean canAllocateNewComputeNode() {
        return true;
    }

    public SPU getNthComputeNode(int n) {
        return spus.elementAt(n);
    }

    public boolean isValidComputeNodeNumber(int spuNumber) {
        return 0 <= spuNumber && spuNumber < spus.size();
    }

    public int newComputeNode() {
        spus.add(new SPU(spus.size()));
        return spus.size() - 1;
    }

    public int size() {
        return spus.size();
    }
    
    public SPU[] toArray() {
        return spus.toArray(new SPU[spus.size()]);
    }

}

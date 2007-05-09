package at.dms.kjc.cell;

import java.util.Vector;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.ComputeNodesI;

public class CellChip implements ComputeNodesI<CellComputeCodeStore> {
    
    private Vector<CellPU> cellPUs;
    private int numSPUs;
    private int totalCellPUs;
    
    /**
     * Construct a new collection and fill it with {@link ComputeNode}s.
     * 
     * @param numberOfNodes
     */
    public CellChip(Integer numberOfSpus) {
        numSPUs = numberOfSpus;
        totalCellPUs = numSPUs + 1;
        cellPUs = new Vector<CellPU>(totalCellPUs);
        cellPUs.add(new PPU(0));
        for (int i = 1; i < totalCellPUs; i++) {
            SPU spu = new SPU(i);
            cellPUs.add(spu);
        }
    }

    public boolean canAllocateNewComputeNode() {
        return true;
    }

    public CellPU getNthComputeNode(int n) {
        return cellPUs.elementAt(n);
    }
    
    public PPU getPPU() {
        return (PPU) cellPUs.elementAt(0);
    }

    public boolean isValidComputeNodeNumber(int cellpuNumber) {
        return 0 <= cellpuNumber && cellpuNumber < cellPUs.size();
    }

    public int newComputeNode() {
        cellPUs.add(new SPU(cellPUs.size()));
        return cellPUs.size() - 1;
    }

    public int size() {
        return cellPUs.size();
    }
    
    public CellPU[] toArray() {
        return cellPUs.toArray(new CellPU[cellPUs.size()]);
    }

}

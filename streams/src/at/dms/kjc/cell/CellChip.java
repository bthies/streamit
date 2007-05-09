package at.dms.kjc.cell;

import java.util.LinkedList;
import java.util.Vector;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.ComputeNodesI;

public class CellChip implements ComputeNodesI<CellComputeCodeStore> {
    
    private Vector<CellPU> cellPUs;
    
    /**
     * Construct a new collection and fill it with {@link ComputeNode}s.
     * 
     * @param numberOfNodes
     */
    public CellChip(Integer numberOfSpus) {
        cellPUs = new Vector<CellPU>(numberOfSpus + 1);
        cellPUs.add(new PPU(0));
        for (int i = 1; i < numberOfSpus + 1; i++) {
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
        return (PPU) cellPUs.firstElement();
    }
    
    public LinkedList<SPU> getSPUs() {
        LinkedList<SPU> spus = new LinkedList<SPU>();
        for (CellPU cpu : cellPUs) {
            if (cpu.isSPU())
                spus.add((SPU) cpu);
        }
        return spus;
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
    
    public int numberOfSPUs() {
        return size() - 1;
    }
    
    public CellPU[] toArray() {
        return cellPUs.toArray(new CellPU[cellPUs.size()]);
    }

}

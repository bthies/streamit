package at.dms.kjc.smp;

import java.util.LinkedList;
import at.dms.kjc.backendSupport.ComputeNodesI;

public class SMPMachine implements ComputeNodesI<CoreCodeStore> {
    protected int numCores;
    protected Core[] cores;
    protected OffChipMemory offChipMemory;
    
    /**
     * Data structures for a machine with a number of cores
     *   
     * @param numCores
     */
    public SMPMachine(int numCores) {
        this.numCores = numCores;
        
        // room for ComputeNode's, to be filled in by subclass
        cores = new Core[numCores];
        for (int x = 0 ; x < numCores ; x++)
            cores[x] = new Core(x, this);

        offChipMemory = new OffChipMemory(this);
    }
    
    public SMPMachine() {
        this(1);
    }
    
    public OffChipMemory getOffChipMemory() {
        return offChipMemory;
    }
    
    public LinkedList<Core> getCores() {
        LinkedList<Core> ts = new LinkedList<Core>();
        for (int x = 0; x < cores.length; x++)
            ts.add(cores[x]);

        return ts;
    }
    
    public boolean canAllocateNewComputeNode() {
        return false;
    }
    
    public Core getNthComputeNode(int n) {
        assert !(n > numCores || n < 0) : "out of bounds in getNthComputeNode() of Machine";
        return cores[n];
    }

    public boolean isValidComputeNodeNumber(int nodeNumber) {
        if(nodeNumber >= 0 && nodeNumber < numCores)
            return true;
        
        return false;
    }

    public int newComputeNode() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int size() {
        return numCores;
    }

    /** 
     * Return the number of cores that have code that needs to execute.
     * 
     * @return the number of cores that have code that needs to execute.
     */
    public int coresWithCompute() {
        int mappedCores = 0;
        
        for (Core t : getCores()) {
            if (t.getComputeCode().shouldGenerateCode()) 
                mappedCores++;
        }
        
        return mappedCores;
    }
}

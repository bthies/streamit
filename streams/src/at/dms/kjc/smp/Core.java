package at.dms.kjc.smp;

import at.dms.kjc.backendSupport.ComputeNode;

public class Core extends ComputeNode<CoreCodeStore> {
    /** the core number */
    protected int coreNum;
    /** the parent machine */
    protected SMPMachine machine;

    /**
     * Construct a new ComputeNode of chip. 
     * 
     * @param machine The parent Machine.
     */
    public Core(int coreNum, SMPMachine machine) 
    {
        super();
        this.machine = machine;
        this.coreNum = coreNum;
        setUniqueId(coreNum);
        
        computeCode = new CoreCodeStore(this);
    }
    
    /**
     * Return the core number of this core
     * 
     * @return The core number
     */
    public int getCoreNumber() {
       return getUniqueId();
    }
    
    /**
     * Return the Machine we are a part of.
     * @return the Machine we are a part of.
     */
    public SMPMachine getMachine() {
        return machine;
    }
    
    public CoreCodeStore getComputeCode() {
        return computeCode;
    }
    
    public boolean isComputeNode() {
        return true;
    }
}

package at.dms.kjc.smp;

import sun.security.krb5.internal.crypto.CksumType;
import at.dms.kjc.CStdType;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.common.ALocalVariable;

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
        
        if(KjcOptions.iterations == -1)
	    computeCode = new CoreCodeStore(this);
        else
	    computeCode = new CoreCodeStore(this, ALocalVariable.makeVar(CStdType.Integer, "maxSteadyIter"));
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

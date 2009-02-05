package at.dms.kjc.smp;

public class OffChipMemory extends Core {
    /**
     * Construct a new ComputeNode of chip. 
     * 
     * @param chip The parent Tile64Chip.
     */
    public OffChipMemory(SMPMachine machine) 
    {
        super(-1, machine);
    }
    
    public boolean isComputeNode() {
        return false;
    }
}

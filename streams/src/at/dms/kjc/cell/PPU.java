package at.dms.kjc.cell;


public class PPU extends CellPU {
    
    public PPU(int uniqueId) {
        super(uniqueId);
    }
    
    public boolean isPPU() {
        return true;
    }
    
    public boolean isSPU() {
        return false;
    }
}

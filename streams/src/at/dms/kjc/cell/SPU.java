package at.dms.kjc.cell;


public class SPU extends CellPU {

    public SPU(int uniqueId) {
        super(uniqueId);
    }
    
    public boolean isPPU() {
        return false;
    }
    
    public boolean isSPU() {
        return true;
    }  
}

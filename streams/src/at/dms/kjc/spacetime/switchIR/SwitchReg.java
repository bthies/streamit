package at.dms.kjc.spacetime.switchIR;

//Switch registers
public class SwitchReg implements SwitchSrc {
    public static final SwitchReg R0=new SwitchReg("$0");
    public static final SwitchReg R1=new SwitchReg("$1");
    public static final SwitchReg R2=new SwitchReg("$2");
    public static final SwitchReg R3=new SwitchReg("$3");
    
    private String reg;
    
    private SwitchReg(String reg) {
	this.reg=reg;
    }
    
    public String toString() {
	return reg;
    }
}

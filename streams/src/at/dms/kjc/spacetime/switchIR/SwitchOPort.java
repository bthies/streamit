package at.dms.kjc.spacetime.switchIR;

//Ports that can be used as output
public class SwitchOPort  {
    public static final SwitchOPort CSTI=new SwitchOPort("$csti");
    public static final SwitchOPort N=new SwitchOPort("$cNo");
    public static final SwitchOPort E=new SwitchOPort("$cEo");
    public static final SwitchOPort S=new SwitchOPort("$cSo");
    public static final SwitchOPort W=new SwitchOPort("$cWo");
    public static final SwitchOPort N2=new SwitchOPort("$cNo2");
    public static final SwitchOPort E2=new SwitchOPort("$cEo2");
    public static final SwitchOPort S2=new SwitchOPort("$cSo2");
    public static final SwitchOPort W2=new SwitchOPort("$cWo2");
    public static final SwitchOPort SWI1=new SwitchOPort("$swo1");
    public static final SwitchOPort SWI2=new SwitchOPort("$swo2");
    
    private String reg;
    
    private SwitchOPort(String reg) {
	this.reg=reg;
    }
    
    public String toString() {
	return reg;
    }
}

package at.dms.kjc.spacetime.switchIR;

//Ports that can be used as output
public class SwitchOPort {
    public static final SwitchOPort CSTO=new SwitchOPort("$csti");
    public static final SwitchOPort N=new SwitchOPort("$cNi");
    public static final SwitchOPort E=new SwitchOPort("$cEi");
    public static final SwitchOPort S=new SwitchOPort("$cSi");
    public static final SwitchOPort W=new SwitchOPort("$cWi");
    public static final SwitchOPort N2=new SwitchOPort("$cNi2");
    public static final SwitchOPort E2=new SwitchOPort("$cEi2");
    public static final SwitchOPort S2=new SwitchOPort("$cSi2");
    public static final SwitchOPort W2=new SwitchOPort("$cWi2");
    public static final SwitchOPort SWI1=new SwitchOPort("$swi1");
    public static final SwitchOPort SWI2=new SwitchOPort("$swi2");
    
    private String reg;
    
    private SwitchOPort(String reg) {
	this.reg=reg;
    }
    
    public String toString() {
	return reg;
    }
}

package at.dms.kjc.spacetime.switchIR;

//Ports that can be used as output
public class SwitchOPort  {
    public static final SwitchOPort CSTI=new SwitchOPort("$csti");
    public static final SwitchOPort CSTI2=new SwitchOPort("$csti2");
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
    
   public static SwitchOPort getOPort(String dir) 
    {
	if (dir == "st")
	    return CSTI;
	if (dir == "N")
	    return N;
	if (dir == "E")
	    return E;
	if (dir == "S")
	    return S;
	if (dir == "W")
	    return W;
	
	assert false : "invalid direction for getIPort";
	return null;
    }
    
    public static SwitchOPort getOPort2(String dir) 
    {
	if (dir == "st")
	    return CSTI2;
	if (dir == "N")
	    return N2;
	if (dir == "E")
	    return E2;
	if (dir == "S")
	    return S2;
	if (dir == "W")
	    return W2;
	assert false : "invalid direction for getIPort2";
	return null;
    }	
}

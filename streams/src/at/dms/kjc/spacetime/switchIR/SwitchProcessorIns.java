package at.dms.kjc.spacetime.switchIR;

//Just a processor inst without route inst
public class SwitchProcessorIns {
    protected String op;
    protected String arg1;
    protected String arg2;
    protected String arg3;

    public String toString() {
	if(op==null)
	    return "nop";
	if(arg2==null)
	    return op+" "+arg1;
	if(arg3==null)
	    return op+" "+arg1+", "+arg2;
	return op+" "+arg1+", "+arg2+" "+arg3;
    }
    
    //Creates just a NOP
    public SwitchProcessorIns() {
    }
}

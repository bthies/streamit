package at.dms.kjc.spacetime.switchIR;

abstract public class SwitchIns {
    protected String op;

    public abstract String toString();
    
    public SwitchIns(String op) {
	this.op = op;
    }
}

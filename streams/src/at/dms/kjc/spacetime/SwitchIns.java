package at.dms.kjc.spacetime;

abstract class SwitchIns {
    protected String op;

    public abstract String toString();
    
    public SwitchIns(String op) {
	this.op = op;
    }
}

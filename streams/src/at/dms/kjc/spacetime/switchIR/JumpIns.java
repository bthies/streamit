package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

public class JumpIns extends SwitchIns {
    private String label;
    
    public JumpIns(String label) {
	super("j");
	this.label = label;
    }

    public String toString() {
	return op + "\t" + label;
    }
}

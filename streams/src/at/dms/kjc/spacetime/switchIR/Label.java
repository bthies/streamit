package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

public class Label implements SwitchIns {
    private String label;

    public Label(String label) {
	//super("");
	this.label = label;
    }

    public String toString() {
	return label + ":";
    }
}

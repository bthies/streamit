package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

public class Label implements SwitchIns {
    private String label;
    private static int uniqueID = 1;

    public Label(String label) {
	//super("");
	this.label = label;
    }

    //generate label 
    public Label() 
    {
	this.label = "__label" + uniqueID++ + "__";
    }
    

    public String toString() {
	return label + ":";
    }

    public String getLabel() {
	return label;
    }
}

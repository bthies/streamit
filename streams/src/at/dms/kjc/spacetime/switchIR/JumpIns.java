package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

public class JumpIns extends SwitchProcessorIns {
    //private String label;
    
    public JumpIns(String label) {
	//super("j");
	//this.label = label;
	op="j";
	arg1=label;
    }

    /*public String toString() {
      return "j" + "\t" + label;
      }*/
}

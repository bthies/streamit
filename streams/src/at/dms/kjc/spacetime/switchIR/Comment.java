package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

public class Comment implements SwitchIns {
    String comment;
    public Comment(String comment) 
    {
	this.comment = comment;
    }
    
    public String toString() 
    {
	return "; " + comment;
    }
}

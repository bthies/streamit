package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;

/**
 * This class generates a push schedule for the switch code by simulating pushes from 
 * the filter. 
 */
public class PushSimulator extends at.dms.util.Utils implements FlatVisitor 
{
    FlatNode current;
    
   
    public PushSimulator(FlatNode currentNode) {
	current = currentNode;
	current.accept(this, new HashSet(), true);
    }
    
    public SwitchScheduleNode getPushSchedule() {
	return null;
    }
    
    public void visitNode(FlatNode node) {
	
    }
}

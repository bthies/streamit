package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class generates the switch code for each tile and writes it to a file 
 */

public class SwitchCode extends at.dms.util.Utils implements FlatVisitor 
{
    private static HashMap switchPushCode;
    private static HashMap switchPopCode;
    
    static {
	switchPushCode = new HashMap();
	switchPopCode = new HashMap();
    }

    public SwitchCode() 
    {
    }
    
    public static void generate(FlatNode top) 
    {
	top.accept(new SwitchCode(), new HashSet(), true);
    }
    
    public void visitNode(FlatNode node) {
	if (node.contents instanceof SIRFilter) {
	    switchPushCode.put(node, PushSimulator.simulate(node));
	    switchPopCode.put(node, PopSimulator.simulate(node));
	}
    }

    public static String getHeading (FlatNode from, FlatNode to) 
    {
	int toTile = Layout.getTile(to.contents);
	int fromTile = Layout.getTile(from.contents);
	if (fromTile - 4 == toTile)
	    return "north";
	if (fromTile -1 == toTile) 
	    return "west";
	if (fromTile +1 == toTile)
	    return "east";
	if (fromTile +4 == toTile)
	    return "south";
	Utils.fail("Nodes not directly connected");
	return null;
    }
    

    public static void dumpCode() {
	System.out.println("============== Push Schedules ==============");
	{
	    Iterator it = switchPushCode.keySet().iterator();
	    
	    while (it.hasNext()) {
		FlatNode node = (FlatNode)it.next();
		System.out.println("Push Schedule for " + 
				   Namer.getName(node.contents) + " on tile " +
				   Layout.getTile(node.contents));
		
		SwitchScheduleNode current = (SwitchScheduleNode)switchPushCode.get(node);
		while (current.next != null) {
		    current.printMe();
		    current = current.next;
		}
	    }
	}
	System.out.println("============== Pop Schedules ==============");
	{
	    Iterator it = switchPopCode.keySet().iterator();
	    
	    while (it.hasNext()) {
		FlatNode node = (FlatNode)it.next();
		System.out.println("Pop Schedule for " + 
				   Namer.getName(node.contents) + " on tile " +
				   Layout.getTile(node.contents));
		
		SwitchScheduleNode current = (SwitchScheduleNode)switchPopCode.get(node);
		while (current.next != null) {
		    current.printMe();
		    current = current.next;
		}
	    }
	    
	}
    }  
}

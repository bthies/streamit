package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.*;


/**
 * This class represents a node in the switch schedule.
 * Each node corresponds with one data item.  So 
 * for duplicate splitters there will be multiple targets
 * for a data item (hence the vector destinations).
 */
public class SwitchScheduleNode 
{
    /* Next node in the schedule */
    public SwitchScheduleNode next;
    /* destination filters for this data item */
    private Vector destinations;
           
    public SwitchScheduleNode() {
	destinations = new Vector();
    }

    public void addDest(FlatNode to) {
	destinations.add(to);
    }
    
    public int getSize() 
    {
	return destinations.size();
    }
    
    public FlatNode getDest(int i) 
    {
	return (FlatNode)destinations.get(i);
    }
    
    //print the assembly code, send is true if this is a schedule for sending
    public String toAssembly(FlatNode node, boolean send) {
	StringBuffer buf = new StringBuffer();
	buf.append("\tnop\troute ");
	for (int i = 0; i < destinations.size(); i++) {
	    if (send)
		buf.append("$csto->" + 
			   //getHeading(node, (FlatNode)destinations.get(i), send) + 
			   ",");
	    else {
		buf.append(//getHeading(node, (FlatNode)destinations.get(i), send) + 
			   "->$csti,");
	    }
	}
	//erase the trailing ,
	buf.setCharAt(buf.length() - 1, '\n');
	return buf.toString();
    }
    
    public void printMe() {
	for (int i = 0; i < destinations.size(); i++) {
	    FlatNode node = (FlatNode)destinations.get(i);
	    System.out.println(" " + node.contents.getName() + " Tile:" +
			       Layout.getTile(node.contents));
	}
	System.out.println("=====");
    }
    /* no longer needed
    private static String getHeading (FlatNode from, FlatNode to, boolean send) 
    {
	StringBuffer buf = new StringBuffer();
	int toTile = Layout.getTile(to.contents);
	int fromTile = Layout.getTile(from.contents);
	if (KjcOptions.raw == 16) {
	    if (fromTile - 4 == toTile)
		buf.append("$cN");
	    if (fromTile -1 == toTile) 
		buf.append("$cW");
	    if (fromTile +1 == toTile)
		buf.append("$cE");
	    if (fromTile +4 == toTile)
		buf.append("$cS");
	    if (buf.toString().equals(""))
		Utils.fail("Nodes not directly connected");
	}
	else if (KjcOptions.raw == 4) {
	    if (fromTile - 2 == toTile)
		buf.append("$cN");
	    if (fromTile -1 == toTile) 
		buf.append("$cW");
	    if (fromTile +1 == toTile)
		buf.append("$cE");
	    if (fromTile +2 == toTile)
		buf.append("$cS");
	    if (buf.toString().equals(""))
		Utils.fail("Nodes not directly connected");
	}

	//sending
	if (send)
	    buf.append("o");
	else
	    buf.append("i");
	return buf.toString();
    }
    */
}


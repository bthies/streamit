package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

//Kept for backwards compatibility
//FullIns should usually suffice
public class RouteIns implements SwitchIns {
    Vector sources;
    Vector dests;
    RawTile tile;

    public RouteIns(RawTile tile) {
	//super("route");
	sources = new Vector();
	dests = new Vector();
	this.tile = tile;
    }

    public void addRoute(ComputeNode source, ComputeNode dest) {
	if (source == null || dest == null) 
	    Utils.fail("Trying to add a null source or dest to route instruction");
	sources.add(source);
	dests.add(dest);
    }

    public String toString() {
	String ins = "nop\troute ";
	
	for (int i = 0; i < sources.size(); i++) {
	    //append the src, then ->, then dst
	    String dir;
	    
	    dir = tile.getRawChip().getDirection(tile, (ComputeNode)sources.get(i));
	    if (dir.equals("st"))
		ins += "$c" + dir + "o";
	    else 
		ins += "$c" + dir + "i";

	    ins += "->";
	    
	    dir = tile.getRawChip().getDirection(tile, (ComputeNode)dests.get(i));
	    if (dir.equals("st"))
		ins += "$c" + dir + "i";
	    else
		ins += "$c" + dir + "o";

	    if (i < sources.size() - 1)
		ins += ",";
	}
	
	return ins;
    }
}





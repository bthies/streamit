package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.util.Utils;

public class RouteIns extends SwitchIns {
    Vector sources;
    Vector dests;
    RawTile tile;

    public RouteIns(RawTile tile) {
	super("route");
	sources = new Vector();
	dests = new Vector();
    }

    public void addRoute(RawTile source, RawTile dest) {
	if (source == null || dest == null) 
	    Utils.fail("Trying to add a null source or dest to route instruction");
	sources.add(source);
	dests.add(dest);
    }

    public String toString() {
	String ins = op + " ";
	
	for (int i = 0; i < sources.size(); i++) {
	    //append the src, then ->, then dst
	    ins += "$c" + tile.getRawChip().getDirection(tile, (RawTile)sources.get(i)) + "i" +
		"->" + 
		"$c" + tile.getRawChip().getDirection(tile, (RawTile)dests.get(i)) + "o";
	    
	    if (i < sources.size() - 1)
		ins += ",";
	}
	
	return ins;
    }
}

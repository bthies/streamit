package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;

public class RateMatch extends at.dms.util.Utils 
    implements FlatVisitor, Constants 
{
    //To test that there are no crossed routes, 
    //keep this hashset of all tiles that are used to 
    //route items (excluding the source/dest of a route)
    HashSet routerTiles;
    private static boolean overlappingRoutes;

    public static boolean doit(FlatNode top) 
    {
	//assume there are no overlapping routes
	overlappingRoutes = false;

	top.accept(new RateMatch(), null, true);
	if (overlappingRoutes) 
	    return false;
	
	return true;
    }

    public RateMatch() 
    {
	routerTiles = new HashSet();
    }

    public void visitNode(FlatNode node) 
    {
	if (Layout.isAssigned(node)) {
	    Iterator it = Util.getAssignedEdges(node).iterator();
	    
	    while (it.hasNext()) {
		FlatNode dest = (FlatNode)it.next();
		Iterator route = Router.getRoute(node, dest).listIterator();
		//remove the source
		Coordinate current = (Coordinate)route.next();
		while (route.hasNext()) {
		    current = (Coordinate)route.next();
		    //now check to see if this tile has been routed thru
		    //before
		    if (current != Layout.getTile(dest)) {
			//now check if we have routed thru here before
			//or if the intermediate hop is a tile assigned 
			//to a filter or joiner
			if (routerTiles.contains(current) ||
			    (Layout.getNode(current) != null))
			    overlappingRoutes = true;
			routerTiles.add(current);
		    }
		    
		}
	    }
	}
    }
}

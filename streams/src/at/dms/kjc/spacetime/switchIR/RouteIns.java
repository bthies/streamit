package at.dms.kjc.spacetime.switchIR;

import java.util.Vector;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.*;

//Kept for backwards compatibility
//FullIns should usually suffice
public class RouteIns implements SwitchIns {
    Vector<Object> sources;
    Vector<RawComputeNode> dests;
    RawTile tile;

    public RouteIns(RawTile tile) {
        //super("route");
        sources = new Vector<Object>();
        dests = new Vector<RawComputeNode>();
        this.tile = tile;
    }

    public void addRoute(RawComputeNode source, RawComputeNode dest) {
        if (source == null || dest == null) 
            Utils.fail("Trying to add a null source or dest to route instruction");
        //check if the source,dest pair exists
        for (int i = 0; i < sources.size();i++) {
            if (sources.get(i) == source &&
                dests.get(i) == dest)
                return;
        }    
        sources.add(source);
        dests.add(dest);
    }

    public void addRoute(SwitchSrc source, RawComputeNode dest) 
    {
        assert (source != null && dest != null) :
            "Trying to add a null source or dest to route instruction";
        //check if the source,dest pair exists
        for (int i = 0; i < sources.size();i++) {
            if (sources.get(i) == source &&
                dests.get(i) == dest)
                return;
        }    
        sources.add(source);
        dests.add(dest);
    }
    

    public String toString() {
        String ins = "nop\troute ";
    
        for (int i = 0; i < sources.size(); i++) {
            //append the src, then ->, then dst
            String dir;
        
            if (sources.get(i) instanceof RawComputeNode) {
                dir = tile.getRawChip().getDirection(tile, (RawComputeNode)sources.get(i));
                if (dir.equals("st"))
                    ins += "$c" + dir + "o";
                else 
                    ins += "$c" + dir + "i";
            }
            else if (sources.get(i) instanceof SwitchReg) {
                ins += ((SwitchReg)sources.get(i)).toString();
            }
        
        
            ins += "->";
        
            dir = tile.getRawChip().getDirection(tile, dests.get(i));
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





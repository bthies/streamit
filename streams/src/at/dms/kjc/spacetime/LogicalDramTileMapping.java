/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This static class stores the logical mapping between tiles and drams that the 
 * compiler uses to map buffers to drams.  A dram is owned by a tile and all the
 * dram commands are issued from the owning tile.  
 *   
 * @author mgordon
 *
 */
public class LogicalDramTileMapping {

    /** the raw chip these buffers are using **/
    private static RawChip rawChip;
    /** the mapping of drams to the "owner" of the dram, the owner
     * is the raw tile where the dram commands are issued.
     */
    private static HashMap dramTileMap;
    /** a hash set of RawTiles that must use the gdn for communication 
     * non-border tiles.
     */
    private static HashSet mustUseGDN;
    /** map of RawTile -> StreamingDRAM, the "home-base" dram for a tile */ 
    private static HashMap tileDramMap;
    
    static {
        rawChip = SpaceTimeBackend.getRawChip();
    
        assert rawChip.getTotalTiles() == 16 : "We only support 16 tiles configs.";
        
        //create the mapping of buffers to owners
        dramTileMap = new HashMap();
        tileDramMap = new HashMap();
        mustUseGDN = new HashSet();
        
        dramTileMap.put((StreamingDram)rawChip.getDevices()[15], rawChip.getTile(0));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[1], rawChip.getTile(1));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[2], rawChip.getTile(2));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[3], rawChip.getTile(3));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[14], rawChip.getTile(4));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[0], rawChip.getTile(5));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[4], rawChip.getTile(6));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[5], rawChip.getTile(7));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[13], rawChip.getTile(8));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[12], rawChip.getTile(9));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[8], rawChip.getTile(10));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[6], rawChip.getTile(11));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[11], rawChip.getTile(12));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[10], rawChip.getTile(13));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[9], rawChip.getTile(14));
        dramTileMap.put((StreamingDram)rawChip.getDevices()[7], rawChip.getTile(15));
        
        Iterator keys = dramTileMap.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            tileDramMap.put(dramTileMap.get(key), key);
        }
        
        mustUseGDN.add(rawChip.getTile(5));
        mustUseGDN.add(rawChip.getTile(6));
        mustUseGDN.add(rawChip.getTile(9));
        mustUseGDN.add(rawChip.getTile(10));
    }
    
    public static RawTile getOwnerTile(StreamingDram dram) {
        return (RawTile)dramTileMap.get(dram);
    }
    
    public static StreamingDram getHomeDram(RawTile tile) {
        return (StreamingDram)tileDramMap.get(tile);
    }
    
    public static boolean mustUseGdn(RawTile tile) {
        return mustUseGDN.contains(tile);
    }
}


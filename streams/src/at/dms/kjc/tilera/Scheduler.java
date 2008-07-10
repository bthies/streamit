package at.dms.kjc.tilera;

import at.dms.kjc.sir.*;
import at.dms.kjc.backendSupport.*;
import java.util.HashMap;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.KjcOptions;

/**
 * This class is the super class of all partitioners that act on the SIR graph to
 * data-parallelize the application.  Currently we support space-multiplexed and
 * time-multiplexed data parallel partitioners.  
 * 
 * @author mgordon
 *
 */
public abstract class Scheduler implements Layout<Tile> {
    
    protected SpaceTimeScheduleAndSlicer graphSchedule;
    protected HashMap<SliceNode, Tile> layoutMap;
    
    public Scheduler() {
        graphSchedule = null;
        layoutMap = new HashMap<SliceNode, Tile>();
    }

    public boolean isSMD() {
        return (this instanceof SMD);
    }
    
    public boolean isTMD() {
        return (this instanceof TMD);
    }
    
    public void setGraphSchedule(SpaceTimeScheduleAndSlicer graphSchedule) {
        this.graphSchedule = graphSchedule;
    }
    
    public abstract void run(SIRStream str, int tiles);
    
    /** 
     * Given that we always have to generate code for an 8x8 config, but we might 
     * not want to use all those tiles, this function translates a tile number on the 
     * abstract chip config (where we could have a config smaller than 8x8) to a tile 
     * number on the 8x8 chip.  The top left of the chip will be used for smaller configs.
     * 
     * This will work for only square configs.
     * 
     * @param n The abstract tile number to translate
     * 
     * @return The tile number translated to the 8x8 config
     */
    public int translateTileNumber(int n) {
        int row = n / KjcOptions.tilera;
        int col = n % KjcOptions.tilera;
        
        return TileraBackend.chip.getComputeNode(row, col).getTileNumber();
    }
    
    /**
     * Given a tile number in the abstract chip configuration that the 
     * user requested (where tiles per row could be less than 8 and/or tiles per
     * column could be less than 8), get the tile in the actual chip (alway 8x8) 
     * that corresponds to this tile.  
     * 
     * @param n The abstract tile number of the tile we want
     * 
     * @return The tile we desire
     */
    public Tile getTranslatedTile(int n) {
        return TileraBackend.chip.getNthComputeNode(translateTileNumber(n));
    }
}

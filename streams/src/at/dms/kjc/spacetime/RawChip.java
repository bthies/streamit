package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.ComputeNodesI;
import at.dms.kjc.spacetime.switchIR.*;

/**
 * This class represents the raw chip to which we are compiling. 
 * This class tries to deal with the collection of RAW tiles.
 * See {@link RawProcElements} for dealing with I/O devices,
 * and dealing with geometry that could include I/O devices as well as tiles.
 * @author mgordon
 *
 */
public class RawChip extends RawProcElements implements ComputeNodesI<RawComputeCodeStore> {
 
    public static final int cacheLineBytes = 32;

    public static final int cacheLineWords = 8;

    
    
    /** the name of the function that constructs dynamic headers in raw.h */
    public static final String ConstructDynHdr = "construct_dyn_hdr";
    /** the maximum packet size of a gdn packet (not including the header) */
    public static final int MAX_GDN_PKT_SIZE = 31;
    /** the opcode that for a packet of data for the dram send over the gdn */
    public static final int DRAM_GDN_DATA_OPCODE = 13;
    
    /**
     * Initialize a <pre>xSize</pre> x <pre>ySize</pre> raw chip.
     *   
     * @param xSize
     * @param ySize
     */
    public RawChip(int xSize, int ySize) {
        super(xSize,ySize);
        for (int x = 0; x < xSize; x++)
            for (int y = 0; y < ySize; y++)
                tiles[x][y] = new RawTile(x, y, this);

//        if (KjcOptions.magicdram)
//            addMagicDrams();
//        else
            // install streaming drams
            addStreamingDrams();

        printChip();
        // System.exit(1);
    }
    
    /**
     * Create a RawChip and its associated RawTiles given a total number of tiles.
     * @param size number of tiles, in a square array.
     */
    public RawChip(int size) {
        this((int)(Math.sqrt((double)size)), (int)(Math.sqrt((double)size)));
        assert (int)(Math.sqrt((double)size)) * (int)(Math.sqrt((double)size)) == size;
    }

    public boolean isValidComputeNodeNumber(int tileNum) {
        return (tileNum / gXSize) < gYSize;
    }
    
    public RawTile getTile(int tileNumber) {
        int y = tileNumber / gXSize;
        int x = tileNumber % gXSize;
        if (y >= gYSize)
            Utils.fail("out of bounds in getTile() of RawChip");
        return tiles[x][y];
    }

    public RawTile getNthComputeNode(int n) {
        return getTile(n);
    }
    
    public RawTile getTile(int x, int y) {
        assert !(x >= gXSize || y >= gYSize) : "out of bounds in getTile() of RawChip";
        return tiles[x][y];
    }

    public int getTotalTiles() {
        return gXSize * gYSize;
    }
    
    public int size() {
        return getTotalTiles();
    }
    
    public boolean canAllocateNewComputeNode() {
        return false;
    }
    
    public int newComputeNode() {
        throw new AssertionError("tiles on RawChip are statically allocated");
    }
    
    /**
     * Returns the numbers of tiles of the raw chip that have compute code at
     * the given tile
     */
    public int computingTiles() {
        int sum = 0;

        for (int i = 0; i < this.gXSize; i++)
            for (int j = 0; j < this.gYSize; j++)
                if (getTile(i, j).hasComputeCode())
                    sum++;
        return sum;
    }

    /**
     * Get the tile that is at the specified <pre>dir</pre> from <pre>tile</pre>.
     * 
     * @param tile
     * @param dir 'N', 'S', 'W', 'E'
     * @return the tile
     */
    public RawTile getTile(RawTile tile, char dir) {
        int dirX = 0;
        int dirY = 0;
        if (dir == 'N')
            dirY = -1;
        else if (dir == 'S')
            dirY = 1;
        else if (dir == 'E')
            dirX = 1;
        else if (dir == 'W')
            dirX = -1;
        else
            assert false : "Calling getTile() with a bad direction char.";
        
        //get the node in the specified direction...        
        RawComputeNode node =  
            getComputeNode(tile.getX() + dirX , tile.getY() + dirY);
        
        //now if it is a tile, return it, otherwise return null
        if (node instanceof RawTile)
            return (RawTile)node;
        else
            return null;
    }
    
    public void printChip() {
//        if (!KjcOptions.magicdram) {
            StreamingDram.printSetup(this);
            RawTile.printDramSetup(this);
//        }
    }

    private void addStreamingDrams() {
        int i, index = 0;
        // add the north streaming drams
        for (i = 0; i < this.gXSize; i++) {
            devices[index] = new StreamingDram(this, index);
            index++;
        }
        // add the east streaming drams
        for (i = 0; i < this.gYSize; i++) {
            devices[index] = new StreamingDram(this, index);
            index++;
        }
        // add the south streaming drams
        for (i = this.gXSize - 1; i >= 0; i--) {
            devices[index] = new StreamingDram(this, index);
            index++;
        }
        // add the west streaming drams
        for (i = this.gYSize - 1; i >= 0; i--) {
            devices[index] = new StreamingDram(this, index);
            index++;
        }
        StreamingDram.setSize(this);
        StreamingDram.setBounds(this);
        StreamingDram.setTiles(this);
    
    }

    private void addMagicDrams() {
        devices[0] = new MagicDram(this, 0, tiles[0][0]);
        devices[1] = new MagicDram(this, 1, tiles[1][0]);
        devices[2] = new MagicDram(this, 2, tiles[2][0]);
        devices[3] = new MagicDram(this, 3, tiles[3][0]);
        devices[5] = new MagicDram(this, 5, tiles[1][3]);
        devices[6] = new MagicDram(this, 6, tiles[2][3]);
        devices[7] = new MagicDram(this, 7, tiles[3][3]);
        devices[9] = new MagicDram(this, 9, tiles[2][3]);
        devices[10] = new MagicDram(this, 10, tiles[1][3]);
        devices[11] = new MagicDram(this, 11, tiles[0][3]);
        devices[13] = new MagicDram(this, 13, tiles[0][2]);
        devices[14] = new MagicDram(this, 14, tiles[0][1]);
    }
}

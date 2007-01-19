package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.spacetime.switchIR.*;

/**
 * This class represents the raw chip to which we are compiling.  It has tiles and 
 * offchip devices and ports, etc.  
 * 
 * @author mgordon
 *
 */
public class RawChip {
    // the indices are x, y
    private RawTile[][] tiles;

    private IODevice[] devices;

    private int gXSize;

    private int gYSize;

    public static final int cacheLineBytes = 32;

    public static final int cacheLineWords = 8;

    
    
    /** the name of the function that constructs dynamic headers in raw.h */
    public static final String ConstructDynHdr = "construct_dyn_hdr";
    /** the maximum packet size of a gdn packet (not including the header) */
    public static final int MAX_GDN_PKT_SIZE = 31;
    /** the opcode that for a packet of data for the dram send over the gdn */
    public static final int DRAM_GDN_DATA_OPCODE = 13;
    
    /**
     * Initialize a <pre>xsize</pre> x <pre>ysize</pre> raw chip.
     *   
     * @param xSize
     * @param ySize
     */
    public RawChip(int xSize, int ySize) {
        gXSize = xSize;
        gYSize = ySize;

        tiles = new RawTile[xSize][ySize];
        for (int x = 0; x < xSize; x++)
            for (int y = 0; y < ySize; y++)
                tiles[x][y] = new RawTile(x, y, this);

        devices = new IODevice[2 * gXSize + 2 * gYSize];

//        if (KjcOptions.magicdram)
//            addMagicDrams();
//        else
            // install streaming drams
            addStreamingDrams();

        printChip();
        // System.exit(1);
    }

    public boolean isValidTileNumber(int tileNum) {
        return (tileNum / gXSize) < gYSize;
    }
    
    public RawTile getTile(int tileNumber) {
        int y = tileNumber / gXSize;
        int x = tileNumber % gXSize;
        if (y >= gYSize)
            Utils.fail("out of bounds in getTile() of RawChip");
        return tiles[x][y];
    }

    public RawComputeNode getComputeNode(int x, int y) {
        assert !(x > gXSize || y > gYSize || x < -1 || y < -1) : "out of bounds in getComputeNode() of RawChip";

        if (x == gXSize || y == gYSize || x == -1 || y == -1) {
            if (y == -1)
                return devices[x];
            if (x == -1)
                return devices[(gYSize - 1) + (2 * gXSize + gYSize) - y];
            if (y == gYSize)
                return devices[(gXSize + 1) + (gXSize + gYSize) - x];
            if (x == gXSize)
                return devices[y + gXSize];
        }
        return tiles[x][y];
    }

    public RawTile getTile(int x, int y) {
        assert !(x >= gXSize || y >= gYSize) : "out of bounds in getTile() of RawChip";
        return tiles[x][y];
    }

    public int getTotalTiles() {
        return gXSize * gYSize;
    }

    public int getXSize() {
        return gXSize;
    }

    public int getYSize() {
        return gYSize;
    }

    // returns "E", "N", "S", "W", or "st" if src == dst
    public String getDirection(RawComputeNode from, RawComputeNode to) {
        if (from == to)
            return "st";

        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return "S";
            else if (dir == 1)
                return "N";
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return "E";
            else if (dir == 1)
                return "W";
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        // System.out.println(from);
        // System.out.println(((MagicDram)to).getPort());
        // System.out.println("[" + from.getX() + ", " + from.getY() + "] -> ["
        // +
        // to.getX() + ", " + to.getY() + "]");

        Utils.fail("calling getDirection on non-neighbors");
        return "";
    }

    // Same as getDirection(ComputeNode from, ComputeNode to) except returns
    // SwitchOPort
    public SwitchOPort getOPort(RawComputeNode from, RawComputeNode to) {
        System.out.println("Get Out: " + from + " " + to);
        if (from == to)
            return SwitchOPort.CSTI;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchOPort.S;
            else if (dir == 1)
                return SwitchOPort.N;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchOPort.E;
            else if (dir == 1)
                return SwitchOPort.W;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
    }

    // Same as getOPort(ComputeNode from, ComputeNode to) except use static net
    // 2
    public SwitchOPort getOPort2(RawComputeNode from, RawComputeNode to) {
        System.out.println("Get Out: " + from + " " + to);
        if (from == to)
            return SwitchOPort.CSTI2;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchOPort.S2;
            else if (dir == 1)
                return SwitchOPort.N2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchOPort.E2;
            else if (dir == 1)
                return SwitchOPort.W2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
    }

    // Same as getDirection(ComputeNode from, ComputeNode to) except returns
    // SwitchIPort
    public SwitchIPort getIPort(RawComputeNode from, RawComputeNode to) {
        // System.out.println("Get In: "+from+" "+to);
        if (from == to)
            return SwitchIPort.CSTO;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchIPort.N;
            else if (dir == 1)
                return SwitchIPort.S;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchIPort.W;
            else if (dir == 1)
                return SwitchIPort.E;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
    }

    // Same as getIPort2(ComputeNode from, ComputeNode to) except returns static
    // net 2 port
    public SwitchIPort getIPort2(RawComputeNode from, RawComputeNode to) {
        // System.out.println("Get In: "+from+" "+to);
        if (from == to)
            return SwitchIPort.CSTO;
        if (from.getX() == to.getX()) {
            int dir = from.getY() - to.getY();
            if (dir == -1)
                return SwitchIPort.N2;
            else if (dir == 1)
                return SwitchIPort.S2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        if (from.getY() == to.getY()) {
            int dir = from.getX() - to.getX();
            if (dir == -1)
                return SwitchIPort.W2;
            else if (dir == 1)
                return SwitchIPort.E2;
            else
                Utils.fail("calling getDirection on non-neighbors");
        }
        Utils.fail("calling getDirection on non-neighbors");
        return null;
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

    public int getNumDev() {
        return devices.length;
    }

    public IODevice[] getDevices() {
        return devices;
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

    /**
     * @param tile1
     * @param tile2
     * @return True if <pre>tile1</pre> neighbors <pre>tile2</pre>.
     */
    public boolean areNeighbors(RawComputeNode tile1, RawComputeNode tile2) {
        if (tile1 == tile2)
            return false;
        if (tile1.getY() == tile2.getY())
            if (Math.abs(tile1.getX() - tile2.getX()) == 1)
                return true;
        if (tile1.getX() == tile2.getX())
            if (Math.abs(tile1.getY() - tile2.getY()) == 1)
                return true;
        // not conntected
        return false;
    }
 
    /***************************************************************************
     * get the direction in X from src to dst, -1 for west 0 for same X, and 1
     * for east
     **************************************************************************/
    public int getXDir(RawComputeNode src, RawComputeNode dst) {
        if (dst.getX() - src.getX() < 0)
            return -1;
        if (dst.getX() - src.getX() > 0)
            return 1;
        return 0;
    }

    /**
     * get the direction in Y from src to dst, -1 for North 0 for same and 1 for
     * south.
     */
    public int getYDir(RawComputeNode src, RawComputeNode dst) {
        if (dst.getY() - src.getY() < 0)
            return -1;
        if (dst.getY() - src.getY() > 0)
            return 1;
        return 0;
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
    
    /** 
     * @param n1
     * @param n2
     * @return The hops between <pre>n1</pre> and <pre>n2</pre>.
     */
    public int manhattanDistance(RawComputeNode n1, 
            RawComputeNode n2) {
        //now compute the manhattan distance from the source and from the
        //dest
        int dist = Math.abs(n1.getX() - n2.getX()) + 
                Math.abs(n1.getY() - n2.getY());
    
        return dist;
    }
    
    public void printChip() {
//        if (!KjcOptions.magicdram) {
            StreamingDram.printSetup(this);
            RawTile.printDramSetup(this);
//        }
    }
}

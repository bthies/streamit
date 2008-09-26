package at.dms.kjc.tilera;

import java.util.LinkedList;
import java.util.List;
import at.dms.kjc.backendSupport.ComputeNodesI;
import at.dms.util.Utils;
import at.dms.kjc.KjcOptions;

public class TileraChip implements ComputeNodesI<TileCodeStore> {
    protected int gXSize;
    protected int gYSize;
    protected Tile[][] tiles;
    protected OffChipMemory offChipMemory;
    
    /**
     * Data structures for a <b>xSize</b> x <b>ySize</b> Tile64 chip.
     *   
     * @param xSize
     * @param ySize
     */
    public TileraChip(int xSize, int ySize) {
        // grid sizes
        gXSize = xSize;
        gYSize = ySize;
        // room for ComputeNode's, to be filled in by subclass
        tiles = new Tile[xSize][ySize];
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                tiles[x][y] = new Tile(x, y, this);             
            }
        }
        offChipMemory = new OffChipMemory(-1, -1, this);
    }
    
    public TileraChip() {
        this(8, 8);
    }
    
    public OffChipMemory getOffChipMemory() {
        return offChipMemory;
    }
    
    public LinkedList<Tile> getTiles() {
        LinkedList<Tile> ts = new LinkedList<Tile>();
        for (int x = 0; x < tiles.length; x++)
            for (int y= 0; y < tiles[x].length; y++)
                ts.add(tiles[x][y]);
        return ts;
    }
    
    public int getXSize() {
        return gXSize;
    }

    public int getYSize() {
        return gYSize;
    }

    /***************************************************************************
     * get the direction in X from src to dst, -1 for west 0 for same X, and 1
     * for east
     **************************************************************************/
    public int getXDir(Tile src, Tile dst) {
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
    public int getYDir(Tile src, Tile dst) {
        if (dst.getY() - src.getY() < 0)
            return -1;
        if (dst.getY() - src.getY() > 0)
            return 1;
        return 0;
    }

    /** 
     * @param n1
     * @param n2
     * @return The hops between <pre>n1</pre> and <pre>n2</pre>.
     */
    public int manhattanDistance(Tile n1, Tile n2) {
        //now compute the manhattan distance from the source and from the
        //dest
        int dist = Math.abs(n1.getX() - n2.getX()) + 
                Math.abs(n1.getY() - n2.getY());
    
        return dist;
    }

    /**
     * @param tile1
     * @param tile2
     * @return True if <pre>tile1</pre> neighbors <pre>tile2</pre>.
     */
    public boolean areNeighbors(Tile tile1, Tile tile2) {
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

    public Tile getComputeNode(int x, int y) {
        assert !(x > gXSize || y > gYSize || x < -1 || y < -1) : "out of bounds in getComputeNode() of RawChip";
        /*
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
        */
        return tiles[x][y];
    }

    public String getDirection(Tile from, Tile to) {
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
    
    public boolean canAllocateNewComputeNode() {
        return false;
    }

    public Tile getNthComputeNode(int n) {
        return tiles[n / gYSize][n % gYSize];
    }

    public boolean isValidComputeNodeNumber(int nodeNumber) {
        // TODO Auto-generated method stub
        return false;
    }

    public int newComputeNode() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Given that we for now we have to generate code for an 8x8 config and 
     * we might not want to use all those tiles, this returns the number of tiles that 
     * we would actually want to use from the 64 tiles.  It just squares KjcOption.tilera,
     * the parameter value that the user passes to the backend.
     * 
     * @return The number of tiles the user would like to generate code for
     */
    public int abstractSize() {
        return KjcOptions.tilera * KjcOptions.tilera;
    }
    
    /**
     * Return a linked list of the tiles that we are mapping to (a subset of the 
     * 8x8 grid specified by the user)
     * 
     * @return list of tiles we are mapping to
     */
    public List<Tile> getAbstractTiles() {
        LinkedList<Tile> tiles = new LinkedList<Tile>();
        
        for (int x = 0; x < KjcOptions.tilera; x++)
            for (int y = 0; y < KjcOptions.tilera; y++)
                tiles.add(this.tiles[x][y]);
        
        return tiles;
    }
    
    /**
     * Given that we always have to generate code for an 8x8 config, but we might 
     * not want to use all those tiles, this function takes a tile number on the 8x8 config
     * and translates it into an abstract tile number on the config size the user specifies.
     * 
     * @param t tile number in the 8x8 actual config
     * 
     * @return The abstract tile number for the user specified config
     */
    public int getTranslatedTileNumber(int t) {
        Tile tile = getNthComputeNode(t);
        assert tile.X < KjcOptions.tilera;
        assert tile.Y < KjcOptions.tilera;
        
        return tile.X * KjcOptions.tilera + tile.Y;
    }
    
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
        
        return getComputeNode(row, col).getTileNumber();
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
        return getNthComputeNode(translateTileNumber(n));
    }
    
    public int size() {
        // TODO Auto-generated method stub
        return gXSize * gYSize;
    }

    /** 
     * Return the number of tiles that have code that needs to execute.
     * 
     * @return the number of tiles that have code that needs to execute.
     */
    public int tilesWithCompute() {
        int mappedTiles = 0;
        
        for (Tile t : getTiles()) {
            if (t.getComputeCode().shouldGenerateCode()) 
                mappedTiles++;
        }
        
        return mappedTiles;
    }
}

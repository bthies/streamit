package at.dms.kjc.tilera;

import at.dms.kjc.backendSupport.ComputeNode;

public class Tile extends ComputeNode<TileCodeStore> {
    /** the x coordinate */
    protected int X;
    /** the y coordinate */
    protected int Y;
    /** the parent RawChip */
    protected TileraChip tile64Chip;
    

    /**
     * Construct a new ComputeNode of chip. 
     * 
     * @param chip The parent Tile64Chip.
     */
    public Tile(int x, int y, TileraChip chip) 
    {
        super();
        this.tile64Chip = chip;
        X = x;
        Y = y;
        setTileNumber();
        computeCode = new TileCodeStore(this);
    }
    
    private void setTileNumber() {
        setUniqueId((Y * tile64Chip.getXSize()) + X);
    }

    /**
     * Return the tile number of this tile which is an int [0, tiles), that counts 
     * the rows starting at the left...
     * 
     * @return The tile number
     */
    public int getTileNumber() {
       return getUniqueId();
    }
    
    /**
     * Return the Tile64Chip we are a part of.
     * @return the Tile64Chip we are a part of.
     */
    public TileraChip getRawChip() {
        return tile64Chip;
    }
    
    /**
     * Return the x coordinate.
     * @return the x coordinate.
     */
    public int getX() {
        return X;
    }

    /** 
     * @return true if this tile is a compute tile, or false otherwise (e.g., it is
     * off-chip memory which is crappily a subclass of tile now).
     */
    public boolean isComputeTile() {
       return (X >= 0 && X < tile64Chip.gXSize &&
                Y >= 0 && Y < tile64Chip.gYSize);          
    }
    
    /**
     * Return the y coordinate.
     * @return the y coordinate.
     */
    public int getY() {
        return Y;
    }
    
    public TileCodeStore getComputeCode() {
        assert isComputeTile();
        return computeCode;
    }
}

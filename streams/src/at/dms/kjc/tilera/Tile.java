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
     * When we are snaking a circuit across the chip of all the tiles, this
     * function returns the next tile that is on the snake path.  This is done
     * on the abstract config. 
     */
    public Tile getNextSnakeTile() {
        assert tile64Chip.abstractXSize() % 2 == 0 &&
            tile64Chip.abstractYSize() %2 == 0;
        
        int row = Y, col = X;
        
        if (row == 0 && col == 0) {
            return tile64Chip.getComputeNode(col, row + 1);
        } else if (col % 2 == 0) {
            if (row == 0) { //not 0,0
                return tile64Chip.getComputeNode(col - 1, row);
            } else if (row == 1 && col > 0) {
                return tile64Chip.getComputeNode(col, row + 1);
            } else if (row == tile64Chip.abstractYSize() -1) {
                return tile64Chip.getComputeNode(col + 1, row);
            } else {
                return tile64Chip.getComputeNode(col, row + 1);
            }
        } else {
            //odd col
            if (row == 0 && col == tile64Chip.abstractXSize() -1) {
                return tile64Chip.getComputeNode(col - 1, row);
            } else if (row == 0) {
                return tile64Chip.getComputeNode(col - 1, row);
            } else if (row == 1 && col < tile64Chip.abstractXSize() - 1) {
                return tile64Chip.getComputeNode(col + 1, row);
            } else if (row == tile64Chip.abstractYSize() - 1) {
                return tile64Chip.getComputeNode(col, row - 1);
            } else {
                return tile64Chip.getComputeNode(col, row - 1);
            }
        }
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

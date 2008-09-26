package at.dms.kjc.tilera;

public class OffChipMemory extends Tile {
    /**
     * Construct a new ComputeNode of chip. 
     * 
     * @param chip The parent Tile64Chip.
     */
    public OffChipMemory(int x, int y, TileraChip chip) 
    {
        super(x, y, chip);
        this.tile64Chip = chip;
        X = x;
        Y = y;
        setTileNumber();
        computeCode = new TileCodeStore(this);
    }
    
    private void setTileNumber() {
        setUniqueId(-1);
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
        assert false;
        return -1;
    }

    /**
     * Return the y coordinate.
     * @return the y coordinate.
     */
    public int getY() {
        assert false;
        return Y;
    }
}

package at.dms.kjc.spacetime;

import at.dms.util.Utils;

public class RawChip {
    //the indices are x, y
    private RawTile[][] tiles;
    private int gXSize;
    private int gYSize;
    
    public RawChip(int rows, int columns) {
	tiles = new RawTile[rows][columns];
	for (int row = 0; row < rows; row++)
	    for (int col = 0; col < columns; col++)
		tiles[row][col] = new RawTile(row, col, this);

	gXSize = rows;
	gYSize = columns;
    }
    
    public RawTile getTile(int x, int y) {
	if (x >= gXSize || y >= gYSize)
	    Utils.fail("out of bounds in getTile() of RawChip");
	return tiles[x][y];
    }

    public int getXSize() {
	return gXSize;
    }

    public int getYSize() {
	return gYSize;
    }

    //returns "E", "N", "S", "W", or "st" if src == dst
    public String getDirection(RawTile from, RawTile to) {
	if (from == to)
	    return "st";

	
	if (from.getX() == to.getX()) {
	    int dir = from.getY() - to.getY();
	    if (dir == -1)
		return "E";
	    else if (dir == 1)
		return "W";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if (from.getY() == to.getY()) {
	    int dir = from.getX() - to.getX();
	    if (dir == -1) 
		return "S";
	    else if (dir == 1)
		return "N";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	Utils.fail("calling getDirection on non-neighbors");
	return "";
    }
}

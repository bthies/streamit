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
		tiles[row][col] = new RawTile(row, col);

	gXSize = rows;
	gYSize = columns;
    }
    
    public RawTile getTile(int x, int y) {
	if (x >= gXSize || y >= gYSize)
	    Utils.fail("out of bounds in getTile() of RawChip");
	return tiles[x][y];
    }
}

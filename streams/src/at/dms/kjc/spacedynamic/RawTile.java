package at.dms.kjc.spacedynamic;

import at.dms.kjc.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;


public class RawTile extends ComputeNode {
    private int tileNumber;
    //true if this tile has switch code
    private boolean switches;
    //true if this tile has compute code
    private boolean computes;
    //true if a filter has been mapped to it
    private boolean mapped;
    
    public RawTile(int x, int y, RawChip rawChip) {
	super(rawChip);
	X = x;
	Y = y;
	setTileNumber();
	computes = false;
	mapped = false;
	switches = false;
    }

    public String toString() {
	return "Tile["+X+", "+Y+"]";
    }
    
    
    private void setTileNumber() {
	tileNumber = (Y * rawChip.getXSize()) + X;
	/*
	//because the simulator only simulates 4x4 or 8x8 we
	//have to translate the tile number according to these layouts
	int columns = 4;
	if (rawChip.getYSize() > 4 || rawChip.getXSize() > 4)
	    columns = 8;
	tileNumber = (Y * columns) + X;
	*/
    }


    public int getTileNumber() {
	return tileNumber;
    }

    public boolean hasComputeCode() {
	return computes;
    }

    public boolean hasSwitchCode() {
	return switches;
    }

    //this is set by SwitchCodeStore
    public void setSwitches() {
	switches = true;
    }

    public void setMapped() 
    {
	mapped = true;
	setComputes();
    }
    
    public boolean isMapped() 
    {
	return mapped;
    }
    

    //this is set by ComputeCodeStore
    public void setComputes() {
	computes = true;
    }

    public List getSouthAndEastNeighbors() 
    {
	LinkedList neighbors = new LinkedList();
	
	//get east neighbor
	if (X + 1 < rawChip.getXSize()) 
	    neighbors.add(rawChip.getTile(X + 1, Y));	
	//get south neighbor	
	if (Y + 1 < rawChip.getYSize())
	    neighbors.add(rawChip.getTile(X, Y + 1));

	return neighbors;
    }
    

    public Vector getNeighborTiles() 
    {
	Vector ret = new Vector();
	if (X - 1 >= 0)
	    ret.add(rawChip.getTile(X-1, Y));
	if (X + 1 < rawChip.getXSize())
	    ret.add(rawChip.getTile(X+1, Y));
	if (Y - 1 >= 0)
	    ret.add(rawChip.getTile(X, Y-1));
	if (Y + 1 < rawChip.getYSize())
	    ret.add(rawChip.getTile(X, Y+1));
	return ret;
    }
    
}

package at.dms.kjc.spacetime;

import at.dms.kjc.*;

public abstract class ComputeNode 
{
    protected int X;
    protected int Y;
    
    protected RawChip rawChip;

    public ComputeNode(RawChip chip) 
    {
	this.rawChip = chip;
    }
    
    public RawChip getRawChip() {
	return rawChip;
    }
    
    public int getX() {
	return X;
    }

    public int getY() {
	return Y;
    }
}

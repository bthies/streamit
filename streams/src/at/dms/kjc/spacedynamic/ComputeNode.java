package at.dms.kjc.spacedynamic;

import at.dms.kjc.*;

/** A node on a raw chip that can compute, right now either a
    raw tile or an IOPort **/

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
    
    public boolean isPort() 
    {
        return this instanceof IOPort;
    }

    public boolean isTile() 
    {
        return this instanceof RawTile;
    }
}

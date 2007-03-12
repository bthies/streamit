package at.dms.kjc.spacetime;

import at.dms.kjc.*;

/**
 * This abstract class represents a node of the Raw chip that
 * performs computation.  So conceptually, it could be a tile or 
 * even an i/o device connected to the side of the chip that we
 * route from or to.  
 * 
 * For tiles, we start the coordinate numbering at the upper left 
 * side of the chip.  Thus devices can have negative coordinate 
 * components.
 * 
 * @author mgordon
 *
 */
public class RawComputeNode extends at.dms.kjc.backendSupport.ComputeNode<RawComputeCodeStore>
{
    /** the x coordinate */
    protected int X;
    /** the y coordinate */
    protected int Y;
    /** the parent RawChip */
    protected RawChip rawChip;

    /**
     * Construct a new ComputeNode of chip. 
     * 
     * @param chip The parent RawChip.
     */
    public RawComputeNode(RawChip chip) 
    {
        this.rawChip = chip;
    }
    
    /**
     * Return the RawChip we are a part of.
     * @return the RawChip we are a part of.
     */
    public RawChip getRawChip() {
        return rawChip;
    }
    
    /**
     * Return the x coordinate.
     * @return the x coordinate.
     */
    public int getX() {
        return X;
    }

    /**
     * Return the y coordinate.
     * @return the y coordinate.
     */
    public int getY() {
        return Y;
    }
}

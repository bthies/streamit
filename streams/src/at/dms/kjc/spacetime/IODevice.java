package at.dms.kjc.spacetime;

import at.dms.util.Utils;

public abstract class IODevice extends ComputeNode
{
    protected int port;
    protected RawTile tile;

    public IODevice(RawChip chip, int port, RawTile tile) 
    {
	super(chip);
	
	if (port < 0 || port >= (2 * rawChip.getXSize() + 2 *rawChip.getYSize()))
	    Utils.fail("invalid port number for io device");
	
	this.port = port;
	this.tile = tile;
	if (port >= 0 && port < rawChip.getXSize()) {
	    Y = -1;
	    X = port;
	} else if (port >= rawChip.getXSize() && port < (rawChip.getXSize() + rawChip.getYSize())) {
	    X = rawChip.getXSize();
	    Y = port - rawChip.getXSize();
	} else if (port >= (rawChip.getXSize() + rawChip.getYSize()) && 
		   port < (2 * rawChip.getXSize() + rawChip.getYSize())) {
	    X = (rawChip.getXSize() - 1) - (port - (rawChip.getXSize() + rawChip.getYSize()));
	    Y = rawChip.getYSize();
	} else if (port >= (2 * rawChip.getXSize() + rawChip.getYSize()) &&
		   port < (2 * rawChip.getXSize() + 2 *rawChip.getYSize())) {
	    X = -1;
	    Y = (rawChip.getYSize() - 1) - (port - (2 * rawChip.getXSize() + rawChip.getYSize()));
	}
    }

    public int getPort() 
    {
	return port;
    }
    
    public RawTile getTile() 
    {
	return tile;
    }
}

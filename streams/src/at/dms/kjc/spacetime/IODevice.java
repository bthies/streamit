package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;

public abstract class IODevice extends ComputeNode
{
    protected int port;
    protected Vector tiles;

    public IODevice (RawChip chip, int port) 
    {
	super(chip);
	tiles = new Vector();
	if (port < 0 || port >= (2 * rawChip.getXSize() + 2 *rawChip.getYSize()))
	    Utils.fail("invalid port number for io device");
	this.port = port;
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
    

    public IODevice(RawChip chip, int port, RawTile tile) 
    {
	this(chip, port);
	tiles.add(tile);
    }

    public int getPort() 
    {
	return port;
    }
    
    public void addTile(RawTile tile) 
    {
	tiles.add(tile);
    }
    
    public RawTile[] getTiles() 
    {
	return (RawTile[])tiles.toArray(new RawTile[0]);
    }

    public RawTile getTile() 
    {
	return (RawTile)tiles.get(0);
    }
    
    public RawTile getNeighboringTile() 
    {
	if (Y == -1)
	    return rawChip.getTile(X, 0);
	if (X == -1)
	    return rawChip.getTile(0, Y);
	if (X == rawChip.getXSize())
	    return rawChip.getTile(X - 1, Y);
	else // Y == rawChip.getYSize()
	    return rawChip.getTile(X, Y -1);
    }
}

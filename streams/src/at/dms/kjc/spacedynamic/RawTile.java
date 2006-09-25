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
    private IOPort[] IOPorts;
    
    public RawTile(int x, int y, RawChip rawChip) {
        super(rawChip);
        X = x;
        Y = y;
        setTileNumber();
        computes = false;
        mapped = false;
        switches = false;
        IOPorts = new IOPort[0];
    }

    public void addIOPort(IOPort io)
    {
        assert IOPorts.length < 2 || 
            rawChip.getTotalTiles() == 1 && IOPorts.length < 5: 
            "Trying to add too many neighboring IO devices";
        IOPort[] newIOs = new IOPort[IOPorts.length + 1];
        for (int i = 0; i < IOPorts.length; i++)
            newIOs[i] = IOPorts[i];
        newIOs[newIOs.length - 1] = io;
        IOPorts = newIOs;
    }
    
    /** Function that returns the device connected to this tile
        it will die if there isn't a device or if there are two devices **/
    public LinkedList<FileReaderDevice> getAttachedFileReaders() 
    {
        LinkedList<FileReaderDevice> frs = new LinkedList<FileReaderDevice>();
        assert IOPorts.length > 0 : "Calling getAttachedDevice() on a non-border tile";
        
        for (int i = 0; i < IOPorts.length; i++) {
            for (int d = 0; d < IOPorts[i].getDevices().length; d++) {
                if (IOPorts[i].getDevices()[d] instanceof FileReaderDevice)
                    frs.add((FileReaderDevice)IOPorts[i].getDevices()[d]);
            }
        }
        
        return frs;
    }
    
        
    public String toString() {
        return "Tile["+X+", "+Y+"]";
    }
    
    public IOPort[] getIOPorts() 
    {
        return IOPorts;
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


    /** return the shortest number of hops to the edge of the chip
        don't count the src or dest, only count intermediate hops, 
        so tile on the border return 0 **/
    public int hopsToEdge() 
    {
        int ydist = Math.min(Y, (rawChip.getYSize() - 1) - Y);
        int xdist = Math.min(X, (rawChip.getXSize() - 1) - X);
                 
        return Math.min(ydist, xdist);
    }
    

    public int getTileNumber() {
        return tileNumber;
    }

    public List<RawTile> getSouthAndEastNeighbors() 
    {
        LinkedList<RawTile> neighbors = new LinkedList<RawTile>();
    
        //get east neighbor
        if (X + 1 < rawChip.getXSize()) 
            neighbors.add(rawChip.getTile(X + 1, Y));   
        //get south neighbor    
        if (Y + 1 < rawChip.getYSize())
            neighbors.add(rawChip.getTile(X, Y + 1));

        return neighbors;
    }
    

    public Vector<RawTile> getNeighborTiles() 
    {
        Vector<RawTile> ret = new Vector<RawTile>();
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

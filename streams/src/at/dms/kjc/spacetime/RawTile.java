package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.util.Utils;
import java.util.HashSet;

public class RawTile extends ComputeNode {
    private int tileNumber;
    //true if this tile has switch code
    private boolean switches;
    //true if this tile has compute code
    private boolean computes;

    private SwitchCodeStore switchCode;
    private ComputeCodeStore computeCode;

    private StreamingDram dram;
    
    private IODevice[] IODevices;

    private HashSet offChipBuffers;

    public RawTile(int x, int y, RawChip rawChip) {
	super(rawChip);
	X = x;
	Y = y;
	setTileNumber();
	computes = false;
	switches = false;
	switchCode = new SwitchCodeStore(this);
	computeCode = new ComputeCodeStore(this);
	IODevices = new IODevice[0];
	offChipBuffers = new HashSet();
    }

    public String toString() {
	return "Tile["+X+","+Y+"]";
    }

    public boolean hasIODevice() 
    {
	return (IODevices.length > 1);
    }
    
    public void addIODevice(IODevice io) 
    {
	assert IODevices.length < 2 : "Trying to add too many neighboring IO devices";
	IODevice[] newIOs = new IODevice[IODevices.length + 1];
	for (int i = 0; i < IODevices.length; i++)
	    newIOs[i] = IODevices[i];
	newIOs[newIOs.length - 1] = io;
	IODevices = newIOs;
    }

    public void addBuffer(OffChipBuffer buf) 
    {
	offChipBuffers.add(buf);
    }
    
    public HashSet getBuffers() 
    {
	return offChipBuffers;
    }
    
    public StreamingDram getDRAM() 
    {
	return dram;
    }
    
    public void setDRAM(StreamingDram sd) 
    {
	dram = sd;
    }
    
    public boolean isAttached(IODevice dev) 
    {
	for (int i = 0; i < IODevices.length; i++)
	    if (dev == IODevices[i])
		return true;
	return false;
    }
    
    public int getIOIndex(IODevice dev) 
    {
	for (int i = 0; i < IODevices.length; i++)
	    if (dev == IODevices[i])
		return i;
	assert false : "IODevice not attached to tile";
	return -1;
    }
    

    public IODevice[] getIODevices() 
    {
	return IODevices;
    }

    public IODevice getIODevice() 
    {
	assert IODevices.length == 1 : "cannot use getIODevice()";
	return IODevices[0];
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

    public SwitchCodeStore getSwitchCode() {
	return switchCode;
    }

    public ComputeCodeStore getComputeCode() {
	return computeCode;
    }

    //this is set by SwitchCodeStore
    public void setSwitches() {
	switches = true;
    }

    //this is set by ComputeCodeStore
    public void setComputes() {
	computes = true;
    }
    public void printDram() 
    {
	if (dram != null)
	    System.out.print("Tile: " + getTileNumber() + " -> port: "  + dram.getPort() + " ");
	else
	    System.out.print("Tile: " + getTileNumber() + " -> null ");
	if (IODevices.length > 0) {
	    System.out.print("(neighbors: ");
	    for (int i = 0; i < IODevices.length; i++) {
		System.out.print("port " + IODevices[i].getPort());
		if (i < IODevices.length - 1) 
		    System.out.print(", ");
	    }
	    System.out.print(")");
	}
	System.out.println();
    }
    
    public static void printDramSetup(RawChip chip) 
    {
	System.out.println("Memory Mapping:");
	if (!KjcOptions.magicdram)
	    for (int x = 0; x < chip.getXSize(); x++)
		for (int y = 0; y < chip.getYSize(); y++)
		    chip.getTile(x, y).printDram();
    }
    
}

package at.dms.kjc.spacetime;

import at.dms.util.Utils;

public class StreamingDram extends IODevice 
{
    //the upper and lower memory bound
    //the lower being inclusive and the upper being exclusive
    private Address ub;
    private Address lb;
    private Address index;
    private static Address size;

    public static void setSize(RawChip chip)
    {
	size = Address.MAX_ADDRESS.div(2*chip.getXSize() +
				       2*chip.getYSize());
    }
    
    StreamingDram(RawChip chip, int port)
    {
	super(chip, port);
	//	tile.addIODevice(this, dir);
    }

    public static StreamingDram getStrDram(Address addr, RawChip chip) 
    {
	for (int i = 0; i < chip.getDevices().length; i++) {
	    StreamingDram dram = ((StreamingDram)chip.getDevices()[i]);
	    if (Address.inRange(addr, dram.lb, dram.ub))
		return dram;
	}
	Utils.fail("Cannot find a streaming dram for this address");
	return null;
    }
    
    
    public Address getUB() 
    {
	return ub;
    }
    
    public Address getLB() 
    {
	return lb;
    }
    
    private Address getIndex() 
    {
	return index;
    }

    private void addToIndex(int size) 
    {
	index = index.add(size);
    }

    //call this after setBounds and setSize
    //a bunch of this code was ripped from four-sides.bc
    public static void setTiles(RawChip chip)
    {
	int dimY = chip.getYSize();
	int dimX = chip.getXSize();
	Address halfAddrSpace = Address.MAX_ADDRESS.div(2);
	Address addrPerTile = (halfAddrSpace.div(dimY *dimX)).mult(2);
	
	int i, j, x, y, xOff, yOff;
	Address startAddr;
		
	for (i = 0; i < dimY; i++) {
	    y = i;
	    
	    if (y < dimY/4 || y>= 3*dimY/4)
		xOff = 3*dimX/4;
	    else
		xOff = dimX/2;
	    
	    //	    startAddr = ((halfAddrSpace / 2) / dimY) * y;
	    startAddr = ((halfAddrSpace.div(2)).div(dimY)).mult(y);
	    for (j = 0; j < dimX/4; j++) {
		x = xOff + j;
		Address lb = startAddr.add(addrPerTile.mult(x - xOff));
		chip.getTile(dimX*y + x).setIODevice(StreamingDram.getStrDram(lb, chip));
		StreamingDram.getStrDram(lb, chip).addTile(chip.getTile(dimX*y + x));
		//Address ub = lb.add(addrPerTile);
		//System.out.println("Tile: " + (dimX*y + x) + 
		//		   " lb: " + lb + 
		//		   " ub: " + ub);
		//lbs[dimX*y + x] = startAddr + (x - xOff) * addrPerTile;
		//ubs[dimX*y + x] = lbs[dimX*y + x] + addrPerTile - 4;
	    }
	}
	
	for (i = 0; i < dimY; i++) {
	    y = i;
	    if (y >= 3*dimY/4 || y < dimY/4)
		xOff = dimX/4-1;
	    else
		xOff = dimX/2-1;
	    startAddr = (halfAddrSpace.div(2)).add(((halfAddrSpace.div(2)).div(dimY)).mult(y));
	    // startAddr = halfAddrSpace/2 + ((halfAddrSpace/2) / dimY) * y ;
	    for (j = 0; j < dimX/4; j++) {
		x = xOff - j;
		Address lb = startAddr.add(addrPerTile.mult(xOff - x));
		chip.getTile(dimX*y + x).setIODevice(StreamingDram.getStrDram(lb, chip));
		StreamingDram.getStrDram(lb, chip).addTile(chip.getTile(dimX*y + x));
		//Address ub = lb.add(addrPerTile);
		//System.out.println("Tile: " + (dimX*y + x) + 
		//		   " lb: " + lb + 
		//		   " ub: " + ub);
		//lbs[dimX*y + x] = startAddr + (xOff-x) * addrPerTile;
		//ubs[dimX*y + x] = lbs[dimX*y + x] + addrPerTile - 4;
	    }
	}
	
	for (i = 0; i < dimX; i++) {
	    x = i;
	    
	    if (x >= 3*dimX/4 || x < dimX/4)
		yOff = dimY/2;
	    else
		yOff = 3*dimY/4;
	    startAddr = halfAddrSpace.add(((halfAddrSpace.div(2)).div(dimX)).mult(x));
	    //startAddr = halfAddrSpace + ((halfAddrSpace/2) / dimX)  * x ;
	    for (j = 0; j < dimY/4; j++) {
		y = yOff + j;
		Address lb = startAddr.add(addrPerTile.mult(y-yOff));
		chip.getTile(dimX*y + x).setIODevice(StreamingDram.getStrDram(lb, chip));
		StreamingDram.getStrDram(lb, chip).addTile(chip.getTile(dimX*y + x));
		//Address ub = lb.add(addrPerTile);
		//System.out.println("Tile: " + (dimX*y + x) + 
		//		   " lb: " + lb + 
		//		   " ub: " + ub);
		//lbs[dimX*y + x] = startAddr + (y - yOff) * addrPerTile;
		//ubs[dimX*y + x] = lbs[dimX*y + x] + addrPerTile - 4;
	    }
	}


	for (i = 0; i < dimX; i++) {
	    x = i;
	    
	    if (x < dimX/4 || x >= 3*dimX/4)
		yOff = dimY/2-1;
	    else
		yOff = dimY/4-1;
	    startAddr = ((halfAddrSpace.div(2)).mult(3)).add(((halfAddrSpace.div(2)).div(dimX)).mult(x));
	    // startAddr = ((halfAddrSpace)/2 * 3) + ((halfAddrSpace/2) / dimX ) * x;
	    for (j=0; j < dimY/4; j++) {
		y = yOff - j;
		Address lb = startAddr.add(addrPerTile.mult(yOff-y));
		chip.getTile(dimX*y + x).setIODevice(StreamingDram.getStrDram(lb, chip));
		StreamingDram.getStrDram(lb, chip).addTile(chip.getTile(dimX*y + x));
		//Address ub = lb.add(addrPerTile);
		//System.out.println("Tile: " + (dimX*y + x) + 
		//		   " lb: " + lb + 
		//		   " ub: " + ub);
		//lbs[dimX*y + x] = startAddr + (yOff - y) * addrPerTile;
		//ubs[dimX*y + x] = lbs[dimX*y + x] + addrPerTile - 4;
	    }
	}
    }
    
    
    //set the address range for all the drams
    //call this after set size
    public static void setBounds(RawChip chip) 
    {
	Address addr = Address.ZERO;
	int i, index = 0;
	int gXSize = chip.getXSize(), gYSize = chip.getYSize();
	
	//we start counting from the upper right corner
	//and go down
	index = gXSize;
	for (i = 0; i < gYSize; i++) {
	    ((StreamingDram)chip.getDevices()[index]).lb = addr;
	    ((StreamingDram)chip.getDevices()[index]).ub = addr.add(size);		
	    index ++;
	    addr = addr.add(size);
	}	
	//now start at the upper left corner and go down
	index = (2 * gXSize + 2 * gYSize) - 1;
	for (i = 0; i < gYSize; i++) {
	    ((StreamingDram)chip.getDevices()[index]).lb = addr;
	    ((StreamingDram)chip.getDevices()[index]).ub = addr.add(size);	
	    index --;
	    addr = addr.add(size);
	}
	//now start at the lower left and go right
	index = (2 * gXSize) + gYSize - 1;
	for (i = 0; i < gYSize; i++) {
	    ((StreamingDram)chip.getDevices()[index]).lb = addr;
	    ((StreamingDram)chip.getDevices()[index]).ub = addr.add(size);	
	    index --;
	    addr = addr.add(size);
	}
	//finally start at the upper left and go right
	index = 0;
	for (i = 0; i < gYSize; i++) {
	    ((StreamingDram)chip.getDevices()[index]).lb = addr;
	    ((StreamingDram)chip.getDevices()[index]).ub = addr.add(size);		
	    index ++;
	    addr = addr.add(size);
	}	
    }
    
    public void printDramSetup() 
    {
	System.out.println("port: " + this.port +" lb: " + lb + " ub: " + ub + 
			   " size: " + size + " (" + this.X + ", " + this.Y + ")");
    }

    public static void printSetup(RawChip chip) 
    {
	System.out.println("Streaming DRAM configuration:");
	for (int i = 0; i < chip.getDevices().length; i++) {
	    ((StreamingDram)chip.getDevices()[i]).printDramSetup();
	}
    }
}

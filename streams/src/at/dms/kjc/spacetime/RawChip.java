package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.spacetime.switchIR.*;

public class RawChip {
    //the indices are x, y
    private RawTile[][] tiles;
    private IODevice[] devices;
    private int gXSize;
    private int gYSize;
    public static final int cacheLineBytes = 32;
    public static final int cacheLineWords = 8;
    
    public RawChip(int xSize, int ySize) {
	gXSize = xSize;
	gYSize = ySize;

	tiles = new RawTile[xSize][ySize];
	for (int x = 0; x < xSize; x++)
	    for (int y = 0; y < ySize; y++)
		tiles[x][y] = new RawTile(x, y, this);

	devices = new IODevice[2*gXSize + 2*gYSize];

	if (KjcOptions.magicdram) 
	    addMagicDrams();
	else    //install streaming drams
	    addStreamingDrams();
	
	printChip();
	//System.exit(1);
    }

    public RawTile getTile(int tileNumber) 
    {
	int y = tileNumber / gXSize;
	int x = tileNumber % gXSize;
	if (y >= gYSize) 
	    Utils.fail("out of bounds in getTile() of RawChip");
	return tiles[x][y];
    }
    
    public ComputeNode getComputeNode(int x, int y) 
    {
	assert !(x > gXSize || y > gYSize || x < -1 || y < -1) :
	    "out of bounds in getComputeNode() of RawChip";
	    
	if (x == gXSize || y == gYSize || x == -1 || y == -1) {
	    if (y == -1) 
		return devices[x];
	    if (x == -1)
		return devices[(gYSize - 1) + (2 * gXSize + gYSize) - y];
	    if (y == gYSize) 
		return devices[(gXSize + 1) + (gXSize + gYSize) - x];
	    if (x == gXSize) 
		return devices[y + gXSize];
	}
	return tiles[x][y];
    }
    
    
    public RawTile getTile(int x, int y) {
	assert  !(x >= gXSize || y >= gYSize) : "out of bounds in getTile() of RawChip";
	return tiles[x][y];
    }

    public int getXSize() {
	return gXSize;
    }

    public int getYSize() {
	return gYSize;
    }

    //returns "E", "N", "S", "W", or "st" if src == dst
    public String getDirection(ComputeNode from, ComputeNode to) {
	if (from == to)
	    return "st";

	
	if (from.getX() == to.getX()) {
	    int dir = from.getY() - to.getY();
	    if (dir == -1)
		return "S";
	    else if (dir == 1)
		return "N";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if (from.getY() == to.getY()) {
	    int dir = from.getX() - to.getX();
	    if (dir == -1) 
		return "E";
	    else if (dir == 1)
		return "W";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	//System.out.println(from);
	//System.out.println(((MagicDram)to).getPort());
	//System.out.println("[" + from.getX() + ", " + from.getY() + "] -> [" +
	//to.getX() + ", " + to.getY() + "]");
	
	Utils.fail("calling getDirection on non-neighbors");
	return "";
    }
    
    //Same as getDirection(ComputeNode from, ComputeNode to) except returns SwitchOPort
    public SwitchOPort getOPort(ComputeNode from, ComputeNode to) {
	System.out.println("Get Out: "+from+" "+to);
	if(from==to)
	    return SwitchOPort.CSTI;
	if(from.getX()==to.getX()) {
	    int dir=from.getY()-to.getY();
	    if(dir==-1)
		return SwitchOPort.S;
	    else if (dir == 1)
		return SwitchOPort.N;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if(from.getY()==to.getY()) {
	    int dir=from.getX()-to.getX();
	    if(dir==-1) 
		return SwitchOPort.E;
	    else if (dir == 1)
		return SwitchOPort.W;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	Utils.fail("calling getDirection on non-neighbors");
	return null;
    }

    //Same as getOPort(ComputeNode from, ComputeNode to) except use static net 2
    public SwitchOPort getOPort2(ComputeNode from, ComputeNode to) {
	System.out.println("Get Out: "+from+" "+to);
	if(from==to)
	    return SwitchOPort.CSTI2;
	if(from.getX()==to.getX()) {
	    int dir=from.getY()-to.getY();
	    if(dir==-1)
		return SwitchOPort.S2;
	    else if (dir == 1)
		return SwitchOPort.N2;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if(from.getY()==to.getY()) {
	    int dir=from.getX()-to.getX();
	    if(dir==-1) 
		return SwitchOPort.E2;
	    else if (dir == 1)
		return SwitchOPort.W2;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	Utils.fail("calling getDirection on non-neighbors");
	return null;
    }

    //Same as getDirection(ComputeNode from, ComputeNode to) except returns SwitchIPort
    public SwitchIPort getIPort(ComputeNode from, ComputeNode to) {
	//	System.out.println("Get In: "+from+" "+to);
	if(from==to)
	    return SwitchIPort.CSTO;
	if(from.getX()==to.getX()) {
	    int dir=from.getY()-to.getY();
	    if(dir==-1)
		return SwitchIPort.N;
	    else if (dir == 1)
		return SwitchIPort.S;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if(from.getY()==to.getY()) {
	    int dir=from.getX()-to.getX();
	    if(dir==-1) 
		return SwitchIPort.W;
	    else if (dir == 1)
		return SwitchIPort.E;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	Utils.fail("calling getDirection on non-neighbors");
	return null;
    }

    //Same as getIPort2(ComputeNode from, ComputeNode to) except returns static net 2 port
    public SwitchIPort getIPort2(ComputeNode from, ComputeNode to) {
	//	System.out.println("Get In: "+from+" "+to);
	if(from==to)
	    return SwitchIPort.CSTO;
	if(from.getX()==to.getX()) {
	    int dir=from.getY()-to.getY();
	    if(dir==-1)
		return SwitchIPort.N2;
	    else if (dir == 1)
		return SwitchIPort.S2;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if(from.getY()==to.getY()) {
	    int dir=from.getX()-to.getX();
	    if(dir==-1) 
		return SwitchIPort.W2;
	    else if (dir == 1)
		return SwitchIPort.E2;
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	Utils.fail("calling getDirection on non-neighbors");
	return null;
    }
    
    /**
     * Returns the numbers of tiles of the raw chip that 
     * have compute code at the given tile
     **/
    public int computingTiles() 
    {
	int sum = 0;

	for (int i = 0; i < this.gXSize; i++)
	    for (int j = 0; j < this.gYSize; j++)
		if (getTile(i, j).hasComputeCode())
		    sum++;
	return sum;
    }
    
    public int getNumDev() 
    {
	return devices.length;
    }

    public IODevice[] getDevices() 
    {
	return devices;
    }

    private void addStreamingDrams() 
    {
	int i, index = 0;
	//add the north streaming drams
	String dir = "N";	
	for (i = 0; i < this.gXSize; i++) {
	    devices [index] = new StreamingDram(this, index);						
	    index ++;
	}
	//add the east streaming drams
	dir = "E";	
	for (i = 0; i < this.gYSize; i++) {
	    devices [index] = new StreamingDram(this, index);
	    index ++;
	}	
	//add the south streaming drams
	dir = "S";	
	for (i = this.gXSize - 1; i >= 0; i--) {
	    devices [index] = new StreamingDram(this, index);
	    index ++;
	}
	//add the west streaming drams
	dir = "W";	
	for (i = this.gYSize - 1; i >= 0; i--) {
	    devices [index] = new StreamingDram(this, index);
	    index ++;
	}	
	StreamingDram.setSize(this);
	StreamingDram.setBounds(this);
	StreamingDram.setTiles(this);

    }
    
    private void addMagicDrams() 
    {
	devices[0] = new MagicDram(this, 0, tiles[0][0]);
	devices[1] = new MagicDram(this, 1, tiles[1][0]);
	devices[2] = new MagicDram(this, 2, tiles[2][0]);
	devices[3] = new MagicDram(this, 3, tiles[3][0]);
	devices[5] = new MagicDram(this, 5, tiles[1][3]);
	devices[6] = new MagicDram(this, 6, tiles[2][3]);
	devices[7] = new MagicDram(this, 7, tiles[3][3]);
	devices[9] = new MagicDram(this, 9, tiles[2][3]);
	devices[10] = new MagicDram(this, 10, tiles[1][3]);
	devices[11] = new MagicDram(this, 11, tiles[0][3]);
	devices[13] = new MagicDram(this, 13, tiles[0][2]);
	devices[14] = new MagicDram(this, 14, tiles[0][1]);
    }

    public void printChip() 
    {
	if (!KjcOptions.magicdram) {
	    StreamingDram.printSetup(this);
	    RawTile.printDramSetup(this);
	}
    }
}

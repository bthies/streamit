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
    
    StreamingDram(RawChip chip, int port, RawTile tile, 
		  String dir) 
    {
	super(chip, port, tile);
	tile.addIODevice(this, dir);
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
    
    //set the address range for all the drams    
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
	System.out.println("port: " + this.port +" lb: " + lb + " ub: " + ub + " size: " + size );
    }

    public static void printSetup(RawChip chip) 
    {
	System.out.println("Streaming DRAM configuration:");
	for (int i = 0; i < chip.getDevices().length; i++) {
	    ((StreamingDram)chip.getDevices()[i]).printDramSetup();
	}
    }
}

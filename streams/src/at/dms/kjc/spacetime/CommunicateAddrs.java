package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class CommunicateAddrs 
{
    public static String functName = "__snd_rvc_addrs__";
    private static CommunicateAddrs commAddrs;
    private RawChip chip;
    private HashMap functions;
    private HashMap fields;
    private static Random rand;

    static 
    {
	rand = new Random(17);
    }
    
    
    public static String getFields(RawTile tile) 
    {
	return ((StringBuffer)commAddrs.fields.get(tile)).toString();
    }

    public CommunicateAddrs(RawChip chip)
    {
	this.chip = chip;
	fields = new HashMap();
	functions = new HashMap();
	
	//add the StringBuffer for each tile
	for (int x = 0; x < chip.getXSize(); x++) {
	    for (int y = 0; y < chip.getYSize(); y++) {
		RawTile tile = chip.getTile(x, y);
		functions.put(tile, new StringBuffer());
		fields.put(tile, new StringBuffer());
	    }
	}
	
	Iterator buffers = OffChipBuffer.getBuffers().iterator();
	//iterate over the buffers and communicate each buffer
	//address from its declaring tile to the tile neighboring
	//the dram it is assigned to
	while (buffers.hasNext()) {
	    OffChipBuffer buffer = (OffChipBuffer)buffers.next();
	    //do nothing for redundant buffers
	    if (buffer.redundant())
		continue;
	    
	    //the dram this buffer is mapped to
	    StreamingDram dram = buffer.getDRAM();
	    //the tiles that are mapped to this dram
	    RawTile[] dramTiles = dram.getTiles();
	    //the tile we are going to allocate this buffer on
	    RawTile allocatingTile = null; 
	    //if the neighboring tile is part of dramTiles
	    //choose it for the allocating tile so we do not have
	    //to communicate the address
	    for (int i = 0; i < dramTiles.length; i++) {
		if (dramTiles[i] == dram.getNeighboringTile()) {
		    allocatingTile = dramTiles[i];
		    break;
		}   
	    }
	    
	    //we could not allocate the buffer on the neighbor 
	    //randomly pick a tile to allocate the buffer on
	    if (allocatingTile == null) {
		allocatingTile = dramTiles[rand.nextInt(dramTiles.length)];
	    }
	    
	    //set the allocating tile to have compute code
	    allocatingTile.setComputes();

	    //allocate the buffer on the allocating tile
	    ((StringBuffer)fields.get(allocatingTile)).append
		(buffer.getType().toString() + "* " + 
		 buffer.getIdent() + ";\n");
	    
	    //		 buffer.getSize().toString() + "];\n");
	    //malloc the buffer
	    ((StringBuffer)functions.get(allocatingTile)).append
		("  " + buffer.getIdent() + " = (" + buffer.getType() + 
		 "*) malloc(32 + (" + buffer.getSize().toString() + " * sizeof(" +
		 buffer.getType() + ")));\n");
	    //align the buffer
	    ((StringBuffer)functions.get(allocatingTile)).append
		("  " + buffer.getIdent() + " = ((u_int32_t)" + buffer.getIdent() +
		 ") & 0xffffff00;\n");

	    //if allocator != neighbor, create declaration of 
	    //pointer on neighbor and communicate the address
	    if (allocatingTile != dram.getNeighboringTile()) {
		dram.getNeighboringTile().setComputes();
		SpaceTimeBackend.println("Need to communicate buffer address from " + 
					 allocatingTile + " to " + dram.getNeighboringTile());
		//generate the switch code to send the address
		RawTile[] dest = {dram.getNeighboringTile()};
		SwitchCodeStore.generateSwitchCode(allocatingTile, 
						   dest, 0);
		//add the code to the owner to send the address to the
		//static net
		((StringBuffer)functions.get(allocatingTile)).append
		    ("  " + Util.staticNetworkSendPrefix(buffer.getType()) + 
		     buffer.getIdent() + 
		     Util.staticNetworkSendSuffix() + ";\n");
		
		//add declaration of pointer to neighbor
		((StringBuffer)fields.get(dram.getNeighboringTile())).append
		    (buffer.getType().toString() + "* " + 
		     buffer.getIdent() + ";\n");
		//add the code to receive the address into the pointer
		((StringBuffer)functions.get(dram.getNeighboringTile())).append
		    ("  " + Util.staticNetworkReceivePrefix() + 
		     buffer.getIdent() + 
		     Util.staticNetworkReceiveSuffix(buffer.getType()) + ";\n");
	    }
	}
    }
    
    public static String getFunction(RawTile tile) 
    {
	StringBuffer buf = new StringBuffer();

	//prepend the function name 
	buf.append("\nvoid " + functName + "() {\n");
	//append the closing } and 
	buf.append((StringBuffer)commAddrs.functions.get(tile));
	buf.append("}\n");
	return buf.toString();
    }   
    
    public static void doit(RawChip chip) 
    {
	commAddrs = new CommunicateAddrs(chip);
    }
}

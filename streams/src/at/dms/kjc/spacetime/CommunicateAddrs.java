package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import at.dms.kjc.*;

public class CommunicateAddrs 
{
    public static String functName = "__snd_rvc_addrs__";
    public static String freeFunctName = "__free_init_bufs__";
    private static CommunicateAddrs commAddrs;
    private RawChip chip;
    private HashMap functions;
    private HashMap freeFunctions;
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
	freeFunctions = new HashMap();
	
	//add the StringBuffer for each tile
	for (int x = 0; x < chip.getXSize(); x++) {
	    for (int y = 0; y < chip.getYSize(); y++) {
		RawTile tile = chip.getTile(x, y);
		functions.put(tile, new StringBuffer());
		freeFunctions.put(tile, new StringBuffer());
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

	    //allocate the init buffer on the allocating tile
	    ((StringBuffer)fields.get(allocatingTile)).append
		(buffer.getType().toString() + "* " + 
		 buffer.getIdent(true) + ";\n");

	    //allocate the steady buffer on the allocating tile
	    ((StringBuffer)fields.get(allocatingTile)).append
		(buffer.getType().toString() + "* " + 
		 buffer.getIdent(false) + ";\n");

	    //malloc the init buffer
	    ((StringBuffer)functions.get(allocatingTile)).append
		("  " + buffer.getIdent(true) + " = (" + buffer.getType() + 
		 "*) malloc(32 + (" + buffer.getSize(true).toString() + " * sizeof(" +
		 buffer.getType() + ")));\n");
	    //align the buffer
	    ((StringBuffer)functions.get(allocatingTile)).append
		("  " + buffer.getIdent(true) + " = ((u_int32_t)((char*)" + buffer.getIdent(true) +
		 ") + 31) & 0xffffffe0;\n");
	    //generate the free statement for the free function
	    ((StringBuffer)freeFunctions.get(allocatingTile)).append
		("  free(" + buffer.getIdent(true) + ");\n");

	    //malloc the steady buffer
	    ((StringBuffer)functions.get(allocatingTile)).append
		("  " + buffer.getIdent(false) + " = (" + buffer.getType() + 
		 "*) malloc(32 + (" + buffer.getSize(false).toString() + " * sizeof(" +
		 buffer.getType() + ")));\n");
	    //align the buffer
	    ((StringBuffer)functions.get(allocatingTile)).append
		("  " + buffer.getIdent(false) + " = ((u_int32_t)((char*)" + buffer.getIdent(false) +
		 ") + 31) & 0xffffffe0;\n");

	    //if allocator != neighbor, create declaration of 
	    //pointer on neighbor and communicate the address for both init and steady...
	    if (allocatingTile != dram.getNeighboringTile()) {
		dram.getNeighboringTile().setComputes();
		SpaceTimeBackend.println("Need to communicate buffer address from " + 
					 allocatingTile + " to " + dram.getNeighboringTile());
		//generate the switch code to send the addresses
		RawTile[] dest = {dram.getNeighboringTile()};
		//first for the init
		SwitchCodeStore.generateSwitchCode(allocatingTile, 
						   dest, 0);
		//now for the steady
		SwitchCodeStore.generateSwitchCode(allocatingTile, 
						   dest, 0);
		//add the code to the owner to send the address to the
		//static net for the init
		((StringBuffer)functions.get(allocatingTile)).append
		    ("  " + Util.staticNetworkSendPrefix(CStdType.Integer) + 
		     buffer.getIdent(true) + 
		     Util.staticNetworkSendSuffix() + ";\n");

		//add the code to the owner to send the address to the
		//static net for the steady
		((StringBuffer)functions.get(allocatingTile)).append
		    ("  " + Util.staticNetworkSendPrefix(CStdType.Integer) + 
		     buffer.getIdent(false) + 
		     Util.staticNetworkSendSuffix() + ";\n");

		//add declaration of pointer to neighbor (init)
		((StringBuffer)fields.get(dram.getNeighboringTile())).append
		    (buffer.getType().toString() + "* " + 
		     buffer.getIdent(true) + ";\n");

		//add declaration of pointer to neighbor (steady)
		((StringBuffer)fields.get(dram.getNeighboringTile())).append
		    (buffer.getType().toString() + "* " + 
		     buffer.getIdent(false) + ";\n");

		//add the code to receive the address into the pointer (init)
		((StringBuffer)functions.get(dram.getNeighboringTile())).append
		    ("  " + Util.staticNetworkReceivePrefix() + 
		     buffer.getIdent(true) + 
		     Util.staticNetworkReceiveSuffix(CStdType.Integer) + ";\n");
		//add the code to receive the address into the pointer (steady)
		((StringBuffer)functions.get(dram.getNeighboringTile())).append
		    ("  " + Util.staticNetworkReceivePrefix() + 
		     buffer.getIdent(false) + 
		     Util.staticNetworkReceiveSuffix(CStdType.Integer) + ";\n");
	    }
	}
    }
    
    public static String getFreeFunction(RawTile tile) 
    {
	StringBuffer buf = new StringBuffer();

	//prepend the function name 
	buf.append("\nvoid " + freeFunctName + "() {\n");
	//append the closing } and 
	buf.append((StringBuffer)commAddrs.freeFunctions.get(tile));
	buf.append("}\n");
	return buf.toString();
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

package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.Iterator;

public class CommunicateAddrs 
{
    public static String functName = "__snd_rvc_addrs__";
    private static CommunicateAddrs commAddrs;
    private RawChip chip;
    private HashMap functions;
    private HashMap fields;

    public static String getFunction(RawTile tile) 
    {
	//prepend the function name 
	
	//append the closing } and 
	
	return "";
    }
    
    public static String getFields(RawTile tile) 
    {
	return "";
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
	
	for (int x = 0; x < chip.getXSize(); x++) {
	    for (int y = 0; y < chip.getYSize(); y++) {
		RawTile owner = chip.getTile(x, y);
		
		Iterator bufs = owner.getBuffers().iterator();
		while (bufs.hasNext()) {
		    OffChipBuffer buf = (OffChipBuffer)bufs.next();
		    //check if we need to send this buffer
		    if (buf.getUsers().length == 0)
			continue;
		    
		    //add the switch code to all the tiles to communicate this address
		    owner.getSwitchCode().addCommAddrRoute(buf.getUsers());
		    //add declaration of array to owners fields
		    ((StringBuffer)fields.get(owner)).append
			(buf.getType().toString() + " " + 
			 buf.getIdent() + "[" + 
			 buf.getSize().toString() + "];\n");
		    //add the code to the owner to send the address to the
		    //static net
		    ((StringBuffer)functions.get(owner)).append
			("  " + Util.staticNetworkSendPrefix(buf.getType()) + 
			 buf.getIdent() + 
			 Util.staticNetworkSendSuffix() + ";\n");

		    //cycle thru the users and do stuff
		    for (int i = 0; i < buf.getUsers().length; i++) {
			//add declaration of pointer to all users (except owner) 
			((StringBuffer)fields.get(buf.getUsers()[i])).append
			    (buf.getType().toString() + "* " + 
			     buf.getIdent() + ";\n");
			//add the code to receive the address into the pointer
			((StringBuffer)functions.get(buf.getUsers()[i])).append
			    ("  " + Util.staticNetworkReceivePrefix() + 
			     buf.getIdent() + 
			     Util.staticNetworkReceiveSuffix(buf.getType()) + ";\n");
		    }
		}
	    }
	}
    }
    

    public static void doit(RawChip chip) 
    {
	commAddrs = new CommunicateAddrs(chip);
    }
}

package at.dms.kjc.spacetime;

import java.util.HashMap;

public class CommunicateAddrs 
{
    public static String functName = "__snd_rvc_addrs__";
    private static CommunicateAddrs commAddrs;
    private RawChip chip;
    private HashMap functions;
    private HashMap fields;

    public static String getFunction(RawTile tile) 
    {
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
	//generate everthing...
	//add the switchcode to the switch code store for init
	//add the functions and fields to the hashmaps
    }
    

    public static void doit(RawChip chip) 
    {
	commAddrs = new CommunicateAddrs(chip);
    }
}

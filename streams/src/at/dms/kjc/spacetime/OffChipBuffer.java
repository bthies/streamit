package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.util.Vector;


public class OffChipBuffer 
{
    private RawTile owner;
    private Vector users;
    private String ident;
    private static int unique_id;
    
    static 
    {
	unique_id = 0;
    }
    
    public OffChipBuffer(RawTile owner) 
    {
	this.owner = owner;
	users = new Vector();
	ident = "__buf_" + owner.getIODevice().getPort() + "_" + unique_id + "__";
	unique_id++;
    }

    public void addUser(RawTile tile) 
    {
	users.add(tile);
    }
    
    public RawTile[] getUsers() 
    {
	return (RawTile[])users.toArray(new RawTile[0]);
    }
}


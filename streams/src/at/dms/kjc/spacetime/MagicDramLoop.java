package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;

public class MagicDramLoop extends MagicDramInstruction 
{
    private int tripCount;
    private LinkedList ins;

    public MagicDramLoop()
    {
	ins = new LinkedList();
	tripCount = 0;
    }
    
    public MagicDramLoop(int tc, LinkedList insList) 
    {
	tripCount = tc;
	ins = insList;
    }

    public void addIns(MagicDramInstruction in) 
    {
	ins.add(in);
    }
    
    public void setTripCount(int tc) 
    {
	tripCount = tc;
    }
}

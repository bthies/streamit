package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import java.util.List;


//This class represents an event in the work bases simulator which is 
//implemented as an event-driven simulation
//the class is pretty self-explanatory...
public class SimulatorEvent 
{
    public String type;
    public int time;
    public FlatNode node;
    public List dests;
    public boolean isLast;
    public int itemID;
    
    public SimulatorEvent(String type, int time, FlatNode node, List dests, int itemID,
			  boolean isLast) 
    {
	this.type = type;
	this.time = time;
	this.node = node;
	this.dests = dests;
	this.isLast = isLast;
	this.itemID = itemID;
    }
}

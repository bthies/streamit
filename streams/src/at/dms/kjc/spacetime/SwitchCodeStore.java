package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.kjc.spacetime.switchIR.*;
import java.util.LinkedList;
import java.util.Iterator;

public class SwitchCodeStore {
    protected RawTile parent;
    private Vector steadySwitchIns;
    private Vector initSwitchIns;
    private Vector commAddrIns;
    private static final String LABEL_PREFIX="L_";
    private static int labelId=0;

    public SwitchCodeStore(RawTile parent) {
	this.parent = parent;
	initSwitchIns = new Vector();
	steadySwitchIns = new Vector();
	commAddrIns = new Vector();
    }

    public void appendCommAddrIns(SwitchIns ins) 
    {
	parent.setSwitches();
	commAddrIns.add(ins);
    }

    public void appendIns(SwitchIns ins, boolean init) {
	//this tile has switch code
	parent.setSwitches();
	if (init) 
	    initSwitchIns.add(ins);
	else
	    steadySwitchIns.add(ins);
    }
    
    public void appendIns(int i, SwitchIns ins, boolean init) 
    {
	//this tile has switch code
	parent.setSwitches();
	if (init) 
	    initSwitchIns.add(i, ins);
	else
	    steadySwitchIns.add(i, ins);
    }
    

    public int size(boolean init) {
	return init ? initSwitchIns.size() : steadySwitchIns.size();
    }

    public SwitchIns getIns(int i, boolean init) {
	return (init) ? (SwitchIns)initSwitchIns.get(i) : 
	    (SwitchIns)steadySwitchIns.get(i);
    }

    public Label getFreshLabel() {
	return new Label(LABEL_PREFIX+(labelId++));
    }
    
    //add route from this tile to the dest and all intermediate
    //tiles
    public void addCommAddrRoute(RawTile[] dests)
    {
	RouteIns[] ins = new RouteIns[parent.getRawChip().getXSize() *
				      parent.getRawChip().getYSize()];
	
	for (int i = 0; i < dests.length; i++) {
	    RawTile dest = dests[i];
	    
	    LinkedList route = Router.getRoute(parent, dest);
	    //append the dest again to the end of route 
	    //so we can place the item in the processor queue
	    route.add(dest);
	    Iterator it = route.iterator();
	    if (!it.hasNext()) {
		System.err.println("Warning sending item to itself");
		continue;
	    }
	    
	    RawTile prev = (RawTile)it.next();
	    RawTile current = prev;
	    RawTile next;
	    
	    while (it.hasNext()) {
		next = (RawTile)it.next();
		//create the route instruction if it does not exist
		if (ins[current.getTileNumber()] == null)
		    ins[current.getTileNumber()] = new RouteIns(current);
		//add this route to it
		//RouteIns will take care of the duplicate routes 
		ins[current.getTileNumber()].addRoute(prev, next);
		prev = current;
		current = next;
	    }
	}
	//add the non-null instructions
	for (int i = 0; i < ins.length; i++) {
	    if (ins[i] != null) {
		parent.getRawChip().getTile(i).getSwitchCode().appendCommAddrIns(ins[i]);
	    }
	}
    }
    
    public void addCommAddrRoute(RawTile dest) 
    {
	LinkedList route = Router.getRoute(parent, dest);
	//append the dest again to the end of route 
	//so we can place the item in the processor queue
	route.add(dest);
	
	Iterator it = route.iterator();
	
	if (!it.hasNext()) {
	    System.err.println("Warning sending item to itself");
	    return;
	}
	
	RawTile prev = (RawTile)it.next();
	RawTile current = prev;
	RawTile next;
	
	while (it.hasNext()) {
	    next = (RawTile)it.next();
	    //generate the route on 
	    RouteIns ins = new RouteIns(current);
	    ins.addRoute(prev, next);
	    current.getSwitchCode().appendCommAddrIns(ins);
	    
	    prev = current;
	    current = next;
	}
    }
}

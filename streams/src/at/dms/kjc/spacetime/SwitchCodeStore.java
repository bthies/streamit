package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.kjc.spacetime.switchIR.*;

public class SwitchCodeStore {
    protected RawTile parent;
    Vector steadySwitchIns;
    Vector initSwitchIns;

    public SwitchCodeStore(RawTile parent) {
	this.parent = parent;
	initSwitchIns = new Vector();
	steadySwitchIns = new Vector();
    }

    public void appendIns(SwitchIns ins, boolean init) {
	//this tile has switch code
	parent.setSwitchCode();
	if (init) 
	    initSwitchIns.add(ins);
	else
	    steadySwitchIns.add(ins);
    }

    public int size(boolean init) {
	return init ? initSwitchIns.size() : steadySwitchIns.size();
    }

    public SwitchIns getIns(int i, boolean init) {
	return (init) ? (SwitchIns)initSwitchIns.get(i) : 
	    (SwitchIns)steadySwitchIns.get(i);
    }
}

package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.kjc.spacetime.switchIR.*;

public class SwitchCodeStore {
    protected RawTile parent;
    Vector steadySwitchIns;
    Vector initSwitchIns;
    private static final String LABEL_PREFIX="L_";
    private static int labelId=0;

    public SwitchCodeStore(RawTile parent) {
	this.parent = parent;
	initSwitchIns = new Vector();
	steadySwitchIns = new Vector();
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
}

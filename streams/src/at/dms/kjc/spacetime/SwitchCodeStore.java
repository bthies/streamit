package at.dms.kjc.spacetime;

import java.util.Vector;

public class SwitchCodeStore {
    protected RawTile parent;
    Vector switchIns;

    public SwitchCodeStore(RawTile parent) {
	this.parent = parent;
	switchIns = new Vector();
    }

    public void appendIns(SwitchIns ins) {
	switchIns.add(ins);
    }

    public int size() {
	return switchIns.size();
    }

    public SwitchIns getIns(int i) {
	return (SwitchIns)switchIns.get(i);
    }
}

package at.dms.kjc.spacetime.switchIR;

import java.util.ArrayList;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.RawTile;

//SwitchProcessorIns to Route Instructions
public class FullIns extends SwitchProcessorIns {
    private SwitchProcessorIns procIns;
    private ArrayList srcs;
    private ArrayList dests;
    private RawTile tile;
    
    public FullIns(RawTile tile) {
	this.tile=tile;
	srcs=new ArrayList();
	dests=new ArrayList();
    }
    
    public FullIns(RawTile tile,SwitchProcessorIns procIns) {
	this.tile=tile;
	this.procIns=procIns;
	srcs=new ArrayList();
	dests=new ArrayList();
    }
    
    public void addRoute(SwitchSrc src,SwitchOPort dest) {
	if(src==null||dest==null) 
	    Utils.fail("Trying to add a null source or dest to route instruction");
	srcs.add(src);
	dests.add(dest);
    }

    public String toString() {
	StringBuffer out=new StringBuffer();
	if(procIns==null)
	    out.append("nop\t");
	else
	    out.append(procIns.toString()+"\t");
	SwitchSrc[] from=new SwitchIPort[srcs.size()];
	SwitchOPort[] to=new SwitchOPort[dests.size()];
	srcs.toArray(from);
	dests.toArray(to);
	if(from.length>0) {
	    out.append(from[0].toString());
	    out.append("->");
	    out.append(to[0].toString());
	    for(int i=1;i<from.length;i++) {
		out.append(", ");
		out.append(from[i].toString());
		out.append("->");
		out.append(to[i].toString());
	    }
	}
	return out.toString();
    }
}

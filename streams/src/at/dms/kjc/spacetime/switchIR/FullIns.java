package at.dms.kjc.spacetime.switchIR;

import java.util.*;
import at.dms.util.Utils;
import at.dms.kjc.spacetime.RawTile;

//SwitchProcessorIns to Route Instructions
public class FullIns extends SwitchProcessorIns {
    private SwitchProcessorIns procIns;
    private Vector srcs;
    private ArrayList dests;
    private RawTile tile;
    
    public FullIns(RawTile tile) {
	this.tile=tile;
	srcs=new Vector();
	dests=new ArrayList();
    }
    
    public FullIns(RawTile tile,SwitchProcessorIns procIns) {
	this.tile=tile;
	this.procIns=procIns;
	srcs=new Vector();
	dests=new ArrayList();
    }

    public void setProcessorIns(SwitchProcessorIns procIns) {
	this.procIns=procIns;
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
	    out.append("nop\t\t");
	else
	    out.append(procIns.toString()+"\t");
	/*Object[] from=new SwitchIPort[srcs.size()];
	  SwitchOPort[] to=new SwitchOPort[dests.size()];
	  src.toArray(from);
	  dests.toArray(to);*/
	if(srcs.size()>0) {
	    out.append("route ");
	    out.append(srcs.get(0).toString());
	    out.append("->");
	    out.append(dests.get(0).toString());
	    for(int i=1;i<srcs.size();i++) {
		out.append(", ");
		out.append(srcs.get(i).toString());
		out.append("->");
		out.append(dests.get(i).toString());
	    }
	}
	return out.toString();
    }
}






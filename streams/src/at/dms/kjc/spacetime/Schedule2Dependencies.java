package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.Set;

public class Schedule2Dependencies {
    private Schedule2Dependencies() {}
    
    public static Trace[] findDependencies(SpaceTimeSchedule sched,Trace[] traces,int row,int col) {
	Trace[][][] schedule=sched.getSchedule();
	for(int i=0;i<row;i++)
	    for(int j=0;j<col;j++) {
		Trace[] tile=schedule[i][j];
		if(tile.length>1) {
		    Trace prev=tile[0];
		    for(int k=1;k<tile.length;k++) {
			Trace next=tile[k];
			next.addDependency(prev);
			prev=next;
		    }
		}
	    }
	HashMap topNodes=new HashMap(row*col,1);
	for(int i=0;i<row;i++)
	    for(int j=0;j<col;j++) {
		Trace[] tile=schedule[i][j];
		if(tile.length>0)
		    topNodes.put(tile[0],null);
	    }
	for(int i=0;i<row;i++)
	    for(int j=0;j<col;j++) {
		Trace[] tile=schedule[i][j];
		for(int k=1;k<tile.length;k++)
		    topNodes.remove(tile[k]);
	    }
	Set outSet=topNodes.keySet();
	Trace[] out=new Trace[outSet.size()];
	outSet.toArray(out);
	return out;
    }
}

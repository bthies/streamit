package at.dms.kjc.spacetime;

public class SoftwarePipeline {
    private SoftwarePipeline() {}

    public static void pipeline(SpaceTimeSchedule sched,Trace[] traces) {
	int globalPrimePump=0;
	Trace[][][] schedule=sched.getSchedule();
	for(int i=0;i<traces.length;i++) {
	    Trace trace=traces[i];
	    int[] pos=sched.getPosition(trace);
	    Trace[] tile=schedule[pos[0]][pos[1]];
	    int num=pos[2];
	    InputTraceNode input=(InputTraceNode)trace.getHead();
	    //System.out.println(trace+" "+input.getSources().length);
	    OutputTraceNode[] srcs=input.getSources();
	    for(int j=0;j<srcs.length;j++) {
		OutputTraceNode src=srcs[j];
		int oldPrimePump=1;
		if(!((FilterTraceNode)src.getPrevious()).isPredefined()) {
		    Trace srcTrace=src.getParent();
		    boolean found=false;
		    int srcPrimePump=srcTrace.getPrimePump();
		    if(oldPrimePump==1)
			oldPrimePump=srcPrimePump;
		    else
			assert oldPrimePump==srcPrimePump:"Case Not Supported Yet";
		    if(trace.depends(srcTrace)) { //Hack to get rid of loops
			System.err.println(trace+" depends on "+srcTrace);
			found=true;
		    } else
			for(int k=num-1;k>=0;k--)
			    if(tile[k]==srcTrace)
				found=true;
		    if(found) {
			trace.setPrimePump(srcPrimePump);
		    } else {
			trace.addDependency(srcTrace);
			globalPrimePump++;
			trace.setPrimePump(srcPrimePump-1);
		    }
		}
	    }
	}
	for(int i=0;i<traces.length;i++) {
	    Trace trace=traces[i];
	    trace.setPrimePump(trace.getPrimePump()+globalPrimePump);
	}
    }
}

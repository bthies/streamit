package at.dms.kjc.spacetime;

public class PruneTopTraces {
    private PruneTopTraces() {}

    public static Trace[] prune(Trace[] traces) {
	final int len=traces.length;
	int idx=0;
	Trace[] temp=new Trace[len];
	for(int i=0;i<len;i++) {
	    Trace trace=traces[i];
	    if(trace.getDepends().length==0)
		temp[idx++]=trace;
	    /*InputTraceNode input=(InputTraceNode)trace.getHead();
	      OutputTraceNode[] srcs=input.getSources();
	      boolean ok=true;
	      for(int j=0;j<srcs.length;j++) {
	      OutputTraceNode src=srcs[j];
	      if(!((FilterTraceNode)src.getPrevious()).isPredefined())
	      ok=false;
	      }
	      if(ok)
	      temp[idx++]=trace;*/
	}
	if(idx==len)
	    return traces;
	else {
	    Trace[] out=new Trace[idx];
	    System.arraycopy(temp,0,out,0,idx);
	    return out;
	}
    }
}

package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashSet;

public class SchedulePrimePump 
{
    public static void doit(LinkedList schedule) 
    {
	HashSet alreadyScheduled = new HashSet();
	Iterator traces = schedule.iterator();
	while (traces.hasNext()) {
	    Trace trace = (Trace)traces.next();
	    incrementUpstream(trace, alreadyScheduled);
	    alreadyScheduled.add(trace);
	}
	//check graph 
	//everything one more than downstream...no
    }

    private static void incrementUpstream(Trace trace, HashSet alreadyScheduled) 
    {
	Iterator sources = trace.getHead().getSourceSet().iterator();
	while (sources.hasNext()) {
	    Trace source = ((Edge)sources.next()).getSrc().getParent();
	    if (alreadyScheduled.contains(source)) {
		assert source.getPrimePump() <= trace.getPrimePump() :
		    "Unsupported schedule created (source: " + source.toString() + ", dest: " + trace.toString();
		source.setPrimePump(trace.getPrimePump());
	    }
	    else {
		source.setPrimePump(trace.getPrimePump() + 1);
	    }
	}
    }
    
}

package at.dms.kjc.spacetime;

import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.HashSet;

public class SchedulePrimePump 
{
    public static void doit (SimpleScheduler schedule) 
    {
	Iterator traces = schedule.getSchedule().iterator();
	while (traces.hasNext()) {
	    Trace trace = (Trace)traces.next();
	    incrementUpstream(trace, schedule);
	}
	//check graph 
	checkGraph(schedule);
	//everything one more than downstream...no
    }

    private static void incrementUpstream(Trace trace, SimpleScheduler scheduler)
    {
	Iterator sources = trace.getHead().getSourceSet().iterator();
	while (sources.hasNext()) {
	    Trace source = ((Edge)sources.next()).getSrc().getParent();
	    if (scheduler.scheduledBefore(source, trace)) {
		//assert source.getPrimePump() <= trace.getPrimePump() :
		//    "Unsupported schedule created (source: " + source.toString() + ", dest: " + trace.toString();
		source.setPrimePump(trace.getPrimePump());
		incrementUpstream(source, scheduler);
	    }
	    else {
		source.setPrimePump(trace.getPrimePump() + 1);
		incrementUpstream(source, scheduler);
	    }
	}
    }
    

    private static void checkGraph(SimpleScheduler schedule) 
    {
	Iterator traces = schedule.getInitSchedule().iterator();
	
	while (traces.hasNext()) {
	    Trace trace = (Trace)traces.next();
	    //check sources
	    Iterator sources = trace.getHead().getSourceSet().iterator();
	    if (sources.hasNext()) {
		Trace src = ((Edge)sources.next()).getSrc().getParent();
		int diff = src.getPrimePump() - trace.getPrimePump();
		//if (diff != 0 && diff != 1)
		//   System.out.println("Illegal primepump schedule1 " + trace);
		assert diff == 0 || diff == 1 :
			"Illegal primepump schedule";
		while (sources.hasNext()) {
		    src = ((Edge)sources.next()).getSrc().getParent();
		    //assert all one off
		    //if (src.getPrimePump() - trace.getPrimePump() != diff)
		    //System.out.println("Illegal primepump schedule2 " + trace);
		    assert src.getPrimePump() - trace.getPrimePump() == diff :
			"Illegal primepump schedule";
		}
	    }
	    
	    //check dests    
	    Iterator dests = trace.getTail().getDestSet().iterator();
	    if (dests.hasNext()) {
		Trace dst = ((Edge)dests.next()).getDest().getParent();    
		int diff = trace.getPrimePump() - dst.getPrimePump();
		//if (diff != 0 && diff != 1)
		//    System.out.println("Illegal primepump schedule3 " + trace);
		assert diff == 0 || diff == 1 :
		    "Illegal primepump schedule";
		while (dests.hasNext()) {
		    dst =  ((Edge)dests.next()).getDest().getParent();    
		    //if (trace.getPrimePump() - dst.getPrimePump() != diff)
		    //System.out.println("Illegal primepump schedule3 " + trace);
		    assert trace.getPrimePump() - dst.getPrimePump() == diff :
		    	"Illegal primepump schedule";
		}
	    }
	    
	}
    }
}

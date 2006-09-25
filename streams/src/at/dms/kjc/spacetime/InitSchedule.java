package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import at.dms.util.Utils;

/** This class generates the init schedule, the execution order as 
    given by the flow of the graph 
**/

public class InitSchedule 
{
    public static LinkedList<Trace> getInitSchedule(Trace[] topTraces) 
    {
        LinkedList<Trace> schedule = new LinkedList<Trace>();
        HashSet<Trace> visited = new HashSet<Trace>();
        LinkedList<Trace> queue = new LinkedList<Trace>();
        for (int i = 0; i < topTraces.length; i++) {
            queue.add(topTraces[i]);
            while (!queue.isEmpty()) {      
                Trace trace = queue.removeFirst();
                if (!visited.contains(trace)) {
                    visited.add(trace);
                    Iterator dests = trace.getTail().getDestSet().iterator();
                    while (dests.hasNext()) {
                        Trace current = ((Edge)dests.next()).getDest().getParent();
                        if (!visited.contains(current)) {
                            //only add if all sources has been visited
                            Iterator sources = current.getHead().getSourceSet().iterator();
                            boolean addMe = true;
                            while (sources.hasNext()) {
                                if (!visited.contains(((Edge)sources.next()).getSrc().getParent())) {
                                    addMe = false;
                                    break;
                                }   
                            }
                            if (addMe)
                                queue.add(current);
                        }    
                    }
                    if (!trace.getHead().getNextFilter().isPredefined()) {
                        System.out.println("Adding " + trace + " to init schedule.");           
                        schedule.add(trace);
                    }
                }
            }
        }
    
        return schedule;
    }
}

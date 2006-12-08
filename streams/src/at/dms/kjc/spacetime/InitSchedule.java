package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.Slice;
import at.dms.util.Utils;

/** This class generates the init schedule, the execution order as 
    given by the flow of the graph 
**/

public class InitSchedule 
{
    public static LinkedList<Slice> getInitSchedule(Slice[] topTraces) 
    {
        LinkedList<Slice> schedule = new LinkedList<Slice>();
        HashSet<Slice> visited = new HashSet<Slice>();
        LinkedList<Slice> queue = new LinkedList<Slice>();
        for (int i = 0; i < topTraces.length; i++) {
            queue.add(topTraces[i]);
            while (!queue.isEmpty()) {      
                Slice slice = queue.removeFirst();
                if (!visited.contains(slice)) {
                    visited.add(slice);
                    Iterator dests = slice.getTail().getDestSet().iterator();
                    while (dests.hasNext()) {
                        Slice current = ((Edge)dests.next()).getDest().getParent();
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
                    if (!slice.getHead().getNextFilter().isPredefined()) {
                        System.out.println("Adding " + slice + " to init schedule.");           
                        schedule.add(slice);
                    }
                }
            }
        }
    
        return schedule;
    }
}
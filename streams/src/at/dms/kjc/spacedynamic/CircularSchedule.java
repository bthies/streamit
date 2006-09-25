/**
 * 
 */
package at.dms.kjc.spacedynamic;

import java.util.LinkedList;

import at.dms.kjc.flatgraph.FlatNode;

/**
 * A little class to encode a small circular schedule, it has an internal index that is
 * mod'ed at the end of the list when retrieving.
 * 
 * @author mgordon
 *
 */
public class CircularSchedule {
    private LinkedList<FlatNode> list;
    private int id;
    
    public CircularSchedule(FlatNode[] objs) {
        list = new LinkedList<FlatNode>();
        for (int i = 0; i < objs.length; i++)
            list.add(objs[i]);
    }
    
    /** 
     * @return The next node in the schedule.
     */
    public FlatNode next() {
        int oldID = id;
        id = (id  + 1) % list.size();
        return list.get(oldID);
    }
    
    /** 
     * @return The number of elements in this circular list.
     */
    public int size() {
        return list.size();
    }
    
    /** 
     * @return true if the next element to retreive is the head of the schedule.
     */
    public boolean isHead() {
        return id == 0;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < list.size();i++)
            buf.append(list.get(i).toString() + "\n");
        return buf.toString();
    }
}

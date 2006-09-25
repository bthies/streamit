/**
 * 
 */
package at.dms.kjc.spacedynamic;

import java.util.*;

/**
 * This class encodes a repetition of objects (values) for our generalized 
 * compression framework.
 * 
 * @author mgordon
 *
 */
public class CompressedSequence {
    /** the objects that are repeated */   
    private LinkedList<Object> seq;
    /** the repetition count */
    private int reps;
    
    public CompressedSequence(int reps) {
        this.reps = reps;
        seq = new LinkedList<Object>();
    }

    public int getReps() {
        return reps;
    }

    public Object[] getSequence() {
        return seq.toArray(new Object[0]);
    }
    
    public void addToSeq(Object addMe) {
        seq.add(addMe);
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\nCompressSeq " + hashCode() + "(Reps: " + reps + ") {");
        Iterator<Object> it = seq.iterator();
        while (it.hasNext()) {
            buf.append(it.next());
        }
        return buf.toString() + "}\n";
    }
    
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof CompressedSequence))
            return false;
        CompressedSequence comp = (CompressedSequence)arg0;
        //check the number of reps of the sequence
        if (comp.getReps() != this.getReps())
            return false;
        //check that each element of the sequences equal each other        
        Object[] mySeq = this.getSequence();
        Object[] seq0 = comp.getSequence();
        
        if (mySeq.length != seq0.length)
            return false;
        
        for (int i = 0; i < mySeq.length; i++) {
            if (!mySeq[i].equals(seq0[i]))
                return false;    
        }
                
        return true;
    }
}

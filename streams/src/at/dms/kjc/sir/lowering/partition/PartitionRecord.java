package at.dms.kjc.sir.lowering.partition;

import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.*;

/**
 * This is just a structure for recording what has been assigned to a
 * given partition.
 */
public class PartitionRecord {
    /**
     * List of ORIGINAL SIROperator's that are assigned to this
     * partition.  Note that some SIROperators can be split across
     * multiple partitions in the case of fission.
     */
    private LinkedList contents;
    /**
     * The amount of work in this partition (might not be derivable
     * from <contents> because of fission transforms, etc.)
     */
    private int work;

    public PartitionRecord() {
	this.contents = new LinkedList();
	this.work = 0;
    }

    public int getWork() {
	return work;
    }

    /**
     * Add operator <op> with work amount <k> to this partition.
     * Requires that this does not already contain <op>.
     */
    public void add(SIROperator op, int k) {
	assert !this.contains(op);
	contents.add(op);
	work += k;
    }

    /**
     * Add container <cont> to this. Requires that this does not
     * already contain <cont>.
     */
    public void add(SIRContainer cont) {
	assert !this.contains(cont);
	add(cont, 0);
    }

    /**
     * Returns the i'th contents of this
     */
    public SIROperator get(int i) {
	return (SIROperator)contents.get(i);
    }

    /**
     * Returns number of operators in this.
     */
    public int size() {
	return contents.size();
    }

    /**
     * Returns whether or not this partition contains <op>.
     */
    public boolean contains(SIROperator op) {
	return contents.contains(op);
    }

    /**
     * Given that <partitions> is a list of PartitionRecords, returns
     * a hashmap in which each SIROperator that's in one of the
     * partitions is mapped to a STRING representing the list of
     * partitions it's assigned to.
     */
    public static HashMap asStringMap(LinkedList partitions) {
	HashMap result = new HashMap();
	for (int i=0; i<partitions.size(); i++) {
	    PartitionRecord pr = (PartitionRecord)partitions.get(i);
	    for (int j=0; j<pr.size(); j++) {
		if (result.containsKey(pr.get(j))) {
		    result.put(pr.get(j), result.get(pr.get(j))+","+i);
		} else {
		    result.put(pr.get(j), ""+i);
		}
	    }
	}
	return result;
    }

    /**
     * Given that <partitions> is a list of PartitionRecords, returns
     * a hashmap in which each SIROperator that's in one of the
     * partitions is mapped to an Integer representing the list of
     * partitions it's assigned to.  
     *
     * Unlike asStringMap, requires that each operator is mapped to
     * only one partition.
     */
    public static HashMap asIntegerMap(LinkedList partitions) {
	HashMap result = new HashMap();
	for (int i=0; i<partitions.size(); i++) {
	    PartitionRecord pr = (PartitionRecord)partitions.get(i);
	    for (int j=0; j<pr.size(); j++) {
		if (result.containsKey(pr.get(j))) {
		    Utils.fail("This operator is mapped to two partitions in asIntegerMap: " + pr.get(j));
		} else {
		    result.put(pr.get(j), new Integer(i));
		}
	    }
	}
	return result;
    }
}


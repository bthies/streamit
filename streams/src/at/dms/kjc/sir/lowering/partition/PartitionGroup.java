package at.dms.kjc.sir.lowering.partition;

import at.dms.util.Utils;
import java.util.*;

/**
 * This represents a partitioning of the children of a single SIR
 * Container.  A PartitionGroup is immutable.
 */
public class PartitionGroup {

    /**
     * Representation of the partitions as the number of children that
     * fall in each successive partition.  For instance, if it holds
     *
     *  { 1, 2, 3, 1}
     *
     * Then it means that child 0 is in its own partition, children
     * 1-2 are in another partition, children 3-5 are in another, and
     * child 4 is in another.
     */
    private int[] partitions;
    
    private PartitionGroup(int[] partitions) {
	this.partitions = partitions;
	// make sure that all weights in <partitions> are positive
	for (int i=0; i<partitions.length; i++) {
	    assert partitions[i]>=1:
                "Trying to create partitioning with zero-element partition.";
	}
    }

    /**
     * Creates a partition group where <partitions> is formatted as
     * described above.  Will end up sharing <partitions> in the
     * representation of the new object.
     */
    public static PartitionGroup createFromArray(int[] partitions) {
	return new PartitionGroup(partitions);
    }

    /**
     * Create a uniform partition with one child in each.  <n> is number of children.
     */
    public static PartitionGroup createUniformPartition(int n) {
	int[] partitions = new int[n];
	for (int i=0; i<n; i++) {
	    partitions[i] = 1;
	}
	return new PartitionGroup(partitions);
    }

    /**
     * Given a list of children and a mapping from each child to an
     * Integer denoting the partition, creates a partition group where
     * children mapped to the same integer are allocated to the same
     * partition.  Treats -1 as an indicator that a child should not
     * be fused with any neighbors.  Requires that all of the children
     * are keys in <map>.
     */
    public static PartitionGroup createFromAssignments(List children, HashMap map) {
	List resultList = new LinkedList();
	int pos = 0;
	while (pos<children.size()) {
	    int count = 0;
	    int cur = ((Integer)map.get(children.get(pos))).intValue();
	    do {
		pos++;
		count++;
	    } while (pos<children.size() && 
		     ((Integer)map.get(children.get(pos))).intValue()==cur && 
		     // don't conglomerate -1 children, as they are
		     // containers with differing tile content
		     cur!=-1);
	    resultList.add(new Integer(count));
	}
	// copy results into int array
	int[] result = new int[resultList.size()];
	for (int i=0; i<result.length; i++) {
	    result[i] = ((Integer)resultList.get(i)).intValue();
	}
	return PartitionGroup.createFromArray(result);
    }

    /**
     * Returns number of partitions in this.
     */
    public int size() {
	return partitions.length;
    }

    /**
     * Returns the size of the i'th partition in this.
     */
    public int get(int i) {
	return partitions[i];
    }

    /**
     * Returns the index of the first child in the k'th partition.
     */
    public int getFirst(int k) {
	int sum = 0;
	for (int i=0; i<k; i++) {
	    sum += partitions[i];
	}
	return sum;
    }

    /**
     * Returns the index of the last child in the k'th partition.
     */
    public int getLast(int k) {
	int sum = 0;
	for (int i=0; i<k+1; i++) {
	    sum += partitions[i];
	}
	return sum-1;
    }
    
    /**
     * Returns which partition the k'th child belongs to.  Requires
     * k<numChildren()
     */
    public int getPartForChild(int k) {
	int sum = 0; 
	for (int i=0; i<partitions.length; i++) {
	    if (sum>=k) {
		return i;
	    } else {
		sum += partitions[i];
	    }
	}
	return partitions.length-1;
    }

    /**
     * Returns the number of children accounted for in this
     * partitioning.
     */
    public int getNumChildren() {
	int sum = 0; 
	for (int i=0; i<partitions.length; i++) {
	    sum += partitions[i];
	}
	return sum;
    }

    public String toString () {
	StringBuffer result = new StringBuffer("PartitionGroup with sizes={");
	for (int i=0; i<partitions.length; i++) {
	    result.append(partitions[i]);
	    if (i==partitions.length-1) {
		result.append("}");
	    } else {
		result.append(",");
	    }
	}
	return result.toString();
    }

}

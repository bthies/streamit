package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * A set of Integers corresponding to possible latencies for message
 * delivery.
 */
public class SIRLatencySet extends SIRLatency {
    /**
     * The entries in the set.
     */
    private TreeSet entries;

    public SIRLatencySet() {
	this.entries = new TreeSet();
    }

    // The following methods just mirror those of <entries>.  More can
    // be added if desired.
    
    public boolean add(Integer x) {
	return entries.add(x);
    }

    public Integer first() {
	return (Integer)entries.first();
    }

    public boolean isEmpty() {
	return entries.isEmpty();
    }

    public Iterator iterator() {
	return entries.iterator();
    }

    public Integer last() {
	return (Integer)entries.last();
    }

    public boolean remove(Integer x) {
	return entries.remove(x);
    }

    public int size() {
	return entries.size();
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p) {
	if (p instanceof SLIRVisitor) {
	    ((SLIRVisitor)p).visitLatencySet(this);
	} else {
	    at.dms.util.Utils.fail("Use SLIR visitor to visit an SIR node.");
	}
    }
}

package at.dms.kjc.sir;

import java.util.TreeSet;
import java.util.Iterator;

/**
 * A set of SIRLatencyMax's.
 */
class SIRLatencySet extends SIRLatency {
    /**
     * The entries in the set.
     */
    private TreeSet entries;

    public SIRLatencySet() {
	this.entries = new TreeSet();
    }

    // The following methods just mirror those of <entries>.  More can
    // be added if desired.
    
    public boolean add(SIRLatencyMax x) {
	return entries.add(x);
    }

    public boolean contains(SIRLatencyMax x) {
	return entries.contains(x);
    }

    public SIRLatencyMax first() {
	return (SIRLatencyMax)entries.first();
    }

    public boolean isEmpty() {
	return entries.isEmpty();
    }

    public Iterator iterator() {
	return entries.iterator();
    }

    public SIRLatency last() {
	return (SIRLatency)entries.last();
    }

    public boolean remove(SIRLatency x) {
	return entries.remove(x);
    }

    public int size() {
	return entries.size();
    }
}

package at.dms.kjc.sir.lowering.partition;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;

/**
 * A wrapper for a linked list to save ourself a lot of casting with
 * work entries.
 */
class WorkList extends java.util.LinkedList {

    public WorkList(Collection c) {
	super(c);
    }

    /**
     * Gets work at position <i>.
     */
    public int getWork(int i) {
	return ((WorkInfo)((Map.Entry)super.get(i)).getValue()).totalWork();
    }

    /**
     * Gets filter at position <i>.
     */
    public SIRFilter getFilter(int i) {
	return (SIRFilter)((Map.Entry)super.get(i)).getKey();
    }

    /**
     * Gets container at position <i>.
     */
    public SIRContainer getContainer(int i) {
	return (SIRContainer)((Map.Entry)super.get(i)).getKey();
    }

}

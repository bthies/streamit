package at.dms.kjc.iterator;

import at.dms.kjc.sir.*;
import streamit.scheduler2.iriter.*;

public class SIRPhasedFilterIter extends SIRIterator implements FilterIter 
{
    /**
     * The object this iterator points at.
     */
    private SIRPhasedFilter obj;
    
    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRPhasedFilterIter(SIRPhasedFilter obj) {
	this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRPhasedFilterIter(SIRPhasedFilter obj, SIRIterator parent, int pos) {
	super(parent, pos);
	this.obj = obj;
    }

    public FilterIter isFilter() {
	return this;
    }

    /**
     * Return the stream pointed to by this.
     */
    public SIRStream getStream() {
	checkValidity();
	return obj;
    }

    // At this point we realize that either the FilterIter model is
    // wrong, or it's sufficiently general, just with oddly named
    // functions.
    public int getNumInitStages() {
        // Requires analysis of the work function; in particular, if
        // work doesn't return, then the init stages are before the
        // not-terminating loop.
        return 0;
    }

    public int getInitPeekStage(int phase) {
        return -1;
    }
    
    public int getInitPushStage(int phase) {
        return -1;
    }

    public int getInitPopStage(int phase) {
        return -1;
    }

    public Object getInitFunctionStage(int phase) {
        return null;
    }

    // In particular, everything from here on down we had better be able
    // to straightforwardly implement.
    public int getNumWorkPhases() {
        if (obj.getPhases() == null) return 0;
        return obj.getPhases().length;
    }

    public int getPeekPhase(int phase) {
        return obj.getPhases()[phase].getPeekInt();
    }
    
    public int getPopPhase(int phase) {
        return obj.getPhases()[phase].getPopInt();
    }

    public int getPushPhase(int phase) {
        return obj.getPhases()[phase].getPushInt();
    }
    
    public Object getWorkFunctionPhase(int phase) {
        return obj.getPhases()[phase].getWork();
    }
    
    public void accept(StreamVisitor v) {
        v.visitPhasedFilter(obj, this);
    }

    public Iterator getUnspecializedIter() {
        return this;
    }
}

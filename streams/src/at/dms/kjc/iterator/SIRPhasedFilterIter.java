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
    SIRPhasedFilterIter(IterFactory _factory, SIRPhasedFilter obj) {
	super(_factory);
	this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRPhasedFilterIter(IterFactory _factory, SIRPhasedFilter obj, SIRIterator parent, int pos) {
	super(_factory, parent, pos);
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

    public int getNumInitStages() {
    	if (obj.getInitPhases() == null) return 0;
    	return obj.getInitPhases().length;
    }

    public int getInitPeekStage(int phase) {
    	return obj.getInitPhases()[phase].getPeekInt();
    }
    
    public int getInitPushStage(int phase) {
		return obj.getInitPhases()[phase].getPushInt();
    }

    public int getInitPopStage(int phase) {
		return obj.getInitPhases()[phase].getPopInt();
    }

    public Object getInitFunctionStage(int phase) {
        return obj.getInitPhases()[phase].getWork();
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

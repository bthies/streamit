package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import streamit.scheduler.iriter.*;

public class SIRFilterIter extends SIRIterator implements FilterIter {

    /**
     * The object this iterator points at.
     */
    private SIRFilter obj;

    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRFilterIter(SIRFilter obj) {
	this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRFilterIter(SIRFilter obj, SIRIterator parent, int pos) {
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

    public int getNumInitStages () {
	if (obj instanceof SIRTwoStageFilter) {
	    return 1;
	} else {
	    return 0;
	}
    }

    public int getInitPeekStage (int phase) {
	if (obj instanceof SIRTwoStageFilter) {
	    return ((SIRTwoStageFilter)obj).getInitPeek();
	} else {
	    return -1;
	}
    }

    public int getInitPopStage (int phase) {
	if (obj instanceof SIRTwoStageFilter) {
	    return ((SIRTwoStageFilter)obj).getInitPop();
	} else {
	    return -1;
	}
    }

    public int getInitPushStage (int phase) {
	if (obj instanceof SIRTwoStageFilter) {
	    return ((SIRTwoStageFilter)obj).getInitPush();
	} else {
	    return -1;
	}
    }

    public Object getInitFunctionStage (int phase) {
	return obj.getInit();
    }
    
    public int getNumWorkPhases () {
	return 1;
    }

    public int getPeekPhase (int phase) {
	return obj.getPeekInt();
    }

    public int getPopPhase (int phase) {
	return obj.getPopInt();
    }

    public int getPushPhase (int phase) {
	return obj.getPopInt();
    }

    public Object getWorkFunctionPhase (int phase) {
	return obj.getWork();
    }

    public void accept(StreamVisitor v) {
	v.visitFilter(obj, this);
    }

    /**
     * This function is needed by the scheduler, but isn't useful from
     * the compiler.
     */
    public Iterator getUnspecializedIter() {
	return this;
    }
 }

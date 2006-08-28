package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import streamit.scheduler2.iriter.*;

/**
 * IterFactory uses this for SIRFilter.
 *
 * Includes extra methods as appropriate.
 */

public class SIRFilterIter extends SIRIterator implements FilterIter {

    /**
     * The object this iterator points at.
     */
    private SIRFilter obj;

    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRFilterIter(IterFactory _factory, SIRFilter obj) {
        super(_factory);
        this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRFilterIter(IterFactory _factory, SIRFilter obj, SIRIterator parent, int pos) {
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

    public int getNumInitStages () {
        if (obj instanceof SIRTwoStageFilter) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getInitPeekStage (int phase) {
        if (obj instanceof SIRTwoStageFilter) {
            return ((SIRTwoStageFilter)obj).getInitPeekInt();
        } else {
            return -1;
        }
    }

    public int getInitPopStage (int phase) {
        if (obj instanceof SIRTwoStageFilter) {
            return ((SIRTwoStageFilter)obj).getInitPopInt();
        } else {
            return -1;
        }
    }

    public int getInitPushStage (int phase) {
        if (obj instanceof SIRTwoStageFilter) {
            return ((SIRTwoStageFilter)obj).getInitPushInt();
        } else {
            return -1;
        }
    }

    public Object getInitFunctionStage (int phase) {
        if (obj instanceof SIRTwoStageFilter) {
            return ((SIRTwoStageFilter)obj).getInitWork();
        } else {
            return null;
        }
    }
    
    public int getNumWorkPhases () {
        return 1;
    }

    /** Get number of peeks for phase. Has never handled the phase input. Now returns estimate if number not available. */
    public int getPeekPhase (int phase) {
        return obj.getPeekInt();
        //return obj.getPeekEstimate(); // getPeekInt();
   }

    /** Get number of peeks for phase. Has never handled the phase input. Now returns estimate if number not available. */
    public int getPopPhase (int phase) {
        return obj.getPopInt();
        //return obj.getPopEstimate(); // getPopInt();
    }

    /** Get number of peeks for phase. Has never handled the phase input. Now returns estimate if number not available. */
    public int getPushPhase (int phase) {
        return obj.getPushInt();
        //return obj.getPushEstimate(); // getPushInt();
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

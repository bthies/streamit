package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * A two-stage filter is a filter that has two work phases.  The first
 * phase is for the initial execution of the filter, in which initWork
 * is called and the I/O rates are initPop, initPush, and initPeek.
 * On all subsequent invocations, the filter behaves as a normal
 * steady-state filter with the usual work function and I/O rates.
 */
public class SIRTwoStageFilter extends SIRFilter {
    /**
     * The number of items that are peeked on the inital invocation
     */
    private int initPeek;
    /**
     * The number of items that are popped on the initial invocation.
     */
    private int initPop;
    /**
     * The number of items that are pushed on the initial invocation.
     */
    private int initPush;
    /**
     * The initial work function.
     */
    private JMethodDeclaration initWork;

    public SIRTwoStageFilter() {
	super();
    }

    public SIRTwoStageFilter(SIRContainer parent,
			     String ident,
			     JFieldDeclaration[] fields, 
			     JMethodDeclaration[] methods, 
			     JExpression peek,
			     JExpression pop, 
			     JExpression push, 
			     JMethodDeclaration work, 
			     int initPeek,
			     int initPop,
			     int initPush,
			     JMethodDeclaration initWork, 
			     CType inputType, 
			     CType outputType) {
	super(parent, ident, fields, methods, 
	      peek, pop, push, work,
	      inputType, outputType);
	this.initPeek = initPeek;
	this.initPush = initPush;
	this.initPop = initPop;
	// this ensures that <initWork>> is in our methods array, too
	setInitWork(initWork);
	checkRep();
    }

    /**
     * Copies the state of filter <other> into this.  Fields that are
     * objects will be shared instead of cloned.
     */
    public void copyState(SIRFilter other) {
	super.copyState(other);
	if (other instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter twoStage = (SIRTwoStageFilter)other;
	    this.initPeek = twoStage.initPeek;
	    this.initPush = twoStage.initPush;
	    this.initPop = twoStage.initPop;
	    this.initWork = twoStage.initWork;
	    checkRep();
	}
    }

    /**
     * Checks the representation of this to make sure it's consistent
     * with our assumptions.
     */
    private void checkRep() {
	// we think the peek-pop difference should be the same in the
	// initial and steady states (our simulation routine with the
	// scheduler makes this assumption).
	Utils.assert(initPeek-initPop==getPeekInt()-getPopInt());
	// we need an init work function to be a two-stage filter
	Utils.assert(initWork!=null);
    }

    /**
     * Sets the work function.  Can be made public if there's ever a
     * need for it, but right now there isn't.
     */
    private void setInitWork (JMethodDeclaration newWork) {
	addReplacementMethod(newWork, this.initWork);
	this.initWork = newWork;
	checkRep();
    }

    public int getInitPush() {
	return this.initPush;
    }

    public int getInitPeek() {
	return this.initPeek;
    }

    public int getInitPop() {
	return this.initPop;
    }

    public JMethodDeclaration getInitWork() {
	Utils.assert(initWork!=null);
	return this.initWork;
    }

}



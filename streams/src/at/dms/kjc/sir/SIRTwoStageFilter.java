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
 *
 * By way of implementation, this is a special case of a generalized
 * <code>SIRPhasedFilter</code> that has exactly one phase in each
 * of the init and work stages.  For largely historical reasons,
 * this is actually derived from <code>SIRFilter</code>; it would
 * be "more correct" to actually extend <code>SIRPhasedFilter</code>
 * directly, but making that change involves making all of the rest
 * of the compiler aware of phases.  In some places this is easier
 * than in others; big changes show up in the backends.
 *
 * @version $Id: SIRTwoStageFilter.java,v 1.5 2003-05-16 20:47:03 dmaze Exp $
 */
public class SIRTwoStageFilter extends SIRFilter {
    /* Internal invariant: the init and work phases arrays each have
     * exactly one element.  This means we need to set up initPhases
     * and phases even where we wouldn't otherwise. */

    public SIRTwoStageFilter()
    {
        this(null);
    }

    public SIRTwoStageFilter(String ident) 
    {
        super(ident);
        // setPhases(new SIRWorkFunction[1]);
        // getPhases()[0] = new SIRWorkFunction();
        setInitPhases(new SIRWorkFunction[1]);
        getInitPhases()[0] = new SIRWorkFunction();
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
        super(parent, ident, fields, methods, peek, pop, push, work,
              inputType, outputType);
        // super(parent, ident, fields, methods,
        //       new SIRWorkFunction[1], // initPhases,
        //       new SIRWorkFunction[1], // phases
        //       null, inputType, outputType);
        // Create a single phase for each stage.
        setInitPhases(new SIRWorkFunction[1]);
        getInitPhases()[0] = new SIRWorkFunction(initPeek, initPop,
                                                 initPush, initWork);
        // getPhases()[0] = new SIRWorkFunction(peek, pop, push, work);
	checkRep();
    }

    /**
     * Checks the representation of this to make sure it's consistent
     * with our assumptions.
     */
    private void checkRep() {
	// we think the peek-pop difference should be the same in the
	// initial and steady states (our simulation routine with the
	// scheduler makes this assumption).
	Utils.assert(getInitPeek()-getInitPop()==
                     getPeekInt()-getPopInt(),
		     "\nFor Two Stage Filters, initPeek-initPop must equal peek-pop" +
		     "\ninitPeek=" + getInitPeek() + 
		     "\ninitPop=" + getInitPop() + 
		     "\nPeek=" + getPeekInt() + 
		     "\nPop=" + getPopInt());
    }

    /**
     * Sets the work function for the initialization stage.
     */
    public void setInitWork (JMethodDeclaration newWork) {
	addReplacementMethod(newWork, getInitWork());
        getInitPhases()[0].setWork(newWork);
	checkRep();
    }

    public int getInitPush() {
        return getInitPhases()[0].getPushInt();
    }

    public int getInitPeek() {
        return getInitPhases()[0].getPeekInt();
    }

    public int getInitPop() {
        return getInitPhases()[0].getPopInt();
    }

    public void setInitPush(int i) {
        getInitPhases()[0].setPush(i);
    }

    public void setInitPeek(int i) {
        getInitPhases()[0].setPeek(i);
    }

    public void setInitPop(int i) {
        getInitPhases()[0].setPop(i);
    }

    public JMethodDeclaration getInitWork() {
        return getInitPhases()[0].getWork();
    }

    /* Overridden from SIRPhasedFilter: */
    public void setInitPhases(SIRWorkFunction[] initPhases) 
    {
        if (initPhases.length != 1)
            throw new UnsupportedOperationException
                ("SIRTwoStageFilters have exactly one init phase");
        super.setInitPhases(initPhases);
    }

}



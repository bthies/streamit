package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.util.*;
import java.util.HashMap;

/**
 * This represents a basic StreamIt filter.  In this case, a filter
 * is a specialized phased filter that has only a single stage
 * (no prework function or phases), and only a single phase in its
 * work stage.
 *
 * @version $Id: SIRFilter.java,v 1.32 2003-12-02 20:47:05 thies Exp $
 */
public class SIRFilter extends SIRPhasedFilter implements Cloneable {
    /* Internal invariant: the init phases array is null or has zero
     * elements, the work phases array has exactly one element.
     * This means we need to set up initPhases and phases even where
     * we wouldn't otherwise. */

    public SIRFilter() {
	this(null);
        setPhases(new SIRWorkFunction[1]);
        getPhases()[0] = new SIRWorkFunction();
    }

    public SIRFilter(String ident) {
        super(ident);
        setPhases(new SIRWorkFunction[1]);
        getPhases()[0] = new SIRWorkFunction();
    }
    
    public SIRFilter(SIRContainer parent,
		     String ident,
		     JFieldDeclaration[] fields, 
		     JMethodDeclaration[] methods, 
		     JExpression peek, JExpression pop, JExpression push, 
		     JMethodDeclaration work, 
		     CType inputType, 
		     CType outputType) {
        super(parent, ident, fields, methods,
              new SIRWorkFunction[0], // initPhases
              new SIRWorkFunction[1], // phases
              null, inputType, outputType);
        // Create a single phase corresponding to the work function.
        getPhases()[0] = new SIRWorkFunction(peek, pop, push, work);
        // Confirm that the work function is in the methods array.
        if (work != null)
            addReplacementMethod(work, work);
	// check for void type if we have 0 inputs or outputs
	Utils.assert(this instanceof SIRTwoStageFilter || 
		     ((!(peek instanceof JIntLiteral) || ((JIntLiteral)peek).intValue()>0) || inputType==CStdType.Void),
		     "Filter " + this + " declares peek rate of 0 but has input type of " + inputType + " which should be Void instead.");
	Utils.assert(this instanceof SIRTwoStageFilter || 
		     ((!(push instanceof JIntLiteral) || ((JIntLiteral)push).intValue()>0) || outputType==CStdType.Void),
		     "Filter " + this + " declares push rate of 0 but has output type of " + outputType + " which should be Void instead.");
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitFilter(this,
			     fields,
			     methods,
			     init,
			     getPhases()[0].getWork(),
			     getInputType(), getOutputType());
    }

    public void setPeek(JExpression p) {
        getPhases()[0].setPeek(p);
    }

    public void setPop(JExpression p) {
        getPhases()[0].setPop(p);
    }
    public void setPush(JExpression p) {
        getPhases()[0].setPush(p);
    }

    public void setPeek(int p) {
        setPeek(new JIntLiteral(p));
    }

    public void setPush(int p) {
        setPush(new JIntLiteral(p));
    }

    public void setPop(int p) {
        setPop(new JIntLiteral(p));
    }

    public JExpression getPush() {
        return getPhases()[0].getPush();
    }

    public JExpression getPeek() {
        return getPhases()[0].getPeek();
    }

    public JExpression getPop() {
        return getPhases()[0].getPop();
    }

    public int getPushForSchedule(HashMap[] counts) {
	Utils.assert(counts[1].containsKey(this),
		     "Execution count doesn't contain " + this);
	int steadyCount = ((int[])counts[1].get(this))[0];
	return steadyCount * getPushInt();
    }

    public int getPopForSchedule(HashMap[] counts) {
	Utils.assert(counts[1].containsKey(this),
		     "Execution count doesn't contain " + this);
	int steadyCount = ((int[])counts[1].get(this))[0];
	return steadyCount * getPopInt();
    }

    /**
     * Returns how many items are popped.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPop.
     */
    public int getPopInt() {
        return getPhases()[0].getPopInt();
    }

    /**
     * Returns how many items are peeked.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPeek.
     */
    public int getPeekInt() {
        return getPhases()[0].getPeekInt();
    }

    /**
     * Returns how many items are pushed.This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPush.
     */
    public int getPushInt() {
        return getPhases()[0].getPushInt();
    }

    /* Overridden from SIRStream: */
    public JMethodDeclaration getWork() 
    {
        return getPhases()[0].getWork();
    }
    
    /* Overridden from SIRStream: */
    public void setWork(JMethodDeclaration work)
    {
        addReplacementMethod(work, getWork());
        getPhases()[0].setWork(work);
    }

    /* Overridden from SIRPhasedFilter: */
    /* This seems like a good idea, but it breaks SIRTwoStageFilter
     * being derived from SIRFilter.  Changing that is a Big Change,
     * since it involves making the entire world phase-aware.  Not
     * that this is actually a bad thing, but...
    public void setInitPhases(SIRWorkFunction[] initPhases) 
    {
        throw new UnsupportedOperationException
            ("SIRFilters can't have init phases");
    }
    */

    /* Overridden from SIRPhasedFilter: */
    public void setPhases(SIRWorkFunction[] phases)
    {
        if (phases.length != 1)
            throw new UnsupportedOperationException
                ("SIRFilters have exactly one work phase");
        super.setPhases(phases);
    }

    public String toString() {
	return "SIRFilter name=" + getName();
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRFilter other = new at.dms.kjc.sir.SIRFilter();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRFilter other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}



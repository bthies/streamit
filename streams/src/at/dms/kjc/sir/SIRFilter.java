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
 * @version $Id: SIRFilter.java,v 1.29 2003-05-16 18:37:55 dmaze Exp $
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

    /* Overridden from SIRPhasedFilter: */
    public void setInitPhases(SIRWorkFunction[] initPhases) 
    {
        throw new UnsupportedOperationException
            ("SIRFilters can't have init phases");
    }

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
}



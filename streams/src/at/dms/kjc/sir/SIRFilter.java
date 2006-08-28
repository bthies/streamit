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
 * @version $Id: SIRFilter.java,v 1.41 2006-08-28 02:23:30 dimock Exp $
 */
public class SIRFilter extends SIRPhasedFilter implements Cloneable {
    /* Internal invariant: the init phases array is null or has zero
     * elements, the work phases array has exactly one element.
     * This means we need to set up initPhases and phases even where
     * we wouldn't otherwise. */

    public SIRFilter() {
        this(null);
    }

    public SIRFilter(String ident) {
        super(ident);
        setPhases(new JMethodDeclaration[1]);
        // placeholder for I/O rates
        getPhases()[0] = new JMethodDeclaration("SIRFilter placeholder");
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
              new JMethodDeclaration[0], // initPhases
              new JMethodDeclaration[1], // phases
              inputType, outputType);
        // Create a single phase corresponding to the work function.
        // if work function is null, make a dummy one just to hold I/O rates
        if (work == null) {
            work = new JMethodDeclaration("Placeholder method for SIRFilter");
        }
        getPhases()[0] = work;
        work.setPeek(peek);
        work.setPop(pop);
        work.setPush(push);
        // Confirm that the work function is in the methods array.
        if (work != null)
            addReplacementMethod(work, work);
        // check for void type if we have 0 inputs or outputs
        assert this instanceof SIRTwoStageFilter || 
            ((!(peek instanceof JIntLiteral) ||
              ((JIntLiteral)peek).intValue()>0) ||
             inputType==CStdType.Void):
            "Filter " + this +
            " declares peek rate of 0 but has input type of " +
            inputType + " which should be Void instead.";
        assert this instanceof SIRTwoStageFilter || 
            ((!(push instanceof JIntLiteral) ||
              ((JIntLiteral)push).intValue()>0) ||
             outputType==CStdType.Void):
            "Filter " + this +
            " declares push rate of 0 but has output type of " +
            outputType + " which should be Void instead.";
    }
    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
        return v.visitFilter(this,
                             fields,
                             methods,
                             init,
                             getPhases()[0],
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
        assert counts[1].containsKey(this):
            "Execution count doesn't contain " + this;
        int steadyCount = ((int[])counts[1].get(this))[0];
        return steadyCount * getPushInt();
    }

    public int getPopForSchedule(HashMap[] counts) {
        assert counts[1].containsKey(this):
            "Execution count doesn't contain " + this;
        int steadyCount = ((int[])counts[1].get(this))[0];
        return steadyCount * getPopInt();
    }
    
    /** Get estimated number of pops for size / work estimation.  Handles dynamic rates. */
    public int getPopEstimate() {
        return getEstimate(getPop());
    }

    /** Get estimated number of pushes for size / work estimation.  Handles dynamic rates. */
    public int getPushEstimate() {
        return getEstimate(getPush());
    }
    
    /** Get estimated number of peeks for size / work estimation.  Handles dynamic rates. */
    public int getPeekEstimate() {
        return getEstimate(getPeek());
    }
   
   
    // return number if we have it.
    // else return average from a range if we have it.
    // else return (max - min) / 2 from a range if we have it.
    // else guess min * 2 or max / 2 if only have min or max from a range.
    // return 1 if we have no information.
    private int getEstimate(JExpression e) {
        if (e instanceof JIntLiteral) {
            return ((JIntLiteral)e).intValue();
        }
        if (e instanceof SIRRangeExpression) {
            SIRRangeExpression r = (SIRRangeExpression)e;
            if (r.getAve() instanceof JIntLiteral) {
                return ((JIntLiteral)r.getAve()).intValue();
            }
            JExpression min = r.getMin();
            JExpression max = r.getMax();
            if (min instanceof JIntLiteral) {
                if (max instanceof JIntLiteral) {
                    return Math.min( (((JIntLiteral)min).intValue()
                            + ((JIntLiteral)max).intValue()) / 2, 1);
                } else {
                    return ((JIntLiteral)min).intValue() * 2;
                }
            }
            if (max instanceof JIntLiteral) {
                return Math.min( ((JIntLiteral)max).intValue() / 2, 1);
            }
        }
        return 1;  // no info, allow estimators to underestimate.
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

    /**
     * Return string representation of pop rate (either int literal
     * or range, e.g., [1,2,3]).
     */
    public String getPopString() {
        return getPhases()[0].getPopString();
    }

    /**
     * Return string representation of peek rate (either int literal
     * or range, e.g., [1,2,3]).
     */
    public String getPeekString() {
        return getPhases()[0].getPeekString();
    }

    /**
     * Return string representation of push rate (either int literal
     * or range, e.g., [1,2,3]).
     */
    public String getPushString() {
        return getPhases()[0].getPushString();
    }

    /* Overridden from SIRStream: */
    public JMethodDeclaration getWork() 
    {
        return getPhases()[0];
    }
    
    /* Overridden from SIRStream: */
    public void setWork(JMethodDeclaration work) {
        // if new work function has no I/O rates and old one does,
        // then transfer rates to new one.  This is an ugly remnant of
        // the old mode of operation, where I/O rates were stored
        // outside the function.
        if (!work.doesIO()) {
            work.setPeek(getWork().getPeek());
            work.setPop(getWork().getPop());
            work.setPush(getWork().getPush());
        }

        addReplacementMethod(work, getWork());
        getPhases()[0] = work;
    }

    /* Overridden from SIRPhasedFilter: */
    /* This seems like a good idea, but it breaks SIRTwoStageFilter
     * being derived from SIRFilter.  Changing that is a Big Change,
     * since it involves making the entire world phase-aware.  Not
     * that this is actually a bad thing, but...
     public void setInitPhases(JMethodDeclaration[] initPhases) 
     {
     throw new UnsupportedOperationException
     ("SIRFilters can't have init phases");
     }
    */

    /* Overridden from SIRPhasedFilter: */
    public void setPhases(JMethodDeclaration[] phases)
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

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.sir.SIRFilter other) {
        super.deepCloneInto(other);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}



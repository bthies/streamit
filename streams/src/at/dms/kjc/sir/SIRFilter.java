package at.dms.kjc.sir;

import at.dms.compiler.JavaStyleComment;
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
 * @version $Id: SIRFilter.java,v 1.43 2007-06-07 18:07:21 dimock Exp $
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

    /**
     * Set init and work functions to be for an identity filter with the given pop / push rate.
     * @param rate push and pop rates
     * @param t input and output type
     */
    public void makeIdentityFilter(JExpression rate, CType t) {
        assert rate != null : "Constructing SIRIdentity with null rate";

        this.setInputType(t);
        this.setOutputType(t);

        this.setPush(rate);  // set rates
        this.setPop(rate);
        this.setPeek(rate);  // some parts of compiler expect peek rate >= pop rate, so set it.
        
        
        // work function
        
        JVariableDefinition tmp = new JVariableDefinition(null, 0, this.getInputType(), 
                at.dms.kjc.sir.lowering.ThreeAddressCode.nextTemp(), null);
        JVariableDeclarationStatement declarePopExpr = new JVariableDeclarationStatement(tmp);
        JLocalVariableExpression referencePoppedValue = new JLocalVariableExpression(tmp);
        JStatement popIt = new JExpressionStatement(
                new JAssignmentExpression(
                        referencePoppedValue, 
                        new SIRPopExpression(this.getInputType())));
        
        JStatement work1body[];
        if (rate instanceof JIntLiteral && 
            ((JIntLiteral)rate).intValue() == 1) {
            work1body = new JStatement[] {
                    declarePopExpr,
                    popIt,
                    new JExpressionStatement(null, 
                            new SIRPushExpression(
                                    referencePoppedValue, this.getInputType()), 
                            null) };
        

        } else {
            JStatement pushPop = 
                new JBlock(new JStatement[]{
                        declarePopExpr,
                        popIt,
                        new JExpressionStatement(null,
                                new SIRPushExpression(referencePoppedValue, this.getInputType()),
                                null)});
            JVariableDefinition induction = 
                new JVariableDefinition(null, 0,
                                        CStdType.Integer,
                                        "i",
                                        new JIntLiteral(0));
            JRelationalExpression cond = new JRelationalExpression(null,
                                                                   Constants.OPE_LT,
                                                                   new JLocalVariableExpression(null,
                                                                                                induction),
                                                                   rate);

            JExpressionStatement increment = 
                new JExpressionStatement(null,
                                         new JCompoundAssignmentExpression(null,
                                                 Constants.OPE_PLUS,
                                                                           new JLocalVariableExpression(null,
                                                                                                        induction),
                                                                           new JIntLiteral(1)),
                                         null);
            work1body= new JStatement[] { 
                    new JForStatement(null,
                       new JVariableDeclarationStatement(null, induction, null),
                       cond, increment, pushPop, 
                       new JavaStyleComment[] {
                            new JavaStyleComment("IncreaseFilterMult", true,
                                false, false)})};       
        
        }
    
        JBlock work1block = new JBlock(/* tokref   */ null,
                                       /* body     */ work1body,
                                       /* comments */ null);    
    
        JMethodDeclaration workfn =  new JMethodDeclaration( /* tokref     */ null,
                                                             /* modifiers  */ at.dms.kjc.
                                                             Constants.ACC_PUBLIC,
                                                             /* returntype */ CStdType.Void,
                                                             /* identifier */ "work",
                                                             /* parameters */ JFormalParameter.EMPTY,
                                                             /* exceptions */ CClassType.EMPTY,
                                                             /* body       */ work1block,
                                                             /* javadoc    */ null,
                                                             /* comments   */ null);
        setWork(workfn);

        // init function
        JBlock initblock = new JBlock(/* tokref   */ null,
                                      /* body     */ new JStatement[0],
                                      /* comments */ null);
        setInit(SIRStream.makeEmptyInit());
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



package at.dms.kjc.sir;

import at.dms.kjc.sir.lowering.Propagator;
import at.dms.kjc.*;
import at.dms.compiler.JavaStyleComment; // for debugging

/**
 * This represents a StreaMIT filter that just reads <N> item and sends it along.
 */
public class SIRIdentity extends SIRPredefinedFilter implements Cloneable, Constants {

    private JExpression rate;

    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private SIRIdentity() {
        super();
    }

    public SIRIdentity(JExpression rate, CType type) 
    {
        super(null,
              "Identity",
              /* fields */ JFieldDeclaration.EMPTY(), 
              /* methods */ JMethodDeclaration.EMPTY(),
              rate, rate, rate,
              /* input type */ type,
              /* output type */ type);
        setRate(rate);
    }
    

    public SIRIdentity(CType type) {
        super(null,
              "Identity",
              /* fields */ JFieldDeclaration.EMPTY(), 
              /* methods */ JMethodDeclaration.EMPTY(),
              new JIntLiteral(1), new JIntLiteral(1), new JIntLiteral(1),
              /* input type */ type,
              /* output type */ type);
        setRate(new JIntLiteral(1));
    }

    public void setRate(JExpression r) 
    {
        this.rate = r;
        setType(this.getInputType());
        this.setPeek(r);
        this.setPop(r);
        this.setPush(r);
    }

    public JExpression getRate() 
    {
        return rate;
    }


    public void propagatePredefinedFields(Propagator propagator) {
        JExpression newRate = (JExpression)rate.accept(propagator);
        if (newRate!=null && newRate!=rate) {
            rate = newRate;
        }
    }

    /**
     * Set the input type and output type to t
     * also sets the work function and init function
     */
    public void setType(CType t) {
        this.makeIdentityFilter(rate, t);
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.sir.SIRIdentity other = new at.dms.kjc.sir.SIRIdentity();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.sir.SIRIdentity other) {
        super.deepCloneInto(other);
        other.rate = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.rate);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}


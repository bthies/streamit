package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * As a statement, invoke a phase in a phased filter.  This is used as a
 * placeholder in work functions until constant propagation happens, at
 * which point the list of phases can be generated.
 */
public class SIRPhaseInvocation extends JStatement {
    /**
     * The actual function call corresponding to the phase.
     */
    private JMethodCallExpression call;
    
    /**
     * I/O rates.
     */
    private JExpression peek, pop, push;
    
    /**
     * Construct a node in the parsing tree
     * @param where the line of this node in the source code
     * @param call the function call corresponding to the phase
     * @param peek the peek rate of the phase
     * @param pop the pop rate of the phase
     * @param push the push rate of the phase
     */
    public SIRPhaseInvocation(TokenReference where, JMethodCallExpression call,
                              JExpression peek, JExpression pop,
                              JExpression push, JavaStyleComment[] comments)
    {
        super(where, comments);
        this.call = call;
        this.peek = peek;
        this.pop = pop;
        this.push = push;
    }
    
    public SIRPhaseInvocation()
    {
        super(null, null);
        this.call = null;
        this.peek = null;
        this.pop = null;
        this.push = null;
    }

	public JMethodCallExpression getCall()
	{
		return call;
	}
	
	public JExpression getPeek()
	{
		return peek;
	}
	
	public JExpression getPop()
	{
		return pop;
	}
	
	public JExpression getPush()
	{
		return push;
	}
	
    public void setCall(JMethodCallExpression call)
    {
        this.call = call;
    }
    
    public void setPeek(JExpression peek)
    {
        this.peek = peek;
    }
    
    public void setPop(JExpression pop)
    {
        this.pop = pop;
    }
    
    public void setPush(JExpression push)
    {
        this.push = push;
    }
    
    /**
     * Analyses the statement (semantically) - STUB.
     */
    public void analyse(CBodyContext context) throws PositionedError 
    {
    }
    
    /**
     * Generates a sequence of bytecodes - STUB.
     */
    public void genCode(CodeSequence code) 
    {
    }
    
    /**
     * Accepts the specified attribute visitor.
     */
    public Object accept(AttributeVisitor p)
    {
        if (p instanceof SLIRAttributeVisitor) {
            SLIRAttributeVisitor ap = (SLIRAttributeVisitor)p;
            return ap.visitPhaseInvocation(this, call, peek, pop, push);
        }
        else
            return this;
    }

    /**
     * Accepts the specified visitor.
     */
    public void accept(KjcVisitor p)
    {
        if (p instanceof SLIRVisitor) {
            SLIRVisitor sp = (SLIRVisitor)p;
            sp.visitPhaseInvocation(this, call, peek, pop, push);
        }
        else
        {
            call.accept(p);
            peek.accept(p);
            pop.accept(p);
            push.accept(p);
        }
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRPhaseInvocation other = new at.dms.kjc.sir.SIRPhaseInvocation();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRPhaseInvocation other) {
  super.deepCloneInto(other);
  other.call = (at.dms.kjc.JMethodCallExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.call, other);
  other.peek = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.peek, other);
  other.pop = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.pop, other);
  other.push = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.push, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}


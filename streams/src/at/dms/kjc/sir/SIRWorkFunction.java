package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;

/**
 * A work function performs computation in a (phased) filter.  Work
 * functions will generally be contained within SIRPhasedFilters.
 * They have code and constants for I/O rates.
 */
public class SIRWorkFunction extends Utils
{
    /**
     * The number of items that are peeked in each execution.
     */
    private JExpression peek;
    /**
     * The number of items that are popped in each execution.
     */
    private JExpression pop;
    /**
     * The number of items that are pushed in each execution.
     */
    private JExpression push;
    /**
     * The actual work function.
     */
    private JMethodDeclaration work;
    
    public SIRWorkFunction() 
    {
        this.pop = new JIntLiteral(0);
        this.push = new JIntLiteral(0);
        this.peek = new JIntLiteral(0);
        this.work = null;
    }

    public SIRWorkFunction(JExpression peek,
                           JExpression pop,
                           JExpression push, 
                           JMethodDeclaration work)
    {
        this.peek = peek;
        this.pop = pop;
        this.push = push;
        this.work = work;
    }

    public SIRWorkFunction(int peek, int pop, int push,
                           JMethodDeclaration work)
    {
        this(new JIntLiteral(peek),
             new JIntLiteral(pop),
             new JIntLiteral(push),
             work);
    }

    public void setPeek(JExpression p)
    {
        this.peek = p;
    }
    
    public void setPop(JExpression p)
    {
        this.pop = p;
    }
    
    public void setPush(JExpression p)
    {
        this.push = p;
    }
    
    public void setPeek(int p)
    {
        setPeek(new JIntLiteral(p));
    }
    
    public void setPop(int p)
    {
        setPop(new JIntLiteral(p));
    }
    
    public void setPush(int p)
    {
        setPush(new JIntLiteral(p));
    }
    
    public JExpression getPeek()
    {
        return this.peek;
    }
    
    public JExpression getPop()
    {
        return this.pop;
    }
    
    public JExpression getPush()
    {
        return this.push;
    }

    public JMethodDeclaration getWork()
    {
        return this.work;
    }
    
    public void setWork(JMethodDeclaration work)
    {
        this.work = work;
    }
    
    /**
     * Returns how many items are popped.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPop.
     */
    public int getPopInt() {
      if (pop instanceof JFloatLiteral) { //clleger
	pop = new JIntLiteral(null, (int) ((JFloatLiteral)pop).floatValue());
      }
	// need int literal to get number
	if (!(pop instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for pop value in work function " + this.getWork().getName() + ", but the constant hasn't been resolved yet. " + pop);
	}
	return ((JIntLiteral)pop).intValue();
    }

    /**
     * Returns how many items are peeked.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPeek.
     */
    public int getPeekInt() {
      if (peek instanceof JFloatLiteral) { //clleger
	peek = new JIntLiteral(null, (int) ((JFloatLiteral)peek).floatValue());
      }
	// need int literal to get number
	if (!(peek instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for peek value in work function " + this.getWork().getName() + ", but the constant hasn't been resolved yet. " + peek);
	}
	return ((JIntLiteral)peek).intValue();
    }

    /**
     * Returns how many items are pushed.This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPush.
     */
    public int getPushInt() {
	// need int literal to get number
      if (push instanceof JFloatLiteral) { //clleger
	push = new JIntLiteral(null, (int) ((JFloatLiteral)push).floatValue());
      }

	if (!(push instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for push value in work function " + this.getWork().getName() + ", but the constant hasn't been resolved yet. " + push);
	}
	return ((JIntLiteral)push).intValue();
    }

    public String getIdent()
    {
        return "<<work function>>";
    }

    public Object accept(AttributeStreamVisitor v)
    {
        return v.visitWorkFunction(this, this.work);
    }
}

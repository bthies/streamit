/*
 * ExprFunCall.java: a function call expression
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: ExprFunCall.java,v 1.2 2002-07-15 20:38:51 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

import java.util.ArrayList;

/**
 * A call to a particular named function.  This contains the name of the
 * function and a java.util.List of parameters.  Like other Expressions,
 * this is immutable; an unmodifiable copy of the passed-in List is saved.
 */
public class ExprFunCall extends Expression
{
    private String name;
    private List params;
    
    /** Creates a new function call with the specified name and
     * parameter list. */
    public ExprFunCall(String name, List params)
    {
        this.name = name;
        this.params = Collections.unmodifiableList(params);
    }

    /** Creates a new function call with the specified name and
     * specified single parameter. */
    public ExprFunCall(String name, Expression param)
    {
        this.name = name;
        this.params = new ArrayList();
        this.params.add(param);
        this.params = Collections.unmodifiableList(this.params);
    }

    /** Returns the name of the function being called. */
    public String getName()
    {
        return name;
    }
    
    /** Returns the parameters of the function call, as an unmodifiable
     * list. */
    public List getParams()
    {
        return params;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprFunCall(this);
    }
}

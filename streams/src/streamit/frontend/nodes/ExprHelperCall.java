/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

import java.util.ArrayList;

/**
 * A call to a particular helper function.  
 *
 * @author  Janis Sermulins
 */

public class ExprHelperCall extends Expression
{
    private String helper_package;
    private String name;
    private List<Expression> params;
    
    /** Creates a new helper call with the specified name and
     * parameter list. */
    public ExprHelperCall(FEContext context, String helper_package, String name, List<Expression> params)
    {
        super(context);
        this.helper_package = helper_package;
        this.name = name;
        this.params = Collections.unmodifiableList(params);
    }

    /** Returns the name of the helper package. */
    public String getHelperPackage()
    {
        return helper_package;
    }
   
    /** Returns the name of the function being called. */
    public String getName()
    {
        return name;
    }
    
    /** Returns the parameters of the helper call, as an unmodifiable
     * list. */
    public List<Expression> getParams()
    {
        return params;
    }
    
    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitExprHelperCall(this);
    }
}

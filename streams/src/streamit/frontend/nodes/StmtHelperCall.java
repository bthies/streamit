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

/**
 * A statement that causes a helper call to be executed.
 *
 * @author  Janis Sermulins
 */

public class StmtHelperCall extends Statement
{
    private String helper_package;
    private String name;
    private List<Expression> params;
    
    /**
     * Creates a helper call statement.
     *
     */
    public StmtHelperCall(FEContext context, String helper_package,
                          String name, List<Expression> params)
    {
        super(context);
        this.helper_package = helper_package;
        this.name = name;
        this.params = Collections.unmodifiableList(params);
    }

    /**
     * Get the helper package.
     */
    public String getHelperPackage()
    {
        return helper_package;
    }

    /**
     * Get the name of the helper function.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Get the parameter list of the helper call.
     */
    public List<Expression> getParams()
    {
        return params;
    }
    
    /**
     * Accepts a front-end visitor.  Calls
     * <code>streamit.frontend.nodes.FEVisitor.visitStmtHelperCall</code>
     * on the visitor.
     *
     * @param v  visitor to accept
     * @return   defined by the visitor object
     */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtHelperCall(this);
    }
}

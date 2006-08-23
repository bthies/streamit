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

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

/**
 * This statement is used to indicate that a given function has a
 * given I/O rate.  It also keeps track of whether the given function
 * serves as the work function for either that init or steady-state.
 *
 * If a function serves as a work function, it is translated into a
 * <code>addInitPhase</code> or an <code>addSteadyPhase</code> call.
 * Note that we used to support multiple phases, but now we only
 * support one per execution epoch (init and steady).  
 *
 * Other functions that do I/O are translated into a
 * <code>annotateIORate</code> call.
 * 
 * Both calls have peek, pop, and push rates and the name of a method
 * to call.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtIODecl.java,v 1.6 2006-08-23 23:01:13 thies Exp $
 */
public class StmtIODecl extends Statement
{
    private boolean prework, work;
    private Expression peek, pop, push;
    private String name;

    /**
     * Create an IO-declaring statement from scratch.  You may want
     * one of the constructors below; this is mainly done while
     * replacing.
     *
     * Note that prework=true for prework functions; work=true for
     * work functions, and prework=work=false for helper functions.
     *
     * @param context  Context this statement appears in
     * @param prework  Whether this represents prework function
     * @param work     Whether this represents work function
     * @param peek     Peek rate of this function
     * @param pop      Pop rate of this function
     * @param push     Push rate of this function
     * @param name     Name of this function
     */
    public StmtIODecl(FEContext context, boolean prework, boolean work,
                      Expression peek, Expression pop, Expression push,
                      String name) {
        super(context);
        this.prework = prework;
        this.work = work;
        this.peek = peek;
        this.pop = pop;
        this.push = push;
        this.name = name;
    }

    /**
     * Create an IO-declaring statement from a helper function.
     *
     * @param context  Context this statement appears in
     * @param helper   Helper function itself
     */
    public StmtIODecl(FEContext context, Function helper)
    {
        super(context);
        // this is a helper function, so it is neither prework nor work
        this.prework = false;
        this.work = false;
        this.peek = helper.getPeekRate();
        this.pop = helper.getPopRate();
        this.push = helper.getPushRate();
        this.name = helper.getName();
    }
    
    /**
     * Create an IO-declaring statement from a work function.
     *
     * @param context  Context this statement appears in
     * @param init     true if the function is prework; otherwise it is work
     * @param work     Work function itself
     */
    public StmtIODecl(FEContext context, boolean init, FuncWork work)
    {
        super(context);
        this.prework = init;
        this.work = !init;
        this.peek = work.getPeekRate();
        this.pop = work.getPopRate();
        this.push = work.getPushRate();
        if (work.getName() != null)
            this.name = work.getName();
        else if (init)
            this.name = "prework";
        else
            this.name = "work";
    }
    
    /** Returns whether this corresponds to the prework function. */
    public boolean isPrework()
    {
        return prework;
    }

    /** Returns whether this corresponds to the work function. */
    public boolean isWork()
    {
        return work;
    }

    /** Returns the peek rate of the function. */
    public Expression getPeek()
    {
        return peek;
    }
    
    /** Returns the pop rate of the function. */
    public Expression getPop()
    {
        return pop;
    }
    
    /** Returns the push rate of the function. */
    public Expression getPush()
    {
        return push;
    }

    /** Returns the name of the function. */
    public String getName()
    {
        return name;
    }

    public Object accept(FEVisitor v)
    {
        return v.visitOther(this);
    }
}

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

/**
 * Declare the joiner type for a split-join or feedback loop.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtJoin.java,v 1.5 2003-10-09 19:51:00 dmaze Exp $
 */
public class StmtJoin extends Statement
{
    private SplitterJoiner sj;
    
    /**
     * Creates a new join statement with the specified joiner type.
     *
     * @param context  file and line number this statement corresponds to
     * @param splitter type of splitter for this stream
     */
    public StmtJoin(FEContext context, SplitterJoiner joiner)
    {
        super(context);
        sj = joiner;
    }
    
    /**
     * Returns the joiner type for this.
     *
     * @return the joiner object
     */
    public SplitterJoiner getJoiner()
    {
        return sj;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtJoin(this);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof StmtJoin))
            return false;
        return ((StmtJoin)other).sj.equals(sj);
    }
    
    public int hashCode()
    {
        return sj.hashCode();
    }
    
    public String toString()
    {
        return "join " + sj;
    }
}

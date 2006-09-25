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
 * A block of statements executed in sequence.  This introduces a
 * lexical scope for variable declarations, and is a way for multiple
 * statements to be used in loops or conditionals.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StmtBlock.java,v 1.4 2006-09-25 13:54:54 dimock Exp $
 */
public class StmtBlock extends Statement
{
    private List<Statement> stmts;
    // Should this also have a symbol table?  --dzm
    
    /** Create a new StmtBlock with the specified ordered list of
     * statements. */
    public StmtBlock(FEContext context, List<Statement> stmts)
    {
        super(context);
        this.stmts = Collections.unmodifiableList(stmts);
    }
    
    /** Returns the list of statements of this. */
    public List<Statement> getStmts()
    {
        return stmts;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtBlock(this);
    }
}

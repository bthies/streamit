/*
 * StmtBlock.java: a block of statements
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StmtBlock.java,v 1.2 2002-09-06 16:28:43 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

/**
 * StmtBlock is a block of statements executed in sequence.  It introduces
 * a lexical scope for variable declarations, and is a way for multiple
 * statements to be used in loops or conditionals.
 */
public class StmtBlock extends Statement
{
    private List stmts;
    // Should this also have a symbol table?  --dzm
    
    /** Create a new StmtBlock with the specified ordered list of
     * statements. */
    public StmtBlock(FEContext context, List stmts)
    {
        super(context);
        this.stmts = Collections.unmodifiableList(stmts);
    }
    
    /** Returns the list of statements of this. */
    public List getStmts()
    {
        return stmts;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtBlock(this);
    }
}

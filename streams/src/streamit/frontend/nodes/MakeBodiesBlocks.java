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
 * Front-end visitor pass to replace the bodies of compound statements
 * with blocks.  This obeys the same conventions as FEReplacer.
 * Compound statements, including functions, have their bodies replaced
 * with StmtBlocks if they are a different sort of statement.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: MakeBodiesBlocks.java,v 1.3 2003-10-09 19:50:59 dmaze Exp $
 */
public class MakeBodiesBlocks extends FEReplacer
{
    private Statement buildBlock(Statement stmt)
    {
        if (stmt instanceof StmtBlock) return stmt;
        List body = Collections.singletonList(stmt);
        return new StmtBlock(stmt.getContext(), body);
    }

    public Object visitFunction(Function func)
    {
        Statement newBody = (Statement)func.getBody().accept(this);
        newBody = buildBlock(newBody);
        if (newBody == func.getBody()) return func;
        return new Function(func.getContext(), func.getCls(),
                            func.getName(), func.getReturnType(),
                            func.getParams(), newBody);
    }
    
    public Object visitFuncWork(FuncWork func)
    {
        Statement newBody = (Statement)func.getBody().accept(this);
        newBody = buildBlock(newBody);
        if (newBody == func.getBody())
            return func;
        return new FuncWork(func.getContext(), func.getCls(), func.getName(),
                            newBody, func.getPeekRate(), func.getPopRate(),
                            func.getPushRate());
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        Statement newBody = (Statement)stmt.getBody().accept(this);
        newBody = buildBlock(newBody);
        if (newBody == stmt.getBody())
            return stmt;
        return new StmtDoWhile(stmt.getContext(), newBody, stmt.getCond());
    }
    
    public Object visitStmtFor(StmtFor stmt)
    {
        // Note that init and incr are statements, but we need to
        // deal specially with them; for purposes of converting to
        // Java, we can't legally have a block here.
        Statement newBody = (Statement)stmt.getBody().accept(this);
        newBody = buildBlock(newBody);
        if (newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt.getContext(), stmt.getInit(), stmt.getCond(),
                           stmt.getIncr(), newBody);
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        Statement newCons = null;
        if (stmt.getCons() != null)
        {
            newCons = (Statement)stmt.getCons().accept(this);
            newCons = buildBlock(newCons);
        }
        Statement newAlt = null;
        if (stmt.getAlt() != null)
        {
            newAlt = (Statement)stmt.getAlt().accept(this);
            newAlt = buildBlock(newAlt);
        }
        if (newCons == stmt.getCons() && newAlt == stmt.getAlt())
            return stmt;
        return new StmtIfThen(stmt.getContext(), stmt.getCond(),
                              newCons, newAlt);
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        Statement newBody = (Statement)stmt.getBody().accept(this);
        newBody = buildBlock(newBody);
        if (newBody == stmt.getBody())
            return stmt;
        return new StmtWhile(stmt.getContext(), stmt.getCond(), newBody);
    }
}

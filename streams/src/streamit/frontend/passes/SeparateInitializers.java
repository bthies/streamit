package streamit.frontend.passes;

import streamit.frontend.nodes.*;
import java.util.Collections;
import java.util.List;

/**
 * Separate variable initializers into separate statements.  Given
 * initialized variables like
 *
 * <pre>
 * int c = 4;
 * </pre>
 *
 * separate this into two statements like
 *
 * <pre>
 * int c;
 * c = 4;
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: SeparateInitializers.java,v 1.2 2003-07-09 20:47:45 dmaze Exp $
 */
public class SeparateInitializers extends FEReplacer
{
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        // Make sure the variable declaration stays first.
        // This will have no initializers at all.
        List newInits = Collections.nCopies(stmt.getNumVars(), null);
        Statement newDecl = new StmtVarDecl(stmt.getContext(),
                                            stmt.getTypes(),
                                            stmt.getNames(),
                                            newInits);
        addStatement(newDecl);

        // Now go through the original statement; if there are
        // any initializers, create a new assignment statement.
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String name = stmt.getName(i);
            Expression init = stmt.getInit(i);
            if (init != null)
            {
                Statement assign =
                    new StmtAssign(stmt.getContext(),
                                   new ExprVar(stmt.getContext(), name),
                                   init);
                addStatement(assign);
            }
        }

        // Already added the base statement.
        return null;
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        // Only recurse into the body.
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt.getContext(), stmt.getInit(),
                           stmt.getCond(), stmt.getIncr(), newBody);
    }
}

package streamit.frontend.passes;

import streamit.frontend.nodes.*;
import java.util.List;
import java.util.ListIterator;

/**
 * Pair up variable declarations and adjacent assignments.  Some of the
 * Kopi code depends on having initialized variables, but the front end
 * code generally goes out of its way to separate declarations and
 * initialization.  This looks for adjacent statements that deal with
 * the same variable, and combine them:
 *
 * <pre>
 * int[] v;
 * v = new int[4];
 * // becomes: int[] v = new int[4];
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: AssembleInitializers.java,v 1.1 2003-08-26 19:35:45 dmaze Exp $
 */
public class AssembleInitializers extends FEReplacer
{
    public Object visitStmtBlock(StmtBlock block)
    {
        List oldStatements = newStatements;
        newStatements = new java.util.ArrayList();
        for (ListIterator iter = block.getStmts().listIterator();
             iter.hasNext(); )
        {
            Statement stmt = (Statement)iter.next();
            while (stmt instanceof StmtVarDecl && iter.hasNext())
            {
                Statement nst = (Statement)iter.next();
                iter.previous();
                if (!(nst instanceof StmtAssign))
                    break;
                // check that the LHS of the next statement is
                // a simple variable
                Expression lhs = ((StmtAssign)nst).getLHS();
                if (!(lhs instanceof ExprVar))
                    break;
                String varName = ((ExprVar)lhs).getName();
                // Now, walk through the declaration.
                StmtVarDecl decl = (StmtVarDecl)stmt;
                List newInits = new java.util.ArrayList();
                boolean found = false;
                for (int i = 0; i < decl.getNumVars(); i++)
                {
                    Expression init = decl.getInit(i);
                    if (decl.getName(i).equals(varName) &&
                        init == null)
                    {
                        init = ((StmtAssign)nst).getRHS();
                        found = true;
                        iter.next(); // consume the assignment
                    }
                    newInits.add(init);
                }
                if (!found)
                    break;
                // So, if we've made it here, then newInits
                // is different from stmt's initializer list,
                // and we want to iterate.  Reassign stmt.
                stmt = new StmtVarDecl(decl.getContext(),
                                       decl.getTypes(),
                                       decl.getNames(),
                                       newInits);
            }
            addStatement(stmt);
        }   
        Statement result = new StmtBlock(block.getContext(), newStatements);
        newStatements = oldStatements;
        return result;
    }
}

/*
 * FEVisitor.java: visit a tree of front-end nodes
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FEVisitor.java,v 1.4 2002-09-04 18:42:18 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A FEVisitor implements part of the "visitor" design pattern for
 * all StreamIt front-end nodes.  The pattern basically exchanges type
 * structures for function calls, so a different function in the visitor
 * is called depending on the run-time type of the object being visited.
 * Calling visitor methods returns some value, the type of which
 * depends on the semantics of the visitor in question.  In general,
 * you will create a visitor object, and then pass it to the accept()
 * method of the object in question.
 *
 * This visitor visits all nodes created in the front-end.  It is built
 * on top of FEExprVisitor, which only visits nodes derived from Expression.
 */
public interface FEVisitor extends FEExprVisitor
{
}

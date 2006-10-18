/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: StatementQueueVisitor.java,v 1.4 2006-10-18 23:40:00 dimock Exp $
 */

package at.dms.kjc;

import java.util.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This visitor is for analyses that want to add one or more
 * statements every time they see a given expression.  Since an
 * expression cannot be directly replaced by a statement, this class
 * keeps track of pending statements and adds them as soon as possible
 * at the statement level.
 *
 * For example, if you want to replace "pop()" by "buffer[index];
 * index = (index+1) % buffer_size" then you should extend this and
 * treat the increment of "index" as the pending statement.
 */
public class StatementQueueVisitor extends SLIRReplacingVisitor {
    /**
     * List of pending statements to add after finished with current
     * statement.  (Subclasses add to this list when they are visiting
     * an expression, and thus they can't add a new statement until we
     * are back at the statement level.)
     */
    private final List<JStatement> pendingStatements;
    
    /**
     * Whether or not statement queues are supported under the current
     * enclosing control flow.  (Haven't worried about corner cases
     * yet.)
     */
    private boolean supported;

    /**
     // ----------------------------------------------------------------------
     // INTERFACE
     // ----------------------------------------------------------------------
     */

    public StatementQueueVisitor() {
        this.pendingStatements = new LinkedList<JStatement>();
        this.supported = true;
    }

    /**
     * Subclasses should call this when visiting an expression to add
     * a statement to the queue.
     */
    protected void addPendingStatement(JStatement stmt) {
        if (!supported) {
            Utils.fail("Unsupported control flow in statement queue.\n" +
                       "(For example, maybe you are doing something complicated,\n" +
                       " like push/pop, from within a for loop condition or increment?)");
        }
        pendingStatements.add(stmt);
    }

    /**
     // ----------------------------------------------------------------------
     // LOCAL HELPING FUNCTIONS
     // ----------------------------------------------------------------------
     */

    /**
     * Returns JBlock with all pending statements coming after <pre>old</pre>.
     */
    private JStatement appendPending(JStatement old) {
        if (pendingStatements.size()>0) {
            JBlock result = new JBlock();
            result.addStatement(old);
            result.addAllStatements(pendingStatements);
            pendingStatements.clear();
            return result;
        } else {
            return old;
        }
    }

    /**
     // ----------------------------------------------------------------------
     // STATEMENT VISITORS
     // ----------------------------------------------------------------------
     */
    public Object visitWhileStatement(JWhileStatement self,
                                      JExpression cond,
                                      JStatement body) {
        // pending statements from <pre>cond</pre> go both at top of <pre>body</pre> and
        // after body altogether (since we have to cover both the case
        // when cond is true, and when cond is false).

        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCondition(newExp);
        }

        JStatement result;
        if (pendingStatements.size()>0) {
            // copy pending statements to frong of body
            JStatement oldBody = body;
            body = new JBlock();
            for (int i=0; i<pendingStatements.size(); i++) {
                // put a copy here; the actual statements will go at end clause
                ((JBlock)body).addStatementFirst((JStatement)ObjectDeepCloner.deepCopy((JStatement)pendingStatements.get(i)));
            }
            ((JBlock)body).addStatement(oldBody);
        
            // make JBlock holding result, with pending statements at end
            result = new JBlock();
            ((JBlock)result).addAllStatements(pendingStatements);
            pendingStatements.clear();
        
            // add self as beginning of result block
            ((JBlock)result).addStatementFirst(self);
        } else {
            // set result directly to <pre>self</pre> to avoid introducing
            // extra JBlocks
            result = self;
        }
    
        // visit body as before
        JStatement newSt = (JStatement)body.accept(this);
        if (newSt!=null && newSt!=body) {
            self.setBody(newSt);
        }

        return result;
    }

    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                    JVariableDefinition[] vars) {
        JStatement old = (JStatement)super.visitVariableDeclarationStatement(self, vars);
        return appendPending(old);
    }

    public Object visitSwitchStatement(JSwitchStatement self,
                                       JExpression expr,
                                       JSwitchGroup[] body) {
        // do not bother supporting in <pre>expr</pre>
        boolean oldSupported = supported;
        supported = false;

        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpression(newExp);
        }

        supported = true;

        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        return self;
    }

    public Object visitIfStatement(JIfStatement self,
                                   JExpression cond,
                                   JStatement thenClause,
                                   JStatement elseClause) {
        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCondition(newExp);
        }

        if (pendingStatements.size() > 0) {
            // add pending statements to top of BOTH <pre>thenClause</pre> and <pre>elseClause</pre>
            // -- then clause:
            JStatement oldThen = thenClause;
            thenClause = new JBlock();
            for (int i=0; i<pendingStatements.size(); i++) {
                // put a copy here; the actual statements will go in else clause
                ((JBlock)thenClause).addStatementFirst((JStatement)ObjectDeepCloner.deepCopy((JStatement)pendingStatements.get(i)));
            }
            ((JBlock)thenClause).addStatement(oldThen);
            // -- else clause:
            JStatement oldElse = elseClause;
            elseClause = new JBlock();
            ((JBlock)elseClause).addAllStatements(pendingStatements);
            ((JBlock)elseClause).addStatement(oldElse);
            // clear pending statements
            pendingStatements.clear();
        }

        JStatement newThen = (JStatement)thenClause.accept(this);
        if (newThen!=null && newThen!=thenClause) {
            self.setThenClause(newThen);
        }
        if (elseClause != null) {
            JStatement newElse = (JStatement)elseClause.accept(this);
            if (newElse!=null && newElse!=elseClause) {
                self.setElseClause(newElse);
            }
        }

        return self;
    }

    public Object visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {
        // don't bother supporting init, cond, incr
        boolean oldSupported = supported;
        supported = false;

        {
            // recurse into init
            JStatement newInit = (JStatement)init.accept(this);
            if (newInit!=null && newInit!=init) {
                self.setInit(newInit);
            }
        
            // recurse into cond
            JExpression newExp = (JExpression)cond.accept(this);
            if (newExp!=null && newExp!=cond) {
                self.setCond(newExp);
            }
        
            // recurse into incr
            JStatement newIncr = (JStatement)incr.accept(this);
            if (newIncr!=null && newIncr!=incr) {
                self.setIncr(newIncr);
            }
        }

        supported = oldSupported;
    
        // recurse into body
        JStatement newBody = (JStatement)body.accept(this);
        if (newBody!=null && newBody!=body) {
            self.setBody(newBody);
        }
        return self;

    }

    public Object visitExpressionStatement(JExpressionStatement self,
                                           JExpression expr) {
        JStatement old = (JStatement)super.visitExpressionStatement(self, expr);
        return appendPending(old);
    }

    public Object visitExpressionListStatement(JExpressionListStatement self,
                                               JExpression[] expr) {
        JStatement old = (JStatement)super.visitExpressionListStatement(self, expr);
        // could maybe do something more precise here, like break into
        // two expression list statements depending on where the
        // statements are made pending... but I think expression list
        // statements only appear in for loop initializers (?) which
        // are currently unsupported
        return appendPending(old);
    }

    public Object visitDoStatement(JDoStatement self,
                                   JExpression cond,
                                   JStatement body) {
        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCondition(newExp);
        }

        if (pendingStatements.size()>0) {
            // add pending statements to top of body
            JStatement oldBody= body;
            body = new JBlock();
            ((JBlock)body).addAllStatementsFirst(pendingStatements);
            ((JBlock)body).addStatement(oldBody);
            pendingStatements.clear();
        }

        JStatement newBody = (JStatement)body.accept(this);
        if (newBody!=null && newBody!=body) {
            self.setBody(newBody);
        }

        return self;
    }

    public Object visitMessageStatement(SIRMessageStatement self,
                                        JExpression portal,
                                        String iname,
                                        String ident,
                                        JExpression[] args,
                                        SIRLatency latency) {
        JStatement old = (JStatement)super.visitMessageStatement(self, portal, iname, ident, args, latency);
        return appendPending(old);
    }

    public Object visitPrintStatement(SIRPrintStatement self,
                                      JExpression arg) {
        JStatement old = (JStatement)super.visitPrintStatement(self, arg);
        return appendPending(old);
    }

    // UNSUPPORTED ---------------------------------------------------------

    public Object visitReturnStatement(JReturnStatement self,
                                       JExpression expr) {
        // don't support return statements yet (would have to declare
        // temporary variable of appropriate type and assign to it)
        boolean oldSupported = supported;
        supported = false;
        JStatement old = (JStatement)super.visitReturnStatement(self, expr);
        supported = oldSupported;

        return old;
    }
}

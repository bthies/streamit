/**
 * FIXME: NOT YET DEBUGGED. DO NOT USE YET
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.IRPrinter;
//import at.dms.util.Utils;
import java.util.*;
import at.dms.compiler.JavaStyleComment;
/**
 * Introduce multi-pops.
 * 
 * A single SIRPopExpression may represent multiple pops.
 *
 * This pass takes sequences of 'pop' expressions for which the result is not used
 * and forms a single SIRPopExpression containing the pop count.
 * 
 * 
 * @author dimock
 *
 */

public class IntroduceMultiPops extends SLIRReplacingVisitor {
 
    private IntroduceMultiPops() { }


    /**
     * Walk stream structure and replace multiple pops from single SIRPopExpression.
     * 
     * @param str  a stream to work on
     */
    
    public static void doit(SIRStream str) {
        new IntroduceMultiPops().introduceMultiProps(str);
    }
    
    // The following structure appears all over the place.  It needs to be abstracted somehow.
    // Walk SIR structure to get down to 
    private void introduceMultiProps(SIRStream str) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            introduceMultiProps(fl.getBody());
            introduceMultiProps(fl.getLoop());
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                introduceMultiProps(child);
            }
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                introduceMultiProps(child);
            }
        }
        if (str instanceof SIRFilter) {
            for (int i = 0; i < str.getMethods().length; i++) {
                str.getMethods()[i].accept(this);
            }
        }
    }


    // fields for visitor
    private boolean inFor = false;
    private int forCount = 0;
    private boolean inExpressionStatement = false;
    // a SIRPop expression that we generated from the previously looked at code.
    // nested for scopes.

    // need stack of these for scope entry / exit.
    
    private SIRPopExpression prevPop = null;

    // an expression statment that consists only of a pop statement is
    // a candidate for combining pops.
    public Object visitExpressionStatement(JExpressionStatement self, 
                                           JExpression expr) {
        boolean oldInExpressionStatement = inExpressionStatement;
        
        if (expr instanceof SIRPopExpression) {inExpressionStatement = true;}
        Object newExpr = super.visitExpressionStatement(self,expr); 
        inExpressionStatement = oldInExpressionStatement;
        
        if (newExpr instanceof SIRPopExpression) {
            SIRPopExpression popExpr = (SIRPopExpression)newExpr;
            int numPops = popExpr.getNumPop();
            if (prevPop != null) {
                prevPop.setNumPop(numPops + prevPop.getNumPop());
                return new JEmptyStatement();
            } else {
                prevPop = popExpr;
                
                return newExpr;
            }
        } else {
            if (newExpr == expr) {
                return self;
            } else {
                return new JExpressionStatement((JExpression)newExpr);
            }
        }
    }

    // a pop that is an expression stops the accumulation of pops in statements.
    public Object visitSIRPopExpression(SIRPopExpression self,
            CType tapeType) {
        if (!inExpressionStatement) { prevPop = null; }
        return self;
    }
    
    // don't manipulate prevPop for other statements other than for, block.
    public Object visitLabeledStatement(JLabeledStatement self,
            String label,
            JStatement stmt) {
        SIRPopExpression oldPrevPop = prevPop;
        prevPop = null;
        
        Object newSelf = super.visitLabeledStatement(self,label,stmt);
        prevPop = oldPrevPop;
        return newSelf;
    }
    
    // assume any pop statment in if issued an undetermined number of times
    // could still include ifs not containing pops or non-local transfers 
    // of control 
    public Object visitIfStatement(JIfStatement self,
            JExpression cond,
            JStatement thenClause,
            JStatement elseClause) {
        prevPop = null;
        
        Object newSelf = super.visitIfStatement(self,cond,thenClause,elseClause);
        return newSelf;
    }

    public Object visitCompoundStatement(JCompoundStatement self,
            JStatement[] body) {
        // assumes that Comppound statement really is just block without parentheses
        JBlock b = new JBlock(body);
        JStatement newS = (JStatement)visitBlockStatement(b,b.getComments());
        
        if (newS instanceof JBlock) {
            return new JCompoundStatement(self.getTokenReference(),
                    ((JBlock)newS).getStatementArray());
        } else {
            return newS;
        }
    }
    
    // assume any pop statment in loop issued an undetermined number of times
    // could still include do loops not containing pops or non-local transfers 
    // of control
    public Object visitDoStatement(JDoStatement self,
            JExpression cond,
            JStatement body) {
        prevPop = null;
        
        Object newSelf = super.visitDoStatement(self,cond,body);
        return newSelf;
    }


    public Object visitBlockStatement(JBlock self,
            JavaStyleComment[] comments) {
        SIRPopExpression oldPrevPop = prevPop;
        prevPop = null;
        
        JBlock newBlock = (JBlock)super.visitBlockStatement(self,comments);
        
        List /*<JStatement>*/ oldStatements = newBlock.getStatements();
        List /*<JStatement>*/ simplifiedStatements = new LinkedList();
        int numDecls = 0;
        for (Iterator i = oldStatements.iterator(); i.hasNext();) {
            Object o = i.next();
            if (o != null && !(o instanceof JEmptyStatement)) {
                if ((o instanceof JExpressionStatement) &&
                    ((JExpressionStatement)(o)).getExpression() instanceof 
                    SIRPopExpression) {
                    // found a pop.  
                    SIRPopExpression popExpr = (SIRPopExpression)((JExpressionStatement)o).getExpression();
                    if (prevPop != null) {
                        //combine with immediately previous pop if any 
                        prevPop.setNumPop(popExpr.getNumPop() + prevPop.getNumPop());
                        continue;
                    } else {
                        // no immediately previous pop in block, but this
                        // could be previous for the next statement.
                        prevPop = popExpr;
                        simplifiedStatements.add(o);
                        continue;
                    } // assert false : "execution does not get here";
                } 
                prevPop = null;
                simplifiedStatements.add(o);
                if (o instanceof JVariableDeclarationStatement) {
                numDecls++;
                }
            }
        }
        // restore previous pop outside of block
        prevPop = oldPrevPop;
        // empty block becomes empty statement for surrounding block to elide
        int newLen = simplifiedStatements.size();

        if (newLen - numDecls == 0) {
            // nothing except declarations which should have no effect outside block
            return new JEmptyStatement();
        }
        if (newLen == 1) {
            // single statment (not declation)
            return simplifiedStatements.get(0);
        } else if (newLen == oldStatements.size()) {
            // same length: this loop only deletes never changes
            return newBlock;
        }
        return new JBlock(newBlock.getTokenReference(),
                simplifiedStatements,newBlock.getComments());
    }

    // could allow switch statements so long as no branch contained a pop or
    // non-local control flow.
    public Object visitSwitchStatement(JSwitchStatement self,
            JExpression expr,
            JSwitchGroup[] body) {
        prevPop = null;

        Object newSelf = super.visitSwitchStatement(self,expr,body);
        return newSelf;
           }
 
    // could allow switch groupd so long as no branch contained a pop or
    // non-local control flow.
    public Object visitSwitchGroup(JSwitchGroup self,
            JSwitchLabel[] labels,
            JStatement[] stmts) {
        prevPop = null;

        Object newSelf = super.visitSwitchGroup(self,labels,stmts);
        return newSelf;
    }

    
    // we are very interested in handling a for expression where
    // (1) the body reduces to a SIRPopExpression
    // (2) the trip count can be resolved to an integer.
    public Object visitForStatement(JForStatement self,
            JStatement init,
            JExpression cond,
            JStatement incr,
            JStatement body) {
        SIRPopExpression oldPrevPop = prevPop;
        prevPop = null;
        
        JForStatement newSelf = (JForStatement)super.visitForStatement(self,init,cond,incr,body);

        JStatement newBody = newSelf.getBody();
        if (newBody instanceof JExpressionStatement) {
            JExpressionStatement expStmt = (JExpressionStatement)newBody;
            if (expStmt.getExpression() instanceof SIRPopExpression) {
                System.err.println("Got a possible for statement.");
                self.accept(new IRPrinter());
            }
        }
        
        prevPop = oldPrevPop;
        return newSelf;
    }

    // assume can't determine number of executions of while.
    // if caontains any pops, then can't count them.
    public Object visitWhileStatement(JWhileStatement self,
            JExpression cond,
            JStatement body) {
        prevPop = null;
        
        Object newSelf = super.visitWhileStatement(self,cond,body);
        return newSelf;
    }
    
    // visitFieldDeclarationStatement -- none inside a method.
    // visitVariableDeclarationStatement -- can't contain SIRPopExpression
    // throw and synchronized:  not in StreamIt
    
    // return: pops after return should not be added to pops before...
    public Object visitReturnStatement(JReturnStatement self,
            JExpression expr) {
        prevPop = null;
        return super.visitReturnStatement(self,expr);
    }
    
    // we don't handle visitExpressionListStatement, but should.
    public Object visitExpressionListStatement(JExpressionListStatement self,
            JExpression[] expr) { 
        prevPop = null;
        return super.visitExpressionListStatement(self,expr);
    }
    
    // JContinueStatement -- not handled by replacing visitor, presumably not in StreamIt
    // JBreakStatement -- not in replacingVisitor, presumably not in Streamit.
    // similarly, think we do not have catch, throw, finally, 
    // SIRPhaseInvocation -- presumably removed before this pass.
    
    // RegReceiver probably only at top level, but couldn't update pops past it
    // if not at top level.
    public Object visitRegReceiverStatement(SIRRegReceiverStatement self,
            JExpression portal, SIRStream receiver, JMethodDeclaration[] methods) {
        prevPop = null;
        return super.visitRegReceiverStatement(self,portal,receiver,methods);
    }
    
    public Object visitMethodCallExpression(JMethodCallExpression self,
            JExpression prefix,
            String ident,
            JExpression[] args) {
        prevPop = null;
        return super.visitMethodCallExpression(self,prefix,ident,args);  
    }
}

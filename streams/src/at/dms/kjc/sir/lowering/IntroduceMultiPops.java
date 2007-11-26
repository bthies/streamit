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
 * <p>A single SIRPopExpression may represent multiple pops.<br/>
 * This pass takes sequences of 'pop' expressions for which the result is not used
 * and forms a single SIRPopExpression containing the pop count.</p>
 * 
 * 
 * @author Allyn Dimock
 *
 */

// This pass is supposed to take sequences of pop() statements
// and counted loops containing only pop statements and combine
// all logically adjacent pop() statements into a single pop(N)
// statement.
//
// To do this, we have to keep track of non-pop statements.
// when we find a non-pop statement we stop updating the previous
// pop statement (if any) and wait for another pop statement
// to occur in our scan of the code.

public class IntroduceMultiPops extends SLIRReplacingVisitor {
 
    private IntroduceMultiPops() { }

    /** 
     * Set to true to print program before and after this pass.
     */
    public static boolean debug = false;

    /**
     * Walk stream structure and replace multiple pops from single SIRPopExpression.
     * 
     * @param str  a stream to work on
     */
    
    public static void doit(SIRStream str) {
        if (debug) {
            System.err.println("Program before IntroduceMultiPops");
            SIRToStreamIt.run(str,new JInterfaceDeclaration[]{},new SIRInterfaceTable[]{},new SIRStructure[]{});
            System.err.println("End of program before IntroduceMultiPops");
        }
        new IntroduceMultiPops().introduceMultiProps(str);
        if (debug) {
            System.err.println("Program after IntroduceMultiPops");
            SIRToStreamIt.run(str,new JInterfaceDeclaration[]{},new SIRInterfaceTable[]{},new SIRStructure[]{});
            System.err.println("End of program after IntroduceMultiPops");
        }
        
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
            Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
            while (iter.hasNext()) {
                SIRStream child = iter.next();
                introduceMultiProps(child);
            }
        }
        if (str instanceof SIRFilter) {
            for (int i = 0; i < str.getMethods().length; i++) {
                str.getMethods()[i].accept(this);
            }
        }
    }

    // When processing a JBlock, we can simplify it to something else
    // so restore JBlock if needed before reconstructing the method declaration.
    public Object visitMethodDeclaration(JMethodDeclaration self,
            int modifiers, CType returnType, String ident,
            JFormalParameter[] parameters, CClassType[] exceptions,
            JBlock body) {
        if (body != null) {
            JStatement newBody = (JStatement)body.accept(this);
            if (newBody != body) {
                if (!(newBody instanceof JBlock)) {
                    JBlock newBlock = new JBlock();
                    newBlock.addStatement(newBody);
                    newBody = newBlock;
                }
                self.setBody((JBlock)newBody);
            }
        }
        return self;
    }

    // assume that a CompoundStatement is the equivalent of a Block
    // as its documentation states, and mung it to be processed as a Block
    public Object visitCompoundStatement(JCompoundStatement self,
            JStatement[] body) {
        // assumes that Compound statement really is just block without parentheses
        JBlock b = new JBlock(body);
        JStatement newS = (JStatement)visitBlockStatement(b,b.getComments());
        
        if (newS instanceof JBlock) {
            return new JCompoundStatement(self.getTokenReference(),
                    ((JBlock)newS).getStatementArray());
        } else {
            return newS;
        }
    }
    

    // A block may be transformed into a block, or into a single pop
    // statement -- that my be combinable in an outer scope, or
    // a single statement (non-pop) which may have to become a block
    // again in some outer contexts.
    //
    // recurr to process any sub-blocks, then walk the block 
    // combining adjacent pop statements and for loops consisting
    // only of a pop statement.
    public Object visitBlockStatement(JBlock oldBlock,
            JavaStyleComment[] comments) {
        //SIRPopExpression oldPrevPop = prevPop;
        // non-null when combining a sequence of pop statements.
        SIRPopExpression prevPop = null;
        
        JBlock newBlock = (JBlock)super.visitBlockStatement(oldBlock,comments);
        
        List /*<JStatement>*/ oldStatements = newBlock.getStatements();
        List /*<JStatement>*/ simplifiedStatements = new LinkedList();
        for (Iterator i = oldStatements.iterator(); i.hasNext();) {
            Object o = i.next();
	    // null statments (which may not exist, just following others here)
	    // and EmptyStatements do not go into the list of returned 
	    // statements.
            if (o != null && !(o instanceof JEmptyStatement)) {
	        // getting a pop statment, which may have been a sequence
	        // of pop statements in newBlock which could have been 
	        // sequence of pop statments in self.
                if ((o instanceof JExpressionStatement)
                        && ((JExpressionStatement) (o)).getExpression() instanceof SIRPopExpression) {
                    // found a pop.
                    SIRPopExpression popExpr = (SIRPopExpression) ((JExpressionStatement) o)
                            .getExpression();
                    if (prevPop != null && prevPop.getType().equals(popExpr.getType())) {
                        // combine with immediately previous pop if any and elide
                        prevPop.setNumPop(popExpr.getNumPop()
                                + prevPop.getNumPop());
                        continue;
                    } else {
                        // no immediately previous pop in block, but this
                        // could be previous for the next statement.
                        prevPop = popExpr;
                        simplifiedStatements.add(o);
                        continue;
                    } // assert false : "execution does not get here";
                } else { 
                    LoopIterInfo forLoopInfo = null;
                    SIRPopExpression popExpr = null;
                    // determine if for loop with constant number of iterations
                    // the body of which if a single pop statment.  If so
                    // forLoopInfo will be set non-null, else it will remain null.
                    if (o instanceof JForStatement) {
                        JForStatement forStmt = (JForStatement)o;
                        JStatement body = forStmt.getBody();
                        if ((body instanceof JExpressionStatement)
                                && ((JExpressionStatement)body).getExpression() instanceof SIRPopExpression) {
                            popExpr = (SIRPopExpression) ((JExpressionStatement)body).getExpression();
                            forLoopInfo = LoopIterInfo.getLoopInfo(forStmt.getInit(),
                                                                   forStmt.getCondition(),
                                                                   forStmt.getIncrement(),
                                                                   forStmt.getBody(),
                                                                   new HashMap<JLocalVariable, JExpression>(),
                                                                   new HashMap<JLocalVariable, JLiteral>());
                        }
                        // for loop that we are not going to process:
                        // make sure body is a block, otherwise some code generators
                        // (at least rstream) generate incorrect code.
                        if (forLoopInfo == null && ! (body instanceof JBlock)) {
                            JBlock block = new JBlock();
                            block.addStatement(body);
                            forStmt.setBody(block);
                        }
                    }
                    if (forLoopInfo != null) {
                        int popCount = popExpr.getNumPop() * LoopIterInfo.getNumIterations(forLoopInfo);
                        if (popCount > 0) {   // don't do anything of for loop executes 0 times.
                            if (prevPop != null && prevPop.getType().equals(popExpr.getType())) {
                                prevPop.setNumPop(prevPop.getNumPop()+popCount);
                            } else {
                                popExpr.setNumPop(popCount);
                                prevPop = popExpr;
                                simplifiedStatements.add(new JExpressionStatement(popExpr));
                            }
                        }
                    } else {
                        prevPop = null;    // don't combine pops across this statement
                        simplifiedStatements.add(o);
                    }
                }
            }
        }
        // empty block becomes empty statement for surrounding block to elide
        int newLen = simplifiedStatements.size();

        if (newLen == 0) {
            return new JEmptyStatement();
        }
        if (newLen == 1) {
            // single statement
            // so an outer block can see a pop statement if
            // the block reduces to one.
            return simplifiedStatements.get(0);
        } 
        // Return processed block
        return new JBlock(newBlock.getTokenReference(),
                simplifiedStatements,newBlock.getComments());
    }

    // We are very interested in handling a for statement where
    // (1) the body reduces to a pop statement
    // (2) the trip count can be resolved to an integer.
    // Since all real work is done at the block level, make
    // sure the for statement body is a block before processing
    // the for statement.
    public Object visitForStatement(JForStatement self,
            JStatement init,
            JExpression cond,
            JStatement incr,
            JStatement body) {
        
        if (!(body instanceof JBlock)) {
            JBlock newBlock = new JBlock();
            newBlock.addStatement(body);
            self.setBody(newBlock);
        }
        return super.visitForStatement(self,init,cond,incr,body);
    }
}

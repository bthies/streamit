
package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
//import at.dms.util.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
//import at.dms.compiler.JavadocComment;

/**
 * This class unrolls loops where it can.
 */
public class Unroller extends SLIRReplacingVisitor {
    /**
     * Map allowing the current block to access the modified
     * list of the current for loop
     */
    private Hashtable<JLocalVariable, Boolean> currentModified;
    /**
     * Map of known constants (JLocalVariable -> JLiteral)
     */
    private Hashtable<JLocalVariable,JLiteral> constants;
    /**
     * Holds compile time values
     */
    private Hashtable<JLocalVariable, JExpression> values;
    /**
     * Whether or not anything has been unrolled.
     */
    private boolean hasUnrolled;
    /**
     * Whether or not we're in the init function of a container.
     * (Should unroll maximally here because might contain stream
     * children.)
     */
    private boolean inContainerInit;
    /**
     * Whether or not outer loops should be unrolled.  For performance
     * reasons, the defauls is no, but for some applications (e.g.,
     * linear analysis) full unrolling is required.
     */
    private final boolean unrollOuterLoops;

    private int unrollLimit;

    static boolean limitNoTapeLoops = false;
    static int unrollLimitNoTapeLoops = 0;
    
    /**
     * @param b
     * @param limit
     */
    static public void setLimitNoTapeLoops(boolean b, int limit) {
        limitNoTapeLoops = b;
        unrollLimitNoTapeLoops = limit;
    }

    /**
     * Creates one of these given that <i>constants</i> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.
     * @param constants 
     */
    public Unroller(Hashtable<JLocalVariable,JLiteral> constants) {
        this(constants, false);
    }
    /**
     * @param constants
     * @param unrollOuterLoops
     */
    public Unroller(Hashtable<JLocalVariable,JLiteral> constants, boolean unrollOuterLoops) {
        super();
        this.constants = constants;
        this.unrollOuterLoops = unrollOuterLoops;
        this.hasUnrolled = false;
        currentModified=new Hashtable<JLocalVariable, Boolean>();
        values=new Hashtable<JLocalVariable, JExpression>();
        inContainerInit=false;
    }
    
    /**
     * Unrolls <i>filter</i> up to a factor of 100,000.
     * @param filter 
     */
    public static void unrollFilter(SIRFilter filter) {
        unrollFilter(filter, 100000);
    }
    
    /**
     * Unrolls <i>filter</i> up to a factor of unrollFactor.
     * @param filter 
     * @param unrollFactor 
     */
    public static void unrollFilter(SIRFilter filter, int unrollFactor) {
        // set all loops to be unrolled again
        IterFactory.createFactory().createIter(filter).accept(new EmptyStreamVisitor() {
                public void preVisitStream(SIRStream filter, SIRIterator iter) {
                    for (int i=0; i<filter.getMethods().length; i++) {
                        filter.getMethods()[i].accept(new SLIREmptyVisitor() {
                                public void visitForStatement(JForStatement self, JStatement init, JExpression cond,
                                                              JStatement incr, JStatement body) {
                                    self.setUnrolled(false);
                                }
                            });
                    }
                }
            });
        // now do unrolling
        int origUnroll = KjcOptions.unroll;
        boolean origLimitNoTapeLoops = limitNoTapeLoops;

        KjcOptions.unroll = unrollFactor;
        limitNoTapeLoops = false;
        FieldProp.doPropagate(filter, true);

        KjcOptions.unroll = origUnroll;
        limitNoTapeLoops = origLimitNoTapeLoops;
    }

    /**
     * @param init
     */
    public void setContainerInit(boolean init) {
        inContainerInit=init;
    }

    /**
     * @return
     */
    public boolean getContainerInit() {
        return inContainerInit;
    }
    
    /**
     * Run a propagator on every method of a stream.
     * Originally intended to combine proparagion with agressive unrolling, but
     * unrolling has been removed...
     * @param str  Stream in which to unroll loops in methods
     */
    public static void unroll(SIRStream str) {
        if (str instanceof SIRFeedbackLoop)
            {
                SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
                unroll(fl.getBody());
                unroll(fl.getLoop());
            }
        if (str instanceof SIRPipeline)
            {
                SIRPipeline pl = (SIRPipeline)str;
                Iterator iter = pl.getChildren().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = (SIRStream)iter.next();
                        unroll(child);
                    }
            }
        if (str instanceof SIRSplitJoin)
            {
                SIRSplitJoin sj = (SIRSplitJoin)str;
                Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = iter.next();
                        unroll(child);
                    }
            }
        if (str instanceof SIRFilter)
            for (int i = 0; i < str.getMethods().length; i++) {
                //Unroller unroller;
                //Very aggressive
                //Intended as a last and final unroll pass
                //do {
                //do { //Unroll as much as possible
                //unroller=new Unroller(new Hashtable());
                //str.getMethods()[i].accept(unroller);
                str.getMethods()[i].accept(new Propagator(new Hashtable<JLocalVariable,Object>()));
                //  } while(unroller.hasUnrolled());
                //Constant Prop then check to see if any new unrolling can be done
                //str.getMethods()[i].accept(new Propagator(new Hashtable()));
                //unroller=new Unroller(new Hashtable());
                //str.getMethods()[i].accept(unroller);
                //} while(unroller.hasUnrolled());
            }
    }

    /**
     * checks prefix
     */
    public Object visitPrefixExpression(JPrefixExpression self,
                                        int oper,
                                        JExpression expr) {
        if(expr instanceof JLocalVariableExpression) {
            currentModified.put(((JLocalVariableExpression)expr).getVariable(),Boolean.TRUE);
            values.remove(((JLocalVariableExpression)expr).getVariable());
        }
        return super.visitPrefixExpression(self,oper,expr);
    }
    
    /**
     * checks postfix
     */
    public Object visitPostfixExpression(JPostfixExpression self,
                                         int oper,
                                         JExpression expr) {
        if(expr instanceof JLocalVariableExpression){
            currentModified.put(((JLocalVariableExpression)expr).getVariable(),Boolean.TRUE);
            values.remove(((JLocalVariableExpression)expr).getVariable());
        }
        return super.visitPostfixExpression(self,oper,expr);
    }

    /**
     * checks var def
     */
    public Object visitVariableDefinition(JVariableDefinition self,
                                          int modifiers,
                                          CType type,
                                          String ident,
                                          JExpression expr) {
        currentModified.put(self,Boolean.TRUE);
        if(expr instanceof JLiteral) {
            values.put(self,expr);
        }
        return super.visitVariableDefinition(self,modifiers,type,ident,expr);
    }

    /**
     * checks assignment
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                            JExpression left,
                                            JExpression right) {
        if(left instanceof JLocalVariableExpression) {
            currentModified.put(((JLocalVariableExpression)left).getVariable(),Boolean.TRUE);
            if(right instanceof JLiteral) {
                values.put(((JLocalVariableExpression)left).getVariable(),right);
            }
        }
        return super.visitAssignmentExpression(self,left,right);
    }

    /**
     * Overload the for-statement visit.
     */
    public Object visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {

        // to do the right thing if someone set an unroll factor of 0
        // (or 1, which means to do nothing)

        if((KjcOptions.unroll>1 || inContainerInit) && !self.getUnrolled()) { //Ignore if already unrolled

            // first recurse into body...
            Hashtable<JLocalVariable, Boolean> saveModified=currentModified;
            currentModified=new Hashtable<JLocalVariable, Boolean>();
            // we're going to see if any child unrolls, to avoid
            // unrolling doubly-nested loops
            boolean saveHasUnrolled = hasUnrolled;
            hasUnrolled = false;

            JStatement newStmt = (JStatement)body.accept(this);
            if (newStmt!=null && newStmt!=body) {
                self.setBody(newStmt);
            }
        
            boolean childHasUnrolled = hasUnrolled;
            // restore this way because you want to propagate child
            // unrollings up, but don't want to eliminate record of
            // previous unrolling
            hasUnrolled = saveHasUnrolled || childHasUnrolled;

            // if we are not in init then limit unroll factor for loops that
            // do not have tape operations in them
            unrollLimit = KjcOptions.unroll;
            if (limitNoTapeLoops && !inContainerInit) {
                boolean tape_op = FindTapeOps.findTapeOps(body);
                if (!tape_op) {
                    if (KjcOptions.unroll > unrollLimitNoTapeLoops) {
                        unrollLimit = unrollLimitNoTapeLoops;
                    }
                }
            }

            // only unroll if we're set to unroll outer loops, or if
            // child hasn't unrolled, or if we're doing the init
            // function
            if (unrollOuterLoops || !childHasUnrolled || inContainerInit) {
                // check for loop induction variable
                LoopIterInfo info = LoopIterInfo.getLoopInfo(init, cond, incr, body,values,constants);
                // Unroller doesn't deal with loops that have declarations in the
                // init portion of the loop
                if (info != null && info.getIsDeclaredInInit()) info = null;
                // see if we can unroll...
                if(shouldUnroll(info, body, currentModified)) {
                    // Set modified
                    saveModified.putAll(currentModified);
                    currentModified=saveModified;
                    currentModified.put(info.getVar(),Boolean.TRUE);
                    // do unrolling
                    return doUnroll(info, self);
                } else if(canUnroll(info,currentModified)) {
                    // Set modified
                    saveModified.putAll(currentModified);
                    currentModified=saveModified;
                    currentModified.put(info.getVar(),Boolean.TRUE);
                    // do unrolling
                    return doPartialUnroll(info, self);
                }
            } else {
                // otherwise, still mark the loop as having unrolled,
                // because we don't want to consider it again and
                // unroll it when children were unrolled by a
                // different unroller
                if (!childHasUnrolled)
                    self.setUnrolled(true);
            }
            saveModified.putAll(currentModified);
            currentModified=saveModified;
        }
        return self;
    }

    /**
     * Returns whether or not we should unroll a loop with unrollinfo
     * <info>, body <body> and <currentModified> as in
     * visitForStatement.
     */
    private boolean shouldUnroll(LoopIterInfo info, JStatement body, Hashtable<JLocalVariable, Boolean> currentModified) {
        // if no unroll info or variable is modified in loop, fail
        if (info==null || currentModified.containsKey(info.getVar())) {
            return false;
        }

        //Unroll if in init
        if(inContainerInit)
            return true;
    
        /*
        // otherwise if there is an SIRInitStatement in the loop, then
        // definately unroll for the sake of graph expansion
        final boolean[] hasInit = { false };
        body.accept(new SLIREmptyVisitor() {
        public void visitInitStatement(SIRInitStatement self,
        SIRStream target) {
        super.visitInitStatement(self, target);
        hasInit[0] = true;
        }
        });
        if (hasInit[0]) {
        return true;
        }
        */

        // Unroll maximally for number gathering
        if(KjcOptions.numbers>0) {
            final boolean[] hasPrint = { false };
            body.accept(new SLIREmptyVisitor() {
                    public void visitPrintStatement(SIRPrintStatement self,
                                                    JExpression arg) {
                        hasPrint[0]=true;
                        super.visitPrintStatement(self,arg);
                    }
                });
            if (hasPrint[0]) {
                return true;
            }
        }

        // otherwise calculate how many times the loop will execute,
        // and only unroll if it is within our max unroll range
        int count = LoopIterInfo.getNumIterations(info);

        return count <= unrollLimit;
    }

    /**
     * Failing shouldUnroll (completely) this determines if the loop can
     * be unrolled partially
     */
    private boolean canUnroll(LoopIterInfo info, Hashtable<JLocalVariable, Boolean> currentModified) {
        if (info==null || currentModified.containsKey(info.getVar())) {
            return false;
        }
        return true;
    }

    /**
     * Returns the number of times a for-loop with the given
     * characteristics will execute, or -1 if the count cannot be
     * determined.
     * @param init 
     * @param cond 
     * @param incr 
     * @param body 
     * @return 
     */
    public static int getNumExecutions(JStatement init,
                                       JExpression cond,
                                       JStatement incr,
                                       JStatement body) {
        LoopIterInfo info = LoopIterInfo.getLoopInfo(init, cond, incr, body,
                new Hashtable<JLocalVariable, JExpression>(),
                new Hashtable<JLocalVariable,JLiteral>());
        // Make sure has old behavior: no executions of loop that still has declaration
        // in init.
        if (info != null && info.getIsDeclaredInInit()) info = null;
        return LoopIterInfo.getNumIterations(info);
    }

     /**
     * Given the loop <self> and original unroll info <info>, perform
     * the unrolling and return a statement block of the new
     * statements.
     */
    private JBlock doUnroll(LoopIterInfo info, JForStatement self) {
        // make a list of statements
        List<JStatement> statementList = new LinkedList<JStatement>();
        statementList.add(self.getInit());
        // get the initial value of the counter
        int counter = info.getInitVal();
        // simulate execution of the loop...
        Propagator prop=new Propagator(new Hashtable<JLocalVariable,Object>());
        while (LoopIterInfo.inRange(counter,info)) {
            // replace induction variable with its value current value
            prop.getConstants().put(info.getVar(), new JIntLiteral(counter));
            // do the replacement
            JStatement newBody =
                (JStatement)ObjectDeepCloner.deepCopy(self.getBody());
            newBody.accept(prop);
            // add to statement list
            statementList.add(newBody);
            // increment counter
            counter = LoopIterInfo.incrementCounter(counter, info);
        }
        statementList.add(new JExpressionStatement(self.getTokenReference(),new JAssignmentExpression(self.getTokenReference(),new JLocalVariableExpression(self.getTokenReference(),info.getVar()),new JIntLiteral(counter)),null));
        /*  Hashtable cons=prop.getConstants();
            Enumeration enum=prop.getChanged().keys();
            while(enum.hasMoreElements()) {
            JLocalVariable var=(JLocalVariable)enum.nextElement();
            Object val=cons.get(var);
            if(val instanceof JLiteral)
            statementList.add(new JExpressionStatement(null,new JAssignmentExpression(null,new JLocalVariableExpression(null,var),(JLiteral)val),null));
            System.err.println(var+"="+val);
            }*/
        // mark that we've unrolled
        this.hasUnrolled = true;
        // return new block instead of the for loop
        constants.remove(info.getVar());
        return new JBlock(null, 
                          statementList.
                          toArray(new JStatement[0]),
                          null);
    }
    
    /**
     * Repeats body KjcOptions.unroll times and adds post loop guard
     */
    private JBlock doPartialUnroll(final LoopIterInfo info, JForStatement self) {
        int numExec=LoopIterInfo.getNumIterations(info);
        //int numLoops=numExec/KjcOptions.unroll;
        int remain=numExec%unrollLimit;
        JStatement[] newBody=new JStatement[unrollLimit];
        //if(newBody.length>=2) {
        //newBody[0]=self.getBody();
        //newBody[1]=self.getIncrement();
        //}
        if(unrollLimit>=1) {
            JStatement cloneBody=(JStatement)ObjectDeepCloner.deepCopy(self.getBody());
            newBody[0]=cloneBody;
        }
        {
            final JLocalVariable inductVar=info.getVar();
            final int incrVal=info.getIncrVal();
            for(int i=1;i<unrollLimit;i++) {
                JStatement cloneBody=(JStatement)ObjectDeepCloner.deepCopy(self.getBody());
                //JStatement cloneIncr=(JStatement)ObjectDeepCloner.deepCopy(makeIncr(info,info.incrVal));
                final int incremented=i;
                cloneBody.accept(new SLIRReplacingVisitor() {
                        public Object visitLocalVariableExpression(JLocalVariableExpression self2,
                                                                   String ident) {
                            if(inductVar.equals(self2.getVariable())) {
                                return LoopIterInfo.makeIncreased(info,incremented*incrVal);
                            } else
                                return self2;
                        }  
                    });
                newBody[i]=cloneBody;
            }
        }
        JBlock body=new JBlock(null,newBody,null);
        JStatement[] newStatements=new JStatement[2*remain+2];
        newStatements[0]=self.getInit();
        int result=info.getInitVal();
        for(int i=1;i<2*remain+1;i++) {
            JStatement cloneBody=(JStatement)ObjectDeepCloner.deepCopy(self.getBody());
            JStatement cloneIncr=(JStatement)ObjectDeepCloner.deepCopy
              (LoopIterInfo.makeIncrAssignment(info,info.getIncrVal()));
            newStatements[i]=cloneBody;
            i++;
            newStatements[i]=cloneIncr;
            result=LoopIterInfo.incrementCounter(result,info);
        }
        JForStatement newFor = new JForStatement(null,
                new JExpressionStatement(null, new JAssignmentExpression(null,
                        new JLocalVariableExpression(null, info.getVar()),
                        new JIntLiteral(result)), null),

                self.getCondition(),
                LoopIterInfo.makeIncrAssignment(info, unrollLimit * info.getIncrVal()), body,
                new JavaStyleComment[] {
                    new JavaStyleComment("Unroller", true,
                            false, false)});
        newFor.setUnrolled(true);
        newStatements[newStatements.length-1]=newFor;
        // mark that we've unrolled
        this.hasUnrolled = true;
        return new JBlock(null,
                          newStatements,
                          null);
    }
    
    
    /**
     * Return whether or not this has unrolled any loops.
     * @return whether or not this has unrolled any loops
     */
    public boolean hasUnrolled() {
        return hasUnrolled;
    }

}


package at.dms.kjc.spacedynamic;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.common.*;

//if 
//not 2 stage
//peek == pop
//no peek expression 
//all pops before pushe

public class DirectCommunication extends at.dms.util.Utils implements Constants {
    private SIRFilter filter;

    private FlatNode node;

    private SpdStaticStreamGraph ssg;

    /** true if this filter is the source of a static stream graph * */
    private boolean dynamicInput;

    //the multiplicity of the filter in the init stage
    private static int initMult = 0;
    

    /**
     * See if we can generate input communication channel code without using a
     * buffer.  If true, then we can, and we have already generated the code. This 
     * assumes that bottomPeek == remaining == 0
     * 
     * @param SSG
     * @param node
     * @return True if we can generate buffer-less code and we just did, or 
     * false we cannot, so we have to generate a code using a buffer in 
     * Buffered*Communication.java
     */
    public static boolean doit(SpdStaticStreamGraph SSG, FlatNode node) {
        DirectCommunication dc = new DirectCommunication(SSG, node);
        // see if we can generate direct communication
        if (dc.canRun())
            return dc.run();
        // can't so return false...
        return false;
    }

    private DirectCommunication(SpdStaticStreamGraph SSG, FlatNode node) {
        this.ssg = SSG;
        this.node = node;
        this.filter = (SIRFilter) node.contents;
        // if this is a source of an ssg then set dynamicInput
        dynamicInput = ssg.isInput(node) || SSG.simulator instanceof NoSimulator;
    }

    private boolean canRun() {
        // runs some tests to see if we can
        // generate code direct commmunication code
        if (KjcOptions.ratematch)
            return false;
        if (filter instanceof SIRTwoStageFilter)
            return false;
        if (PeekFinder.findPeek(filter.getWork())) {
            //System.out.println("*** Found Peek! ***");
            return false;
        }
        if (filter.getPeekInt() > filter.getPopInt())
            return false;
        if (!dynamicInput && PeekPopPushInHelper.check(filter))
            return false;
        // must popping a scalar
        if (filter.getInputType().isClassType()
            || filter.getInputType().isArrayType())
            return false;
        // must be pushing a scalar
        if (filter.getOutputType().isClassType()
            || filter.getOutputType().isArrayType())
            return false;
        // for a filter with dynamic input we don't care if the pushes and
        // pops are intermixed, because the pops will use the dynamic network
        // and the switch will only be used for the pushes...
        if (!dynamicInput && PushBeforePop.check(filter.getWork()))
            return false;
        // all tests pass
        return true;
    }

    private boolean run() {
        // convert the communication
        // all the communication is in the work function
        filter.getWork().accept(new DirectConvertCommunication(dynamicInput));
        // generate the raw main function
        rawMainFunction(filter);
        return true;
    }

    private void rawMainFunction(SIRFilter filter) {
        JBlock statements = new JBlock();

        //index variable for loop of work function in init stage
        JVariableDefinition exeIndex1Var = 
            new JVariableDefinition(null, 
                                    0, 
                                    CStdType.Integer,
                                    RawExecutionCode.exeIndex1,
                                    null);


        // create the params list, for some reason
        // calling toArray() on the list breaks a later pass
        System.out.println(filter.getParent());
        List paramList = filter.getParams();
        JExpression[] paramArray;
        if (paramList == null || paramList.size() == 0)
            paramArray = new JExpression[0];
        else
            paramArray = (JExpression[]) paramList.toArray(new JExpression[0]);

        //get the init multiplicty
        initMult = ssg.getMult(node, true);

        //if we execute in the init stage, then create the local to index the for loop
        if (initMult > 0) {
            statements.addStatement
                (new JVariableDeclarationStatement(null,
                                                   exeIndex1Var,
                                                   null));  
        }
    
        //if standalone, add a field for the iteration counter...
        JFieldDeclaration iterationCounter = null;
        if (KjcOptions.standalone) {
            iterationCounter = 
                new JFieldDeclaration(new JVariableDefinition(0,
                                                              CStdType.Integer, 
                                                              FlatIRToC.MAINMETHOD_COUNTER,
                                                              new JIntLiteral(-1)));
            filter.addField(iterationCounter);
        }
    

        // add the call to the init function
        statements.addStatement(new JExpressionStatement(null,
                                                         new JMethodCallExpression(null, new JThisExpression(null),
                                                                                   filter.getInit().getName(), paramArray), null));
    
        //if we execute in the init stage, then create the loop'ed work function
        if (initMult > 0) {
            //inline the work function in a while loop
            JBlock workInitBlock = RawExecutionCode.executeWorkFunction(filter);
                
            //call work function for init stage????
            statements.addStatement
                (RawExecutionCode.makeForLoop(workInitBlock, 
                                              exeIndex1Var,
                                              new JIntLiteral(initMult)));
        }
    

        if (!IMEMEstimation.TESTING_IMEM) {
            //add call to raw_init2, only if not testing imem
            statements.addStatement(new JExpressionStatement(null,
                                                             new JMethodCallExpression
                                                             (null, 
                                                              new JThisExpression(null),
                                                              SwitchCode.SW_SS_TRIPS,
                                                              new JExpression[0]),
                                                             null));
        }

        // inline the work function in a while loop
        JBlock workBlock = RawExecutionCode.executeWorkFunction(filter);

        if (SpaceDynamicBackend.FILTER_DEBUG_MODE) {
            statements.addStatement(new SIRPrintStatement(null,
                                                          new JStringLiteral(null, filter.getName()
                                                                             + " Starting Steady-State\\n"), null));
        }

        // if we are in decoupled mode do not put the work function in a for
        // loop
        // and add the print statements
        if (KjcOptions.decoupled || RawWorkEstimator.SIMULATING_WORK) {
            workBlock.addStatementFirst(new SIRPrintStatement(null,
                                                              new JIntLiteral(0), null));
            workBlock.addStatement(workBlock.size(), new SIRPrintStatement(
                                                                           null, new JIntLiteral(1), null));
            statements.addStatement(workBlock);
        } else {
            statements.addStatement
                (new JWhileStatement
                 (null, 
                  KjcOptions.standalone ?  
                  (JExpression) new JPostfixExpression(null, 
                                                       Constants.OPE_POSTDEC, 
                                                       new JFieldAccessExpression(new JThisExpression(null), 
                                                                                  FlatIRToC.MAINMETHOD_COUNTER)) :
                  (JExpression)new JBooleanLiteral(null, true),
                  workBlock, 
                  null));
        }

        JMethodDeclaration rawMainFunct = new JMethodDeclaration(null,
                                                                 at.dms.kjc.Constants.ACC_PUBLIC, CStdType.Void,
                                                                 RawExecutionCode.rawMain, JFormalParameter.EMPTY,
                                                                 CClassType.EMPTY, statements, null, null);
        filter.addMethod(rawMainFunct);

    }

    static class DirectConvertCommunication extends SLIRReplacingVisitor {
        private boolean dynamic;

        public DirectConvertCommunication(boolean dynamicInput) {
            dynamic = dynamicInput;
        }

        public Object visitAssignmentExpression(JAssignmentExpression oldself,
                                                JExpression oldleft, JExpression oldright) {
            // a little optimization, use the pointer version of the
            // structure's pop in struct.h to avoid copying
            if (oldright instanceof JCastExpression
                && (((JCastExpression) oldright).getExpr() instanceof SIRPopExpression)) {
                SIRPopExpression pop = (SIRPopExpression) ((JCastExpression) oldright)
                    .getExpr();

                if (pop.getType().isClassType()) {
                    JExpression left = (JExpression) oldleft.accept(this);

                    JExpression[] arg = { left };

                    JMethodCallExpression receive = new JMethodCallExpression(
                                                                              null, new JThisExpression(null),
                                                                              RawExecutionCode.structReceivePrefix
                                                                              + (dynamic ? "Dynamic" : "Static")
                                                                              + pop.getType(), arg);
                    receive.setTapeType(pop.getType());
                    return receive;
                }
                if (pop.getType().isArrayType()) {
                    return null;
                }
            }

            // otherwise do the normal thing
            JExpression self = (JExpression) super.visitAssignmentExpression(
                                                                             oldself, oldleft, oldright);
            return self;
        }

        public Object visitPopExpression(SIRPopExpression oldSelf,
                                         CType oldTapeType) {

            // do the super
            SIRPopExpression self = (SIRPopExpression) super
                .visitPopExpression(oldSelf, oldTapeType);

            // if this is a struct, use the struct's pop method, generated in
            // struct.h
            if (self.getType().isClassType()) {
                JMethodCallExpression receive = new JMethodCallExpression(null,
                                                 new JThisExpression(null), "pop" + self.getType(),
                                                 new JExpression[0]);
                receive.setTapeType(self.getType());
                return receive;
            } else if (self.getType().isArrayType()) {
                return null;
            } else {
                JMethodCallExpression receive = new JMethodCallExpression(null,
                                                                          new JThisExpression(null),
                                                                          RawExecutionCode.receiveMethod, new JExpression[0]);
                receive.setTapeType(self.getType());
                return receive;
            }
            /*
             * else { if (self.getType().isFloatingPoint()) return new
             * JLocalVariableExpression (null, new JGeneratedLocalVariable(null,
             * 0, CStdType.Float, dynamic ? Util.CGNIFPVAR : Util.CSTIFPVAR,
             * null)); else return new JLocalVariableExpression (null, new
             * JGeneratedLocalVariable(null, 0, CStdType.Integer, dynamic ?
             * Util.CGNIINTVAR : Util.CSTIINTVAR, null)); }
             */
        }

        public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                          CType oldTapeType, JExpression oldArg) {
            Utils.fail("Should not see a peek expression when generating "
                       + "direct communication");
            return null;
        }
    }
    
    

    static class PushBeforePop extends SLIREmptyVisitor {
        private static boolean sawPush;

        private static boolean pushBeforePop;

        public static boolean check(JMethodDeclaration method) {
            sawPush = false;
            pushBeforePop = false;

            method.accept(new PushBeforePop());
            return pushBeforePop;
        }

        public void visitPeekExpression(SIRPeekExpression self, CType tapeType,
                                        JExpression arg) {
            Utils.fail("Should not see a peek expression");
        }

        public void visitPopExpression(SIRPopExpression self, CType tapeType) {
            if (sawPush)
                pushBeforePop = true;
        }

        public void visitPushExpression(SIRPushExpression self, CType tapeType,
                                        JExpression arg) {
            arg.accept(this);
            sawPush = true;
        }

        // for all loops, visit the cond and body twice to make sure that
        // if a push statement occurs in the body and
        // after all the pops, we will flag this as a
        // case where a push comes before a pop

        public void visitWhileStatement(JWhileStatement self, JExpression cond,
                                        JStatement body) {
            cond.accept(this);
            body.accept(this);
            // second pass
            cond.accept(this);
            body.accept(this);
        }

        public void visitForStatement(JForStatement self, JStatement init,
                                      JExpression cond, JStatement incr, JStatement body) {
            if (init != null) {
                init.accept(this);
            }
            if (cond != null) {
                cond.accept(this);
            }
            if (incr != null) {
                incr.accept(this);
            }
            body.accept(this);
            // second pass
            if (cond != null) {
                cond.accept(this);
            }
            if (incr != null) {
                incr.accept(this);
            }
            body.accept(this);
        }

        public void visitDoStatement(JDoStatement self, JExpression cond,
                                     JStatement body) {
            body.accept(this);
            cond.accept(this);
            // second pass
            body.accept(this);
            cond.accept(this);
        }
    }

    static class PeekFinder extends SLIREmptyVisitor {
        private static boolean found;

        public static boolean findPeek(JMethodDeclaration method) {
            found = false;
            method.accept(new PeekFinder());
            return found;
        }

        /**
         * if we find a peek expression set found to true;
         */
        public void visitPeekExpression(SIRPeekExpression self, CType tapeType,
                                        JExpression arg) {
            found = true;
        }
    }
}

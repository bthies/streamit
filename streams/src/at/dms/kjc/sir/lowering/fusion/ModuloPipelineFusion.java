package at.dms.kjc.sir.lowering.fusion;

//import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
//import at.dms.kjc.sir.lowering.partition.*;
//import at.dms.kjc.lir.*;

//import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This class fuses pipelines by simulating their execution in a
 * single-appearance loop nest.  The intermediate values are stored in
 * a buffer that is referenced using a wraparound counter.  The
 * producer keeps track of the push_index (location in buffer where
 * next item will be pushed) and the consumer keeps track of a
 * pop_index (location in buffer where next item will be popped).
 *
 * This pass differs from the Shift fusion in that this class uses a
 * single buffer with modulo operations.  This class is arguably the
 * more simple implementation (even though it was implemented later,
 * chronologically speaking).
 *
 * Some implementation notes:
 *
 * literals pre-computed and inlined by compiler:
 *  - buffer_size (power of 2 above peeking done by downstream filter
 *    in SAS schedule)
 *
 * new fields in fused filter:
 *  - TYPE[buffer_size] buffer
 *  - int push_index
 *  - int pop_index
 *
 * new statements in init function:
 *  - buffer = new TYPE[buffer_size]
 *  - push_index = 0;
 *  - pop_index = 0;
 *
 * translations in work and prework:
 *  - push(val) -> buffer[push_index] = val; push_index = (push_index + 1) % buffer_size;
 *  - peek(i) -> buffer[(pop_index+i) % buffer_size]
 *  - pop(i) -> buffer[pop_index]; pop_index = (pop_index + 1) % buffer_size
 *
 *  NOTE: mod's are implemented as bitmask
 *
 * Another implementation note: it is rather complicated to replace an
 * expression with an expression followed by a statement.  You have to
 * keep track of the statement S enclosing the original expression and
 * put the increment statement after S.  This turns out to be a lot of
 * cases.  The alternative is letting the index point to the PREVIOUS
 * item pushed, popped, etc., but then you have to add 1 for all the
 * peek indexing, which would be too much overhead.
 *
 * Additional optimizations to implement:
 * - only have a single variable for all work function loop counters
 *   (may or may not help with register allocation)
 *
 */
class ModuloPipelineFusion {
    /**
     * This is the name of the field that is used to hold the items
     * that are buffered.
     */
    private static final String BUFFER_NAME = "___BUFFER";

    /**
     * The name of the counter that is used to write into buffers.
     */
    private static final String PUSH_INDEX_NAME = "___PUSH_INDEX";

    /**
     * The name of the counter that is used to read from buffers.
     */
    private static final String POP_INDEX_NAME = "___POP_INDEX";

    /**
     * The name of the counter that is used to count work executions
     * of a phase.
     */
    private static final String COUNTER_NAME_WORK = "___COUNTER_WORK";

    /**
     * The name of the initial work function.
     */
    public static final String INIT_WORK_NAME() { return FusePipe.INIT_WORK_NAME; }

    /**
     * Prefix for name of parameters in fused init function.
     */
    protected static final String INIT_PARAM_NAME = "___param";

    /**
     * The fundamental fuse operation.
     *
     * Fuses ALL children of pipe, requiring that they are fusable
     * filters.  Leaves just a single filter <f>, which will be
     * <pipe>'s only child following this call.
     */
    public static void doFusion(SIRPipeline pipe) {
        // inline phases
        for (int i=0; i<pipe.size(); i++) {
            InlinePhases.doit((SIRFilter)pipe.get(i));
        }

        // construct set of filter info
        List<FilterInfo> filterInfo = makeFilterInfo(pipe);

        InitFuser initFuser;

        // make the initial work function
        JMethodDeclaration initWork =  makeWork(filterInfo, true);
    
        // make the steady-state work function
        JMethodDeclaration steadyWork = makeWork(filterInfo, false);
    
        // make the fused init functions
        initFuser = makeInitFunction(filterInfo);
    
        // fuse all other fields and methods
        SIRFilter result = makeFused(filterInfo, initFuser.getInitFunction(), initWork, steadyWork);

        // insert the fused filter in the parent
        FusePipe.replace(pipe, result, initFuser.getInitArgs());
    }

    /**
     * Tabulates info on <filterList> that is needed for fusion.
     */
    private static List<FilterInfo> makeFilterInfo(SIRPipeline pipe) {
        // make the result
        List<FilterInfo> result = new LinkedList<FilterInfo>();
        // get execution counts for <pipe>
        HashMap[] execCount = SIRScheduler.getExecutionCounts(pipe);

        // for each filter...
        for (int i=0; i<pipe.size(); i++) {
            // the filter
            SIRFilter filter = (SIRFilter)pipe.get(i);

            // push index
            JVariableDefinition pushVar = new JVariableDefinition(CStdType.Integer,
                                                                  PUSH_INDEX_NAME + "_" + i);
            JFieldDeclaration pushIndex = new JFieldDeclaration(pushVar);
        
            // pop index
            JVariableDefinition popVar = new JVariableDefinition(CStdType.Integer,
                                                                 POP_INDEX_NAME + "_" + i);
            JFieldDeclaration popIndex = new JFieldDeclaration(popVar);
        
            // number of executions (num[0] is init, num[1] is steady)
            int[] num = new int[2];
            for (int j=0; j<2; j++) {
                int[] count = (int[])execCount[j].get(filter);
                if (count==null) {
                    num[j] = 0;
                } else {
                    num[j] = count[0];
                }
            }

            // calculate how much data we should buffer between the
            // i'th and (i-1)'th filter.  This part of the code is
            // ready for two stage filters even though they might not
            // be passed here yet.
            int bufferSize;
            if (i==0) {
                // for the first filter, don't need any buffer
                bufferSize = 0;
            } else {
                // otherwise, need however many were left over during
                // initialization between this filter and the last
                FilterInfo last = result.get(result.size()-1);
                int lastProduce = 0;
                // need to count first execution of a two-stage filter separately
                if (last.filter instanceof SIRTwoStageFilter &&
                    last.init.num > 0) {
                    lastProduce = ((SIRTwoStageFilter)last.filter).getInitPushInt() + 
                        (last.init.num-1) * last.filter.getPushInt();
                } else {
                    lastProduce = last.init.num * last.filter.getPushInt();
                }
                // then add what is produced in steady state
                lastProduce += last.steady.num * last.filter.getPushInt();
                // take the next power of 2
                bufferSize = Utils.nextPow2(lastProduce);
            }

            // get ready to make rest of phase-specific info
            JVariableDefinition loopCounterWork[] = new JVariableDefinition[2];
        
            for (int j=0; j<2; j++) {
                // the exec counter
                loopCounterWork[j] = 
                    new JVariableDefinition(CStdType.Integer,
                                            COUNTER_NAME_WORK + "_" + j + "_" +i);
            }

            // the buffer
            JVariableDefinition bufferVar = 
                new JVariableDefinition(at.dms.kjc.Constants.ACC_FINAL,
                                        new CArrayType(Utils.voidToInt(filter.
                                                                       getInputType()), 
                                                       1 /* dimension */,
                                                       new JExpression[] { new JIntLiteral(bufferSize) } ),
                                        BUFFER_NAME + "_" + i);
            JFieldDeclaration buffer = new JFieldDeclaration(bufferVar);

            // add a filter info to <result>
            result.add(new FilterInfo(filter, buffer, pushIndex, popIndex, bufferSize, 
                                      new PhaseInfo(num[0], loopCounterWork[0]),
                                      new PhaseInfo(num[1], loopCounterWork[1])
                                      ));
        }
        // return result
        return result;
    }
    
    /**
     * Builds the work function for <filterList>, where <init>
     * indicates whether or not we're doing the initialization work
     * function.  If in init mode and there are no statements in the
     * work function, then it returns null instead (to indicate that
     * initWork is not needed.)
     */
    private static JMethodDeclaration makeWork(List<FilterInfo> filterInfo, boolean init) {
        // make a statement list for the init function
        JBlock statements = new JBlock();

        // add the variable declarations
        makeWorkDecls(filterInfo, statements, init);

        // add the work statements
        int before = statements.size();
        makeWorkBody(filterInfo, statements, init);
        int after = statements.size();

        if (after-before==0 && init) {
            // return null to indicate empty initWork function
            return null;
        } else {
            // return result
            return new JMethodDeclaration(CStdType.Void,
                                          RenameAll.newName(init ? INIT_WORK_NAME() : "work"),
                                          JFormalParameter.EMPTY,
                                          statements);

        }
    }

    /**
     * Adds local variable declarations to <statements> that are
     * needed by <filterInfo>.  If <init> is true, it does it for init
     * phase; otherwise for steady phase.
     */
    private static void makeWorkDecls(List<FilterInfo> filterInfo,
                                      JBlock statements,
                                      boolean init) {
        // add declarations for each filter
        for (ListIterator<FilterInfo> it = filterInfo.listIterator(); it.hasNext(); ) {
            FilterInfo info = it.next();
            // get list of local variable definitions from <filterInfo>
            List locals = 
                init ? info.init.getVariables() : info.steady.getVariables();
            // go through locals, adding variable declaration
            for (ListIterator loc = locals.listIterator(); loc.hasNext(); ) {
                // get local
                JVariableDefinition local = 
                    (JVariableDefinition)loc.next();
                // add variable declaration for local
                statements.
                    addStatement(new JVariableDeclarationStatement(local));
            }
        }
    }

    /**
     * Adds the body of the work function.  <init> indicates whether
     * or not this is the initial run of the work function instead of
     * the steady-state version.
     */
    private static void makeWorkBody(List<FilterInfo> filterInfo, 
                                     JBlock statements,
                                     boolean init) {

//        FindVarDecls findVarDecls = new FindVarDecls();

        // for all the filters...
        for (int i=0; i<filterInfo.size(); i++) {
            FilterInfo cur = filterInfo.get(i);
            PhaseInfo curPhase = init ? cur.init : cur.steady;
            // we'll only need the "next" fields if we're not at the
            // end of a pipe.
            FilterInfo next = null;
            PhaseInfo nextPhase = null;
            // get the next fields
            if (i<filterInfo.size()-1) {
                next = filterInfo.get(i+1);
                nextPhase = init ? next.init : next.steady;
            }

            // if the current filter doesn't execute at all, continue
            // (FIXME this is part of some kind of special case for 
            // filters that don't execute at all in a schedule, I think.)
            if (curPhase.num!=0) {

                // if in the steady-state phase, restore the peek values
                SIRFilter filter=cur.filter;
                // get the filter's work function
                JMethodDeclaration work = filter.getWork();
                // take a deep breath and clone the body of the work function
                JBlock oldBody = new JBlock(work.getStatements());
                JBlock body = (JBlock)ObjectDeepCloner.deepCopy(oldBody);

//                if (KjcOptions.rename1) {
//                    body = (JBlock)findVarDecls.findAndReplace(body);
//                }

                // mutate <statements> to make them fit for fusion
                FusingVisitor fuser = new FusingVisitor(cur, next, i!=0, i!=filterInfo.size()-1);
                body.accept(fuser);
                if(init&&(filter instanceof SIRTwoStageFilter)) {
                    JMethodDeclaration initWork = ((SIRTwoStageFilter)filter).getInitWork();
                    // take a deep breath and clone the body of the work function
                    JBlock oldInitBody = new JBlock(initWork.getStatements());
                    JBlock initBody = (JBlock)ObjectDeepCloner.deepCopy(oldInitBody);
                    // mutate <statements> to make them fit for fusion
                    fuser = new FusingVisitor(cur, next, i!=0, i!=filterInfo.size()-1);
                    initBody.accept(fuser);
                    statements.addStatement(initBody);
                    if(curPhase.num>1) {
                        JStatement loop = makeForLoop(body,
                                                      curPhase.loopCounterWork,
                                                      new JIntLiteral(curPhase.num-1));
                        statements.addStatement(Utils.peelMarkers(loop));
                    }
                } else {
                    // get <body> into a loop in <statements>
                    JStatement loop = makeForLoop(body,
                                                  curPhase.loopCounterWork,
                                                  new JIntLiteral(curPhase.num));
                    statements.addStatement(Utils.peelMarkers(loop));
                }
            } else {
                if(init&&(cur.filter instanceof SIRTwoStageFilter))
                    System.err.println("Warning: Two-Stage filter "+cur.filter+" did not fire in init phase.");
            }
        }

//        //add variable declarations calculated by FindVarDecls
//        if (KjcOptions.rename1) {
//            findVarDecls.addVariableDeclarations(statements);
//        }
    }

    /**
     * Returns a for loop that uses local variable <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns the initial assignment statement.
     */
    private static JStatement makeForLoop(JStatement body,
                                          JLocalVariable var,
                                          JExpression count) {
        // if count==0, just return empty statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // return empty statement
                return new JEmptyStatement();
            }
            if (intCount==1) {
                return body;
            }
        }
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = {
            new JAssignmentExpression(new JLocalVariableExpression(var),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(initExpr);
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(Constants.OPE_LT,
                                      new JLocalVariableExpression(var),
                                      count);
        JExpression incrExpr = 
            new JPostfixExpression(Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(var));
        JStatement incr = new JExpressionStatement(incrExpr);

        return new JForStatement(init, cond, incr, body);
    }

    /**
     * Returns an init function that is the combinatio of those in
     * <filterInfo> and includes a call to <initWork>.  Also patches
     * the parent's init function to call the new one, given that
     * <result> will be the resulting fused filter.
     */
    private static InitFuser makeInitFunction(List<FilterInfo> filterInfo) {
        // make an init function builder out of <filterList>
        InitFuser initFuser = new InitFuser(filterInfo);
    
        // do the work on the parent
        initFuser.doit((SIRPipeline)filterInfo.get(0).filter.getParent());

        // make the finished initfuser
        return initFuser;
    }

    /**
     * Returns an array of the fields that should appear in filter
     * fusing all in <filterInfo>.
     */
    private static JFieldDeclaration[] getFields(List<FilterInfo> filterInfo) {
        // make result
        List<JFieldDeclaration> result = new LinkedList<JFieldDeclaration>();
        // add the buffer's and the list of fields from each filter
        int i=0;
        for (ListIterator<FilterInfo> it = filterInfo.listIterator(); it.hasNext(); i++) {
            FilterInfo info = it.next();
            // ignore buffer and pop index for first filter, as it
            // reads from the input with peek/pop operations
            if (i!=0) {
                // do not add buffer if it is empty
                if (info.bufferSize > 0) {
                    result.add(info.buffer);
                }
                result.add(info.popIndex);
            }
            // don't add push counter for last filter, as it writes
            // directly
            if (it.hasNext()) {
                result.add(info.pushIndex);
            }
            result.addAll(Arrays.asList(info.filter.getFields()));
        }
        // return result
        return result.toArray(new JFieldDeclaration[0]);
    }

    /**
     * Returns an array of the methods fields that should appear in
     * filter fusing all in <filterInfo>, with extra <init>, <initWork>, 
     * and <steadyWork> appearing in the fused filter.
     */
    private static 
        JMethodDeclaration[] getMethods(List<FilterInfo> filterInfo,
                                        JMethodDeclaration init,
                                        JMethodDeclaration initWork,
                                        JMethodDeclaration steadyWork) {
        // make result
        List<JMethodDeclaration> result = new LinkedList<JMethodDeclaration>();
        // start with the methods that we were passed
        result.add(init);
        if (initWork!=null) {
            result.add(initWork);
        }
        result.add(steadyWork);
        // add methods from each filter that aren't work methods
        for (ListIterator<FilterInfo> it = filterInfo.listIterator(); it.hasNext(); ) {
            FilterInfo info = it.next();
            SIRFilter filter=info.filter;
            List<JMethodDeclaration> methods = Arrays.asList(filter.getMethods());
            for (ListIterator<JMethodDeclaration> meth = methods.listIterator(); meth.hasNext(); ){
                JMethodDeclaration m = meth.next();
                // add methods that aren't work (or initwork)
                if (m!=info.filter.getWork()) {
                    if(filter instanceof SIRTwoStageFilter) {
                        if(m!=((SIRTwoStageFilter)filter).getInitWork())
                            result.add(m);
                    }
                    else
                        result.add(m);
                }
            }
        }
        // return result
        return result.toArray(new JMethodDeclaration[0]);
    }

    /**
     * Returns the final, fused filter.
     */
    private static SIRFilter makeFused(List<FilterInfo> filterInfo, 
                                       JMethodDeclaration init, 
                                       JMethodDeclaration initWork, 
                                       JMethodDeclaration steadyWork) {
        // get the first and last filters' info
        FilterInfo first = filterInfo.get(0);
        FilterInfo last = filterInfo.get(filterInfo.size()-1);

        // calculate the peek, pop, and push count for the fused
        // filter in the STEADY state
        int steadyPop = first.steady.num * first.filter.getPopInt();
        int steadyPeek = 
            (first.filter.getPeekInt() - first.filter.getPopInt()) + steadyPop;
        int steadyPush = last.steady.num * last.filter.getPushInt();

        SIRFilter result;
        // if initWork is null, then we can get away with a filter for
        // the fused result; otherwise we need a two-stage filter
        if (initWork==null) {
            result = new SIRFilter(first.filter.getParent(),
                                   FusePipe.getFusedName(mapToFilters(filterInfo)),
                                   getFields(filterInfo),
                                   getMethods(filterInfo, 
                                              init, 
                                              initWork, 
                                              steadyWork),
                                   new JIntLiteral(steadyPeek), 
                                   new JIntLiteral(steadyPop),
                                   new JIntLiteral(steadyPush),
                                   steadyWork,
                                   first.filter.getInputType(),
                                   last.filter.getOutputType());
        } else {
            // calculate the peek, pop, and push count for the fused
            // filter in the INITIAL state
            /*int initPop = first.init.num * first.filter.getPopInt();
              int initPeek =
              (first.filter.getPeekInt() - first.filter.getPopInt()) + initPop;
              int initPush = last.init.num * last.filter.getPushInt();*/
        
        
            int initPop = first.init.num * first.filter.getPopInt();
            if(first.filter instanceof SIRTwoStageFilter)
                initPop = ((SIRTwoStageFilter)first.filter).getInitPopInt()+(first.init.num-1) * first.filter.getPopInt();
            int initPeek =
                (first.filter.getPeekInt() - first.filter.getPopInt()) + initPop;
            int initPush = last.init.num * last.filter.getPushInt();
            if(last.filter instanceof SIRTwoStageFilter)
                initPush = ((SIRTwoStageFilter)last.filter).getInitPushInt()+(last.init.num-1) * last.filter.getPushInt();
        
            // make a new filter to represent the fused combo
            result = new SIRTwoStageFilter(first.filter.getParent(),
                                           FusePipe.getFusedName(mapToFilters(filterInfo)),
                                           getFields(filterInfo),
                                           getMethods(filterInfo, 
                                                      init, 
                                                      initWork, 
                                                      steadyWork),
                                           new JIntLiteral(steadyPeek), 
                                           new JIntLiteral(steadyPop),
                                           new JIntLiteral(steadyPush),
                                           steadyWork,
                                           new JIntLiteral(initPeek),
                                           new JIntLiteral(initPop),
                                           new JIntLiteral(initPush),
                                           initWork,
                                           first.filter.getInputType(),
                                           last.filter.getOutputType());
        }
    
        // set init function of fused filter
        result.setInit(init);
        return result;
    }

    /**
     * Given list of filter info's, maps them to list of filters.
     */
    static List<SIRStream> mapToFilters(List<FilterInfo> filterInfo) {
        List<SIRStream> result = new LinkedList<SIRStream>();
        for (int i=0; i<filterInfo.size(); i++) {
            result.add(filterInfo.get(i).filter);
        }
        return result;
    }

    /**
     * Contains information that is relevant to a given filter's
     * inclusion in a fused pipeline.
     */
    static class FilterInfo {
        /**
         * The filter itself.
         */
        public final SIRFilter filter;

        /**
         * The persistent buffer for holding peeked items
         */
        public final JFieldDeclaration buffer;

        /**
         * The push index.
         */
        public final JFieldDeclaration pushIndex;

        /**
         * The pop index.
         */
        public final JFieldDeclaration popIndex;

        /**
         * The size of the buffer
         */
        public final int bufferSize;

        /**
         * The info on the initial execution.
         */
        public final PhaseInfo init;
    
        /**
         * The info on the steady-state execution.
         */
        public final PhaseInfo steady;

        public FilterInfo(SIRFilter filter, JFieldDeclaration buffer,
                          JFieldDeclaration pushIndex, JFieldDeclaration popIndex,
                          int bufferSize, PhaseInfo init, PhaseInfo steady) {
            this.filter = filter;
            this.buffer = buffer;
            this.pushIndex = pushIndex;
            this.popIndex = popIndex;
            this.bufferSize = bufferSize;
            this.init = init;
            this.steady = steady;
        }
    }

    static class PhaseInfo {
        /**
         * The number of times this filter is executed in the parent.
         */ 
        public final int num;

        /**
         * The counter for keeping track of work loop executions
         */
        public final JVariableDefinition loopCounterWork;
    
        public PhaseInfo(int num, JVariableDefinition loopCounterWork) {
            this.num = num;
            this.loopCounterWork = loopCounterWork;
        }

        /**
         * Returns list of JVariableDefinitions of all var defs in here.
         */
        public List<JVariableDefinition> getVariables() {
            List<JVariableDefinition> result = new LinkedList<JVariableDefinition>();
            result.add(loopCounterWork);
            return result;
        }
    }

    static class FusingVisitor extends StatementQueueVisitor {
        /**
         * The info for the current filter.
         */
        private final FilterInfo curInfo;

        /**
         * The info for the next filter in the pipeline.
         */
        private final FilterInfo nextInfo;

        /**
         * Whether or not peek and pop expressions should be fused.
         */
        private final boolean fuseReads;

        /**
         * Whether or not push expressions should be fused.
         */
        private final boolean fuseWrites;
    
        // Whether we are in an ExpressionStatement or not affects
        // behaviour of pops:  as an immediate subexpression of ExpressionStatment,
        // they do not have to return a value.
    
        private boolean inExpressionStatement;

        public FusingVisitor(FilterInfo curInfo, FilterInfo nextInfo,
                             boolean fuseReads, boolean fuseWrites) {
            this.curInfo = curInfo;
            this.nextInfo = nextInfo;
            this.fuseReads = fuseReads;
            this.fuseWrites = fuseWrites;
            this.inExpressionStatement = false;
        }

        public Object visitExpressionStatement(JExpressionStatement self, JExpression expr) {
            boolean oldInExpressionStatement = inExpressionStatement;
            if (expr instanceof SIRPopExpression) {inExpressionStatement = true;}
            Object result = super.visitExpressionStatement(self,expr);
            inExpressionStatement = oldInExpressionStatement;
            return result;
        }
    
        // pop(i) -> buffer[pop_index]; 
        // add to pendingStatements: pop_index = (pop_index + 1) % buffer_size
        public Object visitPopExpression(SIRPopExpression self,
                                         CType tapeType) {

            // leave it alone not fusing reads
            if (!fuseReads) {
                return super.visitPopExpression(self, tapeType);
            }

            // get relevant names
            String bufferName = curInfo.buffer.getVariable().getIdent();
            String popIndexName = curInfo.popIndex.getVariable().getIdent();

            if (inExpressionStatement) {
                // no value needed...
                // immediately emit expression to update popIndex
                JExpression lhs = new JFieldAccessExpression(popIndexName);
                JExpression rhs = new JBitwiseExpression(OPE_BAND,
                                                         new JAddExpression(new JFieldAccessExpression(
                                                                                                       popIndexName), 
                                                                            new JIntLiteral(self.getNumPop())),
                                                         new JIntLiteral(curInfo.bufferSize - 1));
                return new JAssignmentExpression(lhs, rhs);
            }
        
            // build ref to buffer
            JExpression lhs = new JFieldAccessExpression(bufferName);
            // build index
            JExpression rhs = new JFieldAccessExpression(popIndexName);

            // return a new array access expression
            JExpression result = new JArrayAccessExpression(lhs, rhs);

            // add increment expression to pending statements
            lhs = new JFieldAccessExpression(popIndexName);
            // (pop_index + 1) & (buffer_size-1)
            rhs = new JBitwiseExpression(OPE_BAND,
                                         new JAddExpression(new JFieldAccessExpression(popIndexName),
                                                            new JIntLiteral(1)),
                                         new JIntLiteral(curInfo.bufferSize-1));
            addPendingStatement(new JExpressionStatement(new JAssignmentExpression(lhs, rhs)));

            return result;
        }

        // peek(i) -> buffer[(pop_index+i) % buffer_size]
        public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                          CType oldTapeType,
                                          JExpression oldArg) {
            // leave it alone not fusing reads
            if (!fuseReads) {
                return super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
            }

            // do the super
            SIRPeekExpression self = 
                (SIRPeekExpression)
                super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
    
            // build ref to buffer
            JExpression lhs = new JFieldAccessExpression(curInfo.buffer.getVariable().getIdent());
            // build:  (pop_index + self.getArg()) & (buffer_size-1)
            JExpression rhs = new JBitwiseExpression(OPE_BAND,
                                                     new JAddExpression(new JFieldAccessExpression(curInfo.popIndex.getVariable().getIdent()),
                                                                        self.getArg()),
                                                     new JIntLiteral(curInfo.bufferSize-1));

            // return a new array access expression
            return new JArrayAccessExpression(lhs, rhs);
        }

        // push(val) --> buffer[push_index] = val; 
        // add to pending statements: push_index = (push_index + 1) % buffer_size;
        public Object visitPushExpression(SIRPushExpression oldSelf,
                                          CType oldTapeType,
                                          JExpression oldArg) {
            // leave it alone not fusing writes
            if (!fuseWrites) {
                return super.visitPushExpression(oldSelf, oldTapeType, oldArg);
            }

            // do the super
            SIRPushExpression self = 
                (SIRPushExpression)
                super.visitPushExpression(oldSelf, oldTapeType, oldArg);

            // get names of relevant buffer, push index
            String bufferName = nextInfo.buffer.getVariable().getIdent();
            String pushIndexName = curInfo.pushIndex.getVariable().getIdent();

            // build: buffer[push_index] = val; 
            // build ref to buffer
            JExpression base = new JFieldAccessExpression(bufferName);
            // build index
            JExpression index = new JFieldAccessExpression(pushIndexName);
            // return assignment
            JExpression result = new JAssignmentExpression(new JArrayAccessExpression(base, index),
                                                           self.getArg());

            // add increment expression to pending statements...
            // build: push_index = (push_index + 1) % buffer_size;
            JExpression lhs = new JFieldAccessExpression(pushIndexName);
            // (push_index + 1) & (buffer_size-1)
            JExpression rhs = new JBitwiseExpression(OPE_BAND,
                                                     new JAddExpression(new JFieldAccessExpression(pushIndexName),
                                                                        new JIntLiteral(1)),
                                                     new JIntLiteral(nextInfo.bufferSize-1));
            addPendingStatement(new JExpressionStatement(new JAssignmentExpression(lhs, rhs)));

            return result;
        }
    }
    /**
     * This builds up the init function of the fused class by traversing
     * the init function of the parent.
     */
    static class InitFuser {
        /**
         * The info on the filters we're trying to fuse.
         */
        private final List<FilterInfo> filterInfo;

        /**
         * The block of the resulting fused init function.
         */
        private JBlock fusedBlock;
    
        /**
         * A list of the parameters of the fused block, all of type
         * JFormalParameter.
         */
        private List<JFormalParameter> fusedParam;
    
        /**
         * A list of the arguments to the init function of the fused
         * block, all of type JExpression.
         */
        private List fusedArgs;

        /**
         * Cached copy of the method decl for the init function.
         */
        private JMethodDeclaration initFunction;

        /**
         * The number of filter's we've fused.
         */
        private int numFused;

        /**
         * <b>fusedFilter</b> represents what -will- be the result of the
         * fusion.  It has been allocated, but is not filled in with
         * correct values yet.
         */
        public InitFuser(List<FilterInfo> filterInfo) {
            this.filterInfo = filterInfo;
            this.fusedBlock = new JBlock();
            this.fusedParam = new LinkedList<JFormalParameter>();
            this.fusedArgs = new LinkedList();
            this.numFused = 0;
        }

        public void doit(SIRPipeline parent) {
            for (ListIterator<FilterInfo> it = filterInfo.listIterator(); it.hasNext(); ) {
                // process the arguments to a filter being fused
                FilterInfo info = it.next();
                int index = parent.indexOf(info.filter);
                processArgs(info, parent.getParams(index));
            }
            makeInitFunction();
        }

        /**
         * Given that we found <args> in an init call to <info>,
         * incorporate this info into the init function of the fused
         * filter.
         */
        private void processArgs(FilterInfo info, List args) {
            // make parameters for <args>, and build <newArgs> to pass
            // to new init function call
            JExpression[] newArgs = new JExpression[args.size()];
            for (int i=0; i<args.size(); i++) {
                JFormalParameter param = 
                    new JFormalParameter(((JExpression)args.get(i)).getType(),
                                         ShiftPipelineFusion.INIT_PARAM_NAME + 
                                         "_" + i + "_" + numFused);
                // add to list
                fusedParam.add(param);
                // make a new arg
                newArgs[i] = new JLocalVariableExpression(param);
                // increment fused count
                numFused++;
            }

            // add the arguments to the list
            fusedArgs.addAll(args);

            // make a call to the init function of <info> with <params>
            fusedBlock.addStatement(new JExpressionStatement(new JMethodCallExpression(info.filter.getInit().getName(),
                                                                                       newArgs)));
        }

        /**
         * Prepares the init function for the fused block once the
         * traversal of the parent's init function is complete.
         */
        private void makeInitFunction() {
            // add allocations for buffers
            int i=0;
            for (ListIterator<FilterInfo> it = filterInfo.listIterator(); it.hasNext(); i++) {
                // get the next info
                FilterInfo info = it.next();
                // ignore buffer and pop index for first filter, as it
                // reads from the input with peek/pop operations
                if (i!=0) {
                    // calculate dimensions of the buffer
                    JExpression[] dims = { new JIntLiteral(info.bufferSize) };

                    // initialize pop index to 0
                    fusedBlock.addStatementFirst(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(info.popIndex.
                                                                                                                               getVariable().getIdent()),
                                                                                                    new JIntLiteral(0))));
                }
                // initialize push index to 0 for all but last filter
                if (it.hasNext()) {
                    fusedBlock.addStatementFirst(new JExpressionStatement(new JAssignmentExpression(new JFieldAccessExpression(info.pushIndex.
                                                                                                                               getVariable().getIdent()),
                                                                                                    new JIntLiteral(0))));
                }
            }
            // now we can make the init function
            this.initFunction = new JMethodDeclaration(CStdType.Void,
                                                       RenameAll.newName("init"),
                                                       fusedParam.toArray(new JFormalParameter[0]),
                                                       fusedBlock);
        }
    
        /**
         * Returns fused init function of this.
         */
        public JMethodDeclaration getInitFunction() {
            assert initFunction!=null;
            return initFunction;
        }

        /**
         * Returns the list of arguments that should be passed to init
         * function.
         */
        public List getInitArgs() {
            return fusedArgs;
        }
    
    }
}

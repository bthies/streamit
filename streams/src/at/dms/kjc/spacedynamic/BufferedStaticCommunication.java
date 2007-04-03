package at.dms.kjc.spacedynamic;

//import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import at.dms.kjc.common.*;
import at.dms.kjc.CArrayType;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.Constants;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JBitwiseExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBooleanLiteral;
import at.dms.kjc.JEmptyStatement;
import at.dms.kjc.JEqualityExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionListStatement;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JForStatement;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIfStatement;
import at.dms.kjc.JIntLiteral;
//import at.dms.kjc.JLocalVariable;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
//import at.dms.kjc.JNewArrayExpression;
import at.dms.kjc.JParenthesedExpression;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JPrefixExpression;
import at.dms.kjc.JRelationalExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JStringLiteral;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.JWhileStatement;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.ObjectDeepCloner;
import at.dms.kjc.SLIRReplacingVisitor;
//import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.common.CommonUtils;

/**
 * Create SIR code for a static-rate-input filter that requires a buffer (it
 * might be circular or linear).
 * 
 * @author mgordon
 * 
 */
public class BufferedStaticCommunication extends at.dms.util.Utils implements
                                                                       Constants {
    /** the layout object for the flatnode * */
    private Layout layout;

    /** the ssg of this flat node * */
    private SpdStaticStreamGraph ssg;

    /** the flat node we are generating code for * */
    private FlatNode node;

    /** number of items to receive between preWork() and work() * */
    private int bottomPeek = 0;

    /** number of times the filter fires in the init schedule * */
    private int initFire = 0;

    /** number of items to receive after initialization */
    private int remaining = 0;

    /** true if we have communication outside of the work function or if 
        we are not inlining work function calls, so we have to make the buffer
        a global */
    private boolean globalBuffer;

    /** the sir filter */ 
    private SIRFilter filter;

    /***************************************************************************
     * class to hold the local variables we create so we can pass these across
     * functions
     **************************************************************************/
    class LocalVariables {
        JVariableDefinition recvBuffer;

        JVariableDefinition recvBufferSize;

        JVariableDefinition recvBufferBits;

        JVariableDefinition recvBufferIndex;

        JVariableDefinition recvIndex;

        JVariableDefinition exeIndex;

        JVariableDefinition exeIndex1;

        JVariableDefinition[] ARRAY_INDEX;

        JVariableDefinition[] ARRAY_COPY;

        JVariableDefinition simpleIndex;

        JVariableDefinition sendBufferIndex;

        JVariableDefinition sendBuffer;
    }

    /**
     * Create a new BufferedStaticCommunication Generator for:
     * 
     * @param ssg
     *            The SSG of node
     * @param node
     *            The node for which to generate code
     * @param bottomPeek
     *            The bottomPeek value (see my thesis).
     * @param remaining
     *            The remaining value (see my thesis).
     * @param initFire
     *            The init multiplicity of the filter (assuming all filters are
     *            2 stage).
     */
    public BufferedStaticCommunication(SpdStaticStreamGraph ssg, FlatNode node,
                                       int bottomPeek, int remaining, int initFire) {
        this.node = node;
        this.ssg = ssg;
        this.layout = ((SpdStreamGraph)ssg.getStreamGraph()).getLayout();
        this.bottomPeek = bottomPeek;
        this.remaining = remaining;
        this.initFire = initFire;
    }

    /** Create SIR code to execute this filter on Raw * */
    public void doit() {

        filter = node.getFilter();
    
        //see if any of the helper methods access the incoming buffer,
        //or if we are not inlining the work function, if not, then we have 
        //to generate globals for the buffer and its vars...
        globalBuffer = PeekPopInHelper.check(filter) || !RawExecutionCode.INLINE_WORK;

        if (globalBuffer)
            System.out.println("  Input buffer access in helper function, globalizing buffer...");

        LocalVariables localVariables = new LocalVariables();

        JBlock block = new JBlock(null, new JStatement[0], null);

        /*
        if (isSimple(filter))
            System.out.print("(simple) ");
        else if (noBuffer(filter))
            System.out.print("(no buffer) ");
        else {
            if (remaining > 0)
                System.out.print("(remaining) ");
            if (filter.getPeekInt() > filter.getPopInt())
                System.out.print("(peeking)");
        }
        System.out.println();
        */

        createBufferVariables(node, block, localVariables);
        convertCommExps(filter, isSimple(filter), localVariables);

        rawMainFunction(node, block, localVariables);

        // create the method and add it to the filter
        JMethodDeclaration rawMainFunct = new JMethodDeclaration(null,
                                                                 at.dms.kjc.Constants.ACC_PUBLIC, CStdType.Void,
                                                                 RawExecutionCode.rawMain, JFormalParameter.EMPTY,
                                                                 CClassType.EMPTY, block, null, null);
        filter.addMethod(rawMainFunct);
    }

    // returns the dims for the buffer array.
    private JExpression[] bufferDims(SIRFilter filter, CType inputType,
                                     int buffersize) {
        // this is an array type
        if (inputType.isArrayType()) {
            CType baseType = ((CArrayType) inputType).getBaseType();
            // create the array to hold the dims of the buffer
            JExpression baseTypeDims[] = ((CArrayType) inputType).getDims();
            // the buffer is an array itself, so add one to the size of the
            // input type
            JExpression[] dims = new JExpression[baseTypeDims.length + 1];
            // the first dim is the buffersize
            dims[0] = new JIntLiteral(buffersize);
            // copy the dims for the basetype
            for (int i = 0; i < baseTypeDims.length; i++)
                dims[i + 1] = baseTypeDims[i];
            return dims;
        } else {
            JExpression dims[] = { new JIntLiteral(buffersize) };
            return dims;
        }
    }

    /** Create the buffer variables and create the declarations for them,
     * either local or fields.
     * 
     * @param node
     * @param block
     * @param localVariables
     */
    private void createBufferVariables(FlatNode node, JBlock block,
                                       LocalVariables localVariables) {
        
        // index variable for certain for loops
        JVariableDefinition exeIndexVar = new JVariableDefinition(null, 0,
                                                                  CStdType.Integer, RawExecutionCode.exeIndex, null);

        // remember the JVarDef for latter (in the raw main function)
        localVariables.exeIndex = exeIndexVar;

        declareBufferVar(exeIndexVar, block);    
        
        // index variable for certain for loops
        JVariableDefinition exeIndex1Var = new JVariableDefinition(null, 0,
                                                                   CStdType.Integer, RawExecutionCode.exeIndex1, null);

        localVariables.exeIndex1 = exeIndex1Var;

        declareBufferVar(exeIndex1Var, block);
        
        // only add the receive buffer and its vars if the
        // filter receives data
        if (!noBuffer(filter)) {
            // initialize the buffersize to be the size of the
            // struct being passed over it
            int buffersize;

            int prepeek = 0;
            int maxpeek = filter.getPeekInt();
            if (filter instanceof SIRTwoStageFilter)
                prepeek = ((SIRTwoStageFilter) filter).getInitPeekInt();
            // set up the maxpeek
            maxpeek = (prepeek > maxpeek) ? prepeek : maxpeek;

            if (isSimple(filter)) {
                // simple filter (no remaining items)
                if (KjcOptions.ratematch) {
                    // System.out.println(filter.getName());

                    int i = ssg.getMult(node, false);

                    // i don't know, the prepeek could be really large, so just
                    // in case
                    // include it. Make the buffer big enough to hold
                    buffersize = Math
                        .max((ssg.getMult(node, false) - 1)
                             * filter.getPopInt() + filter.getPeekInt(),
                             prepeek);
                } else
                    // not ratematching and we do not have a circular buffer
                    buffersize = maxpeek;

                // define the simple index variable
                JVariableDefinition simpleIndexVar = new JVariableDefinition(
                                                                             null, 0, CStdType.Integer,
                                                                             RawExecutionCode.simpleIndex, new JIntLiteral(-1));

                // remember the JVarDef for latter (in the raw main function)
                localVariables.simpleIndex = simpleIndexVar;
                declareBufferVar(simpleIndexVar, block);
            } else { // filter with remaing items on the buffer after
                // initialization
                // see Mgordon's thesis for explanation (Code Generation
                // Section)
                if (KjcOptions.ratematch)
                    buffersize = Util.nextPow2(Math
                                               .max((ssg.getMult(node, false) - 1)
                                                    * filter.getPopInt() + filter.getPeekInt(),
                                                    prepeek)
                                               + remaining);
                else
                    buffersize = Util.nextPow2(maxpeek + remaining);
            }

            int dim = (filter.getInputType().isArrayType()) ? ((CArrayType) filter
                                                               .getInputType()).getDims().length + 1
                : 1;

            JVariableDefinition recvBufVar = new JVariableDefinition(null,
                                                                     at.dms.kjc.Constants.ACC_FINAL, // ?????????
                                                                     new CArrayType(CommonUtils.getBaseType(filter.getInputType()), dim /* dimension */,
                                                                                    bufferDims(filter, filter.getInputType(),
                                                                                               buffersize)), RawExecutionCode.recvBuffer,
                                                                     null);

            // the size of the buffer
            JVariableDefinition recvBufferSizeVar = new JVariableDefinition(
                                                                            null, at.dms.kjc.Constants.ACC_FINAL, // ?????????
                                                                            CStdType.Integer, RawExecutionCode.recvBufferSize,
                                                                            new JIntLiteral(buffersize));

            // the size of the buffer
            JVariableDefinition recvBufferBitsVar = new JVariableDefinition(
                                                                            null, at.dms.kjc.Constants.ACC_FINAL, // ?????????
                                                                            CStdType.Integer, RawExecutionCode.recvBufferBits,
                                                                            new JIntLiteral(buffersize - 1));

            // the receive buffer index (start of the buffer)
            JVariableDefinition recvBufferIndexVar = new JVariableDefinition(
                                                                             null, 0, CStdType.Integer,
                                                                             RawExecutionCode.recvBufferIndex, new JIntLiteral(-1));

            // the index to the end of the receive buffer)
            JVariableDefinition recvIndexVar = new JVariableDefinition(null, 0,
                                                                       CStdType.Integer, RawExecutionCode.recvIndex,
                                                                       new JIntLiteral(-1));

            JVariableDefinition[] indices = { recvBufferSizeVar,
                                              recvBufferBitsVar, recvBufferIndexVar, recvIndexVar };

            localVariables.recvBuffer = recvBufVar;
            localVariables.recvBufferSize = recvBufferSizeVar;
            localVariables.recvBufferBits = recvBufferBitsVar;
            localVariables.recvBufferIndex = recvBufferIndexVar;
            localVariables.recvIndex = recvIndexVar;
            declareBufferVar(indices, block);
            declareBufferVar(recvBufVar, block);
        }

        // if we are rate matching, create the output buffer with its
        // index
        if (KjcOptions.ratematch && filter.getPushInt() > 0) {
            int steady = ssg.getMult(layout.getNode(layout.getTile(filter)),
                                     false);

            // define the send buffer index variable
            JVariableDefinition sendBufferIndexVar = new JVariableDefinition(
                                                                             null, 0, CStdType.Integer,
                                                                             RawExecutionCode.sendBufferIndex, new JIntLiteral(-1));

            localVariables.sendBufferIndex = sendBufferIndexVar;
            declareBufferVar(sendBufferIndexVar, block);    
            // define the send buffer

            JExpression[] dims = new JExpression[1];
            // the size of the output array is number of executions in steady *
            // number of items pushed * size of item
            dims[0] = new JIntLiteral(steady * filter.getPushInt()
                                      * Util.getTypeSize(filter.getOutputType()));

            JVariableDefinition sendBufVar = new JVariableDefinition(null,
                                                                     at.dms.kjc.Constants.ACC_FINAL, // ?????????
                                                                     new CArrayType(filter.getOutputType(), 1 /* dimension */,
                                                                                    dims), RawExecutionCode.sendBuffer, null);
            localVariables.sendBuffer = sendBufVar;
            declareBufferVar(sendBufVar, block);    
        }

        // print the declarations for the array indices for pushing and popping
        // if this filter deals with arrays
        if (filter.getInputType().isArrayType()) {

            int inputDim = ((CArrayType) filter.getInputType()).getArrayBound();

            localVariables.ARRAY_INDEX = new JVariableDefinition[inputDim];

            // create enough index vars as max dim
            for (int i = 0; i < inputDim; i++) {
                JVariableDefinition arrayIndexVar = new JVariableDefinition(
                                                                            null, 0, CStdType.Integer, RawExecutionCode.ARRAY_INDEX
                                                                            + i, null);
                // remember the array index vars
                localVariables.ARRAY_INDEX[i] = arrayIndexVar;
                declareBufferVar(arrayIndexVar, block);
            }
        }
    }

    /**
     * Declare a variable that has something to do with the peek buffer, if this
     * filter does not have any helper functions that access the tape, then
     * declare them as local to the RawMain <block>, otherwise, add them as a
     * field
     */
    private void declareBufferVar(JVariableDefinition var, JBlock block) {
        if (globalBuffer) {
            filter.addField(new JFieldDeclaration(var));
        } else {
            block.addStatement(new JVariableDeclarationStatement(null, var, null));    
        }
    }

    /**
     * Declare variables that has something to do with the peek buffer, if this
     * filter does not have any helper functions that access the tape, then
     * declare them as local to the RawMain <block>, otherwise, add them as a
     * field.
     * 
     * @param var
     * @param block
     */
    private void declareBufferVar(JVariableDefinition[] var, JBlock block) {
        if (globalBuffer) {
            for (int i = 0; i < var.length; i++) {
                filter.addField(new JFieldDeclaration(var[i]));
            }
        } else {
            block.addStatement(new JVariableDeclarationStatement(null, var, null));    
        } 
    }
    
    private boolean isSimple(SIRFilter filter) {
        if (noBuffer(filter))
            return false;

        if (filter.getPeekInt() == filter.getPopInt()
            && remaining == 0
            && (!(filter instanceof SIRTwoStageFilter) || ((SIRTwoStageFilter) filter)
                .getInitPop() == ((SIRTwoStageFilter) filter)
                .getInitPeek()))
            return true;
        return false;
    }

    // convert the peek and pop expressions for a filter into
    // buffer accesses, do this for all functions just in case helper
    // functions call peek or pop
    private void convertCommExps(SIRFilter filter, boolean simple,
                                 LocalVariables localVars) {
        SLIRReplacingVisitor convert;

        if (simple) {
            convert = new ConvertCommunicationSimple(localVars);
        } else
            convert = new ConvertCommunication(localVars);

        JMethodDeclaration[] methods = filter.getMethods();
        for (int i = 0; i < methods.length; i++) {
            // iterate over the statements and call the ConvertCommunication
            // class to convert peek, pop
            for (ListIterator it = methods[i].getStatementIterator(); it
                     .hasNext();) {
                ((JStatement) it.next()).accept(convert);
            }
        }
    }

    private boolean noBuffer(SIRFilter filter) {
        // always need a buffer for rate matching.
        // if (KjcOptions.ratematch)
        // return false;

        //always generate buffer while testing imem or Raw work estimation
        //so nobuffer is false
        if (IMEMEstimation.TESTING_IMEM || 
                RawWorkEstimator.SIMULATING_WORK) 
            return false;
        
        if (filter.getPeekInt() == 0
            && (!(filter instanceof SIRTwoStageFilter) || (((SIRTwoStageFilter) filter)
                                                           .getInitPeekInt() == 0)))
            return true;
        return false;
    }

    // private void defineLocalVars(JBlock block,
    // LocalVariables localVariables

    private void rawMainFunction(FlatNode node, JBlock statements,
                                 LocalVariables localVariables) {
        SIRFilter filter = (SIRFilter) node.contents;

        // generate the code to define the local variables
        // defineLocalVars(statements, localVariables);

        // create the params list, for some reason
        // calling toArray() on the list breaks a later pass
        List paramList = filter.getParams();
        JExpression[] paramArray;
        if (paramList == null || paramList.size() == 0)
            paramArray = new JExpression[0];
        else
            paramArray = (JExpression[]) paramList.toArray(new JExpression[0]);

        // if standalone, add a field for the iteration counter...
        JFieldDeclaration iterationCounter = null;
        if (KjcOptions.standalone) {
            iterationCounter = new JFieldDeclaration(new JVariableDefinition(0,
                                                                             CStdType.Integer, FlatIRToC.MAINMETHOD_COUNTER,
                                                                             new JIntLiteral(-1)));
            filter.addField(iterationCounter);
        }

        // add the call to the init function
        statements.addStatement(new JExpressionStatement(null,
                                                         new JMethodCallExpression(null, new JThisExpression(null),
                                                                                   filter.getInit().getName(), paramArray), null));

        // add the call to initWork
        if (filter instanceof SIRTwoStageFilter) {
            SIRTwoStageFilter two = (SIRTwoStageFilter) filter;
            JBlock body = (JBlock) ObjectDeepCloner.deepCopy(two.getInitWork()
                                                             .getBody());

            // add the code to receive the items into the buffer
            statements.addStatement(makeForLoopBufferVar(receiveCode(filter, filter.getInputType(), localVariables),
                                                         localVariables.exeIndex, 
                                                         new JIntLiteral(two.getInitPeekInt())));

            // now inline the init work body
            statements.addStatement(body);
            // if a simple filter, reset the simpleIndex
            if (isSimple(filter)) {
                statements.addStatement(new JExpressionStatement(null,
                                                                 (new JAssignmentExpression(null,
                                                                                            accessBufferVar(localVariables.simpleIndex),
                                                                                            new JIntLiteral(-1))), null));
            }
        }

        if (initFire - 1 > 0) {
            // add the code to collect enough data necessary to fire the
            // work function for the first time

            if (bottomPeek > 0) {
                statements.addStatement(makeForLoopBufferVar(
                                                             receiveCode(filter, filter.getInputType(),
                                                                         localVariables), localVariables.exeIndex,
                                                             new JIntLiteral(bottomPeek)));
            }

            // add the calls for the work function in the initialization stage
            statements
                .addStatement(generateInitWorkLoop(filter, localVariables));
        }

        // add the code to collect all data produced by the upstream filter
        // but not consumed by this filter in the initialization stage
        if (remaining > 0) {
            statements.addStatement(makeForLoopBufferVar(receiveCode(
                                                                     filter, filter.getInputType(), localVariables),
                                                         localVariables.exeIndex, new JIntLiteral(remaining)));
        }

        if (!IMEMEstimation.TESTING_IMEM) {
            // add a call to raw_init2 only if not testing imem
            statements.addStatement(new JExpressionStatement(null,
                                                             new JMethodCallExpression(null, new JThisExpression(null),
                                                                                       SwitchCode.SW_SS_TRIPS, new JExpression[0]), null));
        }

        if (SpaceDynamicBackend.FILTER_DEBUG_MODE) {
            statements.addStatement(new SIRPrintStatement(null,
                                                          new JStringLiteral(null, filter.getName()
                                                                             + " Starting Steady-State\\n"), null));
        }

        // add the call to the work function
        statements
            .addStatement(generateSteadyStateLoop(filter, localVariables));

    }

    /** generate the loop for the work function firings in the
     * initialization schedule, this function is poorly named, its has nothing
     * to do with initWork().
     */
    JStatement generateInitWorkLoop(SIRFilter filter,
                                    LocalVariables localVariables) {
        JStatement innerReceiveLoop = makeForLoopBufferVar(receiveCode(
                                                                       filter, filter.getInputType(), localVariables),
                                                           localVariables.exeIndex, new JIntLiteral(filter.getPopInt()));

        JExpression isFirst = new JEqualityExpression(null, false,
                                                      accessBufferVar(localVariables.exeIndex1),
                                                      new JIntLiteral(0));
        JStatement ifStatement = new JIfStatement(null, isFirst,
                                                  innerReceiveLoop, null, null);

        JBlock block = new JBlock(null, new JStatement[0], null);

        // if a simple filter, reset the simpleIndex
        if (isSimple(filter)) {
            block.addStatement(new JExpressionStatement(null,
                                                        (new JAssignmentExpression(null,
                                                                                   accessBufferVar(localVariables.simpleIndex),    
                                                                                   new JIntLiteral(-1))), null));
        }

        // add the if statement
        block.addStatement(ifStatement);

        // clone the work function and inline it
        JBlock workBlock = RawExecutionCode.executeWorkFunction(filter);

        // if we are in debug mode, print out that the filter is firing
        if (SpaceDynamicBackend.FILTER_DEBUG_MODE) {
            block.addStatement(new SIRPrintStatement(null, new JStringLiteral(
                                                                              null, filter.getName() + " firing (init).\\n"), null));
        }

        block.addStatement(workBlock);

        // return the for loop that executes the block init - 1
        // times
        return makeForLoopBufferVar(block, localVariables.exeIndex1,
                                    new JIntLiteral(initFire - 1));
    }

    private int lcm(int x, int y) { // least common multiple
        int v = x, u = y;
        while (x != y)
            if (x > y) {
                x -= y;
                v += u;
            } else {
                y -= x;
                u += v;
            }
        return (u + v) / 2;
    }

    JStatement generateRateMatchSteadyState(SIRFilter filter,
                                            LocalVariables localVariables) {
        JBlock block = new JBlock(null, new JStatement[0], null);
        int steady = ssg.getMult(layout.getNode(layout.getTile(filter)), false);

        // reset the simple index
        if (isSimple(filter)) {
            block.addStatement(new JExpressionStatement(null,
                                                        (new JAssignmentExpression(null,
                                                                                   accessBufferVar(localVariables.simpleIndex),
                                                                                   new JIntLiteral(-1))), null));
        }

        // should be at least peek - pop items in the buffer, so
        // just receive pop * steady in the buffer and we can
        // run for an entire steady state
        block.addStatement(makeForLoopBufferVar(receiveCode(filter,
                                                            filter.getInputType(), localVariables),
                                                localVariables.exeIndex, new JIntLiteral(filter.getPopInt()
                                                                                         * steady)));
        if (filter.getPushInt() > 0) {
            // reset the send buffer index
            block.addStatement(new JExpressionStatement(null,
                                                        new JAssignmentExpression(null,
                                                                                  accessBufferVar(localVariables.sendBufferIndex),    
                                                                                  new JIntLiteral(-1)), null));
        }

        // now, run the work function steady times...
        JBlock workBlock = RawExecutionCode.executeWorkFunction(filter);

        // convert all of the push expressions in the steady state work block
        // into
        // stores to the output buffer
        workBlock.accept(new RateMatchConvertPush(localVariables));

        // if we are in debug mode, print out that the filter is firing
        if (SpaceDynamicBackend.FILTER_DEBUG_MODE) {
            block.addStatement(new SIRPrintStatement(null, new JStringLiteral(
                                                                              null, filter.getName() + " firing.\\n"), null));
        }

        // add the cloned work function to the block
        block.addStatement(makeForLoopBufferVar(workBlock,
                                                localVariables.exeIndex, new JIntLiteral(steady)));

        // now add the code to push the output buffer onto the static network
        // and
        // reset the output buffer index
        // for (steady*push*typesize)
        // push(__SENDBUFFER__[++ __SENDBUFFERINDEX__])
        if (filter.getPushInt() > 0) {

            SIRPushExpression pushExp = new SIRPushExpression(
                                                              new JArrayAccessExpression(null,
                                                                                         accessBufferVar(localVariables.sendBuffer),
                                                                                         accessBufferVar(localVariables.exeIndex)));
                            
            pushExp.setTapeType(CommonUtils.getBaseType(filter.getOutputType()));

            JExpressionStatement send = new JExpressionStatement(null, pushExp,
                                                                 null);

            block.addStatement(makeForLoopBufferVar(send,
                                                    localVariables.exeIndex, new JIntLiteral(steady
                                                                                             * filter.getPushInt()
                                                                                             * Util.getTypeSize(filter.getOutputType()))));

        }

        // if we are in decoupled mode do not put the work function in a for
        // loop
        // and add the print statements
        if (KjcOptions.decoupled || RawWorkEstimator.SIMULATING_WORK) {
            block.addStatementFirst(new SIRPrintStatement(null,
                                                          new JIntLiteral(0), null));
            block.addStatement(block.size(), new SIRPrintStatement(null,
                                                                   new JIntLiteral(1), null));
            return block;
        }

        // return the infinite loop
        return new JWhileStatement(null, new JBooleanLiteral(null, true),
                                   block, null);

    }

    /**
     * Generate and return the SIR code required to fire the filter's work function
     * in the steady state including all the code necessary for receiving items from
     * the network. 
     * 
     * @param filter The fitler to fire
     * @param localVariables The structure of compiler-generated variables
     * @return The code to fire the filter.
     */
    JStatement generateSteadyStateLoop(SIRFilter filter,
                                       LocalVariables localVariables) {

        // is we are rate matching generate the appropriate code
        if (KjcOptions.ratematch)
            return generateRateMatchSteadyState(filter, localVariables);

        JBlock block = new JBlock(null, new JStatement[0], null);

        // reset the simple index
        if (isSimple(filter)) {
            block.addStatement(new JExpressionStatement(null,
                                                        (new JAssignmentExpression(null,
                                                                                   accessBufferVar(localVariables.simpleIndex),
                                                                                   new JIntLiteral(-1))), null));
        }

        // add the statements to receive pop items into the buffer
        block.addStatement(makeForLoopBufferVar(receiveCode(filter,
                                                            filter.getInputType(), localVariables),
                                                localVariables.exeIndex, new JIntLiteral(filter.getPopInt())));

        JBlock workBlock = RawExecutionCode.executeWorkFunction(filter);

        // if we are in debug mode, print out that the filter is firing
        if (SpaceDynamicBackend.FILTER_DEBUG_MODE) {
            block.addStatement(new SIRPrintStatement(null, new JStringLiteral(
                                                                              null, filter.getName() + " firing.\\n"), null));
        }

        // add the cloned work function to the block
        block.addStatement(workBlock);
        // }

        // return block;

        // if we are in decoupled mode do not put the work function in a for
        // loop
        // and add the print statements
        if (KjcOptions.decoupled || RawWorkEstimator.SIMULATING_WORK) {
            block.addStatementFirst(new SIRPrintStatement(null,
                                                          new JIntLiteral(0), null));
            block.addStatement(block.size(), new SIRPrintStatement(null,
                                                                   new JIntLiteral(1), null));
            return block;
        }

        return new JWhileStatement(null,
                                   KjcOptions.standalone ? (JExpression) new JPostfixExpression(
                                                                                                null, Constants.OPE_POSTDEC,
                                                                                                new JFieldAccessExpression(new JThisExpression(null),
                                                                                                                           FlatIRToC.MAINMETHOD_COUNTER))
                                   : (JExpression) new JBooleanLiteral(null, true), block,
                                   null);
    }

    JStatement receiveCode(SIRFilter filter, CType type,
                           LocalVariables localVariables) {
        if (noBuffer(filter))
            return null;

        // the name of the method we are calling, this will
        // depend on type of the pop, by default set it to be the scalar receive
        String receiveMethodName = RawExecutionCode.receiveMethod;

        JBlock statements = new JBlock(null, new JStatement[0], null);

        // if it is not a scalar receive change the name to the appropriate
        // method call, from struct.h
        if (type.isArrayType()) {
            return arrayReceiveCode(filter, (CArrayType) type, localVariables);
        } else if (type.isClassType()) {
            receiveMethodName = RawExecutionCode.structReceivePrefixStatic
                + type.toString();
        }

        // create the array access expression to access the buffer
        JArrayAccessExpression arrayAccess = new JArrayAccessExpression(null,
                                                                        accessBufferVar(localVariables.recvBuffer),
                                                                        bufferIndex(filter, localVariables));

        // put the arrayaccess in an array...
        JExpression[] bufferAccess = { new JParenthesedExpression(null,
                                                                  arrayAccess) };

        // the method call expression, for scalars, flatIRtoC
        // changes this into c code thatw will perform the receive...
        JMethodCallExpression exp = new JMethodCallExpression(null,
                                                              new JThisExpression(null), receiveMethodName, bufferAccess);

        exp.setTapeType(type);
        // return a statement
        return new JExpressionStatement(null, exp, null);
    }

    // generate the code to receive the array into the buffer
    JStatement arrayReceiveCode(SIRFilter filter, CArrayType type,
                                LocalVariables localVariables) {
        // get the dimensionality of the input buffer
        int dim = type.getDims().length + 1;
        // make sure there are enough indices
        if (localVariables.ARRAY_INDEX.length < (dim - 1))
            Utils.fail("Error generating array receive code");
        // generate the first (outermost array access)
        JArrayAccessExpression arrayAccess = new JArrayAccessExpression(null,
                                                                        accessBufferVar(localVariables.recvBuffer),
                                                                        bufferIndex(filter, localVariables));
        // generate the remaining array accesses
        for (int i = 0; i < dim - 1; i++)
            arrayAccess = new JArrayAccessExpression(null, arrayAccess,
                                                     accessBufferVar(localVariables.ARRAY_INDEX[i]));
                    
        // now place the array access in a method call to alert flatirtoc that
        // it is a
        // static receive
        JExpression[] args = new JExpression[1];
        args[0] = arrayAccess;

        JMethodCallExpression methodCall = new JMethodCallExpression(null,
                                                                     new JThisExpression(null), RawExecutionCode.receiveMethod, args);

        methodCall.setTapeType(type);
        // now generate the nested for loops

        // get the dimensions of the array as set by kopi2sir
        JExpression[] dims = type.getDims();
        JStatement stmt = new JExpressionStatement(null, methodCall, null);
        for (int i = dims.length - 1; i >= 0; i--)
            stmt = makeForLoopBufferVar(stmt,
                                        localVariables.ARRAY_INDEX[i], dims[i]);

        return stmt;
    }

    // return the buffer access expression for the receive code
    // depends if this is a simple filter
    private JExpression bufferIndex(SIRFilter filter,
                                    LocalVariables localVariables) {
        if (isSimple(filter)) {
            return accessBufferVar(localVariables.exeIndex);    
        } else {
            // create the increment of the index var
            JPrefixExpression bufferIncrement = new JPrefixExpression(null,
                                                                      OPE_PREINC,
                                                                      accessBufferVar(localVariables.recvIndex));
            /*
             * //create the modulo expression JModuloExpression indexMod = new
             * JModuloExpression(null, bufferIncrement, new
             * JLocalVariableExpression (null, localVariables.recvBufferSize));
             */

            // create the modulo expression
            JBitwiseExpression indexAnd = new JBitwiseExpression(null,
                                                                 OPE_BAND, bufferIncrement,
                                                                 accessBufferVar(localVariables.recvBufferBits));
                    
            return indexAnd;
        }
    }

    class ConvertCommunicationSimple extends SLIRReplacingVisitor {
        LocalVariables localVariables;

        public ConvertCommunicationSimple(LocalVariables locals) {
            localVariables = locals;
        }

        // for pop expressions convert to the form
        // (recvBuffer[++recvBufferIndex % recvBufferSize])
        public Object visitPopExpression(SIRPopExpression oldSelf,
                                         CType oldTapeType) {

            // do the super
            SIRPopExpression self = (SIRPopExpression) super
                .visitPopExpression(oldSelf, oldTapeType);

            // create the increment of the index var
            JPrefixExpression increment = new JPrefixExpression(null,
                                                                OPE_PREINC,
                                                                accessBufferVar(localVariables.simpleIndex));
                    
            // create the array access expression
            JArrayAccessExpression bufferAccess = new JArrayAccessExpression(
                                                                             null, 
                                                                             accessBufferVar(localVariables.recvBuffer),
                                                                             increment);
            bufferAccess.setType(self.getType());
            // return the parenthesed expression
            return new JParenthesedExpression(null, bufferAccess);
        }

        // convert peek exps into:
        // (recvBuffer[(recvBufferIndex + (arg) + 1) mod recvBufferSize])
        public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                          CType oldTapeType, JExpression oldArg) {
            // do the super
            SIRPeekExpression self = (SIRPeekExpression) super
                .visitPeekExpression(oldSelf, oldTapeType, oldArg);

            JAddExpression argIncrement = new JAddExpression(null, self
                                                             .getArg(), new JIntLiteral(1));

            JAddExpression index = new JAddExpression(null,
                                                      accessBufferVar(localVariables.simpleIndex), 
                                                      argIncrement);

            // create the array access expression
            JArrayAccessExpression bufferAccess = new JArrayAccessExpression(
                                                                             null,
                                                                             accessBufferVar(localVariables.recvBuffer),
                                                                             index);
            bufferAccess.setType(self.getType());
            // return the parenthesed expression
            return new JParenthesedExpression(null, bufferAccess);

        }

    }

    /** 
     * Generate an expression to access <var>.  If this filter has helper methods that
     * access the tape, then the buffer and its vars are fields so generate a field access,
     * otherwise generate a local var access.
     * 
     * @param var The buffer var to access.
     * @return The buffer var access expression.
     */
    private JExpression accessBufferVar(JVariableDefinition var) {
        if (globalBuffer) {
            return new JFieldAccessExpression(null, new JThisExpression(null), var.getIdent());
        }
        else {
            return new JLocalVariableExpression(null, var);
        }
    }
    
    class ConvertCommunication extends SLIRReplacingVisitor {
        LocalVariables localVariables;

        public ConvertCommunication(LocalVariables locals) {
            localVariables = locals;
        }

        // for pop expressions convert to the form
        // (recvBuffer[++recvBufferIndex % recvBufferSize])
        public Object visitPopExpression(SIRPopExpression self, CType tapeType) {

            // create the increment of the index var
            JPrefixExpression bufferIncrement = new JPrefixExpression(null,
                                                                      OPE_PREINC,
                                                                      accessBufferVar(localVariables.recvBufferIndex));
                    

            JBitwiseExpression indexAnd = new JBitwiseExpression(null,
                                                                 OPE_BAND, bufferIncrement,
                                                                 accessBufferVar(localVariables.recvBufferBits));
            /*
             * //create the modulo expression JModuloExpression indexMod = new
             * JModuloExpression(null, bufferIncrement, new
             * JLocalVariableExpression (null, localVariables.recvBufferSize));
             */

            // create the array access expression
            JArrayAccessExpression bufferAccess = new JArrayAccessExpression(
                                                                             null, 
                                                                             accessBufferVar(localVariables.recvBuffer),
                                                                             indexAnd);
            bufferAccess.setType(self.getType());
            // return the parenthesed expression
            return new JParenthesedExpression(null, bufferAccess);
        }

        // convert peek exps into:
        // (recvBuffer[(recvBufferIndex + (arg) + 1) mod recvBufferSize])
        public Object visitPeekExpression(SIRPeekExpression oldSelf,
                                          CType oldTapeType, JExpression oldArg) {
            // do the super
            SIRPeekExpression self = (SIRPeekExpression) super
                .visitPeekExpression(oldSelf, oldTapeType, oldArg);

            // create the index calculation expression
            JAddExpression argIncrement = new JAddExpression(null, self
                                                             .getArg(), new JIntLiteral(1));
            JAddExpression index = new JAddExpression(null,
                                                      accessBufferVar(localVariables.recvBufferIndex),
                                                      argIncrement);

            JBitwiseExpression indexAnd = new JBitwiseExpression(null,
                                                                 OPE_BAND, index,
                                                                 accessBufferVar(localVariables.recvBufferBits));
                        
            /*
             * //create the mod expression JModuloExpression indexMod = new
             * JModuloExpression(null, index, new JLocalVariableExpression
             * (null, localVariables.recvBufferSize));
             */

            // create the array access expression
            JArrayAccessExpression bufferAccess = new JArrayAccessExpression(
                                                                             null,
                                                                             accessBufferVar(localVariables.recvBuffer),
                                                                             indexAnd);
            bufferAccess.setType(self.getType());
            // return the parenthesed expression
            return new JParenthesedExpression(null, bufferAccess);
        }

    }

    // this pass will convert all push statements to a method call expression
    // so that in FlatIRtoC we can recognize the call and replace it with a
    // store to
    // the output buffer instead of a send onto the network...
    class RateMatchConvertPush extends SLIRReplacingVisitor {
        LocalVariables localVariables;

        public RateMatchConvertPush(LocalVariables localVariables) {
            this.localVariables = localVariables;
        }

        /**
         * Visits a push expression.
         */
        public Object visitPushExpression(SIRPushExpression self,
                                          CType tapeType, JExpression arg) {
            JExpression newExp = (JExpression) arg.accept(this);
            return new JAssignmentExpression(null, new JArrayAccessExpression(
                                                                              null, 
                                                                              accessBufferVar(localVariables.sendBuffer),
                                                                              new JPrefixExpression(null, OPE_PREINC,
                                                                                                    accessBufferVar(localVariables.sendBufferIndex))),
                                             newExp);

            /*
             * JExpression[] args = new JExpression[1]; args[0] = newExp;
             * 
             * JMethodCallExpression ratematchsend = new
             * JMethodCallExpression(null, new JThisExpression(null),
             * RawExecutionCode.rateMatchSendMethod, args);
             * 
             * return ratematchsend;
             */
        }
    }

    /**
     * Returns a for loop that uses field <var> to count <count> times with the
     * body of the loop being <body>. If count is non-positive, just returns
     * empty (!not legal in the general case).  It will use either a local variable access 
     * or a field access depending on the value of globalBuffer.
     */
    private JStatement makeForLoopBufferVar(JStatement body, JVariableDefinition var,
                                            JExpression count) {
        if (body == null)
            return new JEmptyStatement(null, null);

        // make init statement - assign zero to <var>. We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = { new JAssignmentExpression(null,
                                                             accessBufferVar(var),
                                                             new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(null, initExpr, null);
        // if count==0, just return init statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral) count).intValue();
            if (intCount <= 0) {
                // return assignment statement
                return new JEmptyStatement(null, null);
            }
        }
        // make conditional - test if <var> less than <count>
        JExpression cond = new JRelationalExpression(null, Constants.OPE_LT,
                                                     accessBufferVar(var),
                                                     count);
        JExpression incrExpr = new JPostfixExpression(null,
                                                      Constants.OPE_POSTINC, 
                                                      accessBufferVar(var));
    
        JStatement incr = new JExpressionStatement(null, incrExpr, null);

        return new JForStatement(null, init, cond, incr, body, null);
    }



   
    
}

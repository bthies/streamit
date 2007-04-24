package at.dms.kjc.vanillaSlice;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.common.*;
/**
 * Create kopi code for an {@link at.dms.kjc.slicegraph.InputSliceNode}.
 * @author dimock
 */
public class ProcessInputSliceNode {
    
    /** set of filters for which we have written basic code. */
    // uses WeakHashMap to be self-cleaning, but now have to insert some value.
    private static Map<SliceNode,Boolean>  basicCodeWritten = new WeakHashMap<SliceNode,Boolean>();

    /**
     * Create code for a InputSliceNode.
     * @param <T>          type of a BackEndFactory to access layout, etc.
     * @param inputNode    the InputSliceNode that may need code generated.
     * @param whichPhase   a scheduling phase {@link SchedulingPhase}
     * @param backEndBits  a BackEndFactory to access layout, etc.
     */
    public static <T extends BackEndFactory> void processInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, T backEndBits) {
        // No code generated for inputNode if there is no input.
        if (!backEndBits.sliceHasUpstreamChannel(inputNode.getParent())) { return; }
        
        CodeStoreHelper joiner_code = CodeStoreHelper.findHelperForSliceNode(inputNode);
        
        if (joiner_code == null) {
            joiner_code = getJoinerCode(inputNode,backEndBits);
        }
        
        ComputeNode location = backEndBits.getLayout().getComputeNode(inputNode);
        assert location != null;
        ComputeCodeStore codeStore = location.getComputeCode();
        switch (whichPhase) {
        case INIT:
            // Have the main function for the CodeStore call our init if any
            codeStore.addInitFunctionCall(joiner_code.getInitMethod());
            JMethodDeclaration workAtInit = joiner_code.getInitStageMethod();
            if (workAtInit != null) {
                // if there are calls to work needed at init time then add
                // method to general pool of methods
                codeStore.addMethod(workAtInit);
                // and add call to list of calls made at init time.
                // Note: these calls must execute in the order of the
                // initialization schedule -- so caller of this routine 
                // must follow order of init schedule.
                codeStore.addInitStatement(new JExpressionStatement(null,
                        new JMethodCallExpression(null, new JThisExpression(null),
                                workAtInit.getName(), new JExpression[0]), null));
            }
            break;
        case PRIMEPUMP:
            JMethodDeclaration primePump = joiner_code.getPrimePumpMethod();
            if (primePump != null && ! codeStore.hasMethod(primePump)) {
                // Add method -- but only once
                codeStore.addMethod(primePump);
            }
            if (primePump != null) {
                // for each time this method is called, it adds another call
                // to the primePump routine to the initialization.
                codeStore.addInitStatement(new JExpressionStatement(
                                null,
                                new JMethodCallExpression(null,
                                        new JThisExpression(null), primePump
                                                .getName(), new JExpression[0]),
                                null));

            }
            break;
        case STEADY:
            JStatement steadyBlock = joiner_code.getSteadyBlock();
            // helper has now been used for the last time, so we can write the basic code.
            // write code deemed useful by the helper into the corrrect ComputeCodeStore.
            // write only once if multiple calls for steady state.
            if (!basicCodeWritten.containsKey(inputNode)) {
                codeStore.addFields(joiner_code.getUsefulFields());
                codeStore.addMethods(joiner_code.getUsefulMethods());
                basicCodeWritten.put(inputNode,true);
            }
            codeStore.addSteadyLoopStatement(steadyBlock);
            break;
        }

    }
    /**
         * Create fields and code for a joiner, as follows.
         * Do not create a joiner if all weights are 0: this code
         * fails rather than creating nonsensical kopi code.
         * Note that this <b>always</b> creates code, if you want to reuse
         * any existing code call {@link #getJoinerCode(InputSliceNode, BackEndFactory) getJoinerCode} instead.
         * <pre>
    joiner as a state machine, driven off arrays:
    
    / * joiner (unless single edge, just delegated to a channel 
        arity (4) and weight s but not duplication.
      * /
    
    T pop_1_M() {fprintf(stderr, "pop_1_M\n"); return 0;}
    T pop_2_M() {fprintf(stderr, "pop_2_M\n"); return 0;}
    T pop_4_M() {fprintf(stderr, "pop_4_M\n"); return 0;}
    
    
    static int joiner_M_edge = 4 - 1;
    static int joiner_M_weight = 0;
    
    static inline T joiner_M() {
    
      / * attempt to place const eitherapplies it to function, or gives parse error
       * do we need to move this to file scope to convince inliner to work on joiner_M?
       * /
      static T (*pops[4])() = {
        pop_1_M,
        pop_2_M,
        0,              / * 0-weight edge * /
        pop_4_M
      };
    
      static const int weights[4] = {2, 1, 0, 2};
    
      while (joiner_M_weight == 0) { / * "if" if do not generate for 0-length edges. * /
        joiner_M_edge = (joiner_M_edge + 1) % 4;
        joiner_M_weight = weights[joiner_M_edge];
      }
      joiner_M_weight--;
    
      return pops[joiner_M_edge]();
    }
    
    joiner as a case statement, which is what we implement:
    
    
    static int joiner_M_unrolled_edge = 3 - 1;
    static int joiner_M_unrolled_weight = 0;
    
    static inline T joiner_M_unrolled() {
    
      static const int weights[3] = {2-1, 1-1, 2-1};
    
      if (--joiner_M_unrolled_weight < 0) {
        joiner_M_unrolled_edge = (joiner_M_unrolled_edge + 1) % 3;
        joiner_M_unrolled_weight = weights[joiner_M_unrolled_edge];
      }
    
      switch (joiner_M_unrolled_edge) {
      case 0:
        return pop_1_M();
      case 1:
        return pop_2_M();
      case 2:
        return pop_4_M();
      }
    }
         * </pre>
         * @param joiner An InputSliceNode specifying joiner weights and edges.
         * @param backEndBits to get info from appropriate BackEndFactory
         * @param helper CodeStoreHelper to get the fields and method implementing the joiner
         */
        static <T extends BackEndFactory> void makeJoinerCode(InputSliceNode joiner,
                T backEndBits, CodeStoreHelper helper) {
            String joiner_name = "_joiner_" + ProcessFilterSliceNode.getUid();
            String joiner_method_name =  joiner_name + joiner.getNextFilter().getFilter().getName();
            
            // size is number of edges with non-zero weight.
            int size = 0;
            for (int w : joiner.getWeights()) {
                if (w != 0) {size++;}
            }
            
            assert size > 0 : "asking for code generation for null joiner";
            
            String edge_name = joiner_name + "_edge";
            String weight_name = joiner_name + "_weight";
    
            JVariableDefinition edgeVar = new JVariableDefinition(
                    at.dms.kjc.Constants.ACC_STATIC,
                    CStdType.Integer,
                    edge_name,
                    new JIntLiteral(size - 1));
            
            JFieldDeclaration edgeDecl = new JFieldDeclaration(edgeVar);
            JFieldAccessExpression edgeExpr = new JFieldAccessExpression(edge_name);
            
            JVariableDefinition weightVar = new JVariableDefinition(
                    at.dms.kjc.Constants.ACC_STATIC,
                    CStdType.Integer,
                    weight_name,
                    new JIntLiteral(0));
    
            JFieldDeclaration weightDecl = new JFieldDeclaration(weightVar);
            JFieldAccessExpression weightExpr = new JFieldAccessExpression(weight_name);
            
            JIntLiteral[] weightVals = new JIntLiteral[size];
            {
                int i = 0;
                for (int w : joiner.getWeights()) {
                    if (w != 0) {
                        weightVals[i++] = new JIntLiteral(w - 1);
                    }
                }
            }
            
            JVariableDefinition weightsArray = new JVariableDefinition(
                    at.dms.kjc.Constants.ACC_STATIC | at.dms.kjc.Constants.ACC_FINAL,  // static const in C
                    new CArrayType(CStdType.Integer,
                            1, new JExpression[]{new JIntLiteral(size)}),
                    "weights",
                    new JArrayInitializer(weightVals));
            JLocalVariableExpression weightsExpr = new JLocalVariableExpression(weightsArray);
            
            JStatement next_edge_weight_stmt = new JIfStatement(null,
                    new JRelationalExpression(at.dms.kjc.Constants.OPE_LT,
                            new JPrefixExpression(null,
                                    at.dms.kjc.Constants.OPE_PREDEC,
                                    weightExpr),
                            new JIntLiteral(0)),
                    new JBlock(new JStatement[]{
                            new JExpressionStatement(new JAssignmentExpression(
                                    edgeExpr,
                                    new JModuloExpression(null,
                                            new JAddExpression(
                                                    edgeExpr,
                                                    new JIntLiteral(1)),
                                            new JIntLiteral(size)))),
                            new JExpressionStatement(new JAssignmentExpression(
                                    weightExpr,
                                    new JArrayAccessExpression(weightsExpr,
                                            edgeExpr)
                                    ))
                    }),
                    new JEmptyStatement(),
                    null);
    
            
            JSwitchGroup[] cases = new JSwitchGroup[size]; // fill in later.
            JStatement switch_on_edge_stmt = new JSwitchStatement(null,
                    edgeExpr,
                    cases,
                    null);
            
            {
                int i = 0;
                for (int j = 0; j < joiner.getWeights().length; j++) {
                    if (joiner.getWeights()[j] != 0) {
                        JMethodCallExpression pop = new JMethodCallExpression(
                                backEndBits.getChannel(joiner.getSources()[j]).popMethodName(),
                                new JExpression[0]);
                        pop.setType(joiner.getType());
    
                        cases[i] = new JSwitchGroup(null,
                                new JSwitchLabel[]{new JSwitchLabel(null,new JIntLiteral(i))},
                                new JStatement[]{
                                   new JReturnStatement(null,
                                           pop,
                                           null)});
                        i++;
                    }
                }
            }
    
            
            JMethodDeclaration joiner_method = new JMethodDeclaration(
                    null, at.dms.kjc.Constants.ACC_STATIC | at.dms.kjc.Constants.ACC_INLINE,
                    joiner.getType(),
                    joiner_method_name,
                    new JFormalParameter[]{},
                    new CClassType[]{},
                    new JBlock(),
                    null, null);
            
            JBlock joiner_block = joiner_method.getBody();
            
            joiner_block.addStatement(
                    new JVariableDeclarationStatement(
                            new JVariableDefinition[]{weightsArray}));
            joiner_block.addStatement(next_edge_weight_stmt);
            joiner_block.addStatement(switch_on_edge_stmt);
            
            
            helper.addFields(new JFieldDeclaration[]{edgeDecl, weightDecl});
            helper.addMethod(joiner_method);
        }

         
        /**
         * Make a work function for a joiner 
         */
        
        static <T extends BackEndFactory> void makeJoinerWork(InputSliceNode joiner,
                T backEndBits, CodeStoreHelper joiner_code) {
            JMethodDeclaration joinerWork;

            // the work function will need a temporary variable
            ALocalVariable t = ALocalVariable.makeTmp(joiner.getEdgeToNext().getType());

            Channel downstream = backEndBits.getChannel(joiner.getEdgeToNext());

            // the body of the work method
            JBlock body = new JBlock();
            
            if (backEndBits.sliceNeedsJoinerCode(joiner.getParent())) {
                // There should be generated code for the joiner
                // state machine in the CodeStoreHelper as the only method.
                //
                // generated code is
                // T tmp;
                // tmp = joiner_code();
                // push(tmp);
                //
                // TODO: inline the joiner code at the call site,
                // if inlining, delete joiner code after inlining leaving
                // only this method in the helper.
                assert joiner_code.getMethods().length == 1;
                JMethodDeclaration callable_joiner = joiner_code.getMethods()[0];
                
                body.addStatement(t.getDecl());
                body.addStatement(new JExpressionStatement(
                        new JAssignmentExpression(
                                t.getRef(),
                                new JMethodCallExpression(
                                        callable_joiner.getName(),
                                        new JExpression[0]
                                ))));
                body.addStatement(new JExpressionStatement(
                        new JMethodCallExpression(
                                downstream.pushMethodName(),
                                new JExpression[]{t.getRef()})));
            } else {
                // slice does not need joiner code, so just transfer from upstream
                // to downstream buffer.
                //
                // generated code is
                // T tmp;
                // tmp = pop();
                // push(tmp);
                //
                assert joiner.getWidth() == 1;
                Channel upstream = backEndBits.getChannel(joiner.getSingleEdge());

                body.addStatement(t.getDecl());
                body.addStatement(new JExpressionStatement(
                        new JAssignmentExpression(
                                t.getRef(),
                                new JMethodCallExpression(
                                        upstream.popMethodName(),
                                        new JExpression[0]
                                ))));
                body.addStatement(new JExpressionStatement(
                        new JMethodCallExpression(
                                downstream.pushMethodName(),
                                new JExpression[]{t.getRef()})));

            }
            joinerWork = new JMethodDeclaration(
                    CStdType.Void,
                    "_joinerWork_" + joiner.getNextFilter().getFilter().getName(),
                    JFormalParameter.EMPTY,
                    body);
            joiner_code.setWorkMethod(joinerWork);
        }
        
        /**
         * Get code for a joiner.
         * If code not yet made then makes it.
         * @param <T> Type of the caller's BackEndFactory.
         * @param joiner
         * @param backEndBits
         * @return
         */
        static <T extends BackEndFactory> CodeStoreHelper getJoinerCode(InputSliceNode joiner, T backEndBits) {
            CodeStoreHelper joiner_code = CodeStoreHelper.findHelperForSliceNode(joiner);
            if (joiner_code == null) {
                joiner_code = backEndBits.getCodeStoreHelper(joiner);
                if (backEndBits.sliceNeedsJoinerCode(joiner.getParent())) {
                    makeJoinerCode(joiner,backEndBits,joiner_code);
                }
                if (backEndBits.sliceNeedsJoinerWorkFunction(joiner.getParent())) {
                    makeJoinerWork(joiner,backEndBits,joiner_code);
                }
                CodeStoreHelper.addHelperForSliceNode(joiner, joiner_code);
            }
            return joiner_code;
        }
}

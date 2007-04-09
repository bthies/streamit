package at.dms.kjc.vanillaSlice;

import at.dms.kjc.CArrayType;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JArrayInitializer;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JEmptyStatement;
import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIfStatement;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JModuloExpression;
import at.dms.kjc.JPrefixExpression;
import at.dms.kjc.JRelationalExpression;
import at.dms.kjc.JReturnStatement;
import at.dms.kjc.JStatement;
import at.dms.kjc.JSwitchGroup;
import at.dms.kjc.JSwitchLabel;
import at.dms.kjc.JSwitchStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BufferSize;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.MinCodeUnit;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.backendSupport.SliceNodeToCodeUnit;
import at.dms.kjc.sir.SIRCodeUnit;
import at.dms.kjc.slicegraph.InputSliceNode;
/**
 * Create kopi code for an {@link at.dms.kjc.slicegraph.InputSliceNode}.
 * @author dimock
 */
public class ProcessInputSliceNode {
    
    /**
     * Create code for a InputSliceNode.
     * @param <T>          type of a BackEndFactory to access layout, etc.
     * @param inputNode    the InputSliceNode that may need code generated.
     * @param whichPhase   a scheduling phase {@link SchedulingPhase}
     * @param backEndBits  a BackEndFactory to access layout, etc.
     */
    public static <T extends BackEndFactory> void processInputSliceNode(InputSliceNode inputNode, 
            SchedulingPhase whichPhase, T backEndBits) {
        /* An input slice needs a work function if downstream filter can not call the joiner
         * code directly, which happens if there is a peek buffer between the joiner and
         * the downstream filter: Something needs to push into the peek buffer.
         */
        if (UniChannel.sliceNeedsJoinerCode(inputNode.getParent())) {
            int initPushes = inputNode.getNextFilter().getFilter().initItemsNeeded();
            int steadyPushes = inputNode.getNextFilter().getFilter().getPopInt() *
                inputNode.getNextFilter().getFilter().getSteadyMult();
            // create joiner code and preWork, work Functions to push into next Channel.
            // add these...
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
         * @return a SIRCodeUnit (fields and single method declaration) implementing the joiner
         */
        static <T extends BackEndFactory> SIRCodeUnit makeJoinerCode(InputSliceNode joiner, T backEndBits) {
            String joiner_name = "_joiner_" + ProcessFilterSliceNode.getUid();
            
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
                    new JIntLiteral(0));
            
            JFieldDeclaration edgeDecl = new JFieldDeclaration(edgeVar);
            JFieldAccessExpression edgeExpr = new JFieldAccessExpression(edge_name);
            
            JVariableDefinition weightVar = new JVariableDefinition(
                    at.dms.kjc.Constants.ACC_STATIC,
                    CStdType.Integer,
                    weight_name,
                    new JIntLiteral(size - 1));
    
            JFieldDeclaration weightDecl = new JFieldDeclaration(weightVar);
            JFieldAccessExpression weightExpr = new JFieldAccessExpression(weight_name);
            
            JIntLiteral[] weightVals = new JIntLiteral[size];
            {
                int i = 0;
                for (int w : joiner.getWeights()) {
                    if (w != 0) {
                        weightVals[i++] = new JIntLiteral(w);
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
                    new JSwitchGroup[size],
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
                    joiner_name,
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
            
            
            SIRCodeUnit retval = new MinCodeUnit(
                    new JFieldDeclaration[]{edgeDecl, weightDecl},
                    new JMethodDeclaration[]{joiner_method});
            
            return retval;
        }

        /**
         * Get code for a joiner.
         * If code not yet made then makes it.
         * @param <T> Type of the caller's BackEndFactory.
         * @param joiner
         * @param backEndBits
         * @return
         */
        static <T extends BackEndFactory> SIRCodeUnit getJoinerCode(InputSliceNode joiner, T backEndBits) {
            SIRCodeUnit joiner_code = SliceNodeToCodeUnit.findCodeForSliceNode(joiner);
            if (joiner_code == null) {
                joiner_code = makeJoinerCode(joiner,backEndBits);
            }
            return joiner_code;
        }
 
        /**
         * Get code for a joiner and add it to the appropriate ComputeCodeStore.
         * @param <T> Type of the caller's BackEndFactory.
         * @param joiner
         * @param backEndBits
         */
        static <T extends BackEndFactory> void addJoinerCode(InputSliceNode joiner, T backEndBits) {
            SIRCodeUnit joiner_code = SliceNodeToCodeUnit.findCodeForSliceNode(joiner);
            if (joiner_code == null) {
                joiner_code = makeJoinerCode(joiner,backEndBits);
                ComputeNode location = backEndBits.getLayout().getComputeNode(joiner);
                assert location != null;
                location.getComputeCode().addFields(joiner_code.getFields());
                location.getComputeCode().addMethods(joiner_code.getMethods());
            }
        }
}

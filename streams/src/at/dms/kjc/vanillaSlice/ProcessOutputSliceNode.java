package at.dms.kjc.vanillaSlice;

import at.dms.kjc.CArrayType;
import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.JAddExpression;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JArrayInitializer;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBreakStatement;
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
import at.dms.kjc.JStatement;
import at.dms.kjc.JSwitchGroup;
import at.dms.kjc.JSwitchLabel;
import at.dms.kjc.JSwitchStatement;
import at.dms.kjc.JVariableDeclarationStatement;
import at.dms.kjc.JVariableDefinition;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.MinCodeUnit;
import at.dms.kjc.sir.SIRCodeUnit;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.OutputSliceNode;

/**
 * Create kopi code for an {@link at.dms.kjc.slicegraph.OutputSliceNode}.
 * @author dimock
 *
 */
public class ProcessOutputSliceNode {

    /**
         * Create fields and code for a splitter, as follows.
         * Do not create a splitter if all weights are 0: this code
         * fails rather than creating nonsensical kopi code.
         * <pre>
    splitter as a state machine, driven off arrays:
    
    void push_1_1(T v) {fprintf(stderr, "push_1_1 %d\n", v);}
    void push_3_1(T v) {fprintf(stderr, "push_3_1 %d\n", v);}
    void push_3_2(T v) {fprintf(stderr, "push_3_2 %d\n", v);}
    
    / *
     Splitter as state machine.
     Need arity (3 here), max duplication (2 here).
     Special case if arity == 1: always call push_N_M directly.
     * /
    
    static int splitter_N_dim1 = 3-1;
    static int splitter_N_weight = 0;
    
    static inline void splitter_N (T val) {
      static void (*pushes[3][2])(T) = {
        {push_1_1, NULL},       / * edge * /
        {NULL, NULL},       / * 0-weight edge * /
        {push_3_1, push_3_2}    / * duplicating edge * /
      };
    
      static const int weights[3] = {1, 0, 4};
      int dim2;
    
      while (splitter_N_weight == 0) {
          splitter_N_dim1 = (splitter_N_dim1 + 1) % 3;
          splitter_N_weight = weights[splitter_N_dim1];
      }
    
      dim2 = 0;
      while (dim2 < 2 && pushes[splitter_N_dim1][dim2] != NULL) {
        pushes[splitter_N_dim1][dim2++](val);
      }
      splitter_N_weight--;
    }
    
    
    splitter as actually implemented:
    
    / * inline the array stuff, 
     * remove 0-weight edge 
     * /
    static int splitter_N_unrolled_edge = 2 - 1;
    static int splitter_N_unrolled_weight = 0;
    
    static inline void splitter_N_unrolled(T val) {
    
      static const int weights[2] = {1-1,4-1};
    
      if (--splitter_N_unrolled_weight < 0) {
        splitter_N_unrolled_edge = (splitter_N_unrolled_edge + 1) % 2;
        splitter_N_unrolled_weight = weights[splitter_N_unrolled_edge];
      }
    
      switch (splitter_N_unrolled_edge) {
      case 0:
        push_1_1(val);
        break;
      case 1:
        push_3_1(val); 
        push_3_2(val);
        break;
      }
    }
    
         </pre>
         * @param splitter
         * @return
         */
        static <T extends BackEndFactory> SIRCodeUnit makeSplitterCode(OutputSliceNode splitter, T backEndBits) {
            String splitter_name = "_splitter_" + ProcessFilterSliceNode.getUid();
            
            // size is number of edges with non-zero weight.
            int size = 0;
            for (int w : splitter.getWeights()) {
                if (w != 0) {size++;}
            }
            
            assert size > 0 : "asking for code generation for null splitter";
            
            String edge_name = splitter_name + "_edge";
            String weight_name = splitter_name + "_weight";
    
            String param_name = "arg";
            JFormalParameter arg = new JFormalParameter(splitter.getType(),param_name);
            JExpression argExpr = new JLocalVariableExpression(arg);
            
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
                for (int w : splitter.getWeights()) {
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
                    cases,
                    null);
            
            {
                int i = 0;
                for (int j = 0; j < splitter.getWeights().length; j++) {
                    if (splitter.getWeights()[j] != 0) {
                        Edge[] edges = splitter.getDests()[j];
                        JStatement[] pushes = new JStatement[edges.length + 1];
                        for (int k = 0; k < edges.length; k++) {
                            pushes[k] = new JExpressionStatement(
                                new JMethodCallExpression(
                                        backEndBits.getChannel(edges[k]).pushMethodName(),
                                    new JExpression[]{argExpr}));
                        }
                        pushes[edges.length] = new JBreakStatement(null,null,null);
                        
                        cases[i] = new JSwitchGroup(null,
                                new JSwitchLabel[]{new JSwitchLabel(null,new JIntLiteral(i))},
                                pushes);
                        i++;
                    }
                }
            }
    
            JMethodDeclaration splitter_method = new JMethodDeclaration(
                    null, 
                    at.dms.kjc.Constants.ACC_STATIC | at.dms.kjc.Constants.ACC_INLINE,
                    CStdType.Void,
                    splitter_name,
                    new JFormalParameter[]{},
                    new CClassType[]{},
                    new JBlock(),
                    null, null);
            
            JBlock splitter_block = splitter_method.getBody();
            
            splitter_block.addStatement(
                    new JVariableDeclarationStatement(
                            new JVariableDefinition[]{weightsArray}));
            splitter_block.addStatement(next_edge_weight_stmt);
            splitter_block.addStatement(switch_on_edge_stmt);
            
            
            SIRCodeUnit retval = new MinCodeUnit(
                    new JFieldDeclaration[]{edgeDecl, weightDecl},
                    new JMethodDeclaration[]{splitter_method});
            
            return retval;
    
        }

        
        /**
         * Get code for a splitter.
         * If code not yet made, then makes it.
         * @param <T>
         * @param splitter
         * @param backEndBits
         * @return
         */
        static <T extends BackEndFactory> SIRCodeUnit getSplitterCode(OutputSliceNode splitter, T backEndBits) {
            SIRCodeUnit splitter_code = CodeStoreHelper.findHelperForSliceNode(splitter);
            if (splitter_code == null) {
                splitter_code = makeSplitterCode(splitter,backEndBits);
            }
            return splitter_code;
        }

        /**
         * Get code for a joiner and add it to the appropriate ComputeCodeStore.
         * If code not yet made, then makes the code and adds it to the appropriate ComputeCodeStore
         * @param <T> Type of the caller's BackEndFactory.
         * @param splitter
         * @param backEndBits
         */
        static <T extends BackEndFactory> void addSplitterCode(OutputSliceNode splitter, T backEndBits) {
            SIRCodeUnit splitter_code = CodeStoreHelper.findHelperForSliceNode(splitter);
            if (splitter_code == null) {
                splitter_code = makeSplitterCode(splitter,backEndBits);
                ComputeNode location = backEndBits.getLayout().getComputeNode(splitter);
                assert location != null;
                location.getComputeCode().addFields(splitter_code.getFields());
                location.getComputeCode().addMethods(splitter_code.getMethods());
            }
        }
}

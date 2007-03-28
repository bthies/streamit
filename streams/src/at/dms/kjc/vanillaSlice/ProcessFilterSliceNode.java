package at.dms.kjc.vanillaSlice;

import java.util.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.Constants;
import at.dms.util.Utils;
/**
 * Process a FilterSliceNode creating code in the code store and buffers for connectivity.
 * @author dimock
 *
 */
public class ProcessFilterSliceNode implements Constants {
    
    private static int uid = 0;
    private static int getUid() {
        return uid++;
    }

    public static Map<SliceNode,SIRCodeUnit> codesForSlices = new HashMap<SliceNode,SIRCodeUnit>();
    
    /**
     * Create code for a FilterSliceNode (actually for the whole slice).
     * @param <T>          type of a BackEndFactory to access layout, etc.
     * @param filterNode   the filterNode that needs code generated.
     * @param whichPhase   a scheduling phase {@link SchedulingPhase}
     * @param backEndBits  a BackEndFactory to access layout, etc.
     */
    public static <T extends BackEndFactory> void processFilterSliceNode(FilterSliceNode filterNode, 
            SchedulingPhase whichPhase, T backEndBits) {

        // We should only generate code once for a filter node.
        
        if (codesForSlices.containsKey(filterNode)) {
            return; 
        }
        
        FilterInfo info = FilterInfo.getFilterInfo(filterNode);
        Slice slice = filterNode.getParent();
        assert slice instanceof SimpleSlice;  // we only handle joiner -- filter -- splitter pattern.

        ComputeNode computeNode = backEndBits.getLayout().getComputeNode(filterNode);
        ComputeCodeStore codeStore = computeNode.getComputeCode();
        
        InputSliceNode joiner = slice.getHead();    // get associated joiner and splitter
        OutputSliceNode splitter = slice.getTail();
        
        // determine connectivity, needed to know what buffers to ask for
        // and whether to create splitter or joiner code.
        
        boolean has_upstream_buffer = false;
        boolean make_peek_buffer = false;
        boolean make_joiner = false;
        /*
         * Determine upstream connectivity
         */
        if (info.noBuffer()) {
            // there is no upstream
            assert filterNode.getFilter().getInputType() == CStdType.Void;
        } else {
            // there is an upstream, need inter-slice buffer
            has_upstream_buffer = true;
            if (info.isSimple()) {
                // there is no need for a peek buffer in front of upstream buffer.
                if (joiner.getWidth() == 1) {
                    // no joining logic: just connect to the inter-slice buffer.
                } else {
                    // needs a joiner
                    make_joiner = true;
                    if (Utils.hasPeeks(filterNode.getFilter())) {
                        // if peeks as well as pops, we will give it a peek buffer
                        // rather than trying to peek through a joiner.
                        make_peek_buffer = true;
                    }
                }
            } else {
                // need a peek buffer
                make_peek_buffer = true;
                if (joiner.getWidth() == 1) {
                    // no joining logic: just connect to the inter-slice buffer.
                    // can even do without peek buffer if inter-slice buffer supports copy-down
                } else {
                    // needs a joiner
                    make_joiner = true;
                }
            }
        }
        /*
         * Determine downstream connectivity
         */
        boolean has_downstream_buffer = false;
        boolean make_splitter = false;
        if (info.push > 0 || info.prePush > 0) {
            assert filterNode.getFilter().getOutputType() != CStdType.Void;
            has_downstream_buffer = true;
            if (splitter.getWidth() > 1) {
                make_splitter = true;
            }
        }

        
        /*
         * Make joiner and/or splitter if necesary.
         */

        Channel inputChannel = null;
        

        if (make_joiner) {
            SIRCodeUnit joiner_code = makeJoinerCode(joiner);
            codeStore.addFields(joiner_code.getFields());
            codeStore.addMethods(joiner_code.getMethods());
            if (! make_peek_buffer) {
                inputChannel = UnbufferredPopChannel.getChannel(joiner.getEdgeToNext(), 
                        joiner_code.getMethods()[0].getName());
            }
        }
        
        Channel outputChannel = null;
        
        if (make_splitter) {
            SIRCodeUnit splitter_code =  makeSplitterCode(splitter);
            codeStore.addFields(splitter_code.getFields());
            codeStore.addMethods(splitter_code.getMethods());
            
            outputChannel =  UnbufferredPushChannel.getChannel(filterNode.getEdgeToNext(),
                    splitter_code.getMethods()[0].getName());
        } else {
            outputChannel = Channel.getChannel(splitter.getDests()[0][0]);
        }
        
        
        /*
         * Make Channel for peek buffer if necessary.
         */
        
        if (make_peek_buffer) {
            if (! make_joiner) {
                inputChannel = Channel.getChannel(joiner.getSingleEdge());
            }
        }
        
        SIRCodeUnit filter_code = makeFilterCode(filterNode.getFilter(), inputChannel, outputChannel);
        
        codesForSlices.put(filterNode, filter_code);
    }

    
    /**
     * Take a code unit (here a FilterContent) and return one with all push, peek, pop 
     * replaced with calls to channel routines.
     * Clones the input methods and munges on the clones, further changes to the returned code
     * will not affect the 
     * @param code           The code (fields and methods)
     * @param inputChannel   The input channel -- specified routines to call to replace peek, pop.
     * @param outputChannel  The output channel -- specified routeines to call to replace push.
     * @return a SIRCodeUnit with no push, peek, or pop instructions.
     */
    private static SIRCodeUnit makeFilterCode(SIRCodeUnit code, 
            Channel inputChannel, Channel outputChannel) {
        
        final String peekName = inputChannel.peekMethodName();
        final String popName = inputChannel.popMethodName();
        final String pushName = outputChannel.pushMethodName();
        final String popManyName = inputChannel.popManyMethodName();

        
        JMethodDeclaration[] oldMethods = code.getMethods();
        JMethodDeclaration[] methods = new JMethodDeclaration[oldMethods.length];
        for (int i = 0; i < oldMethods.length; i++) {
            methods[i] = (JMethodDeclaration)AutoCloner.deepCopy(oldMethods[i]);
        }
        
        for (JMethodDeclaration method : methods) {
            method.accept(new SLIRReplacingVisitor(){
                @Override
                public Object visitPopExpression(SIRPopExpression self,
                        CType tapeType) {
                    if (self.getNumPop() > 1) {
                        return new JMethodCallExpression(popManyName, 
                                new JExpression[]{new JIntLiteral(self.getNumPop())});
                    } else {
                        return new JMethodCallExpression(popName, 
                                new JExpression[0]);
                    }
                }
                
                @Override
                public Object visitPeekExpression(SIRPeekExpression self,
                        CType tapeType,
                        JExpression arg) {
                    JExpression newArg = (JExpression)arg.accept(this);
                    return new JMethodCallExpression(peekName, new JExpression[]{newArg});
                }
                
                @Override
                public Object visitPushExpression(SIRPushExpression self,
                        CType tapeType,
                        JExpression arg) {
                    JExpression newArg = (JExpression)arg.accept(this);
                    return new JMethodCallExpression(pushName, new JExpression[]{newArg});
                }
            });
        }
        
        return new MinCodeUnit(code.getFields(),methods);
    }
    
    /**
     * Create fields and code for a joiner, as follows.
     * Do not create a joiner if all weights are 0: this code
     * fails rather than creating nonsensical kopi code.
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
    private static SIRCodeUnit makeJoinerCode(InputSliceNode joiner) {
        String joiner_name = "_joiner_" + getUid();
        
        // size is number of edges with non-zero weight.
        int size = 0;
        for (int w : joiner.getWeights()) {
            if (w != 0) {size++;}
        }
        
        assert size > 0 : "asking for code generation for null joiner";
        
        String edge_name = joiner_name + "_edge";
        String weight_name = joiner_name + "_weight";

        JVariableDefinition edgeVar = new JVariableDefinition(
                ACC_STATIC,
                CStdType.Integer,
                edge_name,
                new JIntLiteral(0));
        
        JFieldDeclaration edgeDecl = new JFieldDeclaration(edgeVar);
        JFieldAccessExpression edgeExpr = new JFieldAccessExpression(edge_name);
        
        JVariableDefinition weightVar = new JVariableDefinition(
                ACC_STATIC,
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
                ACC_STATIC | ACC_FINAL,  // static const in C
                new CArrayType(CStdType.Integer,
                        1, new JExpression[]{new JIntLiteral(size)}),
                "weights",
                new JArrayInitializer(weightVals));
        JLocalVariableExpression weightsExpr = new JLocalVariableExpression(weightsArray);
        
        JStatement next_edge_weight_stmt = new JIfStatement(null,
                new JRelationalExpression(OPE_LT,
                        new JPrefixExpression(null,
                                OPE_PREDEC,
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
                            Channel.getChannel(joiner.getSources()[j]).popMethodName(),
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
                null, ACC_STATIC /* | ACC_INLINE */,  // there is no ACC_INLINE
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
    private static SIRCodeUnit makeSplitterCode(OutputSliceNode splitter) {
        String splitter_name = "_splitter_" + getUid();
        
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
                ACC_STATIC,
                CStdType.Integer,
                edge_name,
                new JIntLiteral(0));
        
        JFieldDeclaration edgeDecl = new JFieldDeclaration(edgeVar);
        JFieldAccessExpression edgeExpr = new JFieldAccessExpression(edge_name);
        
        JVariableDefinition weightVar = new JVariableDefinition(
                ACC_STATIC,
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
                ACC_STATIC | ACC_FINAL,  // static const in C
                new CArrayType(CStdType.Integer,
                        1, new JExpression[]{new JIntLiteral(size)}),
                "weights",
                new JArrayInitializer(weightVals));
        JLocalVariableExpression weightsExpr = new JLocalVariableExpression(weightsArray);
        
        JStatement next_edge_weight_stmt = new JIfStatement(null,
                new JRelationalExpression(OPE_LT,
                        new JPrefixExpression(null,
                                OPE_PREDEC,
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
            for (int j = 0; j < splitter.getWeights().length; j++) {
                if (splitter.getWeights()[j] != 0) {
                    Edge[] edges = splitter.getDests()[j];
                    JStatement[] pushes = new JStatement[edges.length + 1];
                    for (int k = 0; k < pushes.length; k++) {
                        pushes[k] = new JExpressionStatement(
                            new JMethodCallExpression(
                                Channel.getChannel(edges[k]).pushMethodName(),
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
                null, ACC_STATIC /* | ACC_INLINE */,  // there is no ACC_INLINE
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
}

/**
 * Minimum effort usable implementation of SIRCodeUnit interface.
 * 
 * @author dimock
 *
 */
class MinCodeUnit extends SIRStream implements SIRCodeUnit {
    
    private static final long serialVersionUID = 873884375283692159L;
    
    MinCodeUnit(JFieldDeclaration[] fields, JMethodDeclaration[]methods) {
        this.fields = fields;
        this.methods = methods;
    }

    @Override
    public CType getInputType() {
        throw new AssertionError("unusable method");
    }

    @Override
    public CType getOutputType() {
        throw new AssertionError("unusable method");
    }

    @Override
    public int getPopForSchedule(HashMap[] counts) {
        throw new AssertionError("unusable method");
    }

    @Override
    public int getPushForSchedule(HashMap[] counts) {
        throw new AssertionError("unusable method");
    }

    @Override
    public LIRStreamType getStreamType() {
        throw new AssertionError("unusable method");
    }

    @Override
    public Object accept(AttributeStreamVisitor v) {
        throw new AssertionError("unusable method");
    }
}


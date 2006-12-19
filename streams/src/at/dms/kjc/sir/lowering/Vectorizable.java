/**
 * 
 */
package at.dms.kjc.sir.lowering;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.util.GetSteadyMethods;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.iterator.IterFactory;
import at.dms.kjc.iterator.SIRFilterIter;

import java.util.*;

/**
 * Determine if code is naively vectorizable by interleaving executions of different steady states.
 * <br/> 
 * $Id$
 * <br/>
 * Invoked from {@link VectorizeEnable#vectorizeEnable(SIRStream, Map) vectorizeEnable}
 * and from {@link at.dms.kjc.sir.lowering.fusion.FusePipelines#fusePipelinesOfVectorizableFilters(SIRStream) fusePipelinesOfVectorizableFilters}
 * There should be no need to call methods in this class elsewhere.
 * @author Allyn Dimock
 *
 */
public class Vectorizable {
    /**
     * Set to true to print out reasons to not vectorize a filter, and variables dependent on inputs
     * for filters that fail the data dependency check.
     */
    static boolean debugging = false;
    
    /**
     * reason for disqualifying a filter
     */
     static final String[] reason = {""};
     
     /**
     * Return set of naively vectorizable filters in stream.
     * See {@link #vectorizable(SIRFilter) vectorizable} for what makes a filter naively vectorizable.
     * @param str  Stream to check
     * @return  Set of filters passing {@link #vectorizable(SIRFilter) vectorizable} test
     */
    public static Set<SIRFilter> vectorizableStr(SIRStream str) {
        final Set<SIRFilter> vectorizableFilters = new HashSet<SIRFilter>();
        IterFactory.createFactory().createIter(str).accept(
            new EmptyStreamVisitor() {
                public void visitFilter(SIRFilter self, SIRFilterIter iter) {
                    if (at.dms.kjc.sir.lowering.Vectorizable.vectorizable(self)) {
                        vectorizableFilters.add(self);
                    }
                }
            });
    return vectorizableFilters;
    }
    
    /**
     * Check whether a filter could be naively vectorized by interleaving executions of different steady states.
     * (Does not answer question as to whether it is profitable to do so.)
     * <br/>
     * A filter is naively vectorizable if:
     * <ul><li> Its input type or its output type is a 32-bit int or float.
     * </li><li> It has no loop-carried dependencies between steady states.
     * </li><li> It has no visible side effects.
     * </li><li> It has no data-dependent branches. (Should preclude dynamic-rate filters.)
     * </li><li> It is not a descendant of a feedbackloop: This restriction can be lifted with
     * a bit of work: the problem is that vectorization changes the multiplicity of a filter,
     * if this multiplicity change propagates back to the top of a body (or loop) construct then
     * the multiplicity must also change in the corresponding loop (or body).  Furthermore,
     * the number of enqueued values will be insufficient, which means that we need to clone
     * non-vectorized versions to become part of pre-work functions in the body and loop.
     * </li></ul>
     * TODO: Should allow filters with void input type to be vectorizable if the values
     * being constructed for the output do not participate in conditionals or array offset calculations
     * and the filter has no loop-carried dependencies of side effects.  Such filters should only
     * be vectorizable if the following filter is also vectorizable: else adds overhead.  To do this
     * would require extra work and is probably only of use in cases where a source dumps out the
     * contents of a static array (tde_pp, gmti). 
     * @param f : a filter to check.
     * @return true if there are no local conditions precluding vectorizing the filter.
     */
    public static boolean vectorizable (SIRFilter f) {
        // only vectorizing if have a 32-bit type to process and creating a 32
        // bit type if any.  (Actually more: should not cast to non-32-bit 
        // simple type internally, but can ignore since StreamIt simple types
        // are all 32 bit currently -- and special cases for casting to math functions.
        CType inputType = f.getInputType();
        CType outputType = f.getOutputType();
        if (!(inputType instanceof CIntType || inputType instanceof CFloatType)
        &&  (outputType  instanceof CIntType || outputType instanceof CFloatType || outputType == CStdType.Void)) {

            if (debugging) System.err.println("Vectorizable.vectorizable found " + f.getName() + " has wrong type.");
            return false;
        }
        // must have static rates
        if (f.getPeek().isDynamic() ||
            f.getPop().isDynamic() ||
            f.getPush().isDynamic()) {
            return false;
        }

        // only vectorizing if no loop-carried dependencies (no stores to fields).
        if (at.dms.kjc.sir.lowering.fission.StatelessDuplicate.hasMutableState(f)) {
            if (debugging) System.err.println("Vectorizable.vectorizable found " + f.getName() + " has mutable state.");
            return false; // If possible loop-carried dependence through fields: don't vectorize. 
        }
        // only vectorizing if no side effects (prints, file reads, file writes).
        if (hasSideEffects(f)) {
            if (debugging) System.err.println("Vectorizable.vectorizable found " + f.getName() + " has side effects.");
            return false; // If filter has side effects, don't vectorize.
        }
        // only vectorizing if branches, peek and array offsets are not data dependent
        // ans all reachable operations are supported for vectorization.
        reason[0] = "";
        if (isDataDependent(f)) {
            if (debugging) System.err.println("Vectorizable.vectorizable found " + f.getName() 
                    + " has control or offset dependence or unvectorizable operation."
                    + (reason[0].length() > 0 ? " " + reason[0]: ""));
            return false; // Filter has branches that are data dependent, don't vectorize.
        }
        // only vectorize if not in feedback loop
        for (SIRStream parent = f.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof SIRFeedbackLoop) {
                if (debugging) System.err.println("Vectorizable.vectorizable found " + f.getName() + " is contained in feedbackloop.");
                return false;
            }
        }
        
        if (debugging) System.err.println("Vectorizable.vectorizable found " + f.getName() + " is vectorizable!.");
        return true;
    }
    
    /**
     * Check whether a filter has side effects (I/O).
     * This includes message send and receive.
     * @param f : a filter to check
     * @return  true if no side effects, and no messages.
     */
    public static boolean hasSideEffects (SIRFilter f) {
        if (f instanceof SIRFileReader || f instanceof SIRFileWriter) {
            return true;
        }
        final boolean[] tf = {false};
        List<JMethodDeclaration> methods = GetSteadyMethods.getSteadyMethods(f);
        for (JMethodDeclaration method : methods)
            method.accept(new SLIREmptyVisitor() {
                public void visitPrintStatement(SIRPrintStatement self,
                        JExpression exp) { 
                    tf[0] = true;
                }
                public void visitMessageStatement(SIRMessageStatement self,
                        JExpression portal,
                        String iname,
                        String ident,
                        JExpression[] args,
                        SIRLatency latency) {
                    tf[0] = true;
                }
                // above was send, how do we check for receive?
            });
        return tf[0];
    }
    
    /**
     * Check whether a filter's behavior is data dependent (other than through fields).
     * (A peek or pop flows to a branch condition, array offset, or peek offset.)
     * <pre>   
     * push(arr[pop()]);
     * foo = pop();  bar = peek(foo);
     * foo = peek(5); if (foo > 0) baz++;
     * </pre>
     * You can check as to whether there is any possibility of a loop-carried dependency
     * through fields by checking that (! StatelessDuplicate.hasMutableState(f)).
     * <b/>
     * Currently flow insensitive, context insensitive.
     * <b/>
     * Used without need for object state to check id a filter is data dependent.
     * Is used, with need for object state, to determine number of vectorizable 
     *  artihmetic ops in a vectorizable filter.
     * @param f : a filter to check.
     * @return true if no data-dependent branches or offsets. 
     */
    public static boolean isDataDependent (SIRFilter f) {
        // For any assignment statement containing a pop or peek:
        // add simplified lhs to list of variables to check.
        // For any variable in loop expression (init, test, stride)
        // or test expression for if, while...  if a r-exp includes
        // a variable tainted by peek or pop, then we have
        // data-dependent branch.
        
        // identifiers in slice of a peek or a pop.
        final Set<String> idents = new HashSet<String>();
        // if peek / pop stored as element of array, we only
        // worry about extracting from array, not about array itself.
        final Set<String> arrayidents = new HashSet<String>();
        // indicator that this pass has found a dependency.
        final boolean[] hasDepend = {false};

        List<JMethodDeclaration> methods = GetSteadyMethods.getSteadyMethods(f);
        int oldIdentSize = -1;
        while (oldIdentSize != idents.size() && !hasDepend[0]) {
            oldIdentSize = idents.size();
            for (JMethodDeclaration method : methods) {
                method.accept(new SLIREmptyVisitor() {
                    // found a peek or pop expression or propagated ident
                    private boolean flowsHere = false;
                    // # surrounding scopes indicating dependence.
                    private int delicateLocation = 0;

                    // peek or pop: if is in a "delicate location" such as
                    // calculating an array offset, initializing a field,
                    // calculating a peek offset, part of a branch condition;
                    // then we have a data dependency already...
                    @Override
                    public void visitPeekExpression(SIRPeekExpression self,
                            CType tapeType, JExpression arg) {
                        flowsHere = true;
                        if (delicateLocation > 0) {
                            hasDepend[0] = true;
                        }
                        delicateLocation++;
                        super.visitPeekExpression(self, tapeType, arg);
                        delicateLocation--;
                    }

                    @Override
                    public void visitPopExpression(SIRPopExpression self,
                            CType tapeType) {
                        flowsHere = true;
                        if (delicateLocation > 0) {
                            hasDepend[0] = true;
                        }
                        delicateLocation++;
                        super.visitPopExpression(self, tapeType);
                        delicateLocation--;
                    }

                    
                    @Override
                    public void visitFieldExpression(JFieldAccessExpression self,
                            JExpression left,
                            String ident)
                    {
                        super.visitFieldExpression(self, left, ident);
                        if (idents.contains(ident)) {
                            if (delicateLocation > 0) {
                                hasDepend[0] = true;
                            }
                            flowsHere = true;
                        }
                    }

                    @Override
                    public void visitNameExpression(JNameExpression self,
                            JExpression prefix,
                            String ident) {
                        super.visitNameExpression(self, prefix, ident);
                        if (idents.contains(ident)) {
                            if (delicateLocation > 0) {
                                hasDepend[0] = true;
                            }
                            flowsHere = true;
                        }
                    }
                    
                    @Override
                    public void visitLocalVariableExpression(JLocalVariableExpression self,
                            String ident) {
                        super.visitLocalVariableExpression(self, ident);
                        if (idents.contains(ident)) {
                            if (delicateLocation > 0) {
                                hasDepend[0] = true;
                            }
                            flowsHere = true;
                        }
                    }
                    
                    @Override
                    public void visitVariableDefinition(
                            JVariableDefinition self, int modifiers,
                            CType type, String ident, JExpression expr) {
                        boolean oldflowsHere = flowsHere;
                        flowsHere = false;
                        super.visitVariableDefinition(self, modifiers, type,
                                ident, expr);
                        if (flowsHere) {
                            idents.add(ident);
                        }
                        flowsHere = oldflowsHere;
                    }

                    // assignment may be to variable or field, in which case track
                    // through idents, or may be into array offset in which case
                    // track in arrayidents which will not pass on taint until
                    // dereferenced.
                    private void checkassign(JExpression left, JExpression right) {
                        left.accept(this);
                        boolean oldflowsHere = flowsHere;
                        flowsHere = false;
                        right.accept(this);
                        if (flowsHere) {
                            Set<String> aid = arrayAccessIn(left);
                            if (! aid.isEmpty() ) {
                                arrayidents.addAll(aid);
                            } else {
                                idents.add((CommonUtils.lhsBaseExpr(left))
                                    .getIdent());
                            }
                        }
                        flowsHere = oldflowsHere;
                    }
                    
                    @Override
                    public void visitAssignmentExpression(
                            JAssignmentExpression self, JExpression left,
                            JExpression right) {
                        checkassign(left,right);
                    }

                    @Override
                    public void visitCompoundAssignmentExpression(
                            JCompoundAssignmentExpression self, int oper,
                            JExpression left, JExpression right) {
                        if (! KjcOptions.cell_vector_library && oper == OPE_PERCENT)  {
                            hasDepend[0] = true;
                            reason[0] = "Unsupported operation %";
                        } else {
                            checkassign(left,right);
                        }
                    }

                    @Override
                    public void visitArrayAccessExpression(
                            JArrayAccessExpression self, JExpression prefix,
                            JExpression accessor) {
                        if (arrayidents.contains(CommonUtils.lhsBaseExpr(self).getIdent())) {
                            // dereferencing an array with tainted elements.
                            // not checking for the correct number of levels of dereference...
                            flowsHere = true;
                        }
                        delicateLocation++;
                        super.visitArrayAccessExpression(self, prefix,
                                        accessor);
                        delicateLocation--;
                    }
                    
                    @Override
                    public void visitConditionalExpression(JConditionalExpression self,
                            JExpression cond,
                            JExpression left,
                            JExpression right) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "? :"; }
                        cond.accept(this);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                        left.accept(this);
                        right.accept(this);
                    }
                    
                    @Override
                    public void visitForStatement(JForStatement self,
                            JStatement init, JExpression cond, JStatement incr,
                            JStatement body) {
                        String oldReason = null;
                        delicateLocation++;
                        if (init != null) {
                            if (debugging) { oldReason = reason[0]; reason[0] = "for init"; }
                            init.accept(this);
                        }
                        if (cond != null) {
                            if (debugging) { oldReason = reason[0]; reason[0] = "for cond"; }
                            cond.accept(this);
                        }
                        if (incr != null) {
                            if (debugging) { oldReason = reason[0]; reason[0] = "for incr"; }
                            incr.accept(this);
                        }
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                        body.accept(this);
                    }

                    @Override
                    public void visitIfStatement(JIfStatement self,
                            JExpression cond, JStatement thenClause,
                            JStatement elseClause) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "if"; }
                        cond.accept(this);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                        thenClause.accept(this);
                        if (elseClause != null) {
                            elseClause.accept(this);
                        }
                    }

                    @Override
                    public void visitDoStatement(JDoStatement self,
                            JExpression cond, JStatement body) {
                        String oldReason = null;
                        body.accept(this);
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "do while"; }
                        cond.accept(this);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                    }

                    @Override
                    public void visitWhileStatement(JWhileStatement self,
                            JExpression cond, JStatement body) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "while"; }
                        cond.accept(this);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                        body.accept(this);
                    }
                    
                    @Override
                    public void visitSwitchStatement(JSwitchStatement self,
                            JExpression expr,
                            JSwitchGroup[] body) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "while"; }
                        expr.accept(this);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                        for (int i = 0; i < body.length; i++) {
                            body[i].accept(this);
                        }
                    }
 
                    @Override
                    public void visitMethodCallExpression(JMethodCallExpression self,
                            JExpression prefix,
                            String ident,
                            JExpression[] args) {
                        if (prefix != null) {
                            prefix.accept(this);
                        }
                        // The following commits us to all Math functions.
                        // could also have "if (at.dms.util.Utils.cellMathEquivalent(prefix, ident) != null)"
                        // which would leave out some methods.
                        if (KjcOptions.cell_vector_library && at.dms.util.Utils.isMathMethod(prefix, ident)) {
                            // OK: 
                            visitArgs(args);
                        } else {
                            hasDepend[0] = true;
                            reason[0] = "Unsupported method call " + ident;
                        }
                    }

                    /** Dont allow return of vector type. */
                    @Override
                    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "return"; }
                        super.visitReturnStatement(self, expr);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                    }
                    
                    // From here: look at operations that do not vectorize.
                    // check: ~ in C
                    @Override
                    public void visitBitwiseComplementExpression(JUnaryExpression self,
                            JExpression expr) {
                        if (KjcOptions.cell_vector_library) {
                            super.visitBitwiseComplementExpression(self,expr);
                        } else { 
                            String oldReason = null;
                            delicateLocation++;
                            if (debugging) { oldReason = reason[0]; reason[0] = "~"; }
                            super.visitBitwiseComplementExpression(self,expr);
                            if (debugging) { reason[0] = oldReason; }
                            delicateLocation--;
                        }
                    }
                    
                    
                    @Override
                    // gcc does not automatically support shifts, so disallow until 
                    // we develop intrinsics.  (Even so the SSE has some limitations:
                    // the value that you are shifting by must be either immediate or
                    // at least 64 bits long (possible by splatting into a vector register)
                    public void visitShiftExpression(JShiftExpression self,
                            int oper,
                            JExpression left,
                            JExpression right) {
                        if (KjcOptions.cell_vector_library) {
                            super.visitShiftExpression(self,oper,left,right);
                        } else {
                            String oldReason = null;
                            delicateLocation++;
                            if (debugging) { oldReason = reason[0]; reason[0] = "unsupported operation <<, >>, or >>>"; }
                            super.visitShiftExpression(self,oper,left,right);
                            if (debugging) { reason[0] = oldReason; }
                            delicateLocation--;
                        }
                    }
                    
                    // gcc does not support relational expressions on vector except
                    // through intrinsics or builtins, and our vectorization project
                    // does not allow a vector to flow to a branch condition so 
                    // we have little use for vectors in relational expressions.
                    @Override
                    public void visitRelationalExpression(JRelationalExpression self,
                            int oper,
                            JExpression left,
                            JExpression right) {
                        delicateLocation++;
                        super.visitRelationalExpression(self,oper,left,right);
                        delicateLocation--;
                    }

                    // Vectorize does not currently handle casts
                    // TODO: Vectorize should handle casts between same-width
                    // supported types (meaning 32-bit int and 32-bit float).
                    // The problem is that Vectorize has not been fully adapted to
                    // handle multiple vector types in one method.
                    @Override
                    public void visitCastExpression(JCastExpression self,
                            JExpression expr,
                            CType type) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "(cast)"; }
                        super.visitCastExpression(self, expr,type);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                    }
                    @Override
                    public void visitUnaryPromoteExpression(JUnaryPromote self,
                            JExpression expr,
                            CType type) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = "(cast)"; }
                        super.visitUnaryPromoteExpression(self, expr,type);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                    }
                                  
                    /** &&, !!, ==, != */
                    @Override
                    public void visitBinaryExpression(JBinaryExpression self,
                            String oper,
                            JExpression left,
                            JExpression right) {
                        String oldReason = null;
                        delicateLocation++;
                        if (debugging) { oldReason = reason[0]; reason[0] = oper; }
                        super.visitBinaryExpression(self,oper,left,right);
                        if (debugging) { reason[0] = oldReason; }
                        delicateLocation--;
                    }

                    /* mostly for checking lhs's. Return names of all arrays accessed in expression. */
                    private Set<String> arrayAccessIn(JExpression e) {
                        final Set<String> s = new HashSet<String>();
                        e.accept(new SLIREmptyVisitor() {
                            public void visitArrayAccessExpression(
                                    JArrayAccessExpression self, JExpression prefix,
                                    JExpression accessor) {
                                s.add(CommonUtils.lhsBaseExpr(prefix).getIdent());
                                super.visitArrayAccessExpression(self, prefix, accessor);
                            }
                        });
                        return s;
                    }
                });
            }
        }
        if (debugging && hasDepend[0]) {
            System.err.println("Vectorizable.isDataDependent found idents for "
                    + f.getName() + ":");
            for (String ident : idents) {
                System.err.print(ident + " ");
            }
            System.err.println();
        }
        // dependence found during setup.
        if (hasDepend[0]) {
            return true;
        }
        return false;
    }
    
    /**
     * determine whether a stream may contain useful vector operations.
     * <b/>
     * Approximates as to whether a vectorizable stream contains any vector 
     * arithmetic operations.
     */
    // internals: anything other than SIRIdentity or splitJoin reorderer
    // is considered useful.
    public static boolean isUseful(SIRStream str) {
        if (str instanceof SIRPipeline) {
            // a pipeline is useful if it has a useful sub-stream
            SIRPipeline str1 = (SIRPipeline)str;
            boolean useful1 = false;
            for (SIRStream substr : str1.getSequentialStreams()) {
                useful1 |= isUseful(substr);
                if (useful1) break;
            }
            return useful1;
        }
        
        if (str instanceof SIRSplitJoin) {
            // a splitjoin is useful if it has a useful branch
            SIRSplitJoin str1 = (SIRSplitJoin)str;
            boolean useful1 = false;
            for (SIRStream substr : str1.getParallelStreams()) {
                useful1 |= isUseful(substr);
                if (useful1) break;
            }
            return useful1;
        }

        if (str instanceof SIRFilter) {
            // a filter is useful if it contains arithmetic
            SIRFilter str1 = (SIRFilter)str;
            if (str1 instanceof SIRIdentity) {
                // an identity filter can contain no useful arithmetic.
                return false;
            }
            // TODO: need to tie this back to at.dms.kjc.lowering.CollapseDataParallel pass
            // dependent on text that it generates.
            if (str1.getName().startsWith("Pre_CollapsedDataParallel")
             || str1.getName().startsWith("Post_CollapsedDataParallel")) {
                // A splitjoin reorderer: probably not useful.
                return false;
            }
            return true;
        }
        
        // a feedbackloop is not useful since it is not vectorizable.
        return false;
    }
}

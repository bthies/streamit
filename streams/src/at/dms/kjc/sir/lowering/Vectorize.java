/**
 * $Id$
 */
package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/**
 * Transform methods for a filter to use vector types as appropriate.
 * <ul><li>Identify vectors: values from peek/pop, values to push.
 * </li><li>Introduce new tmp for peek/pop
 * </li><li>Replace peek of data with N peeks into tmp.
 * </li><li>Replace pop of data with pop and N-1 peeks into tmp.
 * </li><li>Replace push of data with push and N-1 pokes.
 * </li><li>Introduce correction for pops, pokes at end of method.
 * (need to buffer pushes, pokes if pushing involves communication, 
 * just need to update a pointer if output to a buffer.) 
 * </li><li>For fields needing vector values: calculate scalar then
 * widen into a new variable.
 * </li></ul>
 * Known preconditions: no vector type will be be required
 * for I/O, for branch condition, for array or peek offset
 * calculation, as per {@link Vectorizable#vectorizable(SIRFilter)}
 * <br/>Limitation: No duplication of array initializer values: need
 * to copy array to wider array.  Initializing arrays in fields not handled at all.
 * 
 * @author Allyn Dimock
 *
 */
public class Vectorize {
    /**
     * Take a filter and convert it to using vector types.
     * {@link SimplifyPeekPopPush} must previously have been run.
     * Do not call directly: {@link VectorizeEnable#vectorizeEnable(SIRStream, Map) vectorizeEnable}
     * controls the vectorization pass.
     * @param filter to convert.
     */
    static void vectorize (SIRFilter filter) {
        final boolean[] vectorizePush = {false};  // true if vector type reaches push statements.
        Map<String,CType> vectorIds;
        
        // move all field initializations out of declarations, just in case
        // vectorization marks a field as being vectorizable.
        FieldInitMover.moveFilterInitialAssignments(filter, FieldInitMover.MOVE_ARRAY_INITIALIZERS);
        
        // determine info for filter as a whole.
        vectorIds = toVectorize(filter, vectorizePush);
        
        // Debugging.
        System.err.println("Vectorize " + filter.getName() + " : ");
        for (Map.Entry<String, CType> e : vectorIds.entrySet()) {
            System.err.println(e.getKey() + " " + at.dms.kjc.common.CommonUtils.CTypeToString(e.getValue(),true));
        }
        
        JMethodDeclaration[] ms = filter.getMethods();
        for (JMethodDeclaration m : ms) {
            JBlock b = m.getBody();
            JBlock newB = mungMethodBody(b, m == filter.getWork(), vectorizePush[0], vectorIds, filter);
            m.setBody(newB);
        }
   }
    
    /**
     * 
     * @param methodBody to process
     * @param isWork indicates work() which needs extra fixups at end.
     * @param vectorIds allows determination of which variables need vector types.
     * @return
     */
    static JBlock mungMethodBody(JBlock methodBody, boolean isWork, 
            boolean vectorizePush, Map<String,CType> vectorIds,
            SIRFilter filter) {
        JStatement methodBody2 = moveLocalInitializations(methodBody,vectorIds.keySet());
        JStatement methodbody3 = fixTypesPeeksPopsPushes(methodBody2, vectorizePush, vectorIds);
        JStatement methodbody4;
        if (isWork) {
            methodbody4 = cleanupWork(methodbody3, filter);
        } else {
            methodbody4 = methodbody3;
        }
        if (methodbody4 instanceof JBlock) {
            return (JBlock)methodbody4;
        } else {
            JBlock retval = new JBlock(new JStatement[]{methodbody4});
            return retval;
        }
    }
    
    /**
     * Clean up loop around work body and fix pushes.
     * @param methodBody
     * @param filter
     * @return
     */
    static JStatement cleanupWork(JStatement methodBody, SIRFilter filter) {
        return methodBody; // TODO finish method.
    }
    
    /**
     * Change types on declarations and expand peeks, pops, and possibly pushes.
     * @param methodBody
     * @param vectorizePush
     * @param vectorIds
     * @return
     */
    static JStatement fixTypesPeeksPopsPushes(JStatement methodBody, 
            boolean vectorizePush, Map<String,CType> vectorIds) {
        return methodBody; // TODO finish method.
        
    }
    
    /**
     * Do we need this?
     * @param method
     * @return number of occurrences of peek or pop
     */
    static int numPeeksPops(JMethodDeclaration method) {
        final int[] number = {0};
        method.accept(new SLIREmptyVisitor(){
            public void visitPeekExpression(SIRPeekExpression self,
                    CType tapeType, JExpression arg) {
                number[0]++;
            }
            public void visitPopExpression(SIRPopExpression self,
                    CType tapeType) {
                number[0]++;
            }
        });  
        return number[0];
    }
    
    /**
     * Divide certain JVariableDeclarationStatement's into declaration and separate assignment.
     * <br/>For variables in "varnames" change any initialization of these variables in "block"
     * or any sub-block into separate declaration and assignment statements.
     * <ul><li>
     * If you use this on a JVariableDeclarationStatement in the init portion of a "for" statement.
     * may end up with an un-compilable "for" statement.
     * </li><li>
     * The result of this pass may mix declarations with assignments.  
     * If you want declarations first, run {@link VarDeclRaiser} after this.
     * </li></ul>
     * @param block the statement, usually a block, to con
     * @param idents the variable names (from getIdent() of a definition or use).
     */
    static JStatement moveLocalInitializations (JStatement block, final Set<String> idents) {
        class moveDeclsVisitor extends StatementQueueVisitor {
            private boolean inVariableDeclarationStatement = false;

            @Override
            public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                    JVariableDefinition[] vars) {
                inVariableDeclarationStatement = true;
                Object retval = super.visitVariableDeclarationStatement(self,vars);
                inVariableDeclarationStatement = false;
                return retval;
            }
            
            @Override
            public Object visitVariableDefinition(JVariableDefinition self,
                    int modifiers,
                    CType type,
                    String ident,
                    JExpression expr) {
                if (inVariableDeclarationStatement && expr != null && idents.contains(ident)) {
 //                   if (! (expr instanceof JArrayInitializer)) {
                        JStatement newInit = new JExpressionStatement(expr);
                        addPendingStatement(newInit);
                        self.setExpression(null);
//                    } else {
//                    }
                }
                return self;
            }
            
        }

        return (JStatement)block.accept(new moveDeclsVisitor());

    }
    
    /**
     * Identify locals and fields storing vector types.
     * <br/>
     * A local variable or field of a filter may
     * <ul><li> need to be of a vector type
     * </li><li> may be an array where a vector type needs to be applied at dereference level n,
     * </li><li> may be a struct where a vector type may need to be applied at a field of the structure -- in which
     * case: tough luck.
     * </ul>
     * Determine that a local or field type needs munging:  
     * <ul><li>if a peek or pop flows to the field or if the field is an array with push or pop flowing to its elements.
     * </li><li>if the peek or pop flows to a push
     * </ul>
     * Note: this is not a full type inference, which would require a full bottom-up pass unifying the type from
     * peek or pop with the type of an expression (LackWit for StreamIt).  We make no effort to unify types
     * across complex expressions (the user can fix by simplifying the streamit code).  We make no effort to deal
     * with vectorizable fields of structs (the user can not turn vectorization on).
     * @param filter whose methods are being checked for need for vectorization.
     * @param vectorizePush a second return value: do vector values reach push(E) expressions?
     * @return an indication as to what variables need to be given vector types.
     */
    static Map<String,CType> toVectorize(final SIRFilter filter, 
            final boolean[] vectorizePush) {
        // map used internally by visitor to track types.
        // for now returned to caller, although would be nice to
        // return just the JVariableDeclaration's that need modification.
        final Map<String,CType> typemap = new HashMap<String,CType>();
        // used here and by visitor to 
        final boolean changes[] = {true};

        JMethodDeclaration[] ms = filter.getMethods();

        while (changes[0]) {
          changes[0] = false;
          for (JMethodDeclaration m : ms) {
            JBlock b = m.getBody();
            b.accept(new SLIREmptyVisitor(){
                private boolean isLhs = false;
                private CType haveType = null;
                public void visitPeekExpression(SIRPeekExpression self,
                        CType tapeType, JExpression arg) {
                    assert ! isLhs;
                    haveType = filter.getInputType();
                }
                public void visitPopExpression(SIRPopExpression self,
                        CType tapeType) {
                    assert ! isLhs;
                    haveType = filter.getInputType();
                }
                public void visitVariableDefinition(
                        JVariableDefinition self, int modifiers,
                        CType type, String ident, JExpression expr) {
                    haveType = null;
                    isLhs = false;
                    if (expr != null) {expr.accept(this);}
                    if (haveType != null) {
                        registerv(ident,haveType,type);
                    }
                    haveType = null;
                    isLhs = false;
                }
                public void visitPushExpression(SIRPushExpression self,
                        CType tapeType,
                        JExpression arg) {
                    super.visitPushExpression(self,tapeType,arg);
                    if (haveType != null) {
                        vectorizePush[0] = true;  // push takes a vector type.
                    }
                }
                public void visitArrayAccessExpression(
                        JArrayAccessExpression self, JExpression prefix,
                        JExpression accessor) {
                    if (isLhs && (haveType != null)) {
                        registerv(getIdent(self),new CArrayType(haveType,accessorLevels(self)),null);
                    }
                    if (! isLhs && (typemap.containsKey(getIdent(self)))) {
                        // if multidim array, decrease dimension, else return base type
                        haveType = ((CArrayType)typemap.get(getIdent(self))).getElementType();
                    }
                }
                
                public void visitFieldExpression(JFieldAccessExpression self,
                        JExpression left,
                        String ident) {
                    if (isLhs && (haveType != null)) {
                        registerv(getIdent(self),haveType,null);
                    }
                    if (! isLhs && typemap.containsKey(getIdent(self))) {
                        haveType = filter.getInputType(); // lots of fields set up with no type so fake it.
                    }
                    
                }
                public void visitNameExpression(JNameExpression self,
                        JExpression prefix,
                        String ident) {
                    if (isLhs && (haveType != null)) {
                        registerv(getIdent(self),haveType,null);
                    }
                    if (! isLhs && typemap.containsKey(getIdent(self))) {
                        haveType = filter.getInputType();
                    }
                    System.err.println("Found a JNameExpression " + self.toString());
                }
                public void visitLocalVariableExpression(JLocalVariableExpression self,
                        String ident) {
                    if (isLhs && (haveType != null)) {
                        registerv(ident,haveType,null);
                    }
                    if (! isLhs && typemap.containsKey(ident)) {
                        haveType = filter.getInputType();
                    }
                }
                public void visitAssignmentExpression(
                        JAssignmentExpression self, JExpression left,
                        JExpression right) {
                    isLhs = false;
                    haveType = null;
                    right.accept(this);
                    if (haveType != null) {
                        isLhs = true;
                        left.accept(this);
                        isLhs = false;
                    }
                    if (haveType != null && right instanceof JBinaryArithmeticExpression) {
                        JExpression bleft = ((JBinaryArithmeticExpression)right).getLeft();
                        JExpression bright = ((JBinaryArithmeticExpression)right).getRight();
                        isLhs = true;
                        if (CanPropagateRightTo(bleft)) {
                            bleft.accept(this);
                        }
                        if (CanPropagateRightTo(bright)) {
                            bright.accept(this);
                        }
                        isLhs = false; 
                    }
                }
                
                /* 
                 * TODO: binary arith ops: types match.
                 * TODO: combine w vectorizable.
                 */
                public void visitCompoundAssignmentExpression(
                        JCompoundAssignmentExpression self, int oper,
                        JExpression left, JExpression right) {
                    isLhs = false;
                    haveType = null;
                    right.accept(this);
                    left.accept(this);  // left is also r-val in  a op= b;
                    if (haveType != null) {
                        isLhs = true;
                        left.accept(this);
                        if (CanPropagateRightTo(right)) {
                            // propagate from left to right when certain.
                            // Can not propagate into casts, 
                            // array / field constructors / destructors. 
                            right.accept(this);
                        }
                        isLhs = false;
                    }
                }
                
                // situation:  A = B op C,   B is vectorizable, so C had
                // better be vectorizable.  In code above, after finding that
                // A is vectorizable, make sure that both B and C are vectorizable
                // by propagating information back to the right.
                //
                // could really propagate right to any expression, but would have
                // to take into account type changes from array / field construction
                // / deconstruction, relational epxressions, casts, etc.
                boolean CanPropagateRightTo(JExpression exp) {
                    return exp instanceof JFieldAccessExpression ||
                    exp instanceof JLocalVariableExpression ||
                    exp instanceof JArrayAccessExpression;
                }
                
                // eventually use type to handle array slices, fields of structs, ...
                // or build up a PathToVec.
                private void registerv(String v,
                        CType type, CType declType) {
                    if (! typemap.containsKey(v)) {
                        changes[0] = true;
                        typemap.put(v,type);
                    }
                    return;
//                    if (declType != null) {
//                        assert declType.equals(type);
//                    }
//                    if (type.isArrayType()) {
//                        CType lastSliceType = type;
//                        for (int i = ((CArrayType)type).getDims().length - 1;
//                            i > 0; i--) {
//                                // need variable (accessor|#dims)*
//                                // not just variable.
//                        }
//                    }
                }
            });
          }
        }
        return null;
    }
    
    /**
     * Return a unique identifier for a variable reference expression.
     * @param exp An expression
     * @return A string that uniquely identifies the expression.
     */
    private static String getIdent(JExpression exp) {
        if (exp instanceof JLocalVariableExpression) {
            return ((JLocalVariableExpression)exp).getIdent();
        }
        if (exp instanceof JFieldAccessExpression) {
            JFieldAccessExpression e = (JFieldAccessExpression)exp;
            String prefix = getIdent(e.getPrefix());
            return ((prefix.length()  > 0)? (prefix + ".") : "") + exp.getIdent();
        }
        if (exp instanceof JThisExpression) {
            return "";
        }
        if (exp instanceof JArrayAccessExpression) {
            JArrayAccessExpression e = (JArrayAccessExpression)exp;
            String prefix = getIdent(e.getPrefix());
            return prefix;
        }
        assert false : "Unexpected expression type " + exp.toString();
        return null;
    }
    
    /**
     * attempt to get number of dims of array, or array slice by looking at number of levels of access.
     * @param self
     * @return
     */

    private static int accessorLevels(JExpression self) {
        if (self instanceof JArrayAccessExpression) { 
            return 1 + accessorLevels(((JArrayAccessExpression)self).getAccessor());
        } else {
            // not array access, assume have reached base of array or slice.
            return 0;
        }
    }


//    /**
//     * A path to a place to put a vector type.
//     * <pre>
//     * (foo.myarr[0][0]).bar = pop();
//     * need to remember myarr => foo.2.bar
//     * 
//     * Can we do something simpler: entire prefix to string,
//     * 
//     * somewhere are
//     * struct B {
//     * ...
//     * float bar;
//     * ...
//     * } 
//     * struct {
//     *   ...
//     *   B myarr[n][m]; 
//     * } foo
//     * </pre>
//     * @author Allyn Dimock
//     *
//     */
//    private class PathToVec {
//        final PathToVec more;
//        public PathToVec(PathToVec more) {this.more = more; }
//    }
//    private class Name extends PathToVec {
//        final String name;
//        public Name(String name, PathToVec more) {
//            super (more);
//            this.name = name;
//        }
//    }
//    private class ArrayDepth extends PathToVec {
//        final int numDims;
//        public ArrayDepth(int numDims, PathToVec more) {
//            super(more);
//            this.numDims = numDims;
//        }
//    }
//    private class Prefix extends PathToVec {
//        final String prefix;
//        public Prefix(String prefix, PathToVec more) {
//            super(more);
//            this.prefix = prefix;
//        }
//    }
}

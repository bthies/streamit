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
        
        // fix type of fields if need be.
        JFieldDeclaration[] fields = filter.getFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fixFieldTypes(fields[i], vectorIds.keySet());
        }
        filter.setFields(fields);

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
        // field initializations already moved, move initialization of locals.
        JStatement methodBody2 = moveLocalInitializations(methodBody,vectorIds.keySet());
        // expand peeks, pops, and optionally pushes, and update types of fields and locals.
        JStatement methodbody3 = fixTypesPeeksPopsPushes(methodBody2, 
                filter.getPopInt(),  // original number popped in steady state.
                vectorizePush, 
                filter.getPushInt(),  // original number pushed in steady state.
                vectorIds);
         // return a block.
        if (methodbody3 instanceof JBlock) {
            return (JBlock)methodbody3;
        } else {
            JBlock retval = new JBlock(new JStatement[]{methodbody3});
            return retval;
        }
    }
    
    /**
     * Convert type of a field declaration
     * @param decl field declaration
     * @param idents  identifiers of fields and locals needing vector type.
     * @return possibly modified field declaration (also happens to be munged in place).
     */
    static JFieldDeclaration fixFieldTypes(JFieldDeclaration decl, Set<String> idents) {
        JVariableDefinition defn = decl.getVariable();  // access to internal variable defn.
        if (idents.contains(defn.getIdent())) {
            defn.setType(CVectorType.makeVectortype(defn.getType()));
        }
        return decl;
    }
    
    /**
     * Change types on declarations and expand peeks, pops, and possibly pushes.
     * @param methodBody
     * @param vectorizePush
     * @param vectorIds
     * @return
     */
    static JStatement fixTypesPeeksPopsPushes(JStatement methodBody,
            final int inputStride,
            final boolean vectorizePush, final int outputStride,
            final Map<String,CType> vectorIds) {
            class fixTypesPeeksPopsPushesVisitor extends StatementQueueVisitor {
                JExpression lastLhs = null;   // left hand side of assignment being processed
                JExpression lastRhs = null;   // right hand side od assignment being processed
                boolean suppressSelector = false; // true to not refer to as vector
                boolean replaceStatement = false; // true to discard current statement
                @Override
                public Object visitVariableDefinition(JVariableDefinition self,
                        int modifiers,
                        CType type,
                        String ident,
                        JExpression expr) {
                    if (vectorIds.containsKey(ident)) {
                        CType newType = CVectorType.makeVectortype(type == null? vectorIds.get(ident): type);
                        return (new JVariableDefinition(modifiers, newType, ident, expr));
                    } else {
                        return self;
                    }
                }
                @Override
                public Object visitPopExpression(SIRPopExpression self,
                        CType tapeType) {
                    if (lastLhs == null) {
                        // just a pop not returning a value.
                        return self;
                    }
                    // e = pop();    =>
                    // (e).a[0] = pop();
                    // (e).a[1] = peek(1 * inputStride);
                    // (e).a[2] = peek(2 * inputStride);
                    // (e).a[3] = peek(3 * inputStride);
                    // lastLhs non-null here.
                    
                    this.addPendingStatement(
                            new JExpressionStatement(
                                    new JAssignmentExpression(
                                            CVectorType.asArrayRef(lastLhs,0),
                                            new SIRPopExpression(tapeType))));
                    for (int i = 1; i < KjcOptions.vectorize / 4; i++) {
                        this.addPendingStatement(
                                new JExpressionStatement(
                                        new JAssignmentExpression(
                                                CVectorType.asArrayRef(lastLhs,i),
                                                new SIRPeekExpression(
                                                        new JIntLiteral(i * inputStride),
                                                        tapeType))));
                    }
                    replaceStatement = true; // throw out statement built with this return value
                    return self;
                }
                @Override
                public Object visitPeekExpression(SIRPeekExpression self,
                        CType tapeType, JExpression arg) {
                    assert (self == lastRhs);
                    // e1 = peek(e2);  =>
                    // (e1).a[0] = peek((e2) + 0 * inputStride);
                    // ...
                    // (e1).a[3] = peek((e2) + 3 * inputStride);
                    
                    for (int i = 0; i < KjcOptions.vectorize / 4; i++) {
                        this.addPendingStatement(
                                new JExpressionStatement(
                                        new JAssignmentExpression(
                                                CVectorType.asArrayRef(lastLhs,i),
                                                new SIRPeekExpression(
                                                        new JAddExpression(arg,
                                                                new JIntLiteral(i * inputStride)),
                                                        tapeType))));
                    }
                    replaceStatement = true; // throw out statement built with this return value
                    return self; 
                }
                @Override
                public Object visitPushExpression(SIRPushExpression self,
                        CType tapeType,
                        JExpression arg) {
                    if (! vectorizePush) {
                        // vector type does not reach push expression.
                        return self;
                    }
                    // push(e)  =>
                    // push((e).a[0]);
                    // poke((e).a[1], 1 * outputStride);
                    // ...
                    // poke((e).a[3], 3 * outputStride);
                    for (int i = 1; i < KjcOptions.vectorize / 4; i++) {
                        this.addPendingStatement(
                                new JExpressionStatement(
                                        new SIRPokeExpression(
                                                CVectorType.asArrayRef(arg,i),
                                                i * outputStride,
                                                tapeType)));
                    }
                    return new SIRPushExpression(
                            CVectorType.asArrayRef(arg,0),
                            tapeType);
                }

                @Override
                public Object visitAssignmentExpression(JAssignmentExpression self,
                        JExpression left,
                        JExpression right) {
                    // setting a vector value from non-vector.
                    if (hasVectorType(left,vectorIds) && ! hasVectorType(right,vectorIds)) {
                        CType oldType = left.getType();
                        suppressSelector = true;
                        JExpression newLeft = (JExpression)left.accept(this);
                        // JExpression newLeftArray = CVectorType.asArrayRef(newLeft, i);
                        suppressSelector = false;
                        JVariableDefinition defn = 
                            new JVariableDefinition(oldType, 
                                    ThreeAddressCode.nextTemp());
                        JStatement decl = new JVariableDeclarationStatement(defn);
                        JLocalVariableExpression tmp = new JLocalVariableExpression(
                                defn);
                        
                        addPendingStatement(decl);
                        addPendingStatement(
                                new JExpressionStatement(
                                        new JAssignmentExpression(tmp, right)));
                        for (int i = 0; i < KjcOptions.vectorize / 4; i++) {
                            addPendingStatement(
                                    new JExpressionStatement(
                                            new JAssignmentExpression(
                                                    CVectorType.asArrayRef(newLeft, i),
                                                    tmp)));
                        }
                        replaceStatement = true;
                        return self;
                    }
                    lastLhs = left;
                    lastRhs = right;
                    Object retval = super.visitAssignmentExpression(self,left,right);
                    lastLhs = null;
                    lastRhs = null;
                    return retval;
                }

                @Override 
                public Object visitExpressionStatement(JExpressionStatement self, JExpression expr) {
                    // Some expressions need to return an expression, but have the statement
                    // they are in removed.  Such expressions are all expected to be inside
                    // a JExpressionStatement
                    replaceStatement = false;
                    Object retval = super.visitExpressionStatement(self, expr);
                    if (replaceStatement) {
                        assert retval instanceof JBlock;
                        JBlock b = (JBlock)retval;
                        b.removeStatement(0);
                        return b;
                    } else {
                        return retval;
                    }
                }

                @Override
                public Object visitFieldExpression(JFieldAccessExpression self,
                        JExpression left,
                        String ident) {
                    if (vectorIds.containsKey(getIdent(self)) && ! suppressSelector) {
                        return CVectorType.asVectorRef(self);
                    } else {
                        return self;
                    }
                }
                @Override
                public Object visitLocalVariableExpression(JLocalVariableExpression self, String ident) {
                    if (vectorIds.containsKey(getIdent(self)) && ! suppressSelector) {
                        return CVectorType.asVectorRef(self);
                    } else {
                        return self;
                    }
                }
                @Override
                public Object visitArrayAccessExpression(JArrayAccessExpression self,
                        JExpression prefix,
                        JExpression accessor) {
                    if (vectorIds.containsKey(getIdent(self)) && ! suppressSelector) {
                        // XXX does not currently work for array slices
                        return CVectorType.asVectorRef(self);
                    } else {
                        return self;
                    }
                }
            }
           
            return (JStatement)methodBody.accept(new fixTypesPeeksPopsPushesVisitor());
        
    }
    
            /** 
             * Check whether an expression refers to a variable or array that should have a vector type.
             * @param expr
             * @param vectorIds
             * @return
             */
    static boolean hasVectorType(JExpression expr, final Map<String,CType> vectorIds) {
        final boolean[] hasVectorType = {false};
 
        class hasVectorTypeVisitor extends SLIREmptyVisitor {
            @Override
            public void visitLocalVariableExpression(JLocalVariableExpression self, String ident) {
                if (vectorIds.containsKey(getIdent(self))) {
                    hasVectorType[0] = true;
                }
            }
            @Override
            public void visitArrayAccessExpression(JArrayAccessExpression self,
                    JExpression prefix,
                    JExpression accessor) {
                if (vectorIds.containsKey(getIdent(self))) {
                    hasVectorType[0] = true;
                }
            }
            @Override
            public void visitFieldExpression(JFieldAccessExpression self,
                    JExpression left,
                    String ident) {
                if (vectorIds.containsKey(getIdent(self))) {
                    hasVectorType[0] = true;
                }
            }
        }
        
        expr.accept(new hasVectorTypeVisitor());
        return hasVectorType[0];
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
        return typemap;
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
class SIRPokeExpression extends SIRPushExpression {
    int offset;
    public SIRPokeExpression(JExpression arg, int offset, CType tapeType)
    {
        super(arg, tapeType);
        this.offset = offset;
    }
    public int getOffset () { return offset; }

}

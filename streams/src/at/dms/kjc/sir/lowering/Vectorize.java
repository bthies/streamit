/**
 * $Id$
 */
package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;

/**
 * Transform methods for a filter to use vector types as appropriate.
 * <ul><li>Identify vectors: values from peek/pop, values to push.
 * </li><li>Introduce new tmp for peek/pop
 * </li><li>Replace peek of data with N peeks into tmp.
 * </li><li>Replace pop of data with pop and N-1 peeks into tmp.
 * </li><li>Replace push of data with and N pokes.
 * </li><li>Introduce correction for pokes at end of method.
 * (need to buffer pokes if pushing involves communication, 
 * just need to update a pointer if output to a buffer.) 
 * </li><li>For fields needing vector values: calculate scalar then
 * widen into a new variable.
 * </li></ul>
 * Known preconditions: no vector type will be be required
 * for I/O, for branch condition, for array or peek offset
 * calculation, as per {@link Vectorizable#vectorizable(SIRFilter)}
 * Arguments to peeks, pushes are purely functional: no side effects.
 * In fact they are either constant, local variable access, field access or array access.
 * <br/>Limitation: Initializing arrays in fields not handled at all.
 * 
 * @author Allyn Dimock
 *
 */

/*
 * Changes: version 1.8
 * (o) Previous: load each field into vector.  
 *     Now: if stride != 1, copy into aligned buffer that is union of vector and scalar.
 * (o) Previous: put out one element on push and N-1 elements in poke buffer, copy out poke buffer at end.
 *     Now: if stride != 1, store vectors into aligned buffer, copy out all elements at end (more storage, 
 *     more copying, but 9% faster in dct kernel on Altivec.
 * (o) Previous: use union types on temporaries in work function.
 *     Now: use just vector temporaries in work function.
 * (o) Found out about IBM implementations of common math functions on Cell: start using them.
 * (o) In related news: in back ends that can support it, should not attempt to initialize pop and poke
 *     buffers, nor any vector-valued temp.
 */
public class Vectorize {
    /**
     * print out names of variables that are given vector type
     */
    static public boolean debugging = false;
    
    /**
     * Take a filter and convert it to using vector types.
     * {@link SimplifyPeekPopPush} must previously have been run.
     * Do not call directly: {@link VectorizeEnable#vectorizeEnable(SIRStream, Map) vectorizeEnable}
     * controls the vectorization pass.
     * @param filter to convert.
     * @param vectorInputTape set to true if input tape has / will have a vector type.
     * @param vectorOutputTape is set to true if output tape should have a vector type.
     */
    static boolean vectorize (SIRFilter filter, boolean vectorInputTape, boolean vectorOutputTape) {
        final boolean[] vectorizePush = {false};  // true if vector type reaches push statements.
        Map<String,CType> vectorIds;
        
//        // move all field initializations out of declarations, just in case
//        // vectorization marks a field as being vectorizable.
//        FieldInitMover.moveFilterInitialAssignments(filter, FieldInitMover.MOVE_ARRAY_INITIALIZERS);
        
        // determine all fields, locals in filter to vectorize, and mark constants to vectorize.
        vectorIds = toVectorize(filter, vectorizePush);
        
        if (debugging) {
            System.err.println("Vectorize " + filter.getName() + " : ");
            for (Map.Entry<String, CType> e : vectorIds.entrySet()) {
                System.err.println(e.getKey()
                        + " "
                        + at.dms.kjc.common.CommonUtils.CTypeToString(e
                                .getValue(), true));
            }
        }
        
        // fix type of fields if need be.
        JFieldDeclaration[] fields = filter.getFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fixFieldTypes(fields[i], vectorIds.keySet());
        }
        filter.setFields(fields);

        // rewrite types, pops, peeks, and possible pushes as necessary in each method.

        // define poke buffer here since JVariableDefinition must be shared.
        // may or may not actually need buffer.
        JVariableDefinition pokeBufDefn = new JVariableDefinition(
                null,
                at.dms.kjc.Constants.ACC_FINAL,
                new CArrayType(filter.getOutputType(),
                        1, 
                        new JExpression[]{new JIntLiteral(
                                filter.getPushInt() * ((KjcOptions.vectorize / 4) - 1))}),
                "__POKEBUFFER__",
                null);
        JVariableDefinition pokeBufOffsetDef = new JVariableDefinition(
                CStdType.Integer, "__POKEBUFFERHEAD__");
        JFieldAccessExpression pokeBufRef = new JFieldAccessExpression(
                null,
                new JThisExpression(),
                pokeBufDefn.getIdent(),
                new CSourceField(null,0,pokeBufDefn,false));
        JFieldAccessExpression pokeBufOffset = new JFieldAccessExpression(
                null,
                new JThisExpression(), 
                pokeBufOffsetDef.getIdent(),
                new CSourceField(null,0,pokeBufOffsetDef,false));

        JMethodDeclaration[] ms = filter.getMethods();
        for (JMethodDeclaration m : ms) {
            JBlock b = m.getBody();
            JBlock newB = mungMethodBody(b, vectorizePush[0], vectorIds, filter, pokeBufRef, pokeBufOffset);
            m.setBody(newB);
        }
        
        // pull vector constants to const (ACC_FINAL) fields or local variables.
        raiseVectorConstants(filter);
        
        // put in poke buffer handling
        if (filter.getPushInt() > 1 && vectorizePush[0]) {
            putInPokeBuffer(filter,pokeBufDefn,pokeBufRef,pokeBufOffsetDef,pokeBufOffset);
        }
        
        return vectorizePush[0];
  }
    
    /**
     * Put poke buffer definition in filter, and push from poke buffer at end of work.
     * @param filter  : filter needing poke buffer
     * @param pokeBufDefn : Variable definition for poke buffer
     * @param pokeBufAccess : Field access expression for poke buffer
     * @param pokeBufOffsetDef : variable used to access poke buffer
     * @param pokeBufOffset : Field access expression for poke buffer offset
     */
    private static void putInPokeBuffer(SIRFilter filter,
            JVariableDefinition pokeBufDefn, 
            JFieldAccessExpression pokeBufAccess,
            JVariableDefinition pokeBufOffsetDef,
            JFieldAccessExpression pokeBufOffset) {

            int vecSize = KjcOptions.vectorize / 4;
            int bufsize = (vecSize - 1) * filter.getPushInt();  
        
            // add buffer and and offset declarations.
            filter.addField(new JFieldDeclaration(pokeBufDefn));
            filter.addField(new JFieldDeclaration(pokeBufOffsetDef));
        
            // statements to make a loop to push poke buffer contents
            JStatement initOffset = new JExpressionStatement(
                    new JAssignmentExpression(
                            pokeBufOffset,
                            new JIntLiteral(0)));
            JStatement moveOne = new JExpressionStatement(
                    new SIRPushExpression(
                            new JArrayAccessExpression(
                                    pokeBufAccess,
                                    pokeBufOffset),
                                    filter.getOutputType()));
            JStatement bump = new JExpressionStatement(
                new JPostfixExpression(
                        at.dms.kjc.Constants.OPE_POSTINC,
                        pokeBufOffset));
            JBlock moveBump = new JBlock(new JStatement[]{moveOne,bump});
            JStatement moveLoop = Utils.makeForLoop(moveBump, bufsize);
            List<JStatement> initAndLoop = 
                Arrays.asList(new JStatement[]{initOffset,moveLoop});
            
            // instert statements at end of block but before
            // any SIRMarker of final SIRPopExpression
            JBlock workBody = filter.getWork().getBody();
            List<JStatement> stmts = workBody.getStatements();
            int lastPos = stmts.size() - 1;
            JStatement last = stmts.get(lastPos);
            while (last instanceof SIRMarker
                    || (last instanceof JExpressionStatement
                            && ((JExpressionStatement) last).getExpression() instanceof SIRPopExpression) ) {
                lastPos--;
                last = stmts.get(lastPos);
            }
    
            workBody.addAllStatements(lastPos+1,initAndLoop);
            workBody.addStatement(0, (JStatement)AutoCloner.deepCopy(initOffset));
    }
    
    /**
     * Pull vector constants to static const (ACC_STATIC | ACC_FINAL) fields or local variables.
     * Munges a filter in place.
     */
    private static void raiseVectorConstants (final SIRFilter filter) {
        
        // Find all JVectorLiteral's in all methods, record in vlitToMeths
        // (Map needs to convert to Number which has structural equality / hash
        // from JVectorLiteral which has identity equality / hash.)
        final Map<Number,Set<JMethodDeclaration>>vlitToMeths = 
            new HashMap<Number,Set<JMethodDeclaration>>();
        JMethodDeclaration[] ms = filter.getMethods();
        for (final JMethodDeclaration m : ms) {
            m.accept(new SLIREmptyVisitor(){
                @Override
                public void visitVectorLiteral(JVectorLiteral self, JLiteral scalar) {
                    JLiteral scalarv = self.getScalar();
                    Number n;
                    if (scalarv instanceof JIntLiteral) {
                        n = ((JIntLiteral)scalarv).intValue();
                    } else if (scalarv instanceof JFloatLiteral) {
                        n = ((JFloatLiteral)scalarv).floatValue();
                    } else {
                        throw new AssertionError();
                    }
                    Set<JMethodDeclaration> inMs = vlitToMeths.get(n);
                    if (inMs == null) inMs = new HashSet<JMethodDeclaration>();
                    inMs.add(m);
                    vlitToMeths.put(n, inMs);
                }
            });
        }
        
        // create const definitions for each constant and record references in vlitReplacements
        final Map<Number,JExpression> vlitReplacements =  new HashMap<Number,JExpression>();

        for (Map.Entry<Number,Set<JMethodDeclaration>> vMeths : vlitToMeths.entrySet()) {
            Number vnum = vMeths.getKey();
            JLiteral scalar;
            CNumericType scalartype;
            if (vnum instanceof Integer) {
                scalartype = CStdType.Integer;
                scalar = new JIntLiteral((Integer)vnum);
            } else if (vnum instanceof Float) {
                scalartype = CStdType.Float;
                scalar = new JFloatLiteral((Float)vnum);
            } else {
                throw new AssertionError();
            }
            JExpression[] initElems = new JExpression[KjcOptions.vectorize / 4];
            for (int i = 0; i < initElems.length; i++) {
                initElems[i] = (JExpression)AutoCloner.deepCopy(scalar);
            }
            JArrayInitializer init = new JArrayInitializer(initElems);
            JVariableDefinition defn = new JVariableDefinition(
                    at.dms.kjc.Constants.ACC_STATIC | at.dms.kjc.Constants.ACC_FINAL,
                    new CVectorTypeLow(scalartype, KjcOptions.vectorize),
                    ThreeAddressCode.nextTemp(), 
                    init);

            if (vMeths.getValue().size() > 1) {
               // constant used in multiple methods: set up as field.
                JFieldDeclaration decl = new JFieldDeclaration(defn);
                JFieldAccessExpression access = new JFieldAccessExpression(new JThisExpression(), defn.getIdent());
                filter.addField(0,decl);
                vlitReplacements.put(vnum, access);
            } else {
                // constant used in single method: set up as local.
                JVariableDeclarationStatement decl = new JVariableDeclarationStatement(defn);
                JLocalVariableExpression access = new JLocalVariableExpression(defn);
                for (JMethodDeclaration m : vMeths.getValue()) {
                    // this loop should iterate exactly once
                    m.getBody().addStatement(0, decl);
                }
                vlitReplacements.put(vnum, access);
            }
        }

        // replace references to vector constants with references to variable.
        for (final JMethodDeclaration m : ms) {
            m.accept(new SLIRReplacingVisitor(){
                @Override
                public Object visitVectorLiteral(JVectorLiteral self, JLiteral scalar) {
                    Number n;
                    JLiteral l = self.getScalar();
                    if (l instanceof JIntLiteral) {
                        n = ((JIntLiteral)l).intValue();
                    } else if (l instanceof JFloatLiteral) {
                        n = ((JFloatLiteral)l).floatValue();
                    } else {
                        throw new AssertionError();
                    }
                    return vlitReplacements.get(n);
                }
            });
        }

    }
    
    /**
     * Given information about a method body, make any changes needed in the method body.
     * Down to: call fixTypesPeeksPopsPushes for each method body.
     * @param methodBody to process
     * @param vectorIds allows determination of which variables need vector types.
     * @param filter the filter being processed, allows access to push and pop rates.
     * @param pokeBuf an expression to access a buffer used to reorder pushes as needed.
     * @param pokeBufOffset an expression to refer to an offset in pokeBuf.
     * @return
     */
    static JBlock mungMethodBody(JBlock methodBody, 
            boolean vectorizePush, Map<String,CType> vectorIds,
            SIRFilter filter, JExpression pokeBuf,
            JExpression pokeBufOffset) {
//        // field initializations already moved, move initialization of locals.
//        JStatement methodBody2 = moveLocalInitializations(methodBody,vectorIds.keySet());
        // expand peeks, pops, and optionally pushes, and update types of fields and locals.
        
        JStatement methodbody3 = fixTypesPeeksPopsPushes(methodBody/*2*/, 
                filter.getPopInt(),  // original number popped in steady state.
                vectorizePush, 
                filter.getPushInt(),  // original number pushed in steady state.
                pokeBuf,
                pokeBufOffset,
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
     * @param methodBody method being processed.
     * @param vectorizePush do vectors flow to push or only scalars?
     * @param pokeBufRef root expression to make an array reference from to access poke buffer.
     * @param pokeBufOffset variable indicating current position in poke buffer.
     * @param vectorIds indication of what variables need vector types
     * @return
     */
    static JStatement fixTypesPeeksPopsPushes(JStatement methodBody,
            final int inputStride,
            final boolean vectorizePush, final int outputStride,
            final JExpression pokeBufRef,
            final JExpression pokeBufOffset,
            final Map<String,CType> vectorIds) {
            class fixTypesPeeksPopsPushesVisitor extends StatementQueueVisitor {
                JExpression lastLhs = null;   // left hand side of assignment being processed or null if not processing rhs of assignment
//                JExpression lastOldLhs = null; // value before converting.
                boolean leftHasVectorType = false; // true if in assignment and left has vector type.
                boolean suppressSelector = false; // true to not refer to as vector
                boolean replaceStatement = false; // true to discard current statement
                boolean constantsToVector = false; // rhs should have vector type, so turn constants in rhs into
                                                   // vector constants.
                @Override
                public Object visitVariableDefinition(JVariableDefinition self,
                        int modifiers,
                        CType type,
                        String ident,
                        JExpression expr) {
                    if (vectorIds.containsKey(ident)) {
//                        System.err.println("VisitVariableDefinition: " 
//                                + self.getIdent() + " " + self.getType().toString() 
//                                + " " + self.hashCode());
                        if (!(type instanceof CVectorType || 
                                (type instanceof CArrayType  &&
                                        ((CArrayType)type).getBaseType() instanceof CVectorType))) {
                            CType newType = CVectorType.makeVectortype(type);
                            self.setType(newType);
//                            System.err.println("                becomes: " 
//                                    + self.getIdent() + " " + self.getType().toString() 
//                                    + " " + self.hashCode());
                        }
                    }
                    return self;
                }
                
                
                @Override
                public Object visitPopExpression(SIRPopExpression self,
                        CType tapeType) {
                    if (lastLhs == null) {
                        // just a pop not returning a value.
                        return self;
                    }
                    // e = pop();    =>
                    // (e).a[1] = peek(1 * inputStride);
                    // (e).a[2] = peek(2 * inputStride);
                    // (e).a[3] = peek(3 * inputStride);
                    // (e).a[0] = pop();
                    // lastLhs non-null here.
                    
                    JExpression lhs = lastLhs;
                    // left hand side L converted to L.v, but we want just L 
                    // so as to convert to L.a[0] .. L.a[n] below
                    assert lhs instanceof JFieldAccessExpression;
                    lhs = ((JFieldAccessExpression)lhs).getPrefix();
                    
                    for (int i = 1; i < KjcOptions.vectorize / 4; i++) {
                        this.addPendingStatement(
                                new JExpressionStatement(
                                        new JAssignmentExpression(
                                                CVectorType.asArrayRef(lhs,i),
                                                new SIRPeekExpression(
                                                        new JIntLiteral(i * inputStride),
                                                        tapeType))));
                    }
                    this.addPendingStatement(
                            new JExpressionStatement(
                                    new JAssignmentExpression(
                                            CVectorType.asArrayRef(lhs,0),
                                            new SIRPopExpression(tapeType))));
                    replaceStatement = true; // throw out statement built with this return value
                    return self;
                }
                @Override
                public Object visitPeekExpression(SIRPeekExpression self,
                        CType tapeType, JExpression arg) {
                    // e1 = peek(e2);  =>
                    // (e1).a[0] = peek((e2) + 0 * inputStride);
                    // ...
                    // (e1).a[3] = peek((e2) + 3 * inputStride);

                    JExpression lhs = lastLhs;
                    // left hand side L converted to L.v, but we want just L 
                    // so as to convert to L.a[0] .. L.a[n] below
                    assert lhs instanceof JFieldAccessExpression;
                    lhs = ((JFieldAccessExpression)lhs).getPrefix();
                    

                    for (int i = 0; i < KjcOptions.vectorize / 4; i++) {
                        this.addPendingStatement(
                                new JExpressionStatement(
                                        new JAssignmentExpression(
                                                CVectorType.asArrayRef(lhs,i),
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
                    // process argument but do not put on ".v" on vectors
                    suppressSelector = true;
                    arg.accept(this);
                    suppressSelector = false;

                    // Assuming that SimplifyPushPopPeek() has been run as required: 
                    // Here we are pushing either a vector value or a constant.
                    // use maybeAsArrayRef to put the correct selector on:
                    // constants do not get a selector.
                    // If not a manifest constant, then the definition of 
                    // the local ref / field ref / array ref should have
                    // been modified to be a vector type.
                    
                    // push(e)  =>
                    // poke((e).a[0], 0 * outputStride);
                    // poke((e).a[1], 1 * outputStride);
                    // ...
                    // poke((e).a[3], 3 * outputStride);
                    // (assuming vector length of 4  4-byte words).
                    //
                    // Variants: at least on Pentiuum + SSE, storing a SSE register
                    // seems no faster than storing individual 4-byte fields, so
                    // push(e)  =>
                    // push((e).a[0]);
                    // poke((e).a[1], 1 * outputStride);
                    // ...
                    // poke((e).a[3], 3 * outputStride);
                    //
                    // If we know that pushes were to a buffer with sufficient space
                    // (rather than to a communication register or to a variable-
                    // length buffer) we could write out-of order to the output buffer.
                    // For now, just keep a separate buffer for pokes.
                    // since pushing the first element of the vector, the poke buffer
                    // is of size ((KjcOptions.vectorize / 4) - 1) * outputStride)
                    // and the converted code is:
                    // push(e)  =>
                    // push((e).a[0]);
                    // pokeBuf[head + 0 * outputStride] = (e).a[1];
                    // ...
                    // pokeBuf[head + 2 * outputStride] = (e).a[3];
                    // head++;
                    //

                    // Important case: if work is "push 1" then 
                    // we do not need to buffer "pokes"
                    // We still write as 4-byte fields since
                    // a nuisance to do otherwise: would require casting (supported)
                    // and pointer dereference (not supported) to use a single vector
                    // store.

                    for (int i = 1; i < KjcOptions.vectorize / 4; i++) {
                        if (outputStride == 1) {
                            this.addPendingStatement(
                                new JExpressionStatement(
                                        new SIRPushExpression(
                                                maybeAsArrayRef(arg,i),
                                                tapeType)));
                        } else {
//                         if (canPokeToPushTape)                           
//                            this.addPendingStatement(
//                                    new JExpressionStatement(
//                                            new SIRPokeExpression(
//                                                    maybeAsArrayRef(arg,i),
//                                                    i * outputStride,
//                                                    tapeType)));
                            this.addPendingStatement(
                                new JExpressionStatement(
                                    new JAssignmentExpression (
                                        new JArrayAccessExpression(
                                            pokeBufRef,
                                            new JAddExpression(
                                                pokeBufOffset,
                                                new JIntLiteral((i - 1) * outputStride))),
                                            maybeAsArrayRef(arg,i)
                                    )));
                        }
                    }
                    if (outputStride != 1) {
                        this.addPendingStatement(
                            new JExpressionStatement(
                                new JPostfixExpression(
                                    at.dms.kjc.Constants.OPE_POSTINC,
                                    pokeBufOffset)));
                    }
                    return new SIRPushExpression(
                            maybeAsArrayRef(arg,0),
                            tapeType);
                }
                
                
                private JExpression maybeAsArrayRef(JExpression arg, int offset) {
                    if (arg instanceof JLiteral) {
                        return arg;
                    }
                    CType type = arg.getType();
                    if (type == null || (type instanceof CVectorType) /*|| 
                        (type instanceof CArrayType 
                         && ((CArrayType)type).getBaseType() instanceof CVectorType)*/) {
                        return CVectorType.asArrayRef(arg,offset);
                    }
                    System.err.println("push with type = " + type.toString() + " : " + arg.toString());
                    return arg;
                    
                }
                
                @Override
                public Object visitAssignmentExpression(JAssignmentExpression self,
                    JExpression left, JExpression right) {
                // assignment has 4 cases
                // (1) vector_value = constant;
                // (2) vector_value = complex expression not containing vector
                // values;
                // (2b) array of vector values = array of scalar values.
                // (3) vector_value = expression containing vector value(s);
                // (4) non_vector = expression;
                leftHasVectorType = hasVectorType(left, vectorIds);
                // invariant: leftHasVectorType will be false on return.
                boolean rightHasVectorType = hasVectorType(right, vectorIds);
                if (leftHasVectorType && !rightHasVectorType) {
                    // setting a vector value from non-vector.
                    // _v4F b[N];
                    // float j, k;
                    // ...
                    // b[i] = j + k;
                    // 
                    // ==>
                    // 
                    // _v4F b[N];
                    // float j, k;
                    // ...
                    // tmp = j + k;
                    // b[i].a[0] = tmp;
                    // ...
                    // b[i].a[3] = tmp;
                    //
                    CType oldType = left.getType();
                    if (oldType instanceof CVectorType) {
                        // if not first reference to a variable on left, then
                        // the JVariableDefinition has probably been updated
                        // with the vector type -- since JVariableDefinition's 
                        // are shared.
                        oldType = ((CVectorType) oldType).getBaseType();
                    }
                    suppressSelector = true;
                    JExpression newLeft = (JExpression) left.accept(this);
                    // JExpression newLeftArray =
                    // CVectorType.asArrayRef(newLeft, i);
                    suppressSelector = false;
                    if (right instanceof JLiteral) {
                        // odd case:
                        // x = 2.0f;
                        // ==>
                        // static const tmp = {2.0f,2.0f,2.0f,2.0f};
                        // x.v = tmp;
                        // by setting rhs to be a "vector constant"
                        newLeft = CVectorType.asVectorRef(newLeft);
                        constantsToVector = true;
                        JExpression newRight = (JExpression) right.accept(this);
                        constantsToVector = false;
                        self.setLeft(newLeft);
                        self.setRight(newRight);
                        leftHasVectorType = false;
                        return self;
                    } else if (oldType instanceof CArrayType) {
                        // odd case:
                        // __v(basetype) array1[N1]...[Nk];
                        // basetype array2[N1]...[Nk];
                        // ...
                        // array1 = array2;
                        //
                        // ==>
                        //
                        // ...
                        // for (I1 = 0; I1 < N1; I1++) {
                        //   ...
                        //        for(Ik = 0; Ik <Nk; Ik++) {
                        //           basetype tmp0
                        //           __v(basetype) tmp1;
                        //           tmp0 = array2[I1]...[Ik]
                        //           for (i = 0; i < vectorlength; i++) {
                        //             tmp1.a[i] = tmp0;
                        //           }
                        //           array1[I1]...[Ik] = tmp1;
                        //        }
                        //  }...}
                        // (comes up in code for copying parameters in FMRadio (asplos06/fm)

                        //CArrayType arrayType = (CArrayType)oldType;
                        //Had problems with constant prop not propagating dimension into array...
                        //situation came up with arrays from parameters.  Have now hacked frontend
                        //so have the dimension of the source array (right), but not the dest in that case.
                        CArrayType arrayType = ((CArrayType)right.getType());
                        JExpression[] dims = arrayType.getDims();
                        JVariableDefinition[] dimVars = new JVariableDefinition[dims.length];
                        JLocalVariableExpression[] dimVarRefs = new JLocalVariableExpression[dims.length];
                        for (int i = 0; i < dims.length; i++) {
                            JVariableDefinition d = new JVariableDefinition(/* where */ null,
                                    /* modifiers */ 0,
                                    /* type */ CStdType.Integer,
                                    /* ident */ LoweringConstants.getUniqueVarName(),
                                    /* initializer */ new JIntLiteral(0));
                            dimVars[i] = d;
                            dimVarRefs[i] = new JLocalVariableExpression(d);
                        }
                        List<JStatement>loopBodyStmts = new LinkedList<JStatement>();
                        JVariableDefinition tmp0d = new JVariableDefinition(/* where */ null,
                                /* modifiers */ 0,
                                /* type */ arrayType.getBaseType(),
                                /* ident */  ThreeAddressCode.nextTemp(),
                                /* initializer */ null);
                        JLocalVariableExpression tmp0 = new JLocalVariableExpression(tmp0d);
                        JVariableDefinition tmp1d = new JVariableDefinition(/* where */ null,
                                /* modifiers */ 0,
                                /* type */ CVectorType.makeVectortype(arrayType.getBaseType()),
                                /* ident */  ThreeAddressCode.nextTemp(),
                                /* initializer */ null);
                        JLocalVariableExpression tmp1 = new JLocalVariableExpression(tmp1d);
                        loopBodyStmts.add(new JVariableDeclarationStatement(tmp0d));
                        loopBodyStmts.add(new JVariableDeclarationStatement(tmp1d));
                        loopBodyStmts.add( 
                                new JExpressionStatement(new JAssignmentExpression(tmp0,
                                        makeArrayReference(right,dimVarRefs))));
                        for (int i = 0; i < KjcOptions.vectorize / 4; i++) {
                            loopBodyStmts.add( 
                                new JExpressionStatement(new JAssignmentExpression(CVectorType.asArrayRef(tmp1,i),tmp0)));
                        }
                        loopBodyStmts.add (
                                new JExpressionStatement(new JAssignmentExpression(
                                        makeArrayReference(newLeft,dimVarRefs), tmp1)));
                        JStatement loopBody = new JBlock(loopBodyStmts);
                       
                        for (int i = dims.length-1; i>=0; i--) {
                            loopBody = Utils.makeForLoop(loopBody, dims[i], dimVars[i]);
                        }
                        this.addPendingStatement(loopBody);
                        replaceStatement = true;
                        leftHasVectorType = false;
                        return self;  // must output en expression.
                    } else {
                        JVariableDefinition defn = new JVariableDefinition(
                                oldType, ThreeAddressCode.nextTemp());
                        JStatement decl = new JVariableDeclarationStatement(
                                defn);
                        JLocalVariableExpression tmp = new JLocalVariableExpression(
                                defn);

                        addPendingStatement(decl);
                        addPendingStatement(new JExpressionStatement(
                                new JAssignmentExpression(tmp, right)));
                        for (int i = 0; i < KjcOptions.vectorize / 4; i++) {
                            addPendingStatement(new JExpressionStatement(
                                    new JAssignmentExpression(CVectorType
                                            .asArrayRef(newLeft, i), tmp)));
                        }
                        replaceStatement = true;
                        leftHasVectorType = false;
                        return self;
                    }
                }
                // cases (3) or (4)
                JExpression newLeft = (JExpression) left.accept(this);
                lastLhs = newLeft;
                if (leftHasVectorType) {
                    constantsToVector = true;
                }
                JExpression newRight = (JExpression) right.accept(this);
                constantsToVector = false;
                lastLhs = null;
                self.setLeft(newLeft);
                self.setRight(newRight);
                leftHasVectorType = false;
                return self;
            }

            private JExpression makeArrayReference(JExpression array,
                    JExpression[] offsets) {
                JExpression expr = array;
                for (int i = 0; i < offsets.length; i++) {
                    expr = new JArrayAccessExpression(expr, offsets[i]);
                }
                // should set type at each level of access expression
                return expr;
            }
                
            @Override
            public Object visitExpressionStatement(JExpressionStatement self,
                    JExpression expr) {
                // Some expressions need to return a statement or block of
                // statments, but need
                // to discard the statment in which they occurred in the input.
                // We expect any
                // such expressions to occur inside a JExpressionStatment.
                // visitors to such
                // expressions should use addPendingStatement to add the new
                // statments, and
                // should set replaceStatement=true.
                //
                replaceStatement = false;
                Object retval = super.visitExpressionStatement(self, expr);
                if (replaceStatement) {
                    assert retval instanceof JBlock;
                    JBlock b = (JBlock) retval;
                    b.removeStatement(0);
                    return b;
                } else {
                    return retval;
                }
            }

            /**
             * visits a method call. Expect all arguments to be converted
             * appropriately. <br/>XXX Warning: assumes that any methodcall
             * expression is the right hand side of an assignment
             * expression.
             */
            @Override
            public Object visitMethodCallExpression(JMethodCallExpression self,
                    JExpression prefix, String ident, JExpression[] args) {
                if (leftHasVectorType == true && KjcOptions.cell_vector_library) {

                    String equivalent_method_name = at.dms.util.Utils
                            .cellMathEquivalent(prefix, ident);
                    if (equivalent_method_name != null) {
                        // we have a vector method to convert to.
                        boolean old_constantsToVector = constantsToVector;
                        constantsToVector = true;
                        // so far all vector methods have vectors for all args.
                        visitArgs(args); // process args, munging array
                        constantsToVector = old_constantsToVector;
                        CType argtype = args[0].getType();
                        assert argtype instanceof CVectorTypeLow;  //TODO: may need to check for scalar needing SPLAT().
                        CType retType;
                        // deal with abs, max, min of integer vectors.
                        if (((CVectorTypeLow)argtype).getBaseType().isOrdinal()) {
                            retType = new CVectorTypeLow(CStdType.Integer,KjcOptions.vectorize);
                            if ("abs".equals(ident)) {
                                equivalent_method_name = "_abs_v4i";
                            } else if ("max".equals(ident)) {
                                equivalent_method_name = "_max_v4i";
                            } else if ("min".equals(ident)) {
                                equivalent_method_name = "_min_v4i";
                            } else {
                                throw new AssertionError("no integer vector equivalent to math function " + ident);
                            }
                        } else {
                            retType = new CVectorTypeLow(CStdType.Float,KjcOptions.vectorize);
                        }
                        JExpression retval = new JMethodCallExpression(self
                                .getTokenReference(), equivalent_method_name,
                                args);
                        retval.setType(retType);
                        return retval;
                    }
                    
                }
                return super.visitMethodCallExpression(self, prefix, ident,
                        args);
            }
                
                /**
                 * visits a float literal (what about double if accepted by
                 * parser?)
                 */
            @Override
            public Object visitFloatLiteral(JFloatLiteral self, float value) {
                if (constantsToVector) {
                    return new JVectorLiteral(self);
                }
                return self;
            }

                /**
                 * visits a int literal (what about other int types accepted by
                 * parser?)
                 */
            @Override
            public Object visitIntLiteral(JIntLiteral self, int value) {
                if (constantsToVector) {
                    return new JVectorLiteral(self);
                }
                return self;
            }
                
            @Override
            public Object visitFieldExpression(JFieldAccessExpression self,
                    JExpression left, String ident) {
                if (vectorIds.containsKey(getIdent(self)) && !suppressSelector) {
                    return CVectorType.asVectorRef(self);
                } else {
                    return self;
                }
            }
 
            @Override
            public Object visitLocalVariableExpression(
                    JLocalVariableExpression self, String ident) {
                if (vectorIds.containsKey(getIdent(self))) {
                    if (!(self.getType() instanceof CVectorType)) {
                        assert self.getVariable() instanceof JVariableDefinition;
                        JVariableDefinition newDefn = (JVariableDefinition) self
                                .getVariable().accept(this);
                        self = new JLocalVariableExpression(self
                                .getTokenReference(), newDefn);
                    }
                    if (!suppressSelector) {
                        return CVectorType.asVectorRef(self);
                    }
                }
                return self;
            }
            
            @Override
            public Object visitArrayAccessExpression(
                    JArrayAccessExpression self, JExpression prefix,
                    JExpression accessor) {
                prefix.accept(this);
                // array accessors don't always have correct type.
                CType prefixType = prefix.getType();
                assert prefixType instanceof CArrayType;
                // System.err.print("Array accessor type for " + self + " was "
                // + self.getType());
                CArrayType prefixaType = (CArrayType) prefixType;
                int numDims = prefixaType.getArrayBound();
                if (numDims == 1) {
                    self.setType(prefixaType.getBaseType());
                } else {
                    JExpression[] dims = prefixaType.getDims();
                    JExpression[] newDims = null;
                    if (dims != null) {
                        newDims = new JExpression[dims.length - 1];
                        for (int i = 1; i < dims.length; i++)
                            newDims[i - 1] = dims[i];
                    }
                    self.setType(new CArrayType(prefixaType.getBaseType(),
                            numDims - 1, newDims));
                }
                // System.err.println(" ==> " + self.getType());

                // note if recurr into accessor, need to set constantsToVector
                // temporarily to false
                if (vectorIds.containsKey(getIdent(self)) && !suppressSelector) {
                    // does not currently work for array slices, but StreeamIt
                    // does not currently support array slices
                    return CVectorType.asVectorRef(self);
                } else {
                    return self;
                }
            }
            
            @Override
                public Object visitShiftExpression(JShiftExpression self, int oper,
                    JExpression left, JExpression right) {
                JExpression newLeft = (JExpression) left.accept(this);
                self.setLeft(newLeft);
// do not visit right on a shift expression: always int!
//                boolean oldConstantsToVector;
//                oldConstantsToVector = constantsToVector;
//                constantsToVector = false;
//                JExpression newRight = (JExpression) right.accept(this);
//                constantsToVector = oldConstantsToVector;
                if (KjcOptions.cell_vector_library && newLeft.getType() instanceof CVectorTypeLow) {
                    JExpression[] args = {newLeft,right};
                    String ident;
                    switch (oper) {
                    //Constants.OPE_SL, Constants.OPE_SR, Constants.OPE_BSR
                    case Constants.OPE_SL:
                        ident = "_lsl_v4i"; break;
                    case Constants.OPE_SR:
                        ident = "_sra_v4i"; break;
                    case Constants.OPE_BSR:
                        ident = "_srl_v4i"; break;
                    default: throw new AssertionError();
                    }
                    JExpression retval = new JMethodCallExpression(self.getTokenReference(), ident, args);
                    retval.setType(new CVectorTypeLow(CStdType.Integer, KjcOptions.vectorize));
                    return retval;
                } else {
                    return self;
                }
            }

            @Override
            public Object visitBinaryExpression(JBinaryExpression self, String oper,
                        JExpression left, JExpression right) {
                JExpression newLeft = (JExpression)left.accept(this);
                JExpression newRight = (JExpression)right.accept(this);
                CType leftType = newLeft.getType();
                CType rightType = newRight.getType();
 
                if (! KjcOptions.cell_vector_library
                        || oper.equals("+") || oper.equals("-")
                        || (! (leftType instanceof CVectorTypeLow) && ! (rightType instanceof CVectorTypeLow))) {
                    self.setLeft(newLeft);
                    self.setRight(newRight);
                    return self;
                }
                // here if operation is vector % or vector / and cell_vector_library set.
                // cell_vector_library handles integer division with a macro
                // because the IBM xlc compiler (as of compiler version 8.1, SDK version 2)
                // does not recognize division of integer vectors.
                CVectorTypeLow vtype = (CVectorTypeLow)(leftType != null ? leftType : rightType);
                if (oper.equals("/") && vtype.getBaseType().isOrdinal()) {
                    JExpression[] args = {newLeft,newRight};
                    String ident = "_divide_v4i";
                    JMethodCallExpression retval = new JMethodCallExpression(self.getTokenReference(),ident,args);
                    retval.setType(new CVectorTypeLow(CStdType.Integer,KjcOptions.vectorize));
                    return retval;
                } else if (oper.equals("%")) {
                    JExpression[] args = {newLeft,newRight};
                    String ident;
                    CType retType;
                    if (vtype.isOrdinal()) {
                        ident = "_modulus_v4i";
                        retType = new CVectorTypeLow(CStdType.Integer,KjcOptions.vectorize);
                    } else {
                        ident = "_modulus_v4f";
                        retType = new CVectorTypeLow(CStdType.Float,KjcOptions.vectorize);
                    }
                    JMethodCallExpression retval = new JMethodCallExpression(self.getTokenReference(),ident,args);
                    retval.setType(retType);
                    return retval;
                } else {
                    // floating division: leave as is
                    self.setLeft(newLeft);
                    self.setRight(newRight);
                    return self;
                }

            }
            }   

           
        return (JStatement)methodBody.accept(new fixTypesPeeksPopsPushesVisitor());
        
    }
    
   /** 
    * Check whether an expression refers to a variable or array that should have a vector type.
    * 
    * <br/>Used to check if a side of an assignment has vector type.
    * 
    * <br/>Just checks leaves of expression tree: peek() or pop() has vecttor type, or else
    * woudn't be running Vectorize. Local Variable, Field, and Array Access expressions are checked
    * against the vectorIds parameter.
    * 
    * <br/>XXX Warning: Implicit assumption is that if any part of expression has a vector type, 
    * then the whole expression has a vector type.
    * 
    * @param expr
    * @param vectorIds
    * @return true if expression has vector type.
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
            @Override
            public void visitPeekExpression(SIRPeekExpression self, CType tapeType, JExpression arg) {
                hasVectorType[0] = true;
            }
            @Override
            public void visitPopExpression(SIRPopExpression self, CType tapeType) {
                hasVectorType[0] = true;
            }
        }
        
        expr.accept(new hasVectorTypeVisitor());
        return hasVectorType[0];
    }
            
    
//    /**
//     * Divide certain JVariableDeclarationStatement's into declaration and separate assignment.
//     * <br/>For variables in "varnames" change any initialization of these variables in "block"
//     * or any sub-block into separate declaration and assignment statements.
//     * <ul><li>
//     * If you use this on a JVariableDeclarationStatement in the init portion of a "for" statement.
//     * may end up with an un-compilable "for" statement.
//     * </li><li>
//     * The result of this pass may mix declarations with assignments.  
//     * If you want declarations first, run {@link VarDeclRaiser} after this.
//     * </li></ul>
//     * @param block the statement, usually a block, to con
//     * @param idents the variable names (from getIdent() of a definition or use).
//     */
//    static JStatement moveLocalInitializations (JStatement block, final Set<String> idents) {
//        class moveDeclsVisitor extends StatementQueueVisitor {
//            private boolean inVariableDeclarationStatement = false;
//
//            @Override
//            public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
//                    JVariableDefinition[] vars) {
//                inVariableDeclarationStatement = true;
//                Object retval = super.visitVariableDeclarationStatement(self,vars);
//                inVariableDeclarationStatement = false;
//                return retval;
//            }
//            
//            @Override
//            public Object visitVariableDefinition(JVariableDefinition self,
//                    int modifiers,
//                    CType type,
//                    String ident,
//                    JExpression expr) {
//                if (inVariableDeclarationStatement && expr != null && idents.contains(ident)) {
// //                   if (! (expr instanceof JArrayInitializer)) {
//                        JStatement newInit = new JExpressionStatement(expr);
//                        addPendingStatement(newInit);
//                        self.setExpression(null);
////                    } else {
////                    }
//                }
//                return self;
//            }
//            
//        }
//
//        return (JStatement)block.accept(new moveDeclsVisitor());
//
//    }
    
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
     * Note: This is a relatively dumb taint checker, working mostly on the structure of
     * assignment statements, and reconstructing types as it goes.
     * 
     * A more elegant version might use type inference based on tainting the type of the input tape.
     * A type ionference version would require a full bottom-up pass unifying the type from
     * peek or pop with the type of an expression (LackWit for StreamIt).  
     * 
     * We make no effort to unify types across complex expressions (the user can fix by 
     * simplifying the StreamIt code).  We make no effort to deal with vectorizable fields of structs.
     * If the current inference is in error, the user can decide not to turn vectorization on.
     * 
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
                private boolean rePropagate = false; // should replace special case of binary arithmetic on rhs of assignment.
                @Override
                public void visitPeekExpression(SIRPeekExpression self,
                        CType tapeType, JExpression arg) {
                    haveType = filter.getInputType();
                }
                @Override
                public void visitPopExpression(SIRPopExpression self,
                        CType tapeType) {
                    haveType = filter.getInputType();
                }
                @Override
               public void visitVariableDefinition(
                        JVariableDefinition self, int modifiers,
                        CType type, String ident, JExpression expr) {
                    // only recurr into VariableDefinition if part of VariableDeclaration.
                    haveType = null;
                    if (expr != null) {
                        boolean oldIsLhs = isLhs;
                        isLhs = false;
                        expr.accept(this);
                        isLhs = oldIsLhs;
                    }
                    if (haveType != null) {
                        registerv(ident, haveType, type);
                    }
                    haveType = null;
                }
                @Override
                public void visitPushExpression(SIRPushExpression self,
                        CType tapeType,
                        JExpression arg) {
                    super.visitPushExpression(self,tapeType,arg);
                    if (haveType != null) {
                        vectorizePush[0] = true;  // push takes a vector type.
                    }
                }
                
                @Override
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
                
                @Override
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
                @Override
               public void visitLocalVariableExpression(JLocalVariableExpression self,
                        String ident) {
                    if (isLhs && (haveType != null)) {
                        registerv(ident,haveType,null);
                    }
                    if (! isLhs && typemap.containsKey(ident)) {
                        haveType = filter.getInputType();
                    }
                }
                @Override
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
                    if (haveType != null) {
                        rePropagate = true;
                        isLhs = true; // force variables to register
                        right.accept(this);
                        isLhs = false; 
                        rePropagate = false;
                    }
                }
                
                /* 
                 * TODO: binary arith ops: types match.
                 * TODO: combine w vectorizable.
                 */
                @Override
                public void visitCompoundAssignmentExpression(
                        JCompoundAssignmentExpression self, int oper,
                        JExpression left, JExpression right) {
                    isLhs = false;
                    haveType = null;
                    right.accept(this);
                    left.accept(this);  // left is also r-val in  a op= b;
                    if (haveType != null) {
                        rePropagate = true;
                        isLhs = true;  // force variables to register
                        left.accept(this);
                        right.accept(this);
                        isLhs = false;
                        rePropagate = false;
                    }
                }
                
                @Override
                public void visitCastExpression(JCastExpression self,
                        JExpression expr,
                        CType type) {
                    // don't look inside cast expression.
                    // multiple vector types in one module not yet supported.
                    return;
                }   
                
                @Override
                public void visitShiftExpression(JShiftExpression self,
                        int oper,    
                        JExpression left,
                        JExpression right) {
                        // do not visit right on a shift expression: always int!
                        // left only vectorizable if we have supporting intrinsics. 
                        if (KjcOptions.cell_vector_library) {
                            left.accept(this); 
                        }
                }

                
//                // situation:  A = B op C,   B is vectorizable, so C had
//                // better be vectorizable.  In code above, after finding that
//                // A is vectorizable, make sure that both B and C are vectorizable
//                // by propagating information back to the right.
//                //
//                // could really propagate right to any expression, but would have
//                // to take into account type changes from array / field construction
//                // / deconstruction, relational epxressions, casts, etc.
//                boolean CanPropagateRightTo(JExpression exp) {
//                    return exp instanceof JFieldAccessExpression ||
//                    exp instanceof JLocalVariableExpression ||
//                    exp instanceof JArrayAccessExpression;
//                }
//                
                // eventually use type to handle structs of arrays, arrays of structs, arrays of arrays, ...
                // or build up a PathToVec.
                private void registerv(String v,
                        CType type, CType declType) {
                    if (! typemap.containsKey(v)) {
                        changes[0] = true;
                        typemap.put(v,type);
                    }
                    return;
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

}
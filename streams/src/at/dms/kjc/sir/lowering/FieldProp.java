package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.CommonUtils;
import java.util.*;

/**
 * This class propagates constant assignments to field variables from
 * the init function into other functions.
 * $Id: FieldProp.java,v 1.41 2007-11-26 20:17:56 rabbah Exp $
 */
public class FieldProp implements Constants
{
    static boolean debugPrint = false;
    
    /** Maps field names to CTypes. */
    private HashMap<String, CType> types;
    
    /*
     * a field, if noticed, will either have a JExpression in
     * 'fields' if it may possibly be propagated, or will have
     * its name in the set 'nofields' if it may not be propagated.
     * 
     * a field which is an array will further have information
     * about its slots: 'arrays' maps array names to arrays of 
     * expressions, 'noarrays' maps an array name to an array of
     * booleans, where a boolean is 'true' if the slot can not
     * be propagated.
     * 
     */
    
    /** Maps field names to JExpression values. */
    private HashMap<String, JExpression> fields;
    /** List of field names that can't be propagated. */
    private HashSet<String> nofields;
    /** Maps field names to JExpression arrays.  If a particular array
        element is null, that field isn't a constant (or hasn't been
        determined yet). */
    private HashMap<String, JExpression[]> arrays;
    /** Maps field names to boolean arrays to track array elements that
        can't be propagated.  If the array element is true, that element
        can't be propagated; if it's false, it can. */
    private HashMap<String, boolean[]> noarrays;
    
    private FieldProp()
    {
        types = new HashMap<String, CType>();
        fields = new HashMap<String, JExpression>();
        nofields = new HashSet<String>();
        arrays = new HashMap<String, JExpression[]>();
        noarrays = new HashMap<String, boolean[]>();
    }

    /**
     * Performs a depth-first traversal of an SIRStream tree, and
     * calls propagate() on any SIRSplitJoins as a post-pass.
     * 
     * @param str a SIRStream
     */
    public static SIRStream doPropagate(SIRStream str) {
        return doPropagate(str, false, false);
    }

    /**
     * Performs a depth-first traversal of an SIRStream tree, and
     * calls propagate() on any SIRSplitJoins as a post-pass.
     * 
     * @param str a SIRStream
     * @param unrollOuterLoops whether to unroll outer (for) loops.
     */
    public static SIRStream doPropagate(SIRStream str, boolean unrollOuterLoops) {
        return doPropagate(str, unrollOuterLoops, false);
    }
    
    // the way the code was written, unrollOuterLoops applied only to the 
    // root of the stream graph.  Is this correct?  Presumably if run after
    // constant prop, loops will already be unrolled...
 
    /**
     * Performs a depth-first traversal of an SIRStream tree, and
     * calls propagate() on any SIRSplitJoins as a post-pass.
     * 
     * @param str a SIRStream
     * @param unrollOuterLoops whether to unroll outer (for) loops.
     * @param removeDeadFields whether to remove fully-propagated fields.
     */
    public static SIRStream doPropagate(SIRStream str, boolean unrollOuterLoops,
                                        boolean removeDeadFields)
    {
        // First, visit children (if any).
        if (str instanceof SIRFeedbackLoop)
            {
                SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
                doPropagate(fl.getBody(),false,removeDeadFields);
                doPropagate(fl.getLoop(),false,removeDeadFields);
            }
        if (str instanceof SIRPipeline)
            {
                SIRPipeline pl = (SIRPipeline)str;
                Iterator iter = pl.getChildren().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = (SIRStream)iter.next();
                        doPropagate(child,false,removeDeadFields);
                    }
            }
        if (str instanceof SIRSplitJoin)
            {
                SIRSplitJoin sj = (SIRSplitJoin)str;
                Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = iter.next();
                        doPropagate(child,false,removeDeadFields);
                    }
            }
        
        return doPropagateNotRecursive(str, unrollOuterLoops, removeDeadFields); 
    }
    
    /**
     * Perform field propagation on a given stream, without recursing into its child streams.
     */
    public static SIRStream doPropagateNotRecursive(SIRStream str) {
        return doPropagateNotRecursive(str, false, false);
    }

    public static SIRStream doPropagateNotRecursive(SIRStream str,
                                                    boolean unrollOuterLoops, boolean removeDeadFields) {
        // Propagate fields once (resolves params to filters)
        new FieldProp().propagate(str);

        // unroll and propagate to a fixed point
        FieldProp lastProp;
        boolean stillPropagating;
        do {
            // Run the unroller...
            Unroller unroller;
            for (int i = 0; i < str.getMethods().length; i++) {
                do {
                    unroller = new Unroller(new Hashtable(), unrollOuterLoops);
                    str.getMethods()[i].accept(unroller);
                } while (unroller.hasUnrolled());
            }
            // Propagate fields...
            lastProp = new FieldProp();
            stillPropagating = lastProp.propagate(str);
        } while (stillPropagating);
        
        // Remove uninvalidated fields
        if (removeDeadFields) {
            LinkedList<JFieldDeclaration> keepFields = new LinkedList<JFieldDeclaration>();
            Set<String> removedFields = new HashSet<String>();
            JFieldDeclaration[] fields = str.getFields();
            for (int i = 0; i < fields.length; i++) {
                JFieldDeclaration thisField = fields[i];
                String thisFieldIdent = thisField.getVariable().getIdent();
                if (! lastProp.isFieldInvalidated(thisFieldIdent)
                    && allUsesAreFullyQualified(str,thisField)) {
                    if (debugPrint) {
                        System.out.println("Removing:" + thisFieldIdent);
                    }
                    removedFields.add(thisFieldIdent);
                } else {
                    keepFields.add(thisField);
                }
            }
            str.setFields(keepFields
                          .toArray(new JFieldDeclaration[0]));
            removeAssignmentsToFields(str, removedFields);
            if (debugPrint) {
                System.err
                    .println("FieldProp: Stream after doPropagateNonRecursive with dead field removal");
                SIRToStreamIt.runNonRecursive(str);
            }
        }
        // All done, return the object.
        return str;
    }
    
    /**
     * It is not safe to remove a propagated field unless all references
     * to the field are fully qualified.
     * 
     * A reference is fully qualified if it follows the structure of the
     * field to a leaf.
     * 
     * For instance: if the declaration is
     * my_struct[20] my_field;
     * where my_struct does not have any further array or structure components
     * then
     * 
     * if all uses are of the form
     * my_field[n].my_struct_component
     * then the declaration of my_struct is removable.
     * however, if any references are of the form
     * my_field[n]
     * or of the form
     * my_field
     * then the declaration of my_struct is not removable.
     * 
     * @param str     SIRStream element in which field occurrs
     * @param field   Field declaration
     * @return        Whether safe to remove assignments to field
     * 
     */
    private static boolean allUsesAreFullyQualified(SIRStream str, 
                                                    final JFieldDeclaration field) {
        if (field.getType().isPrimitive()) {
            return true;
        }
        // Look at all assignments and determine if they are
        // assigning to a primitive type.  If any one is not, then
        // return false.  TODO: implement.
        String thisFieldIdent = field.getVariable().getIdent();
        return false;
    }
    
    private static void removeAssignmentsToFields(SIRStream str, 
                                                  final Set/*<String>*/<String> fields) {

        final boolean[] makeEmpty = {false};
        JMethodDeclaration[] methods = str.getMethods();
        for (int i = 0; i < methods.length; i++) {
            methods[i].accept(new SLIRReplacingVisitor() {
                    // If an expression statement is an assignment to a removed field
                    // then makeEmpty[0] will be set to true, in which case return 
                    // an empty statement.  Otherwise return this statement.
                    public Object visitExpressionStatement(JExpressionStatement self,
                                                           JExpression expr) {
                        makeEmpty[0] = false;
                        super.visitExpressionStatement(self,expr);
                        if (makeEmpty[0]) {
                            return new JEmptyStatement();
                        }
                        return self;
                    }
                
                    // only need to look at direct JAssignmentExpression since
                    // that is all that propagation looks at.
                
                    public Object visitAssignmentExpression(JAssignmentExpression self,
                                                            JExpression left, JExpression right) {
                        JExpression field = CommonUtils.lhsBaseExpr(left);
                        if (field instanceof JFieldAccessExpression) {
                            String ident = ((JFieldAccessExpression)field).getIdent();
                            makeEmpty[0] = fields.contains(ident);
                        }
                        return self;
                    }
                });
            
        }
        
    }

    /**
     * Does the actual work on <filter>.  Propagates constants within
     * this filter to a fixed point.  Returns whether or not anything
     * was propagated.
     */
    private boolean propagate(SIRStream filter) {
        boolean didPropagation = false;
        // so that our first hash does not include garbage code
        DeadCodeElimination.removeDeadLocalDecls(filter);
        do {
            findCandidates(filter);
            
            // System.out.println("--------------------");
            // System.out.println("Candidates Fields : ");
            // Iterator keyIter = this.types.keySet().iterator();
            // while (keyIter.hasNext()) {
            //    Object f = */ keyIter.next();
            //    System.out.println("Field: " + f + " " + this.types.get(f)
            //      + " --> " + this.fields.get(f));
            // }

            long oldHash = HashLiterals.doit(filter);
            doPropagation(filter);
            // to eliminate garbage introduced by propagator, don't
            // want it to affect the hash.  (Don't remove dead field
            // decls, because that strips valid fields out of Globals.)
            DeadCodeElimination.removeDeadLocalDecls(filter);
            long newHash = HashLiterals.doit(filter);

            // if the literals in the filter didn't change, then quit
            // propagating
            if (oldHash == newHash)  break;

            // mark that we did some propagating
            didPropagation = true;
        } while (true);
        return didPropagation;
    }
    
    /** Helper function to determine if a field has been invalidated. 
     * 
     * a field name is invalidated if it is contained in 'nofields'
     */
    private boolean isFieldInvalidated(String name)
    {
        return nofields.contains(name);
    }

    /** Helper function to determine if an array slot has been invalidated. 
     * 
     * an array slot is invalidated if the array field is invalidated or
     * if a boolean array exists in 'noarrays' for the array name and
     * the slot is invalidated (true)  in the boolean array.
     */
    private boolean isArrayInvalidated(String name, int slot)
    {
        if (isFieldInvalidated(name)) return true;
        boolean[] bary = noarrays.get(name);
        if (bary == null)
            return false;
        return bary[slot];
    }

    /** Helper function to invalidate a particular field. 
     *
     * (move its name from 'fields' to 'noields')
     */
    private void invalidateField(String name)
    {
        // System.out.println("Invalidating field: " + name);
        nofields.add(name);
        fields.remove(name);
    }

    /** Helper function to invalidate a particular array slot.
        This requires that the array have already been noticed. */
    private void invalidateArray(String name, int slot)
    {
        // Stop early if the field has already been invalidated.
        if (isFieldInvalidated(name)) return;
        boolean[] bary = noarrays.get(name);
        JExpression[] exprs = arrays.get(name);
        if ((bary == null)&&(exprs!=null))
            {
                bary = new boolean[exprs.length];
                noarrays.put(name, bary);
            }
        if(bary!=null)
            bary[slot] = true;
        if(exprs!=null)
            exprs[slot] = null;
    }

    /** 
     * Helper function to invalidate whatever a particular expression
     * points to.  
     *  
     *   No effect if the expression isn't a field-access
     *   or array-access expression.
     *   
     *   WTF: only invalidates JFieldAccessExpression,
     *   not JArrayAccessExpression (which is not sub-class)
     *   so appears to not be doing it's documented job on
     *   arrays.
     */
    private void invalidateExpression(JExpression expr)
    {
        if (expr instanceof JFieldAccessExpression)
            {
                JFieldAccessExpression fae = 
                    (JFieldAccessExpression)expr;
                String name = fae.getIdent();
                if (!(fae.getPrefix() instanceof JThisExpression))
                    return;
                invalidateField(name);
            }
    }

    /** Helper function to determine if a field can be propagated. 
     * 
     * a field can propagate if it has already been added to 'fields'
     * but has not been subsequently invalidated (moved to 'nofields') 
     */
    private boolean canFieldPropagate(String name)
    {
        if (isFieldInvalidated(name)) return false;
        if (!fields.containsKey(name)) return false;
        return true;
    }

    /** Helper function to determine if an array slot can be propagated. 
     * 
     * An array slot can be propagated if neither the array as a whole
     * nor the slot has been invalidated and if the slot contains an
     * (non-null) JExpression.
     */
    private boolean canArrayPropagate(String name, int slot)
    {
        if (isFieldInvalidated(name)) return false;
        if (isArrayInvalidated(name, slot)) return false;
        JExpression[] exprs = arrays.get(name);
        if (exprs == null) return false;
        if (exprs[slot] == null) return false;
        return true;
    }   

    /** Helper function returning the constant value of a field. */
    private JExpression propagatedField(String name)
    {
        return fields.get(name);
    }
    
    /** Helper function returning the constant value of an array slot. */
    private JExpression propagatedArray(String name, int slot)
    {
        JExpression[] exprs = arrays.get(name);
        return exprs[slot];
    }

    /** Force a literal JExpression type to a particular other type. 
     * 
     * looks like forces double literals to be float literals, else no-op
     */
    private JExpression forceLiteralType(JExpression expr, CType type)
    {
        switch(type.getTypeID())
            {
            case TID_FLOAT:
                if (expr instanceof JDoubleLiteral)
                    return new JFloatLiteral(expr.getTokenReference(),
                                             (float)expr.doubleValue());
                return expr;
            }
        return expr;
    }

    /** Notice that a field variable has a constant assignment. 
     * 
     * in fact, does not live up to the coment above:
     * if field already has been noticed to have an assignment
     * then it is invalidated.  So we are tracking single assignment
     * to a field, not all assignments to field being the same value.
     */
    private void noticeFieldAssignment(String name, JExpression value)
    {
        // If the field has already been invalidated, stop now.
        if (isFieldInvalidated(name)) return;
        // If the field has a value, invalidate it.
        if (canFieldPropagate(name))
            {
                invalidateField(name);
                return;
            }
        // Otherwise, add the name/value pair to the hash table.
        value = forceLiteralType(value, types.get(name));
        fields.put(name, value);
    }

    /** Notice that an array slot has a constant assignment. */
    private void noticeArrayAssignment(String name, int slot,
                                       JExpression value)
    {
        // Same as before...
        if (isArrayInvalidated(name, slot)) return;
        if (canArrayPropagate(name, slot))
            {
                invalidateArray(name, slot);
                return;
            }
        // Okay, populate the array slot.  The expression array
        // needs to already exist.
        CType atype = types.get(name);
        value = forceLiteralType(value, ((CArrayType)atype).getBaseType());
        JExpression[] exprs = arrays.get(name);
        if(exprs!=null) exprs[slot] = value;
    }

    /** Notice that an array exists with a fixed size. 
     * 
     *  updates: arrays
     *  
     *  If the name is not invalidated and 'arrays' does not already
     *  contain the name, then create a new array of (null) expressions
     *  in 'arrays'
     */
    private void noticeArrayCreation(String name, int size)
    {
        // Punt if the field has been invalidated, or if the array
        // entry already exists.
        if (isFieldInvalidated(name)) return;
        if (arrays.containsKey(name)) return;
        JExpression[] exprs = new JExpression[size];
        arrays.put(name, exprs);
    }

    /** Look for candidate fields in a filter. 
     * 
     * First sets up 'types' for all declared fields.
     * Then puts fields and 1-d array slots assigned a 
     * single constant into 'fields' and 'arrays'
     * possibly invalidating them as it goes...
     * 
     * Then looks at assignment statements to fields in 'this' 
     * and "notices" all fields that are assigned a scalar value.
     * Also "notices" assignments of literals to constant offsets
     * of 1-d arrays that are fields. 
     * 
     * Compound assignments, prefix and postfix result in
     * the fields being immediately invalidated.
     *
     */
    private void findCandidates(SIRStream filter)
    {
        JFieldDeclaration[] fields = filter.getFields();
        for (int i = 0; i < fields.length; i++)
            {
                // WTF: the visitor should only be necessary if 
                // field declarations can be nested.  Else simple
                // loop should do.
                fields[i].accept(new SLIREmptyVisitor() {
                        public void visitFieldDeclaration(JFieldDeclaration self,
                                                          int modifiers,
                                                          CType type,
                                                          String ident,
                                                          JExpression expr)
                        {
                            // add entry to name->type mapping
                            types.put(ident, type);
                            // notice arrays
                            if (type.isArrayType()) {
                                JExpression[] dims = ((CArrayType)type).getDims();
                                if (dims.length == 1 &&
                                    dims[0] instanceof JIntLiteral) {
                                    noticeArrayCreation
                                        (ident,
                                         ((JIntLiteral)dims[0]).intValue());
                                } else {
                                    invalidateField(ident);
                                }
                            }
                        }
                    });
            }

        JMethodDeclaration[] meths = filter.getMethods();
        for (int i = 0; i < meths.length; i++)
            {
                meths[i].accept(new SLIREmptyVisitor() {
                        public void visitAssignmentExpression
                            (JAssignmentExpression self,
                             JExpression left,
                             JExpression right)
                        {
                            super.visitAssignmentExpression(self, left, right);
                            if (left instanceof JFieldAccessExpression)
                                {
                                    JFieldAccessExpression fae =
                                        (JFieldAccessExpression)left;
                                    String name = fae.getIdent();
                                    // Look inside of fae; the left-hand side should
                                    // be this.
                                    if (!(fae.getPrefix() instanceof JThisExpression))
                                        return;
                                    // Okay; what's the right hand side?  Notice
                                    // if it's a literal or (new type[]); invalidate
                                    // otherwise.
                                    if (right instanceof JLiteral)
                                        noticeFieldAssignment(name, right);
                                    else
                                        invalidateField(name);
                                }
                            else if (left instanceof JArrayAccessExpression)
                                {
                                    JArrayAccessExpression aae =
                                        (JArrayAccessExpression)left;
                                    // Check that the prefix is a FieldAccessExpr.
                                    // Same rules as above.
                                    JExpression prefix = aae.getPrefix();
                                    if (!(prefix instanceof JFieldAccessExpression))
                                        return;
                                    JFieldAccessExpression fae =
                                        (JFieldAccessExpression)prefix;
                                    String name = fae.getIdent();
                                    if (!(fae.getPrefix() instanceof JThisExpression))
                                        return;
                                    // Also check that the offset is constant.
                                    // If it's not, invalidate the whole thing.
                                    JExpression accessor = aae.getAccessor();
                                    if (!(accessor instanceof JIntLiteral))
                                        {
                                            invalidateField(name);
                                            return;
                                        }
                                    int slot = ((JIntLiteral)accessor).intValue();
                                    // Now look at the right-hand side.
                                    if (right instanceof JLiteral)
                                        noticeArrayAssignment(name, slot, right);
                                    else
                                        invalidateArray(name, slot);
                                }
                        }
                        public void visitCompoundAssignmentExpression
                            (JCompoundAssignmentExpression self,
                             int oper,
                             JExpression left,
                             JExpression right)
                        {
                            super.visitCompoundAssignmentExpression
                                (self, oper, left, right);
                            // Instant death.
                            invalidateExpression(left);
                        }
                        public void visitPrefixExpression
                            (JPrefixExpression self,
                             int oper,
                             JExpression expr)
                        {
                            super.visitPrefixExpression(self, oper, expr);
                            // Again, instant death.
                            invalidateExpression(expr);
                        }
                        public void visitPostfixExpression
                            (JPostfixExpression self,
                             int oper,
                             JExpression expr)
                        {
                            super.visitPostfixExpression(self, oper, expr);
                            // Again, instant death.
                            invalidateExpression(expr);
                        }
                    });
            }
    }

    /** Replace previously-notice candidate fields. 
     * 
     * Visitor propagates into  rhs of assignments,
     * field expressions, array access expressions.
     * 
     * Visitor is applied in method bodies, field initializers,
     * and push / pop / peek expressions of a (phased)filter.
     * 
     * After visiting a method, calls Propagate with all the 
     * local (scalar) variables of the method that have a single
     * constant assignment.  A Propagator is also run over the
     * push / peek / pop expressions and the field initializers.
     *
     * Returns whether or not any field was propagated.
     */
    private void doPropagation(SIRStream filter) {
        JMethodDeclaration[] meths = filter.getMethods();
        // Do not propagate fields on lhs of assignment
        // unless involved in array offest calculation
        final boolean[] inLeftHandSide = {false};
        final boolean[] inArrayOffest = {false};
        SLIRReplacingVisitor theVisitor = new SLIRReplacingVisitor() {
                public Object visitAssignmentExpression(JAssignmentExpression self,
                                                        JExpression left, JExpression right) {
                    inLeftHandSide[0] = true;
                    JExpression newLeft = (JExpression)left.accept(this);
                    inLeftHandSide[0] = false;
                    JExpression newRight = (JExpression)right.accept(this);
                    return new JAssignmentExpression(self.getTokenReference(),
                                                     newLeft, newRight);
                }

                public Object visitFieldExpression(JFieldAccessExpression self,
                                                   JExpression left, String ident) {
                    Object orig = super.visitFieldExpression(self, left, ident);
                    if (canFieldPropagate(ident)
                        && (inArrayOffest[0]
                            || !inLeftHandSide[0])) {
                        return propagatedField(ident);
                    }
                    else {
                        return orig;
                    }
                }

                public Object visitArrayAccessExpression(JArrayAccessExpression self, JExpression prefix,
                                                         JExpression accessor) {
                    JExpression newPfx = (JExpression)prefix.accept(this);
                    if (newPfx != null && newPfx != prefix) {
                        self.setPrefix(newPfx);
                    }
                    boolean oldInArrayOffset = inArrayOffest[0];
                    inArrayOffest[0] = true;
                    // Recurse so we have something to return.
  
                    // Object orig = super.visitArrayAccessExpression(self,prefix, acc);
                    JExpression newAcc = (JExpression)accessor.accept(this);
                    if (newAcc != null && newAcc != accessor) {
                        self.setAccessor(newAcc);
                    }

                    inArrayOffest[0] = oldInArrayOffset;
                    // Take a harder look at what we have...
                    if (!(prefix instanceof JFieldAccessExpression))
                        return self;
                    JFieldAccessExpression fae = (JFieldAccessExpression) prefix;
                    if (!(fae.getPrefix() instanceof JThisExpression))
                        return self;
                    // Okay, the base is a FAE with this. Yay.
                    
                    // RMR { progagate into the dims since they may have references
                    // to fields which should now be resolved; this is done so that
                    // the field arraytype is fully resolved for later passes
                    JExpression[] dims = ((CArrayType) fae.getType()).getDims();
                    for (int i = 0; i < dims.length; i++) {
                        if (!(dims[i] instanceof JLiteral)) {
                            // eventually dims[i] will be a JLiteral; requires
                            // a few passes of the propagator
                            dims[i] = (JExpression) (dims[i]).accept(this);
                        }
                    }
                    // } RMR
                    
                    // Save its name.
                    String name = fae.getIdent();
                    // Now, is the offset an integer literal?
                    if (!(newAcc instanceof JIntLiteral))
                        return self;
                    // Yay, we win (hopefully).
                    int slot = ((JIntLiteral) newAcc).intValue();
                    if (canArrayPropagate(name, slot)
                        && (inArrayOffest[0]
                            || !inLeftHandSide[0])) {
                        return propagatedArray(name, slot);
                    } else {
                        return self;
                    }
                }
            };
        for (int i = 0; i < meths.length; i++) {
            meths[i].accept(theVisitor);
            // Also run some simple algebraic simplification now.
            Propagator prop = new Propagator(findLocals(meths[i]));
            meths[i].accept(prop); 
            // Raise Variable Declarations to beginning of block
            // meths[i].accept(new VarDeclRaiser());
        }
        // propagate into initializers and types of field declarations
        JFieldDeclaration[] fields = filter.getFields();
        Propagator prop = new Propagator(new Hashtable());
        for (int i = 0; i < fields.length; i++) {
            // the field type (propagate into static array bounds)
            CType type = fields[i].getType();
            if (type.isArrayType()) {
                JExpression[] dims = ((CArrayType)type).getDims();
                for (int j=0; j<dims.length; j++) {
                    dims[j] = (JExpression)dims[j].accept(theVisitor);
                    dims[j] = (JExpression)dims[j].accept(prop);
                }
            }
            // initializer
            JVariableDefinition var = fields[i].getVariable();
            if (var.hasInitializer()) {
                JExpression origInit = var.getValue();
                JExpression newInit = (JExpression) origInit
                    .accept(theVisitor);
                newInit = (JExpression) newInit.accept(prop);
                if (newInit != origInit) {
                    var.setValue(newInit);
                }
            }
        }
        // If this is a filter, also run on I/O rates and other field
        // initializers
        if (filter instanceof SIRPhasedFilter) {
            SIRPhasedFilter filt = (SIRPhasedFilter) filter;

            for (int j = 0; j < filter.getMethods().length; j++) {
                JMethodDeclaration method = filter.getMethods()[j];

                // pop
                JExpression newPop = (JExpression) method.getPop().accept(theVisitor);
                newPop = (JExpression) newPop.accept(prop);
                if (newPop != null && newPop != method.getPop()) {
                    method.setPop(newPop);
                }
                // peek
                JExpression newPeek = (JExpression) method.getPeek().accept(theVisitor);
                newPeek = (JExpression) newPeek.accept(prop);
                if (newPeek != null && newPeek != method.getPeek()) {
                    method.setPeek(newPeek);
                }
                // push
                JExpression newPush = (JExpression) method.getPush().accept(theVisitor);
                newPush = (JExpression) newPush.accept(prop);
                if (newPush != null && newPush != method.getPush()) {
                    method.setPush(newPush);
                }
            }
        }
    }

    
    /**
     * Find local scalar variables of method with a single literal
     * assignment.  (Fed into Propagate in doPropagation above.)
     * 
     * @param meth
     * @return
     */
    private Hashtable findLocals(JMethodDeclaration meth)
    {
        final Hashtable yes = new Hashtable();
        
        // This looks a lot like the thing we had for fields, except
        // that it looks for local variables.
        meth.accept(new SLIREmptyVisitor() {
                private HashSet<JLocalVariable> no = new HashSet<JLocalVariable>();
                
                public void visitAssignmentExpression
                    (JAssignmentExpression self,
                     JExpression left,
                     JExpression right)
                {
                    super.visitAssignmentExpression(self, left, right);
                    if (left instanceof JLocalVariableExpression)
                        {
                            JLocalVariableExpression lve =
                                (JLocalVariableExpression)left;
                            JLocalVariable lv = lve.getVariable();
                            if (no.contains(lv)) return;
                            if (yes.containsKey(lv))
                                {
                                    yes.remove(lv);
                                    no.add(lv);
                                    return;
                                }
                            if (right instanceof JLiteral)
                                yes.put(lv, right);
                            else
                                no.add(lv);
                        }
                }
                public void visitCompoundAssignmentExpression
                    (JCompoundAssignmentExpression self,
                     int oper,
                     JExpression left,
                     JExpression right)
                {
                    super.visitCompoundAssignmentExpression
                        (self, oper, left, right);
                    // Instant death.
                    if (left instanceof JLocalVariableExpression)
                        {
                            JLocalVariable lv =
                                ((JLocalVariableExpression)left).getVariable();
                            yes.remove(lv);
                            no.add(lv);
                        }
                }
                public void visitPrefixExpression
                    (JPrefixExpression self,
                     int oper,
                     JExpression expr)
                {
                    super.visitPrefixExpression(self, oper, expr);
                    // Again, instant death.
                    if (expr instanceof JLocalVariableExpression)
                        {
                            JLocalVariable lv =
                                ((JLocalVariableExpression)expr).getVariable();
                            yes.remove(lv);
                            no.add(lv);
                        }
                }
                public void visitPostfixExpression
                    (JPostfixExpression self,
                     int oper,
                     JExpression expr)
                {
                    super.visitPostfixExpression(self, oper, expr);
                    // Again, instant death.
                    if (expr instanceof JLocalVariableExpression)
                        {
                            JLocalVariable lv =
                                ((JLocalVariableExpression)expr).getVariable();
                            yes.remove(lv);
                            no.add(lv);
                        }
                }
            });
        return yes;
    }

}

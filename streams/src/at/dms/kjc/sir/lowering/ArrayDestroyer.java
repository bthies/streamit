package at.dms.kjc.sir.lowering;

import java.util.*;

import at.dms.kjc.*;
//import at.dms.util.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.lir.*;
//import at.dms.compiler.JavaStyleComment;
//import at.dms.compiler.JavadocComment;

import java.io.Serializable;
//import java.lang.Math;
//import at.dms.compiler.TokenReference;

/**
 * This class breaks up arrays into local vars as much as possible
 */
public class ArrayDestroyer extends SLIRReplacingVisitor {

    private final HashMap<String, HashMap<Integer, Boolean>> targetsField;
    private final HashSet<String> unsafeField;

    private HashMap<JLocalVariable, JLocalVariable[]> replaced;
    private HashMap<String,String[]> replacedFields;
    
    private boolean deadend;

    /**
     * Constructor.
     * Constructed object can then be accepted by a method to break up local
     * arrays into individual local vars when posible, and to collect information
     * for an eventual call to {@link #destroyFieldArrays(SIRCodeUnit) destroyFieldArrays}. 
     */
    public ArrayDestroyer() {
        targetsField=new HashMap<String, HashMap<Integer, Boolean>>();
        unsafeField= new HashSet<String>();
        replaced=new HashMap<JLocalVariable, JLocalVariable[]>();
        replacedFields = new HashMap<String,String[]>();
        deadend=false;
    }

     /**
     * Destructure all destructurable local arrays in stream graph.
     * Also destructure (destroy) field arrays if switch is set.
     * @param str
     */
    public void destroyArrays(SIRStream str) {
        if (str instanceof SIRFeedbackLoop)
            {
                SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
                destroyArrays(fl.getBody());
                destroyArrays(fl.getLoop());
            }
        if (str instanceof SIRPipeline)
            {
                SIRPipeline pl = (SIRPipeline)str;
                Iterator iter = pl.getChildren().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = (SIRStream)iter.next();
                        destroyArrays(child);
                    }
            }
        if (str instanceof SIRSplitJoin)
            {
                SIRSplitJoin sj = (SIRSplitJoin)str;
                Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
                while (iter.hasNext())
                    {
                        SIRStream child = iter.next();
                        destroyArrays(child);
                    }
            }
        if (str instanceof SIRFilter) {
            for (int i = 0; i < str.getMethods().length; i++) {
                //varDefs=new HashMap();
                str.getMethods()[i].accept(this);
            }
            if(KjcOptions.destroyfieldarray)
                destroyFieldArrays((SIRFilter)str);
        }
    }
    
    public Object visitVariableDefinition(JVariableDefinition self,
            int modifiers,
            CType type,
            String ident,
            JExpression expr) {
        Object retval = super.visitVariableDefinition(self,modifiers,type,ident,expr);
        if (type.isArrayType() && ((CArrayType)type).getDims().length != 1) {
            unsafeField.add(ident);
        }
        return retval;
    }
    
    
    public Object visitMethodDeclaration(JMethodDeclaration self,
                                         int modifiers,
                                         CType returnType,
                                         String ident,
                                         JFormalParameter[] parameters,
                                         CClassType[] exceptions,
                                         JBlock body) {
        replaced.clear();
        final boolean init=ident.startsWith("init");
        // A.D.  WTF: could detect that parameters are arrays and eliminate them from
        // consideration but not currently done...
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
            }
        }
        if (body != null) {
            // XXX: seems to reuse targets with multiple key types: Integer, JLocalVariable, JExpression...
            final Map<JLocalVariable, HashMap<Integer, Boolean>> targets=new HashMap<JLocalVariable, HashMap<Integer, Boolean>>();
            final Set<String> unsafe=new HashSet<String>();
            body.accept(new SLIRReplacingVisitor() {
                    HashMap<JLocalVariable, Boolean> declared=new HashMap<JLocalVariable, Boolean>();

                    /**
                     * Eliminate all multidimensional arrays from consideration since
                     * we can not deal with them.
                     */
                    public Object visitVariableDefinition(JVariableDefinition self,
                            int modifiers,
                            CType type,
                            String ident,
                            JExpression expr) {
                        Object retval = super.visitVariableDefinition(self,modifiers,type,ident,expr);
                        if (type.isArrayType() && ((CArrayType)type).getDims().length != 1) {
                            unsafe.add(ident);
                        }
                        return retval;
                    }

                    /**
                     * If vars used in any way except in array access then remove from targets
                     */
                    public Object visitLocalVariableExpression(JLocalVariableExpression self2,
                                                               String ident2) {
                        targets.remove(((JLocalVariableExpression)self2).getVariable());
                        unsafe.add(((JLocalVariableExpression)self2).getVariable().getIdent());
                        return self2;
                    }

                    /**
                     * If fields used in any way except in array access then remove from targets.
                     * Use of fields in array access checked below in visitArrayAccessExpression.
                     */
                    public Object visitFieldExpression(JFieldAccessExpression self,
                                                       JExpression left,
                                                       String ident) {
                        if(!init) {
                            targetsField.remove(self.getIdent());
                            unsafe.add(self.getIdent());
                            return self;
                        }
                        return self;
                    }
           
                    public Object visitAssignmentExpression(JAssignmentExpression self,
                                                            JExpression left,
                                                            JExpression right) {
                        if((left instanceof JLocalVariableExpression)&&(right instanceof JNewArrayExpression)) {
                            JLocalVariable var=((JLocalVariableExpression)left).getVariable();
                            if(!declared.containsKey(var)) {
                                declared.put(var,Boolean.TRUE);
                                return self;
                            }
                        } else if (left instanceof JFieldAccessExpression) {
                            // assigning one array to another.  We do
                            // not yet track the flow of values
                            // through an assignment such as this.
                            //
                            // This means that if you pass an array of
                            // coefficients to a filter and store it
                            // to a field, the array cannot be
                            // destroyed.  To support this, would need
                            // to propagate constants from locals to
                            // fields, which I don't think FieldProp
                            // or Propagator currently dues. 
                            // SEE RT #257.  --BFT
                            JFieldAccessExpression l = (JFieldAccessExpression)left;
                            targetsField.remove(l.getIdent());
                            unsafeField.add(l.getIdent());
                        }
                        return super.visitAssignmentExpression(self,left,right);
                    }

                    public Object visitMethodCallExpression(JMethodCallExpression self,
                                                            JExpression prefix,
                                                            String ident,
                                                            JExpression[] args) {
                        if(!(KjcOptions.removeglobals && ident.equals("memset"))
                           ||ident.equals("sizeof")) {
                            if (prefix != null) {
                                JExpression newExp = (JExpression)prefix.accept(this);
                                if (newExp!=null && newExp!=prefix) {
                                    self.setPrefix(newExp);
                                }
                            }
                            visitArgs(args);
                        }
                        return self;
                    }

                    /**
                     * Considers target
                     */
                    public Object visitArrayAccessExpression(JArrayAccessExpression self2,
                                                             JExpression prefix,
                                                             JExpression accessor) {
                        if(!deadend) {
                            accessor.accept(this);
                            // if local[int] (but not argument[int]) and not unsafe, add to targets
                            if(prefix instanceof JLocalVariableExpression) {
                                JLocalVariable var=((JLocalVariableExpression)prefix).getVariable();
                                if(!(unsafe.contains(var.getIdent()) || (var instanceof JFormalParameter)))
                                    if(accessor instanceof JIntLiteral) {
                                        HashMap<Integer, Boolean> map=targets.get(var);
                                        if(map==null) {
                                            map=new HashMap<Integer, Boolean>();
                                            targets.put(var,map);
                                        }
                                        map.put(new Integer(((JIntLiteral)accessor).intValue()),Boolean.TRUE);
                                    } else {
                                        targets.remove(var);
                                        unsafe.add(var.getIdent());
                                    }
                                // if not processing init() and not unsafe field, add this.name[int] to targetsField
                            } else if(!init&&(prefix instanceof JFieldAccessExpression)&&(((JFieldAccessExpression)prefix).getPrefix() instanceof JThisExpression)) {
                                String ident=((JFieldAccessExpression)prefix).getIdent();
                                if(!unsafeField.contains(ident))
                                    if(accessor instanceof JIntLiteral) {
                                        HashMap<Integer, Boolean> map=targetsField.get(ident);
                                        if(map==null) {
                                            map=new HashMap<Integer, Boolean>();
                                            targetsField.put(ident,map);
                                        }
                                        map.put(new Integer(((JIntLiteral)accessor).intValue()),Boolean.TRUE);
                                    } else {
                                        unsafeField.add(ident);
                                    }
                            } else {
                                deadend=true;
                                prefix.accept(this);
                                deadend=false;
                            }
                        } else /* deadend == true */ {
                            prefix.accept(this);
                            accessor.accept(this);
                        }
                        return self2;
                    }
                });

            Set<JLocalVariable> keySet=targets.keySet();
            JLocalVariable[] vars=keySet.toArray(new JLocalVariable[keySet.size()]);
            keySet.toArray(vars);
            for(int i=0;i<vars.length;i++) {
                JLocalVariable var=vars[i];
                Set<Integer> varKeySet = targets.get(var).keySet();
                Integer[] ints = varKeySet.toArray(new Integer[varKeySet.size()]);
                int top=0;
                for(int j=0;j<ints.length;j++) {
                    int newInt=ints[j].intValue();
                    if(newInt>top)
                        top=newInt;
                }
                JLocalVariable[] newVars=new JLocalVariable[top+1];
                for(int j=0;j<ints.length;j++) {
                    int newInt=ints[j].intValue();
                    JVariableDefinition varDef=toVar(var,newInt);
                    body.addStatementFirst(new JVariableDeclarationStatement(null,varDef,null));
                    newVars[newInt]=varDef;
                }
                replaced.put(var,newVars);
            }
            body.accept(this);
        }
        return self;
    }
    
    /**
     * Remove constant 1-dimensional arrays in fields.
     * <i>After having all methods in a SIRCodeUnit accept this ArrayDestroyer</i>, you
     * can invoke this method to replace constant fields of array type in this SIRCodeUnit
     * with scalars.
     * 
     * @param unit  the SIRCodeUnit (something with fields and methods) to work on.
     */
    
    public void destroyFieldArrays(SIRCodeUnit unit) {
        replacedFields.clear();  // no replacements yet
        // loop over all array names from targetsField map.
        for(String name : targetsField.keySet()) {
            // if found by any method to be unsafe to replace, then ignore.
            if (unsafeField.contains(name)) { continue; }
            
            // array variable definition
            JVariableDefinition var = getDefn(name, unit.getFields());
            CType arrayType = var.getType();
            if(arrayType==null) { continue; }
            assert arrayType instanceof CArrayType;
            // type of elements of array
            CType baseType = ((CArrayType)arrayType).getBaseType();
            JExpression init = var.getValue();
            // array initialializer for each element, or null array.
            JExpression[] elems;
            if (init == null) {
                elems = null; // default, but make it explicit
            } else if (init instanceof JArrayInitializer) {
                elems = ((JArrayInitializer)init).getElems();
            } else {
                throw new AssertionError("unexpected array field initialization " + init.getClass());
            }
            
            // set of array offsets guaranteed to be constant
            // (some code above makes me think that his had better be all
            // the offsets or else we will find more bugs)
            Set<Integer> keySet = targetsField.get(name).keySet();
            int top = Collections.max(keySet);
// assertion in next line not true for tde.str where only offsets 0, 4, 8, .. of bitr are used.
//            assert top + 1 == keySet.size();
            // will be filled with names of fields guaranteed to be constant
            String[] newFields=new String[top+1];

            // create new field names for all constant fields
            for (int j : keySet) {
                JFieldDeclaration newField = toField(name,j,baseType,elems);
                unit.addField(newField);
                newFields[j] = newField.getVariable().getIdent();
            }
            replacedFields.put(name,newFields);
        }
        
        // Iterate over all methods, updating references to constant fields.
        // (Using visitArrayReference below.)
        for(JMethodDeclaration method : unit.getMethods()) {
            method.getBody().accept(this);
        }
    }

    /*
     * return type of field with ident "name" in "fields".
     */
    private JVariableDefinition getDefn(String name,JFieldDeclaration[] fields) {
        for(int i=0;i<fields.length;i++) {
            JVariableDefinition var=fields[i].getVariable();
            if(var.getIdent().equals(name))
                return var;
        }
        return null;
    }

    /**
     * Create a variable definition or an element of a destroyed local array.
     * @param var
     * @param idx
     * @return
     */
    private JVariableDefinition toVar(JLocalVariable var,int idx) {
        JExpression init = null;
        // extract array initializer expression at given index.  Right
        // now this is only designed to work when the initializer is a
        // constant.
        if (var instanceof JVariableDefinition && ((JVariableDefinition)var).getValue() instanceof JArrayInitializer) { 
            JArrayInitializer arr = (JArrayInitializer)((JVariableDefinition)var).getValue();
            init = arr.getElems()[idx];
        }
        return new JVariableDefinition(null,0,((CArrayType)var.getType()).getBaseType(),var.getIdent()+"__destroyed_"+idx,init);
    }

    /**
     * Create a variable definition and field declaration for an element of a destroyed field array.
     * @param name        array name used to construct field name
     * @param idx         offset in array
     * @param type        base type of array
     * @param init_vals   array of Jexpressions for initial vals, or null
     * @return
     */
    private JFieldDeclaration toField(String name,int idx,CType type, JExpression[] init_vals) {
        return new JFieldDeclaration(null,
                new JVariableDefinition(null,0,
                        type,
                        name+"__destroyed_"+idx,
                        init_vals == null? null : init_vals[idx]),  null,null);
    }

    /*
     * This depends critically on timing: will  replaced  / replacedFields be stable by time array access
     * is reached?
     * 
     * Relies on replaced being set up in visitmethodDeclaration.  Recursion into method not done
     * until after visitmethodDeclaration, so OK for local arrays.
     * 
     * replacedFields set up in destroyFieldArrays, and then every method required to accetp(this) 
     * which will cause replacement to happen at correct time.
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
                                             JExpression prefix,
                                             JExpression accessor) {
        JExpression newExp=(JExpression)accessor.accept(this);
        if (newExp!=null && newExp!=accessor) {
            self.setAccessor(newExp);
        }
        if((prefix instanceof JLocalVariableExpression)&&(accessor instanceof JIntLiteral)) {
            JLocalVariable[] varArray=(JLocalVariable[])replaced.get(((JLocalVariableExpression)prefix).getVariable());
            if(varArray!=null)
                return new JLocalVariableExpression(null,varArray[((JIntLiteral)accessor).intValue()]);
        }
        if((prefix instanceof JFieldAccessExpression)&&(((JFieldAccessExpression)prefix).getPrefix() instanceof JThisExpression)&&(accessor instanceof JIntLiteral)) {
            String[] fieldArray=(String[])replacedFields.get(((JFieldAccessExpression)prefix).getIdent());
            if(fieldArray!=null)
                return new JFieldAccessExpression(null,((JFieldAccessExpression)prefix).getPrefix(),fieldArray[((JIntLiteral)accessor).intValue()],null);
        }
        return self;
    }
}

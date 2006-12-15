package at.dms.kjc.sir.lowering;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

import java.io.Serializable;
import java.lang.Math;
import at.dms.compiler.TokenReference;

/**
 * This class breaks up arrays into local vars as much as possible
 */
public class ArrayDestroyer extends SLIRReplacingVisitor {
    // XXX: seems to use with several key types
    private HashMap<Object, HashMap<Integer, Boolean>> targets;
    // XXX: value seems to be several different HashMap types.
    private final HashMap<String, HashMap<Integer, Boolean>> targetsField;
    private HashMap<Serializable, Serializable[]> replaced;
    //private HashMap varDefs;
    private boolean deadend;

    public ArrayDestroyer() {
        targetsField=new HashMap<String, HashMap<Integer, Boolean>>();
        replaced=new HashMap<Serializable, Serializable[]>();
        deadend=false;
    }

    // returns all local variables created by the array destroyer

    public void addDestroyedLocals(Set<JLocalVariable> set) {
        Set<Serializable> key_set = replaced.keySet();
        Iterator<Serializable> iter = key_set.iterator();
    
        while (iter.hasNext()) {
            Object key = iter.next();
            if (key instanceof JLocalVariable) {
                JLocalVariable vars[] = (JLocalVariable[])replaced.get(key);
                for (int i = 0; i < vars.length; i++) {
                    set.add(vars[i]);
                }
            }
        }
    }

    //wraps JLocalVar and index together
    /*public class VarIndex {
      public JLocalVariable var;
      public int index;
    
      public VarIndex(JLocalVariable var,int index) {
      this.var=var;
      this.index=index;
      }

      public boolean equals(Object o) {
      return (o instanceof VarIndex)&&(var.equals(((VarIndex)o).var))&&(index==((VarIndex)o).index);
      }

      public int hashCode() {
      return var.hashCode()+4999*index;
      }
      }*/

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
            final HashMap<Object, HashMap<Integer, Boolean>> targets=new HashMap<Object, HashMap<Integer, Boolean>>();
            final HashMap<Serializable, Boolean> unsafe=new HashMap<Serializable, Boolean>();
            body.accept(new SLIRReplacingVisitor() {
                    HashMap<JLocalVariable, Boolean> declared=new HashMap<JLocalVariable, Boolean>();

                    /**
                     * If vars used in any way except in array access then remove from targets
                     */
                    public Object visitLocalVariableExpression(JLocalVariableExpression self2,
                                                               String ident2) {
                        targets.remove(((JLocalVariableExpression)self2).getVariable());
                        unsafe.put(((JLocalVariableExpression)self2).getVariable(),Boolean.TRUE);
                        return self2;
                    }

                    /**
                     * If fields used in any way except in array access then remove from targets
                     */
                    public Object visitFieldExpression(JFieldAccessExpression self,
                                                       JExpression left,
                                                       String ident) {
                        if(!init) {
                            targetsField.remove(self.getIdent());
                            unsafe.put(self.getIdent(),Boolean.TRUE);
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
                            unsafe.put(l.getIdent(), Boolean.TRUE);
                        }
                        return super.visitAssignmentExpression(self,left,right);
                    }

                    public Object visitMethodCallExpression(JMethodCallExpression self,
                                                            JExpression prefix,
                                                            String ident,
                                                            JExpression[] args) {
                        if(!(KjcOptions.removeglobals&&ident.equals("memset"))
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
                            //if((prefix instanceof JLocalVariableExpression)&&(!unsafe.containsKey(((JLocalVariableExpression)prefix).getVariable())))
                            if(prefix instanceof JLocalVariableExpression) {
                                JLocalVariable var=((JLocalVariableExpression)prefix).getVariable();
                                if(!(unsafe.containsKey(var) || (var instanceof JFormalParameter)))
                                    if(accessor instanceof JIntLiteral) {
                                        HashMap<Integer, Boolean> map=targets.get(var);
                                        if(map==null) {
                                            map=new HashMap<Integer, Boolean>();
                                            targets.put(var,map);
                                        }
                                        map.put(new Integer(((JIntLiteral)accessor).intValue()),Boolean.TRUE);
                                    } else {
                                        targets.remove(var);
                                        unsafe.put(var,Boolean.TRUE);
                                    }
                            } else if(!init&&(prefix instanceof JFieldAccessExpression)&&(((JFieldAccessExpression)prefix).getPrefix() instanceof JThisExpression)) {
                                String ident=((JFieldAccessExpression)prefix).getIdent();
                                if(!unsafe.containsKey(ident))
                                    if(accessor instanceof JIntLiteral) {
                                        HashMap<Integer, Boolean> map=targetsField.get(ident);
                                        if(map==null) {
                                            map=new HashMap<Integer, Boolean>();
                                            targetsField.put(ident,map);
                                        }
                                        map.put(new Integer(((JIntLiteral)accessor).intValue()),Boolean.TRUE);
                                    } else {
                                        targetsField.remove(ident);
                                        unsafe.put(ident,Boolean.TRUE);
                                    }
                            } else {
                                deadend=true;
                                prefix.accept(this);
                                deadend=false;
                            }
                        } else {
                            prefix.accept(this);
                            accessor.accept(this);
                        }
                        return self2;
                    }
                });
            this.targets=targets;
            // XXX: can not cast, need to clean up use of targets.
            Set keySet=targets.keySet();
            JLocalVariable[] vars=new JLocalVariable[keySet.size()];
            keySet.toArray(vars);
            for(int i=0;i<vars.length;i++) {
                JLocalVariable var=vars[i];
                keySet=targets.get(var).keySet();
                Integer[] ints=new Integer[keySet.size()];
                keySet.toArray(ints);
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
        replaced.clear();  // no replacements yet
        // loop over all array names from targetsField map.
        for(String name : targetsField.keySet()) {

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
            assert top + 1 == keySet.size();
            // will be filled with names of fields guaranteed to be constant
            String[] newFields=new String[top+1];

            // create new field names for all constant fields
            for (int j : keySet) {
                JFieldDeclaration newField = toField(name,j,baseType,elems);
                unit.addField(newField);
                newFields[j] = newField.getVariable().getIdent();
            }
            replaced.put(name,newFields);
        }
        
        // Iterate over all methods, updating references to constant fields.
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
            String[] fieldArray=(String[])replaced.get(((JFieldAccessExpression)prefix).getIdent());
            if(fieldArray!=null)
                return new JFieldAccessExpression(null,((JFieldAccessExpression)prefix).getPrefix(),fieldArray[((JIntLiteral)accessor).intValue()],null);
        }
        return self;
    }

    /*private JVariableDefinition toVar(JArrayAccessExpression array,JExpression init) {
      return new JVariableDefinition(null,0,array.getType(),array.getPrefix().getIdent()+"__destroyed_"+((JIntLiteral)array.getAccessor()).intValue(),init);
      }*/
    
    /**
     * Visits an assignment expression
     */
    /*public Object visitAssignmentExpression(JAssignmentExpression self,
      JExpression left,
      JExpression right) {
      JExpression newRight=(JExpression)right.accept(this);
      if (newRight!=null && newRight!=right) {
      self.setRight(newRight);
      }
      if(left instanceof JArrayAccessExpression) {
      JExpression prefix=((JArrayAccessExpression)left).getPrefix();
      JExpression accessor=((JArrayAccessExpression)left).getAccessor();
      if((prefix instanceof JLocalVariableExpression)&&(accessor instanceof JIntLiteral)&&(targets.containsKey(((JLocalVariableExpression)prefix).getVariable()))) {
      VarIndex index=new VarIndex(((JLocalVariableExpression)prefix).getVariable(),((JIntLiteral)accessor).intValue());
      JVariableDefinition var=(JVariableDefinition)replaced.get(index);
      if(var==null) {
      var=toVar((JArrayAccessExpression)left,newRight);
      replaced.put(index,var);
      return new JVariableDeclarationStatement(null,var,null);
      } else {
      return new JAssignmentExpression(null,new JLocalVariableExpression(null,var),newRight);
      }
      }
      }
      return self;
      }*/

    
    /**
     * visits expression statement
     * passes on the VarDeclaration on if encountered
     */
    /*public Object visitExpressionStatement(JExpressionStatement self,
      JExpression expr) {
      Object newExp=expr.accept(this);
      if (newExp!=null && newExp!=expr) {
      if(newExp instanceof JVariableDeclarationStatement)
      return newExp;
      else
      self.setExpression((JExpression)newExp);
      }
      return self;
      }*/
    /**
     * visits an array access
     */
    /*public Object visitArrayAccessExpression(JArrayAccessExpression self,
      JExpression prefix,
      JExpression accessor) {
      JExpression newExp = (JExpression)prefix.accept(this);
      if (newExp!=null && newExp!=prefix) {
      self.setPrefix(newExp);
      }
    
      newExp = (JExpression)accessor.accept(this);
      if (newExp!=null && newExp!=accessor) {
      self.setAccessor(newExp);
      }
    
      if((prefix instanceof JLocalVariableExpression)&&(accessor instanceof JIntLiteral)&&replaced.get(new VarIndex(((JLocalVariableExpression)prefix).getVariable(),((JIntLiteral)accessor).intValue()))!=null) {
      return new JLocalVariableExpression(null,(JLocalVariable)replaced.get(new VarIndex(((JLocalVariableExpression)prefix).getVariable(),((JIntLiteral)accessor).intValue())));
      }
      return self;
      }*/
}

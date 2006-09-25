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
    private final HashMap<String, HashMap<Object, Boolean>> targetsField;
    private HashMap<Serializable, Serializable[]> replaced;
    //private HashMap varDefs;
    private boolean deadend;

    public ArrayDestroyer() {
        targetsField=new HashMap<String, HashMap<Object, Boolean>>();
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
                                if(!unsafe.containsKey(var))
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
                                        HashMap<Object, Boolean> map=targetsField.get(ident);
                                        if(map==null) {
                                            map=new HashMap<Object, Boolean>();
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
    
    
    public void destroyFieldArrays(SIRCodeUnit unit) {
        replaced.clear();
        Set<String> keySet=targetsField.keySet();
        String[] names=new String[keySet.size()];
        keySet.toArray(names);
        for(int i=0;i<names.length;i++) {
            String name=names[i];
            //CType type=(CType)((HashMap)targetsField.get(name)).remove(Boolean.TRUE);
            CType type=getType(name, unit.getFields());
            if(type==null)
                break;
            // XXX: Want Set<Integer>, previosly reused keySet.
            Set<Object>tmpSet = targetsField.get(name).keySet();
            Integer[] ints=new Integer[tmpSet.size()];
            tmpSet.toArray(ints);
            int top=0;
            for(int j=0;j<ints.length;j++) {
                int newInt=ints[j].intValue();
                if(newInt>top)
                    top=newInt;
            }
            String[] newFields=new String[top+1];
            for(int j=0;j<ints.length;j++) {
                int newInt=ints[j].intValue();
                JFieldDeclaration newField=toField(name,newInt,type);
                unit.addField(newField);
                newFields[newInt]=newField.getVariable().getIdent();
            }
            replaced.put(name,newFields);
        }
        JMethodDeclaration[] methods = unit.getMethods(); 
        for(int i=0;i<methods.length;i++) {
            JMethodDeclaration method=methods[i];
            //if(!(method.getName().startsWith("work")||method.getName().startsWith("initWork")))
                method.getBody().accept(this);
        }
    }

    private CType getType(String name,JFieldDeclaration[] fields) {
        for(int i=0;i<fields.length;i++) {
            JVariableDefinition var=fields[i].getVariable();
            if(var.getIdent().equals(name))
                return var.getType();
        }
        return null;
    }

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

    private JFieldDeclaration toField(String name,int idx,CType type) {
        return new JFieldDeclaration(null,new JVariableDefinition(null,0,((CArrayType)type).getBaseType(),name+"__destroyed_"+idx,null),null,null);
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

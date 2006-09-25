package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import java.lang.Math;
import at.dms.compiler.TokenReference;

/**
 * This class breaks up structures as much as possible. The goal is to
 * break up structure assignments and modifications into regular
 * assignments on local variable.
 * @author jasperln
 */
public class StructDestroyer extends SLIRReplacingVisitor {
    private static final String SEP="_"; //Separator mark
    private HashMap<String, JLocalVariable> nameToVar; //caches created vars by their name
    private HashMap<String, JFieldDeclaration> newFields; //caches created fields by their name
    private ArrayList<JExpression> leftF; //list of left expression in a series of assignments
    private JExpression rightF; //rightmost expression in an assignment
    private HashMap<String, JFormalParameter> paramMap; //stores parameters indexed by name
    private JExpression unsafeRhs; //stores last structure used unsafely (need to pack fields beforehand)

    /**
     * Construct new StructDestroyer. Initializes state.
     */
    public StructDestroyer() {
        nameToVar=new HashMap<String, JLocalVariable>();
        newFields=new HashMap<String, JFieldDeclaration>();
        leftF=new ArrayList<JExpression>();
        paramMap=new HashMap<String, JFormalParameter>();
    }

    /**
     * Call after visiting all methods. Adds the needed fields.
     * @param str The SIRStream containing the methods.
     */
    public void addFields(SIRStream str) {
        JFieldDeclaration[] fields=str.getFields();
        JFieldDeclaration[] newField=new JFieldDeclaration[fields.length+newFields.size()];
        System.arraycopy(fields,0,newField,0,fields.length);
        Object[] newF=newFields.values().toArray();
        for(int i=0,j=fields.length;i<newF.length;i++,j++)
            newField[j]=(JFieldDeclaration)newF[i];
        str.setFields(newField);
    }
    
    /**
     * Visit field expression. Only needs to worry about visiting
     * struct assignments since visitBlockStatement only descends into
     * those.
     * @param self Visiting this JFieldAccessExpression.
     * @param left Left hand side JExpression.
     * @param ident Identifier of field access.
     */
    public Object visitFieldExpression(JFieldAccessExpression self,
                                       JExpression left,
                                       String ident) {
        JExpression newLeft=(JExpression)left.accept(this);
        if(newLeft instanceof JThisExpression)
            return self;
        if(newLeft instanceof JLocalVariableExpression) //Break up local struct
            return new JLocalVariableExpression(null,getVar(((JLocalVariableExpression)newLeft).getVariable().getIdent()+SEP+ident,self.getType()));
        if(newLeft instanceof JFieldAccessExpression) { //Break up field struct
            JFieldAccessExpression field=(JFieldAccessExpression)newLeft;
            String name=field.getIdent()+SEP+ident;
            addField(name,self.getType());
            JFieldAccessExpression out=new JFieldAccessExpression(null,field.getPrefix(),name);
            if(out.getType() instanceof CClassNameType)
                unsafeRhs=out;
            return out;
        }
        if(self.getType() instanceof CClassNameType) //unsafe access
            unsafeRhs=self;
        return self;
    }

    /**
     * Visit local variable expression. Only appropriate for variable
     * expressions on the right hand side. When called via
     * visitAssignmentExpression() this is taken care of.
     * @param self Visiting this JLocalVariableExpression.
     * @param ident Identifier of variable access.
     */
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                               String ident) {
        JExpression out=(JExpression)super.visitLocalVariableExpression(self,ident);
        if(out.getType() instanceof CClassNameType) //unsafe access
	    /*visitAssignmentExpression() takes care of saving and
	      restoring the old unsafeRhs when not visiting right hand
	      side so ok to always overwrite.*/
            unsafeRhs=out;
        return out;
    }
    
    /**
     * Visit assignment expression. Makes sure to only change right
     * hand state when descending into right hand side
     * @param self Visiting this JAssignmentExpression.
     * @param left Left hand side JExpression.
     * @param right Right hand side JExpression.
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                            JExpression left,
                                            JExpression right) {
        JExpression savedUnsafeRhs=unsafeRhs;
        super.visitAssignmentExpression(self,left,right);
        JExpression newLeft=(JExpression)left.accept(this);
        if(newLeft!=null&&newLeft!=left) {
            self.setLeft(newLeft);
            left=newLeft;
        }
        unsafeRhs=savedUnsafeRhs; //Dont count leftside as unsafeRhs
        JExpression newRight=(JExpression)right.accept(this);
        if(newRight!=null&&newRight!=right) {
            self.setRight(newRight);
            right=newRight;
        }
        if(right.getType() instanceof CClassNameType) //Right side was actually safe
            unsafeRhs=savedUnsafeRhs;
        if(left.getType() instanceof CClassNameType) { //Fill leftF and rightF
            if(right.getType() instanceof CClassNameType) {
                leftF.add(left);
                if(rightF==null)
                    rightF=right;
                if((right instanceof JLocalVariableExpression)&& //Skip over old parameter
                   (((JLocalVariableExpression)right).getVariable() instanceof JFormalParameter)&&
                   (left instanceof JFieldAccessExpression))
                    return left;
                if(right instanceof JAssignmentExpression)
                    return right;
            }
        }
        return self;
    }

    /**
     * Visit cast expression. Try to pull out expression from cast
     * expression changed to the right type.
     * @param self Visiting this JCastExpression.
     * @param expr Expression being cast.
     * @param type Type being cast to.
     */
    public Object visitCastExpression(JCastExpression self,
                                      JExpression expr,
                                      CType type) {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }
        if(expr.getType().equals(type)) //Correct type so pass through
            return expr;
	//Wrong type so set the type instead
        if((expr.getType() instanceof CClassNameType)&&(type instanceof CClassNameType))
            if(((CClassNameType)expr.getType()).getQualifiedName().equals("java/lang/Object"))
                if(expr instanceof JLocalVariableExpression) {
                    ((JLocalVariableExpression)expr).getVariable().setType(type);
                    return expr;
                }
        return self;
    }

    /**
     * Visit block. Only interested in struct=struct,
     * struct=something, and struct construction statements. The rest
     * are mostly ignored.
     * @param self Visiting this JBlock.
     * @param comments Comments attached to this block.
     */
    public Object visitBlockStatement(JBlock self,
                                      JavaStyleComment[] comments) {
        List statements=self.getStatements();
        for(int i=0;i<statements.size();i++) {
            leftF.clear(); //Reset things that should be clear before visiting each statement
            rightF=null;
            unsafeRhs=null;
            JStatement cur=(JStatement)statements.get(i);
            Object newCur=cur.accept(this);
            if(!(newCur instanceof JStatement))
                continue;
            if(newCur!=null&&newCur!=cur)
                statements.set(i,newCur);
            if(unsafeRhs!=null) { //Pack struct if unsafe
                List[] temp=extractFields(((CClassNameType)unsafeRhs.getType()).getCClass());
                List f=temp[0];
                List t=temp[1];
                final int len=f.size();
                for(int j=0;j<len;j++) {
                    String field=(String)f.get(j);
                    CType type=(CType)t.get(j);
                    JExpression l=new JFieldAccessExpression(null,unsafeRhs,field);
                    JExpression r=null;
                    if(unsafeRhs instanceof JFieldAccessExpression) {
                        JExpression prefix=((JFieldAccessExpression)unsafeRhs).getPrefix();
                        String ident=((JFieldAccessExpression)unsafeRhs).getIdent();
                        String name=ident+SEP+field;
                        addField(name,type);
                        r=new JFieldAccessExpression(null,prefix,name);
                    } else if(unsafeRhs instanceof JLocalVariableExpression)
                        r=new JLocalVariableExpression(null,getVar(((JLocalVariableExpression)unsafeRhs).getIdent()+SEP+field,type));
                    statements.add(i,new JExpressionStatement(null,new JAssignmentExpression(null,l,r),null));
                    i++;
                }
            }
            if(leftF.size()>0) { //Deal with struct=struct or struct=something
                List[] temp=extractFields(((CClassNameType)rightF.getType()).getCClass());
                List f=temp[0];
                List t=temp[1];
                final int len=f.size();
                if(!((rightF instanceof JFieldAccessExpression)||(rightF instanceof JLocalVariableExpression))) //Deal with struct=something (pop)
                    for(int j=0;j<len;j++) {
                        String field=(String)f.get(j);
                        CType type=(CType)t.get(j);
                        JExpression left=leftF.get(0);
                        JExpression r=new JFieldAccessExpression(null,left,field);
                        JExpression l=null;
                        if(left instanceof JFieldAccessExpression) {
                            JExpression prefix=((JFieldAccessExpression)left).getPrefix();
                            String ident=((JFieldAccessExpression)left).getIdent();
                            String name=ident+SEP+field;
                            addField(name,type);
                            l=new JFieldAccessExpression(null,prefix,name);
                        } else if(left instanceof JLocalVariableExpression)
                            l=new JLocalVariableExpression(null,getVar(((JLocalVariableExpression)left).getIdent()+SEP+field,type));
                        i++;
                        statements.add(i,new JExpressionStatement(null,new JAssignmentExpression(null,l,r),null));
                    }
                for(int j=0;j<len;j++) { //struct=struct case
                    String field=(String)f.get(j);
                    CType type=(CType)t.get(j);
                    JExpression r=null;
                    if(rightF instanceof JFieldAccessExpression) {
                        JExpression prefix=((JFieldAccessExpression)rightF).getPrefix();
                        String ident=((JFieldAccessExpression)rightF).getIdent();
                        String name=ident+SEP+field;
                        addField(name,type);
                        r=new JFieldAccessExpression(null,prefix,name);
                    } else if(rightF instanceof JLocalVariableExpression)
                        r=new JLocalVariableExpression(null,getVar(((JLocalVariableExpression)rightF).getIdent()+SEP+field,type));
                    final int len2=leftF.size();
                    for(int k=0;k<len2;k++) {
                        JExpression left=leftF.get(k);
                        JExpression l=null;
                        if(left instanceof JFieldAccessExpression) {
                            JExpression prefix=((JFieldAccessExpression)left).getPrefix();
                            String ident=((JFieldAccessExpression)left).getIdent();
                            String name=ident+SEP+field;
                            addField(name,type);
                            l=new JFieldAccessExpression(null,prefix,name);
                        } else if(left instanceof JLocalVariableExpression)
                            l=new JLocalVariableExpression(null,getVar(((JLocalVariableExpression)left).getIdent()+SEP+field,type));
                        if(l!=null&&r!=null) {
                            i++;
                            statements.add(i,new JExpressionStatement(null,new JAssignmentExpression(null,l,r),null));
                        } else
                            System.err.println("WARNING: Cannot break appart structure assignment "+left+" "+rightF);
                    }
                }
            }
        }
        return self;
    }

    /**
     * Visit Init Statements. Tries to break up structure parameters
     * into regular arguments.
     * @param self The SIRInitStatement being visited.
     * @param target The expanded SIRStream init statement refers to.
     */
    public Object visitInitStatement(SIRInitStatement self,
                                     SIRStream target) {
        List args=self.getArgs();
        if(target instanceof SIRRecursiveStub) {
            target=((SIRRecursiveStub)target).expand();
            self.setTarget(target);
        }
        JMethodDeclaration init=target.getInit();
        if(init!=null) { //Ignore Builtin types
            JFormalParameter[] params=init.getParameters();
            final int len=args.size();
            ArrayList newArgs=new ArrayList();
            ArrayList<JFormalParameter> newParams=new ArrayList<JFormalParameter>();
            List types=null;
            for(int i=0;i<len;i++) {
                JExpression arg=(JExpression)args.get(i);
                JFormalParameter param=params[i];
                CType paramType=param.getType();
                String paramName=param.getIdent();
                boolean paramFinal=param.isFinal();
                if(arg.getType() instanceof CClassNameType) { //Struct arg
                    List[] temp=extractFields(((CClassNameType)arg.getType()).getCClass()); //Break into components
                    List fields=temp[0];
                    types=temp[1];
                    final int len2=fields.size();
                    if(arg instanceof JFieldAccessExpression) {
                        JExpression prefix=((JFieldAccessExpression)arg).getPrefix();
                        String ident=((JFieldAccessExpression)arg).getIdent();
                        for(int j=0;j<len2;j++) {
                            newArgs.add(new JFieldAccessExpression(null,prefix,ident+SEP+(String)fields.get(j)));
                            newParams.add(new JFormalParameter(null,0,paramType,paramName+SEP+(String)fields.get(j),paramFinal));
                        }
                    } else if(arg instanceof JLocalVariableExpression) {
                        for(int j=0;j<len2;j++) {
                            newArgs.add(new JLocalVariableExpression(null,getVar(((JLocalVariableExpression)arg).getIdent()+SEP+(String)fields.get(j),(CType)types.get(j))));
                            newParams.add(new JFormalParameter(null,0,paramType,paramName+SEP+(String)fields.get(j),paramFinal));
                        }
                    } else
                        System.err.println("WARNING: Cannot break argument structure "+arg);
                } else { //Nonstruct arg
                    newArgs.add(arg);
                    newParams.add(param);
                }
        
            }
            if(newArgs.size()!=args.size()) { //Set new args and params
                self.setArgs(newArgs);
                JFormalParameter[] temp=new JFormalParameter[newParams.size()];
                newParams.toArray(temp);
                init.setParameters(temp);
            }
        }
        return self;
    }
    
    /**
     * Helper function that creates a fresh new var or retreives one
     * from the nameToVar cache.
     * @param name Desired name.
     * @param type Desired type.
     */
    private JLocalVariable getVar(String name,CType type) {
        if(paramMap.containsKey(name))
            return paramMap.get(name);
        JLocalVariable out=nameToVar.get(name);
        if(out==null) {
            out=new JVariableDefinition(null,0,type,name,null);
            nameToVar.put(name,out);
        }
        return out;
    }

    /**
     * Helper function to add a field to the list that will later be
     * added all at once by addFields(SIRStream) when done visting all
     * methods.
     * @param name Desired name.
     * @param type Desired type.
     */
    private void addField(String name,CType type) {
        JFieldDeclaration decl=new JFieldDeclaration(null,new JVariableDefinition(null,0,type,name,null),null,null);
        if(!newFields.containsKey(name)) {
            newFields.put(name,decl);
        }
    }

    /**
     * Recursive extracts component's structures and
     * substructures. Returns array [List name,List type] containing
     * names and types in Lists.
     * @param struct The struct to attempt to extract fields from.
     */
    private static List[] extractFields(CClass struct) {
        CField[] fields=struct.getFields();
        List<String> out=new ArrayList<String>();
        List<CType> out2=new ArrayList<CType>();
        for(int i=0;i<fields.length;i++) {
            if(!(fields[i].getType() instanceof CClassNameType)|| //Nonstruct or struct of same type (avoid self recursion)
               ((CClassNameType)fields[i].getType()).getCClass()==struct) {
                out.add(fields[i].getIdent());
                out2.add(fields[i].getType());
            } else {
                String prefix=fields[i].getIdent(); //Recursively add components of substruct
                List[] temp=extractFields(((CClassNameType)fields[i].getType()).getCClass());
                List inner=temp[0];
                final int len=inner.size();
                for(int j=0;j<len;j++) {
                    out.add(prefix+SEP+(String)inner.get(j));
                    out2.add((CType)temp[1].get(j));
                }
            }
        }
        return new List[]{out,out2}; //Return list of names and types
    }

    /**
     * Visit method declaration. Same as normal except when done be
     * sure to add declarations for the temporary variables created by
     * calling getVar(String name,CType type).
     */
    public Object visitMethodDeclaration(JMethodDeclaration self,
                                         int modifiers,
                                         CType returnType,
                                         String ident,
                                         JFormalParameter[] parameters,
                                         CClassType[] exceptions,
                                         JBlock body) {
        nameToVar.clear();
        paramMap.clear();
        for(int i=0;i<parameters.length;i++)
            paramMap.put(parameters[i].getIdent(),parameters[i]);
        super.visitMethodDeclaration(self,modifiers,returnType,ident,parameters,exceptions,body);
        if(nameToVar.size()>0) {
            Object[] vars=nameToVar.values().toArray();
            for(int i=vars.length-1;i>=0;i--)
                body.addStatementFirst(new JVariableDeclarationStatement(null,(JVariableDefinition)vars[i],null));
        }
        return self;
    }
}

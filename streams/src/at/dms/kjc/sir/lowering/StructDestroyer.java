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
 * This class breaks up structures as much as possible
 */
public class StructDestroyer extends SLIRReplacingVisitor {
    private static final String SEP="_"; //Separator mark
    private HashMap nameToVar; //caches created vars by their name
    private HashMap newFields; //caches created fields by their name
    private ArrayList leftF; //list of left expression in a series of assignments
    private JExpression rightF; //rightmost expression in an assignment
    private HashMap paramMap; //stores parametes indexed by name
    private JExpression unsafeRhs; //stores last structure used unsafely (need to pack fields beforehand)

    public StructDestroyer() {
	nameToVar=new HashMap();
	newFields=new HashMap();
	leftF=new ArrayList();
	paramMap=new HashMap();
    }

    //Call after visiting all methods. Adds the needed fields
    public void addFields(SIRStream str) {
	JFieldDeclaration[] fields=str.getFields();
	JFieldDeclaration[] newField=new JFieldDeclaration[fields.length+newFields.size()];
	System.arraycopy(fields,0,newField,0,fields.length);
	Object[] newF=newFields.values().toArray();
	for(int i=0,j=fields.length;i<newF.length;i++,j++)
	    newField[j]=(JFieldDeclaration)newF[i];
	str.setFields(newField);
    }
    
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

    public Object visitLocalVariableExpression(JLocalVariableExpression self,
					       String ident) {
	JExpression out=(JExpression)super.visitLocalVariableExpression(self,ident);
	if(out.getType() instanceof CClassNameType) //unsafe access
	    unsafeRhs=out;
	return out;
    }
    
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

    //Try to pull out expr from cast with right type
    public Object visitCastExpression(JCastExpression self,
				      JExpression expr,
				      CType type) {
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp!=null && newExp!=expr) {
	    self.setExpr(newExp);
	}
	if(expr.getType().equals(type))
	    return expr;
	if((expr.getType() instanceof CClassNameType)&&(type instanceof CClassNameType))
	    if(((CClassNameType)expr.getType()).getQualifiedName().equals("java/lang/Object"))
		if(expr instanceof JLocalVariableExpression) {
		    ((JLocalVariableExpression)expr).getVariable().setType(type);
		    return expr;
		}
	return self;
    }

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
			JExpression left=(JExpression)leftF.get(0);
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
			JExpression left=(JExpression)leftF.get(k);
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
	    ArrayList newParams=new ArrayList();
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
    
    //Get fresh new var and or retrieve from cache
    private JLocalVariable getVar(String name,CType type) {
	if(paramMap.containsKey(name))
	    return (JFormalParameter)paramMap.get(name);
	JLocalVariable out=(JLocalVariable)nameToVar.get(name);
	if(out==null) {
	    out=new JVariableDefinition(null,0,type,name,null);
	    nameToVar.put(name,out);
	}
	return out;
    }

    //Add to fields that need to be added by call to addFields(SIRStream)
    private void addField(String name,CType type) {
	JFieldDeclaration decl=new JFieldDeclaration(null,new JVariableDefinition(null,0,type,name,null),null,null);
	if(!newFields.containsKey(name)) {
	    newFields.put(name,decl);
	}
    }

    //Recursive extracts components struct and substructs
    private static List[] extractFields(CClass struct) {
	CField[] fields=struct.getFields();
	List out=new ArrayList();
	List out2=new ArrayList();
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
		    out2.add(temp[1].get(j));
		}
	    }
	}
	return new List[]{out,out2}; //Return list of names and types
    }

    //Same as normal except be sure to add vars created with getVar(String name,CType type) at end
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

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
 * This class breaks up arrays into local vars as much as possible
 */
public class ArrayDestroyer extends SLIRReplacingVisitor {
    private HashMap targets;    
    private HashMap replaced;
    private HashMap varDefs;

    public ArrayDestroyer() {
	replaced=new HashMap();
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
		Iterator iter = sj.getParallelStreams().iterator();
		while (iter.hasNext())
		    {
			SIRStream child = (SIRStream)iter.next();
			destroyArrays(child);
		    }
	    }
	if (str instanceof SIRFilter)
	    for (int i = 0; i < str.getMethods().length; i++) {
		varDefs=new HashMap();
		str.getMethods()[i].accept(this);
	    }
    }
    
    /**
     * prints a method declaration
     */
    public Object visitMethodDeclaration(JMethodDeclaration self,
					 int modifiers,
					 CType returnType,
					 String ident,
					 JFormalParameter[] parameters,
					 CClassType[] exceptions,
					 JBlock body) {
	for (int i = 0; i < parameters.length; i++) {
	    if (!parameters[i].isGenerated()) {
		parameters[i].accept(this);
	    }
	}
	if (body != null) {
	    final HashMap targets=new HashMap();
	    final HashMap unsafe=new HashMap();
	    body.accept(new SLIRReplacingVisitor() {
		    /**
		     * If arrays used in any way except in array access then remove from targets
		     */
		    public Object visitLocalVariableExpression(JLocalVariableExpression self2,
							       String ident2) {
			targets.remove(((JLocalVariableExpression)self2).getVariable());
			unsafe.put(((JLocalVariableExpression)self2).getVariable(),Boolean.TRUE);
			return self2;
		    }

		    /**
		     * Considers target
		     */
		    public Object visitArrayAccessExpression(JArrayAccessExpression self2,
							     JExpression prefix,
							     JExpression accessor) {
			//prefix.accept(this);
			accessor.accept(this);
			if((prefix instanceof JLocalVariableExpression)&&(!unsafe.containsKey(((JLocalVariableExpression)prefix).getVariable())))
			    if(accessor instanceof JIntLiteral) {
				HashMap map=(HashMap)targets.get(((JLocalVariableExpression)prefix).getVariable());
				if(map==null) {
				    map=new HashMap();
				    targets.put(((JLocalVariableExpression)prefix).getVariable(),map);
				}
				map.put(new Integer(((JIntLiteral)accessor).intValue()),Boolean.TRUE);
			    } else {
				targets.remove(((JLocalVariableExpression)prefix).getVariable());
				unsafe.put(((JLocalVariableExpression)prefix).getVariable(),Boolean.TRUE);
			    }
			return self2;
		    }
		});
	    this.targets=targets;
	    Set keySet=targets.keySet();
	    JLocalVariable[] vars=new JLocalVariable[keySet.size()];
	    keySet.toArray(vars);
	    for(int i=0;i<vars.length;i++) {
		JLocalVariable var=vars[i];
		keySet=((HashMap)targets.get(var)).keySet();
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

    private JVariableDefinition toVar(JLocalVariable var,int idx) {
	return new JVariableDefinition(null,0,((CArrayType)var.getType()).getBaseType(),var.getIdent()+"__destroyed_"+idx,null);
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

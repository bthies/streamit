package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.*;

public class ArrayCopy {
    //Only handles rectangle multi dim arrays now
    public static void acceptInit(JMethodDeclaration init,Hashtable constants) {
	JBlock body=init.getBody();
	JFormalParameter[] params=init.getParameters();
	for(int i=params.length-1;i>=0;i--) {
	    LinkedList dims=new LinkedList();
	    Object val=constants.get(params[i]);
	    Object temp=val;
	    while(temp instanceof Object[]) {
		dims.add(new JIntLiteral(((Object[])temp).length));
		temp=((Object[])temp)[0];
	    }
	    if(dims.size()>0) {
		dumpAssign(val,body,new JLocalVariableExpression(null,params[i]));
		body.addStatementFirst(new JExpressionStatement(null,new JAssignmentExpression(null,new JLocalVariableExpression(null,params[i]),new JNewArrayExpression(null,params[i].getType(),(JExpression[])dims.toArray(new JExpression[0]),null)),null));
	    }
	}
    }
    
    private static void dumpAssign(Object array,JBlock body,JExpression prefix) {
	if(array instanceof JExpression) {
	    if(((JExpression)array).isConstant())
		body.addStatementFirst(new JExpressionStatement(null,new JAssignmentExpression(null,prefix,(JExpression)array),null));
	} else if(array instanceof Object[]) {
	    for(int i=((Object[])array).length-1;i>=0;i--)
		dumpAssign(((Object[])array)[i],body,new JArrayAccessExpression(null,prefix,new JIntLiteral(i)));
	} else {
	    System.err.println("WARNING: Non Array input to dumpAssign"+array);
	}
    }
}

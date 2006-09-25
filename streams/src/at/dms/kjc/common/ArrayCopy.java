package at.dms.kjc.common;

import at.dms.kjc.*;

import java.util.*;

public class ArrayCopy {
    //Only handles rectangle multi dim arrays now
    public static void acceptInit(JMethodDeclaration init,Hashtable constants) {
        JBlock body=init.getBody();
        JFormalParameter[] params=init.getParameters();
        for(int i=params.length-1;i>=0;i--) {
            LinkedList<JIntLiteral> dims=new LinkedList<JIntLiteral>();
            Object val=constants.get(params[i]);
            Object temp=val;
            while(temp instanceof Object[]) {
                dims.add(new JIntLiteral(((Object[])temp).length));
                temp=((Object[])temp)[0];
            }
            if(dims.size()>0) {
                printWarning = false;
                dumpAssign(val,body,new JLocalVariableExpression(null,params[i]));
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
            if (!printWarning) {
                System.err.println("WARNING: Non-array input in ArrayCopy (" + array + ")");
                printWarning = true;
            }
        }
    }
    // whether or not we have already printed a warning -- to avoid
    // hundreds of pages of warnings
    private static boolean printWarning;
 
}

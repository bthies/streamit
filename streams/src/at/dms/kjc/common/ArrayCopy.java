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
		// remove array assignments from this formal parameter, since it will be assigned elementwise instead
		final JFormalParameter param = params[i];
		final boolean[] didReplace = new boolean[] { false };
		body.accept(new SLIRReplacingVisitor() {
			public Object visitExpressionStatement(JExpressionStatement self,
							       JExpression expr) {
			    if (self.getExpression() instanceof JAssignmentExpression) {
				JExpression right = ((JAssignmentExpression)self.getExpression()).getRight();
				if ((right instanceof JLocalVariableExpression) && ((JLocalVariableExpression)right).getVariable()==param) {
				    didReplace[0] = true;
				    return new JEmptyStatement();
				}
			    } 
			    return super.visitExpressionStatement(self, expr);
			}
		    });
		// sometimes we do constant prop multiple times, which
		// causes redundant dumps of this assignment.  detect
		// if we are here for the first time by seeing if we
		// replaced the array assignment above.  If we did,
		// then dump the elementwise assignments
		if (didReplace[0]) {
		    dumpAssign(val,body,new JLocalVariableExpression(null,params[i]));
		}
            }
        }
    }

    private static void dumpAssign(Object array,JBlock body,JExpression prefix) {
        if(array instanceof JExpression) {
            if(((JExpression)array).isConstant()) {
		// if this is assigning a constant to a formal parameter, assign it to a field instead of to the formal parameter
	        body.addStatementFirst(new JExpressionStatement(null,new JAssignmentExpression(null,wrap(prefix),(JExpression)array),null));
	    }
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

    // instead of assigning constants to a parameter, assign them to the field
    private static JExpression wrap(JExpression prefix) {
	return (JExpression)prefix.accept(new SLIRReplacingVisitor() {
		public Object visitLocalVariableExpression(JLocalVariableExpression self,
							   String ident) {
		    if (ident.startsWith("_param_")) {
			JFieldAccessExpression result = new JFieldAccessExpression(ident.substring(7));
			result.setType(self.getType());
			return result;
		    } else {
			return super.visitLocalVariableExpression(self, ident);
		    }
		}
	    });
    }
    
    // whether or not we have already printed a warning -- to avoid
    // hundreds of pages of warnings
    private static boolean printWarning;
 
}

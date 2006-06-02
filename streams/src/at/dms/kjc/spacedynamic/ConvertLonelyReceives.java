package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;

/**
 * This class converts network receives that are not assigned to a
 * variable or used in an enclosing expression, into assignments to a 
 * dummy variable, so they are not optimized out by GCC.
 */
public class ConvertLonelyReceives
{
    public static final String VARNAME = "__lonely_receive__";

    public static void doit(SpdStaticStreamGraph ssg, FlatNode node)
    {
        SIRFilter filter = node.getFilter();
    
        //types, only do this if it is a scalar, because receives on
        //structs and arrays generate function calls instead of var refs
        CType type = ssg.getInputType(node);
        if (!type.isNumeric())
            return;

        for (int i = 0; i < filter.getMethods().length; i++) {
            JMethodDeclaration meth = filter.getMethods()[i];
            //did we replace anything...
            final boolean[] replaced = {false};
            final JVariableDefinition varDef = 
                new JVariableDefinition(null, 0, type, VARNAME, 
                                        type.isFloatingPoint() ? (JLiteral) new JFloatLiteral(0) :
                                        (JLiteral)new JIntLiteral(0));

            //replace the lonely receives
            meth.getBody().accept(new SLIRReplacingVisitor() {
                    public Object visitExpressionStatement(JExpressionStatement self,
                                                           JExpression expr) {

                        //this this statement consists only of a method call and 
                        //it is a receive and it has zero args, the create a dummy assignment
                        if (expr instanceof JMethodCallExpression &&
                            ((JMethodCallExpression)expr).getIdent().equals(RawExecutionCode.receiveMethod) &&
                            ((JMethodCallExpression)expr).getArgs().length == 0) {              
                            self.setExpression
                                (new JAssignmentExpression(new JLocalVariableExpression(varDef), 
                                                           expr));
                            replaced[0] = true;
                        }
            
                        return self;
                    }
                });
        
            //if we created an assignment expression, create a var def for the 
            //dummy variable
            if (replaced[0])
                meth.getBody().
                    addStatementFirst(new JVariableDeclarationStatement(null, varDef, null));
        
        
        }
    }
}

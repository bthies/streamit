package at.dms.kjc.rstream;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;

/**
 * Given an entry point for a visitor on the java IR, check
 * if anything under it has a method call.
 *
 * @author Michael Gordon
 * 
 */

public class CheckForMethodCalls extends SLIREmptyVisitor
{
    private boolean methodCall;

    /**
     * Check for method calls starting at entry.
     *
     * @param entry The contruct that starts the visiting
     *
     *
     * @return True if there is a method call discovered while visiting 
       <entry>, false otherwise
     *
     */
    public static boolean check(JPhylum entry) 
    {
	CheckForMethodCalls cm = new CheckForMethodCalls();
	entry.accept(cm);
	return cm.methodCall;
    }
    
    private CheckForMethodCalls() 
    {
	methodCall = false;
    }
    
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
	methodCall = true;
    }
    
}


package at.dms.kjc.rstream;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;

/**
 * This class will return a HashSet containing all the
 * variables (locals and fields) used (use or def) from the entry
 * point of the visitor. 
 *
 * @author Michael Gordon
 * 
 */

public class VariablesUsed extends SLIREmptyVisitor
{
    private HashSet vars;

    /**
     * Given <entry>, the starting point of the visit, return 
     * a HashSet of all variables used or defined during the IR visit.
     *
     *
     * @param entry The contruct that starts the visiting
     *
     *
     * @return A Hashset containing JLocalVariables for accessed locals 
     * or Strings for accessed fields
     *
     */
    public static HashSet getVars(JPhylum entry) 
    {
	VariablesUsed used = new VariablesUsed();
	
	entry.accept(used);
	
	return used.vars;
    }
    
    private VariablesUsed() 
    {
	vars = new HashSet();
    }
    

    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
				     String ident) 
    {
	vars.add(ident);
    }

    public void visitLocalVariableExpression(JLocalVariableExpression self,
					     String ident) 
    {
	vars.add(self.getVariable());
    }
}

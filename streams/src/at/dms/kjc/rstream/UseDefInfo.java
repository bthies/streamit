package at.dms.kjc.rstream;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.ListIterator;
import at.dms.kjc.flatgraph.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class will return a HashSet containing all the
 * variables (locals and fields) used (use or def) from the entry
 * point of the visitor. 
 *
 * @author Michael Gordon
 * 
 */

public class UseDefInfo extends SLIREmptyVisitor 
{
    private HashMap uses;

    public static HashMap getUsesMap(JMethodDeclaration meth) 
    {
	UseDefInfo useInfo = new UseDefInfo();
	meth.accept(useInfo);
	return useInfo.uses;
    }
    
    public static HashSet getForUses(JForStatement jfor) 
    {
	UseDefInfo useInfo = new UseDefInfo();
	jfor.getBody().accept(useInfo);
	jfor.getInit().accept(useInfo);
	jfor.getCondition().accept(useInfo);
	jfor.getIncrement().accept(useInfo);
	
	HashSet ret = new HashSet();
	Iterator vars = useInfo.uses.keySet().iterator();
	while (vars.hasNext()) {
	    StrToRStream.addAll(ret, (HashSet)useInfo.uses.get(vars.next()));
	}

	return ret;
    }
    

    public static HashSet getUses(JPhylum jsomething)  
    {
	UseDefInfo useInfo = new UseDefInfo();
	jsomething.accept(useInfo);

	HashSet ret = new HashSet();
	Iterator vars = useInfo.uses.keySet().iterator();
	while (vars.hasNext()) {
	    StrToRStream.addAll(ret, (HashSet)useInfo.uses.get(vars.next()));
	}

	return ret;
    }
    
    private UseDefInfo() 
    {
	uses = new HashMap();
    }

    /*    
    private void addUse(JFieldAccessExpression exp)
    {

	if (!uses.containsKey(exp.getIdent()))
	    uses.put(exp.getIdent(), new HashSet());

	((HashSet)uses.get(exp.getIdent())).add(exp);
    }
    */
    private void addUse(JLocalVariableExpression exp) 
    {
	//if we didn't see this var before, add the 
	//hashset to hold uses
	
	if (exp.getVariable() == null) 
	    System.out.println("Null variable");
	

	if (!uses.containsKey(exp.getVariable()))
	    uses.put(exp.getVariable(), new HashSet());

	((HashSet)uses.get(exp.getVariable())).add(exp);
    }
    
    public void visitLocalVariableExpression(JLocalVariableExpression self,
					     String ident) {
	addUse(self);
    }
}

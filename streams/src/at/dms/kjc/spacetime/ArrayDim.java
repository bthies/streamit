package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;


/**
 * This class finds the array dimensions for a given variable
 */
public class ArrayDim extends SLIREmptyVisitor 
{
    //the dimension of the array when found
    private JExpression[] dims;
    
    String variable;
    
    public static String[] findDim(ComputeCodeStore codeStore, String var)
    {
	ArrayDim ad = new ArrayDim(var);

	//Visit fields declared in the filter class
	JFieldDeclaration[] fields = codeStore.getFields();
	for (int i = 0; i < fields.length; i++)
	    fields[i].accept(ad);

	//visit methods
	JMethodDeclaration[] methods = codeStore.getMethods();
	for (int i =0; i < methods.length; i++)
	    methods[i].accept(ad);
	
	if (ad.dims == null)
	    return null;
	
	String[] ret = new String[ad.dims.length];
	

	for (int i = 0; i < ad.dims.length; i++) {
	    TraceIRtoC ttoc = new TraceIRtoC();
	    ad.dims[i].accept(ttoc);
	    ret[i] = ttoc.getString();
	}
	
	return ret;
    }
    
    private ArrayDim(String var) 
    {
	variable = var;
    }

    //search for array definitions in variable definitions (local variables)
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
	if (!variable.equals(ident))
	    return;

	//if we are declaring a new array, record the dims
	if (expr instanceof JNewArrayExpression) {
	    dims = ((JNewArrayExpression)expr).getDims();
	    return;
	}
    }
    

    //search for array declarations in assignment statements
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {
	
	String ident = "";
	
	//get the string ident of the var we are assigning to
	if (left instanceof JFieldAccessExpression) 
	    ident = ((JFieldAccessExpression)left).getIdent();
	else if (left instanceof JLocalVariableExpression) 
	    ident = ((JLocalVariableExpression)left).getVariable().getIdent();
	else 
	    return;
	
	//check the name
	if (!variable.equals(ident))
	    return;
	
	//if we are declaring a new array, record the dims
	if (right instanceof JNewArrayExpression) {
	    dims = ((JNewArrayExpression)right).getDims();
	    return;
	}

    }

    
    //we can also stack allocate fields (globals) of the filter
    //also, this will stack allocate the peek buffer introduced 
    //by the RawExecutionCode pass
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
	//check the name
	if (!variable.equals(ident))
	    return;
	
	//record the dims
	if (expr instanceof JNewArrayExpression) {
	    dims = ((JNewArrayExpression)expr).getDims();
	    return;
	}
    }
}

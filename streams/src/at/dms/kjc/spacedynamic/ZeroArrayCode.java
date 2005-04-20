package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ArrayList;
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
 * We attempt to stack allocate array locals (not fields), so we have 
 * to insert memset() calls to zero the local arrays.  Fields are declared with
 * calloc.
 */
public class ZeroArrayCode
{
    
    public static void doit(StaticStreamGraph ssg) 
    {
	//do nothing if the simulator initializes memory to 0
	if (KjcOptions.malloczeros)
	    return;

	Iterator flatNodes = ssg.getFlatNodes().iterator();
	while (flatNodes.hasNext()) {
	    FlatNode node = (FlatNode)flatNodes.next();
	    //only do this for filters...
	    if (!node.isFilter())
		continue;
	    //a hashset of JVariableDefinitions of variables 
	    //that we need to call memset 
	    HashSet vars = getStackArrayVars(node.getFilter());
	    //create the memset calls...
	    insertMemsetCalls(node.getFilter(), vars);
	}
    }
    
    private static HashSet getStackArrayVars(final SIRFilter filter) 
    {
	final HashSet vars = new HashSet();
	
	for (int i = 0; i < filter.getMethods().length; i++) {
	    final JMethodDeclaration meth = filter.getMethods()[i];
	    //don't generate memsets for arrays declared local to the init
	    //function
	    if (meth.getName().startsWith("init"))
		continue;
	    
	    meth.accept(new SLIREmptyVisitor() {
		    //look at FlatIRToC to determine when we stack allocate arrays
		    //this must be kept in synch with that...
		    public void visitVariableDefinition(JVariableDefinition self,
							int modifiers,
							CType type,
							String ident,
							JExpression expr) {
			if (type.isArrayType()) {
			    String[] dims = ArrayDim.findDim(new FlatIRToC(), filter.getFields(), meth, ident);
			    //if it has a new array exp as an init then it is going to be stack allocated
			    if (expr instanceof JNewArrayExpression)
				vars.add(self);
			    else { //if we can find the dims of the array somewhere else 
				   //it will be stack allocated as well...
				if (dims != null)
				    vars.add(self);
			    }
			}
		    }
		    
		    public void visitAssignmentExpression(JAssignmentExpression self,
							  JExpression left,
							  JExpression right) { 
			//if the array is new'ed in an assignment then remember 
			//the array for memset'ing
			if (right instanceof JNewArrayExpression &&
			    (left instanceof JLocalVariableExpression)) {
			    vars.add(((JLocalVariableExpression)left).getVariable());
			}
		    }
		});
	}
	
	return vars;
    }
    
    private static void insertMemsetCalls(SIRFilter filter, final HashSet vars) 
    {
	JMethodDeclaration[] methods = filter.getMethods();
	for (int i=0; i<methods.length; i++) {
	    JBlock newBody = (JBlock)methods[i].getBody().accept(new SLIRReplacingVisitor() {
		    public Object visitBlockStatement(JBlock self,
						      JavaStyleComment[] comments) {
			ArrayList newStatements = new ArrayList();
			for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
			    JStatement statement = (JStatement)it.next();
			    //recursively visit statement, it could be a block itself
			    Object newStatement = statement.accept(this);
			    //add the transformed statement
			    newStatements.add(newStatement);
			    //now see if this statement is a var def
			    if (statement instanceof JVariableDeclarationStatement) {
				JVariableDeclarationStatement varDecl = 
				    (JVariableDeclarationStatement)statement;
				//see if any of the var defs need memset calls
				for (int i = 0; i < varDecl.getVars().length; i++) {
				    if (vars.contains(varDecl.getVars()[i])) {
					newStatements.add(getMemsetCall(varDecl.getVars()[i]));
				    }
				}
			    }
			}
			return new JBlock(null,(JStatement[])newStatements.toArray(new JStatement[0]),null);
		    }
		});
	}
    }

    private static JStatement getMemsetCall(JVariableDefinition var) 
    {
	JExpression[] sizeofArgs = {new JLocalVariableExpression(var)};
	
	JMethodCallExpression sizeof = 
	    new JMethodCallExpression(null, new JThisExpression(null),
				      "sizeof", sizeofArgs);
	
	JExpression[] memsetArgs = 
	    {new JLocalVariableExpression(var),
	     new JIntLiteral(0),
	     sizeof};
	
	return new JExpressionStatement(null,
					new JMethodCallExpression(null, new JThisExpression(null),
					 "memset", memsetArgs),
					null);   
    }
    
}

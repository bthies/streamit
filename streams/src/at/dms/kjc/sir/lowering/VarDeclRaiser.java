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
 * This class raises Variable Declaration to the beginning
 * of blocks for C friendly code
 * Use visitBlockStatement to move Var Decls to top of block or
 * have it run on the method and have it recurse
 */
public class VarDeclRaiser extends SLIRReplacingVisitor {
    /**
     * List of variableDeclarations to move to the front of the block
     */
    private LinkedList varDefs;
    /**
     * List of JNewArrayExpression to move to the front of the block
     */
    private LinkedList newArrays;
    /**
     * Int used to rename conflicting variable names
     */
    private int conflict;

    /**
     * Top level block of current analysis
     */
    private JBlock parent;

    public VarDeclRaiser() {
	super();
	conflict=0;
    }

    // ----------------------------------------------------------------------
    // Moving VariableDeclarations to front of block
    // ----------------------------------------------------------------------

    public void raiseVars(SIRStream str) {
	if (str instanceof SIRFeedbackLoop)
	    {
		SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
		if (str.getInit()!=null) 
		    str.getInit().accept(this);
		raiseVars(fl.getBody());
		raiseVars(fl.getLoop());
	    }
        if (str instanceof SIRPipeline)
	    {
		SIRPipeline pl = (SIRPipeline)str;
		Iterator iter = pl.getChildren().iterator();
		if (str.getInit()!=null) 
		    str.getInit().accept(this);
		while (iter.hasNext())
		    {
			SIRStream child = (SIRStream)iter.next();
			raiseVars(child);
		    }
	    }
        if (str instanceof SIRSplitJoin)
	    {
		SIRSplitJoin sj = (SIRSplitJoin)str;
		Iterator iter = sj.getParallelStreams().iterator();
		if (str.getInit()!=null) 
		    str.getInit().accept(this);
		while (iter.hasNext())
		    {
			SIRStream child = (SIRStream)iter.next();
			raiseVars(child);
		    }
	    }
	if (str instanceof SIRFilter)
	    for (int i = 0; i < str.getMethods().length; i++) {
		str.getMethods()[i].accept(this);
	    }
    }

    public Object visitBlockStatement(JBlock self,
				      JavaStyleComment[] comments) {
	if(parent==null) {
	    parent=self;
	    varDefs=new LinkedList();
	    newArrays=new LinkedList();
	}
	//LinkedList saveDefs=varDefs;
	int size=self.size();
	for (int i=0;i<size;i++) {
	    boolean neg=false;
	    //if(i<0)
	    //continue;
	    JStatement oldBody = (JStatement)self.getStatement(i);
	    Object newBody = oldBody.accept(this);
	    if (!(newBody instanceof JStatement))
		continue;
	    //System.out.println("VarDeclRaiser:"+newBody);
	    if(newBody instanceof JVariableDeclarationStatement) {
		self.removeStatement(i);
		varDefs.add(newBody);
		JVariableDefinition[] vars=((JVariableDeclarationStatement)newBody).getVars();
		for(int j=vars.length-1;j>=0;j--) {
		    JVariableDefinition def=(JVariableDefinition)vars[j];
		    JExpression val=def.getValue();
		    // move array initializers up because they have to
		    // stick with their declaration.  This might be
		    // unsafe if the initializer references variables
		    // that are defined above... we should really be
		    // inserting new blocks instead of moving
		    // statements up.
		    if(val!=null && !(val instanceof JNewArrayExpression || val instanceof JArrayInitializer)) {
			def.setValue(null);
			TokenReference ref=((JVariableDeclarationStatement)newBody).getTokenReference();
			JStatement state=new JExpressionStatement(ref,new JAssignmentExpression(ref,new JLocalVariableExpression(ref,def),val),null);
			self.addStatement(i,(JStatement)state);
			//i++;
			size++;
		    }
		}
		neg=true;
		size--;
	    } else if(newBody instanceof JExpressionStatement) {
		JExpressionStatement exp=(JExpressionStatement)newBody;
		if(exp.getExpression() instanceof JAssignmentExpression) {
		    JAssignmentExpression assign=(JAssignmentExpression)exp.getExpression();
		    if((assign.getRight() instanceof JNewArrayExpression)&&(assign.getLeft() instanceof JLocalVariableExpression)) {
			JNewArrayExpression newArray=(JNewArrayExpression)assign.getRight();
			//Make sure all dimensions are IntLiterals
			JExpression[] dims=newArray.getDims();
			boolean ok=true;
			for(int j=0;j<dims.length;j++)
			    if(!(dims[j] instanceof JIntLiteral))
				ok=false;
			if(ok) {
			    newArrays.add(newBody);
			    self.removeStatement(i);
			    //i--;
			    neg=true;
			    size--;
			}
		    }
		}
	    } else if(newBody instanceof JExpressionListStatement) {
		JExpression[] exps=((JExpressionListStatement)newBody).getExpressions();
		for(int k=0;k<exps.length;k++)
		    if(exps[k] instanceof JAssignmentExpression) {
			JAssignmentExpression assign=(JAssignmentExpression)exps[k];
			if((assign.getRight() instanceof JNewArrayExpression)&&(assign.getLeft() instanceof JLocalVariableExpression)) {
			    JNewArrayExpression newArray=(JNewArrayExpression)assign.getRight();
			    //Make sure all dimensions are IntLiterals
			    JExpression[] dims=newArray.getDims();
			    boolean ok=true;
			    for(int j=0;j<dims.length;j++)
				if(!(dims[j] instanceof JIntLiteral))
				    ok=false;
			    if(ok) {
				newArrays.add(newBody);
				self.removeStatement(i);
				//i--;
				neg=true;
				size--;
			    }
			}
		    }
	    } else if (newBody!=null && newBody!=oldBody) {
		self.setStatement(i,(JStatement)newBody);
	    }
	    if(neg)
		i--;
	}
	if(parent==self) {
	    for(int i=newArrays.size()-1;i>=0;i--) {
		JStatement newState=(JStatement)newArrays.get(i);
		self.addStatementFirst(newState);
	    }
	    Hashtable visitedVars=new Hashtable();
	    for(int i=varDefs.size()-1;i>=0;i--) {
		JVariableDeclarationStatement varDec=(JVariableDeclarationStatement)varDefs.get(i);
		self.addStatementFirst(varDec);
		JVariableDefinition[] varArray=varDec.getVars();
		LinkedList newVars=new LinkedList();
		for(int j=0;j<varArray.length;j++) {
		    JLocalVariable var=(JLocalVariable)varArray[j];
		    if(!visitedVars.containsKey(var.getIdent())) {
			visitedVars.put(var.getIdent(),Boolean.TRUE);
			newVars.add(var);
		    } else {
			var.setIdent(var.getIdent()+"__conflict__"+conflict++);
			visitedVars.put(var.getIdent(),Boolean.TRUE);
			//System.err.println("Conflict:"+var.getIdent());
			newVars.add(var);
		    }
		}
		varDec.setVars((JVariableDefinition[])newVars.toArray(new JVariableDefinition[0]));
	    }
	    parent=null;
	}
	//varDefs=saveDefs;
	visitComments(comments);
	return self;
    }

    /**
     * Visits a for statement
     */
    public Object visitForStatement(JForStatement self,
				    JStatement init,
				    JExpression cond,
				    JStatement incr,
				    JStatement body) {
	// cond should never be a constant, or else we have an
	// infinite or empty loop.  Thus I won't check for it... 
	// recurse into init
	JStatement newInit = (JStatement)init.accept(this);
	if (newInit!=null && newInit!=init) {
	    self.setInit(newInit);
	}
	
	// recurse into incr
	JStatement newIncr = (JStatement)incr.accept(this);
	if (newIncr!=null && newIncr!=incr) {
	    self.setIncr(newIncr);
	}

	JExpression newExp = (JExpression)cond.accept(this);
	if (newExp!=null && newExp!=cond) {
	    self.setCond(newExp);
	}
	
	// recurse into body
	JStatement newBody = (JStatement)body.accept(this);
	if (newBody!=null && newBody!=body) {
	    self.setBody(newBody);
	}
	if(newInit instanceof JVariableDeclarationStatement) {
	    JVariableDefinition[] vars=((JVariableDeclarationStatement)newInit).getVars();
	    if(vars.length>1)
		System.err.println("Warning: Compound Variable Declaration in for loop (not handled)"); //Not handled
	    JVariableDefinition def=(JVariableDefinition)vars[0];
	    JExpression val=def.getValue();
	    varDefs.add(newInit);
	    if(val!=null) {
		def.setValue(null);
		TokenReference ref=((JVariableDeclarationStatement)newInit).getTokenReference();
		self.setInit(new JExpressionListStatement(ref,new JExpression[] {new JAssignmentExpression(ref,new JLocalVariableExpression(ref,def),val)},null));
	    } else
		self.setInit(new JEmptyStatement(null, null));
	}
	return self;
    }
}


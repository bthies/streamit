package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;

public class ComputeCodeStore {
    public static String main = "__RAWMAIN__";

    protected JFieldDeclaration[] fields;
    protected JMethodDeclaration[] methods;
    //this method calls all the initialization routines
    //and the steady state routines...
    protected JMethodDeclaration rawMain;

    protected RawTile parent;
    //index into the main function for this tile that keeps
    //the index where we should add the next init stage call
    protected int initIndex;
    //steady index for steady state calls
    protected int steadyIndex;
    protected JBlock steadyLoop;

    public ComputeCodeStore(RawTile parent) {
	this.parent = parent;
	methods = new JMethodDeclaration[0];
	fields = new JFieldDeclaration[0];
	rawMain = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
					 CStdType.Void, main,
					 JFormalParameter.EMPTY,
					 CClassType.EMPTY,
					 new JBlock(null, new JStatement[0], null) , null, null);
	initIndex = steadyIndex = 0;
	//create the body of steady state loop
	steadyLoop = new JBlock(null, new JStatement[0], null);
	//add it to the while statement
	rawMain.addStatement(new JWhileStatement(null, new JBooleanLiteral(null, true),
						 steadyLoop, null));
    }

    public void addTrace(FilterInfo filterInfo)
    {
	//now this raw tile has compute code so tell it so
	parent.setComputes();


	//Create a new RawExecutionCode object for this trace
	RawExecutionCode exeCode = new RawExecutionCode(filterInfo);
	
	//check if we can use direct communication, easy check because
	//no crossed routes
	//	if (filterInfo.bottomPeek == 0 && filterInfo.remaining == 0 &&
	
	//add the fields of the trace
	addFields(exeCode.getVarDecls());
	//get the initialization routine of the phase
	JMethodDeclaration init = exeCode.getInitStageMethod();
	//add the method
	addMethod(init);
	//now add a call to the init stage in main at the appropiate index
	//and increment the index
	rawMain.getBody().
	    addStatement(initIndex++,
			 new JExpressionStatement
			 (null, 
			  new JMethodCallExpression(null,
						    new JThisExpression(null),
						    init.getName(),
						    new JExpression[0]),
			  null));
	//add the steady state
	JMethodDeclaration steady = exeCode.getSteadyMethod();
	addMethod(steady);
	steadyLoop.addStatement(steadyIndex++, 
				new JExpressionStatement
				(null, 
				 new JMethodCallExpression(null,
							   new JThisExpression(null),
							   steady.getName(),
							   new JExpression[0]),
				 null));
    }
    

    /** Bill's code 
     * adds method <meth> to this, if <meth> is not already registered
     * as a method of this.  Requires that <method> is non-null.
     */
    public void addMethod(JMethodDeclaration method) {
	Utils.assert(method!=null);
	// see if we already have <method> in this
	for (int i=0; i<methods.length; i++) {
	    if (methods[i]==method) {
		return;
	    }
	}
	// otherwise, create new methods array
	JMethodDeclaration[] newMethods = new JMethodDeclaration[methods.length
								+ 1];
	// copy in new method
	newMethods[0] = method;
	// copy in old methods
	for (int i=0; i<methods.length; i++) {
	    newMethods[i+1] = methods[i];
	}
	// reset old to new
	this.methods = newMethods;
    }

    /**
     * Adds <f> to the fields of this.  Does not check
     * for duplicates.
     */
    public void addFields (JFieldDeclaration[] f) {
	JFieldDeclaration[] newFields = 
	    new JFieldDeclaration[fields.length + f.length];
	for (int i=0; i<fields.length; i++) {
	    newFields[i] = fields[i];
	}
	for (int i=0; i<f.length; i++) {
	    newFields[fields.length+i] = f[i];
	}
	this.fields = newFields;
    }


    /**
     * adds field <field> to this, if <field> is not already registered
     * as a field of this.  
     */
    public void addField(JFieldDeclaration field) {
	// see if we already have <field> in this
	for (int i=0; i<fields.length; i++) {
	    if (fields[i]==field) {
		return;
	    }
	}
	// otherwise, create new fields array
	JFieldDeclaration[] newFields = new JFieldDeclaration[fields.length
								+ 1];
	// copy in new field
	newFields[0] = field;
	// copy in old fields
	for (int i=0; i<fields.length; i++) {
	    newFields[i+1] = fields[i];
	}
	// reset old to new
	this.fields = newFields;
    }
    
    public JMethodDeclaration[] getMethods() 
    {
	return methods;
    }

    public JFieldDeclaration[] getFields() 
    {
	return fields;
    }
    
    public JMethodDeclaration getMainFunction() 
    {
	return rawMain;
    }
}

package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import java.util.HashMap;
import java.util.List;

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
    //the block that executes each tracenode's init schedule
    protected JBlock initBlock;
    //this hash map holds RawExecutionCode that was generated
    //so when we see the filter for the first time in the init
    //and if we see the same filter again, we do not have to 
    //add the functions again...
    HashMap rawCode;

    public ComputeCodeStore(RawTile parent) {
	this.parent = parent;
	rawCode = new HashMap();
	methods = new JMethodDeclaration[0];
	fields = new JFieldDeclaration[0];
	rawMain = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
					 CStdType.Void, main,
					 JFormalParameter.EMPTY,
					 CClassType.EMPTY,
					 new JBlock(null, new JStatement[0], null) , null, null);
	

	initIndex = 1;
	//add the block for the init stage calls
	initBlock = new JBlock(null, new JStatement[0], null);
	rawMain.addStatement(initBlock);

	steadyIndex = 0;
	//create the body of steady state loop
	steadyLoop = new JBlock(null, new JStatement[0], null);
	//add it to the while statement
	rawMain.addStatement(new JWhileStatement(null, new JBooleanLiteral(null, true),
						 steadyLoop, null));
	addMethod(rawMain);
    }

    //add a dram command to the compute code at the current time
    //if read is false, then it is a write
    //
    public void addDRAMCommand(boolean read, boolean init, 
			       int bytes, OffChipBuffer buffer) 
    {
	parent.setComputes();
	String functName = "raw_streaming_dram_request_" +
	    (read ? "read" : "write") + "_presynched";

	//the args for the streaming dram command
	
	//cachelines that are needed
	assert bytes % RawChip.cacheLineBytes == 0 :
	    "transfer size for dram must be a multiple of cache line size";

	int cacheLines = bytes / RawChip.cacheLineBytes;
	JExpression[] args = {new JIntLiteral(1),
			      new JIntLiteral(cacheLines)};

	//the dram command
	JMethodCallExpression call = 
	    new JMethodCallExpression(null, functName, args);
	
	//send over the address
	JFieldAccessExpression dynNetSend = 
	    new JFieldAccessExpression(null, new JThisExpression(null),
				       Util.CGNOINTVAR);
	
	JFieldAccessExpression bufAccess = 
	    new JFieldAccessExpression(null, new JThisExpression(null),
				       buffer.getIdent());
	
	JAssignmentExpression assExp = 
	    new JAssignmentExpression(null, dynNetSend, bufAccess);
	
	SpaceTimeBackend.println("Adding DRAM Command to " + parent + " " + buffer + " " + cacheLines);
	//add the statements to the appropriate stage
	if (init) {
	    initBlock.addStatement(new JExpressionStatement(null, call, null));
	    initBlock.addStatement(new JExpressionStatement(null, assExp, null));
	}
	else {
	    steadyLoop.addStatement(steadyIndex ++, 
				    new JExpressionStatement(null, call, null));
	    steadyLoop.addStatement(steadyIndex ++, 
				    new JExpressionStatement(null, assExp, null));
	}
    }

    public void addTraceSteady(FilterInfo filterInfo)
    {
	parent.setComputes();
	RawExecutionCode exeCode;

	//check to see if we have seen this filter already
	if (rawCode.containsKey(filterInfo.filter)) {
	    exeCode = (RawExecutionCode)rawCode.get(filterInfo.filter);
	}
	else {
	    //otherwise create the raw ir code 
	    //if we can run linear or direct communication, run it
	    if(filterInfo.isLinear())
		exeCode=new Linear(filterInfo);
	    else if (filterInfo.isDirect())
		exeCode = new DirectCommunication(filterInfo);
	    else
		exeCode = new BufferedCommunication(filterInfo);
	    addTraceFieldsAndMethods(exeCode, filterInfo);
	}
	//add the steady state
	JBlock steady = exeCode.getSteadyBlock();
	steadyLoop.addStatement(steadyIndex++, steady);
	/* addMethod(steady);
	steadyLoop.addStatement(steadyIndex++, 
				new JExpressionStatement
				(null, 
				 new JMethodCallExpression(null,
							   new JThisExpression(null),
							   steady.getName(),
							   new JExpression[0]),
				 null));
	*/
    }
    

    private void addTraceFieldsAndMethods(RawExecutionCode exeCode, 
					  FilterInfo filterInfo) 
    {
	//add the fields of the trace
	addFields(exeCode.getVarDecls());
	//add the helper methods
	addMethods(exeCode.getHelperMethods());
	//add the init function 
	addInitFunctionCall(filterInfo);
    }
    
    private void addInitFunctionCall(FilterInfo filterInfo) 
    {
	//create the params list, for some reason 
	//calling toArray() on the list breaks a later pass
	List paramList = filterInfo.filter.getParams();
	JExpression[] paramArray;
	if (paramList == null || paramList.size() == 0)
	    paramArray = new JExpression[0];
	else
	    paramArray = (JExpression[])paramList.toArray(new JExpression[0]);
	

	JMethodDeclaration init=filterInfo.filter.getInit();
	if(init!=null)
	    rawMain.addStatementFirst(new 
		JExpressionStatement(null,
				     new JMethodCallExpression
					 (null,
					  new JThisExpression(null),
					  filterInfo.filter.getInit().getName(),
					  paramArray),
				     null));
	
    }
    

    public void addTraceInit(FilterInfo filterInfo)
    {
	parent.setComputes();
	RawExecutionCode exeCode;
	
	//if we can run direct communication, run it
	if(filterInfo.isLinear())
	    exeCode=new Linear(filterInfo);
	else if (filterInfo.isDirect())
	    exeCode = new DirectCommunication(filterInfo);
	else 
	    exeCode = new BufferedCommunication(filterInfo);
	
	//add this raw IR code to the rawCode hashmap
	//if the steady-state is on the same tile, don't
	//regenerate the IR code and add dups of the functions,
	//fields
	rawCode.put(filterInfo.filter, exeCode);

	//this must come before anything
	addTraceFieldsAndMethods(exeCode, filterInfo);
	//get the initialization routine of the phase
	JMethodDeclaration initStage = exeCode.getInitStageMethod();
	if(initStage!=null) {
	    //add the method
	    addMethod(initStage);
	    
	    
	    //now add a call to the init stage in main at the appropiate index
	    //and increment the index
	    initBlock.
		addStatement(new JExpressionStatement
		    (null, 
		     new JMethodCallExpression(null,
					       new JThisExpression(null),
					       initStage.getName(),
					       new JExpression[0]),
		     null));
	}
    }
    

    /** Bill's code 
     * adds method <meth> to this, if <meth> is not already registered
     * as a method of this.  Requires that <method> is non-null.
     */
    public void addMethod(JMethodDeclaration method) {
	assert method!=null;
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

    /**
     * Adds <m> to the methods of this.  Does not check for
     * duplicates. 
     */
    public void addMethods (JMethodDeclaration[] m) {
	JMethodDeclaration[] newMethods = 
	    new JMethodDeclaration[methods.length + m.length];
	for (int i=0; i<methods.length; i++) {
	    newMethods[i] = methods[i];
	}
	for (int i=0; i<m.length; i++) {
	    newMethods[methods.length+i] = m[i];
	}
	this.methods = newMethods;
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

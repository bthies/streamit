package at.dms.kjc.spacetime;

import java.util.Vector;
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
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;
import at.dms.kjc.flatgraph2.FilterContent;

public abstract class RawExecutionCode 
{
     /*** fields for the var names we introduce ***/
    public static String recvBuffer = "__RECVBUFFER__";
    public static String recvBufferSize = "__RECVBUFFERSIZE__";
    public static String recvBufferBits = "__RECVBUFFERBITS__";

    //the output buffer for ratematching
    public static String sendBuffer = "__SENDBUFFER__";
    public static String sendBufferIndex = "__SENDBUFFERINDEX__";
    public static String rateMatchSendMethod = "__RATEMATCHSEND__";
    
    //recvBufferIndex points to the beginning of the tape
    public static String recvBufferIndex = "__RECVBUFFERINDEX__";
    //recvIndex points to the end of the tape
    public static String recvIndex = "_RECVINDEX__";

    public static String simpleIndex = "__SIMPLEINDEX__";
    
    public static String exeIndex = "__EXEINDEX__";
    public static String exeIndex1 = "__EXEINDEX__1__";

    public static String ARRAY_INDEX = "__ARRAY_INDEX__";
    public static String ARRAY_COPY = "__ARRAY_COPY__";

    public static String initSchedFunction = "__RAWINITSCHED__";
    public static String steadySchedFunction = "__RAWSTEADYSCHED__";
    
    public static String receiveMethod = "static_receive_to_mem";
    public static String structReceiveMethodPrefix = "__popPointer";
    public static String arrayReceiveMethod = "__array_receive__";

    public static String initStage = "__INITSTAGE__";
    public static String steadyStage = "__STEADYSTAGE__";
    public static String workCounter = "__WORKCOUNTER__";
 
    //keep a unique integer for each filter in each trace
    //so var names do not clash
    private static int globalID = 0;
    protected int uniqueID;

    protected GeneratedVariables generatedVariables;
    protected FilterInfo filterInfo;

    public RawExecutionCode(FilterInfo filterInfo) 
    {
	this.filterInfo = filterInfo;
	generatedVariables = new GeneratedVariables();
	uniqueID = getUniqueID();
    }
    

    public static int getUniqueID() 
    {
	return globalID++;
    }
    
    public abstract JFieldDeclaration[] getVarDecls();
    public abstract JMethodDeclaration[] getHelperMethods();
    public abstract JMethodDeclaration getInitStageMethod();
    public abstract JMethodDeclaration getSteadyMethod();
    
    
     /**
     * Returns a for loop that uses field <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns empty (!not legal in the general case)
     */
    public static JStatement makeForLoop(JStatement body,
					 JVariableDefinition var,
					 JExpression count) {
	if (body == null)
	    return new JEmptyStatement(null, null);
	
	// make init statement - assign zero to <var>.  We need to use
	// an expression list statement to follow the convention of
	// other for loops and to get the codegen right.
	JExpression initExpr[] = {
	    new JAssignmentExpression(null,
				      new JFieldAccessExpression(null, 
								 new JThisExpression(null),
								 var.getIdent()),
				      new JIntLiteral(0)) };
	JStatement init = new JExpressionListStatement(null, initExpr, null);
	// if count==0, just return init statement
	if (count instanceof JIntLiteral) {
	    int intCount = ((JIntLiteral)count).intValue();
	    if (intCount<=0) {
		// return assignment statement
		return new JEmptyStatement(null, null);
	    }
	}
	// make conditional - test if <var> less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JFieldAccessExpression(null, 
								   new JThisExpression(null),
								   var.getIdent()),
				      count);
	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JFieldAccessExpression(null, new JThisExpression(null),
								var.getIdent()));
	JStatement incr = 
	    new JExpressionStatement(null, incrExpr, null);

	return new JForStatement(null, init, cond, incr, body, null);
    }
}

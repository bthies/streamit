package at.dms.kjc.sir.lowering;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;

/**
 * Contains various constants for the lowering process.
 */
public class LoweringConstants {
    
    /**
     * The name of the type serving as the stream context.
     */
    public static final String CONTEXT_TYPE_NAME = "StreamContext";

    /**
     * The name of the variable holding the stream context in structures.
     */
    public static final String CONTEXT_VAR_NAME = "context";

    /**
     * The prefix of the name for the variable holding child
     * substreams in structures.
     */
    public static final String CHILD_NAME = "child";

    /**
     * The name of the parameter that functions get to access their
     * fields.
     */
    public static final String STATE_PARAM_NAME = "data";

    /**
     * The name of the type of the tape parameters to work functions.
     */
    public static final String TAPE_TYPE_NAME = "Tape";

    /**
     * The name of the parameter corresponding to the input tape.
     */
    public static final String INPUT_TAPE_NAME = "inTape";

    /**
     * The name of the parameter corresponding to the output tape.
     */
    public static final String OUTPUT_TAPE_NAME = "outTape";

    /**
     * Returns the name of the <childNum>'th child for use as a
     * variable name in the structure for a stream construct.
     * <childNum> should start counting at zero.
     */
    public static String getChildName(int childNum) {
	return "child" + (childNum+1);
    }

    /**
     * Returns a field access to child struct number <n>, as for use
     * within an init function referencing a child.  
     */
    public static JFieldAccessExpression getChildStruct(int n) {
	return new JFieldAccessExpression(
					  null,
					  /* prefix */
					  new JNameExpression(null, 
							     null, 
							     STATE_PARAM_NAME),
					  /* ident */
					  getChildName(n));
    }

    /**
     * Returns the field declaration declaring a stream context.
     */
    public static JFieldDeclaration getContextField() {
	// define a variable
	JVariableDefinition var = 
	    new JVariableDefinition(/* tokenref */ null, 
				    /* modifiers */ at.dms.kjc.
				    Constants.ACC_PUBLIC,
				    /* type */ CClassType.lookup(
					       CONTEXT_TYPE_NAME),
				    /* identifier  */ CONTEXT_VAR_NAME,
				    /* initializer */ null);
	// return the field
	return new JFieldDeclaration(/* tokenref */ null, 
				     /* variable */ var, 
				     /* javadoc  */ null, 
				     /* comments */ null);
    }

    /**
     * Returns a field declaration for the input tape of structures.  
     */
    public static JFieldDeclaration getInTapeField() {
	return getTapeField(INPUT_TAPE_NAME);
    }

    /**
     * Returns a field declaration for the output tape of structures.  
     */
    public static JFieldDeclaration getOutTapeField() {
	return getTapeField(OUTPUT_TAPE_NAME);
    }

    /**
     * Returns a field declaration for the tape fields of structures.  
     */
    private static JFieldDeclaration getTapeField(String name) {
	// define a variable
	JVariableDefinition var = 
	    new JVariableDefinition(/* tokenref */ null, 
				    /* modifiers */ at.dms.kjc.
				    Constants.ACC_PUBLIC,
				    /* type */ CClassType.lookup(
					       TAPE_TYPE_NAME),
				    /* identifier  */ name,
				    /* initializer */ null);
	// return the field
	return new JFieldDeclaration(/* tokenref */ null, 
				     /* variable */ var, 
				     /* javadoc  */ null, 
				     /* comments */ null);
    }


    /**
     * Given that the name of a stream structure is <streamName>, and
     * the name of a method in that structure is <methodName>, returns
     * the name for that methd in the flattened class.
     * */
    public static String getMethodName(String streamName, String methodName) {
	return streamName + "_" + methodName;
    }

    /**
     * Returns the name of the flattened work function for <str>.
     */
    public static String getWorkName(SIRStream str) {
	return getMethodName(str.getName(), "work");
    }

    /**
     * Returns the name of the flattened init function for <str>.
     */
    public static String getInitName(SIRStream str) {
	return getMethodName(str.getName(), "init");
    }

    /**
     * Returns a reference to current stream context inside a work
     * function.  
     */
    public static JExpression getStreamContext() {
	return new JFieldAccessExpression(/* tokref */
				       null,
				       /* prefix */
				       new JNameExpression(/* tokref */ 
							   null,
							   /* ident */
							   STATE_PARAM_NAME),
				       /* ident */
				       CONTEXT_VAR_NAME);
    }
}





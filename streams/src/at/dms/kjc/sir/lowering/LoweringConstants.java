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

}

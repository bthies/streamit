package at.dms.kjc.sir.lowering;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;

/**
 * Contains various constants for the lowering process.
 */
public class LoweringConstants {
    
  /**
   * The name of the method in the runtime system that runs a splitter.
   */
  public static final String SPLITTER_WORK_NAME = "run_splitter";

  /**
   * The name of the method in the runtime system that runs a joiner.
   */
  public static final String JOINER_WORK_NAME = "run_joiner";

  /**
   * The name of the type serving as the stream context.
   */
  public static final String CONTEXT_TYPE_NAME = "stream_context*";

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
  public static final String TAPE_TYPE_NAME = "tape";

  /**
   * The name of the parameter corresponding to the input tape.
   */
  public static final String INPUT_TAPE_NAME = "inTape";

  /**
   * The name of the parameter corresponding to the output tape.
   */
  public static final String OUTPUT_TAPE_NAME = "outTape";

  /**
   * Counts the work functions that have been created so that each
   * can be assigned a unique name.
   */
  private static int workFunctionCounter = 1;

  /**
   * Returns a field access to <child>, as for use within an init
   * function referencing a child.  
   */
  public static JFieldAccessExpression getChildStruct(SIROperator child) {
    return new JFieldAccessExpression(
				      null,
				      /* prefix */
				      getDataField(),
				      /* ident */
				      child.getRelativeName());
  }

  /**
   * Returns a field access to the current stream structure.
   */
  public static JExpression getDataField() {
    return new JNameExpression(null, 
			       null, 
			       STATE_PARAM_NAME);
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
  public static String getWorkName(SIRFilter str) {
    return getMethodName(str.getName(), "work");
  }

  /**
   * Returns a new, unique name for a work function that might have
   * no analogue in the stream structure (is "anonymous")
   */
  public static String getAnonWorkName() {
    return "hierarchical_work_" + workFunctionCounter++;
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
				      getDataField(),
				      /* ident */
				      CONTEXT_VAR_NAME);
  }
}





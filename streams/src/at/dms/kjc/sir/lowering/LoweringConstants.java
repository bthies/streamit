package at.dms.kjc.sir.lowering;

import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;

/**
 * Contains various constants for the lowering process.
 */
public class LoweringConstants {
    
    /**
     * The name of the method in the runtime system that runs a filereader.
     */
    public static final String 
        FILE_READER_WORK_NAME = "streamit_filereader_work";

    /**
     * The name of the method in the runtime system that runs a filewriter.
     */
    public static final String 
        FILE_WRITER_WORK_NAME = "streamit_filewriter_work";

    /**
     * The name of the method in the runtime system that runs identity.
     */
    public static final String
        IDENTITY_WORK_NAME = "streamit_identity_work";

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
    public static final String CONTEXT_VAR_NAME = "_context";

    /**
     * The prefix of the name for the variable holding child
     * substreams in structures.
     */
    public static final String CHILD_NAME = "_child";

    /**
     * The name of the parameter that functions get to access their
     * fields.
     */
    public static final String STATE_PARAM_NAME = "_data";

    /**
     * The name of the type of the tape parameters to work functions.
     */
    public static final String TAPE_TYPE_NAME = "tape";

    /**
     * The name of the parameter corresponding to the input tape.
     */
    public static final String INPUT_TAPE_NAME = "_inTape";

    /**
     * The name of the parameter corresponding to the output tape.
     */
    public static final String OUTPUT_TAPE_NAME = "_outTape";

    /**
     * Counts the work functions that have been created so that each
     * can be assigned a unique name.
     */
    private static int workFunctionCounter = 1;

    /**
     * Counts the interface table fields that have been assigned;
     */
    private static int interfaceTableCounter = 1;

    /**
     * Counts the number of vars that have been defined, so that each
     * can be given a unique identifier.
     */
    private static int varCounter = 1;


    /**
     * Returns a unique name for a variable to be added to a method of
     * the program.
     */
    public static String getUniqueVarName() {
        return "streamItVar" + varCounter++;
    }
    
    /**
     * Given the <initializer> for a field that corresponds to an
     * interface table, returns a variable definition for that field.
     */
    public static JVariableDefinition 
        getInterfaceTableVariable(JExpression initializer) {
        return new JVariableDefinition(/* tokref */ null,
                                       /* modifiers - emulate a constant */
                                       at.dms.kjc.Constants.ACC_PUBLIC |
                                       at.dms.kjc.Constants.ACC_STATIC | 
                                       at.dms.kjc.Constants.ACC_FINAL,
                                       /* type--doesn't matter, so try void */ 
                                       CStdType.Void,
                                       /* ident - number them */
                                       "interfaceTable" + 
                                       interfaceTableCounter++, 
                                       /* init exp */ initializer);
    }

    /**
     * Creates a field access that gets to the structure of <iter>
     * from the toplevel stream.
     */
    public static JExpression getParentStructureAccess(SIRIterator iter) {
        // get parents of <str>
        SIRStream parents[] = iter.getParents();

        // construct result expression
        JExpression result = getDataField();

        // go through parents from top to bottom, building up the
        // field access expression.
        for (int i=parents.length-2; i>=0; i--) {
            // get field name for child context
            String childName = parents[i].getRelativeName();
            // build up cascaded field reference
            result = new JFieldAccessExpression(/* tokref */
                                                null,
                                                /* prefix is previous ref*/
                                                result,
                                                /* ident */
                                                childName);
        }

        // return result
        return result;
    }

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
        return getStreamContext(getDataField());
    }

    /**
     * Returns access to stream context given parent structure <par>
     */
    public static JExpression getStreamContext(JExpression parentStructure) {
        return new JFieldAccessExpression(/* tokref */
                                          null,
                                          /* prefix */
                                          parentStructure,
                                          /* ident */
                                          CONTEXT_VAR_NAME);
    }
}





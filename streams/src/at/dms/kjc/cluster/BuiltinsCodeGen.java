package at.dms.kjc.cluster;

import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.sir.*;
import at.dms.kjc.CType;
import java.util.*;
//import at.dms.kjc.JMethodDeclaration;

class BuiltinsCodeGen {

    /**
     * Create code for the work function of a built-in filter.
     * 
     * @param filter  The filter that the work function is being built for
     * @param selfID  A unique id assigned in FlatIRToC
     * @param p       A printer to output the generated code
     */
    static void predefinedFilterWork(SIRPredefinedFilter filter, int selfID,
            CodegenPrintWriter p) {
        // Caller has printed function name and an iteration parameter ____n
        // Generate loop to execute body of this function ____n times,
        // In loop generate specialized code for function.
        p.print("void " + ClusterUtils.getWorkName(filter, selfID)
                + "(int ____n) {");
        p.indent();
        p.newLine();
        p.indent();
        p.print("// predefinedFilterWork " + filter.getIdent());
        p.newLine();
        p.print("for (; 0 < ____n; ____n--) {");
        p.indent();
        p.newLine();

        // SIRFileReader
        if (filter instanceof SIRFileReader) {
            String theType = "" + filter.getOutputType();
            // pop into a location.
            // Is the cast sufficisnt for the bit type?
            p.print(theType + " v;");
            p.newLine();
            p.print("fread(" + "&v, " + "sizeof(v), " + "1, " + fpName(filter)
                    + ");");
            p.newLine();
            p.print(ClusterUtils.pushName(selfID) + "(v);");
            p.newLine();

            // SIRFileWriter
        } else if (filter instanceof SIRFileWriter) {
            String theType = "" + filter.getInputType();
            // pop into a location.
            // Is the cast sufficient for the bit type? No!
            p.print(theType + " v = (" + theType + ")("
                    + ClusterUtils.popName(selfID) + "());");
            p.newLine();
            p.print("fwrite(" + "&v, " + "sizeof(v), " + "1, " + fpName(filter)
                    + ");");
            p.newLine();

            // SIRIdentity
        } else if (filter instanceof SIRIdentity) {
            p.print("  //SIRIdentity work");
            p.newline();
            p.print("  return " + ClusterUtils.pushName(selfID) + "("
                    + ClusterUtils.popName(selfID) + "());");
            p.newline();

            // SIRDummySink
        } else if (filter instanceof SIRDummySink) {
            // TODO: get right exception for unimplemented.
            throw new Error("Unsupported predefined filter "
                    + filter.getIdent());

            // SirDummySource
        } else if (filter instanceof SIRDummySource) {
            throw new Error("Unsupported predefined filter "
                    + filter.getIdent());
        } else {
            // TODO: get right unchecked exception for unextended code...
            throw new Error("Unknown predefined filter " + filter.getIdent());
        }
        p.outdent();
        p.print("}"); // end of for loop.
        p.newLine();
        p.outdent(); // end of method body
        p.outdent(); // end of method definition
        p.print("}");
        p.newLine();
        p.newLine();

    }

    private static void startParameterlessFunction(String return_type,
            String function_name, CodegenPrintWriter p) {
        p.print(return_type);
        p.print(" " + function_name + "() {");
        p.newLine();
        p.indent();
    }

    private static void endFunction(CodegenPrintWriter p) {
        p.outdent();
        p.print("}");
        p.newLine();
        p.newLine();
    }

    private static String fpName(SIRFilter f) {
        // assuming that a filter manages at most one C- stype file
        // pointer, what is that file pointer named?
        return f.getIdent() + "__fp";
    }

    /**
     * Create code for the init function of built-in filter
     * 
     * @param filter
     *            The filter for which we are creating init function
     * @param return_type
     *            The return type for the init function
     * @param function_name
     *            The name for the init function
     * @param selfID
     *            The unique identifier that FlatIRToCluster has assigned to the
     *            function
     * @param cleanupCode
     *            A list of statements run at "clean-up" time to which this
     *            code can add requests to close files, etc.
     * @param p
     *            The printer for outputting code for the function
     */
    static void predefinedFilterInit(SIRPredefinedFilter filter,
            CType return_type, String function_name, int selfID,
            List/*String*/ cleanupCode,CodegenPrintWriter p) {
        // No wrapper code around init since may need to create defns at file
        // level. We assume that the code generator will continue to
        // generate code for init before code for work, so scope of
        // any file-level code will include the work function.
        //
        // should replace this if sequence...

        p.print("// predefinedFilterInit " + filter.getIdent()
		+ " " + filter.getInputType().toString() + " -> " 
		+ filter.getOutputType().toString());
        p.newLine();
        if (filter instanceof SIRFileReader) {
            SIRFileReader fr = (SIRFileReader) filter;
            p.print("FILE* " + fpName(filter) + ";");
            p.newLine();
            p.newLine();
            startParameterlessFunction(ClusterUtils.CTypeToString(return_type),
                    function_name, p);
            p.print(fpName(fr) + " = fopen(\"" + fr.getFileName()
                    + "\", \"r\");");
            p.newLine();
            p.print("assert (" + fpName(filter) + ");");
            p.newLine();
            endFunction(p);

            String closeName = ClusterUtils.getWorkName(fr, selfID)
                               + "__close"; 
                                      
            startParameterlessFunction("void", closeName, p);
            p.print("fclose(" + fpName(fr) + ");");
            p.newLine();
            endFunction(p);
            
            cleanupCode.add(closeName+"();\n");
        } else if (filter instanceof SIRFileWriter) {
            SIRFileWriter fw = (SIRFileWriter) filter;
            p.print("FILE* " + fpName(fw) + ";");
            p.newLine();
            p.newLine();
            startParameterlessFunction(ClusterUtils.CTypeToString(return_type),
                    function_name, p);
            p.print(fpName(fw) + " = fopen(\"" + fw.getFileName()
                    + "\", \"w\");");
            p.newLine();
            p.print("assert (" + fpName(fw) + ");");
            p.newLine();
            endFunction(p);

            String closeName = ClusterUtils.getWorkName(fw, selfID)
            + "__close"; 
                   
            startParameterlessFunction("void", closeName, p);
            p.print("fclose(" + fpName(fw) + ");");
            p.newLine();
            endFunction(p);
            
            cleanupCode.add(closeName+"();\n");
        } else if (filter instanceof SIRIdentity) {
            p.print("//SIRIdentity init");
            p.newline();
            startParameterlessFunction(ClusterUtils.CTypeToString(return_type),
                    function_name, p);
            p.print(ClusterUtils.popName(selfID) + "("
                    + ClusterUtils.peekName(selfID) + ")");
            endFunction(p);
        } else if (filter instanceof SIRDummySink) {
            // TODO:  get right exception for unimplemented.
            throw new Error("Unsupported predefined filter "
                    + filter.getIdent());
        } else if (filter instanceof SIRDummySource) {
            throw new Error("Unsupported predefined filter "
                    + filter.getIdent());
        } else {
            // TODO:  get right unchecked exception for unextended code...
            throw new Error("Unknown predefined filter " + filter.getIdent());
        }

    }
}

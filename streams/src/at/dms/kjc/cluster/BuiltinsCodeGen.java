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
//        p.println("// predefinedFilterWork " + filter.getIdent());
        p.println("for (; 0 < ____n; ____n--) {");
        p.indent();

        // SIRFileReader
        if (filter instanceof SIRFileReader) {
            genFileReaderWork((SIRFileReader)filter,selfID,p);
            // SIRFileWriter
        } else if (filter instanceof SIRFileWriter) {
            genFileWriterWork((SIRFileWriter)filter,selfID,p);
           // SIRIdentity
        } else if (filter instanceof SIRIdentity) {
            p.println("  return " + ClusterUtils.pushName(selfID) + "("
                    + ClusterUtils.popName(selfID) + "());");

        } else if (filter instanceof SIRDummySink
                   || filter instanceof SIRDummySource) {
            // DummySource and SummySink do not appear in any of our
            // application code.  Are they part of the language?
            // TODO: get right exception for unimplemented.
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

    // utility routines for framing functions (only parameterless functions 
    // for now)

    private static void startParameterlessFunction(String return_type,
            String function_name, CodegenPrintWriter p) {
        p.print(return_type);
        p.println(" " + function_name + "() {");
        p.indent();
    }

    private static void endFunction(CodegenPrintWriter p) {
        p.outdent();
        p.println("}");
        p.newLine();
    }

    // utility routines for FileReader and FileWriter.
    
    private static String fpName(SIRFilter f) {
        // assuming that a filter manages at most one C- stype file
        // pointer, what is that file pointer named?
        return f.getIdent() + "__fp";
    }

    private static String bitsToGoName(SIRFilter f) {
        // assuming that a filter manages at most one C- stype file
        // pointer, what is that file pointer named?
        return f.getIdent() + "__bits_to_go";
    }

    private static String theBitsName(SIRFilter f) {
        // assuming that a filter manages at most one C- stype file
        // pointer, what is that file pointer named?
        return f.getIdent() + "__the_bits";
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

        p.print("// predefinedFilterInit " + filter.getIdent()
		+ " " + filter.getInputType().toString() + " -> " 
		+ filter.getOutputType().toString());
        p.newLine();
        if (filter instanceof SIRFileReader) {
            genFileReaderInit((SIRFileReader)filter, return_type, 
                    function_name, selfID, cleanupCode, p);
        } else if (filter instanceof SIRFileWriter) {
            genFileWriterInit((SIRFileWriter)filter, return_type, 
                    function_name, selfID, cleanupCode, p);
        } else if (filter instanceof SIRIdentity 
                   || filter instanceof SIRDummySink
                   || filter instanceof SIRDummySource) {
            // all of these have filters produce empty init functions.
            startParameterlessFunction(ClusterUtils.CTypeToString(return_type),
                    function_name, p);
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
   
    // should be able to change this to longer numeric type for less
    // frequent I/O if we have endian-ness correct.
    private static final String bits_type = "unsigned char";
    
    /*
     * File reader code currently works by using fread -- check on buffering 
     * efficiency...
     * 
     * The File* is declared outside any function / method.
     * 
     * There is special case code for FileReader<bit> since individual
     * bits can not be read in by any system routine that I know.
     * 
     * With most bit streams that I (AD) have seen, the order is
     * little-endian in words, low-to-high bit in bytes.
     * This code -- matching Matt's implementation for the library
     * is a bit odd: endian-ness depends on what fwrite -- my
     * laziness, should always be little-endian -- but bits are read
     * and written from high to low.  So byte 0b10011111
     * is read as 1, 0, 0, 1, 1, 1, 1, 1 and is written back as
     * 0b10011111.  This looks a little odd if a bit stream does not
     * end on a byte boundary: writing stream 1,0,1,1 results in 0b10110000 
     * 
     * If we change the number of bits bufferred from 1 byte to a larger
     * integer type, we will have to worry about endian-ness on disk.
     * This code was designed for .BMP files, which are little-endian.
     * on disk.  We will also have to worry about file lengths: Unix allows
     * file lengths in bytes, this will involve some extra code for not writing
     * an excessive number of bytes on the last write, causing the file lengths
     * to be dependent on the size of 'bits_type'.
     * 
     */
    private static void genFileReaderWork(SIRFileReader filter, 
            int selfID,
            CodegenPrintWriter p) {
        String theType = "" + filter.getOutputType();
        if (theType.equals("bit")) {
            // the bit type is special since you can not just read or
            // write a bit.  It requires bufferring in some larger
            // integer type.
            String bits_to_go = bitsToGoName(filter);
            String the_bits = theBitsName(filter);
            
            p.println("static " + bits_type + " " + the_bits + " = 0;");
            p.println("static int " + bits_to_go + " = 0;");
            p.newline();
            p.println("if (" + bits_to_go + " == 0) {");
            p.indent();
            p.println("fread(" + "&"+ the_bits + ", " 
                        + "sizeof(" + the_bits + "),"
                        + " " + "1, " 
                        + fpName(filter) + ");");
            p.println(bits_to_go + " = 8 * sizeof("+ the_bits + ");");
            p.outdent();
            p.println("}");
            p.println(ClusterUtils.pushName(selfID) 
                      + "((" + the_bits +" & (1 << (sizeof(" + the_bits + ") * 8 - 1))) ? 1 : 0);");
            p.println(the_bits + " <<= 1;");
            p.println(bits_to_go + "--;");
        } else {
            // push into a location.
            p.println(theType + " v;");
            p.println("fread(" + "&v, " + "sizeof(v), " + "1, " 
                    + fpName(filter) + ");");
            p.println(ClusterUtils.pushName(selfID) + "(v);");
        }
    }
    
    /*
     * When generating (no) code for init routine, also generate File*
     * and close() for file.
     */
    private static void genFileReaderInit(SIRFileReader fr,
            CType return_type, String function_name, int selfID,
            List/*String*/ cleanupCode,CodegenPrintWriter p) {
        p.print("FILE* " + fpName(fr) + ";");
        p.newLine();
        p.newLine();
        startParameterlessFunction(ClusterUtils.CTypeToString(return_type),
                function_name, p);
        p.print(fpName(fr) + " = fopen(\"" + fr.getFileName()
                + "\", \"r\");");
        p.newLine();
        p.print("assert (" + fpName(fr) + ");");
        p.newLine();
        endFunction(p);

        String closeName = ClusterUtils.getWorkName(fr, selfID)
                           + "__close"; 
                                  
        startParameterlessFunction("void", closeName, p);
        p.print("fclose(" + fpName(fr) + ");");
        p.newLine();
        endFunction(p);
        
        cleanupCode.add(closeName+"();\n");

    }
    

    /*
     * File writer code currently works by using fread -- check on buffering 
     * efficiency...
     * 
     * The File* is declared outside any function / method.
     * 
     * There is special case code for FileReader<bit> since individual
     * bits can not be read in by any system routine that I know.
     * The current bits and the count of unprocessed bits are created
     * outside of 
     */
    
    private static void genFileWriterWork(SIRFileWriter fw, int selfID,
            CodegenPrintWriter p) {
        String theType = "" + fw.getInputType();
        if (theType.equals("bit")) {
            // the bit type is special since you can not just read or
            // write a bit.  It requires buffering in some larger
            // integer type.
            String bits_to_go = bitsToGoName(fw);
            String the_bits = theBitsName(fw);
            
            p.println(the_bits + " = (" + bits_type + ") ((" + the_bits 
                    + " << 1) | (" + ClusterUtils.popName(selfID) 
                    + "() & 1));");
            p.println(bits_to_go + "--;");
            p.println("if (" + bits_to_go + " == 0) {");
            p.indent();
            p.println("fwrite(" + "&" + the_bits + ", " 
                    + "sizeof(" + the_bits +"), " + "1, " + fpName(fw)
                    + ");");
            p.println(the_bits + " = 0;");
            p.println(bits_to_go + " = 8 * sizeof(" + the_bits + ");");
            p.outdent();
            p.println("}");
        } else {
            // pop into a location.
            p.println(theType + " v = (" + theType + ")("
                    + ClusterUtils.popName(selfID) + "());");
            p.println("fwrite(" + "&v, " + "sizeof(v), " + "1, " + fpName(fw)
                    + ");");
        }
    }
    
    /*
     * When generating (no) code for init routine, also generate File*
     * and close() for file.
     */
    private static void genFileWriterInit(SIRFileWriter fw,
            CType return_type, String function_name, int selfID,
            List/*String*/ cleanupCode,CodegenPrintWriter p) {

        p.println("FILE* " + fpName(fw) + ";");
        String theType = "" + fw.getInputType();
        String bits_to_go = bitsToGoName(fw);
        String the_bits = theBitsName(fw);

        if (theType.equals("bit")) {
            // the bit type is special since you can not just read or
            // write a bit.  It requires buffering in some larger
            // integer type.

            p.println(bits_type + " " + the_bits + " = 0;");
            p.println("int " + bits_to_go + " = 8 * sizeof(" + the_bits + ");");
        }
        p.newLine();

        startParameterlessFunction(ClusterUtils.CTypeToString(return_type),
                function_name, p);
        p.println(fpName(fw) + " = fopen(\"" + fw.getFileName()
                + "\", \"w\");");
        p.println("assert (" + fpName(fw) + ");");
        endFunction(p);

        String closeName = ClusterUtils.getWorkName(fw, selfID)
        + "__close"; 
               
        startParameterlessFunction("void", closeName, p);
        if (theType.equals("bit")) {
            p.println("if (" + bits_to_go + " != 8 * sizeof(" 
                    + the_bits + ")) {");
            p.indent();
            p.println(the_bits + " = " + the_bits + " << " + bits_to_go + ";");
            p.println("fwrite(" + "&" + the_bits + ", " 
                    + "sizeof(" + the_bits +"), " + "1, " + fpName(fw)
                    + ");");
            p.outdent();
            p.println("}");
        } 
        p.println("fclose(" + fpName(fw) + ");");
        endFunction(p);
        
        cleanupCode.add(closeName+"();\n");
    }
    

}
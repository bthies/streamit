package at.dms.kjc.common;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.util.Utils;

/**
 * This class gathers common code for printing on the raw simulator,
 * including C code defines and SIRPrintStatement code generation.
 * It is used by the old RawBackend, SpaceDynamic, and SpaceTime.
 * 
 * @author mgordon
 *
 */
public class RawSimulatorPrint {
    /** because the raw simulator/hw does not support easy printing 
    of strings, replace all string printing with a debug callback
    with a unique negative int id **/
    private static int string_print_ID = -1;
    
    public static String STREAMIT_PRINT_INT = "streamit_print_int";
    public static String STREAMIT_PRINT_FLOAT = "streamit_print_float";
    
    /**
     * Return the header (c #define statements) that defines the print 
     * macros for integers and floats.
     * 
     * @return the header that defines the print macros for integers and floats.
     */
    public static String getHeader() 
    {
        StringBuffer ret = new StringBuffer();
        ret.append("\n#define " + STREAMIT_PRINT_INT + "(print_me) __asm__ volatile (\"magc $0, %0, 21\" : \\\n" +
                ": \"r\"(print_me) );\n");

        ret.append("\n#define " + STREAMIT_PRINT_FLOAT + "(print_me) __asm__ volatile (\"magc $0, %0, 22\" : \\\n" +
                ": \"r\"(print_me) );\n\n");
        
        return ret.toString();
    }
    
    /**
     * Return the bC code that will define the magic print handler and
     * register the handler.
     * 
     * @return the bC code that will define the magic print handler and
     * register the handler.
     */
    public static String bCMagicHandler() {
        StringBuffer ret = new StringBuffer();
        
        ret.append("fn streamit_print(procNum, rs, imm, result_ptr)\n");
        ret.append("{\n");
        ret.append("  local proc, time_hi, time_lo;\n");  //, outputs;\n");
        ret.append("  if (imm != 21 && imm != 22)\n");
        ret.append("    return 0;\n");
        
        ret.append("  //get the proc\n");
        ret.append("  proc = Machine_GetProc(machine, procNum);\n");
        ret.append("  //get the cycle\n");
        ret.append("  Proc_GetCycleCounter(proc, &time_hi, &time_lo);\n");

        //handle the --outputs options
        
        //ret.append("  outputs = get_variable_val(\"gStreamItOutputs\");\n");
        ret.append("  if (gStreamItOutputs == 0) {\n");
        ret.append("     gInterrupted = 1;\n");
        ret.append("     exit_now(0);\n");
        ret.append("   }\n");
        ret.append("   else if (gStreamItOutputs > 0) {\n");
        ret.append("      gStreamItOutputs--;\n");
        ret.append("   }\n");
        ret.append("\n");
        
        //print the value
        ret.append("  if (imm == 21) {  //print an int\n");
        ret.append("    printf(\"[%d: %d]: %d\\n\", procNum, time_lo, rs);\n");
        ret.append("  }\n");
        ret.append("  else {   //print a float\n");
        ret.append("    printf(\"[%d: %d]: %f\\n\", procNum, time_lo, double(rs));\n");
        ret.append("  }\n");
        ret.append("  return 1;\n");
        ret.append("}\n");

        ret.append("{\n");
        ret.append("  //register the magic instruction\n");
        ret.append("  listi_add(get_variable_val(\"gMagicInstrHMS\").theList, & fn (procNum, rs, imm, result_ptr)\n");
        ret.append("  {\n");
        ret.append("  return streamit_print(procNum, rs, imm, result_ptr);\n");
        ret.append("  });\n");
        ret.append("}\n");

        return ret.toString();
    }
    
    /**
     * Generate code for an SIRPrintstatement when we are targeting the
     * raw simulator.  We use two magic instructions, one to print int types and
     * one to print floats, to pass the value we would like to print to the 
     * simulator runtime so it can display it.  We convert each print
     * statement into a call of a macro defined to call the appropriate 
     * magic instruction.
     * 
     * @param self The print statement
     * @param exp The expression to print
     * @param p The CodeGenPrinter to append the code to.
     * @param toC The parent visiter used to visit the exp.
     */
    public static void visitPrintStatement(SIRPrintStatement self,
            JExpression exp, CodegenPrintWriter p, ToC toC) {
        //get the type for the target of the print statement
        CType type = null;

        try {
            type = exp.getType();
        }
        catch (Exception e) {
            System.err.println("Cannot get type for print statement");
            type = CStdType.Integer;
        }
        
        //now
        if (type.equals(CStdType.Byte) ||
                type.equals(CStdType.Integer) ||
                type.equals(CStdType.Short) ||
                type.equals(CStdType.Boolean) ||
                type.equals(CStdType.Char) ||
                type.equals(CStdType.Long))
        {
            p.print(STREAMIT_PRINT_INT + "(");
            exp.accept(toC);
            p.print(");");
        }
        else if (type.equals(CStdType.Float))
        {
            p.print(STREAMIT_PRINT_FLOAT + "(");
            exp.accept(toC);
            p.print(");");
        }
        else if (type.equals(CStdType.String)) 
        {
            p.print(STREAMIT_PRINT_INT + "(/*");
            exp.accept(toC);
            p.print("*/" + string_print_ID + ");");
            string_print_ID--;
            return;
        }
        else
        {
            System.err.println("Unprintable type: Assuming it is can be printed as an int.");
            p.print(STREAMIT_PRINT_INT + "(");
            exp.accept(toC);
            p.print(");");
            //Utils.fail("Unprintable Type");
        }
    }
}

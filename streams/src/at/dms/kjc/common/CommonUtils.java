package at.dms.kjc.common;
 
import at.dms.kjc.CType;
import at.dms.kjc.CArrayType;
import at.dms.kjc.common.CodegenPrintWriter;
/**
 * Some public static utility functions pulled out of other routines.
 */
public class CommonUtils {

    /**
     * Turn a CType into a string for includion in C or C++ code generation.
     *
     * @param s            a CType.
     * @param hasBoolType  if true then Java 'boolean' becomse 'bool' for C++
     *                     if false then Java 'boolean' becomse 'int' for C
     *
     */
    public static String CTypeToString(CType s, boolean hasBoolType) {
	if (s instanceof CArrayType){
	    return CTypeToString(((CArrayType)s).getElementType(), hasBoolType)  + "*";
	} else if (s.getTypeID() == CType.TID_BOOLEAN) {
	    return hasBoolType ? "bool" : "int";
	} else if (s.toString().endsWith("Portal")) {
	    // ignore the specific type of portal in the C library
	    return "portal";
	} else {
           return s.toString();
	}
    } 

    /**
     * Print a CType to a specified CodegenPrintWriter
     *
     * @param s            a CType to be printed.
     * @param cpw          a CodePrintWriter to print the type
     * @param hasBoolType  if true then Java 'boolean' becomse 'bool' for C++
     *                     if false then Java 'boolean' becomse 'int' for C
     */
    public static void printCTypeString(CType s, CodegenPrintWriter cpw,
				   boolean hasBoolType) {
	cpw.print(CTypeToString(s, hasBoolType));
    }

}

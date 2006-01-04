package at.dms.kjc.common;
 
import at.dms.kjc.CType;
import at.dms.kjc.CArrayType;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JClassExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JThisExpression;
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
    
    
    /**
     *  Take an expression that could occur on the lhs of an assignment
     * and drill down to find the name of the field or local involved.
     * 
     * These expressions have the form
     * (Local | (This|ClassName).field | ...) ([Expression] | .StructureField)*
     * 
     * 
     * @param expr   An expression that could occur on the left-hand side
     *               of an assignment
     *               
     * @return       The root field (including this or classname) or 
     *               local expression, or whatever else was found after
     *               peeling off all structure field modifiers and array 
     *               offset modifiers.  (There are assignments of the form
     *               SIRPortal = SIRPortalCreation and such like, so this
     *               is not guaranteed to return a field or local.)
     */

    public static JExpression lhsBaseExpr (JExpression expr) {
        if (expr instanceof JArrayAccessExpression) {
            return lhsBaseExpr(((JArrayAccessExpression)expr).getPrefix());
        } 
        if (expr instanceof JFieldAccessExpression) {
            JFieldAccessExpression fexpr = (JFieldAccessExpression)expr;
            if (fexpr.getPrefix() instanceof JThisExpression
                || fexpr.getPrefix() instanceof JClassExpression) {
                // field of named class or of 'this' class: is as
                // far as we can go.
                return (JExpression)fexpr;
            } else {
                return lhsBaseExpr(fexpr.getPrefix());
            }
        } 
        if (expr instanceof JLocalVariableExpression) {
            return expr;
        }
        // There are some other odd left-hand sides such as 
        // SIRPortal = SIRCreatePortal
        return expr;
    }

}

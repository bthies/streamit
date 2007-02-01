package at.dms.kjc.common;
 
import at.dms.kjc.CStdType;
import at.dms.kjc.JIntLiteral;
import at.dms.kjc.JExpression;
import at.dms.kjc.CType;
import at.dms.kjc.CBitType;
import at.dms.kjc.CArrayType;
import at.dms.kjc.CEmittedTextType;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JClassExpression;
import at.dms.kjc.JFieldDeclaration;
//import at.dms.kjc.JExpression;
import at.dms.kjc.sir.SIRStructure;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.KjcOptions;
//import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRJoiner;
import at.dms.kjc.sir.SIRSplitter;
/**
 * Some public static utility functions pulled out of other routines.
 */
public class CommonUtils {

    /**
     * Make an array of int's from an array of JExpression's for array dimensions.
     * 
     * @param dims An array of JExpressions that shuold all be  JIntLiteral's
     * @return array of ints, or asserts that the array has a non-int dimension.
     */
    public static int[] makeArrayInts(JExpression[] dims) {
        int[] ret = new int[dims.length];
    
        for (int i = 0; i < dims.length; i++) {
            assert dims[i] instanceof JIntLiteral :
                "Length of array dimension is not an int literal";
            ret[i] = ((JIntLiteral)dims[i]).intValue();
        }
        return ret;
    }


    /**
     * Return the underlying CType for a Ctype.
     * 
     * For anything except an array this is  no-op.
     * For an array, return the array's base type (which according to CArrayType
     * is not allowed to be another array type).
     * 
     * @param type a CType
     * @return the underlying type
     */
    public static CType getBaseType (CType type) 
    {
        if (type.isArrayType())
            return ((CArrayType)type).getBaseType();
        return type;
    }

    /**
     * Turn a CType into a string for inclusion in C or C++ code generation.
     * N-dimensional arrays have N "*" in type, this is the type needed if
     * as a return type for a N-dimensional array if you are going to copy it
     * element-wise.
     * Note: SIRStructure type is not handled (and is not a CType)
     *
     * @param s            a CType.
     * @param hasBoolType  if true then Java 'boolean' becomse 'bool' for C++
     *                     if false then Java 'boolean' becomse 'int' for C
     *
     */
    public static String CTypeToString(CType s, boolean hasBoolType) {
        if (s instanceof CArrayType){
            // getElementType rather than getBaseType to go one dimension at a time.
            return CTypeToString(((CArrayType)s).getElementType(), hasBoolType)  + "*";
        } else if (s.getTypeID() == CType.TID_BOOLEAN) {
            return hasBoolType ? "bool" : "int";
        } else if (s.toString().endsWith("Portal")) {
            // ignore the specific type of portal in the C library
            return "portal";
        } else if (s instanceof CBitType) {
            // for now convert bit's to int's
            return "int";
        } else if (s instanceof CEmittedTextType) {
            String typ = "";
            for (Object part : ((CEmittedTextType)s).getParts()) {
                if (part instanceof String) {
                    typ += (String)part;
                } else if (part instanceof CType) {
                    typ += CTypeToString(s,hasBoolType);
                } else {
                    throw new AssertionError("object has unexpected type " + part);
                }
            }
            return typ;
        } else {
            return s.toString();
        }
    } 
    /**
     * Turn a CType into a string for inclusion in C or C++ code generation.
     * (multi-dimensional arrays have a single "*" in type.)
     * Note: SIRStructure type is not handled (and is not a CType)
     *
     * These types are useful for C or C++ return types you get e.g. 
     * int* rather than int[4][4].   This is the correct C++ type for 
     * returning an array pointer (actually for returning &array[0][0]).
     * 
     * If you want the array type with dimensions, (for a argument type in C++)
     * use {@link #declToString(CType, String, boolean) declToString} as
     *     declToString(type, "", tf)
     *
     * @param s            a CType.
     * @param hasBoolType  if true then Java 'boolean' becomse 'bool' for C++
     *                     if false then Java 'boolean' becomse 'int' for C
     * @return A string representation suitable for C or C++ return types.
     */
    public static String CTypeToStringA(CType s, boolean hasBoolType) {
        if (s instanceof CArrayType){
            return CTypeToString(((CArrayType)s).getBaseType(), hasBoolType)  + "*";
        } else if (s.getTypeID() == CType.TID_BOOLEAN) {
            return hasBoolType ? "bool" : "int";
        } else if (s.toString().endsWith("Portal")) {
            // ignore the specific type of portal in the C library
            return "portal";
        } else if (s instanceof CBitType) {
            // for now convert bit's to int's
            return "int";
        } else if (s instanceof CEmittedTextType) {
            String typ = "";
            for (Object part : ((CEmittedTextType)s).getParts()) {
                if (part instanceof String) {
                    typ += (String)part;
                } else if (part instanceof CType) {
                    typ += CTypeToString(s,hasBoolType);
                } else {
                    throw new AssertionError("object has unexpected type " + part);
                }
            }
            return typ;
        } else {
            return s.toString();
        }
    } 

    /**
     * Returns a declaration for the given type with the given
     * identifier.  Returns int x[10][10] for arrays.
     * 
     * @param s      the type to declare
     * @param ident  the identifier to declare
     * @param hasBoolType  if true then Java 'boolean' becomse 'bool' for C++
     *                     if false then Java 'boolean' becomse 'int' for C
     */
    public static String declToString(CType s, String ident, boolean hasBoolType) {
        StringBuffer result = new StringBuffer();
        if (s instanceof CArrayType) {
            // special case: "int main(char** argv)" does not have
            // static bounds in array
            if (ident.equals("argv")) {
                // first print type
                result.append(CTypeToString(((CArrayType)s).getBaseType(), hasBoolType));
                // then *
                for (int i = 0; i < ((CArrayType)s).getArrayBound(); i++) {
                    result.append("*");
                }
                // then identifier
                result.append(" ");
                result.append(ident);
            } else {
                // first print type
                result.append(CTypeToString(((CArrayType)s).getBaseType(), hasBoolType));
                // the identifier
                result.append(" ");
                result.append(ident);
                // then dims
                JExpression[] dims = ((CArrayType)s).getDims();
                for (int i = 0; i < dims.length; i++) {
                    result.append("[");
                    // require that dimensions are resolved to int
                    // literals.  It might be more general to send a
                    // visitor through, but some of the cluster code
                    // generators do not have visitors handy (e.g.,
                    // ClusterCode.java)
                    assert dims[i] instanceof JIntLiteral : "Array dimension is not int literal during codegen, instead is: " + dims[i];
                    result.append(((JIntLiteral)dims[i]).intValue());
                    result.append("]");
                }
            }
        } else {
            // print type
            result.append(CTypeToString(s, hasBoolType));
            // then identifier
            result.append(" ");
            result.append(ident);
        }
        return result.toString();
    }
    
    /**
     * Factor out printing (or misprinting) of SIRStruct typedefs from various backends
     * @param strct a SIRStructure to print
     * @param hasBoolType  if true then Java 'boolean' becomes 'bool' for C++
     *                     if false then Java 'boolean' becomes 'int' for C
     * @return printable C or C++ representation, no final newline.
     */
    public static String structToTypedef(SIRStructure strct, boolean hasBoolType) {
        StringBuffer retval = new StringBuffer();
        retval.append("typedef ");
        retval.append(strct.isCUnion()? "union" : "struct");
        retval.append(" __");
        retval.append(strct.getIdent());
        retval.append(" {\n");
        for (JFieldDeclaration field : strct.getFields()) {
            retval.append("\t");
            retval.append(declToString(field.getType(),field.getVariable().getIdent(), hasBoolType));
            retval.append(";\n");
        }
        retval.append("} ");
        retval.append(strct.getIdent());
        retval.append(";");
        
        return retval.toString();
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
    /**
     * Get the output type of a joiner in a Flatnode representation.
     * <br/>
     * The type of a joiner is the output type of the first filter found searching
     * back from the joiner.  (Actually a bit more complicated: it searches back in
     * the flat graph for a non-void input to the joiner, and returns that as the
     * joiner type.  Only id all inputs to the joiner have output type of void, or 
     * are null, will getJoinerType return a void type for the joiner.  This complexity
     * is to deal with split-joins that decimiate the initial portion of the data, in which
     * case the initial input to the joiner would be void, where some other input would
     * be non-void.)
     * 
     * Edge cases: If you pass this a null joiner, you will get back void.
     * If you pass this method a FlatNode that is not a joiner, the output
     * and side effects are undefined.
     * 
     * @param joiner a joiner in a FlatNode representation
     * @return a CType
     */
       public static CType getJoinerType(FlatNode joiner) 
       {
           if (joiner == null) {return CStdType.Void;}
           for (int i = 0; i < joiner.inputs; i++) {
               if (joiner.incoming[i] != null) {
                   CType typ = getOutputType(joiner.incoming[i]);
                   if (typ != CStdType.Void) {
                       return typ;
                   }
               }
           }
           return CStdType.Void;
       }
       
       /**
        * Get the output type of any FlatNode element (filter, splitter, joiner).
        *
        * The output type of a filter is stored in the filter.
        *
        * The output type of a splitter is the output type of its incoming edge:
        * If the splitter has 0 total outgoing weight then its incoming edge,
        * if any, should be null, so return the Void type.
        * 
        * The output type of a joiner is that of the first filter found above the
        * joiner.
        *
        * @param node a FlatNode (and not null)
        * @return a CType
        */
       public static CType getOutputType(FlatNode node) {
           if (node.contents instanceof SIRFilter)
               return ((SIRFilter)node.contents).getOutputType();
           else if (node.contents instanceof SIRJoiner)
               return getJoinerType(node);
           else if (node.contents instanceof SIRSplitter) {
               if (node.getTotalOutgoingWeights() == 0) {
                   return CStdType.Void;
               }
               return getOutputType(node.incoming[0]);
           } else {
               assert false: "Cannot get output type for this node";
               return null;
           }
       }


    /**
     * Print a string only if compiling with --debug 
     * @param s the debugging string to print.
     */
    public static void println_debugging(String s) {
        if (KjcOptions.debug)
            System.out.println(s);
    }

}

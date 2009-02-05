package at.dms.kjc.smp;

import at.dms.kjc.*;

/**
 * Some utility functions
 * 
 * @author mgordon
 *
 */
public class Util {
    public static JStatement toStmt(String txt) {
        return new JExpressionStatement(new JEmittedTextExpression(txt));
    }
    
    /**
     * For type return the number of words that it occupies.
     * 
     * @param type The type.
     * @return The number of words occupied by type.
     */
    public static int getTypeSize(CType type) {

        if (!(type.isArrayType() || type.isClassType()))
            return 1;
        else if (type.isArrayType()) {
            int elements = 1;
            int dims[] = Util.makeInt(((CArrayType) type).getDims());

            for (int i = 0; i < dims.length; i++) {
                elements *= dims[i];
            }
            return elements;
        } else if (type.isClassType()) {
            int size = 0;
            for (int i = 0; i < type.getCClass().getFields().length; i++) {
                size += getTypeSize(type.getCClass().getFields()[i].getType());
            }
            return size;
        }
        assert false : "Unrecognized type";
        return 0;
    }

    /**
     * Given an array of jexpressions each of type jintliteral, return an int array of the literals
     * 
     * @param dims The array of jintliterals
     * @return the int array
     */
    public static int[] makeInt(JExpression[] dims) {
        int[] ret = new int[dims.length];

        for (int i = 0; i < dims.length; i++) {
            assert dims[i] instanceof JIntLiteral : "Array length for tape declaration not an int literal";
            ret[i] = ((JIntLiteral) dims[i]).intValue();
        }
        return ret;
    }
    
}

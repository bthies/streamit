/**
 * <br/>$Id$
 */
package at.dms.kjc;

import at.dms.compiler.UnpositionedError;
import at.dms.util.SimpleStringBuffer;

/**
 * Short vectors of base types.
 * Not from KJC.  StreamIt only.
 * @author dimock
 *
 */
public class CVectorType extends CType {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * For cloner only.
     */
    protected CVectorType() {}

    private CNumericType base_type;
    private int width;
    private int width_in_base;
    
    /**
     * Construct a vector for short vrctor hardware (SSE, ...).
     * @param baseType : base type, just CSTdType.Integer or CStdType.Float for now.
     * @param width : vector width in bytes.
     */
    public CVectorType(CNumericType baseType, int width) {
        
        super(TID_VECTOR);
        assert baseType != null;
        base_type = baseType;
        this.width = width;
    }

    /** accessor for base type */
    public CType getBaseType() {
        return base_type;
    }
    
    /** accessor for width in units of base type */
    public int getWidthInBase() {
        return width / base_type.getSizeInC();
    }
    
    /** accessor for width in bytes */
    public int getWidth () {
        return width;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.CType#appendSignature(at.dms.util.SimpleStringBuffer)
     */
    @Override
    protected void appendSignature(SimpleStringBuffer buffer) {
        assert false : "No reasonable appendSignature for CVectorType";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.CType#checkType(at.dms.kjc.CContext)
     */
    @Override
    public void checkType(CContext context) throws UnpositionedError {
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.CType#getSize()
     */
    @Override
    public int getSize() {
        // seems to be in word  (4-byte) units
        return ((width + 3) / 4);
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.CType#getSizeInC()
     */
    @Override
    public int getSizeInC() {
        // seems to be in byte units
        // Does alignment play a role here?
        return ((width + 3) / 4) * 4;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.CType#isAssignableTo(at.dms.kjc.CType)
     */
    @Override
    public boolean isAssignableTo(CType dest) {
        // no need for this: no Java narrowings or widenings.
        return false;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.CType#isCastableTo(at.dms.kjc.CType)
     */
    @Override
    public boolean isCastableTo(CType dest) {
        // no Java casts
        return false;
    }

    public boolean equals (CType other) {
        if (other instanceof CVectorType) {
            return base_type.equals(
                    ((CVectorType)other).getBaseType());
        } else {
            return false;
        }
    }
    
    /**
     * The returned string can not be used as a C or C++ type
     * unless the result of calling {@link #typedefString} occurrs
     * earlier in the code.
     * 
     * The gcc extension for vector types would require a 
     * declaration of a vector of 4 floats with name foo to be
     * something like:
     * <pre>
     * union foo {
     *   float v __attribute__ ((vector_size (16))); //foo.v as vector
     *   float a [4]; // foo.a[i] to load or store elements
     * }
     * </pre>
     * Presumably we will handle this as a typedef.
     * @see at.dms.kjc.CType#toString()
     */
    @Override
    public String toString() {
        return "__v" +  getWidthInBase() + base_type.getSignature();
    }

    /** 
     * Create C (or C++) typedef allowing {@link toString()} to be used as a type. 
     * @return typedef with ";" but no line terminator.
     */
    public String typedefString() {
        return "typedef union { "
        + base_type.toString()    // valid C of the currently allowed base types
        + " v __attribute__ ((vector_size (" + width + "))); "
        + base_type.toString()
        + " a[" + getWidthInBase() + "];} "
        + toString() + ";";
    }
    /**
     * Make expression refer to position in a vector (as an array element).
     * @param expr
     * @param n
     * @return the reference to expr as array element n of the vector.
     */
    public static JExpression asArrayRef(JExpression expr, int n) {
        JExpression retval = new JArrayAccessExpression(
                new JFieldAccessExpression(expr, "a"),
                new JIntLiteral(n));
        return retval;
    }
    
    /**
     * Make expression refer to vector (as a vector).
     * @param expr  expression to turn into reference to vector as vector.
     * @return the reference to expr as a vector
     */
    public static JExpression asVectorRef(JExpression expr) {
        JExpression retval = 
            new JFieldAccessExpression(expr, "v");
        return retval;
    }
    
    /**
     * Turn numeric types into vector types, array types into array types of vector types.
     * @param inputType The input type.
     * @return the type using a vector type
     */
    public static CType makeVectortype(CType inputType) {
        if (inputType instanceof CNumericType) {
            return new CVectorType((CNumericType)inputType, KjcOptions.vectorize); 
        } else if (inputType instanceof CArrayType) {
            CType baseType = ((CArrayType)inputType).getBaseType();
            if (baseType instanceof CNumericType) {
                return 
                    new CArrayType(new CVectorType((CNumericType)baseType, KjcOptions.vectorize),
                    ((CArrayType)inputType).getArrayBound(),
                    ((CArrayType)inputType).getDims());
            }   
        }
        assert false : "Unexpected type " + inputType.toString();
        return null;
    }


}

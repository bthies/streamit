/**
 * <br/>$Id$
 */
package at.dms.kjc;

import at.dms.compiler.UnpositionedError;
import at.dms.util.SimpleStringBuffer;
import java.util.*;
import at.dms.kjc.sir.SIRStructure;

/**
 * Short vectors of base types.
 * Not from KJC.  StreamIt only.
 * <br/>
 * Vector types come at two levels: CVectorType is a 
 * type that allows individual vector elements to be loaded (gathered)
 * and stored (scatterred) by referencing the vector through {@link #asArrayRef(JExpression, int)}
 * and allows arithmetic through {@link #asVectorRef(JExpression)}.
 * a CVectorTypeLow does not allow loading or storing. 
 * A CVectorType always referes to a CLowVectorType.
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
    private CVectorType() {}

    private CVectorTypeLow low_type;
    
    /**
     * Construct a vector for short vector hardware (SSE, ...).
     * @param baseType : base type, just CSTdType.Integer or CStdType.Float for now.
     * @param width : vector width in bytes.
     */
    public CVectorType(CNumericType baseType, int width) {
        super(TID_VECTOR);
        low_type = new CVectorTypeLow(baseType,width);
    }

    /** Get type for low-level implementation */
    public final CVectorTypeLow getLowType() {
        return low_type;
    }
    
    /** accessor for base type */
    public final CType getBaseType() {
        return low_type.getBaseType();
    }
    
    /** accessor for width in units of base type */
    public final int getWidthInBase() {
        return low_type.getWidthInBase();
    }
    
    /** accessor for width in bytes */
    public final int getWidth () {
        return low_type.getWidth();
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
    public final int getSize() {
        // seems to be in word  (4-byte) units
        return low_type.getSize();
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.CType#getSizeInC()
     */
    @Override
    public final int getSizeInC() {
        // seems to be in byte units
        // Does alignment play a role here?
        return low_type.getSizeInC();
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
            return low_type.equals(
                    ((CVectorType)other).getLowType());
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
        return "__v" +  getWidthInBase() + low_type.getBaseType().getSignature();
    }
    
    
    /** 
     * Create C (or C++) typedef allowing {@link toString()} to be used as a type. 
     * @return typedefs with ";" and internal line terminators but no final line terminator.
     */
    public String typedefString() {
        return 
        // typedef for vector
        low_type.typedefString() + "\n"
        // typedef for mixed vector and array for manipulating elements
        + "typedef union {"
        + getBaseType().toString()
        + " a[" + getWidthInBase() + "]; "
        + low_type.toString()
        + " v;} "
        + toString() + ";";
    }
    
    static HashSet<SIRStructure> bufferUnionTypes = new HashSet<SIRStructure>();
    
    /**
     * Returns a collection of SIRStructure's correspoding to C unions for
     * peek or poke buffers.
     * @return
     */
    public static Iterable<SIRStructure> structDefs() {
        return bufferUnionTypes;
    }
    
    /**
     * Add a SIRStructure corresponding to the type of a peek or poke buffer.
     * @param struct
     */
    public static void addBufferStructDef(SIRStructure struct) {
        bufferUnionTypes.add(struct);
    }
    
    /**
     * Return vector stuff to be included in a header file (including any final ";" and "\n").
     * @return strings needed in a header file that aren't from @{link #typedefString() typedefString} 
     * but does include definitions from @{link #structDefs()}
     */
    public static String miscStrings() {
        String includeString;
        includeString = "";
        if (KjcOptions.cell_vector_library) {
            String streamitHome = System.getenv("STREAMIT_HOME");
            includeString = "#include \"" +
            ((streamitHome != null) ? streamitHome + "/" : "") +
            "misc/vectorization.h\"\n";
        }
        for (SIRStructure struct : bufferUnionTypes) {
            includeString += at.dms.kjc.common.CommonUtils.structToTypedef(struct,false);
            includeString += "\n";
        }
        return includeString;
    }
    
    /**
     * Make expression refer to position in a vector (as an array element) for load / store.
     * You will probably want to change the type of the expression to a vector type before calling this.
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
     * Make expression refer to vector (as a vector) for arithmetic.
     * You will probably want to change the type of the expression to a vector type before calling this.
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

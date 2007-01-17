package at.dms.kjc;

import at.dms.compiler.UnpositionedError;
import at.dms.util.SimpleStringBuffer;
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

public class CVectorTypeLow extends CType {

    private CNumericType base_type;
    private int width; //vector width in bytes
    //private int width_in_base;

    /**
     * For cloner only.
     */
    private CVectorTypeLow() {}
    
    /**
     * Construct a vector for short vector hardware (SSE, ...).
     * @param baseType : base type, just CSTdType.Integer or CStdType.Float for now.
     * @param width : vector width in bytes.
     */
    public CVectorTypeLow(CNumericType baseType, int width) {
        
        super(TID_VECTOR);
        assert baseType != null;
        base_type = baseType;
        this.width = width;
    }

    /** accessor for base type */
    public final CType getBaseType() {
        return base_type;
    }
    
    
    /** accessor for width in units of base type */
    public final int getWidthInBase() {
        return width / base_type.getSizeInC();
    }
   
    /** accessor for width in bytes */
    public final int getWidth () {
        return width;
    }

    
    
    @Override
    protected void appendSignature(SimpleStringBuffer buffer) {
        assert false : "No reasonable appendSignature for CVectorType";
    }

    @Override
    public void checkType(CContext context) throws UnpositionedError {
    }

    @Override
    public final int getSize() {
        // seems to be in word  (4-byte) units
        return ((width + 3) / 4);
    }

    @Override
    public final int getSizeInC() {
        // seems to be in byte units
        // Does alignment play a role here?
        return ((width + 3) / 4) * 4;
    }

    @Override
    public boolean isAssignableTo(CType dest) {
        // no need for this: no Java narrowings or widenings.
        return false;
    }

    @Override
    public boolean isCastableTo(CType dest) {
        // no Java casts
        return false;
    }

    public boolean equals (CType other) {
        if (other instanceof CVectorType) {
            return base_type.equals(
                    ((CVectorTypeLow)other).getBaseType())
                    && ((CVectorTypeLow)other).width == width;
        } else {
            return false;
        }
    }

    /**
     * Type name of naked vector type, only used for constants.
     * @return type name of naked vector
     */
    @Override
    public String toString() {
        return "__v_" +  getWidthInBase() + base_type.getSignature();
    }
    /** 
     * Create C (or C++) typedef allowing {@link toString()} to be used as a type. 
     * @return typedefs with ";" and internal line terminators but no final line terminator.
     */
    public String typedefString() {
        return 
        // typedef for vector
        "typedef " + base_type.toString() + " " + toString() 
        + " __attribute__ ((vector_size (" + width + ")));";
 
    }
}

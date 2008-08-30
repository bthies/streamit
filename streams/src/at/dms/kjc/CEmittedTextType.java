package at.dms.kjc;

import at.dms.compiler.UnpositionedError;
import at.dms.util.SimpleStringBuffer;

public class CEmittedTextType extends CType {

    Object[] parts;
    public Object[] getParts() {
        return parts;
    }
    
    public CEmittedTextType() {
    }

    public CEmittedTextType(int typeId) {
        super(typeId);
    }

    public CEmittedTextType(String typeString) {
        this.parts = new Object[]{typeString};
    }
     
    public CEmittedTextType(Object[] parts) {
        for (Object part : parts) {
            assert part instanceof String
                || part instanceof CType;
        }
        this.parts = parts;
     }
    
    @Override
    protected void appendSignature(SimpleStringBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkType(CContext context) throws UnpositionedError {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSize() {
        return getSizeInC() / 4;  // if it had a size in a JVM the size would be...
    }

    @Override
    public int getSizeInC() {
        int size = 0;
        for (Object part : parts) {
            if (part instanceof CType) {
                size += ((CType)part).getSize();
            }
        }
        return size;
    }

    @Override
    public boolean isAssignableTo(CType dest) {
        return false;
    }

    @Override
    public boolean isCastableTo(CType dest) {
        return false;
    }

    @Override
    public String toString() {
        return java.util.Arrays.toString(parts);
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.CEmittedTextType other = new at.dms.kjc.CEmittedTextType();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.CEmittedTextType other) {
        super.deepCloneInto(other);
        other.parts = (java.lang.Object[])at.dms.kjc.AutoCloner.cloneToplevel(this.parts);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

package at.dms.kjc.smp;

import at.dms.kjc.CLongType;

final class CInt64Type extends CLongType {

    public static CInt64Type Int64 = new CInt64Type();

    /**
     * Returns a string representation of this type.
     */
    public String toString() {
        return "uint64_t";
    }
}


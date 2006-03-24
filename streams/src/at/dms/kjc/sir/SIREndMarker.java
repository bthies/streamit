package at.dms.kjc.sir;

import at.dms.kjc.JStatement;

/**
 * This statement is an annotation that marks the end of a given
 * function or stream so that it can be followed through fusion, etc.
 */
public class SIREndMarker extends SIRMarker {
    /**
     * The name of the stream, function, etc. that is beginning at
     * this statement.
     */
    private String name;

    /**
     * For cloner only.
     */
    private SIREndMarker() {
        super();
    }

    /**
     * Create an SIREndMarker.
     *
     * @param name   The name of the stream.
     */
    public SIREndMarker(String name) {
        super();
        this.name = name;
    }

    /**
     * @return the name of the stream being marked.
     */
    public String getName() {
        return name;
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.sir.SIREndMarker other = new at.dms.kjc.sir.SIREndMarker();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.sir.SIREndMarker other) {
        super.deepCloneInto(other);
        other.name = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.name);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

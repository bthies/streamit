package at.dms.kjc.linprog;

import java.io.*;
import java.util.*;

/**
 * Representation of a constraint type for linprog package.
 */

class ConstraintType implements Serializable {
    // for objective function only
    public static final ConstraintType OBJ = new ConstraintType("OBJ");
    // for GE
    public static final ConstraintType GE = new ConstraintType("GE");
    // for EQ
    public static final ConstraintType EQ = new ConstraintType("EQ");

    private final String name;
    private ConstraintType(String name) {
        this.name = name;
    }

    public String toString() {
        return "Constraint type: " + name;
    }

    public boolean equals(Object o) {
        return (o instanceof ConstraintType &&
                ((ConstraintType)o).name.equals(this.name));
    }
}

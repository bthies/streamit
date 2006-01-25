package at.dms.kjc.linprog;

import java.io.*;
import java.util.*;

/**
 * Representation of a constraint for linprog package.
 */

class Constraint implements Serializable {
    public final ConstraintType type;
    public final double[] lhs;
    public final double rhs;

    public Constraint(ConstraintType type, double[] lhs, double rhs) {
        this.type = type;
        this.rhs = rhs;
        this.lhs = lhs;
    }
}

package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreaMIT filter that writes a file to a data source.
 */
public class SIRFileWriter extends SIRPredefinedFilter implements Cloneable {
    /**
     * The filename of the data source.
     */
    private String fileName;

    public SIRFileWriter() {
	super(null,
	      "FileWriter",
	      /* fields */ JFieldDeclaration.EMPTY(),
	      /* methods */ JMethodDeclaration.EMPTY(),
	      new JIntLiteral(null, 1),
	      new JIntLiteral(null, 1),
	      new JIntLiteral(null, 0),
	      null, null);
	this.fileName = "";
    }

    public void setFileName(String fileName) {
	this.fileName = fileName;
    }

    public String getFileName() {
	return this.fileName;
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRFileWriter other = new at.dms.kjc.sir.SIRFileWriter();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRFileWriter other) {
  super.deepCloneInto(other);
  other.fileName = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.fileName);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}



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
}



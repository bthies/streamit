package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.sir.lowering.LoweringConstants;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreaMIT filter that reads from a data source.
 */
public class SIRFileReader extends SIRPredefinedFilter implements Cloneable {
    /**
     * The filename of the data source.
     */
    private String fileName;

    public SIRFileReader() {
	super(null,
	      "FileReader",
	      /* fields */ JFieldDeclaration.EMPTY(),
	      /* methods */ JMethodDeclaration.EMPTY(),
	      new JIntLiteral(null, 0),
	      new JIntLiteral(null, 0),
	      new JIntLiteral(null, 1),
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



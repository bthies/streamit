package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreaMIT filter that writes to data source.
 */
public class SIRFileWriter extends SIRFilter implements Cloneable {

    /**
     * The filename of the data source.
     */
    private String fileName;

    public SIRFileWriter() {
	super();
    }

    public SIRFileWriter(SIRContainer parent,
			 JExpression peek, JExpression pop, JExpression push, 
			 CType inputType,
			 String fileName) {
	super(parent,
	      /* fields */ null,
	      /* methods */ null,
	      peek, pop, push,
	      /* work */ null,
	      /* input type */ inputType,
	      /* output type */ null);
	this.fileName = fileName;
    }

    /**
     * Return shallow copy of this.
     */
    public Object clone() {
	SIRFileWriter f = new SIRFileWriter(getParent(),
					    getPeek(),
					    getPop(),
					    getPush(),
					    getOutputType(),
					    getFileName());
	f.setInit(this.init);
	return f;
    }

    public void setFileName(String fileName) {
	this.fileName = fileName;
    }

    public String getFileName() {
	return this.fileName;
    }
}



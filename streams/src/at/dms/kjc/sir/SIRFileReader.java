package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreaMIT filter that reads from a data source.
 */
public class SIRFileReader extends SIRFilter implements Cloneable {

    /**
     * The filename of the data source.
     */
    private String fileName;

    public SIRFileReader() {
	super();
    }

    public SIRFileReader(SIRContainer parent,
			 JExpression peek, JExpression pop, JExpression push, 
			 CType outputType,
			 String fileName) {
	super(parent,
	      /* fields */ null,
	      /* methods */ null,
	      peek, pop, push,
	      /* work */ null,
	      /* input type */ null,
	      /* output type */ outputType);
	this.fileName = fileName;
    }

    /**
     * Return shallow copy of this.
     */
    public Object clone() {
	SIRFileReader f = new SIRFileReader(getParent(),
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



package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.sir.lowering.LoweringConstants;
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
			 String ident,
			 JExpression peek, JExpression pop, JExpression push, 
			 CType inputType,
			 String fileName) {
	super(parent,
	      ident,
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
					    getIdent(),
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

    /**
     * Returns whether or not this class needs a call to an init
     * function to be generated.  Special library functions like
     * FileReader's and FileWriter's do not need an init call.
     */
    public boolean needsInit() {
	return false;
    }

    /**
     * Returns the C type of the object, which is always a stream_context.
     */
    public String getName() {
        return "ContextContainer";
    }
}



 package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

public class FileInputContent extends PredefinedContent {
    private String filename;

    public FileInputContent(FileInputContent content) {
	super(content);
	filename=content.filename;
    }

    public FileInputContent(SIRFileReader filter) {
	super(filter);
	filename=filter.getFileName();
    }

    public FileInputContent(UnflatFilter unflat) {
	super(unflat);
	filename=((SIRFileWriter)unflat.filter).getFileName();
    }

    public String getFileName() {
	return filename;
    }
}

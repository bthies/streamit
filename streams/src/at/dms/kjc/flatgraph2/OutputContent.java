 package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

public class OutputContent extends PredefinedContent {
    public OutputContent(OutputContent content) {
	super(content);
    }

    public OutputContent(SIRPredefinedFilter filter) {
	super(filter);
    }

    public OutputContent(UnflatFilter unflat) {
	super(unflat);
    }
}

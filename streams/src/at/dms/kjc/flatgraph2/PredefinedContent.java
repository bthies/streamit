package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

public class PredefinedContent extends FilterContent {
    public PredefinedContent(PredefinedContent content) {
	super(content);
    }

    public PredefinedContent(SIRPredefinedFilter filter) {
	super(filter);
    }

    public PredefinedContent(UnflatFilter unflat) {
	super(unflat);
    }
}

package at.dms.kjc.flatgraph2;

import at.dms.kjc.CType;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;
import at.dms.kjc.sir.linear.*;

public class InputContent extends PredefinedContent {
    public InputContent(InputContent content) {
	super(content);
    }

    public InputContent(SIRPredefinedFilter filter) {
	super(filter);
    }

    public InputContent(UnflatFilter unflat) {
	super(unflat);
    }
}

package at.dms.kjc.sir.lowering.partition.linear;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.partition.*;

class LDPConfigSplitJoin extends LDPConfigContainer {

    public LDPConfigSplitJoin(SIRSplitJoin sj, LinearPartitioner partitioner) {
	// pass 0 as dummy argument in case uniform height is -1; then assert afterwards
	super(sj, partitioner, sj.size(), Math.max(0, sj.getRectangularHeight()));
	Utils.assert(sj.isRectangular(), "Require rectangular splitjoins in LDPConfig.");
    }

    protected LDPConfig childConfig(int x, int y) {
	SIRStream c1 = cont.get(x), c2;
	// if we're just accessing a hierarchical unit, return it
	if (y==0 && !(c1 instanceof SIRPipeline)) {
	    c2 = c1;
	} else {
	    // otherwise, we're looking inside a hierarchical unit -- must
	    // be a pipeline
	    Utils.assert(c1 instanceof SIRPipeline);
	    c2 = ((SIRPipeline)c1).get(y);
	}
	return partitioner.getConfig(c2);
    }
}

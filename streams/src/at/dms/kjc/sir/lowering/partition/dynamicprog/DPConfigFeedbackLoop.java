package at.dms.kjc.sir.lowering.partition.dynamicprog;

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

class DPConfigFeedbackLoop extends DPConfigContainer {

    public DPConfigFeedbackLoop(SIRFeedbackLoop cont, DynamicProgPartitioner partitioner) {
	super(cont, partitioner, 1, 2);
    }

    protected DPConfig childConfig(int x, int y) {
	Utils.assert(x==0);
	return partitioner.getConfig(cont.get(y));
    }

}

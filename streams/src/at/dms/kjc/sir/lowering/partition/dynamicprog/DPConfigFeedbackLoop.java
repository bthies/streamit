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
    private static final int[] WIDTH = { 1, 1 };

    public DPConfigFeedbackLoop(SIRFeedbackLoop cont, DynamicProgPartitioner partitioner) {
	super(cont, partitioner, WIDTH, 2);
    }

    protected DPConfig childConfig(int x, int y) {
	assert x==0;
	return partitioner.getConfig(cont.get(y));
    }

}

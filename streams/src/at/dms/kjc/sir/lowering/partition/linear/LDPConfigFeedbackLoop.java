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

class LDPConfigFeedbackLoop extends LDPConfigContainer {
    private static final int[] WIDTH = { 1, 1 };

    public LDPConfigFeedbackLoop(SIRFeedbackLoop cont, LinearPartitioner partitioner) {
	super(cont, partitioner, WIDTH, 2);
    }

    protected LDPConfig childConfig(int x, int y) {
	assert x==0;
	return partitioner.getConfig(cont.get(y));
    }

}

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

class LDPConfigPipeline extends LDPConfigContainer {

    public LDPConfigPipeline(SIRPipeline cont, LinearPartitioner partitioner) {
	super(cont, partitioner, 1, cont.size());
    }

    protected LDPConfig childConfig(int x, int y) {
	Utils.assert(x==0);
	return partitioner.getConfig(cont.get(y));
    }

}

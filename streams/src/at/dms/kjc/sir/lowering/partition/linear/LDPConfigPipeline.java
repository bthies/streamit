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
	super(cont, partitioner, getWidths(cont), cont.size());
    }

    protected LDPConfig childConfig(int x, int y) {
	SIRStream c1 = cont.get(y), c2;
	// if we're just accessing a hierarchical unit, return it
	if (x==0 && !(c1 instanceof SIRSplitJoin)) {
	    c2 = c1;
	} else {
	    // otherwise, we're looking inside a hierarchical unit -- must
	    // be a splitjoin
	    assert c1 instanceof SIRSplitJoin;
	    c2 = ((SIRSplitJoin)c1).get(x);
	}
	return partitioner.getConfig(c2);
    }

    /**
     * Returns the width of all child streams... sj's have width of
     * their size; all else has width of 1.
     */
    private static final int[] getWidths(SIRPipeline cont) {
	int[] result = new int[cont.size()];
	for (int i=0; i<result.length; i++) {
	    SIRStream child = cont.get(i);
	    if (child instanceof SIRSplitJoin) {
		result[i] = ((SIRSplitJoin)child).size();
	    } else {
		result[i] = 1;
	    }
	}
	return result;
    }

}

import java.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.partition.*;

/**
 * Manual partitioning for the FFT2 program.  This is intended as a
 * demo only; it is not intended to be particularly optimized.
 */
public class ManualOpt {

    public static SIRStream manualPartition(SIRStream str) {
	// get the small-numbered CombineDFT streams
	LinkedList dft = new LinkedList();
	for (int i=37; i<=40; i++) {
	    dft.add(ManualPartition.getStream(str, "CombineDFT_" + i));
	}
	
	// unroll them each by a factor of 32, and destroy arrays
	for (int i=0; i<dft.size(); i++) {
	    SIRStream s = (SIRStream)dft.get(i);
	    ManualPartition.unroll(s, 32);
	    ManualPartition.destroyArrays(s);
	}

	return str;
    }
}

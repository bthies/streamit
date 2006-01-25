package at.dms.kjc.sir.lowering.partition;

import java.util.*;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.linear.LinearPartitioner;
import at.dms.kjc.sir.lowering.fusion.*;

/**
 * LinearReplace transform on a stream graph.
 */

public final class LinearReplaceTransform extends StreamTransform {
    /**
     * Linear analyzer used to construct this.
     */
    private LinearAnalyzer lfa;

    public LinearReplaceTransform(LinearAnalyzer lfa) {
        super();
        this.lfa = lfa;
    }

    /**
     * Perform the transform on <str> and return new stream.
     */
    public SIRStream doMyTransform(SIRStream str) {
        // again detect that <str> is linear, since it is a newly constructed stream
        LinearAnalyzer.findLinearFilters(str, KjcOptions.debug, lfa);
        LinearFilterRepresentation linearRep = lfa.getLinearRepresentation(str);
        boolean smallCode = Math.max(linearRep.getCost().getMultiplies(), linearRep.getCost().getAdds()) < LinearPartitioner.MAX_MULT_TO_UNROLL;

        // seems like we can never beat the original filter's
        // implementation (at least in beamformer), so just do nothing
        // if we have a filter
        if (str instanceof SIRFilter) {}
        // if we don't have many multiplies, or if direct replacer is
        // explicitly noted, then generate direct unrolled code
        else if (smallCode || KjcOptions.linearreplacement) { LinearDirectReplacer.doReplace(lfa, str); }
        // otherwise, we have large code...
        // do indirect replacement if option specified
        else if (KjcOptions.linearreplacement2) { LinearIndirectReplacer.doReplace(lfa, str); }
        // do atlas replacement if option specified, and if this
        // filter pushes at least 2 (since presumably atlas can't
        // leverage anything otherwise)
        else if (KjcOptions.atlas && linearRep.getPushCount() >=2) { LinearAtlasReplacer.doReplace(lfa, str); }
        // otherwise, (DEFAULT for large code) do diagonal replace
        else { LinearDiagonalReplacer.doReplace(lfa, str); }

        // kind of hard to get a handle on the new stream... return
        // null for now; this shouldn't get dereferenced in linear
        // partitioner
        return null;
    }

    public String toString() {
        return "LinearReplace Transform, #" + id;
    }

}

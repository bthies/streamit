package at.dms.kjc.vanillaSlice;

import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.*;

public class UniComputeCodeStore extends ComputeCodeStore<UniProcessor> {
    public UniComputeCodeStore(UniProcessor parent) {
        super(parent);
    }
    
    public void addSliceInit(FilterInfo filterInfo, Layout layout) {
        
    }

    public void addSlicePrimePump(FilterInfo filterInfo, Layout layout) {
        // no-op: TODO eventually support spacetime scheduling.
    }

    public void addSliceSteady(FilterInfo filterInfo, Layout layout) {
        
    }
}

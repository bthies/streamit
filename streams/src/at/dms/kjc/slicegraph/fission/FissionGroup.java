package at.dms.kjc.slicegraph.fission;

import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.backendSupport.FilterInfo;

public class FissionGroup {
    public Slice origSlice;
    public FilterInfo origFilterInfo;

    public Slice[] fizzedSlices;

    public FissionGroup(Slice origSlice, FilterInfo origFilterInfo, Slice[] fizzedSlices) {
        this.origSlice = origSlice;
        this.origFilterInfo = origFilterInfo;
        this.fizzedSlices = fizzedSlices;
    }

    public Slice getOrigSlice() {
        return origSlice;
    }

    public FilterInfo getOrigFilterInfo() {
        return origFilterInfo;
    }

    public Slice[] getFizzedSlices() {
        return fizzedSlices;
    }
}

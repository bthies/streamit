package at.dms.kjc.slicegraph.fission;

import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.backendSupport.FilterInfo;

public class FissionGroup {
    public Slice unfizzedSlice;
    public FilterInfo unfizzedFilterInfo;

    public Slice[] fizzedSlices;

    public FissionGroup(Slice unfizzedSlice, FilterInfo unfizzedFilterInfo, Slice[] fizzedSlices) {
        this.unfizzedSlice = unfizzedSlice;
        this.unfizzedFilterInfo = unfizzedFilterInfo;
        this.fizzedSlices = fizzedSlices;
    }

    public Slice getUnfizzedSlice() {
        return unfizzedSlice;
    }

    public FilterInfo getUnfizzedFilterInfo() {
        return unfizzedFilterInfo;
    }

    public Slice[] getFizzedSlices() {
        return fizzedSlices;
    }

    public int getFizzedSliceIndex(Slice slice) {
        int index = -1;
        for(int x = 0 ; x < fizzedSlices.length ; x++) {
            if(fizzedSlices[x].equals(slice)) {
                index = x;
                break;
            }
        }

        return index;
    }
}

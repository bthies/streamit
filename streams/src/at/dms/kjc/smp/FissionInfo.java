package at.dms.kjc.smp;

import at.dms.kjc.slicegraph.Slice;

import java.util.HashMap;
import java.util.List;

public class FissionInfo {

    private static HashMap<Slice, List<Slice>> origToFizzedSet;
    private static HashMap<Slice, Slice> fizzedToOrig;

    static {
        origToFizzedSet = new HashMap<Slice, List<Slice>>();
        fizzedToOrig = new HashMap<Slice, Slice>();
    }

    public static void registerFission(Slice origSlice, List<Slice> fizzedSlices) {
        origToFizzedSet.put(origSlice, fizzedSlices);

        for(Slice slice : fizzedSlices)
            fizzedToOrig.put(slice, origSlice);
    }

    public static boolean isFizzed(Slice slice) {
        return origToFizzedSet.containsKey(slice) || fizzedToOrig.containsKey(slice);
    }

    public static Slice getOrigSlice(Slice fizzedSlice) {
        return fizzedToOrig.get(fizzedSlice);
    }

    public static List<Slice> getFizzedSet(Slice slice) {
        if(origToFizzedSet.containsKey(slice))
            return origToFizzedSet.get(slice);

        if(fizzedToOrig.containsKey(slice))
            return origToFizzedSet.get(fizzedToOrig.get(slice));

        return null;
    }
}

package at.dms.kjc.smp;

import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.fission.FissionGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class FissionGroupStore {

    private static HashSet<FissionGroup> fissionGroups;

    private static HashMap<Slice, List<Slice>> origToFizzed;
    private static HashMap<Slice, Slice> fizzedToOrig;

    static {
        fissionGroups = new HashSet<FissionGroup>();
        origToFizzed = new HashMap<Slice, List<Slice>>();
        fizzedToOrig = new HashMap<Slice, Slice>();
    }

    public static void addFissionGroup(FissionGroup group) {
        fissionGroups.add(group);

        List<Slice> fizzedSlicesList = new LinkedList<Slice>();
        for(Slice slice : group.fizzedSlices)
            fizzedSlicesList.add(slice);
        origToFizzed.put(group.origSlice, fizzedSlicesList);

        for(Slice slice : group.fizzedSlices)
            fizzedToOrig.put(slice, group.origSlice);
    }

    public static Set<FissionGroup> getFissionGroups() {
        return fissionGroups;
    }

    public static boolean isFizzed(Slice slice) {
        return origToFizzed.containsKey(slice) || fizzedToOrig.containsKey(slice);
    }

    public static Slice getOrigSlice(Slice fizzedSlice) {
        return fizzedToOrig.get(fizzedSlice);
    }

    public static List<Slice> getFizzedSlices(Slice slice) {
        if(origToFizzed.containsKey(slice))
            return origToFizzed.get(slice);

        if(fizzedToOrig.containsKey(slice))
            return origToFizzed.get(fizzedToOrig.get(slice));

        return null;
    }

    public static void reset() {
        fissionGroups.clear();
        origToFizzed.clear();
        fizzedToOrig.clear();
    }
}

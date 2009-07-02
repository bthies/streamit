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

    private static HashMap<Slice, FissionGroup> unfizzedToFissionGroup;
    private static HashMap<Slice, FissionGroup> fizzedToFissionGroup;

    static {
        fissionGroups = new HashSet<FissionGroup>();

        unfizzedToFissionGroup = new HashMap<Slice, FissionGroup>();
        fizzedToFissionGroup = new HashMap<Slice, FissionGroup>();
    }

    public static void addFissionGroup(FissionGroup group) {
        fissionGroups.add(group);

        unfizzedToFissionGroup.put(group.unfizzedSlice, group);

        for(Slice slice : group.fizzedSlices)
            fizzedToFissionGroup.put(slice, group);
    }

    public static Set<FissionGroup> getFissionGroups() {
        return fissionGroups;
    }

    public static FissionGroup getFissionGroup(Slice slice) {
        if(unfizzedToFissionGroup.containsKey(slice))
            return unfizzedToFissionGroup.get(slice);

        if(fizzedToFissionGroup.containsKey(slice))
            return fizzedToFissionGroup.get(slice);

        return null;
    }

    public static boolean isFizzed(Slice slice) {
        return unfizzedToFissionGroup.containsKey(slice) ||
            fizzedToFissionGroup.containsKey(slice);
    }

    public static Slice getUnfizzedSlice(Slice fizzedSlice) {
        if(fizzedToFissionGroup.containsKey(fizzedSlice))
            return fizzedToFissionGroup.get(fizzedSlice).unfizzedSlice;

        return null;
    }

    public static Slice[] getFizzedSlices(Slice slice) {
        if(unfizzedToFissionGroup.containsKey(slice))
            return unfizzedToFissionGroup.get(slice).fizzedSlices;

        if(fizzedToFissionGroup.containsKey(slice))
            return fizzedToFissionGroup.get(slice).fizzedSlices;

        return null;
    }

    public static void reset() {
        fissionGroups.clear();
        unfizzedToFissionGroup.clear();
        fizzedToFissionGroup.clear();
    }
}

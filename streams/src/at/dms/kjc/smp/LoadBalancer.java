package at.dms.kjc.smp;

import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.fission.FissionGroup;

import java.util.HashSet;
import java.util.Set;

public class LoadBalancer {

    private static HashSet<FissionGroup> loadBalancedGroups;

    static {
        loadBalancedGroups = new HashSet<FissionGroup>();
    }

    public static boolean canLoadBalance(FissionGroup group) {
        FilterSliceNode filter = group.unfizzedSlice.getFirstFilter();
        FilterInfo filterInfo = group.unfizzedFilterInfo;
        OutputSliceNode output = group.unfizzedSlice.getTail();

        if(filterInfo.push % output.totalWeights(SchedulingPhase.STEADY) != 0)
            return false;

        int numOutputRots = filterInfo.push / output.totalWeights(SchedulingPhase.STEADY);

        for(InterSliceEdge edge : output.getDestSet(SchedulingPhase.STEADY)) {
            InputSliceNode input = edge.getDest();

            int outputWeight = output.getWeight(edge, SchedulingPhase.STEADY);
            int inputWeight = input.getWeight(edge, SchedulingPhase.STEADY);

            if((numOutputRots * outputWeight) % inputWeight != 0)
                return false;
        }

        return true;
    }

    public static void findCandidates() {
        Set<FissionGroup> groups = FissionGroupStore.getFissionGroups();

        for(FissionGroup group : groups) {
            if(canLoadBalance(group)) {
                System.out.println(group.unfizzedSlice.getFirstFilter() + " can be load balanced");
                loadBalancedGroups.add(group);
            }
            else {
                System.out.println(group.unfizzedSlice.getFirstFilter() + " can't be load balanced");
            }
        }
    }

    public static boolean isLoadBalanced(FissionGroup group) {
        return loadBalancedGroups.contains(group);
    }

    public static boolean isLoadBalanced(Slice slice) {
        FissionGroupStore group = FissionGroupStore.getFissionGroup(slice);
        if(group == null)
            return false;
        return isLoadBalanced(group);
    }

    
}

package at.dms.kjc.smp;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.fission.FissionGroup;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LoadBalancer {

    private static final String startItersArrayPrefix = "start_iters";
    private static final String numItersArrayPrefix = "num_iters";
    private static final String cycleCountsArrayPrefix = "cycle_counts";

    private static final String numSamplesDef = "LOAD_BALANCE_SAMPLES";
    private static final int numSamples = 100;

    private static HashSet<FissionGroup> loadBalancedGroups;
    private static HashMap<FissionGroup, Integer> groupIDs;

    static {
        loadBalancedGroups = new HashSet<FissionGroup>();
        groupIDs = new HashMap<FissionGroup, Integer>();
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

        int id = 0;
        for(FissionGroup group : loadBalancedGroups) {
            groupIDs.put(group, new Integer(id));
            id++;
        }
    }

    public static boolean isLoadBalanced(FissionGroup group) {
        return loadBalancedGroups.contains(group);
    }

    public static boolean isLoadBalanced(Slice slice) {
        FissionGroup group = FissionGroupStore.getFissionGroup(slice);
        if(group == null)
            return false;
        return isLoadBalanced(group);
    }

    public static void generateLoadBalancerCode() throws IOException {
        CodegenPrintWriter p;

        p = new CodegenPrintWriter(new BufferedWriter(new FileWriter("load_balancer.h", false)));
        p.println("#ifndef LOAD_BALANCER_H");
        p.println("#define LOAD_BALANCER_H");
        p.println();
        p.println("#include <stdint.h>");
        p.println();
        p.println("extern void initLoadBalancer(int _num_cores, int _num_filters, int _num_samples);");
        p.println("extern void registerGroup(int *_start_iters, int *_num_iters, uint64_t **_cycle_counts);");
        p.println();
        p.println("extern void calcAvgCycles();");
        p.println("extern void swapGroups(int index1, int index2);");
        p.println("extern void sortGroupsInsertionSort();");
        p.println("extern void recalcStartIters();");
        p.println();
        p.println("extern void doLoadBalance();");
        p.println();
        p.println("#endif");
        p.close();

        p = new CodegenPrintWriter(new BufferedWriter(new FileWriter("load_balancer.c", false)));
        p.println("#include <stdlib.h>");
        p.println("#include <stdint.h>");
        p.println();
        p.println("int num_cores;");
        p.println("int num_filters;");
        p.println("int num_samples;");
        p.println();
        p.println("int **start_iters;");
        p.println("int **num_iters;");
        p.println("uint64_t ***cycle_counts;");
        p.println("uint64_t *avg_cycles;");
        p.println();
        p.println("int num_registered;");
        p.println();
        p.println("void initLoadBalancer(int _num_cores, int _num_filters, int _num_samples) {");
        p.println("  num_cores = _num_cores;");
        p.println("  num_filters = _num_filters;");
        p.println("  num_samples = _num_samples;");
        p.println();
        p.println("  start_iters = (int **)malloc(num_filters * sizeof(int *));");
        p.println("  num_iters = (int **)malloc(num_filters * sizeof(int *));");
        p.println("  cycle_counts = (uint64_t ***)malloc(num_filters * sizeof(int **));");
        p.println("  avg_cycles = (uint64_t *)malloc(num_filters * sizeof(float));");
        p.println();
        p.println("  num_registered = 0;");
        p.println("}");
        p.println();
        p.println("void registerGroup(int *_start_iters, int *_num_iters, uint64_t **_cycle_counts) {");
        p.println("  start_iters[num_registered] = _start_iters;");
        p.println("  num_iters[num_registered] = _num_iters;");
        p.println("  cycle_counts[num_registered] = _cycle_counts;");
        p.println("  num_registered++;");
        p.println("}");
        p.println();
        p.println("void calcAvgCycles() {");
        p.println("  int x, y, z;");
        p.println("  uint64_t sum;");
        p.println("  for(x = 0 ; x < num_filters ; x++) {");
        p.println("    sum = 0;");
        p.println("    for(y = 0 ; y < num_cores ; y++) {");
        p.println("      for(z = 0 ; z < num_samples ; z++) {");
        p.println("        sum += cycle_counts[x][y][z];");
        p.println("      }");
        p.println("    }");
        p.println("    avg_cycles[x] = (uint64_t)(sum / (num_cores * num_samples));");
        p.println("  }");
        p.println("}");
        p.println();
        p.println("void swapGroups(int index1, int index2) {");
        p.println("  int *temp_start_iters = start_iters[index1];");
        p.println("  int *temp_num_iters = num_iters[index1];");
        p.println("  uint64_t **temp_cycle_counts = cycle_counts[index1];");
        p.println("  uint64_t temp_avg_cycles = avg_cycles[index1];");
        p.println();
        p.println("  start_iters[index1] = start_iters[index2];");
        p.println("  num_iters[index1] = num_iters[index2];");
        p.println("  cycle_counts[index1] = cycle_counts[index2];");
        p.println("  avg_cycles[index1] = avg_cycles[index2];");
        p.println();
        p.println("  start_iters[index2] = temp_start_iters;");
        p.println("  num_iters[index2] = temp_num_iters;");
        p.println("  cycle_counts[index2] = temp_cycle_counts;");
        p.println("  avg_cycles[index2] = temp_avg_cycles;");
        p.println("}");
        p.println();
        p.println("void sortGroupsInsertionSort() {");
        p.println("  int x, y;");
        p.println();
        p.println("  int *temp_start_iters;");
        p.println("  int *temp_num_iters;");
        p.println("  uint64_t **temp_cycle_counts;");
        p.println("  uint64_t temp_avg_cycles;");
        p.println();
        p.println("  for(x = 1 ; x < num_filters ; x++) {");
        p.println("    temp_start_iters = start_iters[x];");
        p.println("    temp_num_iters = num_iters[x];");
        p.println("    temp_cycle_counts = cycle_counts[x];");
        p.println("    temp_avg_cycles = avg_cycles[x];");
        p.println();
        p.println("    y = x - 1;");
        p.println("    while(y >= 0 && avg_cycles[y] < temp_avg_cycles) {");
        p.println("      start_iters[y + 1] = start_iters[y];");
        p.println("      num_iters[y + 1] = num_iters[y];");
        p.println("      cycle_counts[y + 1] = cycle_counts[y];");
        p.println("      avg_cycles[y + 1] = avg_cycles[y];");
        p.println("      y--;");
        p.println("    }");
        p.println();
        p.println("    start_iters[y + 1] = temp_start_iters;");
        p.println("    num_iters[y + 1] = temp_num_iters;");
        p.println("    cycle_counts[y + 1] = temp_cycle_counts;");
        p.println("    avg_cycles[y + 1] = temp_avg_cycles;");
        p.println("  }");
        p.println("}");
        p.println("");
        p.println("void recalcStartIters() {");
        p.println("  int x, y;");
        p.println("  int start_iter;");
        p.println("  for(x = 0 ; x < num_filters ; x++) {");
        p.println("    start_iter = 0;");
        p.println("    for(y = 0 ; y < num_cores ; y++) {");
        p.println("      if(num_iters[x][y] != -1) {");
        p.println("        start_iters[x][y] = start_iter;");
        p.println("        start_iter += num_iters[x][y];");
        p.println("      }");
        p.println("      else {");
        p.println("        start_iters[x][y] = -1;");
        p.println("      }");
        p.println("    }");
        p.println("  }");
        p.println("}");
        p.println("");
        p.println("void doLoadBalance() {");
        p.println("  calcAvgCycles();");
        p.println("  sortGroupsInsertionSort();");
        p.println("");
        p.println("  // do load balancing stuff");
        p.println("");
        p.println("  recalcStartIters();");
        p.println("}");
        p.close();
    }

    public static void emitLoadBalancerExternGlobals(CodegenPrintWriter p) throws IOException {
        p.println("// Load balancing globals");
        p.println("#define " + numSamplesDef + " " + numSamples);
        p.println();

        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();

            p.println("extern int " + startItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println("extern int " + numItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");

            for(int fizzedSlice = 0 ; fizzedSlice < group.fizzedSlices.length ; fizzedSlice++) {
                int coreID = SMPBackend.scheduler.getComputeNode(group.fizzedSlices[fizzedSlice].getFirstFilter()).getCoreID();
                p.println("extern uint64_t " + cycleCountsArrayPrefix + "_" + id + "_" + coreID + "[" + numSamplesDef + "];");
            }

            p.println("extern uint64_t *" + cycleCountsArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println();
        }
        p.println();
    }

    public static void emitLoadBalancerGlobals(CodegenPrintWriter p) throws IOException {
        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();
            int itersPerFizzedSlice = group.unfizzedFilterInfo.steadyMult / group.fizzedSlices.length;

            p.println();
            p.println("// Load balancing for " + group.unfizzedSlice.getFirstFilter());

            p.print("int " + startItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "] = {0");
            for(int core = 1 ; core < KjcOptions.smp ; core++) {
                p.print(", " + itersPerFizzedSlice * core);
            }
            p.println("};");

            p.print("int " + numItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "] = {" + itersPerFizzedSlice);
            for(int core = 1 ; core < KjcOptions.smp ; core++) {
                p.print(", " + itersPerFizzedSlice);
            }
            p.println("};");

            for(int fizzedSlice = 0 ; fizzedSlice < group.fizzedSlices.length ; fizzedSlice++) {
                int coreID = SMPBackend.scheduler.getComputeNode(group.fizzedSlices[fizzedSlice].getFirstFilter()).getCoreID();
                p.println("uint64_t " + cycleCountsArrayPrefix + "_" + id + "_" + coreID + "[" + numSamplesDef + "];");
            }

            p.print("uint64_t *" + cycleCountsArrayPrefix + "_" + id + 
                    "[" + KjcOptions.smp + "] = {" + cycleCountsArrayPrefix + "_" + id + "_" + 
                    SMPBackend.scheduler.getComputeNode(group.fizzedSlices[0].getFirstFilter()).getCoreID());
            for(int fizzedSlice = 1 ; fizzedSlice < group.fizzedSlices.length ; fizzedSlice++) {
                int coreID = SMPBackend.scheduler.getComputeNode(group.fizzedSlices[fizzedSlice].getFirstFilter()).getCoreID();
                p.print(", " + cycleCountsArrayPrefix + "_" + id + "_" + coreID);
            }
            p.println("};");
        }
        p.println();
    }

    public static void emitLoadBalancerInit(CodegenPrintWriter p) throws IOException {
        p.println("// Initializes load balancer");
        p.println("void " + initLoadBalancerMethodName() + "() {");
        p.indent();

        p.println("initLoadBalancer(" + KjcOptions.smp + ", " + loadBalancedGroups.size() + ", " + numSamplesDef + ");");

        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();

            p.println("registerGroup(" + 
                      startItersArrayPrefix + "_" + id + ", " +
                      numItersArrayPrefix + "_" + id + ", " +
                      cycleCountsArrayPrefix + "_" + id + ");");
        }

        p.outdent();
        p.println("}");
        p.println();
    }

    public static String initLoadBalancerMethodName() {
        return "init_load_balancer";
    }
}

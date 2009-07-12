package at.dms.kjc.smp;

import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.slicegraph.fission.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class LoadBalancer {

    private static final String startItersArrayPrefix = "start_iters";
    private static final String numItersArrayPrefix = "num_iters";
    private static final String filterCycleCountsArrayPrefix = "filter_cycle_counts";
    private static final String coreCycleCountsArrayPrefix = "core_cycle_counts";

    private static final String numSamplesDef = "LB_NUM_SAMPLES";
    private static final int numSamples = 10;

    private static final String samplesIntervalDef = "LB_SAMPLES_INTERVAL";
    private static final int samplesInterval = 25;

    private static HashSet<FissionGroup> loadBalancedGroups;
    private static HashMap<FissionGroup, Integer> groupIDs;

    private static HashMap<Core, JVariableDefinition> sampleBoolVars;
    private static HashMap<Core, JVariableDefinition> sampleIntervalIterVars;
    private static HashMap<Core, JVariableDefinition> numSamplesIterVars;
    private static HashMap<Core, JVariableDefinition> coreStartCycleVars;
    private static HashMap<Core, JVariableDefinition> coreEndCycleVars;

    static {
        loadBalancedGroups = new HashSet<FissionGroup>();
        groupIDs = new HashMap<FissionGroup, Integer>();

        sampleBoolVars = new HashMap<Core, JVariableDefinition>();
        sampleIntervalIterVars = new HashMap<Core, JVariableDefinition>();
        numSamplesIterVars = new HashMap<Core, JVariableDefinition>();
        coreStartCycleVars = new HashMap<Core, JVariableDefinition>();
        coreEndCycleVars = new HashMap<Core, JVariableDefinition>();
    }

    private static boolean canLoadBalance(FissionGroup group) {
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

    public static String getStartIterRef(FissionGroup group, Slice fizzedSlice) {
        int id = groupIDs.get(group).intValue();
        Core core = SMPBackend.scheduler.getComputeNode(fizzedSlice.getFirstFilter());
        int coreIndex = SMPBackend.chip.getCoreIndex(core);

        return startItersArrayPrefix + "_" + id + "[" + coreIndex + "]";
    }

    public static String getNumItersRef(FissionGroup group, Slice fizzedSlice) {
        int id = groupIDs.get(group).intValue();
        Core core = SMPBackend.scheduler.getComputeNode(fizzedSlice.getFirstFilter());
        int coreIndex = SMPBackend.chip.getCoreIndex(core);

        return numItersArrayPrefix + "_" + id + "[" + coreIndex + "]";
    }

    public static String getFilterCycleCountRef(FissionGroup group, Slice fizzedSlice) {
        int id = groupIDs.get(group).intValue();
        Core core = SMPBackend.scheduler.getComputeNode(fizzedSlice.getFirstFilter());
        int coreIndex = SMPBackend.chip.getCoreIndex(core);

        return filterCycleCountsArrayPrefix + "_" + id + "[" + coreIndex + "]";
    }

    public static String getCoreCycleCountRef(Core core) {
        int coreIndex = SMPBackend.chip.getCoreIndex(core);

        return coreCycleCountsArrayPrefix + "[" + coreIndex + "]";
    }

    public static void generateLoadBalancerCode() throws IOException {
        CodegenPrintWriter p;

        p = new CodegenPrintWriter(new BufferedWriter(new FileWriter("load_balancer.h", false)));
        p.println("#ifndef LOAD_BALANCER_H");
        p.println("#define LOAD_BALANCER_H");
        p.println();
        p.println("#include <stdint.h>");
        p.println();
        p.println("extern void initLoadBalancer(int _num_cores, int _num_filters, int _num_samples, uint64_t *_core_cycle_counts);");
        p.println("extern void registerGroup(int *_start_iters, int *_num_iters, int _total_iters, uint64_t *_filter_cycle_counts);");
        p.println();
        p.println("extern void doLoadBalance();");
        p.println();
        p.println("#endif");
        p.close();

        p = new CodegenPrintWriter(new BufferedWriter(new FileWriter("load_balancer.c", false)));
        p.println("#include <stdlib.h>");
        p.println("#include <stdint.h>");
        p.println("#include <stdio.h>");
        p.println();
        p.println("//#define DEBUG");
        p.println();
        p.println("#define MIN(a, b) ((a) < (b) ? (a) : (b))");
        p.println("#define MAX(a, b) ((a) > (b) ? (a) : (b))");
        p.println();
        p.println("static int num_cores;");
        p.println("static int num_filters;");
        p.println("static int num_samples;");
        p.println();
        p.println("static int **start_iters;");
        p.println("static int **num_iters;");
        p.println("static int *total_iters;");
        p.println();
        p.println("static uint64_t **filter_cycle_counts;");
        p.println("static uint64_t *filter_avg_cycles;");
        p.println();
        p.println("static uint64_t *core_cycle_counts;");
        p.println("static uint64_t *core_avg_cycles;");
        p.println();
        p.println("static int num_registered;");
        p.println();
        p.println("void initLoadBalancer(int _num_cores, int _num_filters, int _num_samples, uint64_t *_core_cycle_counts) {");
        p.println("  num_cores = _num_cores;");
        p.println("  num_filters = _num_filters;");
        p.println("  num_samples = _num_samples;");
        p.println();
        p.println("  start_iters = (int **)malloc(num_filters * sizeof(int *));");
        p.println("  num_iters = (int **)malloc(num_filters * sizeof(int *));");
        p.println("  total_iters = (int *)malloc(num_filters * sizeof(int));");
        p.println();
        p.println("  filter_cycle_counts = (uint64_t **)malloc(num_filters * sizeof(int *));");
        p.println("  filter_avg_cycles = (uint64_t *)malloc(num_filters * sizeof(float));");
        p.println();
        p.println("  core_cycle_counts = _core_cycle_counts;");
        p.println("  core_avg_cycles = (uint64_t *)malloc(num_cores * sizeof(uint64_t));");
        p.println();
        p.println("  num_registered = 0;");
        p.println("}");
        p.println();
        p.println("void registerGroup(int *_start_iters, int *_num_iters, int _total_iters, uint64_t *_filter_cycle_counts) {");
        p.println("  start_iters[num_registered] = _start_iters;");
        p.println("  num_iters[num_registered] = _num_iters;");
        p.println("  total_iters[num_registered] = _total_iters;");
        p.println("  filter_cycle_counts[num_registered] = _filter_cycle_counts;");
        p.println("  num_registered++;");
        p.println("}");
        p.println();
        p.println("void calcAvgCycles() {");
        p.println("  int x, y;");
        p.println("  uint64_t sum;");
        p.println("  for(x = 0 ; x < num_filters ; x++) {");
        p.println("    sum = 0;");
        p.println("    for(y = 0 ; y < num_cores ; y++) {");
        p.println("      if(num_iters[x][y] == -1)");
        p.println("        continue;");
        p.println("");
        p.println("      sum += filter_cycle_counts[x][y];");
        p.println("    }");
        p.println("    filter_avg_cycles[x] = (uint64_t)(sum / (num_samples * total_iters[x]));");
        p.println("  }");
        p.println("  for(x = 0 ; x < num_cores ; x++) {");
        p.println("    core_avg_cycles[x] = core_cycle_counts[x] / num_samples;");
        p.println("  }");
        p.println("}");
        p.println();
        p.println("void swapGroups(int index1, int index2) {");
        p.println("  int *temp_start_iters = start_iters[index1];");
        p.println("  int *temp_num_iters = num_iters[index1];");
        p.println("  int temp_total_iters = total_iters[index1];");
        p.println("  uint64_t *temp_filter_cycle_counts = filter_cycle_counts[index1];");
        p.println("  uint64_t temp_filter_avg_cycles = filter_avg_cycles[index1];");
        p.println();
        p.println("  start_iters[index1] = start_iters[index2];");
        p.println("  num_iters[index1] = num_iters[index2];");
        p.println("  total_iters[index1] = total_iters[index2];");
        p.println("  filter_cycle_counts[index1] = filter_cycle_counts[index2];");
        p.println("  filter_avg_cycles[index1] = filter_avg_cycles[index2];");
        p.println();
        p.println("  start_iters[index2] = temp_start_iters;");
        p.println("  num_iters[index2] = temp_num_iters;");
        p.println("  total_iters[index2] = temp_total_iters;");
        p.println("  filter_cycle_counts[index2] = temp_filter_cycle_counts;");
        p.println("  filter_avg_cycles[index2] = temp_filter_avg_cycles;");
        p.println("}");
        p.println();
        p.println("void sortGroupsInsertionSort() {");
        p.println("  int x, y;");
        p.println();
        p.println("  int *temp_start_iters;");
        p.println("  int *temp_num_iters;");
        p.println("  int temp_total_iters;");
        p.println("  uint64_t *temp_filter_cycle_counts;");
        p.println("  uint64_t temp_filter_avg_cycles;");
        p.println();
        p.println("  for(x = 1 ; x < num_filters ; x++) {");
        p.println("    if(filter_avg_cycles[x - 1] < filter_avg_cycles[x]) {");
        p.println("      temp_start_iters = start_iters[x];");
        p.println("      temp_num_iters = num_iters[x];");
        p.println("      temp_total_iters = total_iters[x];");
        p.println("      temp_filter_cycle_counts = filter_cycle_counts[x];");
        p.println("      temp_filter_avg_cycles = filter_avg_cycles[x];");
        p.println();
        p.println("      y = x - 1;");
        p.println("      while(y >= 0 && filter_avg_cycles[y] < temp_filter_avg_cycles) {");
        p.println("        start_iters[y + 1] = start_iters[y];");
        p.println("        num_iters[y + 1] = num_iters[y];");
        p.println("        total_iters[y + 1] = total_iters[y];");
        p.println("        filter_cycle_counts[y + 1] = filter_cycle_counts[y];");
        p.println("        filter_avg_cycles[y + 1] = filter_avg_cycles[y];");
        p.println("        y--;");
        p.println("      }");
        p.println();
        p.println("      start_iters[y + 1] = temp_start_iters;");
        p.println("      num_iters[y + 1] = temp_num_iters;");
        p.println("      total_iters[y + 1] = temp_total_iters;");
        p.println("      filter_cycle_counts[y + 1] = temp_filter_cycle_counts;");
        p.println("      filter_avg_cycles[y + 1] = temp_filter_avg_cycles;");
        p.println("    }");
        p.println("  }");
        p.println("}");
        p.println();
        p.println("int findMinCore() {");
        p.println("  int x;");
        p.println("  int mincore = 0;");
        p.println("  uint64_t mincore_cycles = core_avg_cycles[0];");
        p.println("  for(x = 1 ; x < num_cores ; x++) {");
        p.println("    if(core_avg_cycles[x] < mincore_cycles) {");
        p.println("      mincore = x;");
        p.println("      mincore_cycles = core_avg_cycles[x];");
        p.println("    }");
        p.println("  }");
        p.println("  return mincore;");
        p.println("}");
        p.println();
        p.println("int findMaxCore() {");
        p.println("  int x;");
        p.println("  int maxcore = 0;");
        p.println("  uint64_t maxcore_cycles = core_avg_cycles[0];");
        p.println("  for(x = 1 ; x < num_cores ; x++) {");
        p.println("    if(core_avg_cycles[x] > maxcore_cycles) {");
        p.println("      maxcore = x;");
        p.println("      maxcore_cycles = core_avg_cycles[x];");
        p.println("    }");
        p.println("  }");
        p.println("  return maxcore;");
        p.println("}");
        p.println();
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
        p.println();
        p.println("void clearSamples() {");
        p.println("  int x, y;");
        p.println("  for(x = 0 ; x < num_filters ; x++) {");
        p.println("    for(y = 0 ; y < num_cores ; y++) {");
        p.println("      filter_cycle_counts[x][y] = 0;");
        p.println("    }");
        p.println("  }");
        p.println("  for(x = 0 ; x < num_cores ; x++) {");
        p.println("    core_cycle_counts[x] = 0;");
        p.println("  }");
        p.println("}");
        p.println();
        p.println("void doLoadBalance() {");
        p.println("  int x, y;");
        p.println("  int mincore, maxcore;");
        p.println("  uint64_t cycles_to_transfer;");
        p.println("  int num_iters_to_transfer;");
        p.println();
        p.println("#ifdef DEBUG");
        p.println("  printf(\"==================\\n\");");
        p.println("  printf(\"= Load balancing =\\n\");");
        p.println("  printf(\"==================\\n\");");
        p.println("#endif");
        p.println();
        p.println("  calcAvgCycles();");
        p.println("  sortGroupsInsertionSort();");
        p.println();
        p.println("#ifdef DEBUG");
        p.println("  for(x = 0 ; x < num_cores ; x++) {");
        p.println("    printf(\"core %d avg cycles: %llu\\n\", x, core_avg_cycles[x]);");
        p.println("  }");
        p.println("  printf(\"\\n\");");
        p.println("  for(x = 0 ; x < num_filters ; x++) {");
        p.println("    printf(\"filter %d cycles: \", x);");
        p.println("    for(y = 0 ; y < num_cores ; y++) {");
        p.println("      printf(\"%llu \", filter_cycle_counts[x][y]);");
        p.println("    }");
        p.println("    printf(\"\\n\");");
        p.println("  }");
        p.println("  printf(\"\\n\");");
        p.println("  for(x = 0 ; x < num_filters ; x++) {");
        p.println("    printf(\"filter %d avg cycles: %llu\\n\", x, filter_avg_cycles[x]);");
        p.println("  }");
        p.println("  printf(\"\\n\");");
        p.println("  printf(\"filter iterations:\\n\");");
        p.println("  for(x = 0 ; x < num_cores ; x++) {");
        p.println("    printf(\"core %d: \", x);");
        p.println("    for(y = 0 ; y < num_filters ; y++) {");
        p.println("      printf(\"%6d \", num_iters[y][x]);");
        p.println("    }");
        p.println("    printf(\"\\n\");");
        p.println("  }");
        p.println("  printf(\"\\n\");");
        p.println("#endif");
        p.println();
        p.println("  mincore = findMinCore();");
        p.println("  maxcore = findMaxCore();");
        p.println("  cycles_to_transfer = (core_avg_cycles[maxcore] - core_avg_cycles[mincore]) / 2;");
        p.println();
        p.println("#ifdef DEBUG");
        p.println("  printf(\"mincore: %d\\n\", mincore);");
        p.println("  printf(\"maxcore: %d\\n\", maxcore);");
        p.println("  printf(\"cycles to transfer: %llu\\n\", cycles_to_transfer);");
        p.println("  printf(\"\\n\");");
        p.println("#endif");
        p.println();
        p.println("  for(x = 0 ; x < num_filters ; x++) {");
        p.println("     if(filter_avg_cycles[x] > cycles_to_transfer || num_iters[x][maxcore] == 0)");
        p.println("       continue;");
        p.println();
        p.println("     num_iters_to_transfer = (int)(cycles_to_transfer / filter_avg_cycles[x]);");
        p.println("     num_iters_to_transfer = MIN(num_iters_to_transfer, num_iters[x][maxcore]);");
        p.println();
        p.println("#ifdef DEBUG");
        p.println("     printf(\"filter %d, num iters to transfer: %d\\n\", x, num_iters_to_transfer);");
        p.println("     printf(\"mincore, iters before: %d\\n\", num_iters[x][mincore]);");
        p.println("     printf(\"maxcore, iters before: %d\\n\", num_iters[x][maxcore]);");
        p.println("#endif");
        p.println();
        p.println("     num_iters[x][maxcore] -= num_iters_to_transfer;");
        p.println("     num_iters[x][mincore] += num_iters_to_transfer;");
        p.println("     cycles_to_transfer -= num_iters_to_transfer * filter_avg_cycles[x];");
        p.println();
        p.println("#ifdef DEBUG");
        p.println("     printf(\"mincore, iters after: %d\\n\", num_iters[x][mincore]);");
        p.println("     printf(\"maxcore, iters after: %d\\n\", num_iters[x][maxcore]);");
        p.println("     printf(\"cycles left to transfer: %llu\\n\", cycles_to_transfer);");
        p.println("     printf(\"\\n\");");
        p.println("#endif");
        p.println("  }");
        p.println();
        p.println("  recalcStartIters();");
        p.println("  clearSamples();");
        p.println("}");
        p.close();
    }

    public static void emitLoadBalancerExternGlobals(CodegenPrintWriter p) throws IOException {
        p.println("// Load balancing globals");
        p.println("#define " + numSamplesDef + " " + numSamples);
        p.println("#define " + samplesIntervalDef + " " + samplesInterval);
        p.println();

        p.println("extern uint64_t " + coreCycleCountsArrayPrefix + "[" + KjcOptions.smp + "];");

        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();

            p.println("extern int " + startItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println("extern int " + numItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println("extern uint64_t " + filterCycleCountsArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println();
        }
    }

    private static Slice getFizzedSliceOnCore(FissionGroup group, Core core) {
        for(Slice slice : group.fizzedSlices) {
            if(SMPBackend.scheduler.getComputeNode(slice.getFirstFilter()).equals(core)) {
                return slice;
            }
        }

        return null;
    }

    public static void emitLoadBalancerGlobals(CodegenPrintWriter p) throws IOException {

        p.println("// Core cycle counts for steady-states");
        p.println("uint64_t " + coreCycleCountsArrayPrefix + "[" + KjcOptions.smp + "];");
        p.println();

        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();

            int itersPerFizzedSlice = group.unfizzedFilterInfo.steadyMult / group.fizzedSlices.length;
            int curStartIter = 0;

            p.println("// Load balancing for " + group.unfizzedSlice.getFirstFilter());

            // Construct array for starting iterations
            p.print("int " + startItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "] = {");

            if(getFizzedSliceOnCore(group, SMPBackend.chip.getNthComputeNode(0)) == null) {
                p.print("-1");
            }
            else {
                p.print(curStartIter);
                curStartIter += itersPerFizzedSlice;
            }

            for(int core = 1 ; core < KjcOptions.smp ; core++) {
                if(getFizzedSliceOnCore(group, SMPBackend.chip.getNthComputeNode(0)) == null) {
                    p.print(", -1");
                }
                else {
                    p.print(", " + curStartIter);
                    curStartIter += itersPerFizzedSlice;
                }
            }
            p.println("};");

            // Construct array for number of iterations
            p.print("int " + numItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "] = {");

            if(getFizzedSliceOnCore(group, SMPBackend.chip.getNthComputeNode(0)) == null) {
                p.print("-1");
            }
            else {
                p.print(itersPerFizzedSlice);
            }

            for(int core = 1 ; core < KjcOptions.smp ; core++) {
                if(getFizzedSliceOnCore(group, SMPBackend.chip.getNthComputeNode(0)) == null) {
                    p.print(", -1");
                }
                else {
                    p.print(", " + itersPerFizzedSlice);
                }
            }
            p.println("};");

            // Construct array to store clock cycle samples
            p.println("uint64_t " + filterCycleCountsArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println();
        }
    }

    public static void emitLoadBalancerInit(CodegenPrintWriter p) throws IOException {
        p.println("// Load balancer initialization");
        p.println("void " + initLoadBalancerMethodName() + "() {");
        p.indent();

        p.println("initLoadBalancer(" + KjcOptions.smp + ", " + loadBalancedGroups.size() + ", " + numSamplesDef + ", " + coreCycleCountsArrayPrefix + ");");

        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();

            p.println("registerGroup(" + 
                      startItersArrayPrefix + "_" + id + ", " +
                      numItersArrayPrefix + "_" + id + ", " +
                      group.unfizzedFilterInfo.steadyMult + ", " +
                      filterCycleCountsArrayPrefix + "_" + id + ");");
        }

        p.outdent();
        p.println("}");
        p.println();
    }

    public static String initLoadBalancerMethodName() {
        return "init_load_balancer";
    }

    public static void instrumentMainMethods() {
        for(int x = 0 ; x < KjcOptions.smp ; x++) {
            Core core = SMPBackend.chip.getNthComputeNode(x);
            
            // Whether to sample on this iteration
            JVariableDefinition sampleBoolVar =
                new JVariableDefinition(null,
                                        0,
                                        CStdType.Boolean,
                                        "sample__" + x,
                                        null);
            core.getComputeCode().getMainFunction().addStatementFirst(
                new JVariableDeclarationStatement(sampleBoolVar));
            sampleBoolVars.put(core, sampleBoolVar);

            // Number of steady-state iterations between sampling
            JVariableDefinition sampleIntervalIterVar =
                new JVariableDefinition(null,
                                        0,
                                        CStdType.Integer,
                                        "sampleIntervalIter__" + x,
                                        null);
            core.getComputeCode().getMainFunction().addStatementFirst(
                new JVariableDeclarationStatement(sampleIntervalIterVar));
            sampleIntervalIterVars.put(core, sampleIntervalIterVar);

            // Number of samples between load balancing
            JVariableDefinition numSamplesIterVar =
                new JVariableDefinition(null,
                                        0,
                                        CStdType.Integer,
                                        "numSamplesIter__" + x,
                                        null);
            core.getComputeCode().getMainFunction().addStatementFirst(
                new JVariableDeclarationStatement(numSamplesIterVar));
            numSamplesIterVars.put(core, numSamplesIterVar);

            // Start clock cycle for entire steady-state
            JVariableDefinition coreStartCycleVar =
                new JVariableDefinition(null,
                                        0,
                                        CInt64Type.Int64,
                                        "coreStartCycle__" + x,
                                        null);
            core.getComputeCode().getMainFunction().addStatementFirst(
                new JVariableDeclarationStatement(coreStartCycleVar));
            coreStartCycleVars.put(core, coreStartCycleVar);
            
            // End clock cycle for entire steady-state
            JVariableDefinition coreEndCycleVar =
                new JVariableDefinition(null,
                                        0,
                                        CInt64Type.Int64,
                                        "coreEndCycle__" + x,
                                        null);
            core.getComputeCode().getMainFunction().addStatementFirst(
                new JVariableDeclarationStatement(coreEndCycleVar));
            coreEndCycleVars.put(core, coreEndCycleVar);
        }
    }

    public static void instrumentSteadyStateLoopsBeforeBarrier() {
        for(int x = 0 ; x < KjcOptions.smp ; x++) {
            Core core = SMPBackend.chip.getNthComputeNode(x);

            JVariableDefinition sampleBoolVar = sampleBoolVars.get(core);
            JVariableDefinition sampleIntervalIterVar = sampleIntervalIterVars.get(core);
            JVariableDefinition numSamplesIterVar = numSamplesIterVars.get(core);
            JVariableDefinition coreStartCycleVar = coreStartCycleVars.get(core);
            JVariableDefinition coreEndCycleVar = coreEndCycleVars.get(core);

            // Add if-statement to beginning of steady-state loop that checks to see if it's 
            // time to take a sample.  If so, read clock cycle at beginning of steady-state loop
            core.getComputeCode().addSteadyLoopStatementFirst(
                new JIfStatement(null,
                                 new JEqualityExpression(null,
                                                         true,
                                                         new JLocalVariableExpression(sampleBoolVar),
                                                         new JBooleanLiteral(true)),
                                 new JExpressionStatement(
                                     new JAssignmentExpression(
                                         new JLocalVariableExpression(coreStartCycleVar),
                                         new JMethodCallExpression("rdtsc", new JExpression[0]))),
                                 new JBlock(),
                                 new JavaStyleComment[0]));
            
            // Add block to beginning of steady-state loop that increments 
            // sampleIntervalIter, then checks to see if it's time to take a sample
            JBlock incIntervalIterBlock = new JBlock();
            incIntervalIterBlock.addStatement(
                new JExpressionStatement(
                    new JPostfixExpression(null,
                                           Constants.OPE_POSTINC,
                                           new JLocalVariableExpression(sampleIntervalIterVar))));

            JEqualityExpression setSampleCond =
                new JEqualityExpression(null,
                                        true,
                                        new JLocalVariableExpression(sampleIntervalIterVar),
                                        new JFieldAccessExpression(samplesIntervalDef));

            JBlock setSampleThen = new JBlock();
            setSampleThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(sampleBoolVar),
                        new JBooleanLiteral(true))));
            setSampleThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(sampleIntervalIterVar),
                        new JIntLiteral(0))));

            JBlock setSampleElse = new JBlock();
            setSampleElse.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(sampleBoolVar),
                        new JBooleanLiteral(false))));

            JIfStatement setSampleIf =
                new JIfStatement(null,
                                 setSampleCond,
                                 setSampleThen,
                                 setSampleElse,
                                 new JavaStyleComment[0]);

            incIntervalIterBlock.addStatement(setSampleIf);
            core.getComputeCode().addSteadyLoopStatementFirst(incIntervalIterBlock);

            // Add if-statement to end of steady-state loop that checks to see if it's 
            // time to take a sample.  If so, read clock cycle at end of steady-state loop,
            // then store difference into array of core cycle counts.
            JBlock sampleEndSteadyStateBlock = new JBlock();

            JEqualityExpression sampleEndSteadyStateCond =
                new JEqualityExpression(null,
                                        true,
                                        new JLocalVariableExpression(sampleBoolVar),
                                        new JBooleanLiteral(true));

            JBlock sampleEndSteadyStateThen = new JBlock();
            sampleEndSteadyStateThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(coreEndCycleVar),
                        new JMethodCallExpression("rdtsc", new JExpression[0]))));
            sampleEndSteadyStateThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JFieldAccessExpression(getCoreCycleCountRef(core)),
                        new JAddExpression(
                            new JFieldAccessExpression(getCoreCycleCountRef(core)),
                            new JMinusExpression(
                                null, 
                                new JLocalVariableExpression(coreEndCycleVar),
                                new JLocalVariableExpression(coreStartCycleVar))))));

            JStatement sampleEndSteadyStateIf =
                new JIfStatement(null,
                                 sampleEndSteadyStateCond,
                                 sampleEndSteadyStateThen,
                                 new JBlock(),
                                 new JavaStyleComment[0]);
            sampleEndSteadyStateBlock.addStatement(sampleEndSteadyStateIf);

            core.getComputeCode().addSteadyLoopStatement(sampleEndSteadyStateBlock);
        }
    }

    public static void instrumentSteadyStateLoopsAfterBarrier() {
        for(int x = 0 ; x < KjcOptions.smp ; x++) {
            // Add block after barrier that checks if we sampled.  If so, increment number of
            // samples.  If target number of samples reached, then do load balancing.

            Core core = SMPBackend.chip.getNthComputeNode(x);

            JVariableDefinition sampleBoolVar = sampleBoolVars.get(core);
            JVariableDefinition numSamplesIterVar = numSamplesIterVars.get(core);

            JEqualityExpression ifSampledCond =
                new JEqualityExpression(null,
                                        true,
                                        new JLocalVariableExpression(sampleBoolVar),
                                        new JBooleanLiteral(true));
            
            JBlock ifSampledThen = new JBlock();
            {
                ifSampledThen.addStatement(
                    new JExpressionStatement(
                        new JPostfixExpression(null,
                                               Constants.OPE_POSTINC,
                                               new JLocalVariableExpression(numSamplesIterVar))));
                
                JEqualityExpression loadBalanceCond =
                    new JEqualityExpression(null,
                                            true,
                                            new JLocalVariableExpression(numSamplesIterVar),
                                            new JFieldAccessExpression(numSamplesDef));
                
                JBlock loadBalanceThen = new JBlock();
                if(x == 0) {
                    loadBalanceThen.addStatement(
                        new JExpressionStatement(
                            new JMethodCallExpression("doLoadBalance", new JExpression[0])));
                }
                loadBalanceThen.addStatement(
                    new JExpressionStatement(
                        new JAssignmentExpression(
                            new JLocalVariableExpression(numSamplesIterVar),
                            new JIntLiteral(0))));
                loadBalanceThen.addStatement(
                    new JExpressionStatement(
                        new JEmittedTextExpression("barrier_wait(&barrier)")));
                
                JIfStatement loadBalanceIf =
                    new JIfStatement(null,
                                     loadBalanceCond,
                                     loadBalanceThen,
                                     new JBlock(),
                                     new JavaStyleComment[0]);
                ifSampledThen.addStatement(loadBalanceIf);
            }
            
            JIfStatement ifSampledIf =
                new JIfStatement(null,
                                 ifSampledCond,
                                 ifSampledThen,
                                 new JBlock(),
                                 new JavaStyleComment[0]);
            
            core.getComputeCode().addSteadyLoopStatement(ifSampledIf);
        }
    }

    public static JVariableDefinition getSampleIntervalIterVar(Core core) {
        return sampleIntervalIterVars.get(core);
    }

    public static JVariableDefinition numSamplesIterVar(Core core) {
        return numSamplesIterVars.get(core);
    }

    public static JVariableDefinition coreStartCycleVar(Core core) {
        return coreStartCycleVars.get(core);
    }

    public static JVariableDefinition coreEndCycleVar(Core core) {
        return coreEndCycleVars.get(core);
    }

    public static JVariableDefinition getSampleBoolVar(Core core) {
        return sampleBoolVars.get(core);
    }
}

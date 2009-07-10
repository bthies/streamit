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
    private static final String cycleCountsArrayPrefix = "cycle_counts";
    private static final String totalCycleCountsArrayPrefix = "total_cycle_counts";

    private static final String numSamplesDef = "LB_NUM_SAMPLES";
    private static final int numSamples = 10;

    private static final String samplesIntervalDef = "LB_SAMPLES_INTERVAL";
    private static final int samplesInterval = 25;

    private static HashSet<FissionGroup> loadBalancedGroups;
    private static HashMap<FissionGroup, Integer> groupIDs;

    private static HashMap<Core, JVariableDefinition> sampleIntervalIterVars;
    private static HashMap<Core, JVariableDefinition> numSamplesIterVars;
    private static HashMap<Core, JVariableDefinition> totalStartCycleVars;
    private static HashMap<Core, JVariableDefinition> totalEndCycleVars;
    private static HashMap<Core, JVariableDefinition> sampleBoolVars;

    static {
        loadBalancedGroups = new HashSet<FissionGroup>();
        groupIDs = new HashMap<FissionGroup, Integer>();

        sampleIntervalIterVars = new HashMap<Core, JVariableDefinition>();
        numSamplesIterVars = new HashMap<Core, JVariableDefinition>();
        sampleBoolVars = new HashMap<Core, JVariableDefinition>();
        totalStartCycleVars = new HashMap<Core, JVariableDefinition>();
        totalEndCycleVars = new HashMap<Core, JVariableDefinition>();
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

    public static String getCycleCountRef(FissionGroup group, Slice fizzedSlice) {
        int id = groupIDs.get(group).intValue();
        Core core = SMPBackend.scheduler.getComputeNode(fizzedSlice.getFirstFilter());
        int coreIndex = SMPBackend.chip.getCoreIndex(core);

        return cycleCountsArrayPrefix + "_" + id + "[" + coreIndex + "]";
    }

    public static void generateLoadBalancerCode() throws IOException {
        CodegenPrintWriter p;

        p = new CodegenPrintWriter(new BufferedWriter(new FileWriter("load_balancer.h", false)));
        p.println("#ifndef LOAD_BALANCER_H");
        p.println("#define LOAD_BALANCER_H");
        p.println();
        p.println("#include <stdint.h>");
        p.println();
        p.println("extern void initLoadBalancer(int _num_cores, int _num_filters, int _num_samples, uint64_t *_total_cycle_counts);");
        p.println("extern void registerGroup(int *_start_iters, int *_num_iters, int _total_iters, uint64_t *_cycle_counts);");
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
        p.println("int *total_iters;");
        p.println("uint64_t **cycle_counts;");
        p.println("uint64_t *avg_cycles;");
        p.println();
        p.println("uint64_t *total_cycle_counts;");
        p.println();
        p.println("int num_registered;");
        p.println();
        p.println("void initLoadBalancer(int _num_cores, int _num_filters, int _num_samples, uint64_t *_total_cycle_counts) {");
        p.println("  num_cores = _num_cores;");
        p.println("  num_filters = _num_filters;");
        p.println("  num_samples = _num_samples;");
        p.println();
        p.println("  start_iters = (int **)malloc(num_filters * sizeof(int *));");
        p.println("  num_iters = (int **)malloc(num_filters * sizeof(int *));");
        p.println("  total_iters = (int *)malloc(num_filters * sizeof(int));");
        p.println("  cycle_counts = (uint64_t **)malloc(num_filters * sizeof(int *));");
        p.println("  avg_cycles = (uint64_t *)malloc(num_filters * sizeof(float));");
        p.println();
        p.println("  total_cycle_counts = _total_cycle_counts;");
        p.println();
        p.println("  num_registered = 0;");
        p.println("}");
        p.println();
        p.println("void registerGroup(int *_start_iters, int *_num_iters, int _total_iters, uint64_t *_cycle_counts) {");
        p.println("  start_iters[num_registered] = _start_iters;");
        p.println("  num_iters[num_registered] = _num_iters;");
        p.println("  total_iters[num_registered] = _total_iters;");
        p.println("  cycle_counts[num_registered] = _cycle_counts;");
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
        p.println("      sum += cycle_counts[x][y];");
        p.println("    }");
        p.println("    avg_cycles[x] = (uint64_t)(sum / (num_samples * total_iters[x]));");
        p.println("  }");
        p.println("}");
        p.println();
        p.println("void swapGroups(int index1, int index2) {");
        p.println("  int *temp_start_iters = start_iters[index1];");
        p.println("  int *temp_num_iters = num_iters[index1];");
        p.println("  int temp_total_iters = total_iters[index1];");
        p.println("  uint64_t *temp_cycle_counts = cycle_counts[index1];");
        p.println("  uint64_t temp_avg_cycles = avg_cycles[index1];");
        p.println();
        p.println("  start_iters[index1] = start_iters[index2];");
        p.println("  num_iters[index1] = num_iters[index2];");
        p.println("  total_iters[index1] = total_iters[index2];");
        p.println("  cycle_counts[index1] = cycle_counts[index2];");
        p.println("  avg_cycles[index1] = avg_cycles[index2];");
        p.println();
        p.println("  start_iters[index2] = temp_start_iters;");
        p.println("  num_iters[index2] = temp_num_iters;");
        p.println("  total_iters[index2] = temp_total_iters;");
        p.println("  cycle_counts[index2] = temp_cycle_counts;");
        p.println("  avg_cycles[index2] = temp_avg_cycles;");
        p.println("}");
        p.println();
        p.println("void sortGroupsInsertionSort() {");
        p.println("  int x, y;");
        p.println();
        p.println("  int *temp_start_iters;");
        p.println("  int *temp_num_iters;");
        p.println("  int temp_total_iters;");
        p.println("  uint64_t *temp_cycle_counts;");
        p.println("  uint64_t temp_avg_cycles;");
        p.println();
        p.println("  for(x = 1 ; x < num_filters ; x++) {");
        p.println("    temp_start_iters = start_iters[x];");
        p.println("    temp_num_iters = num_iters[x];");
        p.println("    temp_total_iters = total_iters[x];");
        p.println("    temp_cycle_counts = cycle_counts[x];");
        p.println("    temp_avg_cycles = avg_cycles[x];");
        p.println();
        p.println("    y = x - 1;");
        p.println("    while(y >= 0 && avg_cycles[y] < temp_avg_cycles) {");
        p.println("      start_iters[y + 1] = start_iters[y];");
        p.println("      num_iters[y + 1] = num_iters[y];");
        p.println("      total_iters[y + 1] = total_iters[y];");
        p.println("      cycle_counts[y + 1] = cycle_counts[y];");
        p.println("      avg_cycles[y + 1] = avg_cycles[y];");
        p.println("      y--;");
        p.println("    }");
        p.println();
        p.println("    start_iters[y + 1] = temp_start_iters;");
        p.println("    num_iters[y + 1] = temp_num_iters;");
        p.println("    total_iters[y + 1] = temp_total_iters;");
        p.println("    cycle_counts[y + 1] = temp_cycle_counts;");
        p.println("    avg_cycles[y + 1] = temp_avg_cycles;");
        p.println("  }");
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
        p.println("void doLoadBalance() {");
        p.println("  calcAvgCycles();");
        p.println("  sortGroupsInsertionSort();");
        p.println();
        p.println("  // do load balancing stuff");
        p.println();
        p.println("  recalcStartIters();");
        p.println("}");
        p.close();
    }

    public static void emitLoadBalancerExternGlobals(CodegenPrintWriter p) throws IOException {
        p.println("// Load balancing globals");
        p.println("#define " + numSamplesDef + " " + numSamples);
        p.println("#define " + samplesIntervalDef + " " + samplesInterval);
        p.println();

        p.println("extern uint64_t " + totalCycleCountsArrayPrefix + "[" + KjcOptions.smp + "];");

        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();

            p.println("extern int " + startItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println("extern int " + numItersArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println("extern uint64_t " + cycleCountsArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
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

        p.println("// Total cycle counts for steady-states");
        p.println("uint64_t " + totalCycleCountsArrayPrefix + "[" + KjcOptions.smp + "];");
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
            p.println("uint64_t " + cycleCountsArrayPrefix + "_" + id + "[" + KjcOptions.smp + "];");
            p.println();
        }
    }

    public static void emitLoadBalancerInit(CodegenPrintWriter p) throws IOException {
        p.println("// Load balancer initialization");
        p.println("void " + initLoadBalancerMethodName() + "() {");
        p.indent();

        p.println("initLoadBalancer(" + KjcOptions.smp + ", " + loadBalancedGroups.size() + ", " + numSamplesDef + ", " + totalCycleCountsArrayPrefix + ");");

        for(FissionGroup group : loadBalancedGroups) {
            int id = groupIDs.get(group).intValue();

            p.println("registerGroup(" + 
                      startItersArrayPrefix + "_" + id + ", " +
                      numItersArrayPrefix + "_" + id + ", " +
                      group.unfizzedFilterInfo.steadyMult + ", " +
                      cycleCountsArrayPrefix + "_" + id + ");");
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

            // Start clock cycle for entire steady-state
            JVariableDefinition totalStartCycleVar =
                new JVariableDefinition(null,
                                        0,
                                        CInt64Type.Int64,
                                        "totalStartCycle__" + x,
                                        null);
            core.getComputeCode().getMainFunction().addStatementFirst(
                new JVariableDeclarationStatement(totalStartCycleVar));
            totalStartCycleVars.put(core, totalStartCycleVar);
            
            // End clock cycle for entire steady-state
            JVariableDefinition totalEndCycleVar =
                new JVariableDefinition(null,
                                        0,
                                        CInt64Type.Int64,
                                        "totalEndCycle__" + x,
                                        null);
            core.getComputeCode().getMainFunction().addStatementFirst(
                new JVariableDeclarationStatement(totalEndCycleVar));
            totalEndCycleVars.put(core, totalEndCycleVar);
        }
    }

    public static void instrumentSteadyStateLoops() {
        for(int x = 0 ; x < KjcOptions.smp ; x++) {
            Core core = SMPBackend.chip.getNthComputeNode(x);

            JVariableDefinition sampleIntervalIterVar = sampleIntervalIterVars.get(core);
            JVariableDefinition sampleBoolVar = sampleBoolVars.get(core);
            
            // Add block that increments sampleIntervalIter, then checks to see if it's
            // time to take a sample
            JBlock header = new JBlock();
            header.addStatement(
                new JExpressionStatement(null,
                                         new JPostfixExpression(null,
                                                                Constants.OPE_POSTINC,
                                                                new JLocalVariableExpression(sampleIntervalIterVar)),
                                         null));

            JEqualityExpression ifSampleCond =
                new JEqualityExpression(null,
                                        true,
                                        new JLocalVariableExpression(sampleIntervalIterVar),
                                        new JFieldAccessExpression(samplesIntervalDef));

            JBlock ifSampleThen = new JBlock();
            ifSampleThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(sampleBoolVar),
                        new JBooleanLiteral(true))));
            ifSampleThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(sampleIntervalIterVar),
                        new JIntLiteral(0))));

            JBlock ifNotSampleThen = new JBlock();
            ifNotSampleThen.addStatement(
                new JExpressionStatement(
                    new JAssignmentExpression(
                        new JLocalVariableExpression(sampleBoolVar),
                        new JBooleanLiteral(false))));

            JIfStatement ifSampleStatement =
                new JIfStatement(null,
                                 ifSampleCond,
                                 ifSampleThen,
                                 ifNotSampleThen,
                                 new JavaStyleComment[0]);

            header.addStatement(ifSampleStatement);

            core.getComputeCode().addSteadyLoopStatementFirst(header);
        }
    }

    public static JVariableDefinition getSampleIntervalIterVar(Core core) {
        return sampleIntervalIterVars.get(core);
    }

    public static JVariableDefinition numSamplesIterVar(Core core) {
        return numSamplesIterVars.get(core);
    }

    public static JVariableDefinition totalStartCycleVar(Core core) {
        return totalStartCycleVars.get(core);
    }

    public static JVariableDefinition totalEndCycleVar(Core core) {
        return totalEndCycleVars.get(core);
    }

    public static JVariableDefinition getSampleBoolVar(Core core) {
        return sampleBoolVars.get(core);
    }
}

package at.dms.kjc.smp;

import at.dms.kjc.JExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JThisExpression;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.common.CodegenPrintWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Profiler {

    public static void instrumentBeforeBarrier() throws IOException {
        for (Core core : SMPBackend.chip.getCores()) {
            if (!core.getComputeCode().shouldGenerateCode())
                continue;
            
            core.getComputeCode().addSteadyLoopStatementFirst(
                Util.toStmt("start_times[" + core.getCoreID() + "][perfstats_iter_n" + core.getCoreID() + "] = rdtsc()"));
            
            if(KjcOptions.smp > 1)
                core.getComputeCode().addSteadyLoopStatement(
                    Util.toStmt("barrier_times[" + core.getCoreID() + "][perfstats_iter_n" + core.getCoreID() + "] = rdtsc()"));
        }
    }

    public static void instrumentAfterBarrier() throws IOException {
        for (Core core : SMPBackend.chip.getCores()) {
            if (!core.getComputeCode().shouldGenerateCode())
                continue;
            
            core.getComputeCode().addSteadyLoopStatement(
                Util.toStmt("end_times[" + core.getCoreID() + "][perfstats_iter_n" + core.getCoreID() + "] = rdtsc()"));
            core.getComputeCode().addSteadyLoopStatement(
                Util.toStmt("perfstats_iter_n" + core.getCoreID() + "++"));
        }
        
        JExpression[] perfStatsOutputArgs = new JExpression[0];
        SMPBackend.chip.getNthComputeNode(0).getComputeCode()
            .addCleanupStatement(
                new JExpressionStatement(null, 
                                         new JMethodCallExpression(null, 
                                                                   new JThisExpression(null),
                                                                   "perfStatsOutput", 
                                                                   perfStatsOutputArgs),
                                         null));
    }

    public static void emitProfilerExternGlobals(CodegenPrintWriter p) throws IOException {
        assert(KjcOptions.iterations != -1);
        
        p.println("// Debugging stats");
        p.println("extern uint64_t start_times[" + KjcOptions.smp + "][" + KjcOptions.iterations + "];");
        if(KjcOptions.smp > 1) {
            p.println("extern uint64_t barrier_times[" + KjcOptions.smp + "][" + KjcOptions.iterations + "];");
        }
        p.println("extern uint64_t end_times[" + KjcOptions.smp + "][" + KjcOptions.iterations + "];");
        p.println();
        
        for (Core core : SMPBackend.chip.getCores()) {
            if (!core.getComputeCode().shouldGenerateCode())
                continue;
            p.println("extern int perfstats_iter_n" + core.getCoreID() + ";");
        }
        p.println();
        p.println("extern void perfStatsOutput();");
        p.println();
    }

    public static void emitProfilerGlobals(CodegenPrintWriter p) throws IOException {
        assert(KjcOptions.iterations != -1);
        
        p.println("// Debugging stats");
        p.println("uint64_t start_times[" + KjcOptions.smp + "][" + KjcOptions.iterations + "];");
        if(KjcOptions.smp > 1) {
            p.println("uint64_t barrier_times[" + KjcOptions.smp + "][" + KjcOptions.iterations + "];");
        }
        p.println("uint64_t end_times[" + KjcOptions.smp + "][" + KjcOptions.iterations + "];");
        p.println();
        
        for (Core core : SMPBackend.chip.getCores()) {
            if (!core.getComputeCode().shouldGenerateCode())
                continue;
            p.println("int perfstats_iter_n" + core.getCoreID() + " = 0;");
        }
        p.println();
    }

    public static void generateProfilerOutputCode(CodegenPrintWriter p) throws IOException {
        p.println("void perfStatsOutput() {");
        p.indent();
        if(KjcOptions.smp > 1) {
            p.println("int core, iter;");
            p.println("uint64_t min_start, min_barrier, min_end;");
            p.println("uint64_t work_time, barrier_time;");
            p.println("float aggregate_work_percentage = 0;");
            p.println("uint64_t average_barrier_time = 0;");
            p.println("uint64_t core_work_totals[" + KjcOptions.smp + "];");
            p.println();
            
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("core_work_totals[core] = 0;");
            p.outdent();
            p.println();
            
            p.println("for (iter = 0 ; iter < " + KjcOptions.iterations + " ; iter++) {");
            p.indent();
            
            p.println("printf(\"Steady-state iteration: %d\\n\", iter);");
            p.println("printf(\"=======================\\n\");");
            p.println();
            
            p.println("min_start = -1;");
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("if (start_times[core][iter] < min_start)");
            p.indent();
            p.println("min_start = start_times[core][iter];");
            p.outdent();
            p.outdent();
            p.println();
            
            p.println("min_barrier = -1;");
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("if (barrier_times[core][iter] < min_barrier)");
            p.indent();
            p.println("min_barrier = barrier_times[core][iter];");
            p.outdent();
            p.outdent();
            p.println();
            
            p.println("barrier_time = 0;");
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("if (end_times[core][iter] - start_times[core][iter] > barrier_time)");
            p.indent();
            p.println("barrier_time = end_times[core][iter] - start_times[core][iter];");
            p.outdent();
            p.outdent();
            p.println();
            
            p.println("work_time = 0;");
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("work_time += barrier_times[core][iter] - start_times[core][iter];");
            p.outdent();
            p.println();
            
            p.println("min_end = -1;");
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("if (end_times[core][iter] < min_end)");
            p.indent();
            p.println("min_end = end_times[core][iter];");
            p.outdent();
            p.outdent();
            p.println();
            
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("core_work_totals[core] += barrier_times[core][iter] - start_times[core][iter];");
            p.outdent();
            p.println();
            
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("printf(\"Thread %3d, start:   %10llu            %llu\\n\", core, start_times[core][iter] - min_start, start_times[core][iter]);");
            p.outdent();
            p.println();
            
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("printf(\"Thread %3d, barrier: %10llu %10llu %llu\\n\", core, barrier_times[core][iter] - min_barrier, barrier_times[core][iter] - start_times[core][iter], barrier_times[core][iter]);");
            p.outdent();
            p.println();
            
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("printf(\"Thread %3d, end:     %10llu %10llu %llu\\n\", core, end_times[core][iter] - min_end, end_times[core][iter] - start_times[core][iter], end_times[core][iter]);");
            p.outdent();
            p.println();
            
            p.println("printf(\"\\n\");");
            
            p.println("printf(\"Total work: %llu\\n\", work_time);");
            p.println("printf(\"Total time: %llu\\n\", barrier_time * " + KjcOptions.smp + ");");
            p.println("printf(\"Work percentage: %f\\n\", ((float)work_time / (float)(barrier_time * " + KjcOptions.smp + ")) * 100);");
            p.println("printf(\"\\n\");");
            p.println();
            
            p.println("aggregate_work_percentage += ((float)work_time / (float)(barrier_time * " + KjcOptions.smp + "));");
            p.println("average_barrier_time += barrier_time;");
            
            p.outdent();
            p.println("}");
            p.println();

            p.println("printf(\"Aggregate stats\\n\");");
            p.println("printf(\"===============\\n\");");
            p.println("printf(\"Aggregate work percentage: %f\\n\", (aggregate_work_percentage / " + KjcOptions.iterations + ") * 100);");
            p.println("printf(\"Average barrier time: %llu\\n\", (average_barrier_time / " + KjcOptions.iterations + "));");
            p.println("for (core = 0 ; core < " + KjcOptions.smp + " ; core++)");
            p.indent();
            p.println("printf(\"Core %d avg work per steady state: %llu\\n\", core, core_work_totals[core] / " + KjcOptions.iterations + ");");
            p.outdent();
        }
        else {
            p.println("int iter;");
            p.println("uint64_t sum_work_cycles = 0;");

            p.println("for (iter = 0 ; iter < " + KjcOptions.iterations + " ; iter++) {");
            p.indent();
            p.println("printf(\"Steady-state iteration: %d\\n\", iter);");
            p.println("printf(\"=======================\\n\");");
            p.println("printf(\"Thread 0, start:   %llu\\n\", start_times[0][iter]);");
            p.println("printf(\"Thread 0, end:     %llu\\n\", end_times[0][iter]);");
            p.println("printf(\"Number of cycles:  %llu\\n\", end_times[0][iter] - start_times[0][iter]);");
            p.println("printf(\"\\n\");");
            p.println("sum_work_cycles += end_times[0][iter] - start_times[0][iter];");
            p.outdent();
            p.println("}");

            p.println("printf(\"Average cycles per steady-state: %llu\\n\", (sum_work_cycles / " + KjcOptions.iterations + "));");
        }
        
        p.outdent();
        p.println("}");
        p.println();
    }
}
